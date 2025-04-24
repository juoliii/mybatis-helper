package com.bitian.db.mybatis.dto

import groovy.transform.CompileStatic
import org.apache.ibatis.reflection.MetaObject
import org.apache.ibatis.scripting.xmltags.DynamicContext

/**
 * @author admin
 */
@CompileStatic
class ContextMap extends HashMap {
    private final MetaObject parameterMetaObject
    private final boolean fallbackParameterObject

    ContextMap(MetaObject parameterMetaObject, boolean fallbackParameterObject) {
        this.parameterMetaObject = parameterMetaObject;
        this.fallbackParameterObject = fallbackParameterObject;
    }

    @Override
    Object get(Object key) {
        if (super.containsKey(key)) {
            return super.get(key);
        }

        Object param=super.get(DynamicContext.PARAMETER_OBJECT_KEY);
        if(param instanceof Map){
            Map map= (Map) param;
            if(map.containsKey(key)){
                return map.get(key);
            }
        }

        if (parameterMetaObject == null) {
            return null;
        }

        if (fallbackParameterObject && !parameterMetaObject.hasGetter(key as String)) {
            return parameterMetaObject.getOriginalObject();
        }
        // issue #61 do not modify the context when reading
        return parameterMetaObject.getValue(key as String);
    }

    @Override
    boolean containsKey(Object key) {
        if(super.containsKey(key)){
            return true
        }

        Object param=super.get(DynamicContext.PARAMETER_OBJECT_KEY)
        if(param instanceof Map){
            if(param.containsKey(key)){
                return true
            }
        }

        if (parameterMetaObject == null)
            return false


        if(parameterMetaObject.hasGetter(key as String))
            return true

        return fallbackParameterObject
    }
}
