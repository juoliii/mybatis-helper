package com.bitian.db.mybatis_helper;

import com.bitian.db.mybatis_helper.entity.SysUser;
import com.bitian.db.mybatis_helper.mapper.SysUserMapper;
import com.bitian.db.mybatis_helper.tk.TkQueryWrapper;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.mapperhelper.MapperHelper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * TkQueryWrapper AND/OR 分组功能集成测试
 * 使用 H2 内存数据库 + tkmapper
 */
public class TkTest {

    private static SqlSessionFactory sqlSessionFactory;

    @BeforeClass
    public static void setUp() throws Exception {
        // 1. 创建 H2 数据源
        BasicDataSource ds = new BasicDataSource();
        ds.setUrl("jdbc:h2:mem:tktest;DB_CLOSE_DELAY=-1;MODE=MySQL");
        ds.setDriverClassName("org.h2.Driver");
        ds.setUsername("sa");
        ds.setPassword("");

        // 2. 构建 MyBatis Configuration
        Environment env = new Environment("test", new JdbcTransactionFactory(), ds);
        Configuration configuration = new Configuration(env);
        configuration.setMapUnderscoreToCamelCase(true);

        // 3. 注册 SysUserMapper（tkmapper 方式）
        configuration.addMapper(SysUserMapper.class);

        // 4. 初始化 MapperHelper（让 tkmapper 处理自定义 Provider）
        MapperHelper mapperHelper = new MapperHelper();
        Config config = new Config();
        config.setNotEmpty(false);
        mapperHelper.setConfig(config);
        mapperHelper.registerMapper(SysUserMapper.class);
        mapperHelper.processConfiguration(configuration);

        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);

