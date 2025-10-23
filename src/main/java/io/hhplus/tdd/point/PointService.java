package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    private static final long POINT_UNIT = 100L;
    private static final long MIN_CHARGE_AMOUNT = 1_000L;
    private static final long MAX_CHARGE_AMOUNT = 1_000_000L;
    private static final long MAX_USE_AMOUNT = 100_000L;

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
        return updatePoint(id, amount, TransactionType.CHARGE);
    }

    public UserPoint usePoint(long id, long amount){
        return updatePoint(id, amount, TransactionType.USE);
    }

    private UserPoint updatePoint(long id, long amount, TransactionType type) {
        // 1. 금액 검증
        validateAmount(amount, type);

        // 2. 현재 포인트 조회
        UserPoint current = userPointTable.selectById(id);

        // 3. 포인트 연산 가능 여부 검증 (USE일 때만 잔액 체크)
        validatePoint(current.point(), amount, type);

        // 4. 새로운 포인트 계산
        long newPoint = (type == TransactionType.CHARGE)
                ? current.point() + amount
                : current.point() - amount;

        // 5. History에 내역 기록
        pointHistoryTable.insert(id, amount, type, System.currentTimeMillis());

        // 6. 포인트 업데이트 및 반환
        return userPointTable.insertOrUpdate(id, newPoint);
    }

    //목적 : 충전/사용 금액이 유효한지 검증
    private void validateAmount(long amount, TransactionType type){
        if(amount <= 0){
            throw new IllegalArgumentException("충전/사용 금액은 0보다 커야합니다");
        }
        if(amount % POINT_UNIT !=0){
            throw new IllegalArgumentException("금액은 100원 단위로만 가능합니다");
        }

        // 충전 금액 제한 검증
        if (type == TransactionType.CHARGE) {
            if (amount < MIN_CHARGE_AMOUNT) {
                throw new IllegalArgumentException("충전 금액은 1,000원 이상이어야 합니다");
            }
            if (amount > MAX_CHARGE_AMOUNT) {
                throw new IllegalArgumentException("충전 금액은 1,000,000원 이하여야 합니다");
            }
        }

        // 사용 금액 제한 검증
        if (type == TransactionType.USE) {
            if (amount > MAX_USE_AMOUNT) {
                throw new IllegalArgumentException("사용 금액은 100,000원 이하여야 합니다");
            }
        }
    }

    //목적 : 잔고 부족시 에러 발생
    private void validatePoint(long currentPoint, long amount, TransactionType type){
        if (type == TransactionType.USE && currentPoint < amount){
            throw new IllegalArgumentException("잔고가 부족합니다");
        }
    }
}
