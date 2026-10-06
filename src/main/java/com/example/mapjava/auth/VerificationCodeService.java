package com.example.mapjava.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

import com.example.mapjava.common.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VerificationCodeService {

    private static final String DEFAULT_CHANNEL = "sms";
    private static final Pattern CHINA_MAINLAND_PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern SIX_DIGIT_CODE = Pattern.compile("^\\d{6}$");

    private final VerificationCodeRepository verificationCodeRepository;
    private final VerificationCodeSender verificationCodeSender;
    private final PasswordHasher passwordHasher;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long expirationSeconds;
    private final boolean exposeCode;
    private final String defaultCode;

    public VerificationCodeService(
            VerificationCodeRepository verificationCodeRepository,
            VerificationCodeSender verificationCodeSender,
            PasswordHasher passwordHasher,
            Clock clock,
            @Value("${auth.verification-code.expiration-seconds:300}") long expirationSeconds,
            @Value("${auth.verification-code.expose-code:true}") boolean exposeCode,
            @Value("${auth.verification-code.default-code:}") String defaultCode
    ) {
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("auth.verification-code.expiration-seconds must be positive");
        }

        this.verificationCodeRepository = verificationCodeRepository;
        this.verificationCodeSender = verificationCodeSender;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
        this.expirationSeconds = expirationSeconds;
        this.exposeCode = exposeCode;
        this.defaultCode = normalizeDefaultCode(defaultCode);
    }

    public VerificationCodeResponse sendCode(SendVerificationCodeRequest request) {
        String channel = DEFAULT_CHANNEL;
        String target = normalizeTarget(channel, request.phone());
        String generatedCode = generateCode();
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plusSeconds(expirationSeconds);

        verificationCodeRepository.save(new VerificationCode(
                channel,
                target,
                passwordHasher.hash(generatedCode),
                now,
                expiresAt
        ));
        verificationCodeSender.send(channel, target, generatedCode);

        return new VerificationCodeResponse(
                target,
                expirationSeconds,
                expiresAt,
                exposeCode ? generatedCode : null
        );
    }

    public VerificationCodeVerifyResponse verifyCode(VerifyVerificationCodeRequest request) {
        String channel = normalizeChannel(request.channel());
        String target = normalizeTarget(channel, request.target());
        String code = request.code().trim();

        verifyStoredCode(channel, target, code);
        return new VerificationCodeVerifyResponse(true);
    }

    public String verifySmsCode(String phone, String code) {
        String target = normalizeTarget(DEFAULT_CHANNEL, phone);
        verifyStoredCode(DEFAULT_CHANNEL, target, code.trim());
        return target;
    }

    private void verifyStoredCode(String channel, String target, String code) {
        VerificationCode verificationCode = verificationCodeRepository
                .findByChannelAndTarget(channel, target)
                .orElseThrow(() -> new BadRequestException("Verification code is invalid or expired"));

        if (verificationCode.isExpired(clock)) {
            verificationCodeRepository.deleteByChannelAndTarget(channel, target);
            throw new BadRequestException("Verification code is invalid or expired");
        }

        if (!passwordHasher.matches(code, verificationCode.codeHash())) {
            throw new BadRequestException("Verification code is invalid or expired");
        }

        verificationCodeRepository.deleteByChannelAndTarget(channel, target);
    }

    private String generateCode() {
        if (defaultCode != null) {
            return defaultCode;
        }

        return "%06d".formatted(secureRandom.nextInt(1_000_000));
    }

    private static String normalizeDefaultCode(String defaultCode) {
        if (defaultCode == null || defaultCode.isBlank()) {
            return null;
        }

        String normalizedCode = defaultCode.trim();
        if (!SIX_DIGIT_CODE.matcher(normalizedCode).matches()) {
            throw new IllegalArgumentException("auth.verification-code.default-code must be 6 digits");
        }

        return normalizedCode;
    }

    private static String normalizeChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return DEFAULT_CHANNEL;
        }

        return channel.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeTarget(String channel, String target) {
        String normalizedTarget = target.trim();
        if ("sms".equals(channel)) {
            return normalizePhone(normalizedTarget);
        }

        if ("email".equals(channel)) {
            return normalizeEmail(normalizedTarget);
        }

        throw new BadRequestException("channel must be sms or email");
    }

    private static String normalizePhone(String target) {
        String phone = target.replaceAll("[\\s-]", "");
        if (!CHINA_MAINLAND_PHONE.matcher(phone).matches()) {
            throw new BadRequestException("phone must be a valid mainland China mobile number");
        }

        return phone;
    }

    private static String normalizeEmail(String target) {
        String email = target.toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) {
            throw new BadRequestException("target must be a valid email address");
        }

        return email;
    }
}
