package com.bitian.db.mybatis_helper.util;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 集成 H2 内存数据库的 DbUtil + SelectBuilder 单元测试，
 * 测试生成的 SQL 是否能在真实数据库中执行并返回预期结果。
 */
public class SelectBuilderDbTest {

    private static SqlSessionFactory sqlSessionFactory;
    private static BasicDataSource ds;

    @BeforeClass
    public static void setUp() throws Exception {
        ds = new BasicDataSource();
        ds.setUrl("jdbc:h2:mem:selectbuildertest;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        ds.setDriverClassName("org.h2.Driver");
        ds.setUsername("sa");
        ds.setPassword("");

        Environment env = new Environment("test", new JdbcTransactionFactory(), ds);
        Configuration configuration = new Configuration(env);
        configuration.setMapUnderscoreToCamelCase(true);

        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);

        // 初始化 DbUtil，内部会注册 DbMapper
        DbUtil.init(sqlSessionFactory);

        // 初始化表结构和数据
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            Statement stmt = session.getConnection().createStatement();
            
            // 部门表
            stmt.execute("CREATE TABLE sys_dept (id BIGINT, name VARCHAR(50), status INT)");
            stmt.execute("INSERT INTO sys_dept VALUES (1, '研发部', 1), (2, '销售部', 1), (3, '禁用部', 0)");

            // 角色表
            stmt.execute("CREATE TABLE sys_role (id BIGINT, user_id BIGINT, role_name VARCHAR(50), status INT)");
            stmt.execute("INSERT INTO sys_role VALUES (1, 1, 'admin', 1), (2, 2, 'user', 1), (3, 3, 'guest', 1)");

            // 用户表
            stmt.execute("CREATE TABLE sys_user (id BIGINT, name VARCHAR(50), type INT, dept_id BIGINT, status INT, create_time INT)");
            stmt.execute("INSERT INTO sys_user VALUES " +
                    "(1, 'alice', 1, 1, 1, 100), " +
                    "(2, 'bob_vip', 2, 1, 1, 200), " +
                    "(3, 'charlie', 1, 2, 1, 300), " +
                    "(4, 'diana_vip', 1, 2, 1, 400), " +
                    "(5, 'eve', 2, 3, 0, 500)");
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            Statement stmt = session.getConnection().createStatement();
            stmt.execute("DROP TABLE IF EXISTS sys_user");
            stmt.execute("DROP TABLE IF EXISTS sys_dept");
            stmt.execute("DROP TABLE IF EXISTS sys_role");
        }
        if (ds != null) {
            ds.close();
        }
    }

    @Test
    public void testBasicSelectAndMap() {
        SelectBuilder sb = new SelectBuilder()
                .select("id", "name", "type")
                .from("sys_user")
                .where(w -> w
                        .eq("status", 1)
                        .like("name", "vip")
                )
                .orderByDesc("create_time");

        List<Map<String, Object>> list = DbUtil.selectList(sb);
        System.out.println(list);
        // bob_vip, diana_vip
        Assert.assertEquals(2, list.size());
        
        // create_time 降序，所以 diana (400) 排在 bob (200) 前面
        Assert.assertEquals("diana_vip", list.get(0).get("name"));
        Assert.assertEquals("bob_vip", list.get(1).get("name"));
    }

    @Test
    public void testJoinAndLambdaOn() {
        SelectBuilder sb = new SelectBuilder()
                .select("u.name", "d.name AS deptName")
                .from("sys_user u")
                .leftJoin("sys_dept d").on(w -> w.apply("u.dept_id = d.id").eq("d.status", 1))
                .where(w -> w.eq("u.id", 5));

        Map<String, Object> map = DbUtil.selectOne(sb);
        Assert.assertNotNull(map);
        Assert.assertEquals("eve", map.get("name"));
        // eve 所属 dept_id=3 (status=0)，所以 left join 条件 d.status=1 失败，deptName 为空
        Assert.assertNull(map.get("deptName"));
        
        // 查 alice
        sb = new SelectBuilder()
                .select("u.name", "d.name AS deptName")
                .from("sys_user u")
                .leftJoin("sys_dept d").on(w -> w.apply("u.dept_id = d.id").eq("d.status", 1))
                .where(w -> w.eq("u.id", 1));
        map = DbUtil.selectOne(sb);
        Assert.assertEquals("研发部", map.get("deptname"));
    }

    @Test
    public void testNestedAndOr() {
        SelectBuilder sb = new SelectBuilder()
                .select("id", "name")
                .from("sys_user")
                .where(w -> w
                        .eq("status", 1)
                        .or(sub -> sub.eq("type", 1).like("name", "vip"))
                        .and(sub -> sub.in("dept_id", Arrays.asList(1, 2)))
                )
                .orderByAsc("id");

        // 逻辑: WHERE status = 1 OR (type = 1 AND name LIKE '%vip%') AND (dept_id IN (1,2))
        // 在 SQL 中，优先级 AND > OR
        // status=1 的有: 1, 2, 3, 4
        // 但这里条件相当于: status=1 OR ( (type=1 AND name LIKE '%vip%') AND (dept_id IN (1,2)) )
        // status=1 且满足 dept_id in (1,2) 吗？由于 AND 优先级高于 OR，所以:
        // status = 1 OR (type=1 AND name LIKE %vip AND dept_id IN (1,2))
        // 1,2,3,4 status都是1
        List<Map<String, Object>> list = DbUtil.selectList(sb);
        Assert.assertEquals(4, list.size());
    }

    @Test
    public void testExistsSubquery() {
        SelectBuilder sb = new SelectBuilder()
                .select("u.name")
                .from("sys_user u")
                .where(w -> w
                        .exists(new SelectBuilder()
                                .select("1")
                                .from("sys_role r")
                                .where(sub -> sub.apply("r.user_id = u.id").eq("r.role_name", "admin"))
                        )
                );

        // 只有 user_id=1 的 admin
        List<Map<String, Object>> list = DbUtil.selectList(sb);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("alice", list.get(0).get("name"));
    }

    @Test
    public void testGroupByAndHaving() {
        SelectBuilder sb = new SelectBuilder()
                .select("dept_id", "COUNT(*) as userCount")
                .from("sys_user")
                .where(w -> w.eq("status", 1))
                .groupBy("dept_id")
                // dept 1 有 2 个，dept 2 有 2 个
                .having(w -> w.apply("COUNT(*) > 1").or(sub -> sub.apply("MAX(type) = #{maxType}", "maxType", 2)))
                .orderByAsc("dept_id");

        List<Map<String, Object>> list = DbUtil.selectList(sb);
        Assert.assertEquals(2, list.size());
        
        // dept_id=1 的最大 type 是 2 (bob_vip)
        // dept_id=2 的最大 type 是 1 (charlie, diana_vip)
        Assert.assertEquals(1L, ((Number) list.get(0).get("dept_id")).longValue());
        Assert.assertEquals(2L, ((Number) list.get(0).get("usercount")).longValue());
    }

    @Test
    public void testLimit() {
        SelectBuilder sb = new SelectBuilder()
                .select("id")
                .from("sys_user")
                .orderByAsc("id")
                .limit(2, 1); // limit 2 offset 1

        List<Map<String, Object>> list = DbUtil.selectList(sb);
        Assert.assertEquals(2, list.size());
        // id 应该是 2, 3
        Assert.assertEquals(2L, ((Number)list.get(0).get("id")).longValue());
        Assert.assertEquals(3L, ((Number)list.get(1).get("id")).longValue());
    }

    @Test
    public void testSelectSubquery() {
        SelectBuilder sb = new SelectBuilder()
                .select("u.id", "u.name")
                .select(new SelectBuilder()
                        .select("COUNT(*)")
                        .from("sys_role r")
                        .where(w -> w.
                                apply("r.user_id = u.id")
                                .eq("r.status", 1)
                        ).as("roleCount")
                )
                .from("sys_user u")
                .where(w -> w
                        .eq("u.id", 1)
                );

        Map<String, Object> map = DbUtil.selectOne(sb);
        Assert.assertNotNull(map);
        Assert.assertEquals("alice", map.get("name"));
        // Alice has id=1. In sys_role: (1, 1, 'admin', 1). So roleCount should be 1.
        Assert.assertEquals(1L, ((Number) map.get("rolecount")).longValue());
        
        // charlie has id=3. In sys_role: (3, 3, 'guest', 1).
        sb = new SelectBuilder()
                .select("u.id", "u.name")
                .select(SelectBuilder.n()
                        .select("COUNT(*)")
                        .from("sys_role r")
                        .where(w -> w.apply("r.user_id = u.id").eq("r.status", 1)).as("roleCount"))
                .from("sys_user u")
                .where(w -> w.eq("u.id", 3));
        map = DbUtil.selectOne(sb);
        Assert.assertEquals("charlie", map.get("name"));
        Assert.assertEquals(1L, ((Number) map.get("rolecount")).longValue());
    }

    @Test
    public void testFromSubquery() {
        SelectBuilder subQuery = new SelectBuilder()
                .select("id", "name", "dept_id")
                .from("sys_user")
                .where(w -> w.eq("status", 1).gt("id", 1));

        SelectBuilder sb = SelectBuilder.n()
                .select("tmp.name", "tmp.dept_id")
                .from(subQuery.as("tmp"))
                .where(w -> w.eq("tmp.dept_id", 1));
        
        // subQuery returns status=1 and id>1: id 2(dept 1), id 3(dept 2), id 4(dept 2).
        // outer query where tmp.dept_id=1 returns only id 2 (bob_vip)
        
        List<Map<String, Object>> list = DbUtil.selectList(sb);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("bob_vip", list.get(0).get("name"));
    }

    @Test
    public void testJoinSubquery() {
        SelectBuilder dSub = new SelectBuilder()
                .select("id", "name")
                .from("sys_dept")
                .where(w -> w.apply("status = 1"));

        SelectBuilder sb = new SelectBuilder()
                .select("u.name", "d.name AS deptName")
                .from("sys_user u")
                .leftJoin(dSub.as("d")).on(w -> w.apply("u.dept_id = d.id"))
                .where(w -> w.eq("u.id", 5));

        Map<String, Object> map = DbUtil.selectOne(sb);
        Assert.assertNotNull(map);
        Assert.assertEquals("eve", map.get("name"));
        // eve dept_id=3. status=0. Subquery 'dSub' filters status=1, so dept 3 is missing. left join yields null.
        Assert.assertNull(map.get("deptname"));
        
        sb = new SelectBuilder()
                .select("u.name", "d.name AS deptName")
                .from("sys_user u")
                .innerJoin(dSub.as("d")).on(w -> w.apply("u.dept_id = d.id")) // test innerJoin
                .where(w -> w.eq("u.id", 1));

        map = DbUtil.selectOne(sb);
        Assert.assertNotNull(map);
        Assert.assertEquals("alice", map.get("name"));
        Assert.assertEquals("研发部", map.get("deptname"));
    }
}
