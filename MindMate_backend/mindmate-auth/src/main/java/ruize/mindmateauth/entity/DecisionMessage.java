package ruize.mindmateauth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 决策会话消息。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@Data
@Accessors(chain = true)
@TableName(value = "DECISION_MESSAGE", autoResultMap = true)
public class DecisionMessage {
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    private Long sessionId;
    private Long userId;
    @TableField("\"ROLE\"")
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
