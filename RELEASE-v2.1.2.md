# v2.1.2 - 公开页面加载与旧路由残留修复版

## 问题原因

- Halo 默认保护插件的 `/apis/**` 接口，未登录访客请求日历 JSON 时会被重定向到登录页，前端因此显示“活跃度数据加载失败”。
- 服务器中仍有旧版 `v1alpha1` 端点实例响应请求，所以即使 JAR 清单显示 2.1.1，旧调试地址仍会返回 `2.0.0-debug.4`。

## 修复

- 日历数据由服务端在渲染页面时嵌入为安全的 JSON，公开页面不再依赖游客访问 `/apis/**`。
- 嵌入 JSON 对 `<`、`>`、`&` 和 Unicode 行分隔符进行转义，避免内容意外结束 `<script>` 标签。
- 前端优先读取嵌入数据，仅在嵌入数据缺失时使用管理端 API 回退。
- 自定义端点从 `v1alpha1` 迁移至 `v1alpha2`，绕过旧类或旧路由残留。
- 调试结果新增以下不可混淆的运行标识：
  - `pluginVersion: 2.1.2`
  - `apiVersion: v1alpha2`
  - `buildSignature: hac-2.1.2-212`
- 最近 10 年数据只扫描一次，并设置 15 秒内存缓存，降低页面渲染开销。
- 保留 2.1.1 对 Reactor 阻塞设置读取问题的修复。

## 升级后验证

1. 在 Halo 后台停用旧插件，上传 2.1.2 JAR 后重新启用。
2. 以管理员身份访问新的调试地址：

   ```text
   /apis/api.activity.foxbridge.team/v1alpha2/calendar/debug?year=2026&build=212
   ```

3. 确认返回值包含：
   - `pluginVersion` 为 `2.1.2`；
   - `apiVersion` 为 `v1alpha2`；
   - `buildSignature` 为 `hac-2.1.2-212`；
   - `status` 为 `completed`；
   - `baselineRecordCount` 和 `baselineTotalScore` 大于 0。
4. 退出 Halo 登录或使用无痕窗口打开包含活跃日历的公开页面，确认日历正常显示。

> 旧的 `v1alpha1` 调试地址可能继续返回旧插件结果，2.1.2 不再使用该地址。
