package com.example.mapjava.friend;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import com.example.mapjava.auth.AppUser;
import com.example.mapjava.auth.UserRepository;
import com.example.mapjava.common.BadRequestException;
import com.example.mapjava.systemmessage.SystemMessageService;
import org.springframework.stereotype.Service;

@Service
public class FriendService {

    private static final Pattern CHINA_MAINLAND_PHONE = Pattern.compile("^1[3-9]\\d{9}$");

    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendRemarkRepository friendRemarkRepository;
    private final SystemMessageService systemMessageService;
    private final Clock clock;

    public FriendService(
            UserRepository userRepository,
            FriendRepository friendRepository,
            FriendRequestRepository friendRequestRepository,
            FriendRemarkRepository friendRemarkRepository,
            SystemMessageService systemMessageService,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.friendRepository = friendRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.friendRemarkRepository = friendRemarkRepository;
        this.systemMessageService = systemMessageService;
        this.clock = clock;
    }

    public RegisteredUserResponse findRegisteredUser(String phone) {
        String normalizedPhone = normalizePhone(phone);
        return userRepository.findByPhone(normalizedPhone)
                .map(user -> new RegisteredUserResponse(normalizedPhone, true, "用户已注册", toProfile(user)))
                .orElseGet(() -> new RegisteredUserResponse(normalizedPhone, false, "用户未注册", null));
    }

    public FriendRequestResponse sendFriendRequest(UUID userId, AddFriendRequest request) {
        String normalizedPhone = normalizePhone(request.phone());
        AppUser friend = userRepository.findByPhone(normalizedPhone)
                .orElseThrow(() -> new BadRequestException("该手机号还没有注册"));

        if (friend.id().equals(userId)) {
            throw new BadRequestException("不能添加自己为好友");
        }

        if (friendRepository.findBetween(userId, friend.id()).isPresent()) {
            throw new BadRequestException("你们已经是好友");
        }

        AppUser requester = getUser(userId);
        return friendRequestRepository.findPendingByRequesterAndReceiver(userId, friend.id())
                .map(existing -> toFriendRequestResponse(existing, requester, friend, "好友请求已发送，等待对方同意"))
                .orElseGet(() -> friendRequestRepository.findPendingByRequesterAndReceiver(friend.id(), userId)
                        .map(existing -> toFriendRequestResponse(existing, friend, requester, "对方已向你发送好友请求，请到好友请求列表同意"))
                        .orElseGet(() -> {
                            FriendRequest friendRequest = friendRequestRepository.save(new FriendRequest(
                                    UUID.randomUUID(),
                                    userId,
                                    friend.id(),
                                    FriendRequestStatus.PENDING,
                                    Instant.now(clock),
                                    null
                            ));
                            systemMessageService.createFriendRequestMessage(friend, requester, friendRequest.id());
                            return toFriendRequestResponse(friendRequest, requester, friend, "好友请求已发送");
                        }));
    }

    public List<FriendRequestResponse> findIncomingRequests(UUID userId) {
        return friendRequestRepository.findPendingByReceiverId(userId)
                .stream()
                .map(request -> toFriendRequestResponse(
                        request,
                        getUser(request.requesterUserId()),
                        getUser(request.receiverUserId()),
                        "待处理"
                ))
                .toList();
    }

    public List<FriendResponse> findFriends(UUID userId) {
        return friendRepository.findByUserId(userId)
                .stream()
                .map(relation -> {
                    UUID friendUserId = relation.firstUserId().equals(userId)
                            ? relation.secondUserId()
                            : relation.firstUserId();
                    return toFriendResponse(userId, getUser(friendUserId), relation, true);
                })
                .toList();
    }

