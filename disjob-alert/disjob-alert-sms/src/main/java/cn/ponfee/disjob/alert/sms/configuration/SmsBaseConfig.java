package cn.ponfee.disjob.alert.sms.configuration;

import lombok.Data;

@Data
public class SmsBaseConfig {
    private String accessKeyId;

    private String accessKeySecret;

    private String signaTure;

    private String templateId;

    private String supplier;

}
