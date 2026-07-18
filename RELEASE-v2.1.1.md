# v2.1.1 - 历史活跃度阻塞异常修复版

## 修复

- 修复历史基线生成期间调用 Halo 同步设置接口导致的 Reactor 阻塞异常：
  `blockOptional() is blocking, which is not supported in thread reactor-tcp-epoll-*`。
- 修复正文已经读取成功，但 `baselineRecordCount`、`baselineTotalScore` 和前台活跃度仍为 0 的问题。
- 修复插件清单与 Gradle 构建版本不一致的问题。

## 改进

- 将同步设置读取隔离到 Reactor 的 bounded-elastic 调度线程。
- 每次日历统计只读取一次评分设置，并在整次基线计算中复用。
- 基线诊断每次重新执行时清理旧错误，错误项增加对应内容标识。
- 调试接口版本更新为 `2.1.1`。

## 验证

升级并启用插件后访问：

```text
/apis/api.activity.foxbridge.team/v1alpha1/calendar/debug?year=2026
```

预期结果：

- `pluginVersion` 为 `2.1.1`；
- `status` 为 `completed`；
- `baselineRecordCount` 大于 0；
- `baselineTotalScore` 大于 0；
- `baselineErrors` 中不再出现 `blockOptional() is blocking`。

