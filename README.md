# Jason-Bot-Fix

你的 Minecraft 服务器 AI 伙伴——基于大语言模型的智能虚拟玩家，能与玩家自由对话、查询游戏状态、执行指令，甚至联网搜索最新资讯。

## 目录

- [模组简介](#模组简介)
- [核心特性](#核心特性)
- [安装与使用](#安装与使用)
- [配置文件详解](#配置文件详解)
- [模组架构与实现](#模组架构与实现)
- [AI 工具列表](#ai-工具列表)
- [更新日志](#更新日志)
- [后续移植建议](#后续移植建议)
- [许可证](#许可证)

---

## 模组简介

**Jason-Bot-Fix** 是一个 Minecraft Forge 服务端模组。它在服务器中创建一个名为 **Jason（杰森）** 的 AI 虚拟玩家，玩家在聊天中 @ 他即可触发对话。Jason 不仅能闲聊，还能通过 **Function Calling（工具调用）** 机制实时查询游戏状态、执行原版指令、联网搜索，让 AI 真正"活"在 Minecraft 世界里。

> **注意：本项目为Jason-Bot`https://github.com/CPearl0/Jason-Bot`的Fork项目，并经过修改和优化，用于学习和研究。**

| 项目 | 信息 |
|---|---|
| Minecraft 版本 | 1.20.1 |
| 模组加载器 | Forge 47.3.11 |
| Java 版本 | 17 |
| 默认 AI 模型 | DeepSeek-Chat（兼容所有 OpenAI 格式 API） |
| 模组类型 | 服务端（Server-side only） |

---

## 核心特性

### 智能对话
- 基于大语言模型的多轮对话，支持语境记忆
- 可自定义 AI 身份、性格、系统提示词
- 支持多唤醒词（默认 "Jason" 和 "杰森"），消息以唤醒词开头或结尾时触发

### 游戏状态感知（Tool Calling）
AI **不会**一次性收到所有游戏信息，而是按需调用工具获取：

| 工具 | 功能 |
|---|---|
| `get_player_info` | 查询玩家维度、坐标、生物群系、生命值、饥饿值、经验、游戏模式 |
| `get_player_equipment` | 查询玩家主手/副手物品及完整 NBT 数据、护甲栏 |
| `get_looking_at` | 射线检测玩家视线指向的方块或流体 |
| `get_server_info` | 查询服务器在线人数、最近的玩家姓名与坐标 |
| `get_real_time` | 获取现实世界当前时间 |

### 指令执行
- AI 可以执行 Minecraft 原版指令（如 `/give`、`/time set`、`/weather`）
- 可配置权限等级（0-4），默认 2（等同于命令方块）
- 内置指令黑名单，`stop`/`op`/`ban`/`kick` 等敏感指令默认禁用

### 联网搜索
- AI 可联网搜索最新 Minecraft 资讯（版本特性、模组攻略、合成配方等）
- 支持 AnySearch API（POST + JSON body 格式）
- 搜索结果自动结构化解析，返回 title/url/snippet

### 安全可控
- 所有功能均可在配置文件中独立开关
- 指令黑名单防止恶意操作
- 错误处理完善，异常不会导致服务器崩溃

---

## 安装与使用

### 前置要求
- Minecraft 1.20.1 服务端（Forge 47.3.11）
- Java 17+
- DeepSeek API Key（或任何兼容 OpenAI Chat Completions 格式的 API）

### 安装步骤

1. 下载 `Jason-Bot-1.20.1-0.1.0.jar`
2. 放入服务器 `mods/` 文件夹
3. 启动服务器，模组会自动生成配置文件
4. 关闭服务器，编辑 `config/jasonbot-server.toml`
5. 填入你的 API Key 和其他配置
6. 重新启动服务器，在聊天中喊 "Jason" 或 "杰森" 即可开始对话

### 使用示例

```
玩家: Jason，你在吗？
Jason: 我在呢！有什么需要帮忙的吗？

玩家: 杰森，我现在在哪？
Jason: [调用 get_player_info] 你现在在主世界(x:128, y:64, z:-256)，位于平原生物群系，生命值满的！

玩家: Jason，给我一组钻石
Jason: [调用 execute_command] 已经给你64个钻石啦，检查背包吧 💎

玩家: Jason，1.21的重锤怎么做？
Jason: [调用 web_search] 重锤（Mace）是1.21的新武器，需要1个重型核心 + 1个微风棒在锻造台合成...
```

---

## 配置文件详解

配置文件路径：`config/jasonbot-server.toml`

### AI 接口配置

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `APIEndpoint` | String | `https://api.deepseek.com/v1/chat/completions` | API 端点，兼容 OpenAI 格式 |
| `AIModel` | String | `deepseek-chat` | 模型名称 |
| `APIKey` | String | `Enter your api key here` | API 密钥，**必须填写** |

### 模型参数

| 配置项 | 类型 | 范围 | 默认值 | 说明 |
|---|---|---|---|---|
| `temperature` | Double | 0.0 ~ 2.0 | `1.0` | 模型温度，越高越随机 |
| `presencePenalty` | Double | -2.0 ~ 2.0 | `1.0` | 存在惩罚，减少重复 |

### 角色与对话

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `assistantName` | String | `Jason` | AI 在游戏中的显示名称 |
| `systemPrompt` | String | 中文提示词 | 系统提示词，定义 AI 身份和性格 |
| `maxHistorySize` | Integer | `24` | 对话记忆条数，设为 `0` 禁用记忆（无上下文模式） |
| `wakeNames` | List\<String\> | `["Jason", "杰森"]` | 唤醒词列表 |

### 功能开关

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `useInGameInformation` | Boolean | `true` | 是否启用游戏信息查询工具（Tool Calling） |

### 指令执行配置

| 配置项 | 类型 | 范围 | 默认值 | 说明 |
|---|---|---|---|---|
| `commandPermissionLevel` | Integer | 0 ~ 4 | `2` | 指令执行权限等级（2 = 命令方块级别） |
| `commandBlacklist` | List\<String\> | `["stop", "kick", "ban", ...]` | 禁止执行的指令根名列表 |

### 联网搜索配置

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `webSearchEnabled` | Boolean | `false` | 是否启用联网搜索 |
| `webSearchEndpoint` | String | 占位文本 | 搜索 API 端点（兼容 AnySearch API） |
| `webSearchAPIKey` | String | 占位文本 | 搜索 API 密钥 |

---

## 模组架构与实现

### 项目结构

```
src/main/java/io/github/cpearl0/jasonbot/
├── JasonBot.java              模组主入口，注册配置
├── Config.java                配置文件管理（ForgeConfigSpec）
├── event/
│   └── EventHandler.java      事件监听（聊天唤醒、服务器启停）
└── bot/
    ├── PromptGenerator.java    动态生成系统提示词（附带工具使用指引）
    ├── ChatHistory.java        对话历史管理（线程安全的环形队列）
    ├── AsyncAIChat.java        异步 AI API 通信核心（支持 Tool Calling 循环）
    └── GameTools.java          6 个 Tool 的定义与执行逻辑
```

### 核心工作流程

```
玩家发送聊天消息
    │
    ▼
EventHandler.onChat() ─── 不匹配 ──→ 忽略
    │ 匹配唤醒词
    ▼
AsyncAIChat.chat(player, message)
    │
    ├── 将用户消息存入 ChatHistory
    ├── 构建 API 请求（系统提示词 + 历史消息 + Tool 定义）
    │
    ▼
━━━━━━━━ HTTP POST 到 AI API ━━━━━━━━
    │
    ├── AI 返回文本 → 直接回复玩家
    │
    ├── AI 返回 tool_calls:
    │   ├── 执行对应 Tool（GameTools.executeTool）
    │   ├── 将工具结果发回 AI 继续推理
    │   └── 最多循环 5 轮
    │
    ▼
以 FakePlayer 身份广播回复到服务器聊天栏
```

### 关键设计决策

**工具调用不污染历史**：AI 调用工具产生的中间消息（assistant tool_calls + tool results）只在单次请求的多轮循环中传递，不写入持久化的 `ChatHistory`，避免后续对话被工具中间数据干扰。

**全局共享记忆**：当前版本所有玩家共享一个 `ChatHistory` 实例，AI 能通过每条消息的 `name` 字段识别说话者。未来的版本可能会支持每个玩家独立记忆。

**按需获取游戏信息**：AI 不再一次性收到全部游戏状态数据，而是通过 Tool Calling 按需查询，大幅减少无效 token 消耗，也让 AI 的行为更加"智能"。

**原生 HTTP 通信**：不依赖第三方 HTTP 库（如 OkHttp），直接使用 `HttpURLConnection` 进行 API 通信，减少依赖冲突风险。

---

## AI 工具列表

### get_player_info
- **触发场景**："我在哪？""我的血量？""我的游戏模式？"
- **返回数据**：维度、坐标、生物群系、生命值/最大生命、饥饿值、饱腹度、经验等级、游戏模式

### get_player_equipment
- **触发场景**："我拿着什么？""我穿了什么装备？"
- **返回数据**：主手物品及完整 NBT、副手物品及 NBT、护甲栏全部物品及 NBT

### get_looking_at
- **触发场景**："面前是什么方块？""这里能挖吗？"
- **返回数据**：20 格视线射线检测结果，返回方块或流体的注册名

### get_server_info
- **触发场景**："服务器有多少人？""谁离我最近？"
- **返回数据**：在线人数、最近玩家的姓名与坐标

### get_real_time
- **触发场景**：讨论现实世界时间
- **返回数据**：`yyyy-MM-dd HH:mm:ss` 格式的当前时间

### execute_command
- **触发场景**：需要执行游戏操作
- **参数**：`command` - 无 `/` 前缀的指令字符串
- **安全机制**：黑名单检查 → 权限等级限制 → 执行

### web_search（需配置启用）
- **触发场景**：询问最新版本特性、模组攻略、合成配方等
- **参数**：`query` - 搜索关键词
- **返回数据**：结构化搜索结果（title/url/snippet）

---

## 更新日志

### v0.1.0（当前版本）

**全新架构：Tool Calling**
- 将 Prompt 拼接游戏信息的方式重构为 Function Calling 模式
- AI 按需调用工具获取游戏信息，减少 token 消耗

**新增 Tool：指令执行**
- AI 可执行 Minecraft 原版指令
- 可配置权限等级和指令黑名单

**新增 Tool：联网搜索**
- AI 可调用外部搜索 API 获取实时信息
- 支持 AnySearch API（POST JSON 格式）
- 搜索结果自动结构化解析

**代码改进**
- 修复 `getOnPos()` → `blockPosition()` 弃用警告
- 改进错误处理，HTTP 请求失败时返回更友好的错误信息
- 切换到 JDK 17 编译

---

## 后续移植建议

### 多平台移植路线

当前模组基于 Forge 1.20.1，若需移植到 Fabric / NeoForge / 更高版本，建议采用以下策略：

#### 短期：Architectury（推荐）
[Architectury](https://docs.architectury.dev/) 提供跨加载器抽象 API，一次编写，同时输出 Forge、Fabric 两个平台的 jar：
- 核心逻辑（`GameTools`、`ChatHistory`、`AsyncAIChat`、`PromptGenerator`）放入 `common` 模块，这些代码仅依赖 vanilla Minecraft API
- 加载器胶水代码（`@Mod`、FakePlayer、Config 系统）放入 `forge`/`fabric` 子模块，各约 20 行

#### 长期：Multi-Module Gradle（最灵活）
不依赖第三方抽象，通过接口注入解耦：
```
jason-bot/
├── common/        ← 纯 vanilla 代码 + PlatformHelper 接口定义
├── forge/         ← Forge 平台实现
├── fabric/        ← Fabric 平台实现
└── neoforge/      ← NeoForge 平台实现
```

#### 高版本适配要点

| 关注点 | 说明 |
|---|---|
| 聊天系统 | 1.19+ 引入聊天签名，`PlayerChatMessage` API 有变化，建议改用 `Component` 系统发消息 |
| FakePlayer | 高版本 Forge 中 FakePlayer 签名变化，Fabric 中需自行实现 |
| Mojang 映射改名 | 如 `getOnPos()` → `blockPosition()`，查阅对应版本的 MCP 映射 |
| Config 系统 | 建议改用 JSON + Gson，一次实现全平台通用 |

### 功能扩展建议

- [ ] **每个玩家独立记忆**：`Map<UUID, ChatHistory>` 代替全局单例
- [ ] **流式响应（SSE）**：支持 AI 逐字输出，提升交互体验
- [ ] **多语言扩展**：支持更多语言的系统提示词和 Tool 描述
- [ ] **速率限制**：防止玩家滥用 AI 功能
- [ ] **黑名单/白名单玩家**：控制哪些玩家可以使用 AI 功能

---

## 许可证

本项目采用 MIT 许可证。

---

> 让 AI 走进你的 Minecraft 世界，Jason 在这里等你！🎮