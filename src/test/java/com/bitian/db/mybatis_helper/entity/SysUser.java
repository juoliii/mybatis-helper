package com.bitian.db.mybatis_helper.entity;

import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author admin
 */
@Data
@Table(name="sys_user")
public class SysUser {
    @Id
    @GeneratedValue
    private Long id;
    private String username;
    private String password;
    private Integer age;
}
