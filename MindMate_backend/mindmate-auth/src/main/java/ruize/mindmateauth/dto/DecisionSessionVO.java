package ruize.mindmateauth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 决策会话响应。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@Data
@AllArgsConstructor
public class DecisionSessionVO {
    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
