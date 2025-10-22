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
        return userPointTable.selectById(userId);
    }

    public List<PointHistory> getUserPointHistory(long userId){
        return pointHistoryTable.selectAllByUserId(userId);
    }

    public UserPoint chargePoint(long id, long amount){
        //1.현재 포인트 조회
        UserPoint current = userPointTable.selectById(id);

        //2.새로운 포인트 = 기존 포인트  + 충전 금액
        long newPoint = current.point() + amount;

        //3.History에 충전 내역 기록
        pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

        return userPointTable.insertOrUpdate(id,newPoint);
    }

    public UserPoint usePoint(long id, long amount){

        //1.현재 포인트 조회
        UserPoint current = userPointTable.selectById(id);

        //2.잔액이 부족할 결우 포인트 사용은 실패
        if(current.point()<amount){
            throw new IllegalArgumentException("잔고가 부족합니다");
        }

        //3.새로운 포인트 = 기존 포인트 - 사용금액
        long newPoint = current.point() - amount;

        //4.History에 충전 내역 기록
        pointHistoryTable.insert(id, amount, TransactionType.USE, System.currentTimeMillis());

        return userPointTable.insertOrUpdate(id,newPoint);
    }
}
