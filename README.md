# mybatis-helper
## 1. 功能
- 分页
- `DbUtil` 提供了一套免写 XML 即可执行动态 SQL 的便捷工具，支持无缝接入 Spring 事务外加完整的面向对象 SQL DSL 构建器。
- 实体对象 CRUD（通过 JPA 注解 `@Table`、`@Column`、`@Id`、`@Transient` 自动解析映射，免写 SQL 直接传入实体对象进行 insert / update / delete）
- 主键生成策略（`@KeySql` 注解支持数据库自增 IDENTITY、UUID、SQL 序列、自定义生成器，插入后自动回填主键值）
- 非 Spring 环境下的轻量级事务管控（基于 ThreadLocal 自动维护同线程事务边界）

## 2.使用方法

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

## 3. 功能介绍

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
    .orderByDesc("u.create_time");

// 直接传入 DbUtil 查询，完全免写 baseSql 和 xml
List<Map<String, Object>> list = DbUtil.selectList(sb);

// 或者通过 class 指定结果类型，支持自动属性映射
List<SysUser> users = DbUtil.selectList(sb, SysUser.class);
```

### 编译期强类型 SQL DSL (APT 自动生成元模型)

除了灵活的原生字符串拼接外，`SelectBuilder` 现在更**原生支持高度类型安全的强类型元模型模式**，让它完全可以媲美 QueryDSL/jOOQ 等顶尖强类型框架！

本项目内建了 Annotation Processor (APT)。在编译期，它会自动拦截 `tk.mybatis` 等框架常用的 `@Table` 注解，为您零配置自动生成基于 `Q` 打头的增强表描述元模型（例如 `QSysUser.java`）。您无需担心拼错数据库列名或者缺乏代码格式重构支持：

```java
// 使用编译自动生成的 QSysUser（内部已包含所有列定义的强类型常量）
QSysUser u = new QSysUser("u");
QSysDept d = new QSysDept("d");

SelectBuilder sb = new SelectBuilder()
    // 独占特性的 u.all 可将自身包含的所有列一网打尽，无需手写 u.*，且支持混用 d.name.as("别名")
    .select(u.all) 
    .select(d.name.as("deptName"))
    .from(u)
    // 底层智能识别类型：当两个强类型列比对时，自动生成原生列判断 sql（且不会有 ? 的预编译占位符）
    .leftJoin(d).on(w -> w.eq(u.deptId, d.id))
    // 若和普通数据值对比，则自动提取出防 SQL 注入的占位符模式（如 .status = #{_qw_param_X}）
    .where(w -> w.eq(u.status, 1).like(u.username, "vip"))
    .groupBy(u.deptId)
    .orderByDesc(u.createTime);

// 执行产出的效果和原生字符串完全一致，但您享受到了 100% 的编译期排错能力和 IDE 代码补全！
List<Map<String, Object>> list = DbUtil.selectList(sb);
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

### 非 Spring 环境下的事务支持

在非 Spring 环境下，默认每次调用 CRUD 方法都会自动开启新的 Session 并自动提交。如果你需要在同一个事务中执行多次操作，可以使用 `DbUtil.executeInTransaction`：

```java
DbUtil.executeInTransaction(() -> {
    // 以下操作将在同一个底层 SqlSession 事务中执行
    DbUtil.insert(user1);
    DbUtil.insert(user2);
    DbUtil.update(user3);
    // 如果发生异常，事务会自动回滚；全部成功则自动 commit
});

// 带返回值的事务操作
boolean success = DbUtil.executeInTransaction(() -> {
    DbUtil.insert(user1);
    return true; 
});
```
> 注：如果在 Spring 环境中（即上下文中检测到 ApplicationContext），该方法会自动安全降级，直接执行传入的逻辑，交由 Spring `@Transactional` 统一接管事务。

### 实体对象 CRUD（免写 SQL）

`DbUtil` 支持直接传入实体对象进行增删改操作，通过 JPA 注解自动解析表名、列名和主键，无需手写 SQL。

#### 实体类定义

实体类通过以下 JPA 注解标记映射关系（兼容 tk-mapper 风格）：

