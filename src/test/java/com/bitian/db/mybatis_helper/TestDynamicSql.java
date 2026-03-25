package com.bitian.db.mybatis_helper;

import org.apache.ibatis.mapping.*;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.*;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import java.util.*;

public class TestDynamicSql {
    public static void main(String[] args) throws Exception {
        BasicDataSource ds=new BasicDataSource();
        ds.setUsername("root");
        ds.setPassword("1qaz2wsx");
        ds.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        ds.setDriverClassName("org.h2.Driver");
        TransactionFactory f=new JdbcTransactionFactory();
        Environment en=new Environment("test", f, ds);
        Configuration configuration = new Configuration(en);
        configuration.setMapUnderscoreToCamelCase(true);
        
        SqlSessionFactory factory=new SqlSessionFactoryBuilder().build(configuration);
        SqlSession session = factory.openSession();
        
        session.getConnection().createStatement().execute("CREATE TABLE sys_user (id INT, user_name VARCHAR(50))");
        session.getConnection().createStatement().execute("INSERT INTO sys_user VALUES (1, 'john_doe')");
        
        Class<?> resultType = SysUser.class;
        String msId = "DbUtil_dynamic_select_" + resultType.getName();
        
        LanguageDriver languageDriver = configuration.getDefaultScriptingLanguageInstance();
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, "<script>${_dbutil_sql}</script>", Map.class);
        
        ResultMap resultMap = new ResultMap.Builder(configuration, msId + "-Inline", resultType, new ArrayList<>(), true).build();
        
        MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, msId, sqlSource, SqlCommandType.SELECT)
                .resultMaps(Collections.singletonList(resultMap));
        
        configuration.addMappedStatement(statementBuilder.build());
        
        Map<String, Object> params = new HashMap<>();
        params.put("_dbutil_sql", "SELECT id, user_name FROM sys_user WHERE id = #{id}");
        params.put("id", 1);
        
        List<SysUser> result = session.selectList(msId, params);
        System.out.println("Result size: " + result.size());
        if (!result.isEmpty()) {
            System.out.println("Username: " + result.get(0).getUserName());
        }
        session.close();
    }
    
    public static class SysUser {
        private Integer id;
        private String userName;
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
    }
}
