# ProxyChat

![:ProxyChat](https://count.getloli.com/@railgun19457_ProxyChat?name=railgun19457_ProxyChat&theme=minecraft&padding=6&offset=0&align=top&scale=1&pixelated=1&darkmode=auto)

ProxyChat 是一个运行在 Velocity 上的跨服聊天插件，提供跨子服聊天转发、进出服广播和 `/at` 提醒功能。

## 功能特性

- 跨服聊天转发（可按子服控制发送与接收）
- 聊天正则过滤（命中规则后不转发）
- 支持 MiniMessage
- 支持自定义消息格式
- 首次进服、切服、离服广播
- `/at` 玩家提醒（消息、标题、ActionBar）

## 运行环境

- Java 17+
- Velocity 3.3+

## 安装

1. 从 Release 下载插件 Jar（或本地构建）。
2. 放入 Velocity 的 `plugins` 目录。
3. 启动代理端，首次启动会自动生成配置文件：
   - `plugins/proxychat/config.toml`
   - `plugins/proxychat/message.toml`

## 命令

- `/proxychat reload` 重载配置文件
- `/pc reload` 重载配置文件
- `/at <player> [message]` 发送@消息

## 权限

- `proxychat.command.reload`：允许执行重载命令
- `proxychat.command.at`：允许使用 `/at`
- `proxychat.notify.update`：接收更新提醒

## 配置概览

`config.toml` 主要分区：

- `[update]`：更新检查与管理员通知
- `[logging]`：日志控制
  - `print-chat`：是否在控制台打印转发聊天
  - `debug`：是否启用调试日志
- `[chat]`：跨服聊天开关与格式模板
- `[chat-routing]`：按子服控制聊天发送/接收
- `[chat-filter]`：正则过滤规则
- `[server-alias]`：子服别名映射
- `[join-leave]`：进服/切服/离服广播
- `[at]`：点名提醒配置

`message.toml` 用于自定义插件提示文本。

## 本地构建

```bash
./gradlew.bat clean build
```

构建产物位于：

- `build/libs/ProxyChat-<version>.jar`

## 许可证

建议使用 MIT License。
