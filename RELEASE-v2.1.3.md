# v2.1.3 - Halo 插件启动修复版

## 修复

- 修复 2.1.2 启动时出现的 `UnsatisfiedDependencyException`。
- 修复 Halo 插件独立 Spring 上下文中没有可注入 `ObjectMapper` Bean 时，`ActivityCalendarHeadProcessor` 无法创建的问题。
- JSON 序列化器改为由插件内部创建并复用，不再依赖 Halo 主程序提供 Jackson Bean。
- 空数据回退响应改为统一读取版本常量，避免返回旧版本标识。

## 保留的 2.1.2 修复

- 公开页面通过服务端嵌入数据显示日历，不要求游客访问受保护的 `/apis/**`。
- 管理端调试接口使用 `v1alpha2`，避开旧 `v1alpha1` 路由残留。
- 最近 10 年数据一次扫描完成，并使用 15 秒内存缓存。

## 升级后验证

1. 停用并卸载 2.1.2，重启 Halo 后安装 2.1.3。
2. 确认插件能够正常启用，不再出现缺少 `ObjectMapper` Bean 的异常。
3. 以管理员身份访问：

   ```text
   /apis/api.activity.foxbridge.team/v1alpha2/calendar/debug?year=2026&build=213
   ```

4. 确认返回值包含：
   - `pluginVersion: 2.1.3`
   - `apiVersion: v1alpha2`
   - `buildSignature: hac-2.1.3-213`
   - `status: completed`
5. 使用无痕窗口打开公开页面，确认活跃日历可以正常显示。
