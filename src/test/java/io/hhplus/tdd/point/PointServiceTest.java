package io.hhplus.tdd.point;


import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;


public class PointServiceTest {

    private PointService pointService;

    @BeforeEach
    void setUp(){
        pointService = new PointService();
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

        //when
        UserPoint result = pointService.chargePoint(userId,chargeAmount);
        
        //then
        assertThat(result.point()).isEqualTo(chargeAmount);
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
        pointService.chargePoint(userId, 1000L);

        //when
        UserPoint result = pointService.usePoint(userId, 500L);

        //then
        assertThat(result.point()).isEqualTo(500L);
    }

    @Test
    @DisplayName("잔고가 부족할 경우 포인트 사용은 실패한다")
    public void usePoint_FailsWhenInsufficientBalance() throws Exception{
        //given
        long userId = 1L;
        pointService.chargePoint(userId, 500L);

        //when & then
        assertThatThrownBy(() -> pointService.usePoint(userId, 1000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잔고가 부족합니다");
    }

}