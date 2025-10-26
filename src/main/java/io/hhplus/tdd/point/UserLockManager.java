package io.hhplus.tdd.point;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 사용자별 Lock을 관리하는 클래스
 * ConcurrentHashMap과 ReentrantLock을 사용하여 사용자별 동시성 제어
 */
@Component
public class UserLockManager {

    private final ConcurrentHashMap<Long, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    /**
     * 사용자 ID에 해당하는 Lock을 반환
     * 없으면 새로 생성하여 반환 (thread-safe)
     */
    public ReentrantLock getLock(long userId) {
        return lockMap.computeIfAbsent(userId, id -> new ReentrantLock());
    }
}
