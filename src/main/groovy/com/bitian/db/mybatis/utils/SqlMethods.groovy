package com.bitian.db.mybatis.utils

import com.bitian.common.exception.CustomException
import com.bitian.db.mybatis.dto.Entity
import com.bitian.db.mybatis.dto.EntityColumn

class SqlMethods {
    public static final String selectByPrimaryKey="selectByPrimaryKey"
    public static final String deleteByPrimaryKey="deleteByPrimaryKey"
    public static final String updateByPrimaryKeySelective="updateByPrimaryKeySelective"
    public static final String insertSelective="insertSelective"

    public static List<String> methods=[selectByPrimaryKey,deleteByPrimaryKey,updateByPrimaryKeySelective,insertSelective]

    static String genSelectByPrimaryKey(Entity entity){
        if(entity.pkColumn==null){
            throw new CustomException("require primary key column")
        }
        """
        select ${entity.columnNames().join(",")} from ${entity.tableName} where ${entity.pkColumn.column} = #{${entity.pkColumn.property}}
"""
    }

    static String genDeleteByPrimaryKey(Entity entity){
        if(entity.pkColumn==null){
            throw new CustomException("require primary key column")
        }
        """
        delete from ${entity.tableName} where ${entity.pkColumn.column} = #{${entity.pkColumn.property}}
"""
    }

    static String genUpdateByPrimaryKeySelective(Entity entity){
        if(entity.pkColumn==null){
            throw new CustomException("require primary key column")
        }
        def conditions=""
        def columns=entity.columns.findAll{!it.pk&&it.updateable}
        columns.eachWithIndex { EntityColumn entry, int i ->
            conditions+="""<% if(${entry.property}!=null) print "${entry.column}=#{${entry.property}} ${i==columns.size()-1?"":","}"%> """
        }
        assert conditions!=""
        """
        update  ${entity.tableName} set ${conditions} where ${entity.pkColumn.column} = #{${entity.pkColumn.property}}
"""
    }

    static String genInsertSelective(Entity entity){
        def conditions=""
        def values=""
        entity.columns.eachWithIndex { EntityColumn entry, int i ->
            conditions+="""<% if(${entry.property}!=null) print "${entry.column} ${i==entity.columns.size()-1?"":","}"%> """
            values+="""<% if(${entry.property}!=null) print "#{${entry.property}} ${i==entity.columns.size()-1?"":","}"%> """
        }
        assert conditions!=""
        assert values!=""
        """
        insert into  ${entity.tableName} (${conditions}) values(${values})
"""
    }

}
