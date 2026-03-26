# mybatis-helper
## 1. 功能
- 分页
- 去xml，通过groovy模板写sql

mybatis帮助插件,目前已经完成简单的分页插件,无入侵式
目前只支持mysql数据库

## 2.使用方法

```
<plugins>
	<plugin interceptor="com.bitian.db.mybatis_helper.plugins.BitianInterceptor">
		<property name="dialect" value="mysql"/>
	</plugin>
</plugins>
```

```
PageHelper.startPage(1, 1);//查询之前调用
List<Map<String, Object>> list=session.getMapper(TestMapper.class).select(0);
Page page=PageHelper.getPage();//查询之后调用，可以获得总的数据，这个在查询之后必须调用
```
maven引用：

---

加入依赖

```
<dependency>  
    <groupId>com.bitian.db</groupId>  
    <artifactId>mybatis-helper</artifactId>  
    <version>0.0.1</version>
</dependency> 
```

## 3. DbUtil 与 SQL DSL 使用方法

`DbUtil` 提供了一套免写 XML 即可执行动态 SQL 的便捷工具，支持无缝接入 Spring 事务外加完整的面向对象 SQL DSL 构建器。

### 初始化

在使用前，你需要先通过 `SqlSessionFactory` 初始化（如果是 Spring 环境，会自动识别并参与当前事务）：

```java
DbUtil.init(sqlSessionFactory);
```

### SelectBuilder 完整 SQL DSL 构建

`SelectBuilder` 提供了一套非常优雅的链式 API 用于构建完整的 SQL，无论是多表 JOIN 还是复杂的 EXISTS 与 OR 条件层级嵌套均可支持，同时底层会自动处理预编译的防止 SQL 注入问题。

```java
// 实例化一个构建器，可通过字符串或者另一个子查询 SelectBuilder 直接构建输出列
SelectBuilder sb = new SelectBuilder()
    .select("u.id", "u.name", "d.name AS deptName")
    // 甚至可以在 select 里直接传入子查询
    .select(new SelectBuilder()
        .select("COUNT(*)").from("sys_role r").where(sub -> sub.apply("r.user_id = u.id"))
        .as("roleCount"))
    // 支持普通的表，也支持传入另一个构建器作为 FROM 子查询！
    .from(new SelectBuilder().select("id", "name").from("sys_user").where(w -> w.gt("id", 1)).as("u"))
    // JOIN 需要在后面 .on() 添加条件，支持直接写字符串或使用 lambda 嵌套 and/or多条件
    // 同样，不仅仅是字符串表名，现在甚至连 LEFT JOIN 等也可以直接传入另一个子查询！
    .leftJoin(new SelectBuilder().select("id", "status").from("sys_dept").as("d"))
        .on(w -> w.apply("u.dept_id = d.id").eq("d.status", 1))
    // .where(Consumer) 内联条件构建，流式 API 完美衔接不受泛型限制
    .where(w -> w
        .eq("u.status", 1)  // 等于
        .like("u.name", keyword, keyword != null) // 带 condition 的动态判断
        .in("u.type", Arrays.asList(1, 2, 3))
        // EXISTS 嵌套一个子查询构建器，子查询的 where 同样推荐使用 lambda
        .exists(new SelectBuilder()
            .select("1").from("sys_role r")
            .where(sub -> sub
                // 列与列的比较或者原生 SQL 片段可以使用 apply
                .apply("r.user_id = u.id")
                .eq("r.role_name", "admin")
            )
        )
        // 支持 lambda 书写 AND / OR 嵌套逻辑，保持代码清晰
        .or(sub -> sub.eq("u.type", 1).like("u.name", "vip"))
    )
    .groupBy("u.dept_id")
    // HAVING 同理支持 lambda 多条件嵌套
    .having(w -> w.apply("COUNT(*) > 1").or(sub -> sub.apply("MAX(u.status) = 2")))
    .orderByDesc("u.create_time")
    .limit(10, 0); // limit 10 offset 0

// 直接传入 DbUtil 查询，完全免写 baseSql 和 xml
List<Map<String, Object>> list = DbUtil.selectList(sb);

// 或者通过 class 指定结果类型，支持自动属性映射
List<SysUser> users = DbUtil.selectList(sb, SysUser.class);
```

### DbUtil 其它基础 API

除了完整的 DSL 构建引擎，日常的普通直接查询如果不想写繁杂的配置也可以直接通过 `DbUtil` 完成：

```java
// 传递带有占位符的 sql 以及参数 Map 列表
List<Map<String, Object>> list = DbUtil.selectList("SELECT * FROM sys_user WHERE status = #{status}", params);

// 带有实体类映射的动态查询
SysUser user = DbUtil.selectOne("SELECT * FROM sys_user LIMIT 1", SysUser.class);

// 插入、更新、删除，支持 map 与 sql 解析
int rows = DbUtil.insert("INSERT INTO sys_user(name) values(#{name})", params);
DbUtil.update("UPDATE sys_user SET status = 1 WHERE id = #{id}", params);
DbUtil.delete("DELETE FROM sys_user WHERE status = -1");
```
