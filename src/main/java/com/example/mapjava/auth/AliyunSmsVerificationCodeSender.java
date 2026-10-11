package com.example.mapjava.auth;

import java.util.LinkedHashMap;
import java.util.Map;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.example.mapjava.common.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "sms.aliyun", name = "enabled", havingValue = "true")
public class AliyunSmsVerificationCodeSender implements VerificationCodeSender {

    private static final Logger log = LoggerFactory.getLogger(AliyunSmsVerificationCodeSender.class);
    private static final String SUCCESS_CODE = "OK";

    private final Client client;
    private final ObjectMapper objectMapper;
    private final String signName;
    private final String templateCode;
    private final String codeVariableName;
    private final String validMinutesVariableName;
    private final String validMinutes;
    private final String countryCode;

    public AliyunSmsVerificationCodeSender(
            ObjectMapper objectMapper,
            @Value("${sms.aliyun.access-key-id:}") String accessKeyId,
            @Value("${sms.aliyun.access-key-secret:}") String accessKeySecret,
            @Value("${sms.aliyun.sign-name:}") String signName,
            @Value("${sms.aliyun.template-code:}") String templateCode,
            @Value("${sms.aliyun.code-variable-name:code}") String codeVariableName,
            @Value("${sms.aliyun.valid-minutes-variable-name:min}") String validMinutesVariableName,
            @Value("${sms.aliyun.valid-minutes:5}") String validMinutes,
            @Value("${sms.aliyun.country-code:86}") String countryCode,
            @Value("${sms.aliyun.endpoint:dypnsapi.aliyuncs.com}") String endpoint
    ) throws Exception {
        this.objectMapper = objectMapper;
        this.signName = required("sms.aliyun.sign-name", signName);
        this.templateCode = required("sms.aliyun.template-code", templateCode);
        this.codeVariableName = required("sms.aliyun.code-variable-name", codeVariableName);
        this.validMinutesVariableName = required("sms.aliyun.valid-minutes-variable-name", validMinutesVariableName);
        this.validMinutes = required("sms.aliyun.valid-minutes", validMinutes);
        this.countryCode = required("sms.aliyun.country-code", countryCode);
        this.client = new Client(new Config()
                .setAccessKeyId(required("sms.aliyun.access-key-id", accessKeyId))
                .setAccessKeySecret(required("sms.aliyun.access-key-secret", accessKeySecret))
                .setEndpoint(required("sms.aliyun.endpoint", endpoint)));
    }

    @Override
    public void send(String channel, String target, String code) {
        if (!"sms".equals(channel)) {
            throw new BadRequestException("channel must be sms");
        }

        try {
            Map<String, String> templateParams = new LinkedHashMap<>();
            templateParams.put(codeVariableName, code);
            templateParams.put(validMinutesVariableName, validMinutes);

            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setCountryCode(countryCode)
                    .setPhoneNumber(target)
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setValidTime(300L)
                    .setTemplateParam(objectMapper.writeValueAsString(templateParams));
            SendSmsVerifyCodeResponse response = client.sendSmsVerifyCodeWithOptions(request, new RuntimeOptions());

            if (response == null || response.getBody() == null || !SUCCESS_CODE.equals(response.getBody().getCode())) {
                String responseCode = response == null || response.getBody() == null ? "UNKNOWN" : response.getBody().getCode();
                String message = response == null || response.getBody() == null ? "No response body" : response.getBody().getMessage();
                log.warn("Aliyun SMS send failed target={} code={} message={}", target, responseCode, message);
                throw new BadRequestException("验证码发送失败，请稍后重试");
            }
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Aliyun SMS send failed target={}", target, exception);
            throw new BadRequestException("验证码发送失败，请稍后重试");
        }
    }

    private static String required(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank when Aliyun SMS is enabled");
        }

        return value.trim();
    }
}
