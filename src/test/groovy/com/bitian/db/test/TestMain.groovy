package com.bitian.db.test

import com.bitian.db.mybatis_helper.mapper.TestMapper
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.select.PlainSelect
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

    static void query(){
        BasicDataSource ds=new BasicDataSource();
        ds.setUsername("root");
        ds.setPassword("asdf-123");
        ds.setUrl("jdbc:mysql://192.168.20.14:3306/test_groovy?useUnicode=true&characterEncoding=utf8");
        ds.setDriverClassName("com.mysql.jdbc.Driver");
        TransactionFactory f=new JdbcTransactionFactory();
        Environment env=new Environment("weffe", f, ds);
        XMLConfigBuilder builder=new XMLConfigBuilder(TestMain.class.getClassLoader().getResourceAsStream("mybatis-config.xml"));
        Configuration c=builder.parse()
        c.environment=env
        SqlSessionFactory factory=new SqlSessionFactoryBuilder().build(c);
        SqlSession session=factory.openSession(true)
        println session.getMapper(TestMapper.class).selectByPrimaryKey(17)
    }

    static void main(String[] args) {
        String sql="""
        select u.*,(select name from users_name where id=u.id) from user u left join roles r on r.id=u.rid
        where u.name='wef' and r.name='wef' and exists(select 1 from user_role ur where ur.uid=u.id)
        --group by u.id
        order by r.id ,?
"""
        PlainSelect select=CCJSqlParserUtil.parse(sql)
        if(select.orderByElements)
            println select.orderByElements.count {it.expression.toString().contains("?")}
        println select.orderByElements

    }
}
