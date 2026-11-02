package ruize.mindmateauth.decision;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ruize.mindmateauth.dto.DecisionPreferenceRequest;
import ruize.mindmateauth.entity.DecisionPreference;
import ruize.mindmateauth.entity.User;
import ruize.mindmateauth.enums.ResultCode;
import ruize.mindmateauth.exception.BizException;
import ruize.mindmateauth.mapper.DecisionPreferenceMapper;
import ruize.mindmateauth.mapper.UserMapper;

import java.time.LocalDateTime;

/**
 * 决策偏好的短事务持久化，使用用户行锁串行化同一用户的保存。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@Service
public class DecisionPreferencePersistenceService {

    private final DecisionPreferenceMapper preferenceMapper;
    private final UserMapper userMapper;

    public DecisionPreferencePersistenceService(DecisionPreferenceMapper preferenceMapper, UserMapper userMapper) {
        this.preferenceMapper = preferenceMapper;
        this.userMapper = userMapper;
    }

    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public DecisionPreference save(Long userId, DecisionPreferenceRequest request) {
        lockUser(userId);
        LocalDateTime now = LocalDateTime.now();
        DecisionPreference existing = preferenceMapper.selectById(userId);
        if (existing == null) {
            DecisionPreference preference = new DecisionPreference()
                    .setUserId(userId)
                    .setPersonality(normalize(request.getPersonality()))
                    .setInterests(normalize(request.getInterests()))
                    .setValues(normalize(request.getValues()))
                    .setGoals(normalize(request.getGoals()))
                    .setDecisionStyle(normalize(request.getDecisionStyle()))
                    .setMemoryEnabled(request.getMemoryEnabled())
                    .setCreatedAt(now)
                    .setUpdatedAt(now);
            if (preferenceMapper.insert(preference) != 1) {
                throw new BizException(ResultCode.DECISION_WRITE_FAILED);
            }
            return preference;
        }

        existing.setPersonality(normalize(request.getPersonality()))
                .setInterests(normalize(request.getInterests()))
                .setValues(normalize(request.getValues()))
                .setGoals(normalize(request.getGoals()))
                .setDecisionStyle(normalize(request.getDecisionStyle()))
                .setMemoryEnabled(request.getMemoryEnabled())
                .setUpdatedAt(now);
        int updated = preferenceMapper.update(null, new LambdaUpdateWrapper<DecisionPreference>()
                .eq(DecisionPreference::getUserId, userId)
                .set(DecisionPreference::getPersonality, existing.getPersonality())
                .set(DecisionPreference::getInterests, existing.getInterests())
                .set(DecisionPreference::getValues, existing.getValues())
                .set(DecisionPreference::getGoals, existing.getGoals())
                .set(DecisionPreference::getDecisionStyle, existing.getDecisionStyle())
                .set(DecisionPreference::getMemoryEnabled, existing.getMemoryEnabled())
                .set(DecisionPreference::getUpdatedAt, now));
        if (updated != 1) {
            throw new BizException(ResultCode.DECISION_WRITE_FAILED);
        }
        return existing;
    }

    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public void clear(Long userId) {
        lockUser(userId);
        int deleted = preferenceMapper.delete(new LambdaQueryWrapper<DecisionPreference>()
                .eq(DecisionPreference::getUserId, userId));
        if (deleted > 1) {
            throw new BizException(ResultCode.DECISION_WRITE_FAILED);
        }
    }

    private User lockUser(Long userId) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .select(User::getUserId)
                .eq(User::getUserId, userId)
                .last("FOR UPDATE"));
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
