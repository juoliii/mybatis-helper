# mybatis-helper
mybatis帮助插件，日志、分页、简单增删改查等
目前已经完成简单的分页插件,无入侵式
***
使用方法：

---

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


先加入repositories：


```
<repositories>  
   <repository>
		<id>maven-repo-master</id>
		<url>https://raw.github.com/juoliii/maven-repo/master/</url>
		<snapshots>
		    <enabled>true</enabled>
		    <updatePolicy>always</updatePolicy>
		</snapshots>
	</repository>
</repositories>
```
加入依赖

```
<dependency>  
    <groupId>com.bitian.db</groupId>  
    <artifactId>mybatis-helper</artifactId>  
    <version>0.0.1</version>
</dependency> 
```
