package io.hhplus.tdd.point;


import static org.assertj.core.api.Assertions.*;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;


public class PointServiceTest {

    private PointService pointService;
    private UserPointTable userPointTable;
    private PointHistoryTable pointHistoryTable;

    // DB 상태 시각화 헬퍼 메서드
    private void printUserPointTable(String title) {
        System.out.println("\n========== " + title + " ==========");
        System.out.println("┌─────────┬──────────┬─────────────────────┐");
        System.out.println("│ User ID │  Point   │    Update Time      │");
        System.out.println("├─────────┼──────────┼─────────────────────┤");

        for (long userId = 1L; userId <= 3L; userId++) {
            UserPoint up = userPointTable.selectById(userId);
            System.out.printf("│   %-5d │  %-7d │  %s  │%n",
                up.id(), up.point(),
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new java.util.Date(up.updateMillis())));
        }
        System.out.println("└─────────┴──────────┴─────────────────────┘\n");
    }

    private void printPointHistoryTable(String title) {
        System.out.println("\n========== " + title + " ==========");
        System.out.println("┌────┬─────────┬─────────┬──────────┬─────────────────────┐");
        System.out.println("│ ID │ User ID │  Amount │   Type   │    Update Time      │");
        System.out.println("├────┼─────────┼─────────┼──────────┼─────────────────────┤");

        for (long userId = 1L; userId <= 3L; userId++) {
            List<PointHistory> histories = pointHistoryTable.selectAllByUserId(userId);
            for (PointHistory h : histories) {
                System.out.printf("│ %-2d │   %-5d │  %-6d │  %-6s  │  %s  │%n",
                    h.id(), h.userId(), h.amount(), h.type(),
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new java.util.Date(h.updateMillis())));
            }
        }
        System.out.println("└────┴─────────┴─────────┴──────────┴─────────────────────┘\n");
    }

    @BeforeEach
    void setUp(){
        pointHistoryTable = new PointHistoryTable();
        userPointTable = new UserPointTable();
        pointService = new PointService(userPointTable, pointHistoryTable);

        // 더미 데이터 생성

        // 사용자 1: 5000 포인트 보유
        userPointTable.insertOrUpdate(1L, 5000L);
        pointHistoryTable.insert(1L, 5000L, TransactionType.CHARGE, System.currentTimeMillis());

        // 사용자 2: 10000 포인트 보유, 충전 및 사용 내역 있음
        userPointTable.insertOrUpdate(2L, 10000L);
        pointHistoryTable.insert(2L, 15000L, TransactionType.CHARGE, System.currentTimeMillis());
        pointHistoryTable.insert(2L, 5000L, TransactionType.USE, System.currentTimeMillis());

        // 사용자 3: 0 포인트 (빈 계정)
        userPointTable.insertOrUpdate(3L, 0L);
    }

    @Test
    @DisplayName("포인트 조회시 사용자의 포인트를 반환한다")
    public void getUserPoint_WhenSearchUserPoint() throws Exception{
        //given
        long userId = 1L;

        //when
        UserPoint userPoint = pointService.getUserPoint(userId);

        //then
        assertThat(userPoint.id()).isEqualTo(userId);
        printUserPointTable("UserPoint 테이블");
        printPointHistoryTable("PointHistory 테이블");

    }
    @Test
    @DisplayName("해당 사용자의 포인트 충전/이용 내역 조회")
    public void getUserPointHistory_ReturnsNotNull() throws Exception{
        //given
        long userId = 1L;

        //when
        List<PointHistory> histories = pointService.getUserPointHistory(userId);

        //then
        assertThat(histories).isNotNull();
    }
    
    @Test
    @DisplayName("포인트 충전 시 잔액이 증가한다")
    public void chargePoint() throws Exception{
        //given
        long userId = 1L;
        long chargeAmount = 1000L;
        UserPoint before = userPointTable.selectById(userId);
        printUserPointTable("충전 전 UserPoint 테이블");

        //when
        UserPoint result = pointService.chargePoint(userId,chargeAmount);
        printUserPointTable("충전 후 UserPoint 테이블");
        printPointHistoryTable("충전 후 PointHistory 테이블");

        //then
        assertThat(result.point()).isEqualTo(before.point()+chargeAmount);
    }
      

    @Test
    @DisplayName("포인트 충전 시 내역 조회시 충전 기록이 조회된다")
    public void getUserPointHistory_AfterCharge() throws Exception{
        //given
        long userId = 1L;
        pointService.chargePoint(userId, 1000L);  // 먼저 충전!

        //when
        List<PointHistory> histories = pointService.getUserPointHistory(userId);

        //then
        assertThat(histories).isNotNull();
        assertThat(histories).isNotEmpty();
        assertThat(histories)
                .extracting("userId", "type")
                .contains(tuple(userId, TransactionType.CHARGE));
    }
    @Test
    @DisplayName("포인트 사용 시 잔액이 감소한다")
    public void usePoint() throws Exception{
        //given
        long userId = 1L;
        UserPoint before = userPointTable.selectById(userId);
        printUserPointTable("사용 전 UserPoint 테이블");

        //when
        UserPoint result = pointService.usePoint(userId, 500L);
        printUserPointTable("사용 후 UserPoint 테이블");
        printPointHistoryTable("사용 후 PointHistory 테이블");

        //then
        assertThat(result.point()).isEqualTo(before.point()-500L);
    }

    @Test
    @DisplayName("잔고가 부족할 경우 포인트 사용은 실패한다")
    public void usePoint_FailsWhenInsufficientBalance() throws Exception{
        //given
        long userId = 999L;
        pointService.chargePoint(userId, 1000L);

        //when & then
        assertThatThrownBy(() -> pointService.usePoint(userId, 2000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잔고가 부족합니다");
    }

    @Nested
    @DisplayName("동시성 문제 테스트")
    class ConcurrencyTest {

        @Test
        @DisplayName("동시에 같은 사용자가 포인트 충전 시 모든 충전이 반영되어야 한다")
        public void concurrentCharge_ShouldReflectAllCharges() throws Exception {
            //given
            long userId = 1L;
            long chargeAmount = 1000L;
            int threadCount = 10;

            UserPoint before = userPointTable.selectById(userId);
            long expectedPoint = before.point() + (chargeAmount * threadCount);

            System.out.println("\n=== 동시성 테스트: 동시 충전 ===");
            System.out.println("초기 포인트: " + before.point());
            System.out.println("스레드 수: " + threadCount);
            System.out.println("각 충전 금액: " + chargeAmount);
            System.out.println("예상 최종 포인트: " + expectedPoint);

            //when
            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    pointService.chargePoint(userId, chargeAmount);
                });
                threads[i].start();
            }

            // 모든 스레드가 끝날 때까지 대기
            for (Thread thread : threads) {
                thread.join();
            }

            //then
            UserPoint after = userPointTable.selectById(userId);
            List<PointHistory> histories = pointHistoryTable.selectAllByUserId(userId);

            System.out.println("실제 최종 포인트: " + after.point());
            System.out.println("히스토리 개수: " + histories.size());
            System.out.println("차이: " + (expectedPoint - after.point()) + " 포인트 손실\n");

            // 동시성 문제가 있다면 이 assertion은 실패할 것입니다
            assertThat(after.point()).isEqualTo(expectedPoint);
        }

        @Test
        @DisplayName("동시에 같은 사용자가 포인트 사용 시 잔액이 음수가 되면 안된다")
        public void concurrentUse_ShouldNotResultInNegativeBalance() throws Exception {
            //given
            long userId = 1L;
            userPointTable.insertOrUpdate(userId, 10000L);
            long useAmount = 2000L;
            int threadCount = 10; // 총 20000원 사용 시도 (잔액은 10000원)

            System.out.println("\n=== 동시성 테스트: 동시 사용 (잔액 부족) ===");
            System.out.println("초기 포인트: 10000");
            System.out.println("스레드 수: " + threadCount);
            System.out.println("각 사용 금액: " + useAmount);
            System.out.println("총 사용 시도: " + (useAmount * threadCount));

            //when
            java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger failCount = new java.util.concurrent.atomic.AtomicInteger(0);

            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    try {
                        pointService.usePoint(userId, useAmount);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    }
                });
                threads[i].start();
            }

            // 모든 스레드가 끝날 때까지 대기
            for (Thread thread : threads) {
                thread.join();
            }

            //then
            UserPoint after = userPointTable.selectById(userId);

            System.out.println("성공한 사용 요청: " + successCount.get());
            System.out.println("실패한 사용 요청: " + failCount.get());
            System.out.println("실제 최종 포인트: " + after.point());
            System.out.println("예상: 5개 성공, 5개 실패, 최종 잔액 0원\n");

            // 동시성 문제가 있다면 음수가 되거나, 검증이 제대로 작동하지 않을 것입니다
            assertThat(after.point()).isGreaterThanOrEqualTo(0L);
        }

        @Test
        @DisplayName("동시에 충전과 사용이 발생해도 최종 잔액이 정확해야 한다")
        public void concurrentChargeAndUse_ShouldBeConsistent() throws Exception {
            //given
            long userId = 1L;
            userPointTable.insertOrUpdate(userId, 50000L);
            long chargeAmount = 5000L;
            long useAmount = 3000L;
            int chargeThreadCount = 5;
            int useThreadCount = 5;

            long expectedPoint = 50000L + (chargeAmount * chargeThreadCount) - (useAmount * useThreadCount);

            System.out.println("\n=== 동시성 테스트: 충전과 사용 동시 발생 ===");
            System.out.println("초기 포인트: 50000");
            System.out.println("충전 스레드 수: " + chargeThreadCount + " (각 " + chargeAmount + "원)");
            System.out.println("사용 스레드 수: " + useThreadCount + " (각 " + useAmount + "원)");
            System.out.println("예상 최종 포인트: " + expectedPoint);

            //when
            Thread[] threads = new Thread[chargeThreadCount + useThreadCount];
            int threadIndex = 0;

            // 충전 스레드
            for (int i = 0; i < chargeThreadCount; i++) {
                threads[threadIndex++] = new Thread(() -> {
                    pointService.chargePoint(userId, chargeAmount);
                });
            }

            // 사용 스레드
            for (int i = 0; i < useThreadCount; i++) {
                threads[threadIndex++] = new Thread(() -> {
                    pointService.usePoint(userId, useAmount);
                });
            }

            // 모든 스레드 시작
            for (Thread thread : threads) {
                thread.start();
            }

            // 모든 스레드가 끝날 때까지 대기
            for (Thread thread : threads) {
                thread.join();
            }

            //then
            UserPoint after = userPointTable.selectById(userId);
            List<PointHistory> histories = pointHistoryTable.selectAllByUserId(userId);

            long chargeHistoryCount = histories.stream()
                    .filter(h -> h.type() == TransactionType.CHARGE)
                    .count();
            long useHistoryCount = histories.stream()
                    .filter(h -> h.type() == TransactionType.USE)
                    .count();

            System.out.println("실제 최종 포인트: " + after.point());
            System.out.println("충전 히스토리 개수: " + chargeHistoryCount);
            System.out.println("사용 히스토리 개수: " + useHistoryCount);
            System.out.println("차이: " + (expectedPoint - after.point()) + " 포인트\n");

            // 동시성 문제가 있다면 이 assertion은 실패할 것입니다
            assertThat(after.point()).isEqualTo(expectedPoint);
        }
    }

    @Nested
    @DisplayName("추가 비즈니스 정책 검증")
    class AdditionalPolicyTest {

        @Nested
        @DisplayName("100원 단위 검증")
        class MultipleOf100Test {

            @Test
            @DisplayName("충전 실패: 100원 단위가 아닌 금액")
            public void chargePoint_FailWhen_NotMultipleOf100() throws Exception {
                //given
                long userId = 1L;
                long invalidAmount = 1234L;

                //when&then
                assertThatThrownBy(() -> pointService.chargePoint(userId, invalidAmount))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("금액은 100원 단위로만 가능합니다");
            }

            @Test
            @DisplayName("사용 실패: 100원 단위가 아닌 금액")
            public void usePoint_FailWhen_NotMulitpleOf100() throws Exception {
                //given
                long userId = 1L;
                long invalidAmount = 999L;

                //when & then
                assertThatThrownBy(() -> pointService.usePoint(userId, invalidAmount))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("금액은 100원 단위로만 가능합니다");
            }

            @Test
            @DisplayName("충전 성공: 100원 단위 금액")
            public void charPoint_SuccessWhen_MultipleOf100() throws Exception {
                //given
                long userId = 1L;
                long chargeAmount = 1100L;
                UserPoint before = userPointTable.selectById(userId);

                //when
                UserPoint result = pointService.chargePoint(userId, chargeAmount);

                //then
                assertThat(result.point()).isEqualTo(before.point() + chargeAmount);
            }
        }

        @Nested
        @DisplayName("최소/최대 금액 제한 검증")
        class AmountLimitTest {

            @Test
            @DisplayName("충전 실패: 최소 금액 미만 (1,000원 미만)")
            public void chargePoint_FailWhen_LessThanMinAmount() throws Exception {
                //given
                long userId = 1L;
                long invalidAmount = 900L;

                //when & then
                assertThatThrownBy(() -> pointService.chargePoint(userId, invalidAmount))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("충전 금액은 1,000원 이상이어야 합니다");
            }

            @Test
            @DisplayName("충전 실패: 최대 금액 초과 (1,000,000원 초과)")
            public void chargePoint_FailWhen_ExceedMaxAmount() throws Exception {
                //given
                long userId = 1L;
                long invalidAmount = 1_000_100L;

                //when & then
                assertThatThrownBy(() -> pointService.chargePoint(userId, invalidAmount))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("충전 금액은 1,000,000원 이하여야 합니다");
            }

            @Test
            @DisplayName("사용 실패: 최소 금액 미만 (100원 미만)")
            public void usePoint_FailWhen_LessThanMinAmount() throws Exception {
                //given
                long userId = 1L;
                long invalidAmount = 0L;

                //when & then
                assertThatThrownBy(() -> pointService.usePoint(userId, invalidAmount))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("충전/사용 금액은 0보다 커야합니다");
            }

            @Test
            @DisplayName("사용 실패: 최대 금액 초과 (100,000원 초과)")
            public void usePoint_FailWhen_ExceedMaxAmount() throws Exception {
                //given
                long userId = 1L;
                userPointTable.insertOrUpdate(userId, 200_000L);
                long invalidAmount = 100_100L;

                //when & then
                assertThatThrownBy(() -> pointService.usePoint(userId, invalidAmount))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("사용 금액은 100,000원 이하여야 합니다");
            }

            @Test
            @DisplayName("충전 성공: 최소 금액 (1,000원)")
            public void chargePoint_SuccessWhen_MinAmount() throws Exception {
                //given
                long userId = 1L;
                long chargeAmount = 1_000L;
                UserPoint before = userPointTable.selectById(userId);

                //when
                UserPoint result = pointService.chargePoint(userId, chargeAmount);

                //then
                assertThat(result.point()).isEqualTo(before.point() + chargeAmount);
            }

            @Test
            @DisplayName("충전 성공: 최대 금액 (1,000,000원)")
            public void chargePoint_SuccessWhen_MaxAmount() throws Exception {
                //given
                long userId = 1L;
                long chargeAmount = 1_000_000L;
                UserPoint before = userPointTable.selectById(userId);

                //when
                UserPoint result = pointService.chargePoint(userId, chargeAmount);

                //then
                assertThat(result.point()).isEqualTo(before.point() + chargeAmount);
            }
        }

        @Nested
        @DisplayName("최대 보유 포인트 제한 검증")
        class MaxBalanceTest {

            @Test
            @DisplayName("충전 실패: 충전 후 잔액이 최대 한도(10,000,000원) 초과")
            public void chargePoint_FailWhen_ExceedMaxBalance() throws Exception {
                //given
                long userId = 1L;
                userPointTable.insertOrUpdate(userId, 9_500_000L);
                long chargeAmount = 600_000L; // 충전 후 10,100,000원이 되어 한도 초과

                //when & then
                assertThatThrownBy(() -> pointService.chargePoint(userId, chargeAmount))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("최대 보유 가능 포인트는 10,000,000원입니다");
            }

            @Test
            @DisplayName("충전 성공: 충전 후 잔액이 최대 한도(10,000,000원)와 같음")
            public void chargePoint_SuccessWhen_BalanceEqualsMaxBalance() throws Exception {
                //given
                long userId = 1L;
                userPointTable.insertOrUpdate(userId, 9_000_000L);
                long chargeAmount = 1_000_000L; // 충전 후 정확히 10,000,000원
                UserPoint before = userPointTable.selectById(userId);

                //when
                UserPoint result = pointService.chargePoint(userId, chargeAmount);

                //then
                assertThat(result.point()).isEqualTo(before.point() + chargeAmount);
                assertThat(result.point()).isEqualTo(10_000_000L);
            }
        }
    }

}