package com.huangyangwei.iknow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * iknow-youknow 启动入口（聚合模块）：
 * scanBasePackages 覆盖 common/infra 与各业务模块；@MapperScan 注册各模块 Mapper。
 */
@SpringBootApplication(scanBasePackages = "com.huangyangwei.iknow")
@MapperScan({"com.huangyangwei.iknow.module.auth.mapper", "com.huangyangwei.iknow.module.knowledge.mapper",
        "com.huangyangwei.iknow.module.ai.mapper", "com.huangyangwei.iknow.module.feedback.mapper",
        "com.huangyangwei.iknow.module.analytics.mapper"})
public class IKnowApplication {

    public static void main(String[] args) {
        SpringApplication.run(IKnowApplication.class, args);
    }
}
