package ruize.mindmateauth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 决策会话。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@Data
@Accessors(chain = true)
@TableName("DECISION_SESSION")
public class DecisionSession {
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
