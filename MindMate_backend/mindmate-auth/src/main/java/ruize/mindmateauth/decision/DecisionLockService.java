package ruize.mindmateauth.decision;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import ruize.mindmateauth.constant.RedisKeys;
import ruize.mindmateauth.enums.ResultCode;
import ruize.mindmateauth.exception.BizException;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * 决策会话分布式锁，避免模型外部调用期间与删除操作并发。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@Service
public class DecisionLockService {

    private static final Duration LOCK_TTL = Duration.ofSeconds(180);
    private static final RedisScript<Long> RELEASE_SCRIPT = RedisScript.of(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end", Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    public DecisionLockService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public LockHandle acquire(Long userId, Long sessionId) {
        String key = RedisKeys.decisionSessionLock(userId, sessionId);
        String token = UUID.randomUUID().toString();
        try {
            Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(key, token, LOCK_TTL);
            if (!Boolean.TRUE.equals(acquired)) {
                throw new BizException(ResultCode.DECISION_LOCKED);
            }
            return new LockHandle(key, token);
        } catch (BizException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BizException(ResultCode.DECISION_LOCKED, "决策会话暂时无法锁定，请稍后再试");
        }
    }

    public void release(LockHandle lock) {
        try {
            stringRedisTemplate.execute(RELEASE_SCRIPT, Collections.singletonList(lock.key()), lock.token());
        } catch (RuntimeException ignored) {
            // 释放失败时由TTL兜底，不能覆盖模型或数据库原始结果。
        }
    }

    public record LockHandle(String key, String token) {
    }
}
