package com.bitian.db.mybatis_helper.spring;

import com.bitian.db.mybatis_helper.util.DbUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnBean(SqlSessionFactory.class)
@AutoConfigureAfter(name={"org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"})
public class DbUtilAutoConfiguration implements InitializingBean {

    private final List<SqlSessionFactory> sqlSessionFactoryList;

    public DbUtilAutoConfiguration(List<SqlSessionFactory> sqlSessionFactoryList) {
        this.sqlSessionFactoryList = sqlSessionFactoryList;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 将 Spring 管理的 SqlSessionTemplate 注入到 DbUtil 中，
        // 这样 DbUtil 执行 SQL 就可以自动参与到 Spring 的 @Transactional 事务中。
        if(this.sqlSessionFactoryList!=null && !this.sqlSessionFactoryList.isEmpty())
            DbUtil.init(this.sqlSessionFactoryList.get(0));
    }
}
