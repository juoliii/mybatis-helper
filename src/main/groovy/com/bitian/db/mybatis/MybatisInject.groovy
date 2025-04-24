package com.bitian.db.mybatis


import groovy.transform.NamedParam

/**
 * @author admin
 */
class MybatisInject {

    def static sql(@NamedParam def key, @NamedParam String field){
        key?field:""
    }

}
