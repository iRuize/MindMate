package ruize.mindmateauth.dto;

import lombok.Data;
import ruize.mindmateauth.entity.Quote;

import java.time.LocalDateTime;

/**
 * 经典语录视图对象，不暴露归属用户与逻辑删除标记。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@Data
public class QuoteVO {
    private Long id;
    private String content;
    private String source;
    private Integer mediaType;
    private String mediaUrl;
    private LocalDateTime createdAt;

    public static QuoteVO from(Quote quote) {
        QuoteVO vo = new QuoteVO();
        vo.setId(quote.getId());
        vo.setContent(quote.getContent());
        vo.setSource(quote.getSource());
        vo.setMediaType(quote.getMediaType());
        vo.setMediaUrl(quote.getMediaUrl());
        vo.setCreatedAt(quote.getCreatedAt());
        return vo;
    }
}
