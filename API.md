# Map Java 接口文档

## 基础信息

- 本地地址：`http://localhost:8088`
- 请求格式：`Content-Type: application/json`
- 需要登录的接口统一传请求头：`Authorization: Bearer {token}`
- token 当前配置为永不过期，所以登录返回的 `expiresAt` 和 `expiresInSeconds` 为 `null`
- 轨迹按 `Asia/Shanghai` 时区把 `recordedAt` 归到每天一条

## 通用错误返回

请求参数错误、token 错误、资源不存在时会返回类似格式：

```json
{
  "timestamp": "2026-09-19T06:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/login",
  "details": [
    "code: code must be 6 digits"
  ]
}
```

字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| timestamp | string | 错误发生时间 |
| status | number | HTTP 状态码 |
| error | string | HTTP 错误名称 |
| message | string | 错误提示 |
| path | string | 请求路径 |
| details | array | 参数校验明细 |

## 1. 健康检查

### GET `/api/health`

用于确认本地服务是否启动。

请求参数：无

返回参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| status | string | 固定为 `ok` |
| service | string | 服务名 |
| timestamp | string | 服务当前时间 |

返回示例：

```json
{
  "status": "ok",
  "service": "map-java",
  "timestamp": "2026-09-19T06:00:00Z"
}
```

## 2. 发送验证码

### POST `/api/send/code`

获取登录验证码。开发模式下会直接返回 `devCode`，方便前端联调。

当前本地开发环境已设置默认验证码：`123456`。流程仍然是先调用 `/api/send/code`，再用 `123456` 登录。

请求参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| phone | string | 是 | 手机号，最多 20 个字符 |

请求示例：

```json
{
  "phone": "13800138000"
}
```

返回参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| phone | string | 手机号 |
| expiresInSeconds | number | 验证码有效秒数，当前为 300 |
| expiresAt | string | 验证码过期时间 |
| devCode | string | 开发环境验证码，当前固定为 `123456`，生产环境应关闭 |

返回示例：

```json
{
  "phone": "13800138000",
  "expiresInSeconds": 300,
  "expiresAt": "2026-09-19T06:05:00Z",
  "devCode": "123456"
}
```

## 3. 登录

### POST `/api/login`

手机号不存在时会自动创建用户，所以不需要单独注册接口。
登录用的 `code` 填 `/api/send/code` 返回的 `devCode`；当前本地开发环境固定为 `123456`。

请求参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| phone | string | 是 | 手机号，最多 20 个字符 |
| code | string | 是 | 6 位验证码 |

请求示例：

```json
{
  "phone": "13800138000",
  "code": "123456"
}
```

返回参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| tokenType | string | 固定为 `Bearer` |
| token | string | 登录 token |
| expiresAt | string/null | token 过期时间，永不过期时为 `null` |
| expiresInSeconds | number/null | token 有效秒数，永不过期时为 `null` |

返回示例：

```json
{
  "tokenType": "Bearer",
  "token": "u9NJ0b6Q9K...",
  "expiresAt": null,
  "expiresInSeconds": null
}
```

## 4. 获取我的资料

### GET `/api/me`

请求头：

| 名称 | 必填 | 说明 |
| --- | --- | --- |
| Authorization | 是 | `Bearer {token}` |

请求参数：无

返回参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| userId | string | 用户 id |
| phone | string | 手机号 |
| nickname | string | 昵称 |
| avatarUrl | string | 头像地址 |
| memberStatus | string | 会员状态，当前默认 `inactive` |
| memberExpiresAt | string/null | 会员到期时间，当前默认 `null` |

返回示例：

```json
{
  "userId": "6d8cf2a1-cb7b-49e7-a88c-3f4a4ffdc9e7",
  "phone": "13800138000",
  "nickname": "User 8000",
  "avatarUrl": "/api/users/6d8cf2a1-cb7b-49e7-a88c-3f4a4ffdc9e7/avatar",
  "memberStatus": "inactive",
  "memberExpiresAt": null
}
```

## 5. 退出登录

### POST `/api/logout`

请求头：

| 名称 | 必填 | 说明 |
| --- | --- | --- |
| Authorization | 是 | `Bearer {token}` |

请求参数：无

返回：`204 No Content`，无响应体。

## 6. 获取头像

