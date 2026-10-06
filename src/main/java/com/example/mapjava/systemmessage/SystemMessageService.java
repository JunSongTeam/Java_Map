package com.example.mapjava.systemmessage;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.example.mapjava.auth.AppUser;
import com.example.mapjava.common.BadRequestException;
import org.springframework.stereotype.Service;

@Service
public class SystemMessageService {

    private static final String DEFAULT_ANNOUNCEMENT_ID = "local-default-announcement-v1";

    private final SystemMessageRepository systemMessageRepository;
    private final Clock clock;

    public SystemMessageService(SystemMessageRepository systemMessageRepository, Clock clock) {
        this.systemMessageRepository = systemMessageRepository;
        this.clock = clock;
    }

    public List<SystemMessageResponse> findMessages(UUID userId) {
        ensureDefaultAnnouncement(userId);
        return systemMessageRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public SystemMessageUnreadCountResponse unreadCount(UUID userId) {
        ensureDefaultAnnouncement(userId);
        return new SystemMessageUnreadCountResponse((int) systemMessageRepository.countUnreadByUserId(userId));
    }

    public SystemMessageResponse markRead(UUID userId, UUID messageId) {
        ensureDefaultAnnouncement(userId);
        SystemMessage message = systemMessageRepository.findById(messageId)
                .filter(found -> found.userId().equals(userId))
                .orElseThrow(() -> new BadRequestException("系统消息不存在"));

        if (message.read()) {
            return toResponse(message);
        }

        SystemMessage saved = systemMessageRepository.save(new SystemMessage(
                message.id(),
                message.userId(),
                message.type(),
                message.title(),
                message.content(),
                true,
                message.payload(),
                message.createdAt(),
                Instant.now(clock)
        ));
        return toResponse(saved);
    }

    public SystemMessageUnreadCountResponse markAllRead(UUID userId) {
        ensureDefaultAnnouncement(userId);
        Instant now = Instant.now(clock);
        for (SystemMessage message : systemMessageRepository.findByUserId(userId)) {
            if (!message.read()) {
                systemMessageRepository.save(new SystemMessage(
                        message.id(),
                        message.userId(),
                        message.type(),
                        message.title(),
                        message.content(),
                        true,
                        message.payload(),
                        message.createdAt(),
                        now
                ));
            }
        }

        return new SystemMessageUnreadCountResponse(0);
    }

    public void createFriendRequestMessage(AppUser receiver, AppUser requester, UUID requestId) {
        createMessage(
                receiver.id(),
                SystemMessageType.FRIEND_REQUEST,
                "好友请求",
                requester.displayName() + " 请求添加你为好友",
                Map.of(
                        "requestId", requestId.toString(),
                        "fromUserId", requester.id().toString(),
                        "fromPhone", requester.phone()
                )
        );
    }

    public void createFriendAcceptedMessage(AppUser requester, AppUser receiver, UUID requestId) {
        createMessage(
                requester.id(),
                SystemMessageType.FRIEND_ACCEPTED,
                "好友已同意",
                receiver.displayName() + " 已同意你的好友请求",
                Map.of(
                        "requestId", requestId.toString(),
                        "friendUserId", receiver.id().toString(),
                        "friendPhone", receiver.phone()
                )
        );
    }

    private void createMessage(
            UUID userId,
            SystemMessageType type,
            String title,
            String content,
            Map<String, String> payload
    ) {
        systemMessageRepository.save(new SystemMessage(
                UUID.randomUUID(),
                userId,
                type,
                title,
                content,
                false,
                payload,
                Instant.now(clock),
                null
        ));
    }

    private void ensureDefaultAnnouncement(UUID userId) {
        if (systemMessageRepository.existsByUserIdAndPayloadValue(userId, "announcementId", DEFAULT_ANNOUNCEMENT_ID)) {
            return;
        }

        createMessage(
                userId,
                SystemMessageType.SYSTEM_ANNOUNCEMENT,
                "系统公告",
                "欢迎使用 CustomMap，系统消息功能已开启",
                Map.of("announcementId", DEFAULT_ANNOUNCEMENT_ID)
        );
    }

    private SystemMessageResponse toResponse(SystemMessage message) {
        return new SystemMessageResponse(
                message.id(),
                message.type().value(),
                message.title(),
                message.content(),
                message.read(),
                message.payload(),
                message.createdAt(),
                message.readAt()
        );
    }
}
