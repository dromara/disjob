package com.ruoyi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Run after spring container initialized.
 *
 * @author Ponfee
 */
@Component
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
        LOG.info("-------------data-source-------------");
        LOG.info("dataSources: {}", dataSources);
        LOG.info("-------------data-source-------------");

        Map<String, TransactionTemplate> transactionTemplates = BeanFactoryUtils.beansOfTypeIncludingAncestors(
            applicationContext, TransactionTemplate.class, true, true
        );
        LOG.info("-------------data-source-tx-template-------------");
        LOG.info("transactionTemplates: {}", transactionTemplates);
        LOG.info("-------------data-source-tx-template-------------");
    }

}
