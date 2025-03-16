package cn.ponfee.disjob.alert.sms.configuration;


import org.dromara.sms4j.core.factory.SmsFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.context.event.ContextRefreshedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class CreateSmsBlend {
    private static final Logger logger = LoggerFactory.getLogger(CreateSmsBlend.class);

    @Autowired
    ReadConfig readConfig;

    @EventListener
    public void init(ContextRefreshedEvent event) {
        // 自动创建所有配置的blend实例
        SmsFactory.createSmsBlend(readConfig);
        logger.info("create the smsBlend success");
    }

}