### GET `/api/users/{id}/avatar`

请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | string | 是 | 用户 id，放在路径里 |

返回：`image/svg+xml` 图片内容。

## 7. 查询用户是否注册

### GET `/api/users/registered`

按手机号查询该用户是否已经注册。这个接口需要登录后调用。

请求头：

| 名称 | 必填 | 说明 |
| --- | --- | --- |
| Authorization | 是 | `Bearer {token}` |

请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| phone | string | 是 | 要查询的手机号 |

请求示例：

```text
GET /api/users/registered?phone=13800138000
```

已注册返回参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| phone | string | 查询的手机号 |
| registered | boolean | 是否已注册 |
| message | string | 中文提示 |
| user.userId | string | 用户 id |
| user.phone | string | 手机号 |
| user.nickname | string | 昵称 |
| user.avatarUrl | string | 头像地址 |

已注册返回示例：

```json
{
  "phone": "13800138000",
  "registered": true,
  "message": "用户已注册",
  "user": {
    "userId": "6d8cf2a1-cb7b-49e7-a88c-3f4a4ffdc9e7",
    "phone": "13800138000",
    "nickname": "User 8000",
    "avatarUrl": "/api/users/6d8cf2a1-cb7b-49e7-a88c-3f4a4ffdc9e7/avatar"
  }
}
```

未注册返回示例：

```json
{
  "phone": "13800138000",
  "registered": false,
  "message": "用户未注册"
}
```

## 8. 发送好友请求

### POST `/api/friends`

通过手机号发送好友请求。对方同意前不会成为好友；重复发送同一个待处理请求会返回原请求。

请求头：

| 名称 | 必填 | 说明 |
| --- | --- | --- |
| Authorization | 是 | `Bearer {token}` |

请求参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| phone | string | 是 | 要添加的好友手机号 |

请求示例：

```json
{
  "phone": "13800138000"
}
```

返回参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| requestId | string | 好友请求 id |
| status | string | 请求状态，当前为 `pending` |
| message | string | 中文提示 |
| requester.userId | string | 发起人用户 id |
| requester.phone | string | 发起人手机号 |
| requester.nickname | string | 发起人昵称 |
| requester.avatarUrl | string | 发起人头像 |
| receiver.userId | string | 接收人用户 id |
| receiver.phone | string | 接收人手机号 |
| receiver.nickname | string | 接收人昵称 |
| receiver.avatarUrl | string | 接收人头像 |
| createdAt | string | 请求创建时间 |

返回示例：

```json
{
  "requestId": "29b7d10c-89b5-4a51-9c47-b455a26c08c2",
  "status": "pending",
  "message": "好友请求已发送",
  "requester": {
    "userId": "a6f43eb4-7df8-4c33-a7d6-715051ea39a8",
    "phone": "13800138000",
    "nickname": "User 8000",
    "avatarUrl": "/api/users/a6f43eb4-7df8-4c33-a7d6-715051ea39a8/avatar"
  },
  "receiver": {
    "userId": "6d8cf2a1-cb7b-49e7-a88c-3f4a4ffdc9e7",
    "phone": "13900139000",
    "nickname": "User 9000",
    "avatarUrl": "/api/users/6d8cf2a1-cb7b-49e7-a88c-3f4a4ffdc9e7/avatar"
  },
  "createdAt": "2026-09-19T07:30:00Z"
}
```

常见错误：

| 状态码 | message | 说明 |
| --- | --- | --- |
| 400 | `该手机号还没有注册` | 该手机号还没有注册 |
| 400 | `不能添加自己为好友` | 不能添加自己 |
| 400 | `你们已经是好友` | 双方已经是好友 |

## 9. 好友列表

### GET `/api/friends`

查询当前用户已经添加成功的好友列表。只有对方同意好友请求后，才会出现在这里。

本地开发环境启动时，会默认给 `13800138000` 添加两个好友：`13900139000`、`13700137000`。

请求头：

| 名称 | 必填 | 说明 |
| --- | --- | --- |
| Authorization | 是 | `Bearer {token}` |

请求参数：无

返回参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| friendUserId | string | 好友用户 id |
| phone | string | 好友手机号 |
| nickname | string | 好友昵称 |
| remark | string | 我给好友设置的备注，没有备注时为空字符串 |
| avatarUrl | string | 好友头像地址 |
| alreadyFriend | boolean | 固定为 `true` |
| createdAt | string | 成为好友的时间 |

