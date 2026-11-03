# MindMate

一个只给自己用的个人成长空间：把今天要做的事、正在纠结的选择、被打动的台词，安放在同一个地方。

项目当前以**本地单机**方式运行，后端连接本机达梦数据库，未做公网部署。

---

## 它想解决什么

大多数效率工具止步于「记下来」，但真正影响状态的往往是另外三件事：

1. **答应自己的事为什么没做** —— 只记录待办，看不到反复推迟的轨迹；
2. **做选择时脑子里一团乱** —— 顾虑、期待、价值观混在一起，越想越乱；
3. **有些话想留下来** —— 一句台词、一张截图，散落在相册和聊天记录里。

MindMate 分别用「任务事件流」「AI 决策对话」「经典语录」回应这三点，并刻意保持克制：不评分、不打卡、不制造压力。

## 功能

| 模块        | 说明                               | 状态 |
| --------- | -------------------------------- | -- |
| 工作台       | 今日清单、本周完成数、专注入口                  | 可用 |
| 任务清单      | 增删改查、状态流转、计划日期、推迟与耗时记录           | 可用 |
| 决策空间 · 对话 | 多会话，接入 DeepSeek，流式输出，Markdown 渲染 | 可用 |
| 决策空间 · 记忆 | 五项自述偏好与记忆开关，按需注入模型               | 可用 |
| 经典语录      | 文字 / 图片 / 视频三种载体，分列瀑布流展示         | 可用 |
| 回顾与洞察     | 预留页面                             | 占位 |
| 个人中心      | 头像、昵称、签名、邮箱                      | 可用 |

## 技术栈

**前端**：Vue 3（`<script setup>`）、Vite、Vue Router、axios，无 UI 组件库，样式手写。

**后端**：Spring Boot 2.6.13、Java 17、MyBatis-Plus、达梦 DM8、Redis、DeepSeek API。

**运行方式**：前后端分离，前端 5173，后端 8080；登录凭证存 Redis，滑动过期，不用 JWT。

## 架构

```
浏览器 (localhost:5173)
        │  axios + Authorization
        ▼
Spring Boot (localhost:8080)
        ├── 达梦 DM8 (localhost:5236, schema=MINDMATE)
        ├── Redis (localhost:6379)      登录 token、验证码限流
        └── DeepSeek API                决策对话
```

## 目录结构

```
MindMate/
├── MindMate_backend/                    Maven 多模块，当前只有一个业务模块
│   ├── pom.xml
│   ├── SQL/
│   │   ├── mindmate_schema.sql          全量建表脚本（达梦）
│   │   ├── migrate_user_avatar.sql
│   │   ├── migrate_decision_space.sql
│   │   └── migrate_quote.sql
│   └── mindmate-auth/
│       └── src/main/
│           ├── java/ruize/mindmateauth/
│           │   ├── controller/          认证、任务、决策、语录
│           │   ├── service/             业务实现、文件存储
│           │   ├── mapper/              MyBatis-Plus Mapper
│           │   ├── entity/ dto/ enums/
│           │   ├── config/              跨域、拦截器、静态资源、属性类
│           │   ├── interceptor/         登录态校验
│           │   └── ai/ decision/        模型调用与提示词组装
│           └── resources/
│               ├── application.yaml
│               └── application-local.yaml（已忽略，本地密钥）
├── MindMate_frontend/
│   └── src/
│       ├── views/                       LoginView、HomeView
│       ├── components/                  TaskList、DecisionSpace、QuoteWall 等
│       ├── api/                         按模块拆分的请求封装
│       └── router/
└── uploads/                             头像与语录媒体（已忽略）
```

## 快速开始

### 前置条件

- JDK 17、Maven 3.6+
- Node.js 18+
- 达梦 DM8（端口 5236）
- Redis（端口 6379）

### 1. 初始化数据库

创建模式并执行建表脚本。用 DIsql 执行前需把 UTF-8 文件转为 GBK，否则中文注释会乱码：

```bash
# 创建模式（DIsql 中必须另起一行用斜杠结束）
CREATE SCHEMA MINDMATE;
/

iconv -f UTF-8 -t GBK MindMate_backend/SQL/mindmate_schema.sql > /tmp/schema.sql
DIsql SYSDBA/你的密码@localhost:5236 < /tmp/schema.sql
```

### 2. 准备账号

登录只校验手机号是否已存在，没有注册接口，首次使用需先插入一行：

```sql
INSERT INTO MINDMATE.SYS_USER (PHONE_NUMBER, USERNAME) VALUES ('13800000000', '你的昵称');
```

### 3. 配置密钥

在 `mindmate-auth/src/main/resources/application-local.yaml` 中填入 DeepSeek 密钥（该文件已被 `.gitignore` 排除）：

```yaml
mindmate:
  ai:
    api-key: sk-你的密钥
    model: deepseek-flash
```

未配置密钥时应用仍可启动，只是决策空间会提示「模型尚未配置」。

