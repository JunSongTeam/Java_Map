package com.example.mapjava.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "sms.aliyun", name = "enabled", havingValue = "false", matchIfMissing = true)
public class ConsoleVerificationCodeSender implements VerificationCodeSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleVerificationCodeSender.class);

    @Override
    public void send(String channel, String target, String code) {
        log.info("Verification code for {} {} is {}", channel, target, code);
    }
}
