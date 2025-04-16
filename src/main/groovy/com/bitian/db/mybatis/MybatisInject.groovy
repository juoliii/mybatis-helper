package com.bitian.db.mybatis


import groovy.transform.NamedParam

/**
 * @author admin
 */
class MybatisInject {

    def static sql(@NamedParam def key, @NamedParam String field){
        def parameters = this.class.methods.find {it.name=="sql"}.parameters
        key?field:""
    }

}
