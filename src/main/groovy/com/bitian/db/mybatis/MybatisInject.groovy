package com.bitian.db.mybatis

import com.bitian.db.mybatis_helper.PageHelper
import groovy.transform.NamedParam
import org.apache.ibatis.builder.xml.XMLConfigBuilder
import org.apache.ibatis.mapping.Environment
import org.apache.ibatis.mapping.MappedStatement
import org.apache.ibatis.session.Configuration
import org.apache.ibatis.transaction.TransactionFactory
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory

/**
 * @author admin
 */
class MybatisInject {

    static init(){
//        MetaMethod method=Configuration.metaClass.pickMethod("addMappedStatement",MappedStatement.class)
//        println method.getName()
//        Configuration.metaClass.addMappedStatement={
//            MappedStatement ms->{
//                method.invoke()
//            }
//        }
//        Configuration.metaClass.invokeMethod = { name, args ->
//            println "replaced: Called ${name} with ${args}"
//            println args.length
//            println args.class
//            println args.metaClass
//            delegate.class.metaClass.getMetaMethod(name, args)?.invoke(delegate, args)
//        }
    }
    def static sql(@NamedParam def key, @NamedParam String field){
        def parameters = this.class.methods.find {it.name=="sql"}.parameters
        key?field:""
    }

    static void main(String[] args) {
    }
}
