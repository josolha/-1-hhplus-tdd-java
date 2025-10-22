package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }


    public UserPoint getUserPoint(long userId) {
        return null;
    }

    public List<PointHistory> getUserPointHistory(long userId){
        return null;
    }

    public UserPoint chargePoint(long id, long amount){
        return null;
    }

    public UserPoint usePoint(long id, long amount){
        return null;
    }
}