返回示例：

```json
[
  {
    "friendUserId": "6d8cf2a1-cb7b-49e7-a88c-3f4a4ffdc9e7",
    "phone": "13900139000",
    "nickname": "User 9000",
    "remark": "徒步搭子",
    "avatarUrl": "/api/users/6d8cf2a1-cb7b-49e7-a88c-3f4a4ffdc9e7/avatar",
    "alreadyFriend": true,
    "createdAt": "2026-09-19T07:35:00Z"
  }
]
```

## 10. 修改好友备注

### PUT `/api/friends/{friendUserId}/remark`

修改当前用户给某个好友设置的备注。备注只影响当前用户自己看到的好友列表，不影响对方。

请求头：

| 名称 | 必填 | 说明 |
| --- | --- | --- |
| Authorization | 是 | `Bearer {token}` |

请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| friendUserId | string | 是 | 好友用户 id，放在路径里 |
| remark | string | 否 | 备注，最多 40 个字符；传空字符串可清空备注 |

请求示例：

```json
{
  "remark": "徒步搭子"
}
```

返回参数：好友对象，字段同“好友列表”的单项对象。

返回示例：

```json
{
  "friendUserId": "6d8cf2a1-cb7b-49e7-a88c-3f4a4ffdc9e7",
  "phone": "13900139000",
  "nickname": "User 9000",
  "remark": "徒步搭子",
  "avatarUrl": "/api/users/6d8cf2a1-cb7b-49e7-a88c-3f4a4ffdc9e7/avatar",
  "alreadyFriend": true,
  "createdAt": "2026-09-19T07:35:00Z"
}
```

常见错误：

| 状态码 | message | 说明 |
| --- | --- | --- |
| 400 | `好友不存在` | 对方不是当前用户好友 |
| 400 | `Validation failed` | 备注超过 40 个字符 |

## 11. 删除好友

### DELETE `/api/friends/{friendUserId}`

删除当前用户和对方的好友关系，同时清空双方给彼此设置的备注。

请求头：

| 名称 | 必填 | 说明 |
| --- | --- | --- |
| Authorization | 是 | `Bearer {token}` |

请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| friendUserId | string | 是 | 好友用户 id，放在路径里 |

返回：`204 No Content`，无响应体。

常见错误：

| 状态码 | message | 说明 |
| --- | --- | --- |
| 400 | `好友不存在` | 对方不是当前用户好友 |

## 12. 好友请求列表

### GET `/api/friends/requests`

查询当前用户收到的待处理好友请求，按请求时间倒序排列。

请求头：

| 名称 | 必填 | 说明 |
| --- | --- | --- |
| Authorization | 是 | `Bearer {token}` |

请求参数：无

返回参数：数组，每一项字段同“发送好友请求”的返回参数。

返回示例：

```json
[
  {
    "requestId": "29b7d10c-89b5-4a51-9c47-b455a26c08c2",
    "status": "pending",
    "message": "待处理",
    "requester": {
      "userId": "a6f43eb4-7df8-4c33-a7d6-715051ea39a8",
      "phone": "13800138000",
      "nickname": "User 8000",
      "avatarUrl": "/api/users/a6f43eb4-7df8-4c33-a7d6-715051ea39a8/avatar"
    },
    "receiver": {
      "userId": "6d8cf2a1-cb7b-49e7-a88c-3f4a4ffdc9e7",
      "phone": "13900139000",
      "nickname": "User 9000",
      "avatarUrl": "/api/users/6d8cf2a1-cb7b-49e7-a88c-3f4a4ffdc9e7/avatar"
    },
    "createdAt": "2026-09-19T07:30:00Z"
  }
]
```

## 13. 同意好友请求

### POST `/api/friends/requests/{requestId}/accept`

同意别人发给当前用户的好友请求；同意后双方正式成为好友。

请求头：

| 名称 | 必填 | 说明 |
| --- | --- | --- |
| Authorization | 是 | `Bearer {token}` |

请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| requestId | string | 是 | 好友请求 id，放在路径里 |

