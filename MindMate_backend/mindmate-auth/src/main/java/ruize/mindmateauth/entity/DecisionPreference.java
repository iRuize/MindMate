package ruize.mindmateauth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 当前用户的决策偏好。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@Data
@Accessors(chain = true)
@TableName(value = "DECISION_PREFERENCE", autoResultMap = true)
public class DecisionPreference {
    @TableId(value = "USER_ID", type = IdType.INPUT)
    private Long userId;

    private String personality;
    private String interests;
    @TableField("\"VALUES\"")
    private String values;
    private String goals;
    private String decisionStyle;
    private Boolean memoryEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
