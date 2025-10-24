# 포인트 관리 시스템 - TDD 실습 프로젝트

Spring Boot를 사용한 포인트 관리 시스템을 TDD(Test-Driven Development)로 구현하는 실습 프로젝트입니다.

## 목차
- [프로젝트 개요](#프로젝트-개요)
- [기술 스택](#기술-스택)
- [주요 기능](#주요-기능)
- [동시성 제어 방식 분석](#동시성-제어-방식-분석)

---

## 프로젝트 개요

사용자의 포인트를 관리하는 시스템으로, 포인트 충전, 사용, 조회 기능을 제공합니다.
TDD 방식으로 개발하여 테스트 주도 개발의 실무 적용 방법을 학습합니다.

## 기술 스택

- **Java**: 17
- **Framework**: Spring Boot 3.x
- **Build Tool**: Gradle 8.4
- **Testing**: JUnit 5, AssertJ
- **Code Coverage**: JaCoCo

## 주요 기능

### 1. 포인트 충전
- 최소 충전 금액: 1,000원
- 최대 충전 금액: 1,000,000원
- 100원 단위로만 충전 가능
- 최대 보유 가능 포인트: 10,000,000원

### 2. 포인트 사용
- 최대 사용 금액: 100,000원
- 100원 단위로만 사용 가능
- 잔액 부족 시 예외 발생

### 3. 포인트 조회
- 사용자 포인트 잔액 조회
- 포인트 충전/사용 내역 조회

---

## 동시성 제어 방식 분석

### 1. 동시성 문제 정의

#### 1.1 문제 상황
현재 프로젝트의 `UserPointTable`은 thread-safe하지 않은 `HashMap`을 사용합니다.

```java
// UserPointTable.java (수정 불가)
private final Map<Long, UserPoint> table = new HashMap<>();  // ❌ Thread-safe 아님
```

여러 스레드가 동시에 같은 사용자의 포인트를 충전하거나 사용할 경우, 다음과 같은 문제가 발생합니다:

**Race Condition 시나리오:**
```java
// PointService.updatePoint() 메서드
UserPoint current = userPointTable.selectById(id);  // ① 읽기
long newPoint = current.point() + amount;           // ② 계산
userPointTable.insertOrUpdate(id, newPoint);        // ③ 쓰기
```

```
시간 →
Thread 1: [읽기:1000] → [계산:1500] → [쓰기:1500]
Thread 2: [읽기:1000] → [계산:1500] → [쓰기:1500]
                                      최종: 1500 (기대값: 2000)
```

#### 1.2 발견된 동시성 문제

**테스트 결과 (동시성 제어 전):**

1. **동시 충전 테스트**
   - 예상: 15,000원 (5,000 + 1,000 × 10번)
   - 실제: 6,000원
   - **손실: 9,000원 (60% 손실!)**

2. **충전과 사용 동시 발생**
   - 예상: 60,000원
   - 실제: 55,000원
   - **손실: 5,000원**

3. **잔액 부족 검증 실패 가능성**
   - 동시에 여러 사용 요청 시 음수 포인트 발생 가능

---

### 2. 동시성 제어 방법 비교

#### 2.1 고려한 방법들

| 방법 | 사용자별 독립성 | 구현 난이도 | 성능 | 안전성 | 적용 가능 |
|------|----------------|------------|------|--------|----------|
| **전체 synchronized** | ❌ | ⭐ 매우 쉬움 | ⭐ | ⭐⭐⭐⭐⭐ | ✅ |
| **사용자별 ReentrantLock** | ✅ | ⭐⭐⭐ 보통 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ✅ |
| **사용자별 synchronized(Object)** | ✅ | ⭐⭐ 쉬움 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ✅ |
| **ReadWriteLock** | ✅ | ⭐⭐⭐⭐ 어려움 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ✅ |
| **ConcurrentHashMap** | - | - | - | - | ❌ 테이블 수정 금지 |
| **낙관적 Lock** | - | - | - | - | ❌ 테이블 수정 금지 |

#### 2.2 각 방법 상세 분석

##### A. 전체 synchronized (채택 ✅)

```java
public synchronized UserPoint chargePoint(long id, long amount) {
    return updatePoint(id, amount, TransactionType.CHARGE);
}
```

**장점:**
- ✅ 구현이 매우 간단 (키워드 하나만 추가)
- ✅ HashMap 완전히 보호됨
- ✅ 모든 동시성 문제 확실히 해결
- ✅ 테이블 클래스 수정 불필요

**단점:**
- ❌ 모든 사용자가 순차적으로 처리됨 (성능 최악)
- ❌ 사용자 A의 충전 중에 사용자 B도 대기해야 함
- ❌ 처리량(Throughput) 저하

**적용 이유:**
1. UserPointTable이 `HashMap`을 사용하므로 완전한 동기화 필수
2. 학습/실습 프로젝트로 안전성이 성능보다 중요
3. 테이블 클래스 수정 금지 제약 조건

---

##### B. 사용자별 ReentrantLock

```java
@Service
public class PointService {
    private final ConcurrentHashMap<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    public UserPoint chargePoint(long userId, long amount) {
        ReentrantLock lock = userLocks.computeIfAbsent(userId, id -> new ReentrantLock());
        lock.lock();
        try {
            return updatePoint(userId, amount, TransactionType.CHARGE);
        } finally {
            lock.unlock();
        }
    }
}
```

**장점:**
- ✅ 사용자별 독립적 처리 (성능 우수)
- ✅ timeout, tryLock 등 다양한 기능
- ✅ 비즈니스 로직(읽기-수정-쓰기) 보호

**단점:**
- ⚠️ HashMap 동시 접근 위험 (이론적으로)
- ❌ Lock 관리 복잡도 증가
- ❌ 메모리 사용 증가 (Lock 객체 저장)

**미채택 이유:**
- HashMap 자체는 여전히 thread-safe하지 않음
- 다른 사용자라도 HashMap 내부 구조(버킷) 충돌 가능

---

##### C. ConcurrentHashMap + AtomicLong (이상적이지만 불가능)

```java
// ❌ UserPointTable 수정 불가로 적용 불가능
public class UserPointTable {
    private final ConcurrentHashMap<Long, AtomicLong> points = new ConcurrentHashMap<>();

    public long addAndGet(long userId, long amount) {
        return points.computeIfAbsent(userId, id -> new AtomicLong(0))
                     .addAndGet(amount);  // 원자적 연산!
    }
}
```

**장점:**
- ✅ 최고의 성능
- ✅ 완벽한 thread-safety
- ✅ Lock-free 알고리즘

**단점:**
- ❌ 프로젝트 제약: "Database Tables (절대 수정 금지)"

---

### 3. 최종 선택: 전체 synchronized

#### 3.1 선택 근거

```java
public synchronized UserPoint getUserPoint(long userId) { ... }
public synchronized List<PointHistory> getUserPointHistory(long userId) { ... }
public synchronized UserPoint chargePoint(long id, long amount) { ... }
public synchronized UserPoint usePoint(long id, long amount) { ... }
```

**핵심 이유:**

1. **안전성 우선**
   - UserPointTable의 HashMap을 완전히 보호
   - 모든 스레드가 순차 접근하여 Race Condition 방지

2. **제약 조건 준수**
   - UserPointTable 수정 금지 조건 충족
   - 최소한의 코드 변경으로 문제 해결

3. **학습 목적**
   - 동시성 문제와 해결 과정 명확히 학습
   - TDD 방식으로 문제 발견 → 해결 과정 실습

#### 3.2 테스트 결과 (동시성 제어 후)

```
✅ 동시 충전 테스트: PASSED (4.999s)
   - 예상: 15,000원 / 실제: 15,000원

✅ 동시 사용 테스트: PASSED (4.025s)
   - 음수 잔액 발생 없음

✅ 충전+사용 동시 테스트: PASSED (5.958s)
   - 예상: 60,000원 / 실제: 60,000원

전체 테스트: 24개 전체 통과
```

---

### 4. 성능 분석

#### 4.1 현재 방식의 성능 특성

**처리 시간:**
- 단일 사용자 요청: 200~400ms (DB 시뮬레이션 지연)
- 동시 요청 10개: 약 2~4초 (순차 처리)

**병목 지점:**
- synchronized로 인한 직렬화
- 사용자 100명 동시 요청 시 약 20~40초 소요

#### 4.2 개선 방향 (실무 환경)

**실무 적용 시 고려사항:**

1. **데이터베이스 변경**
   ```java
   // MySQL, PostgreSQL 등 실제 DB 사용
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   UserPoint findByIdWithLock(Long id);
   ```

2. **사용자별 Lock + ConcurrentHashMap**
   ```java
   // UserPointTable을 ConcurrentHashMap으로 변경 후
   // Service에서 사용자별 Lock 적용
   ```

3. **분산 Lock (Redis)**
   ```java
   // 다중 서버 환경에서는 Redis 등 분산 Lock 필요
   @RedisLock(key = "#userId")
   public UserPoint chargePoint(long userId, long amount) { ... }
   ```

---

### 5. 결론

#### 5.1 현재 구현

- **방식**: 전체 synchronized
- **적용 범위**: 모든 public 메서드
- **효과**: 모든 동시성 문제 해결

#### 5.2 Trade-off

| 항목 | 평가 | 설명 |
|------|------|------|
| **안전성** | ⭐⭐⭐⭐⭐ | 모든 동시성 문제 해결 |
| **성능** | ⭐⭐ | 순차 처리로 성능 저하 |
| **구현 복잡도** | ⭐⭐⭐⭐⭐ | 매우 간단 |
| **유지보수** | ⭐⭐⭐⭐⭐ | 이해하기 쉬움 |

#### 5.3 학습 성과

1. **동시성 문제 이해**
   - Race Condition 실제 경험
   - 읽기-수정-쓰기 패턴의 위험성 학습

2. **TDD 실습**
   - Red: 동시성 테스트 작성 (실패 확인)
   - Green: synchronized 적용 (테스트 통과)
   - Refactor: 코드 정리 및 문서화

3. **트레이드오프 분석**
   - 안전성 vs 성능
   - 간단함 vs 최적화
   - 현실적 제약 조건 고려

---

## 테스트 실행

```bash
# 전체 테스트 실행
./gradlew test

# 동시성 테스트만 실행
./gradlew test --tests "PointServiceTest\$ConcurrencyTest"

# 커버리지 리포트 확인
open build/reports/tests/test/index.html
```

## 프로젝트 구조

```
src/
├── main/java/io/hhplus/tdd/
│   ├── point/
│   │   ├── PointController.java       # REST API
│   │   ├── PointService.java          # 비즈니스 로직 (synchronized 적용)
│   │   ├── UserPoint.java             # 포인트 도메인
│   │   ├── PointHistory.java          # 내역 도메인
│   │   └── TransactionType.java       # 거래 타입 Enum
│   └── database/
│       ├── UserPointTable.java        # 포인트 저장소 (수정 금지)
│       └── PointHistoryTable.java     # 내역 저장소 (수정 금지)
└── test/java/io/hhplus/tdd/point/
    └── PointServiceTest.java          # 단위 테스트 (동시성 테스트 포함)
```

---

## 참고 자료

- [CLAUDE.md](./CLAUDE.md) - 프로젝트 개발 가이드
- [Java Concurrency in Practice](https://jcip.net/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
