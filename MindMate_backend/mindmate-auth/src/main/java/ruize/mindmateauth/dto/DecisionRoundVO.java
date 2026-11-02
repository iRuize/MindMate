package ruize.mindmateauth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 一轮决策消息响应。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@Data
@AllArgsConstructor
public class DecisionRoundVO {
    private DecisionMessageVO userMessage;
    private DecisionMessageVO assistantMessage;
}