        // 5. 建表 + 插入测试数据
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.getConnection().createStatement().execute(
                    "CREATE TABLE sys_user (" +
                            "id BIGINT PRIMARY KEY, " +
                            "username VARCHAR(50), " +
                            "password VARCHAR(100), " +
                            "nickname VARCHAR(50), " +
                            "test VARCHAR(20)" +
                            ")"
            );
            session.getConnection().createStatement().execute(
                    "INSERT INTO sys_user VALUES (1, 'alice', 'pwd1', '爱丽丝', 'submit')"
            );
            session.getConnection().createStatement().execute(
                    "INSERT INTO sys_user VALUES (2, 'bob', 'pwd2', '鲍勃', 'run')"
            );
            session.getConnection().createStatement().execute(
                    "INSERT INTO sys_user VALUES (3, 'charlie', 'pwd3', '查理', 'submit')"
            );
            session.getConnection().createStatement().execute(
                    "INSERT INTO sys_user VALUES (4, 'diana', 'pwd4', '黛安娜', 'run')"
            );
            session.getConnection().createStatement().execute(
                    "INSERT INTO sys_user VALUES (5, 'eve', 'pwd5', '伊芙', 'submit')"
            );
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.getConnection().createStatement().execute("DROP TABLE IF EXISTS sys_user");
        }
    }

    // ==========================================
    // 1. 基本 AND 查询（无分组）
    // ==========================================
    @Test
    public void testBasicAndConditions() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SysUserMapper mapper = session.getMapper(SysUserMapper.class);
            TkQueryWrapper<SysUser> wrapper = new TkQueryWrapper<>(SysUser.class);
            wrapper.eq("username", "alice")
                    .eq("password", "pwd1");

            List<SysUser> result = mapper.selectByQuery(wrapper);
            assertEquals(1, result.size());
            assertEquals("alice", result.get(0).getUsername());
        }
    }

    // ==========================================
    // 2. or() 切换 — 简单 OR 连接（不分组）
    // ==========================================
    @Test
    public void testSimpleOr() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SysUserMapper mapper = session.getMapper(SysUserMapper.class);
            TkQueryWrapper<SysUser> wrapper = new TkQueryWrapper<>(SysUser.class);
            // WHERE username = 'alice' OR username = 'bob'
            wrapper.eq("username", "alice")
                    .or()
                    .eq("username", "bob");

            List<SysUser> result = mapper.selectByQuery(wrapper);
            assertEquals(2, result.size());
        }
    }

    // ==========================================
    // 3. and(Consumer) 分组 — AND (... OR ...)
    // ==========================================
    @Test
    public void testAndGrouping() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SysUserMapper mapper = session.getMapper(SysUserMapper.class);
            TkQueryWrapper<SysUser> wrapper = new TkQueryWrapper<>(SysUser.class);
            // WHERE test = 'submit' AND (username = 'alice' OR username = 'charlie')
            // 应该命中 alice(id=1) 和 charlie(id=3)
            wrapper.eq("test", "submit")
                    .and(w -> w.eq("username", "alice")
                            .or()
                            .eq("username", "charlie"));

            List<SysUser> result = mapper.selectByQuery(wrapper);
            assertEquals(2, result.size());
            // 确认是 alice 和 charlie
            List<String> names = Arrays.asList(
                    result.get(0).getUsername(),
                    result.get(1).getUsername()
            );
            assertTrue(names.contains("alice"));
            assertTrue(names.contains("charlie"));
        }
    }

    // ==========================================
    // 4. or(Consumer) 分组 — ... OR (... AND ...)
    // ==========================================
    @Test
    public void testOrGrouping() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SysUserMapper mapper = session.getMapper(SysUserMapper.class);
            TkQueryWrapper<SysUser> wrapper = new TkQueryWrapper<>(SysUser.class);
            // WHERE username = 'alice' OR (test = 'run' AND username = 'bob')
            // 应该命中 alice(id=1) 和 bob(id=2)
            wrapper.eq("username", "alice")
                    .or(w -> w.eq("test", "run")
                            .eq("username", "bob"));

            List<SysUser> result = mapper.selectByQuery(wrapper);
            assertEquals(2, result.size());
            List<String> names = Arrays.asList(
                    result.get(0).getUsername(),
                    result.get(1).getUsername()
            );
            assertTrue(names.contains("alice"));
            assertTrue(names.contains("bob"));
        }
    }

    // ==========================================
    // 5. 嵌套分组 — 多层 and/or
    // ==========================================
    @Test
    public void testNestedGrouping() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SysUserMapper mapper = session.getMapper(SysUserMapper.class);
            TkQueryWrapper<SysUser> wrapper = new TkQueryWrapper<>(SysUser.class);
            // WHERE test = 'submit' AND (username = 'alice' OR username = 'eve')
            // submit 用户: alice(1), charlie(3), eve(5)
            // 交集: alice(1), eve(5)
            wrapper.eq("test", "submit")
                    .and(w -> w.eq("username", "alice")
                            .or()
                            .eq("username", "eve"));

            List<SysUser> result = mapper.selectByQuery(wrapper);
            assertEquals(2, result.size());
        }
    }

    // ==========================================
    // 6. 复杂嵌套 — and + or 混合
    // ==========================================
    @Test
    public void testComplexNestedGrouping() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SysUserMapper mapper = session.getMapper(SysUserMapper.class);
            TkQueryWrapper<SysUser> wrapper = new TkQueryWrapper<>(SysUser.class);
            // WHERE id > 0 AND (test = 'submit' AND (username = 'alice' OR username = 'charlie'))
            // 应该命中 alice(1), charlie(3)
            wrapper.gt("id", 0)
                    .and(w -> w.eq("test", "submit")
                            .and(inner -> inner.eq("username", "alice")
                                    .or()
                                    .eq("username", "charlie")));

            List<SysUser> result = mapper.selectByQuery(wrapper);
            assertEquals(2, result.size());
        }
    }

    // ==========================================
    // 7. apply() 自定义 SQL 片段
    // ==========================================
    @Test
    public void testApply() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SysUserMapper mapper = session.getMapper(SysUserMapper.class);
            TkQueryWrapper<SysUser> wrapper = new TkQueryWrapper<>(SysUser.class);
            wrapper.apply("id > 3");

            List<SysUser> result = mapper.selectByQuery(wrapper);
            assertEquals(2, result.size()); // diana(4), eve(5)
        }
    }

    // ==========================================
    // 8. IN 列表查询
    // ==========================================
    @Test
    public void testInCondition() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SysUserMapper mapper = session.getMapper(SysUserMapper.class);
            TkQueryWrapper<SysUser> wrapper = new TkQueryWrapper<>(SysUser.class);
            wrapper.in("username", Arrays.asList("alice", "bob", "eve"));

            List<SysUser> result = mapper.selectByQuery(wrapper);
            assertEquals(3, result.size());
        }
    }

    // ==========================================
    // 9. 空分组（Consumer 中不添加条件）— 不应生成无效 SQL
    // ==========================================
    @Test
    public void testEmptyGrouping() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SysUserMapper mapper = session.getMapper(SysUserMapper.class);
            TkQueryWrapper<SysUser> wrapper = new TkQueryWrapper<>(SysUser.class);
            wrapper.eq("username", "alice")
                    .and(w -> {
                        // 不添加任何条件
                    });

            List<SysUser> result = mapper.selectByQuery(wrapper);
            assertEquals(1, result.size());
            assertEquals("alice", result.get(0).getUsername());
        }
    }

    // ==========================================
    // 10. 纯 SQL 生成验证（不依赖数据库）
    // ==========================================
    @Test
    public void testSqlSegmentGeneration() {
        TkQueryWrapper<SysUser> wrapper = new TkQueryWrapper<>(SysUser.class);
        wrapper.eq("username", "test")
                .and(w -> w.eq("password", "abc")
                        .or()
                        .eq("nickname", "xyz"));

        String sql = wrapper.getSqlSegment();
        System.out.println("Generated SQL: " + sql);
        // 应该包含 WHERE ... AND ( ... OR ... )
        assertTrue("SQL should contain WHERE", sql.contains("WHERE"));
        assertTrue("SQL should contain AND (", sql.contains("AND ("));
        assertTrue("SQL should contain OR", sql.contains(" OR "));
    }
}
