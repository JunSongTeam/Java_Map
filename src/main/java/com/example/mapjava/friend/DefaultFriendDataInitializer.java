package com.example.mapjava.friend;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.example.mapjava.auth.AppUser;
import com.example.mapjava.auth.UserRepository;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DefaultFriendDataInitializer implements ApplicationRunner {

    private static final String DEFAULT_USER_PHONE = "13800138000";
    private static final String[] DEFAULT_FRIEND_PHONES = {
            "13900139000",
            "13700137000"
    };

    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final Clock clock;

    public DefaultFriendDataInitializer(
            UserRepository userRepository,
            FriendRepository friendRepository,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.friendRepository = friendRepository;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        AppUser defaultUser = findOrCreateUser(DEFAULT_USER_PHONE);
        for (String friendPhone : DEFAULT_FRIEND_PHONES) {
            AppUser friend = findOrCreateUser(friendPhone);
            friendRepository.findBetween(defaultUser.id(), friend.id())
                    .orElseGet(() -> friendRepository.save(new FriendRelation(
                            UUID.randomUUID(),
                            firstUserId(defaultUser.id(), friend.id()),
                            secondUserId(defaultUser.id(), friend.id()),
                            Instant.now(clock)
                    )));
        }
    }

    private AppUser findOrCreateUser(String phone) {
        return userRepository.findByPhone(phone)
                .orElseGet(() -> {
                    UUID id = UUID.randomUUID();
                    return userRepository.save(new AppUser(
                            id,
                            phone,
                            defaultDisplayName(phone),
                            "/api/users/" + id + "/avatar",
                            Instant.now(clock)
                    ));
                });
    }

    private static String defaultDisplayName(String phone) {
        return "User " + phone.substring(phone.length() - 4);
    }

    private static UUID firstUserId(UUID firstUserId, UUID secondUserId) {
        return firstUserId.compareTo(secondUserId) <= 0 ? firstUserId : secondUserId;
    }

    private static UUID secondUserId(UUID firstUserId, UUID secondUserId) {
        return firstUserId.compareTo(secondUserId) <= 0 ? secondUserId : firstUserId;
    }
}
