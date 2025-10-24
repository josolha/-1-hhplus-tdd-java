# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소에서 작업할 때 참고하는 가이드입니다.

## 프로젝트 개요

Spring Boot를 사용한 포인트 관리 시스템을 TDD(Test-Driven Development)로 구현하는 실습 프로젝트입니다. Gradle과 Java 17을 사용합니다.

## 빌드 및 테스트 명령어

```bash
# 모든 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests PointServiceTest

# 특정 테스트 메서드 실행
./gradlew test --tests PointServiceTest.포인트_충전_성공

# 프로젝트 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 클린 빌드
./gradlew clean build
```

## 아키텍처

### 계층 구조
- **Controller 계층** (`io.hhplus.tdd.point.PointController`): REST API 엔드포인트
- **Service 계층** (`io.hhplus.tdd.point.PointService`): 비즈니스 로직 (현재 비어있음, 구현 필요)
- **Database 계층** (`io.hhplus.tdd.database.*`): 지연 시간이 시뮬레이션된 인메모리 저장소

### 주요 컴포넌트

**Database Tables (절대 수정 금지)**
- `UserPointTable`: 사용자 포인트를 저장하는 인메모리 저장소. `selectById()`와 `insertOrUpdate()` 메서드 제공
- `PointHistoryTable`: 포인트 거래 내역을 저장하는 인메모리 저장소. `insert()`와 `selectAllByUserId()` 메서드 제공
- 두 테이블 모두 실제 데이터베이스 지연을 시뮬레이션하기 위한 throttling(200-300ms 랜덤 지연) 포함

**도메인 모델**
- `UserPoint`: `id`, `point`, `updateMillis`를 포함하는 Record 클래스
- `PointHistory`: 거래 내역 정보를 포함하는 Record 클래스
- `TransactionType`: `CHARGE`(충전)와 `USE`(사용) 값을 가진 Enum

**에러 처리**
- `ApiControllerAdvice`: 전역 예외 핸들러로 500 상태와 에러 메시지 반환

### 구현해야 할 API 엔드포인트

```
GET    /point/{id}           - 사용자 포인트 잔액 조회
GET    /point/{id}/histories - 사용자 포인트 거래 내역 조회
PATCH  /point/{id}/charge    - 포인트 충전 (body: amount를 long 타입으로)
PATCH  /point/{id}/use       - 포인트 사용 (body: amount를 long 타입으로)
```

## TDD 작업 흐름

이 프로젝트는 테스트 주도 개발(TDD)을 따릅니다:

1. **Red**: `src/test/java/io/hhplus/tdd/point/`에 실패하는 테스트를 먼저 작성
2. **Green**: 테스트를 통과시키기 위한 최소한의 코드를 `PointService`에 구현
3. **Refactor**: 테스트가 통과하는 상태를 유지하며 코드 개선

### 테스트 구조
- `PointServiceTest`: 비즈니스 로직을 위한 단위 테스트 (주요 초점)
- `PointControllerTest`: API 엔드포인트를 위한 통합 테스트 (선택사항)

### JaCoCo 커버리지

프로젝트에 JaCoCo 코드 커버리지 리포팅이 포함되어 있습니다. 테스트 실행 후 커버리지 리포트가 생성됩니다.

## 구현 시 유의사항

- `PointService` 클래스는 비어있으며 구현이 필요합니다
- Controller 메서드들은 현재 더미 데이터를 반환하고 있으며 `PointService`와 통합이 필요합니다
- 제공된 Table 클래스들은 공개된 API만 사용해야 합니다
- 시뮬레이션된 데이터베이스 지연으로 인한 동시성 문제를 고려해야 합니다

## 코딩 컨벤션

### Java 네이밍 규칙
- **클래스**: PascalCase (예: `PointService`, `UserPoint`)
- **메서드/변수**: camelCase (예: `chargePoint`, `userId`)
- **상수**: UPPER_SNAKE_CASE (예: `MIN_POINT_AMOUNT`, `MAX_CHARGE_LIMIT`)
- **패키지**: 소문자 (예: `io.hhplus.tdd.point`)

### 테스트 네이밍
- **형식**: `{테스트대상}_{상황}_{기대결과}`
- **한글 허용**: 테스트의 의도를 명확히 전달하기 위해 한글 사용 권장
- **예시**:
  - `포인트_충전_성공`
  - `음수_금액_충전시_예외발생`
  - `잔액부족시_포인트_사용_실패`
  - `동시에_100명이_충전해도_정확한_잔액_유지`

### 테스트 구조
- **Given-When-Then 패턴 사용**:
  ```java
  @Test
  void 포인트_충전_성공() {
      // given: 테스트 준비 (초기 상태 설정)
      long userId = 1L;
      long chargeAmount = 1000L;

      // when: 테스트 실행 (실제 동작)
      UserPoint result = pointService.charge(userId, chargeAmount);

      // then: 검증 (기대 결과 확인)
      assertThat(result.point()).isEqualTo(1000L);
  }
  ```
