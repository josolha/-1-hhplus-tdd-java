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
    private static final long MAX_BALANCE = 10_000_000L;

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }


    public synchronized UserPoint getUserPoint(long userId) {
        return userPointTable.selectById(userId);
    }

    public synchronized List<PointHistory> getUserPointHistory(long userId){
        return pointHistoryTable.selectAllByUserId(userId);
    }

    public synchronized UserPoint chargePoint(long id, long amount){
        return updatePoint(id, amount, TransactionType.CHARGE);
    }

    public synchronized UserPoint usePoint(long id, long amount){
        return updatePoint(id, amount, TransactionType.USE);
    }

    private UserPoint updatePoint(long id, long amount, TransactionType type) {
        // 1. 금액 검증
        validateAmount(amount, type);

        // 2. 현재 포인트 조회
        UserPoint current = userPointTable.selectById(id);

        // 3. 포인트 연산 가능 여부 검증 (USE일 때만 잔액 체크)
        validateBalance(current.point(), amount, type);

        // 4. 새로운 포인트 계산
        long newPoint = (type == TransactionType.CHARGE)
                ? current.point() + amount
                : current.point() - amount;

        // 5. 최대 잔액 검증 (CHARGE일 때만)
        if (type == TransactionType.CHARGE) {
            validateMaxBalance(newPoint);
        }

        // 6. History에 내역 기록
        pointHistoryTable.insert(id, amount, type, System.currentTimeMillis());

        // 7. 포인트 업데이트 및 반환
        return userPointTable.insertOrUpdate(id, newPoint);
    }

    //목적 : 충전/사용 금액이 유효한지 검증
    private void validateAmount(long amount, TransactionType type){
        validateBasicAmount(amount);
        validateAmountLimit(amount, type);
    }

    //목적 : 기본 금액 검증 (0보다 크고, 100원 단위)
    private void validateBasicAmount(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전/사용 금액은 0보다 커야합니다");
        }
        if (amount % POINT_UNIT != 0) {
            throw new IllegalArgumentException("금액은 100원 단위로만 가능합니다");
        }
    }

    //목적 : 충전/사용 타입별 금액 제한 검증
    private void validateAmountLimit(long amount, TransactionType type) {
        if (type == TransactionType.CHARGE) {
            validateChargeLimit(amount);
        } else {
            validateUseLimit(amount);
        }
    }

    //목적 : 충전 금액 제한 검증 (1,000 ~ 1,000,000)
    private void validateChargeLimit(long amount) {
        if (amount < MIN_CHARGE_AMOUNT) {
            throw new IllegalArgumentException("충전 금액은 1,000원 이상이어야 합니다");
        }
        if (amount > MAX_CHARGE_AMOUNT) {
            throw new IllegalArgumentException("충전 금액은 1,000,000원 이하여야 합니다");
        }
    }

    //목적 : 사용 금액 제한 검증 (최대 100,000)
    private void validateUseLimit(long amount) {
        if (amount > MAX_USE_AMOUNT) {
            throw new IllegalArgumentException("사용 금액은 100,000원 이하여야 합니다");
        }
    }

    //목적 : 잔고 부족시 에러 발생
    private void validateBalance(long currentPoint, long amount, TransactionType type){
        if (type == TransactionType.USE && currentPoint < amount){
            throw new IllegalArgumentException("잔고가 부족합니다");
        }
    }

    //목적 : 최대 보유 포인트 제한 검증
    private void validateMaxBalance(long newPoint) {
        if (newPoint > MAX_BALANCE) {
            throw new IllegalArgumentException("최대 보유 가능 포인트는 10,000,000원입니다");
        }
    }
}
