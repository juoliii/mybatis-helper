package com.bitian.db.mybatis.utils

import com.bitian.common.dto.BaseForm
import groovy.transform.CompileStatic

/**
 * @author admin
 */
@CompileStatic
class MybatisUtil {

    static BaseForm getBaseForm(def param){
        BaseForm form = null
        if (param instanceof BaseForm) {
            form = param
        } else if (param instanceof Map) {
            for (Object value : ((Map<?, ?>) param).values()) {
                if (value instanceof BaseForm) {
                    form = value
                    break
                }
            }
        }
        return form
    }
}