- **한 테스트는 한 가지만 검증**: 여러 검증이 필요하면 테스트를 분리
- **AssertJ 사용**: `assertThat()` 스타일 선호

### Spring Boot 계층별 책임
- **Controller**:
  - HTTP 요청/응답 처리만
  - 비즈니스 로직 포함 금지
  - 서비스 계층 호출 후 결과 반환
- **Service**:
  - 모든 비즈니스 로직 위치
  - 입력값 검증
  - 트랜잭션 관리
  - 동시성 제어
- **도메인 모델**:
  - 불변성 유지 (Record 클래스 활용)
  - 비즈니스 규칙 캡슐화

### 예외 처리
- **비즈니스 규칙 위반**: `IllegalArgumentException` 사용
- **예외 메시지**: 구체적이고 명확하게 작성
  - ❌ "Invalid input"
  - ✅ "충전 금액은 0보다 커야 합니다. 입력값: -1000"
- **예외는 빠르게**: Early Return, Guard Clause 활용

### 주석 작성
- **JavaDoc**: public 메서드에만 작성 (선택적)
- **구현 주석**: "무엇을" 보다 "왜"를 설명
  - ❌ `// userId로 포인트 조회`
  - ✅ `// 동시성 제어를 위해 userId 기준으로 락 획득`
- **불필요한 주석 지양**: 코드 자체가 명확하면 주석 불필요

### 동시성 제어
- **Critical Section 최소화**: synchronized 블록 범위를 최소한으로
- **Lock 객체 분리**: 사용자별 Lock 사용 고려
- **데드락 방지**: Lock 획득 순서 일관성 유지

## Git 커밋 메시지 컨벤션

### 커밋 메시지 구조
```
{타입}: {제목} (50자 이내)

- {구체적인 변경사항 1}
- {구체적인 변경사항 2}
- {구체적인 변경사항 3}
```

### 커밋 타입
- **feat**: 새로운 기능 추가
- **fix**: 버그 수정
- **refactor**: 리팩토링 (기능 변경 없음)
- **test**: 테스트 코드 추가/수정
- **docs**: 문서 수정 (README, 주석, 분석 문서 등)
- **style**: 코드 포맷팅, 세미콜론 추가 등
- **chore**: 빌드 설정, 패키지 매니저 설정 등

### 좋은 커밋 메시지 예시
```
feat: 포인트 충전 API 구현

- PointService.charge() 메서드 추가
- 충전 금액 양수 검증 로직 포함
- UserPointTable 업데이트 및 히스토리 기록
- PointController에 서비스 연동
```

```
test: 동시성 포인트 충전 테스트 추가

- 100명이 동시에 1000원씩 충전하는 시나리오
- CountDownLatch로 동시 실행 보장
- 최종 잔액 100,000원 검증
```

```
refactor: 포인트 검증 로직을 별도 메서드로 분리

- validateAmount() 메서드 추출
- 중복 검증 코드 제거
- 테스트 통과 유지
```

### 나쁜 커밋 메시지 예시
❌ "update code"
❌ "fix bug"
❌ "코드 수정"
❌ "테스트 추가함"
❌ "WIP"

### TDD 작업 시 커밋 패턴
1. **Red 단계**:
   ```
   test: {기능명} 테스트 케이스 작성

   - {테스트 시나리오 1}
   - {테스트 시나리오 2}
   - 현재 테스트 실패 (Red 단계)
   ```

2. **Green 단계**:
   ```
   feat: {기능명} 구현

   - {구현한 로직 설명}
   - 모든 테스트 통과 확인
   ```

3. **Refactor 단계**:
   ```
   refactor: {대상} 리팩토링

   - {개선 사항}
   - 기능 동작 유지, 테스트 통과
   ```

### 커밋 원칙
- **구체적으로**: 무엇을 왜 변경했는지 명확히
- **깔끔하게**: AI 도구 사용 흔적 남기지 않기
  - "🤖 Generated with Claude Code" 추가 금지
  - "Co-Authored-By: Claude" 추가 금지
- **일관성**: 한글/영어 스타일을 프로젝트 전체에서 일관되게 유지
- **원자성**: 하나의 커밋은 하나의 논리적 변경사항만 포함

### 커밋 제외 파일
- `.env`, `.env.local` - 환경 변수
- `credentials.json`, `secrets.yaml` - 민감 정보
- `.DS_Store`, `Thumbs.db` - OS 생성 파일
- `*.log` - 로그 파일
- `.idea/`, `.vscode/` - IDE 설정 (공유 설정 제외)

## 사용 가능한 커스텀 명령어

프로젝트에는 작업을 자동화하는 커스텀 명령어들이 있습니다:

### TDD 관련
- `/tdd-red` - 실패하는 테스트 작성 (Red 단계)
- `/tdd-green` - 테스트 통과 최소 코드 작성 (Green 단계)
- `/tdd-refactor` - 리팩토링 제안 및 실행 (Refactor 단계)
- `/tdd-full` - Red → Green → Refactor 전체 사이클 실행

### Git 관련
- `/commit` - 변경사항 분석 후 구체적인 커밋 메시지 작성 및 커밋
