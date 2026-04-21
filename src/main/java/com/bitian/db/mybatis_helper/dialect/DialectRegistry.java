package com.bitian.db.mybatis_helper.dialect;

import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 方言注册中心，负责管理和自动检测数据库分页方言。
 * <p>
 * 内置支持以下数据库：
 * <ul>
 *     <li>MySQL / MariaDB / SQLite / H2</li>
 *     <li>PostgreSQL / KingbaseES / openGauss</li>
 *     <li>Oracle（12c+）</li>
 *     <li>SQL Server（2012+）</li>
 *     <li>达梦（DM）</li>
 * </ul>
 * <p>
 * 用户可通过 {@link #registerDialect(String, DialectFactory)} 注册自定义方言，
 * 或通过 {@link #setDialect(DialectFactory)} 直接指定方言（跳过自动检测）。
 * </p>
 */
public class DialectRegistry {

    private static final Map<String, DialectFactory> DIALECT_MAP = new ConcurrentHashMap<>();

    /**
     * 用户手动指定的方言，优先级最高
     */
    private static volatile DialectFactory currentDialect;

    /**
     * 自动检测后缓存的方言
     */
    private static volatile DialectFactory detectedDialect;

    static {
        // MySQL 及兼容数据库
        MySqlDialect mysqlDialect = new MySqlDialect();
        DIALECT_MAP.put("mysql", mysqlDialect);
        DIALECT_MAP.put("mariadb", mysqlDialect);
        DIALECT_MAP.put("sqlite", mysqlDialect);
        DIALECT_MAP.put("h2", mysqlDialect);

        // PostgreSQL 及兼容数据库
        PostgreSqlDialect pgDialect = new PostgreSqlDialect();
        DIALECT_MAP.put("postgresql", pgDialect);
        DIALECT_MAP.put("kingbasees", pgDialect);
        DIALECT_MAP.put("kingbase", pgDialect);
        DIALECT_MAP.put("opengauss", pgDialect);

        // Oracle
        DIALECT_MAP.put("oracle", new OracleDialect());

        // SQL Server
        SqlServerDialect sqlServerDialect = new SqlServerDialect();
        DIALECT_MAP.put("sql server", sqlServerDialect);
        DIALECT_MAP.put("microsoft sql server", sqlServerDialect);

        // 达梦
        DIALECT_MAP.put("dm", new DmDialect());
        DIALECT_MAP.put("dm dbms", new DmDialect());
    }

    /**
     * 注册自定义数据库方言
     *
     * @param dbType  数据库类型标识（不区分大小写，如 "gbase"、"oscar" 等）
     * @param dialect 方言实现
     */
    public static void registerDialect(String dbType, DialectFactory dialect) {
        DIALECT_MAP.put(dbType.toLowerCase(), dialect);
    }

    /**
     * 手动指定方言（跳过自动检测）
     *
     * @param dialect 方言实现，传 null 可清除手动设置并恢复自动检测
     */
    public static void setDialect(DialectFactory dialect) {
        currentDialect = dialect;
    }

    /**
     * 获取当前方言。
     * 优先级：手动指定 > 已缓存的自动检测结果 > 实时自动检测
     *
     * @param sqlSessionFactory 用于自动检测数据库类型
     * @return 分页方言
     * @throws RuntimeException 如果无法检测数据库类型或不支持的数据库
     */
    public static DialectFactory getDialect(SqlSessionFactory sqlSessionFactory) {
        // 1. 手动指定优先
        if (currentDialect != null) {
            return currentDialect;
        }

        // 2. 已缓存的检测结果
        if (detectedDialect != null) {
            return detectedDialect;
        }

        // 3. 自动检测
        synchronized (DialectRegistry.class) {
            if (detectedDialect != null) {
                return detectedDialect;
            }
            detectedDialect = autoDetect(sqlSessionFactory);
            return detectedDialect;
        }
    }

    /**
     * 通过 JDBC DatabaseMetaData 自动检测数据库类型并匹配方言
     */
    private static DialectFactory autoDetect(SqlSessionFactory sqlSessionFactory) {
        String productName;
        try {
            DataSource dataSource = sqlSessionFactory.getConfiguration()
                    .getEnvironment().getDataSource();
            Connection connection = null;
            try {
                connection = dataSource.getConnection();
                DatabaseMetaData metaData = connection.getMetaData();
                productName = metaData.getDatabaseProductName();
            } finally {
                if (connection != null) {
                    connection.close();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to detect database type for pagination dialect. " +
                    "Please use DialectRegistry.setDialect() to specify manually.", e);
        }

        if (productName == null || productName.isEmpty()) {
            throw new RuntimeException("Database product name is empty. " +
                    "Please use DialectRegistry.setDialect() to specify manually.");
        }

        String lowerName = productName.toLowerCase();

        // 精确匹配
        DialectFactory dialect = DIALECT_MAP.get(lowerName);
        if (dialect != null) {
            return dialect;
        }

        // 模糊匹配：遍历已注册的 key，看 productName 是否包含
        for (Map.Entry<String, DialectFactory> entry : DIALECT_MAP.entrySet()) {
            if (lowerName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        throw new RuntimeException("Unsupported database: " + productName + ". " +
                "Please use DialectRegistry.registerDialect() to register a custom dialect, " +
                "or use DialectRegistry.setDialect() to specify manually.");
    }
}
