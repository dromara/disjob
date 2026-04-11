/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.alert.sms;

import cn.ponfee.disjob.alert.sms.configuration.SmsAlertSenderProperties;
import cn.ponfee.disjob.alert.sms.configuration.SmsAlertSenderProperties.SmsBlendProperties;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.aliyun.config.AlibabaConfig;
import org.dromara.sms4j.cloopen.config.CloopenConfig;
import org.dromara.sms4j.core.datainterface.SmsReadConfig;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.dromara.sms4j.emay.config.EmayConfig;
import org.dromara.sms4j.huawei.config.HuaweiConfig;
import org.dromara.sms4j.jdcloud.config.JdCloudConfig;
import org.dromara.sms4j.provider.config.BaseConfig;
import org.dromara.sms4j.tencent.config.TencentConfig;
import org.dromara.sms4j.unisms.config.UniConfig;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Read config
 *
 * @author TJxiaobao
 */
@Slf4j
public class SmsAlertReadConfig implements SmsReadConfig, ApplicationListener<ContextRefreshedEvent> {

    private final SmsAlertSenderProperties config;

    public SmsAlertReadConfig(SmsAlertSenderProperties config) {
        this.config = config;
    }

    /**
     * Refresh sms read config by spring event
     *
     * @param event the ContextRefreshedEvent
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        SmsFactory.createSmsBlend(this);
        log.info("Sms alert read config refreshed.");
    }

    @Override
    public BaseConfig getSupplierConfig(String configId) {
        Map<String, SmsBlendProperties> blends = config.getBlends();
        SmsBlendProperties blendProperties = blends.get(configId);
        return blendProperties == null ? null : ConfigFactory.createConfig(configId, blendProperties);
    }

    @Override
    public List<BaseConfig> getSupplierConfigList() {
        List<BaseConfig> configList = new ArrayList<>();
        Map<String, SmsBlendProperties> blends = config.getBlends();
        for (Map.Entry<String, SmsBlendProperties> entry : blends.entrySet()) {
            configList.add(getSupplierConfig(entry.getKey()));
        }
        return configList;
    }

    /**
     * 配置工厂类
     */
    private static class ConfigFactory {

        static final Map<String, Supplier<BaseConfig>> SUPPLIER_MAP = ImmutableMap.of(
            "tencent", TencentConfig::new,
            "alibaba", AlibabaConfig::new,
            "huawei", HuaweiConfig::new,
            "jdcloud", JdCloudConfig::new,
            "unisms", UniConfig::new,
            "emay", EmayConfig::new,
            "cloopen", CloopenConfig::new
        );

        static BaseConfig createConfig(String configId, SmsBlendProperties blendProperties) {
            String supplier = blendProperties.getSupplier().toLowerCase();
            Supplier<BaseConfig> constructor = SUPPLIER_MAP.get(supplier);
            if (constructor == null) {
                throw new UnsupportedOperationException("Unsupported sms supplier: " + supplier);
            }
            BaseConfig baseConfig = constructor.get();
            baseConfig.setConfigId(configId);
            BeanUtils.copyProperties(blendProperties, baseConfig);
            return baseConfig;
        }
    }

}
