package ruize.mindmateauth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import ruize.mindmateauth.entity.TodoTask;

/**
 * 任务快照访问。
 *
 * @author 邸睿泽
 * @since 2026-10-30
 */
@Mapper
public interface TaskMapper extends BaseMapper<TodoTask> {
}
