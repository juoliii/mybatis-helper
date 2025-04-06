package com.bitian.db.mybatis_helper.util;

import org.apache.ibatis.reflection.MetaObject;

import java.util.HashMap;

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

        if (parameterMetaObject == null) {
            return null;
        }

        if (fallbackParameterObject && !parameterMetaObject.hasGetter(strKey)) {
            return parameterMetaObject.getOriginalObject();
        }
        // issue #61 do not modify the context when reading
        return parameterMetaObject.getValue(strKey);
    }
}
