package com.bitian.db.mybatis_helper.util;

import com.bitian.db.mybatis_helper.tk.meta.TkTable;
import com.bitian.db.mybatis_helper.tk.meta.TkColumn;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

public class SelectBuilderTest {

    @Test
    public void testBasicSql() {
        SelectBuilder sb = new SelectBuilder()
                .select("id", "name", "age")
                .from("sys_user")
                .where(w -> w
                    .eq("status", 1)
                    .like("name", "admin")
                )
                .orderByDesc("create_time")
                .limit(10);

        String sql = sb.toSql();
        Map<String, Object> params = sb.getParams();

        Assert.assertTrue(sql.contains("SELECT id, name, age"));
        Assert.assertTrue(sql.contains("FROM sys_user"));
        Assert.assertTrue(sql.contains("WHERE status = #{_qw_"));
        Assert.assertTrue(sql.contains("name LIKE #{_qw_"));
        Assert.assertTrue(sql.contains("ORDER BY create_time DESC"));
        Assert.assertTrue(sql.contains("LIMIT 10"));

        Assert.assertEquals(2, params.size());
        boolean hasAdmin = false;
        boolean hasStatus1 = false;
        for (Object value : params.values()) {
            if (value.equals(1)) hasStatus1 = true;
            if (value.equals("%admin%")) hasAdmin = true;
        }
        Assert.assertTrue(hasStatus1);
        Assert.assertTrue(hasAdmin);
    }

    @Test
    public void testJoinAndOnLambda() {
        SelectBuilder sb = new SelectBuilder()
                .select("u.id", "d.name AS deptName")
                .from("sys_user u")
                .leftJoin("sys_dept d").on(w -> w.apply("u.dept_id = d.id").eq("d.status", 1));

        String sql = sb.toSql();
        Map<String, Object> params = sb.getParams();

        Assert.assertTrue(sql.contains("LEFT JOIN sys_dept d ON (u.dept_id = d.id AND d.status = #{_qw_"));
        Assert.assertEquals(1, params.size());
        Assert.assertTrue(params.values().contains(1));
    }

    @Test
    public void testNestedAndOr() {
        SelectBuilder sb = new SelectBuilder()
                .select("*")
                .from("sys_user")
                .where(w -> w
                    .eq("status", 1)
                    .or(sub -> sub.eq("type", 1).like("name", "vip"))
                    .and(sub -> sub.in("role_id", Arrays.asList(1, 2, 3)))
                );

        String sql = sb.toSql();
        Map<String, Object> params = sb.getParams();

        Assert.assertTrue(sql.contains("OR (type = #{_qw_"));
        Assert.assertTrue(sql.contains("AND name LIKE #{_qw_"));
        Assert.assertTrue(sql.contains("AND (role_id IN (#{_qw_"));
        Assert.assertEquals(6, params.size()); // status, type, name, role_id(3)
    }

    @Test
    public void testExistsSubquery() {
        SelectBuilder sb = new SelectBuilder()
                .select("u.id")
                .from("sys_user u")
                .where(w -> w
                    .exists(new SelectBuilder()
                            .select("1")
                            .from("sys_role r")
                            .where(subW -> subW
                                .apply("r.user_id = u.id")
                                .eq("r.status", 1)
                            )
                    )
                );

        String sql = sb.toSql();
        Map<String, Object> params = sb.getParams();

        Assert.assertTrue(sql.contains("EXISTS (SELECT 1 FROM sys_role r WHERE r.user_id = u.id AND r.status = #{_qw_"));
        Assert.assertEquals(1, params.size());
        Assert.assertTrue(params.values().contains(1));
    }

    @Test
    public void testGroupByAndHavingLambda() {
        SelectBuilder sb = new SelectBuilder()
                .select("u.dept_id", "COUNT(*) as count")
                .from("sys_user u")
                .groupBy("u.dept_id")
                .having(w -> w.apply("COUNT(*) > 1").or(sub -> sub.apply("MAX(u.status) = #{maxStatus}", "maxStatus", 2)));

        String sql = sb.toSql();
        Map<String, Object> params = sb.getParams();

        Assert.assertTrue(sql.contains("GROUP BY u.dept_id"));
        Assert.assertTrue(sql.contains("HAVING COUNT(*) > 1 OR (MAX(u.status) = #{maxStatus})"));
        Assert.assertEquals(1, params.size());
        Assert.assertEquals(2, params.get("maxStatus"));
    }

    static class MySysUser extends TkTable {
        public final TkColumn ID = createColumn("id");
        public final TkColumn NAME = createColumn("name");
        public final TkColumn STATUS = createColumn("status");
        public final TkColumn DEPT_ID = createColumn("dept_id");
        public final TkColumn[] all = new TkColumn[] { ID, NAME, STATUS, DEPT_ID };
        public MySysUser(String alias) { super("sys_user", alias); }
    }

    static class MySysDept extends TkTable {
        public final TkColumn ID = createColumn("id");
        public final TkColumn NAME = createColumn("name");
        public final TkColumn[] all = new TkColumn[] { ID, NAME };
        public MySysDept(String alias) { super("sys_dept", alias); }
    }

    @Test
    public void testTkColumnAliasAndAll() {
        MySysUser u = new MySysUser("u");
        MySysDept d = new MySysDept("d");

        SelectBuilder sb = new SelectBuilder()
                .select(u.all)
                .select(d.NAME.as("deptName"))
                .from(u)
                .leftJoin(d).on(w -> w.eq(u.DEPT_ID, d.ID));

        String sql = sb.toSql();
        Assert.assertTrue(sql.contains("SELECT u.id, u.name, u.status, u.dept_id, d.name AS deptName FROM sys_user u LEFT JOIN sys_dept d ON (u.dept_id = d.id)"));
    }

    @Test
    public void testTKSelectBuilder() {
        MySysUser u = new MySysUser("u");
        MySysDept d = new MySysDept("d");

        SelectBuilder sb = new SelectBuilder()
                .select(u.ID, u.NAME, d.NAME)
                .from(u)
                .leftJoin(d).on(w -> w.eq(u.DEPT_ID, d.ID))
                .where(w -> w.eq(u.STATUS, 1).like(u.NAME, "vip"));

        String sql = sb.toSql();
        Assert.assertTrue(sql.contains("SELECT u.id, u.name, d.name FROM sys_user u LEFT JOIN sys_dept d ON (u.dept_id = d.id) WHERE u.status = #{_qw_param_"));
        Assert.assertTrue(sql.contains("AND u.name LIKE #{_qw_param_"));
        
        Assert.assertEquals(2, sb.getParams().size());
        Assert.assertTrue(sb.getParams().values().contains(1));
        Assert.assertTrue(sb.getParams().values().contains("%vip%"));
    }
}