### 4. 启动后端

```bash
cd MindMate_backend
mvn spring-boot:run
# 或在 IDE 中运行 MindmateAuthApplication
```

### 5. 启动前端

```bash
cd MindMate_frontend
npm install
npm run dev
```

访问 <http://localhost:5173>。

### 6. 登录

短信是**伪装实现**：不会真正下发，验证码直接随接口响应返回，页面上有「一键填入」。因此 `mindmate.auth.expose-sms-code` 仅适合本地使用，任何对外环境都必须换成真实短信服务。

## 主要配置项

| 配置                              | 默认值                                        | 说明                           |
| ------------------------------- | ------------------------------------------ | ---------------------------- |
| `server.port`                   | `8080`                                     | 后端端口                         |
| `spring.datasource.url`         | `jdbc:dm://localhost:5236?schema=MINDMATE` | 达梦连接                         |
| `spring.redis.host` / `port`    | `127.0.0.1` / `6379`                       | 登录态与限流                       |
| `mindmate.ai.api-key`           | 空                                          | 建议放 `application-local.yaml` |
| `mindmate.auth.token-ttl-hours` | `168`                                      | 登录凭证有效期，滑动续期                 |
| `mindmate.auth.expose-sms-code` | `true`                                     | 是否回传验证码，生产必须关闭               |
| `mindmate.avatar.directory`     | `./uploads/avatars`                        | 头像目录                         |
| `mindmate.quote.directory`      | `./uploads/quotes`                         | 语录媒体目录                       |

## 数据模型

| 表                     | 作用                        |
| --------------------- | ------------------------- |
| `SYS_USER`            | 用户，手机号唯一，表名避开达梦保留字 `USER` |
| `TASK`                | 任务当前状态快照，列表查询走它           |
| `TASK_EVENT`          | 不可变事件流，记录创建、推迟、开始、完成等行为   |
| `DECISION_PREFERENCE` | 用户自述的五项偏好与记忆开关，不从对话隐式提取   |
| `DECISION_SESSION`    | 决策会话                      |
| `DECISION_MESSAGE`    | 会话消息，模型成功后整轮落库            |
| `SYS_QUOTE`           | 经典语录，含一条可选媒体              |

三个设计取舍：

1. **快照 + 事件流双写**：列表读快照避免扫表，行为历史靠事件流沉淀，便于后续做推迟分析与建议；
2. **逻辑删除**：统一 `DELETED` 标记，媒体文件不做物理删除，避免并发读取冲突；
3. **不建物理外键**：关联完整性由应用层保证，为将来拆分留余地。

## 接口一览

| 方法                        | 路径                               | 说明                  |
| ------------------------- | -------------------------------- | ------------------- |
| `GET`                     | `/auth/sendSmsCode`              | 获取验证码（伪装实现）         |
| `POST`                    | `/auth/login`                    | 登录，返回 token 与用户信息   |
| `GET` / `PUT`             | `/auth/me`                       | 查询、修改当前用户           |
| `POST`                    | `/auth/avatar`                   | 上传头像，限 2MB          |
| `POST`                    | `/auth/logout`                   | 退出登录                |
| `GET` / `POST`            | `/tasks`                         | 任务列表与新建             |
| `PUT` / `DELETE`          | `/tasks/{id}`                    | 任务编辑与删除             |
| `PUT`                     | `/tasks/{id}/status`             | 状态流转，同时写入事件流        |
| `GET` / `POST` / `DELETE` | `/decisions`、`/decisions/{id}`   | 决策会话列表、新建、删除        |
| `GET` / `POST`            | `/decisions/{id}/messages`       | 消息拉取与提问（流式返回）       |
| `GET` / `PUT` / `DELETE`  | `/decisions/preferences`         | 读取、保存、清空记忆          |
| `GET`                     | `/decisions/config`              | 模型配置状态              |
| `GET` / `POST` / `DELETE` | `/quotes`                        | 语录读写，图片 5MB、视频 50MB |
| `GET`                     | `/media/quotes/**`、`/avatars/**` | 静态媒体，公开读取           |

## 已知限制

- 短信验证码为伪装实现，**不能直接用于公网部署**；
- 图片与视频存在本地磁盘，未接对象存储，多实例部署会失效；
- 单用户视角设计，权限只做到「按 user_id 隔离」，没有角色体系；
- 「回顾与洞察」尚未实现；
- 没有自动化测试与 CI。

## 开发约定

- 类注释统一 `@author` + `@since`；
- 达梦保留字冲突时，表名加 `SYS_` 前缀（如 `SYS_USER`、`SYS_QUOTE`）；
- 实体中 `VALUES`、`ROLE` 等保留字列需用 `@TableField("\"VALUES\"")` 显式加引号；
- SQL 脚本保持 UTF-8，执行前按需转码为 GBK。

## License

未附带开源协议。如需公开，请补充 `LICENSE` 文件。

---

作者：邸睿泽
