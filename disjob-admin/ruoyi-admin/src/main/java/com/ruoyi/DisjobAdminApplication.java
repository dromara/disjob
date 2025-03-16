package com.ruoyi;

import cn.ponfee.disjob.admin.DisjobAdminConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动程序
 *
 * @author ruoyi
 */
@SpringBootApplication(scanBasePackageClasses = {DisjobAdminApplication.class, DisjobAdminConfiguration.class})
public class DisjobAdminApplication {

    public static void main(String[] args) {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(DisjobAdminApplication.class, args);
    }

}
