package ruize.mindmateauth.decision;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ruize.mindmateauth.dto.DecisionMessageVO;
import ruize.mindmateauth.dto.DecisionRoundVO;
import ruize.mindmateauth.entity.DecisionMessage;
import ruize.mindmateauth.entity.DecisionSession;
import ruize.mindmateauth.enums.ResultCode;
import ruize.mindmateauth.exception.BizException;
import ruize.mindmateauth.mapper.DecisionMessageMapper;
import ruize.mindmateauth.mapper.DecisionSessionMapper;

import java.time.LocalDateTime;

/**
 * 模型成功后的本轮消息短事务持久化。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@Service
public class DecisionRoundPersistenceService {

    private final DecisionSessionMapper sessionMapper;
    private final DecisionMessageMapper messageMapper;

    public DecisionRoundPersistenceService(DecisionSessionMapper sessionMapper, DecisionMessageMapper messageMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
    }

    @Transactional(rollbackFor = Exception.class, timeout = 10)
    public DecisionRoundVO saveRound(Long userId, Long sessionId, String userContent, String assistantContent) {
        DecisionSession session = sessionMapper.selectOne(new LambdaQueryWrapper<DecisionSession>()
                .eq(DecisionSession::getId, sessionId)
                .eq(DecisionSession::getUserId, userId)
                .last("FOR UPDATE"));
        if (session == null) {
            throw new BizException(ResultCode.DECISION_SESSION_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        DecisionMessage userMessage = new DecisionMessage()
                .setSessionId(sessionId)
                .setUserId(userId)
                .setRole("user")
                .setContent(userContent)
                .setCreatedAt(now);
        DecisionMessage assistantMessage = new DecisionMessage()
                .setSessionId(sessionId)
                .setUserId(userId)
                .setRole("assistant")
                .setContent(assistantContent)
                .setCreatedAt(now);
        if (messageMapper.insert(userMessage) != 1 || messageMapper.insert(assistantMessage) != 1) {
            throw new BizException(ResultCode.DECISION_WRITE_FAILED);
        }

        int updated = sessionMapper.update(null, new LambdaUpdateWrapper<DecisionSession>()
                .eq(DecisionSession::getId, sessionId)
                .eq(DecisionSession::getUserId, userId)
                .set(DecisionSession::getUpdatedAt, now));
        if (updated != 1) {
            throw new BizException(ResultCode.DECISION_SESSION_NOT_FOUND);
        }
        return new DecisionRoundVO(toVO(userMessage), toVO(assistantMessage));
    }

    private DecisionMessageVO toVO(DecisionMessage message) {
        return new DecisionMessageVO(message.getId(), message.getRole(), message.getContent(), message.getCreatedAt());
    }
}
