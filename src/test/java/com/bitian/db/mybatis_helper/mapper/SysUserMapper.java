package com.bitian.db.mybatis_helper.mapper;

import com.bitian.db.mybatis_helper.entity.SysUser;
import com.bitian.db.mybatis_helper.tk.SelectByQueryMapper;
import tk.mybatis.mapper.common.Mapper;

public interface SysUserMapper extends Mapper<SysUser>, SelectByQueryMapper<SysUser> {
}
