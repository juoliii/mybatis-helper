package com.bitian.db.mybatis_helper.util;

import com.bitian.db.mybatis_helper.dialect.DialectFactory;
import com.bitian.db.mybatis_helper.dialect.DialectRegistry;
import com.bitian.db.mybatis_helper.mapper.DbMapper;
import com.bitian.db.mybatis_helper.meta.ColumnInfo;
import com.bitian.db.mybatis_helper.meta.EntityMetadata;
import com.bitian.db.mybatis_helper.meta.EntityResolver;
import com.bitian.db.mybatis_helper.meta.GenId;
import com.bitian.db.mybatis_helper.meta.IdGenerationType;
import com.bitian.db.mybatis_helper.meta.PageResult;
import com.bitian.db.mybatis_helper.meta.UUIDGenId;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DbUtil {

    private static SqlSessionFactory sqlSessionFactory;

    private static boolean isInSpring=false;
    
    private static final ThreadLocal<SqlSession> threadLocalSession = new ThreadLocal<>();

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
            else {
                SqlSession session = threadLocalSession.get();
                if (session != null) {
                    return session;
                }
                return sqlSessionFactory.openSession(true);
            }
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
                else {
                    if (threadLocalSession.get() == null) {
                        session.close();
                    }
                }
            }
        }
        throw new RuntimeException("DbUtil is not initialized. Please call init(SqlSessionFactory) or init(SqlSession) first.");
    }

    /**
     * 在非 Spring 环境下，在同一事务中执行多个数据库操作。
     * 示例：
     * DbUtil.executeInTransaction(() -> {
     *     DbUtil.insert(entity1);
     *     DbUtil.insert(entity2);
     * });
     */
    public static void executeInTransaction(Runnable action) {
        if (isInSpring) {
            // 如果在 Spring 环境下，交由 Spring 的 @Transactional 控制，直接执行
            action.run();
            return;
        }

        if (threadLocalSession.get() != null) {
            // 已经在事务中，支持嵌套调用
            action.run();
            return;
        }

        SqlSession session = sqlSessionFactory.openSession(false); // 关闭自动提交
        try {
            threadLocalSession.set(session);
            action.run();
            session.commit();
        } catch (Exception e) {
            session.rollback();
            throw new RuntimeException(e);
        } finally {
            session.close();
            threadLocalSession.remove();
        }
    }

    /**
     * 在非 Spring 环境下，在同一事务中执行多个数据库操作，并返回结果。
     */
    public static <R> R executeInTransaction(java.util.function.Supplier<R> action) {
        if (isInSpring) {
            return action.get();
        }

        if (threadLocalSession.get() != null) {
            return action.get();
        }

        SqlSession session = sqlSessionFactory.openSession(false);
        try {
            threadLocalSession.set(session);
            R result = action.get();
            session.commit();
            return result;
        } catch (Exception e) {
            session.rollback();
            throw new RuntimeException(e);
        } finally {
            session.close();
            threadLocalSession.remove();
        }
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

    // ============================
    // 实体对象 CRUD 支持
    // （通过 JPA 注解 @Table, @Column, @Id, @Transient 解析实体类映射）
    // ============================

    /**
     * 将实体对象中所有字段的值提取到参数 Map 中
     */
    private static Map<String, Object> extractEntityParams(Object entity, List<ColumnInfo> columns) {
        Map<String, Object> params = new HashMap<>();
        for (ColumnInfo col : columns) {
            params.put(col.getFieldName(), col.getValue(entity));
        }
        return params;
    }

    // --- insert ---

    /**
     * 插入实体对象，所有字段都会参与（包括 null 值的字段）。
     * <p>
     * 通过 JPA 注解解析表名和列名：
     * <ul>
     *   <li>{@code @Table(name = "...")} 指定表名</li>
     *   <li>{@code @Column(name = "...")} 指定列名</li>
     *   <li>{@code @Transient} 标记的字段会被跳过</li>
     *   <li>{@code @KeySql} 指定主键生成策略（IDENTITY/UUID/SQL/CUSTOM）</li>
     * </ul>
     *
     * @param entity 实体对象
     * @param <T>    实体类型
     * @return 影响的行数
     */
    public static <T> int insert(T entity) {
        EntityMetadata metadata = EntityResolver.resolve(entity.getClass());

        // 插入前生成主键（UUID / SQL / CUSTOM）
        applyPreInsertKeyGeneration(entity, metadata);

        // 获取参与 INSERT 的列（IDENTITY 策略的主键列排除）
        List<ColumnInfo> insertColumns = getInsertColumns(metadata.getColumns());

        String sql = buildInsertSql(metadata.getTableName(), insertColumns);
        Map<String, Object> params = extractEntityParams(entity, insertColumns);

        // IDENTITY 策略：使用 useGeneratedKeys，插入后回填
        ColumnInfo identityIdCol = findIdentityIdColumn(metadata);
        if (identityIdCol != null) {
            return insertWithIdentityKey(sql, params, entity, identityIdCol);
        }

        return insert(sql, params);
    }

    /**
     * 插入实体对象，仅插入非 null 的字段（Selective）。
     * <p>
     * 主键生成策略与 {@link #insert(Object)} 相同。
     *
     * @param entity 实体对象
     * @param <T>    实体类型
     * @return 影响的行数
     */
    public static <T> int insertSelective(T entity) {
        EntityMetadata metadata = EntityResolver.resolve(entity.getClass());

        // 插入前生成主键（UUID / SQL / CUSTOM）
        applyPreInsertKeyGeneration(entity, metadata);

        // 获取参与 INSERT 的列（IDENTITY 策略的主键列排除）
        List<ColumnInfo> insertColumns = getInsertColumns(metadata.getColumns());

        // 过滤出非 null 字段
        List<ColumnInfo> nonNullColumns = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        for (ColumnInfo col : insertColumns) {
            Object value = col.getValue(entity);
            if (value != null) {
                nonNullColumns.add(col);
                params.put(col.getFieldName(), value);
            }
        }

        if (nonNullColumns.isEmpty()) {
            throw new RuntimeException("Entity has no non-null fields to insert.");
        }

        String sql = buildInsertSql(metadata.getTableName(), nonNullColumns);

        // IDENTITY 策略：使用 useGeneratedKeys，插入后回填
        ColumnInfo identityIdCol = findIdentityIdColumn(metadata);
        if (identityIdCol != null) {
            return insertWithIdentityKey(sql, params, entity, identityIdCol);
        }

        return insert(sql, params);
    }

    /**
     * 批量插入实体对象列表，使用 JDBC 批处理（ExecutorType.BATCH）一次性提交，
     * 而非循环逐条插入，性能远高于逐条 insert。
     * <p>
     * 支持事务参与：在 {@link #executeInTransaction(Runnable)} 或 Spring {@code @Transactional}
     * 中调用时，会通过共享底层 JDBC Connection 加入外部事务，异常时由外部事务统一回滚。
     * <p>
     * 所有实体必须为同一类型。主键生成策略与单条 insert 一致：
     * <ul>
     *   <li>UUID / SQL / CUSTOM 策略：在插入前生成并设置到实体对象</li>
     *   <li>IDENTITY 策略：由数据库自增生成，插入后回填到实体对象</li>
     * </ul>
     *
     * @param list 实体对象列表（不能为空，且元素类型必须一致）
     * @param <T>  实体类型
     * @return 影响的总行数
     */
    public static <T> int insert(List<T> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        T first = list.get(0);
        EntityMetadata metadata = EntityResolver.resolve(first.getClass());

        // 获取参与 INSERT 的列（IDENTITY 策略的主键列排除）
        List<ColumnInfo> insertColumns = getInsertColumns(metadata.getColumns());
        String sql = buildInsertSql(metadata.getTableName(), insertColumns);
        ColumnInfo identityIdCol = findIdentityIdColumn(metadata);

        // 为每个实体执行插入前的主键生成（UUID / SQL / CUSTOM）
        for (T entity : list) {
            applyPreInsertKeyGeneration(entity, metadata);
        }

        // 判断是否在事务上下文中，获取共享 Connection
        Connection sharedConnection = getTransactionalConnection();

        if (sharedConnection != null) {
            // 共享 Connection，加入外部事务（不自行 commit/rollback）
            return executeBatchOnConnection(sharedConnection, sql, list, insertColumns, identityIdCol, false);
        } else {
            // 独立事务
            return executeBatchOnConnection(null, sql, list, insertColumns, identityIdCol, true);
        }
    }


    /**
     * 获取当前事务上下文中的 JDBC Connection。
     * 如果不在事务中，返回 null。
     */
    private static Connection getTransactionalConnection() {
        // 非 Spring 环境：检查 executeInTransaction 的 threadLocal session
        if (!isInSpring && threadLocalSession.get() != null) {
            return threadLocalSession.get().getConnection();
        }

        // Spring 环境：获取当前事务 session，检查是否在事务中（autoCommit=false）
        if (isInSpring) {
            SqlSession springSession = SqlSessionUtils.getSqlSession(
                    sqlSessionFactory,
                    sqlSessionFactory.getConfiguration().getDefaultExecutorType(),
                    null);
            try {
                Connection conn = springSession.getConnection();
                if (!conn.getAutoCommit()) {
                    return conn;
                }
            } catch (SQLException ignored) {
            } finally {
                // closeSqlSession 在事务中只减引用计数，不会真正关闭 session 和连接
                SqlSessionUtils.closeSqlSession(springSession, sqlSessionFactory);
            }
        }

        return null;
    }

    /**
     * 在指定 Connection（或独立 session）上执行 JDBC 批量插入。
     *
     * @param sharedConnection 共享连接（null 表示独立模式）
     * @param sql              INSERT SQL 模板
     * @param list             实体列表
     * @param insertColumns    参与插入的列信息
     * @param identityIdCol    IDENTITY 主键列（可为 null）
     * @param standalone       是否独立事务（自行 commit/rollback）
     */
    private static <T> int executeBatchOnConnection(Connection sharedConnection, String sql,
                                                     List<T> list, List<ColumnInfo> insertColumns,
                                                     ColumnInfo identityIdCol, boolean standalone) {
        SqlSession batchSession;
        if (standalone) {
            batchSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false);
        } else {
            // 使用不可关闭的 Connection 代理，防止 batchSession.close() 关闭共享连接
            Connection proxyConn = createNonClosingConnectionProxy(sharedConnection);
            batchSession = sqlSessionFactory.openSession(ExecutorType.BATCH, proxyConn);
        }

        try {
            Configuration config = batchSession.getConfiguration();

            String msId;
            if (identityIdCol != null) {
                msId = ensureInsertKeyGenStatement(config,
                        identityIdCol.getFieldName(), identityIdCol.getColumnName());
            } else {
                msId = ensureBatchInsertStatement(config);
            }

            // 保存每个实体对应的 paramMap 引用，用于 IDENTITY 策略回填
            List<Map<String, Object>> paramMaps = new ArrayList<>(list.size());

            for (T entity : list) {
                Map<String, Object> params = extractEntityParams(entity, insertColumns);
                Map<String, Object> paramMap = buildParam(sql, params);
                paramMaps.add(paramMap);
                batchSession.insert(msId, paramMap);
            }

            batchSession.flushStatements();

            // 独立模式自行 commit；共享模式由外部事务统一 commit
            if (standalone) {
                batchSession.commit();
            }

            // IDENTITY 策略：flushStatements 后 MyBatis 已将生成的 key 回填到各 paramMap 中
            if (identityIdCol != null) {
                for (int i = 0; i < list.size(); i++) {
                    Object generatedKey = paramMaps.get(i).get(identityIdCol.getFieldName());
                    if (generatedKey != null) {
                        identityIdCol.setValue(list.get(i), generatedKey);
                    }
                }
            }

            return list.size();
        } catch (Exception e) {
            if (standalone) {
                batchSession.rollback();
            }
            throw new RuntimeException("Batch insert failed.", e);
        } finally {
            batchSession.close();
        }
    }

    /**
     * 创建不可关闭的 Connection 动态代理。
     * 所有方法调用都委托给原始 Connection，但 close() 被忽略，
     * 防止 BATCH SqlSession 关闭时意外关闭外部事务的共享连接。
     */
    private static Connection createNonClosingConnectionProxy(Connection target) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        return null; // 忽略 close 调用
                    }
                    try {
                        return method.invoke(target, args);
                    } catch (InvocationTargetException e) {
                        throw e.getTargetException();
                    }
                }
        );
    }

    // --- insert 主键生成辅助方法 ---

    /**
     * 构建 INSERT SQL
     */
    private static String buildInsertSql(String tableName, List<ColumnInfo> columns) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tableName).append(" (");

        StringBuilder values = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            ColumnInfo col = columns.get(i);
            if (i > 0) {
                sql.append(", ");
                values.append(", ");
            }
            sql.append(col.getColumnName());
            values.append("#{").append(col.getFieldName()).append("}");
        }
        sql.append(") VALUES (").append(values).append(")");
        return sql.toString();
    }

    /**
     * 获取参与 INSERT 的列（排除 IDENTITY 策略的主键列，因为由数据库自增生成）
     */
    private static List<ColumnInfo> getInsertColumns(List<ColumnInfo> allColumns) {
        List<ColumnInfo> result = new ArrayList<>();
        for (ColumnInfo col : allColumns) {
            if (col.isId() && col.getGenerationType() == IdGenerationType.IDENTITY) {
                continue;
            }
            result.add(col);
        }
        return result;
    }

    /**
     * 查找使用 IDENTITY 策略的主键列（仅支持单一 IDENTITY 主键）
     */
    private static ColumnInfo findIdentityIdColumn(EntityMetadata metadata) {
        for (ColumnInfo col : metadata.getIdColumns()) {
            if (col.getGenerationType() == IdGenerationType.IDENTITY) {
                return col;
            }
        }
        return null;
    }

    /**
     * 插入前主键生成：处理 UUID、SQL、CUSTOM 策略，生成的值会设置到实体对象上。
     * IDENTITY 策略在插入后由数据库生成。
     */
    @SuppressWarnings("unchecked")
    private static <T> void applyPreInsertKeyGeneration(T entity, EntityMetadata metadata) {
        for (ColumnInfo idCol : metadata.getIdColumns()) {
            IdGenerationType genType = idCol.getGenerationType();
            if (genType == IdGenerationType.NONE || genType == IdGenerationType.IDENTITY) {
                continue;
            }
            switch (genType) {
                case UUID:
                    idCol.setValue(entity, new UUIDGenId().genId());
                    break;
                case SQL:
                    String keySql = idCol.getKeySql();
                    if (keySql == null || keySql.isEmpty()) {
                        throw new RuntimeException("@KeySql(type=SQL) requires a non-empty sql attribute.");
                    }
                    Map<String, Object> result = selectOne(keySql);
                    if (result != null && !result.isEmpty()) {
                        Object keyValue = result.values().iterator().next();
                        idCol.setValue(entity, keyValue);
                    }
                    break;
                case CUSTOM:
                    Class<? extends GenId> genIdClass = idCol.getGenIdClass();
                    if (genIdClass == null || genIdClass == GenId.None.class) {
                        throw new RuntimeException("@KeySql(type=CUSTOM) requires a valid genId class.");
                    }
                    try {
                        GenId genId = genIdClass.newInstance();
                        Object key = genId.genId();
                        idCol.setValue(entity, key);
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new RuntimeException("Failed to instantiate GenId: " + genIdClass.getName(), e);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 使用 useGeneratedKeys 执行 INSERT，插入后将数据库生成的主键回填到实体对象。
     */
    private static <T> int insertWithIdentityKey(String sql, Map<String, Object> params,
                                                  T entity, ColumnInfo idCol) {
        return execute(session -> {
            Configuration config = session.getConfiguration();
            String msId = ensureInsertKeyGenStatement(config, idCol.getFieldName(), idCol.getColumnName());
            Map<String, Object> paramMap = buildParam(sql, params);
            int rows = session.insert(msId, paramMap);
            // 回填生成的主键值到实体对象
            Object generatedKey = paramMap.get(idCol.getFieldName());
            if (generatedKey != null) {
                idCol.setValue(entity, generatedKey);
            }
            return rows;
        });
    }

    /**
     * 动态创建并注册带有 useGeneratedKeys 的 INSERT MappedStatement
     */
    private static String ensureInsertKeyGenStatement(Configuration configuration,
                                                       String keyProperty, String keyColumn) {
        String msId = "DbUtil_dynamic_insert_identity_" + keyProperty;
        if (configuration.hasStatement(msId, false)) {
            return msId;
        }
        synchronized (configuration) {
            if (configuration.hasStatement(msId, false)) {
                return msId;
            }
            LanguageDriver languageDriver = configuration.getDefaultScriptingLanguageInstance();
            SqlSource sqlSource = languageDriver.createSqlSource(configuration,
                    "<script>${_dbutil_sql}</script>", Map.class);
            MappedStatement ms = new MappedStatement.Builder(configuration, msId, sqlSource, SqlCommandType.INSERT)
                    .keyGenerator(Jdbc3KeyGenerator.INSTANCE)
                    .keyProperty(keyProperty)
                    .keyColumn(keyColumn)
                    .build();
            configuration.addMappedStatement(ms);
            return msId;
        }
    }

    /**
     * 动态创建并注册用于批量插入的 INSERT MappedStatement（不带 useGeneratedKeys）
     */
    private static String ensureBatchInsertStatement(Configuration configuration) {
        String msId = "DbUtil_dynamic_batch_insert";
        if (configuration.hasStatement(msId, false)) {
            return msId;
        }
        synchronized (configuration) {
            if (configuration.hasStatement(msId, false)) {
                return msId;
            }
            LanguageDriver languageDriver = configuration.getDefaultScriptingLanguageInstance();
            SqlSource sqlSource = languageDriver.createSqlSource(configuration,
                    "<script>${_dbutil_sql}</script>", Map.class);
            MappedStatement ms = new MappedStatement.Builder(configuration, msId, sqlSource, SqlCommandType.INSERT)
                    .build();
            configuration.addMappedStatement(ms);
            return msId;
        }
    }

    // --- update ---

    /**
     * 根据主键更新实体对象，所有字段都会参与（包括 null 值的字段）。
     * <p>
     * 实体类必须包含 {@code @Id} 注解标记的主键字段。
     *
     * @param entity 实体对象（主键字段的值作为 WHERE 条件）
     * @param <T>    实体类型
     * @return 影响的行数
     */
    public static <T> int update(T entity) {
        EntityMetadata metadata = EntityResolver.resolve(entity.getClass());
        metadata.requireId();

        List<ColumnInfo> nonIdColumns = metadata.getNonIdColumns();
        List<ColumnInfo> idColumns = metadata.getIdColumns();

        if (nonIdColumns.isEmpty()) {
            throw new RuntimeException("Entity has no non-id columns to update.");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(metadata.getTableName()).append(" SET ");

        for (int i = 0; i < nonIdColumns.size(); i++) {
            ColumnInfo col = nonIdColumns.get(i);
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(col.getColumnName()).append(" = #{").append(col.getFieldName()).append("}");
        }

        appendWhereId(sql, idColumns);

        Map<String, Object> params = extractEntityParams(entity, metadata.getColumns());
        return update(sql.toString(), params);
    }

    /**
     * 根据主键更新实体对象，仅更新非 null 的字段（Selective）。
     *
     * @param entity 实体对象（主键字段的值作为 WHERE 条件）
     * @param <T>    实体类型
     * @return 影响的行数
     */
    public static <T> int updateSelective(T entity) {
        EntityMetadata metadata = EntityResolver.resolve(entity.getClass());
        metadata.requireId();

        List<ColumnInfo> idColumns = metadata.getIdColumns();

        // 过滤出非主键且非 null 的字段
        List<ColumnInfo> setCols = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        for (ColumnInfo col : metadata.getNonIdColumns()) {
            Object value = col.getValue(entity);
            if (value != null) {
                setCols.add(col);
                params.put(col.getFieldName(), value);
            }
        }

        if (setCols.isEmpty()) {
            throw new RuntimeException("Entity has no non-null, non-id fields to update.");
        }

        // 主键参数也要加入
        for (ColumnInfo idCol : idColumns) {
            params.put(idCol.getFieldName(), idCol.getValue(entity));
        }

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(metadata.getTableName()).append(" SET ");

        for (int i = 0; i < setCols.size(); i++) {
            ColumnInfo col = setCols.get(i);
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(col.getColumnName()).append(" = #{").append(col.getFieldName()).append("}");
        }

        appendWhereId(sql, idColumns);

        return update(sql.toString(), params);
    }

    /**
     * 使用 QueryWrapper 条件更新实体对象的非 null 字段。
     * <p>
     * 不依赖主键，WHERE 条件完全由 QueryWrapper 指定。
     *
     * @param entity  实体对象（提取非 null 字段作为 SET 子句）
     * @param wrapper 查询条件
     * @param <T>     实体类型
     * @return 影响的行数
     */
    public static <T> int updateByQuery(T entity, QueryWrapper wrapper) {
        EntityMetadata metadata = EntityResolver.resolve(entity.getClass());

        List<ColumnInfo> setCols = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        for (ColumnInfo col : metadata.getColumns()) {
            Object value = col.getValue(entity);
            if (value != null) {
                setCols.add(col);
                params.put(col.getFieldName(), value);
            }
        }

        if (setCols.isEmpty()) {
            throw new RuntimeException("Entity has no non-null fields to update.");
        }

        // 合并 QueryWrapper 的参数
        params.putAll(wrapper.getParams());

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(metadata.getTableName()).append(" SET ");

        for (int i = 0; i < setCols.size(); i++) {
            ColumnInfo col = setCols.get(i);
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(col.getColumnName()).append(" = #{").append(col.getFieldName()).append("}");
        }

        sql.append(wrapper.getSqlSegment());

        return update(sql.toString(), params);
    }

    // --- delete ---

    /**
     * 根据主键删除实体对象。
     * <p>
     * 从实体对象中提取 {@code @Id} 标记的主键值作为 WHERE 条件。
     *
     * @param entity 实体对象（主键字段的值作为 WHERE 条件）
     * @param <T>    实体类型
     * @return 影响的行数
     */
    public static <T> int delete(T entity) {
        EntityMetadata metadata = EntityResolver.resolve(entity.getClass());
        metadata.requireId();

        List<ColumnInfo> idColumns = metadata.getIdColumns();

        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(metadata.getTableName());
        appendWhereId(sql, idColumns);

        Map<String, Object> params = new HashMap<>();
        for (ColumnInfo idCol : idColumns) {
            params.put(idCol.getFieldName(), idCol.getValue(entity));
        }

        return delete(sql.toString(), params);
    }

    /**
     * 根据实体类型和 QueryWrapper 条件删除记录。
     *
     * @param clazz   实体类类型（用于解析表名）
     * @param wrapper 查询条件
     * @param <T>     实体类型
     * @return 影响的行数
     */
    public static <T> int deleteByQuery(Class<T> clazz, QueryWrapper wrapper) {
        EntityMetadata metadata = EntityResolver.resolve(clazz);

        String sql = "DELETE FROM " + metadata.getTableName() + wrapper.getSqlSegment();
        return delete(sql, wrapper.getParams());
    }

    /**
     * 拼接主键 WHERE 条件
     */
    private static void appendWhereId(StringBuilder sql, List<ColumnInfo> idColumns) {
        sql.append(" WHERE ");
        for (int i = 0; i < idColumns.size(); i++) {
            ColumnInfo idCol = idColumns.get(i);
            if (i > 0) {
                sql.append(" AND ");
            }
            sql.append(idCol.getColumnName()).append(" = #{").append(idCol.getFieldName()).append("}");
        }
    }
}

