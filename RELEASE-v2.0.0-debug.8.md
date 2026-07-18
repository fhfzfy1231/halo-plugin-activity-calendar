# v2.0.0-debug.8 - 修复合并冲突并增强基线诊断

## 修复

- 修复 Git 合并冲突标记残留导致的 Java 编译失败。
- 修复源码中错误插入的 `\\n` 字符。
- 修复 ActivityCalendarEndpoint.java 编译问题。

## 调试增强

继续保留 Debug7：

- baselineErrors 输出；
- baselineSpec 生成诊断；
- 多作者分析；
- Snapshot 检查；
- 正文读取诊断；
- ActivityRecord 生成流程分析。

## 注意

本版本仍为 Debug 版本，用于定位历史活跃度无法生成问题。