| 注解 | 位置 | 说明 |
|------|------|------|
| `@Table(name = "...")` | 类 | 指定表名，缺省则将类名转为下划线命名 |
| `@Id` | 字段 | 标记主键字段 |
| `@Column(name = "...")` | 字段 | 指定列名，缺省则将字段名转为下划线命名 |
| `@Transient` | 字段 | 标记非数据库字段，不参与任何 SQL 操作 |
| `@KeySql(...)` | 字段 | 指定主键生成策略（配合 `@Id` 使用） |

```java
@Table(name = "sys_user")
public class SysUser {
    @Id
    @KeySql(type = IdGenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name")
    private String userName;

    private String email;       // 自动映射为 email 列

    @Transient
    private String tempField;   // 不参与数据库操作
}
```

#### 插入

```java
SysUser user = new SysUser();
user.setUserName("test");
user.setEmail("test@example.com");

// 插入所有字段（包括 null 值的字段）
DbUtil.insert(user);

// 仅插入非 null 字段（Selective）
DbUtil.insertSelective(user);
```

#### 更新

```java
SysUser user = new SysUser();
user.setId(1L);
user.setEmail("new@example.com");

// 按主键更新所有字段（null 值的字段也会被 SET 为 null）
DbUtil.update(user);

// 按主键仅更新非 null 的字段（Selective，推荐）
DbUtil.updateSelective(user);
```

#### 删除

```java
SysUser user = new SysUser();
user.setId(1L);

// 按主键删除
DbUtil.delete(user);
```

#### 配合 QueryWrapper 的实体操作

```java
// 按条件更新（不依赖主键，WHERE 由 QueryWrapper 指定）
SysUser updateEntity = new SysUser();
updateEntity.setStatus(0);
QueryWrapper wrapper = new QueryWrapper();
wrapper.eq("dept_id", 5);
DbUtil.updateByQuery(updateEntity, wrapper);
// 生成：UPDATE sys_user SET status = 0 WHERE dept_id = 5

// 按条件删除
DbUtil.deleteByQuery(SysUser.class, new QueryWrapper().lt("create_time", "2024-01-01"));
// 生成：DELETE FROM sys_user WHERE create_time < '2024-01-01'
```

### 主键生成策略

通过 `@KeySql` 注解指定主键生成方式，插入后自动将生成的主键值回填到实体对象。

| 策略 | 说明 | 适用场景 |
|------|------|----------|
| `NONE` | 不自动生成，用户手动赋值（默认） | 业务主键 |
| `IDENTITY` | 数据库自增，插入后回填 | MySQL AUTO_INCREMENT、PostgreSQL SERIAL |
| `UUID` | 应用层生成 32 位无分隔符 UUID | 分布式系统、无序主键 |
| `SQL` | 执行指定 SQL 获取主键值 | Oracle 序列、PostgreSQL 序列 |
| `CUSTOM` | 自定义生成器 | 雪花算法、自定义编号规则 |

#### IDENTITY — 数据库自增

```java
@Id
@KeySql(type = IdGenerationType.IDENTITY)
private Long id;

// 使用
SysUser user = new SysUser();
user.setUserName("test");
DbUtil.insert(user);
System.out.println(user.getId()); // 输出数据库生成的自增 ID，如：1001
```

> IDENTITY 策略下，`id` 列不会出现在 INSERT 语句的列列表中，由数据库自增生成，插入完成后自动回填到实体对象。

#### UUID — 应用层自动生成

```java
@Id
@KeySql(type = IdGenerationType.UUID)
private String id;

// 使用
SysUser user = new SysUser();
user.setUserName("test");
DbUtil.insert(user);
System.out.println(user.getId()); // 输出如：a1b2c3d4e5f6...（32位）
```

#### SQL — 执行 SQL 获取主键

适用于 Oracle 序列等场景，插入前先执行指定 SQL 获取主键值：

```java
// Oracle 序列
@Id
@KeySql(type = IdGenerationType.SQL, sql = "SELECT sys_user_seq.nextval FROM DUAL")
private Long id;

// PostgreSQL 序列
@Id
@KeySql(type = IdGenerationType.SQL, sql = "SELECT nextval('sys_user_seq')")
private Long id;
```

