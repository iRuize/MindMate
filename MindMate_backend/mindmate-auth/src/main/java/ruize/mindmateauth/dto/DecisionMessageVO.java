package ruize.mindmateauth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 决策消息响应。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@Data
@AllArgsConstructor
public class DecisionMessageVO {
    private Long id;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
