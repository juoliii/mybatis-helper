package com.bitian.db.mybatis_helper.tk;

import org.apache.ibatis.annotations.SelectProvider;
import tk.mybatis.mapper.annotation.RegisterMapper;

import java.util.List;

/**
 * 继承 tk.mybatis.mapper.common.Mapper 之外，额外提供支持 TkQueryWrapper 的扩展查询接口
 */
@RegisterMapper
public interface SelectByQueryMapper<T> {

    /**
     * 根据 TkQueryWrapper 的动态条件查询多条实体对象
     *
     * @param queryWrapper 查询条件包装器
     * @return 实体列表
     */
    @SelectProvider(type = SelectByQueryProvider.class, method = "dynamicSQL")
    List<T> selectByQuery(TkQueryWrapper<T> queryWrapper);

}
