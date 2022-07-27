package com.trino.on.yarn.executor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.*;
import java.util.List;

public class TrinoJdbc {
    public static final String JDBC_TRINO = "jdbc:trino://{}:{}/hive";
    protected static final Log LOG = LogFactory.getLog(TrinoJdbc.class);

    private static Connection connection = null;
    private static Statement statement = null;
    private static ResultSet resultSet = null;

    public static void run(String ip, Integer port, String email, String sqls) throws ClassNotFoundException, SQLException {


        try {
            Class.forName("io.trino.jdbc.TrinoDriver");
            if (StrUtil.isEmpty(email)) {
                email = "root";
            }
            connection = DriverManager.getConnection(StrUtil.format(JDBC_TRINO, ip, port), email, null);

            for (String sql : StrUtil.split(sqls, ";")) {
                LOG.warn("execute sql:" + sql);
                execute(connection, sql);
            }
        } finally {
            IoUtil.close(connection);
        }
    }

    public static void execute(Connection connection, String sql) throws SQLException {
        try {
            IoUtil.close(resultSet);
            IoUtil.close(statement);
            statement = connection.createStatement();
            boolean execute = statement.execute(sql);
            // TODO: 2022/7/27 这里可能会出现问题,暂不启用
            printResults(sql, execute);
        } catch (SQLException e) {
            String message = e.getMessage();
            if (message.contains("initializing")) {
                ThreadUtil.sleep(500);
                execute(connection, sql);
            } else {
                throw e;
            }
        } finally {
            IoUtil.close(resultSet);
            IoUtil.close(statement);
        }
    }

    private static void printResults(String sql, boolean execute) throws SQLException {
        if (execute) {
            resultSet = statement.getResultSet();
            if (statement.getUpdateCount() < 0 && resultSet.getMetaData().getColumnCount() <= 0) {
                LOG.info("current result is a ResultSet Object , but there are no more results :" + sql);
            } else {
                printResults();
            }
        }
    }

    private static void printResults() throws SQLException {
        List<String> columns = CollUtil.newArrayList();
        try {
            long start = System.currentTimeMillis();

            ResultSetMetaData md = resultSet.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                columns.add(md.getColumnName(i));
            }

            System.out.println(StrUtil.join("  |  ", columns));
            int j = 0;
            while (resultSet.next() && j < 10) {//遍历查询结果，获取每一列字符串长度的最大值，保存在数组中
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= columns.size(); i++) {
                    sb.append(resultSet.getObject(i));
                    if (i != columns.size()) {
                        sb.append("  |  ");
                    }
                }
                System.out.println(sb);
                j++;
            }
            System.out.println("耗时：" + (System.currentTimeMillis() - start) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}