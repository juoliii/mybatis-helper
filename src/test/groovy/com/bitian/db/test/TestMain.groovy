package com.bitian.db.test


import com.bitian.db.mybatis_helper.PageHelper
import com.bitian.db.mybatis_helper.mapper.TestMapper
import org.apache.commons.dbcp.BasicDataSource
import org.apache.ibatis.builder.xml.XMLConfigBuilder
import org.apache.ibatis.mapping.Environment
import org.apache.ibatis.session.Configuration
import org.apache.ibatis.session.SqlSession
import org.apache.ibatis.session.SqlSessionFactory
import org.apache.ibatis.session.SqlSessionFactoryBuilder
import org.apache.ibatis.transaction.TransactionFactory
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory

/**
 * @author admin
 */
class TestMain {

    static void main(String[] args) {

        BasicDataSource ds=new BasicDataSource();
        ds.setUsername("root");
        ds.setPassword("asdf-123");
        ds.setUrl("jdbc:mysql://192.168.20.14:3306/test_groovy?useUnicode=true&characterEncoding=utf8");
        ds.setDriverClassName("com.mysql.jdbc.Driver");
        TransactionFactory f=new JdbcTransactionFactory();
        Environment env=new Environment("weffe", f, ds);
        XMLConfigBuilder builder=new XMLConfigBuilder(PageHelper.class.getClassLoader().getResourceAsStream("mybatis-config.xml"));
        Configuration c=builder.parse()
        c.environment=env
        SqlSessionFactory factory=new SqlSessionFactoryBuilder().build(c);
        SqlSession session=factory.openSession(true)
        println session.getMapper(TestMapper.class).selectByPrimaryKey(17)


    }
}
