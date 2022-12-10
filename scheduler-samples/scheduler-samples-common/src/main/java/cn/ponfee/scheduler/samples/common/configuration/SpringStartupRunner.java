package cn.ponfee.scheduler.samples.common.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Run after spring container initialized.
 *
 * @author Ponfee
 */
//@Component
public class SpringStartupRunner implements ApplicationContextAware, ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SpringStartupRunner.class);

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        Map<String, DataSource> dataSources = BeanFactoryUtils.beansOfTypeIncludingAncestors(
            applicationContext, DataSource.class, true, true
        );
        LOG.info("\n-------------data-source-------------");
        LOG.info("dataSources: {}", dataSources);
        LOG.info("-------------data-source-------------\n");

        Map<String, TransactionTemplate> transactionTemplates = BeanFactoryUtils.beansOfTypeIncludingAncestors(
            applicationContext, TransactionTemplate.class, true, true
        );
        LOG.info("\n-------------data-source-tx-template-------------");
        LOG.info("transactionTemplates: {}", transactionTemplates);
        LOG.info("-------------data-source-tx-template-------------\n");

        Map<String, RedisTemplate> redisTemplateMap = BeanFactoryUtils.beansOfTypeIncludingAncestors(
            applicationContext, RedisTemplate.class, true, true
        );
        LOG.info("\n-------------redis-template-------------");
        LOG.info("redisTemplateMap: {}", redisTemplateMap);
        LOG.info("-------------redis-template-------------\n");

        Map<String, RestTemplate> restTemplateMap = BeanFactoryUtils.beansOfTypeIncludingAncestors(
            applicationContext, RestTemplate.class, true, true
        );
        LOG.info("\n-------------rest-template-------------");
        LOG.info("restTemplateMap: {}", restTemplateMap);
        LOG.info("-------------rest-template-------------\n");
    }

}
