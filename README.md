# Redis 延时队列可视化管理系统

基于 Spring Boot 3、Redis、Redisson、MySQL、MyBatis、Vue 3、Vite 和 Ant Design Vue 的延时队列管理系统。

系统支持三种 Redis 延时队列方案，延迟到期后调用外部接口，再执行回调接口，并把执行记录写入 MySQL。前端通过 SSE 实时接收执行日志，适合演示和验证多实例部署下“只消费一次、只推送一次”的链路。

## 功能

- 支持三种延时队列：ZSET 轮询、Redis Key 过期通知、Redisson 延迟队列。
- 支持多服务实例部署，使用 Redis `SETNX` 幂等锁防止重复调用外部接口。
- 支持外部接口调用配置：请求方法、URL、Headers、Body。
- 支持回调接口配置：请求方法、URL、Headers、Body。
- 支持前端保存接口配置，配置数据落 MySQL。
- 支持快速添加任务，只选择队列方案和延迟秒数即可触发完整演示链路。
- 支持 SSE 实时日志推送，前端按 `eventId` 去重展示。
- 支持 MySQL 查看最近任务记录。
- 支持清空队列、清空任务记录和幂等锁。

## 技术栈

后端：

- Java 17
- Spring Boot 3
- Spring Web
- Spring Data Redis
- Redisson
- MyBatis
- MySQL
- SSE

前端：

- Vue 3
- TypeScript
- Vite
- Ant Design Vue
- Pinia
- Axios

## 目录

```text
backend/
  src/main/java/com/openclaw/delayqueue/
    controller/       HTTP 接口
    queue/            三种延时队列实现
    consumer/         队列消费入口
    service/          任务执行、HTTP 调用
    sse/              SSE 与 Redis Pub/Sub
    mapper/           MyBatis Mapper
    repository/       数据访问封装
  src/main/resources/
    application.yml   后端配置
    schema.sql        MySQL 表结构自动初始化

frontend/
  src/App.vue         主页面
  src/api/            前端接口封装
  src/stores/         SSE 日志状态
  src/styles/         页面样式
```

## 环境要求

- JDK 17
- Maven
- Node.js 18+
- Redis 6379
- MySQL 3306

默认 MySQL 配置：

```text
database: redis_delayqueue
username: root
password: 12345678
port: 3306
```

创建数据库：

```bash
mysql -uroot -p12345678 -h127.0.0.1 -P3306 -e "CREATE DATABASE IF NOT EXISTS redis_delayqueue DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

表结构由 `backend/src/main/resources/schema.sql` 自动初始化。

## 后端配置

配置文件：`backend/src/main/resources/application.yml`

默认配置：

```yaml
server:
  port: ${SERVER_PORT:8080}

spring:
  datasource:
    url: ${MYSQL_URL:jdbc:mysql://localhost:3306/redis_delayqueue?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true}
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:12345678}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

delay-queue:
  instance-id: ${INSTANCE_ID:${spring.application.name}-${server.port}}
  sse-channel: delay:queue:sse:events
  http-timeout-seconds: ${HTTP_TIMEOUT_SECONDS:10}
```

如果本机 `8080` 被占用，可以使用 `18080`：

```bash
cd backend
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home SERVER_PORT=18080 INSTANCE_ID=node-18080 mvn spring-boot:run
```

## 前端启动

```bash
cd frontend
npm install
VITE_API_TARGET=http://localhost:18080 npm run dev
```

如果 `5173` 被占用，Vite 会自动切到下一个端口，例如：

```text
http://localhost:5174/
```

## 当前本机启动地址

如果按本项目当前调试方式启动：

- 前端：`http://localhost:5174/`
- 后端：`http://localhost:18080`

## 页面使用

快速添加：

1. 在“快速添加”区域选择队列方案。
2. 填写延迟秒数。
3. 点击“添加任务”。
4. 到期后会自动调用后端 mock 外部接口和 mock 回调接口。
5. 右侧“实时执行日志”收到 SSE 推送，MySQL 任务记录同步刷新。

完整配置：

