package ruize.mindmateauth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 经典语录。媒体文件落在本地磁盘，表内只保存公开访问路径。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@Data
@Accessors(chain = true)
@TableName("SYS_QUOTE")
public class Quote {
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String content;
    private String source;
    private Integer mediaType;
    private String mediaUrl;
    private LocalDateTime createdAt;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
