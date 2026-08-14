package com.huangyangwei.iknow.spike1.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spike ① 启动类。
 * scanBasePackages 覆盖 iknow-common 模块的 @Configuration（MybatisPlusConfig），
 * @MapperScan 扫描 iknow-common 模块中的 Mapper 接口 —— 验证跨模块扫描。
 */
@SpringBootApplication(scanBasePackages = "com.huangyangwei.iknow.spike1")
@MapperScan("com.huangyangwei.iknow.spike1.common.mapper")
public class Spike1Application {

    public static void main(String[] args) {
        SpringApplication.run(Spike1Application.class, args);
    }
}
