package com.example.mapjava.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.example.mapjava.common.BadRequestException;
import com.example.mapjava.membership.MembershipService;
import com.example.mapjava.membership.MembershipStatusResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final VerificationCodeService verificationCodeService;
    private final TokenGenerator tokenGenerator;
    private final TokenSettings tokenSettings;
    private final MembershipService membershipService;
    private final Clock clock;

    public AuthService(
            UserRepository userRepository,
            TokenRepository tokenRepository,
            VerificationCodeService verificationCodeService,
            TokenGenerator tokenGenerator,
            TokenSettings tokenSettings,
            MembershipService membershipService,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.verificationCodeService = verificationCodeService;
        this.tokenGenerator = tokenGenerator;
        this.tokenSettings = tokenSettings;
        this.membershipService = membershipService;
        this.clock = clock;
    }

    public AuthResponse login(LoginRequest request) {
        String phone = verificationCodeService.verifySmsCode(request.phone(), request.code());
        AppUser user = userRepository.findByPhone(phone)
                .orElseGet(() -> createUser(phone));

        return issueToken(user);
    }

    public UserProfile currentUser(String authorizationHeader) {
        AuthSession session = authenticate(authorizationHeader);
        AppUser user = userRepository.findById(session.userId())
                .orElseThrow(() -> new UnauthorizedException("Token user no longer exists"));

        return toProfile(user, membershipService.status(user.id()));
    }

    public UUID currentUserId(String authorizationHeader) {
        return authenticate(authorizationHeader).userId();
    }

    public UserProfile updateCurrentUser(String authorizationHeader, UpdateUserProfileRequest request) {
        AuthSession session = authenticate(authorizationHeader);
        AppUser user = userRepository.findById(session.userId())
                .orElseThrow(() -> new UnauthorizedException("Token user no longer exists"));

        AppUser updated = new AppUser(
                user.id(),
                user.phone(),
                nextNickname(request.nickname(), user.displayName()),
                nextAvatarUrl(request.avatarUrl(), user.avatarUrl()),
                user.createdAt()
        );

        AppUser saved = userRepository.save(updated);
        return toProfile(saved, membershipService.status(saved.id()));
    }

    public UserProfile updateCurrentUserAvatar(String authorizationHeader, String avatarUrl) {
        AuthSession session = authenticate(authorizationHeader);
        AppUser user = userRepository.findById(session.userId())
                .orElseThrow(() -> new UnauthorizedException("Token user no longer exists"));

        AppUser updated = new AppUser(
                user.id(),
                user.phone(),
                user.displayName(),
                avatarUrl,
                user.createdAt()
        );

        AppUser saved = userRepository.save(updated);
        return toProfile(saved, membershipService.status(saved.id()));
    }

    public void logout(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        tokenRepository.deleteByToken(token);
    }

    private AuthResponse issueToken(AppUser user) {
        String token = tokenGenerator.generate();
        AuthSession session = new AuthSession(
                token,
                user.id(),
                Instant.now(clock),
                tokenSettings.expiresAt(clock)
        );

        tokenRepository.save(session);
        return new AuthResponse(
                "Bearer",
                token,
                session.expiresAt(),
                tokenSettings.expiresInSeconds()
        );
    }

    private AuthSession authenticate(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        AuthSession session = tokenRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid bearer token"));

        if (session.isExpired(clock)) {
            tokenRepository.deleteByToken(token);
            throw new UnauthorizedException("Bearer token expired");
        }

        return session;
    }

    private static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new UnauthorizedException("Missing bearer token");
        }

        if (!authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new UnauthorizedException("Invalid authorization header");
        }

        String token = authorizationHeader.substring(7).trim();
        if (token.isBlank()) {
            throw new UnauthorizedException("Missing bearer token");
        }

        return token;
    }

    private static UserProfile toProfile(AppUser user, MembershipStatusResponse membershipStatus) {
        return new UserProfile(
                user.id(),
                user.phone(),
                user.displayName(),
                user.avatarUrl(),
                membershipStatus.memberStatus(),
                membershipStatus.memberExpiresAt()
        );
    }

    private AppUser createUser(String phone) {
        UUID id = UUID.randomUUID();
        AppUser user = new AppUser(
                id,
                phone,
                defaultDisplayName(phone),
                "/api/users/" + id + "/avatar",
                Instant.now(clock)
        );
        return userRepository.save(user);
    }

    private static String defaultDisplayName(String phone) {
        return "User " + phone.substring(phone.length() - 4);
    }

    private static String nextNickname(String nickname, String currentNickname) {
        if (nickname == null) {
            return currentNickname;
        }

        String trimmed = nickname.trim();
        if (trimmed.isBlank()) {
            throw new BadRequestException("昵称不能为空");
        }

        return trimmed;
    }

    private static String nextAvatarUrl(String avatarUrl, String currentAvatarUrl) {
        if (avatarUrl == null) {
            return currentAvatarUrl;
        }

        String trimmed = avatarUrl.trim();
        if (trimmed.isBlank()) {
            throw new BadRequestException("头像不能为空");
        }

        return trimmed;
    }
}
