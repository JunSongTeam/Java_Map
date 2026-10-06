# Map Java Backend

Spring Boot 后端服务，用来维护地图点位数据。

## 技术栈

- Java 21
- Spring Boot 3.2.5
- Maven
- 内存存储，方便先开发接口；后续可替换成数据库
- 内存 token，默认永不过期；服务重启后 token 会失效

## 运行

```bash
mvn spring-boot:run
```

服务默认运行在 `http://localhost:8088`。

## 接口

完整对接文档见 [API.md](./API.md)。

### 健康检查

```bash
curl http://localhost:8088/api/health
```

### 发送验证码

```bash
curl -X POST http://localhost:8088/api/send/code \
  -H 'Content-Type: application/json' \
  -d '{
    "phone": "13800138000"
  }'
```

开发模式下响应会返回 `devCode`，方便前端联调。
当前本地默认验证码固定为 `123456`，登录前仍然先调一次发送验证码接口。

```json
{
  "phone": "13800138000",
  "expiresInSeconds": 300,
  "expiresAt": "2026-08-10T13:00:00Z",
  "devCode": "123456"
}
```

### 校验验证码

一般情况下前端不用单独调用这个接口，`/api/login` 会自动校验验证码。

```bash
curl -X POST http://localhost:8088/api/verification-codes/verify \
  -H 'Content-Type: application/json' \
  -d '{
    "target": "13800138000",
    "code": "123456",
    "channel": "sms"
  }'
```

验证码默认 5 分钟过期，校验成功后会立即失效。生产环境不要返回 `devCode`：

```yaml
auth:
  verification-code:
    expiration-seconds: 300
    expose-code: false
    default-code: ""
```

当前配置为永不过期：

```yaml
auth:
  token:
    never-expires: true
```

生产环境建议改成有过期时间：

```yaml
auth:
  token:
    never-expires: false
    expiration-seconds: 604800
```

### 登录

手机号不存在时会自动创建用户，所以不需要单独注册接口。

```bash
curl -X POST http://localhost:8088/api/login \
  -H 'Content-Type: application/json' \
  -d '{
    "phone": "13800138000",
    "code": "123456"
  }'
```

返回里只包含 token 信息。

返回示例：

```json
{
  "tokenType": "Bearer",
  "token": "...",
  "expiresAt": null,
  "expiresInSeconds": null
}
```

### 获取资料

```bash
curl http://localhost:8088/api/me \
  -H "Authorization: Bearer {token}"
```

返回示例：

```json
{
  "userId": "...",
  "phone": "13800138000",
  "nickname": "User 8000",
  "avatarUrl": "/api/users/{id}/avatar",
  "memberStatus": "inactive",
  "memberExpiresAt": null
}
```

### 退出登录

```bash
curl -X POST http://localhost:8088/api/logout \
  -H "Authorization: Bearer {token}"
```

### 创建点位

```bash
curl -X POST http://localhost:8088/api/places \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "People'\''s Square",
    "category": "landmark",
    "latitude": 31.2304,
    "longitude": 121.4737,
    "description": "Central Shanghai"
  }'
```

### 查询点位

```bash
curl 'http://localhost:8088/api/places?category=landmark&q=square'
```

### 更新点位

```bash
curl -X PUT http://localhost:8088/api/places/{id} \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "People'\''s Square",
    "category": "landmark",
    "latitude": 31.2304,
    "longitude": 121.4737,
    "description": "Updated description"
  }'
```

### 删除点位

```bash
curl -X DELETE http://localhost:8088/api/places/{id}
```

## 测试

```bash
mvn test
```
