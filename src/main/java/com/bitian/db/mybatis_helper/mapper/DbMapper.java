package com.bitian.db.mybatis_helper.mapper;

import com.bitian.db.mybatis_helper.provider.DbProvider;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;

import java.util.List;
import java.util.Map;

public interface DbMapper {
    @SelectProvider(type = DbProvider.class, method = "executeSql")
    List<Map<String, Object>> selectList(Map<String, Object> param);

    @SelectProvider(type = DbProvider.class, method = "executeSql")
    Map<String, Object> selectOne(Map<String, Object> param);

    @InsertProvider(type = DbProvider.class, method = "executeSql")
    int insert(Map<String, Object> param);

    @UpdateProvider(type = DbProvider.class, method = "executeSql")
    int update(Map<String, Object> param);

    @DeleteProvider(type = DbProvider.class, method = "executeSql")
    int delete(Map<String, Object> param);
}
