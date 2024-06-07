package com.ruoyi;

import cn.ponfee.disjob.admin.DisjobAdminConfiguration;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 启动程序
 *
 * @author ruoyi
 */
@SpringBootApplication(
    exclude = {DataSourceAutoConfiguration.class, DruidDataSourceAutoConfigure.class},
    scanBasePackageClasses = {DisjobAdminApplication.class, DisjobAdminConfiguration.class}
)
public class DisjobAdminApplication {
    public static void main(String[] args) {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(DisjobAdminApplication.class, args);
    }

}