返回参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| friendUserId | string | 对方用户 id |
| phone | string | 对方手机号 |
| nickname | string | 对方昵称 |
| remark | string | 我给对方设置的备注，同意时默认为空字符串 |
| avatarUrl | string | 对方头像地址 |
| alreadyFriend | boolean | 是否已存在好友关系 |
| createdAt | string | 好友关系创建时间 |

返回示例：

```json
{
  "friendUserId": "a6f43eb4-7df8-4c33-a7d6-715051ea39a8",
  "phone": "13800138000",
  "nickname": "User 8000",
  "remark": "",
  "avatarUrl": "/api/users/a6f43eb4-7df8-4c33-a7d6-715051ea39a8/avatar",
  "alreadyFriend": false,
  "createdAt": "2026-09-19T07:35:00Z"
}
```

常见错误：

| 状态码 | message | 说明 |
| --- | --- | --- |
| 400 | `好友请求不存在` | 请求不存在，或不是发给当前用户的请求 |

## 14. 系统消息列表

### GET `/api/system-messages`

查询当前用户的系统消息。当前只会返回 3 种类型：`friend_request`、`friend_accepted`、`system_announcement`。

触发规则：

| 类型 | 说明 |
| --- | --- |
| friend_request | 有人向我发送好友请求时生成 |
| friend_accepted | 对方同意我的好友请求时生成 |
| system_announcement | 系统公告；本地开发环境会默认生成一条欢迎公告 |

请求头：

| 名称 | 必填 | 说明 |
| --- | --- | --- |
| Authorization | 是 | `Bearer {token}` |

请求参数：无

返回参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| messageId | string | 系统消息 id |
| type | string | 消息类型：`friend_request`、`friend_accepted`、`system_announcement` |
| title | string | 消息标题 |
| content | string | 消息内容 |
| read | boolean | 是否已读 |
| payload | object | 业务参数，不同消息类型内容不同 |
| createdAt | string | 消息创建时间 |
| readAt | string/null | 已读时间，未读时为 `null` |

返回示例：

```json
[
  {
    "messageId": "7d403a52-3f92-4a52-8f47-64a86f283a5d",
    "type": "friend_request",
    "title": "好友请求",
    "content": "User 8000 请求添加你为好友",
    "read": false,
    "payload": {
      "requestId": "29b7d10c-89b5-4a51-9c47-b455a26c08c2",
      "fromUserId": "a6f43eb4-7df8-4c33-a7d6-715051ea39a8",
      "fromPhone": "13800138000"
    },
    "createdAt": "2026-09-19T07:30:00Z",
    "readAt": null
  },
  {
    "messageId": "35aa1f99-3cbb-47f6-92cb-934f453cc5ac",
    "type": "system_announcement",
    "title": "系统公告",
    "content": "欢迎使用 CustomMap，系统消息功能已开启",
    "read": false,
    "payload": {
      "announcementId": "local-default-announcement-v1"
    },
    "createdAt": "2026-09-19T07:00:00Z",
    "readAt": null
  }
]
```

## 15. 系统消息未读数

### GET `/api/system-messages/unread-count`

查询当前用户未读系统消息数量。

请求头：

| 名称 | 必填 | 说明 |
| --- | --- | --- |
| Authorization | 是 | `Bearer {token}` |

请求参数：无

返回参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| unreadCount | number | 未读消息数量 |

返回示例：

```json
{
  "unreadCount": 2
}
```

## 16. 标记系统消息已读

### PUT `/api/system-messages/{messageId}/read`

把一条系统消息标记为已读。

请求头：

| 名称 | 必填 | 说明 |
| --- | --- | --- |
| Authorization | 是 | `Bearer {token}` |

请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| messageId | string | 是 | 系统消息 id，放在路径里 |

返回参数：系统消息对象，字段同“系统消息列表”的单项对象。

常见错误：

| 状态码 | message | 说明 |
| --- | --- | --- |
| 400 | `系统消息不存在` | 消息不存在，或不是当前用户的消息 |

## 17. 全部系统消息已读

### PUT `/api/system-messages/read-all`

把当前用户所有系统消息标记为已读。

请求头：

| 名称 | 必填 | 说明 |
| --- | --- | --- |
| Authorization | 是 | `Bearer {token}` |

请求参数：无

返回参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| unreadCount | number | 标记后的未读数量，固定为 `0` |

返回示例：

