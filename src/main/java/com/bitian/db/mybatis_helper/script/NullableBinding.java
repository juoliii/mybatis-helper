package com.bitian.db.mybatis_helper.script;

import groovy.lang.Binding;
import groovy.lang.MissingPropertyException;

import java.util.Map;

/**
 * @author admin
 */
public class NullableBinding extends Binding {

    public NullableBinding() {
        super();
    }

    public NullableBinding(Map variables) {
        super(variables);
    }

    public Object getVariable(String name) {
        if (this.getVariables() == null)
            throw new MissingPropertyException(name, this.getClass());

        if (!this.getVariables().containsKey(name)) {
            throw new MissingPropertyException(name, this.getClass());
        }

        Object result = this.getVariables().get(name);
        return result;
    }

}
