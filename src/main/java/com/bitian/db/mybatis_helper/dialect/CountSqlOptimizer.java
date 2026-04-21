package com.bitian.db.mybatis_helper.dialect;

import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SetOperationList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 使用 JSqlParser 优化 COUNT 查询 SQL。
 * <p>
 * 优化策略：
 * <ul>
 *     <li>移除 ORDER BY 子句（不影响 COUNT 结果，但会影响性能）</li>
 *     <li>移除 LIMIT / OFFSET 子句</li>
 *     <li>简单查询（无 DISTINCT / GROUP BY）：直接将 SELECT 列替换为 COUNT(*)</li>
 *     <li>复杂查询（有 DISTINCT / GROUP BY / UNION）：包装为子查询后 COUNT</li>
 * </ul>
 * 如果 JSqlParser 解析失败，会降级为简单子查询包装。
 * </p>
 */
public class CountSqlOptimizer {

    private static final Pattern MYBATIS_PARAM_PATTERN = Pattern.compile("#\\{[^}]+}");
    private static final String WRAP_TEMPLATE = "SELECT COUNT(*) FROM (%s) _dbutil_count_t";

    /**
     * 将原始查询 SQL 优化为高效的 COUNT SQL
     *
     * @param originalSql 原始查询 SQL（可包含 MyBatis #{...} 占位符）
     * @return 优化后的 COUNT SQL
     */
    public static String toCountSql(String originalSql) {
        // Step 1: 将 MyBatis #{...} 替换为 JSqlParser 兼容的命名参数 :_p0, :_p1, ...
        List<String> placeholders = new ArrayList<>();
        StringBuffer sb = new StringBuffer();
        Matcher matcher = MYBATIS_PARAM_PATTERN.matcher(originalSql);
        int idx = 0;
        while (matcher.find()) {
            placeholders.add(matcher.group());
            matcher.appendReplacement(sb, ":_p" + idx);
            idx++;
        }
        matcher.appendTail(sb);
        String parsableSql = sb.toString();

        try {
            // Step 2: 使用 JSqlParser 解析
            Statement stmt = CCJSqlParserUtil.parse(parsableSql);
            if (!(stmt instanceof Select)) {
                return wrapCount(originalSql);
            }

            Select select = (Select) stmt;

            // UNION / INTERSECT / EXCEPT：移除外层 ORDER BY 后包装
            if (select instanceof SetOperationList) {
                select.setOrderByElements(null);
                select.setLimit(null);
                select.setOffset(null);
                return wrapCount(restorePlaceholders(select.toString(), placeholders));
            }

            if (!(select instanceof PlainSelect)) {
                return wrapCount(originalSql);
            }

            PlainSelect ps = (PlainSelect) select;

            // 移除 ORDER BY 和 LIMIT / OFFSET（对 COUNT 无意义）
            ps.setOrderByElements(null);
            ps.setLimit(null);
            ps.setOffset(null);

            // 有 DISTINCT 或 GROUP BY：必须包装为子查询
            if (ps.getDistinct() != null || ps.getGroupBy() != null) {
                return wrapCount(restorePlaceholders(ps.toString(), placeholders));
            }

            // 简单查询：直接将 SELECT 列替换为 COUNT(*)
            Function countFunc = new Function();
            countFunc.setName("COUNT");
            countFunc.setAllColumns(true);
            ps.setSelectItems(Collections.<SelectItem<?>>singletonList(new SelectItem<>(countFunc)));

            return restorePlaceholders(ps.toString(), placeholders);

        } catch (Exception e) {
            // JSqlParser 解析失败，降级为简单包装
            return wrapCount(originalSql);
        }
    }

    /**
     * 简单子查询包装
     */
    private static String wrapCount(String sql) {
        return String.format(WRAP_TEMPLATE, sql);
    }

    /**
     * 将 :_pN 命名参数还原为原始的 MyBatis #{...} 占位符
     */
    private static String restorePlaceholders(String sql, List<String> placeholders) {
        String result = sql;
        for (int i = 0; i < placeholders.size(); i++) {
            result = result.replace(":_p" + i, placeholders.get(i));
        }
        return result;
    }
}
