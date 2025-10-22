package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public DataInitializer(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    @PostConstruct
    public void init() {
        System.out.println("더미 데이터 초기화 시작...");

        // 사용자 1: 5000 포인트 보유
        userPointTable.insertOrUpdate(1L, 5000L);
        pointHistoryTable.insert(1L, 5000L, TransactionType.CHARGE, System.currentTimeMillis());

        // 사용자 2: 10000 포인트 보유, 충전 및 사용 내역 있음
        userPointTable.insertOrUpdate(2L, 10000L);
        pointHistoryTable.insert(2L, 15000L, TransactionType.CHARGE, System.currentTimeMillis());
        pointHistoryTable.insert(2L, 5000L, TransactionType.USE, System.currentTimeMillis());

        // 사용자 3: 0 포인트 (빈 계정)
        userPointTable.insertOrUpdate(3L, 0L);

        System.out.println("더미 데이터 초기화 완료!");
        System.out.println("- 사용자 1: 5000 포인트");
        System.out.println("- 사용자 2: 10000 포인트 (충전/사용 이력 포함)");
        System.out.println("- 사용자 3: 0 포인트");
    }
}
