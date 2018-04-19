package com.bitian.db.mybatis_helper.util;

import java.lang.reflect.Field;

import org.apache.ibatis.mapping.MappedStatement;

public class ReflectUtil {
	
	public static Object getFieldValue(Object target,String name){
		Object value=null;
		try{
			Field cls=target.getClass().getDeclaredField(name);
			cls.setAccessible(true);
			value=cls.get(target);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
		return value;
	}
	
	public static MappedStatement getFieldValueForMappedStatement(Object target){
		MappedStatement value=null;
		try{
			Field cls=target.getClass().getSuperclass().getDeclaredField("mappedStatement");
			cls.setAccessible(true);
			value=(MappedStatement) cls.get(target);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
		return value;
	}
	
	public static void setFieldValue(Object target,String name,Object value){
		try{
			Field cls=target.getClass().getDeclaredField(name);
			cls.setAccessible(true);
			cls.set(target, value);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

}
