package ruize.mindmateauth.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 创建决策会话请求。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@Data
public class DecisionCreateRequest {
    @Size(max = 100, message = "决策标题不能超过100个字符")
    private String title;
}
