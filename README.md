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