1. 填写队列方案、任务说明、延迟秒数。
2. 配置外部接口请求方法、URL、Headers、Body。
3. 配置回调接口请求方法、URL、Headers、Body。
4. 点击“发送”。
5. 如需复用配置，填写配置名称后点击“保存配置”。

## 核心接口

发送延时任务：

```http
POST /delay-queue/send
Content-Type: application/json
```

示例：

```json
{
  "queueType": "redisson",
  "content": "订单超时检查",
  "delayTime": 5,
  "requestMethod": "POST",
  "requestUrl": "http://localhost:18080/mock/external",
  "requestHeaders": "{\"Content-Type\":\"application/json\"}",
  "requestBody": "{\"orderId\":\"1001\"}",
  "callbackMethod": "POST",
  "callbackUrl": "http://localhost:18080/mock/callback",
  "callbackHeaders": "{\"Content-Type\":\"application/json\"}",
  "callbackBody": ""
}
```

查询任务记录：

```http
GET /delay-queue/tasks
```

SSE 连接：

```http
GET /delay-queue/sse/{clientId}
```

保存接口配置：

```http
POST /delay-queue/configs
```

查询接口配置：

```http
GET /delay-queue/configs
```

删除接口配置：

```http
POST /delay-queue/configs/{id}/delete
```

清空队列和任务：

```http
POST /delay-queue/clear
```

## 多实例防重复消费

多台后端服务同时部署时，可能出现多个实例都监听到同一条到期消息的情况。项目在真正执行 HTTP 外部接口之前会先申请 Redis 幂等锁：

```text
delay:queue:consumed:{messageId}
```

实现方式：

- 使用 Redis `SETNX`，只允许一个实例写入成功。
- 写入成功的实例继续调用外部接口和回调接口。
- 写入失败的实例直接跳过。
- 幂等锁默认保留 24 小时。

对应代码：

- `DelayTaskExecutorService.executeOnce`
- `DelayQueueEventPublisher.acquireConsumeLock`

## SSE 推送设计

任务执行完成后，后端会把执行日志发布到 Redis Pub/Sub：

```text
delay:queue:sse:events
```

每个后端实例都可以订阅该频道，并推送给自己连接的前端浏览器。前端日志 store 使用 `eventId` 做本地去重，避免多实例广播时页面重复展示。

对应代码：

- 后端发布：`DelayQueueEventPublisher.publish`
- 后端连接：`SseServer.connect`
- 前端接收：`App.vue -> connectSse`
- 前端去重：`stores/logStore.ts`

## 三种队列方案

ZSET 轮询：

- 写入 Redis ZSET，score 为到期时间戳。
- 定时任务每 100ms 拉取到期消息。
- Lua 脚本保证读取和删除原子执行。

Redis Key 过期通知：

- 写入带 TTL 的 Redis Key。
- Key 到期后通过 Redis keyspace notification 触发消费。
- 需要 Redis 支持 `notify-keyspace-events`。

Redisson 延迟队列：

- 使用 Redisson `RDelayedQueue`。
- 到期后进入阻塞队列，由后台线程消费。
- 适合生产场景优先使用。

## 验证命令

发送一条 2 秒 Redisson 任务：

```bash
curl -s -X POST http://localhost:18080/delay-queue/send \
  -H 'Content-Type: application/json' \
  -d '{"queueType":"redisson","content":"startup-smoke","delayTime":2,"requestMethod":"POST","requestUrl":"http://localhost:18080/mock/external","requestHeaders":"{\"Content-Type\":\"application/json\"}","requestBody":"{\"bizId\":\"startup\"}","callbackMethod":"POST","callbackUrl":"http://localhost:18080/mock/callback","callbackHeaders":"{\"Content-Type\":\"application/json\"}"}'
```

查询最近任务：

```bash
mysql -uroot -p12345678 -h127.0.0.1 -P3306 redis_delayqueue \
  -e "SELECT message_id,content,status,request_status,callback_status,instance_id FROM delay_task ORDER BY id DESC LIMIT 5;"
```

## 构建

后端：

```bash
cd backend
mvn test
```

前端：

```bash
cd frontend
npm run build
```
