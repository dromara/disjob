package cn.ponfee.disjob.alert.sms.configuration;


import org.dromara.sms4j.aliyun.config.AlibabaConfig;
import org.dromara.sms4j.cloopen.config.CloopenConfig;
import org.dromara.sms4j.emay.config.EmayConfig;
import org.dromara.sms4j.huawei.config.HuaweiConfig;
import org.dromara.sms4j.jdcloud.config.JdCloudConfig;
import org.dromara.sms4j.provider.config.BaseConfig;
import org.dromara.sms4j.core.datainterface.SmsReadConfig;

import org.dromara.sms4j.tencent.config.TencentConfig;
import org.dromara.sms4j.unisms.config.UniConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.beans.BeanUtils;

import java.util.*;
import java.util.function.Supplier;


@Component
public class ReadConfig implements SmsReadConfig {
    @Autowired
    private SmsAlertSenderProperties smsAlertSenderProperties;

    @Override
    public BaseConfig getSupplierConfig(String configId) {
        Map<String, SmsBaseConfig> blends = smsAlertSenderProperties.getBlends();
        if (!blends.containsKey(configId)) {
            return null;
        }
        SmsBaseConfig baseConfig = blends.get(configId);
        return ConfigFactory.createConfig(configId, baseConfig);
    }

    @Override
    public List<BaseConfig> getSupplierConfigList() {
        List<BaseConfig> configList = new ArrayList<>();
        Map<String, SmsBaseConfig> blends = smsAlertSenderProperties.getBlends();
        for (Map.Entry<String, SmsBaseConfig> entry : blends.entrySet()) {
            String configId = entry.getKey();
            configList.add(getSupplierConfig(configId));
        }
        return configList;
    }

    // 新增配置工厂类
    private static class ConfigFactory {
        private static final Map<String, Supplier<BaseConfig>> SUPPLIER_MAP = new HashMap<>();

        static {
            SUPPLIER_MAP.put("tencent", TencentConfig::new);
            SUPPLIER_MAP.put("alibaba", AlibabaConfig::new);
            SUPPLIER_MAP.put("huawei", HuaweiConfig::new);
            SUPPLIER_MAP.put("jdcloud", JdCloudConfig::new);
            SUPPLIER_MAP.put("unisms", UniConfig::new);
            SUPPLIER_MAP.put("emay", EmayConfig::new);
            SUPPLIER_MAP.put("cloopen", CloopenConfig::new);
        }

        static BaseConfig createConfig(String configId, SmsBaseConfig baseConfig) {
            String supplier = baseConfig.getSupplier().toLowerCase();
            Supplier<BaseConfig> constructor = SUPPLIER_MAP.get(supplier);
            if (constructor == null) {
                throw new IllegalArgumentException("Unsupported SMS supplier: " + supplier);
            }
            BaseConfig config = constructor.get();
            config.setConfigId(configId);
            // 使用BeanUtils自动映射同名属性
            BeanUtils.copyProperties(baseConfig, config);
            return config;
        }
    }
}
