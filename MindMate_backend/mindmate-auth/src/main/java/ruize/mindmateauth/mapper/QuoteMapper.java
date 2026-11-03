package ruize.mindmateauth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import ruize.mindmateauth.entity.Quote;

/**
 * 经典语录访问，逻辑删除由实体的 @TableLogic 自动附加。
 *
 * @author 邸睿泽
 * @since 2026-11-02
 */
@Mapper
public interface QuoteMapper extends BaseMapper<Quote> {
}
