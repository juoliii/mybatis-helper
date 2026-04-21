package com.bitian.db.mybatis_helper.util;

import com.bitian.db.mybatis_helper.dialect.DialectFactory;
import com.bitian.db.mybatis_helper.dialect.DialectRegistry;
import com.bitian.db.mybatis_helper.mapper.DbMapper;
import com.bitian.db.mybatis_helper.meta.PageResult;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DbUtil {

    private static SqlSessionFactory sqlSessionFactory;

    private static boolean isInSpring=false;

    public static boolean isSpringPresent() {
        try {
            Class.forName("org.springframework.context.ApplicationContext");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static void init(SqlSessionFactory factory) {
        sqlSessionFactory = factory;
        initConfiguration(factory.getConfiguration());
        isInSpring=isSpringPresent();
    }


    private static void initConfiguration(Configuration configuration) {
        if (!configuration.hasMapper(DbMapper.class)) {
            configuration.addMapper(DbMapper.class);
        }
        // 确保开启了数据库下划线到实体类驼峰命名的自动映射
        if (!configuration.isMapUnderscoreToCamelCase()) {
            configuration.setMapUnderscoreToCamelCase(true);
        }
    }

    /**
     * 向后兼容暴露 SqlSession。如果在 Spring 环境下，则返回自动参与事务的 SqlSessionTemplate
     */
    public static SqlSession getSqlSession() {
        if (sqlSessionFactory != null ) {
            if(isInSpring)
                return SqlSessionUtils.getSqlSession(
                        sqlSessionFactory,
                        sqlSessionFactory.getConfiguration().getDefaultExecutorType(),
                        null
                );
            else
                return sqlSessionFactory.openSession(true);
        }
        throw new RuntimeException("DbUtil is not initialized. Please call init(SqlSessionFactory) or init(SqlSession) first.");
    }

    /**
     * 统一执行器：支持 Spring 事务与非 Spring 环境下的资源自动释放
     */
    private static <R> R execute(Function<SqlSession, R> action) {
        if (sqlSessionFactory != null) {
            SqlSession session=getSqlSession();
            try {
                return action.apply(session);
            }finally {
                if(isInSpring)
                    SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);
                else
                    session.close();
            }
        }
        throw new RuntimeException("DbUtil is not initialized. Please call init(SqlSessionFactory) or init(SqlSession) first.");
    }

    private static Map<String, Object> buildParam(String sql, Map<String, Object> params) {
        Map<String, Object> map = new HashMap<>();
        if (params != null) {
            map.putAll(params);
        }
        map.put("_dbutil_sql", sql);
        return map;
    }

    /**
     * 动态创建并注册针对特定实体类的 MappedStatement
     */
    private static String ensureStatement(Configuration configuration, Class<?> resultType) {
        String msId = "DbUtil_dynamic_select_" + resultType.getName();
        if (configuration.hasStatement(msId, false)) {
            return msId;
        }
        synchronized (configuration) {
            if (configuration.hasStatement(msId, false)) {
                return msId;
            }
            LanguageDriver languageDriver = configuration.getDefaultScriptingLanguageInstance();
            // 利用 MyBatis 的 script 标签解析 SQL，保留 #{} 的处理能力
            SqlSource sqlSource = languageDriver.createSqlSource(configuration, "<script>${_dbutil_sql}</script>", Map.class);
            ResultMap resultMap = new ResultMap.Builder(configuration, msId + "-Inline", resultType, new ArrayList<>(), true).build();
            MappedStatement ms = new MappedStatement.Builder(configuration, msId, sqlSource, SqlCommandType.SELECT)
                    .resultMaps(Collections.singletonList(resultMap))
                    .build();
            configuration.addMappedStatement(ms);
            return msId;
        }
    }

    // --- Entity 查询支持 ---
    public static <T> List<T> selectList(String sql, Map<String, Object> params, Class<T> clazz) {
        return execute(session -> {
            String msId = ensureStatement(session.getConfiguration(), clazz);
            return session.selectList(msId, buildParam(sql, params));
        });
    }

    public static <T> List<T> selectList(String sql, Class<T> clazz) {
        return selectList(sql, null, clazz);
    }

    public static <T> T selectOne(String sql, Map<String, Object> params, Class<T> clazz) {
        return execute(session -> {
            String msId = ensureStatement(session.getConfiguration(), clazz);
            return session.selectOne(msId, buildParam(sql, params));
        });
    }

    public static <T> T selectOne(String sql, Class<T> clazz) {
        return selectOne(sql, null, clazz);
    }

    // --- Map 查询支持 ---
    public static List<Map<String, Object>> selectList(String sql, Map<String, Object> params) {
        return execute(session -> {
            DbMapper mapper = session.getMapper(DbMapper.class);
            return mapper.selectList(buildParam(sql, params));
        });
    }

    public static List<Map<String, Object>> selectList(String sql) {
        return selectList(sql, (Map<String, Object>) null);
    }

    public static Map<String, Object> selectOne(String sql, Map<String, Object> params) {
        return execute(session -> {
            DbMapper mapper = session.getMapper(DbMapper.class);
            return mapper.selectOne(buildParam(sql, params));
        });
    }

    public static Map<String, Object> selectOne(String sql) {
        return selectOne(sql, (Map<String, Object>) null);
    }

    // --- QueryWrapper 查询支持 ---
    public static <T> List<T> selectByQuery(String baseSql, QueryWrapper wrapper, Class<T> clazz) {
        String finalSql = baseSql + wrapper.getSqlSegment();
        return selectList(finalSql, wrapper.getParams(), clazz);
    }

    public static <T> T selectOneByQuery(String baseSql, QueryWrapper wrapper, Class<T> clazz) {
        String finalSql = baseSql + wrapper.getSqlSegment();
        return selectOne(finalSql, wrapper.getParams(), clazz);
    }

    public static List<Map<String, Object>> selectByQuery(String baseSql, QueryWrapper wrapper) {
        String finalSql = baseSql + wrapper.getSqlSegment();
        return selectList(finalSql, wrapper.getParams());
    }

    public static Map<String, Object> selectOneByQuery(String baseSql, QueryWrapper wrapper) {
        String finalSql = baseSql + wrapper.getSqlSegment();
        return selectOne(finalSql, wrapper.getParams());
    }

    // --- SelectBuilder 完整 DSL 查询 ---
    public static <T> List<T> selectList(SelectBuilder builder, Class<T> clazz) {
        return selectList(builder.toSql(), builder.getParams(), clazz);
    }

    public static <T> T selectOne(SelectBuilder builder, Class<T> clazz) {
        return selectOne(builder.toSql(), builder.getParams(), clazz);
    }

    public static List<Map<String, Object>> selectList(SelectBuilder builder) {
        return selectList(builder.toSql(), builder.getParams());
    }

    public static Map<String, Object> selectOne(SelectBuilder builder) {
        return selectOne(builder.toSql(), builder.getParams());
    }

    // ============================
    // 分页查询支持
    // ============================

    /**
     * 获取当前数据库方言
     */
    private static DialectFactory getDialect() {
        return DialectRegistry.getDialect(sqlSessionFactory);
    }

    /**
     * 查询总记录数（内部方法）
     */
    private static long selectCount(String sql, Map<String, Object> params) {
        String countSql = getDialect().buildCountSql(sql);
        Map<String, Object> result = selectOne(countSql, params);
        if (result == null || result.isEmpty()) {
            return 0;
        }
        // COUNT(*) 的结果列名因数据库不同而异，取第一个值即可
        Object countValue = result.values().iterator().next();
        return countValue instanceof Number ? ((Number) countValue).longValue() : 0;
    }

    // --- 分页查询：SQL + params + Entity ---

    /**
     * 分页查询，返回实体类列表。
     *
     * @param sql        原始 SQL
     * @param params     查询参数（可为 null）
     * @param clazz      实体类类型
     * @param pageNumber 页码（从 1 开始）
     * @param pageSize   每页条数
     * @param <T>        实体类型
     * @return 分页结果
     */
    public static <T> PageResult<T> selectPage(String sql, Map<String, Object> params,
                                               Class<T> clazz, int pageNumber, int pageSize) {
        long total = selectCount(sql, params);
        if (total == 0) {
            return new PageResult<>(Collections.<T>emptyList(), 0, pageNumber, pageSize);
        }
        int offset = (pageNumber - 1) * pageSize;
        String pageSql = getDialect().buildPaginationSql(sql, offset, pageSize);
        List<T> records = selectList(pageSql, params, clazz);
        return new PageResult<>(records, total, pageNumber, pageSize);
    }

    /**
     * 分页查询，返回实体类列表（无参数版本）。
     */
    public static <T> PageResult<T> selectPage(String sql, Class<T> clazz,
                                                int pageNumber, int pageSize) {
        return selectPage(sql, null, clazz, pageNumber, pageSize);
    }

    // --- 分页查询：SQL + params + Map ---

    /**
     * 分页查询，返回 Map 列表。
     *
     * @param sql        原始 SQL
     * @param params     查询参数（可为 null）
     * @param pageNumber 页码（从 1 开始）
     * @param pageSize   每页条数
     * @return 分页结果
     */
    public static PageResult<Map<String, Object>> selectPage(String sql, Map<String, Object> params,
                                                              int pageNumber, int pageSize) {
        long total = selectCount(sql, params);
        if (total == 0) {
            return new PageResult<>(Collections.<Map<String, Object>>emptyList(), 0, pageNumber, pageSize);
        }
        int offset = (pageNumber - 1) * pageSize;
        String pageSql = getDialect().buildPaginationSql(sql, offset, pageSize);
        List<Map<String, Object>> records = selectList(pageSql, params);
        return new PageResult<>(records, total, pageNumber, pageSize);
    }

    /**
     * 分页查询，返回 Map 列表（无参数版本）。
     */
    public static PageResult<Map<String, Object>> selectPage(String sql,
                                                              int pageNumber, int pageSize) {
        return selectPage(sql, (Map<String, Object>) null, pageNumber, pageSize);
    }

    // --- 分页查询：SelectBuilder ---

    /**
     * 使用 SelectBuilder 分页查询，返回实体类列表。
     */
    public static <T> PageResult<T> selectPage(SelectBuilder builder, Class<T> clazz,
                                                int pageNumber, int pageSize) {
        return selectPage(builder.toSql(), builder.getParams(), clazz, pageNumber, pageSize);
    }

    /**
     * 使用 SelectBuilder 分页查询，返回 Map 列表。
     */
    public static PageResult<Map<String, Object>> selectPage(SelectBuilder builder,
                                                              int pageNumber, int pageSize) {
        return selectPage(builder.toSql(), builder.getParams(), pageNumber, pageSize);
    }

    // --- 分页查询：QueryWrapper ---

    /**
     * 使用 QueryWrapper 分页查询，返回实体类列表。
     */
    public static <T> PageResult<T> selectPageByQuery(String baseSql, QueryWrapper wrapper,
                                                       Class<T> clazz, int pageNumber, int pageSize) {
        String finalSql = baseSql + wrapper.getSqlSegment();
        return selectPage(finalSql, wrapper.getParams(), clazz, pageNumber, pageSize);
    }

    /**
     * 使用 QueryWrapper 分页查询，返回 Map 列表。
     */
    public static PageResult<Map<String, Object>> selectPageByQuery(String baseSql, QueryWrapper wrapper,
                                                                     int pageNumber, int pageSize) {
        String finalSql = baseSql + wrapper.getSqlSegment();
        return selectPage(finalSql, wrapper.getParams(), pageNumber, pageSize);
    }

    // --- 更新/插入/删除 ---
    public static int insert(String sql, Map<String, Object> params) {
        return execute(session -> {
            DbMapper mapper = session.getMapper(DbMapper.class);
            return mapper.insert(buildParam(sql, params));
        });
    }

    public static int insert(String sql) {
        return insert(sql, null);
    }

    public static int update(String sql, Map<String, Object> params) {
        return execute(session -> {
            DbMapper mapper = session.getMapper(DbMapper.class);
            return mapper.update(buildParam(sql, params));
        });
    }

    public static int update(String sql) {
        return update(sql, null);
    }

    public static int delete(String sql, Map<String, Object> params) {
        return execute(session -> {
            DbMapper mapper = session.getMapper(DbMapper.class);
            return mapper.delete(buildParam(sql, params));
        });
    }

    public static int delete(String sql) {
        return delete(sql, null);
    }
}
