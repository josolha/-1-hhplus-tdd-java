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
        pointService.chargePoint(userId, 500L);

        //when & then
        assertThatThrownBy(() -> pointService.usePoint(userId, 1000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잔고가 부족합니다");
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
    }

}