# v2.0.0-debug.7 - 修复 HeadProcessor 编译错误

## 修复

- 修复 `ActivityCalendarHeadProcessor.java` 中 HTML 字符串换行导致的 Java 编译失败。
- 修复 `unclosed string literal` 错误。
- 修复 HTML 标签被错误解析为 Java 代码的问题。

## 保留

- Debug6 全部历史活跃度诊断功能。
- baselineSpec 生成诊断。
- 多作者分析。
- Snapshot 检查。
- 正文读取检查。

## 注意

本版本仍为 Debug 诊断版本。
