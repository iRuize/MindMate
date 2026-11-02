package ruize.mindmateauth.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 决策偏好全量保存请求。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@Data
public class DecisionPreferenceRequest {
    @Size(max = 1000, message = "人格偏好不能超过1000个字符")
    private String personality;

    @Size(max = 1000, message = "兴趣偏好不能超过1000个字符")
    private String interests;

    @Size(max = 1000, message = "价值观偏好不能超过1000个字符")
    private String values;

    @Size(max = 1000, message = "目标偏好不能超过1000个字符")
    private String goals;

    @Size(max = 1000, message = "决策风格不能超过1000个字符")
    private String decisionStyle;

    @NotNull(message = "memoryEnabled不能为空")
    private Boolean memoryEnabled;
}
