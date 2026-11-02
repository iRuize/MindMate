package ruize.mindmateauth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 决策偏好响应。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@Data
@AllArgsConstructor
public class DecisionPreferenceVO {
    private String personality;
    private String interests;
    private String values;
    private String goals;
    private String decisionStyle;
    private boolean memoryEnabled;
}