```json
{
  "unreadCount": 0
}
```

## 18. 上传轨迹

### POST `/api/tracks/upload`

客户端可以每 5 分钟或 10 分钟上传一组轨迹点。请求体直接传数组；后端会按用户和日期合并到每天一条轨迹。

请求头：

| 名称 | 必填 | 说明 |
| --- | --- | --- |
| Authorization | 是 | `Bearer {token}` |

请求参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| latitude | number | 是 | 纬度，范围 `-90` 到 `90` |
| longitude | number | 是 | 经度，范围 `-180` 到 `180` |
| recordedAt | string | 是 | 轨迹点记录时间，ISO 8601 格式 |
| address | string | 否 | 当前地址，最多 200 个字符 |
| accuracy | number/null | 否 | 定位精度，单位米，不能小于 0 |
| speed | number/null | 否 | 速度，不能小于 0 |

请求示例：

```json
[
  {
    "latitude": 31.2304,
    "longitude": 121.4737,
    "recordedAt": "2026-09-19T08:00:00Z",
    "address": "人民广场",
    "accuracy": 12.5,
    "speed": 1.2
  },
  {
    "latitude": 31.231,
    "longitude": 121.4742,
    "recordedAt": "2026-09-19T08:05:00Z",
    "address": "南京东路",
    "accuracy": 10,
    "speed": 1.4
  }
]
```

返回参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| uploadedCount | number | 本次接收的轨迹点数量 |
| affectedTracks | array | 本次影响到的每日轨迹 |
| affectedTracks[].trackId | string | 轨迹 id |
| affectedTracks[].date | string | 轨迹日期，格式 `yyyy-MM-dd` |

返回示例：

```json
{
  "uploadedCount": 2,
  "affectedTracks": [
    {
      "trackId": "9f38d476-7dbd-4214-98aa-e5858efdb0a4",
      "date": "2026-09-19"
    }
  ]
}
```

## 19. 我的轨迹列表

### GET `/api/me/tracks`

返回当前用户的每日轨迹列表，按日期倒序排列。列表只返回每天的开始地址和结束地址；轨迹点、距离等完整数据走详情接口。

请求头：

| 名称 | 必填 | 说明 |
| --- | --- | --- |
| Authorization | 是 | `Bearer {token}` |

请求参数：无

返回参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| trackId | string | 轨迹 id |
| date | string | 轨迹日期，格式 `yyyy-MM-dd` |
| startAddress | string | 当天第一个轨迹点地址，没有地址时返回经纬度 |
| endAddress | string | 当天最后一个轨迹点地址，没有地址时返回经纬度 |

返回示例：

```json
[
  {
    "trackId": "9f38d476-7dbd-4214-98aa-e5858efdb0a4",
    "date": "2026-09-19",
    "startAddress": "人民广场",
    "endAddress": "外滩"
  }
]
```

## 20. 好友轨迹列表

### GET `/api/friends/{friendUserId}/tracks`

查看某个好友的每日轨迹列表，按日期倒序排列。只有双方已经是好友时才能查看；返回字段和“我的轨迹列表”一致，只返回每天的开始地址和结束地址。

请求头：

| 名称 | 必填 | 说明 |
| --- | --- | --- |
| Authorization | 是 | `Bearer {token}` |

请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| friendUserId | string | 是 | 好友用户 id，放在路径里 |

返回参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| trackId | string | 好友的轨迹 id |
| date | string | 轨迹日期，格式 `yyyy-MM-dd` |
| startAddress | string | 当天第一个轨迹点地址，没有地址时返回经纬度 |
| endAddress | string | 当天最后一个轨迹点地址，没有地址时返回经纬度 |

返回示例：

```json
[
  {
    "trackId": "9f38d476-7dbd-4214-98aa-e5858efdb0a4",
    "date": "2026-09-19",
    "startAddress": "人民广场",
    "endAddress": "外滩"
  }
]
```

常见错误：

| 状态码 | message | 说明 |
| --- | --- | --- |
| 400 | `好友不存在` | 对方不是当前用户好友 |

## 21. 轨迹详情

### GET `/api/tracks/{trackId}`

返回某一天轨迹的全部轨迹点。只能查看自己的轨迹。

请求头：

| 名称 | 必填 | 说明 |
| --- | --- | --- |
| Authorization | 是 | `Bearer {token}` |

