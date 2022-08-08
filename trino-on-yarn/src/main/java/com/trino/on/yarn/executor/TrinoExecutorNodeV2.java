package com.trino.on.yarn.executor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import com.trino.on.yarn.entity.JobInfo;

import java.util.List;

import static com.trino.on.yarn.constant.Constants.*;

public class TrinoExecutorNodeV2 {

    public static final String PATH = "./";
    protected static final List<String> trinoEnv = CollUtil.newArrayList();
    protected JobInfo jobInfo;

    public TrinoExecutorNodeV2(JobInfo jobInfo) {
        this.jobInfo = jobInfo;
    }

    public String toCmd() {
        //获取JAVA11_HOME
        String java11Home = System.getenv("JAVA11_HOME");
        if (StrUtil.isBlank(java11Home)) {
            java11Home = jobInfo.getJdk11Home();
        }
        //写入运行参数
        put(java11Home + "/bin/java");
        put("-cp");
        String lib = ".:" + jobInfo.getLibPath();
        if (!StrUtil.endWith(lib, "*")) {
            put(lib + "/*");
        }

        String jvms = StrUtil.format(TRINO_JVM_CONTENT, jobInfo.getAmMemory());
        for (String jvm : StrUtil.split(jvms, StrPool.LF)) {
            put(jvm);
        }
        putEnv(LOG_LEVELS_FILE, "./log.properties");
        putEnv(CONFIG, "./config.properties");
        putEnv(LOG_OUTPUT_FILE, "./server.log");
        putEnv("log.enable-console=true");
        String catalog = jobInfo.getCatalog();
        //压缩包可能存在多一层嵌套问题
        if (jobInfo.isHdfsOrS3()) {
            catalog = PATH + JAVA_TRINO_CATALOG_PATH;
            if (!jobInfo.isLocal()) {
                catalog = PATH + JAVA_TRINO_CATALOG_PATH + "/" + JAVA_TRINO_CATALOG_PATH;
            }
        } else {
            throw new RuntimeException("catalog not found");
        }
        String nodes = StrUtil.format(TRINO_NODE_CONTENT, StrUtil.uuid(), PATH, catalog, jobInfo.getPluginPath());
        for (String node : StrUtil.split(nodes, StrPool.LF)) {
            putEnv(node);
        }

        return StrUtil.join(" ", trinoEnv);
    }

    protected void putEnv(String k, String v) {
        trinoEnv.add("-D" + k + "=" + v);
    }

    protected void putEnv(String kv) {
        trinoEnv.add("-D" + kv);
    }

    protected void put(String kv) {
        trinoEnv.add(kv);
    }
}
