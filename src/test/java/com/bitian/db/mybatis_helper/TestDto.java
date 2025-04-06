package com.bitian.db.mybatis_helper;

import com.bitian.common.dto.BaseForm;

/**
 * @author admin
 */

public class TestDto extends BaseForm {
    String abc;

    public String getAbc(){
        return abc;
    }

    public void setAbc(String param){
        this.abc=param;
    }
}
