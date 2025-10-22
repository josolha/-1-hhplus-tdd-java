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
