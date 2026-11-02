package ruize.mindmateauth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 发送决策消息请求。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@Data
public class DecisionMessageRequest {
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 4000, message = "消息内容不能超过4000个字符")
    private String content;
}
