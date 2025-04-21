package com.bitian.db.mybatis.plugins

import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONValidator
import com.bitian.common.dto.BaseForm
import com.bitian.common.dto.QueryGroup
import com.bitian.common.dto.QueryJoin
import com.bitian.common.dto.SubQuery
import com.bitian.common.enums.QueryConditionType
import com.bitian.common.enums.SuperQueryType
import com.bitian.common.exception.CustomException
import com.bitian.common.util.PrimaryKeyUtil
import com.bitian.db.mybatis.utils.MybatisUtil
import org.apache.commons.lang3.StringUtils
import org.apache.ibatis.cache.CacheKey
import org.apache.ibatis.executor.Executor
import org.apache.ibatis.mapping.BoundSql
import org.apache.ibatis.mapping.MappedStatement
import org.apache.ibatis.plugin.*
import org.apache.ibatis.session.ResultHandler
import org.apache.ibatis.session.RowBounds

/**
 * 高级查询插件
 * @author admin
 */
@Intercepts([
        @Signature(type = Executor.class, method = "queryCursor", args = [MappedStatement.class, Object.class, RowBounds.class]),
        @Signature(type = Executor.class, method = "query", args = [MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class]),
        @Signature(type = Executor.class, method = "query", args = [MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class])
])
public class SuperQueryInterceptor implements Interceptor {

    private Boolean enable = false

    @Override
    public void setProperties(Properties properties) {
        this.enable = Boolean.parseBoolean(properties.getOrDefault("enable", "false").toString())
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs()
        Object param = args[1]
        BaseForm form = MybatisUtil.getBaseForm(param)
        if (form != null && enable) {
            this.handleSql(form)
        }
        return invocation.proceed()
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    private void handleSql(BaseForm form) throws Exception {
        Map<String, Object> map = new HashMap<>();
        String sql = this.parseCondition(form.get_groups(), map);
        form.set_sql(sql);
        form.set_sql_data(map);
    }

    private String parseQuery(SubQuery subQuery, Map<String, Object> map) throws Exception {
        String sql = "select " + StringUtils.join(subQuery.getColumns(), ",") + " from " + subQuery.getName() + " ";
        if (subQuery.getJoins() != null && subQuery.getJoins().size() > 0) {
            for (QueryJoin join : subQuery.getJoins()) {
                sql += " left join " + join.getName() + " on " + this.parseCondition(join.getConditions(), map);
            }
        }
        sql += " where " + this.parseCondition(subQuery.getConditions(), map);
        return sql;
    }

    private String parseCondition(List<QueryGroup> conditions, Map<String, Object> map) throws Exception {
        if (conditions.size() == 0)
            return "";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < conditions.size(); i++) {
            QueryGroup group = conditions.get(i);
            StringBuffer dt = new StringBuffer();
            for (int j = 0; j < group.getDetails().size(); j++) {
                QueryGroup.QueryDetail detail = group.getDetails().get(j);
                if (detail.getValue() == null || detail.getValue().toString().length() == 0) {
                    continue;
                }
                dt.append(" " + (j == 0 ? "" : detail.getCondition().toString()) + " ");
                if (StringUtils.isNotBlank(detail.getAlias())) {
                    dt.append(" " + detail.getAlias() + ".");
                }
                if (detail.getDynamic()) {
                    dt.append("data->>'\$." + detail.getKey() + "' ");
                } else if (detail.getType() == SuperQueryType.exists) {
                    dt.append(" ");
                } else {
                    dt.append(detail.getKey() + " ");
                }

                String key = detail.getKey() + "_" + PrimaryKeyUtil.getUUID();

                switch (detail.getType()) {
                    case SuperQueryType.eq: {
                        //等于
                        if (detail.getConditionType() == QueryConditionType.specificValue) {
                            dt.append(" = #{_sql_data." + key + "}");
                            map.put(key, detail.getValue());
                        } else if (detail.getConditionType() == QueryConditionType.column) {
                            dt.append(" = " + detail.getValue());
                        } else if (detail.getConditionType() == QueryConditionType.subQuery) {
                            dt.append(" = " + this.parseQuery((SubQuery) detail.getValue(), map));
                        }
                        break;
                    }
                    case SuperQueryType.ne: {
                        //不等于
                        if (detail.getConditionType() == QueryConditionType.specificValue) {
                            dt.append(" != #{_sql_data." + key + "}");
                            map.put(key, detail.getValue());
                        } else if (detail.getConditionType() == QueryConditionType.column) {
                            dt.append(" != " + detail.getValue());
                        } else if (detail.getConditionType() == QueryConditionType.subQuery) {
                            dt.append(" != " + this.parseQuery((SubQuery) detail.getValue(), map));
                        }
                        break;
                    }
                    case SuperQueryType.in: {
                        //多值等于
                        if (detail.getConditionType() == QueryConditionType.specificValue) {
                            List<?> strs = null;
                            if (detail.getValue() instanceof List) {
                                strs = (List<?>) detail.getValue();
                            } else if (detail.getValue() instanceof String) {
                                String value = detail.getValue().toString();
                                if (StringUtils.isNotBlank(value)) {

                                    if (JSONValidator.from(value).validate()) {
                                        strs = JSONArray.parseArray(value);
                                    } else {
                                        strs = Arrays.asList(StringUtils.split(value, "\n"));
                                    }
                                }
                            }
                            dt.append(" in (");
                            for (int k = 0; k < strs.size(); k++) {
                                dt.append("#{_sql_data." + key + k + "}");
                                if (i != strs.size() - 1) {
                                    dt.append(",");
                                }
                                map.put(key + k, strs.get(k));
                            }
                            dt.append(")");
                        } else if (detail.getConditionType() == QueryConditionType.subQuery) {
                            dt.append(" in (" + this.parseQuery((SubQuery) detail.getValue(), map) + ")");
                        }
                        break;
                    }
                    case SuperQueryType.like: {
                        // like
                        dt.append(" like #{_sql_data." + key + "}");
                        map.put(key, "%" + detail.getValue() + "%");
                        break;
                    }
                    case SuperQueryType.lt: {
                        //小于
                        dt.append(" < #{_sql_data." + key + "}");
                        map.put(key, detail.getValue());
                        break;
                    }
                    case SuperQueryType.gt: {
                        //大于
                        dt.append(" > #{_sql_data." + key + "}");
                        map.put(key, detail.getValue());
                        break;
                    }
                    case SuperQueryType.lte: {
                        //小于等于
                        dt.append(" <= #{_sql_data." + key + "}");
                        map.put(key, detail.getValue());
                        break;
                    }
                    case SuperQueryType.gte: {
                        //大于等于
                        dt.append(" >= #{_sql_data." + key + "}");
                        map.put(key, detail.getValue());
                        break;
                    }
                    case SuperQueryType.exists: {
                        if (detail.getConditionType() == QueryConditionType.subQuery) {
                            dt.append(" exists (" + this.parseQuery((SubQuery) detail.getValue(), map) + ")");
                        } else {
                            throw new CustomException("参数异常");
                        }
                        break;
                    }
                }
            }
            if (dt.length() > 0) {
                sb.append(" " + (i == 0 ? "" : group.getCondition().toString()) + " (");
                sb.append(dt);
                sb.append(" ) ");
            }
        }
        return sb.toString();
    }

}
