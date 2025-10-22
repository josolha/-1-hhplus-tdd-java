package io.hhplus.tdd.point;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(PointController.class)
class PointControllerTest {

    //MockMvc는 실제 웹 요청을 보내지 않고, Controller의 HTTP 요청/응답을 테스트할 수 있도록 해준다.
    @Autowired
    MockMvc mockMvc;

    //@MockBean은 Controller가 의존하는 서비스나 리포지토리의 mock 객체를 생성하여 실제 구현 없이 테스트할 수 있게 도와준다.
    @MockBean //가짜 객체 (Mock)
    private PointService pointService;


    /*
      주로 사용하는 것들:
        MockMvcResultMatchers.status()      // HTTP 상태 코드 검증
        MockMvcResultMatchers.jsonPath()    // JSON 응답 검증
        MockMvcResultMatchers.content()     // 응답 본문 검증
        MockMvcResultMatchers.header()      // 헤더 검증
     */

    @Test
    @DisplayName("GET /point/{id} - 사용자 포인트 조회 성공")
    public void getPoint() throws Exception {
        //given : Mock 동작정의
        when(pointService.getUserPoint(1L))
                .thenReturn(new UserPoint(1L, 5000, System.currentTimeMillis()));

        //when & then
        mockMvc.perform(get("/point/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(5000));
    }


    @Test
    @DisplayName("GET /point/{id}/histories - 사용자 히스토리 조회 성공")
    public void getHistory() throws Exception {
        // given
        long userId = 1L;
        when(pointService.getUserPointHistory(1L))
                .thenReturn(List.of(
                        new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, System.currentTimeMillis()),
                        new PointHistory(2L, userId, 1500L, TransactionType.USE, System.currentTimeMillis()),
                        new PointHistory(3L, userId, 2000L, TransactionType.USE, System.currentTimeMillis())
                ));

        // when & then
        mockMvc.perform(get("/point/1/histories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].amount").value(1000))
                .andExpect(jsonPath("$[0].type").value("CHARGE"))
                .andExpect(jsonPath("$[1].amount").value(1500))
                .andExpect(jsonPath("$[2].amount").value(2000))
                .andExpect(jsonPath("$[2].type").value("USE"));

    }
    @Test
    @DisplayName("PATCH /point/{id}/charge - 사용자 포인트 충전 성공")
    public void chargePoint() throws Exception {
        // given
        long userId = 1L;
        long currentPoint = 1000L;
        long chargeAmount = 500L;
        long expectedPoint = currentPoint + chargeAmount;  // 1500

        when(pointService.chargePoint(userId, chargeAmount))
                .thenReturn(new UserPoint(userId, expectedPoint, System.currentTimeMillis()));

        // when & then
        mockMvc.perform(
                        patch("/point/{id}/charge", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.valueOf(chargeAmount))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(expectedPoint));
    }

    @Test
    @DisplayName("PATCH /point/{id}/use - 사용자 포인트 사용 성공")
    public void usePoint() throws Exception {
        // given
        long userId = 1L;
        long currentPoint = 1000L;
        long useAmount = 300L;
        long expectedPoint = currentPoint - useAmount;  // 700

        when(pointService.usePoint(userId, useAmount))
                .thenReturn(new UserPoint(userId, expectedPoint, System.currentTimeMillis()));

        // when & then
        mockMvc.perform(
                        patch("/point/{id}/use", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.valueOf(useAmount))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(expectedPoint));
    }
}