请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| trackId | string | 是 | 轨迹 id，放在路径里 |

返回参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| trackId | string | 轨迹 id |
| date | string | 轨迹日期，格式 `yyyy-MM-dd` |
| startAddress | string | 开始地址 |
| endAddress | string | 结束地址 |
| startedAt | string/null | 开始时间 |
| endedAt | string/null | 结束时间 |
| distanceMeters | number | 轨迹总距离，单位米 |
| pointCount | number | 轨迹点数量 |
| points | array | 轨迹点列表，按 `recordedAt` 升序 |
| points[].latitude | number | 纬度 |
| points[].longitude | number | 经度 |
| points[].recordedAt | string | 轨迹点记录时间 |
| points[].address | string | 地址 |
| points[].accuracy | number/null | 定位精度 |
| points[].speed | number/null | 速度 |

返回示例：

```json
{
  "trackId": "9f38d476-7dbd-4214-98aa-e5858efdb0a4",
  "date": "2026-09-19",
  "startAddress": "人民广场",
  "endAddress": "外滩",
  "startedAt": "2026-09-19T08:00:00Z",
  "endedAt": "2026-09-19T08:10:00Z",
  "distanceMeters": 211.4,
  "pointCount": 3,
  "points": [
    {
      "latitude": 31.2304,
      "longitude": 121.4737,
      "recordedAt": "2026-09-19T08:00:00Z",
      "address": "人民广场",
      "accuracy": 12.5,
      "speed": 1.2
    }
  ]
}
```

## 22. 查询点位列表

### GET `/api/places`

请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| category | string | 否 | 按分类筛选 |
| q | string | 否 | 按名称或描述搜索 |

请求示例：

```text
GET /api/places?category=landmark&q=square
```

返回参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | string | 点位 id |
| name | string | 点位名称 |
| category | string | 分类 |
| latitude | number | 纬度 |
| longitude | number | 经度 |
| description | string/null | 描述 |
| createdAt | string | 创建时间 |
| updatedAt | string | 更新时间 |

返回示例：

```json
[
  {
    "id": "b7dbf03d-ed67-4215-ae31-33f49825048b",
    "name": "People's Square",
    "category": "landmark",
    "latitude": 31.2304,
    "longitude": 121.4737,
    "description": "Central Shanghai",
    "createdAt": "2026-09-19T06:00:00Z",
    "updatedAt": "2026-09-19T06:00:00Z"
  }
]
```

## 23. 查询点位详情

### GET `/api/places/{id}`

请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | string | 是 | 点位 id，放在路径里 |

返回参数：同“查询点位列表”的单个对象。

## 24. 创建点位

### POST `/api/places`

请求参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| name | string | 是 | 点位名称，最多 80 个字符 |
| category | string | 是 | 分类，最多 40 个字符 |
| latitude | number | 是 | 纬度，范围 `-90` 到 `90` |
| longitude | number | 是 | 经度，范围 `-180` 到 `180` |
| description | string/null | 否 | 描述，最多 500 个字符 |

请求示例：

```json
{
  "name": "People's Square",
  "category": "landmark",
  "latitude": 31.2304,
  "longitude": 121.4737,
  "description": "Central Shanghai"
}
```

返回参数：创建后的点位对象，字段同“查询点位列表”。

## 25. 更新点位

### PUT `/api/places/{id}`

请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | string | 是 | 点位 id，放在路径里 |

请求体字段同“创建点位”。

返回参数：更新后的点位对象，字段同“查询点位列表”。

## 26. 删除点位

### DELETE `/api/places/{id}`

请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | string | 是 | 点位 id，放在路径里 |

返回：`204 No Content`，无响应体。

## 27. 验证验证码

### POST `/api/verification-codes/verify`

这个接口主要用于调试。正常登录时前端不用单独调用它，`/api/login` 会自动校验验证码。

请求参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| target | string | 是 | 手机号 |
| code | string | 是 | 6 位验证码 |
| channel | string | 否 | `sms` 或 `email`，不传默认按短信处理 |

请求示例：

```json
{
  "target": "13800138000",
  "code": "123456",
  "channel": "sms"
}
```

返回参数：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| verified | boolean | 是否验证成功 |

返回示例：

```json
{
  "verified": true
}
```
