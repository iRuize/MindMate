package ruize.mindmateauth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户实体。
 * 表名用 SYS_USER 而非 user：USER 是达梦保留字，未加引号不能作为表名，
 * 而 MyBatis-Plus 生成的 SQL 不会给表名加引号。
 *
 * @author 邸睿泽
 * @since 2026-10-27
 */
// 使用链式setters
@Accessors(chain = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("SYS_USER")
public class User {
    @TableId(value = "USER_ID", type = IdType.AUTO)
    private Long userId;

    private String phoneNumber;
    private String username;
    private String avatarUrl;
    private String personalizedSignature;
    private String email;

    /** 创建时间由数据库 DEFAULT CURRENT_TIMESTAMP 生成，插入时留空即可 */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** 最近登录时间，登录成功时由服务层写入 */
    private LocalDateTime lastLoginAt;
}