#### CUSTOM — 自定义生成器

实现 `GenId` 接口编写自定义生成逻辑：

```java
// 1. 实现 GenId 接口
public class SnowflakeGenId implements GenId<Long> {
    @Override
    public Long genId() {
        return SnowflakeIdWorker.nextId(); // 你的雪花算法实现
    }
}

// 2. 在实体类中使用
@Id
@KeySql(type = IdGenerationType.CUSTOM, genId = SnowflakeGenId.class)
private Long id;
```

> 自定义生成器类必须提供无参构造函数。

### 分页查询

`DbUtil` 内置了基于数据库方言的分页查询支持，通过 `selectPage` 系列方法即可实现分页，返回 `PageResult` 对象。

**自动方言检测**：无需手动配置，初始化时会通过 JDBC `DatabaseMetaData` 自动识别数据库类型并匹配对应的分页方言。

内置支持的数据库：

| 数据库 | 分页语法 |
|--------|---------|
| MySQL / MariaDB / SQLite / H2 | `LIMIT ... OFFSET ...` |
| PostgreSQL / KingbaseES / openGauss | `LIMIT ... OFFSET ...` |
| Oracle（12c+） | `OFFSET ... ROWS FETCH NEXT ... ROWS ONLY` |
| SQL Server（2012+） | `OFFSET ... ROWS FETCH NEXT ... ROWS ONLY` |
| 达梦（DM） | `LIMIT ... OFFSET ...` |

#### 基本用法

```java
// 直接 SQL 分页，pageNumber 从 1 开始
PageResult<SysUser> page = DbUtil.selectPage(
    "SELECT * FROM sys_user WHERE status = 1",
    SysUser.class, 1, 10  // 第1页，每页10条
);

// 带参数的分页查询
Map<String, Object> params = new HashMap<>();
params.put("status", 1);
PageResult<Map<String, Object>> page = DbUtil.selectPage(
    "SELECT * FROM sys_user WHERE status = #{status}",
    params, 1, 10
);
```

#### 配合 SelectBuilder 分页

```java
SelectBuilder sb = SelectBuilder.n()
    .select("*")
    .from("sys_user")
    .where(w -> w.eq("status", 1))
    .orderByDesc("create_time");

PageResult<SysUser> page = DbUtil.selectPage(sb, SysUser.class, 2, 20);
```

#### 配合 QueryWrapper 分页

```java
QueryWrapper wrapper = new QueryWrapper();
wrapper.eq("status", 1).like("name", "test");

PageResult<SysUser> page = DbUtil.selectPageByQuery(
    "SELECT * FROM sys_user", wrapper, SysUser.class, 1, 10
);
```

#### PageResult 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `list` | `List<T>` | 当前页数据列表 |
| `total` | `long` | 总记录数 |
| `pageNum` | `int` | 当前页码（从 1 开始） |
| `pageSize` | `int` | 每页条数 |
| `pages` | `int` | 总页数 |

```java
PageResult<SysUser> page = DbUtil.selectPage(sql, SysUser.class, 1, 10);

List<SysUser> users = page.getList();    // 当前页数据
long total = page.getTotal();               // 总记录数
int pages = page.getPages();      // 总页数
boolean hasNext = page.isHasNext();           // 是否有下一页
boolean hasPrev = page.isHasPrevious();       // 是否有上一页
```

#### 自定义方言扩展

如果使用的数据库不在内置支持列表中，可以通过以下方式扩展：

```java
// 方式一：注册自定义方言（按数据库产品名匹配）
DialectRegistry.registerDialect("gbase", new DialectFactory() {
    @Override
    public String buildPaginationSql(String sql, int offset, int limit) {
        return sql + " LIMIT " + limit + " OFFSET " + offset;
    }

    @Override
    public String buildCountSql(String sql) {
        return "SELECT COUNT(*) FROM (" + sql + ") _t";
    }
});

// 方式二：直接指定方言（跳过自动检测）
DialectRegistry.setDialect(new MySqlDialect());
```
