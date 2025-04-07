package com.bitian.db.mybatis_helper.util;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.scripting.xmltags.DynamicContext;

import java.util.HashMap;
import java.util.Map;

/**
 * @author admin
 */
public class ContextMap extends HashMap {
    private final MetaObject parameterMetaObject;
    private final boolean fallbackParameterObject;

    public ContextMap(MetaObject parameterMetaObject, boolean fallbackParameterObject) {
        this.parameterMetaObject = parameterMetaObject;
        this.fallbackParameterObject = fallbackParameterObject;
    }

    @Override
    public Object get(Object key) {
        String strKey = (String) key;
        if (super.containsKey(strKey)) {
            return super.get(strKey);
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

        if (fallbackParameterObject && !parameterMetaObject.hasGetter(strKey)) {
            return parameterMetaObject.getOriginalObject();
        }
        // issue #61 do not modify the context when reading
        return parameterMetaObject.getValue(strKey);
    }

    @Override
    public boolean containsKey(Object key) {
        String strKey = (String) key;
        if(super.containsKey(key)){
            return true;
        }

        Object param=super.get(DynamicContext.PARAMETER_OBJECT_KEY);
        if(param instanceof Map){
            Map map= (Map) param;
            if(map.containsKey(key)){
                return true;
            }
        }

        if (parameterMetaObject == null) {
            return false;
        }
        return parameterMetaObject.hasGetter(strKey);
    }
}
