package com.bitian.db.mybatis

import groovy.transform.NamedParam
import groovy.transform.NamedParams
import org.codehaus.groovy.ast.builder.AstBuilder

/**
 * @author admin
 */
class Test {
    def static sql(@NamedParam def key, @NamedParam String field){
        def parameters = this.class.methods.find {it.name=="sql"}.parameters
        key?field:""
    }

    static void main(String[] args) {
//        def sql = "SELECT u.id, r.name FROM users u left join roles r on r.id=u.rid WHERE u.id > 10"
//
//        def astBuilder = new AstBuilder()
//        def rootNode = astBuilder.buildFromString( sql)
        def abc=[a:12]
        println new Test().sql(null,"and name=#{name}")
    }
}
