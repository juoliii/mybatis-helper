package com.bitian.db.test

import com.bitian.db.mybatis.MybatisInject
import com.bitian.db.mybatis_helper.PageHelper
import org.apache.commons.dbcp.BasicDataSource
import org.apache.ibatis.builder.xml.XMLConfigBuilder
import org.apache.ibatis.mapping.Environment
import org.apache.ibatis.session.Configuration
import org.apache.ibatis.transaction.TransactionFactory
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory

/**
 * @author admin
 */
class TestMain {

    static {
        MybatisInject.init();
    }

    static void main(String[] args) {

        def proxy=ProxyMetaClass.getInstance(Configuration.class)
        proxy.interceptor=new Interceptor() {
            @Override
            Object beforeInvoke(Object object, String methodName, Object[] arguments) {
                println methodName
                return null
            }

            @Override
            Object afterInvoke(Object object, String methodName, Object[] arguments, Object result) {
                return result
            }

            @Override
            boolean doInvoke() {
                return false
            }
        }
        Configuration.metaClass=proxy
        BasicDataSource ds=new BasicDataSource();
        ds.setUsername("root");
        ds.setPassword("asdf-123");
        ds.setUrl("jdbc:mysql://192.168.20.14:3306/test_groovy?useUnicode=true&characterEncoding=utf8");
        ds.setDriverClassName("com.mysql.jdbc.Driver");
        TransactionFactory f=new JdbcTransactionFactory();
        Environment en=new Environment("weffe", f, ds);
        XMLConfigBuilder builder=new XMLConfigBuilder(PageHelper.class.getClassLoader().getResourceAsStream("mybatis-config.xml"));
        Configuration c=builder.parse();


    }
}
