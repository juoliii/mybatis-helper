package com.bitian.db.mybatis.constants

/**
 * @author admin
 */
class Constant {

    public static String templateHeader="""<%
    def sql(@groovy.transform.NamedParam def key, @groovy.transform.NamedParam String field){
        def parameters = this.class.methods.find {it.name=="sql"}.parameters
        key?field:""
    }
%>
"""

}