    public FriendResponse updateFriendRemark(UUID userId, UUID friendUserId, UpdateFriendRemarkRequest request) {
        FriendRelation relation = friendRepository.findBetween(userId, friendUserId)
                .orElseThrow(() -> new BadRequestException("好友不存在"));

        AppUser friend = getUser(friendUserId);
        friendRemarkRepository.saveRemark(userId, friendUserId, normalizeRemark(request.remark()));
        return toFriendResponse(userId, friend, relation, true);
    }

    public void deleteFriend(UUID userId, UUID friendUserId) {
        friendRepository.findBetween(userId, friendUserId)
                .orElseThrow(() -> new BadRequestException("好友不存在"));

        friendRepository.deleteBetween(userId, friendUserId);
        friendRemarkRepository.deleteRemarksBetween(userId, friendUserId);
    }

    public FriendResponse acceptFriendRequest(UUID userId, UUID requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .filter(found -> found.receiverUserId().equals(userId))
                .orElseThrow(() -> new BadRequestException("好友请求不存在"));

        AppUser requester = getUser(request.requesterUserId());
        if (request.status() == FriendRequestStatus.ACCEPTED) {
            FriendRelation relation = friendRepository.findBetween(request.requesterUserId(), request.receiverUserId())
                    .orElseThrow(() -> new BadRequestException("好友请求已处理"));
            return toFriendResponse(userId, requester, relation, true);
        }

        FriendRelation relation = friendRepository.findBetween(request.requesterUserId(), request.receiverUserId())
                .orElseGet(() -> friendRepository.save(new FriendRelation(
                        UUID.randomUUID(),
                        firstUserId(request.requesterUserId(), request.receiverUserId()),
                        secondUserId(request.requesterUserId(), request.receiverUserId()),
                        Instant.now(clock)
                )));

        friendRequestRepository.save(new FriendRequest(
                request.id(),
                request.requesterUserId(),
                request.receiverUserId(),
                FriendRequestStatus.ACCEPTED,
                request.createdAt(),
                Instant.now(clock)
        ));
        systemMessageService.createFriendAcceptedMessage(requester, getUser(userId), request.id());

        return toFriendResponse(userId, requester, relation, false);
    }

    private FriendResponse toFriendResponse(
            UUID userId,
            AppUser friend,
            FriendRelation relation,
            boolean alreadyFriend
    ) {
        return new FriendResponse(
                friend.id(),
                friend.phone(),
                friend.displayName(),
                friendRemarkRepository.findRemark(userId, friend.id()).orElse(""),
                friend.avatarUrl(),
                alreadyFriend,
                relation.createdAt()
        );
    }

    private static UserLookupProfile toProfile(AppUser user) {
        return new UserLookupProfile(
                user.id(),
                user.phone(),
                user.displayName(),
                user.avatarUrl()
        );
    }

    private static FriendRequestResponse toFriendRequestResponse(
            FriendRequest request,
            AppUser requester,
            AppUser receiver,
            String message
    ) {
        return new FriendRequestResponse(
                request.id(),
                request.status().name().toLowerCase(),
                message,
                toProfile(requester),
                toProfile(receiver),
                request.createdAt()
        );
    }

    private AppUser getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("用户不存在"));
    }

    private static UUID firstUserId(UUID firstUserId, UUID secondUserId) {
        return firstUserId.compareTo(secondUserId) <= 0 ? firstUserId : secondUserId;
    }

    private static UUID secondUserId(UUID firstUserId, UUID secondUserId) {
        return firstUserId.compareTo(secondUserId) <= 0 ? secondUserId : firstUserId;
    }

    private static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BadRequestException("手机号不能为空");
        }

        String normalizedPhone = phone.trim().replaceAll("[\\s-]", "");
        if (!CHINA_MAINLAND_PHONE.matcher(normalizedPhone).matches()) {
            throw new BadRequestException("请输入正确的手机号");
        }

        return normalizedPhone;
    }

    private static String normalizeRemark(String remark) {
        if (remark == null || remark.isBlank()) {
            return "";
        }

        return remark.trim();
    }
}
