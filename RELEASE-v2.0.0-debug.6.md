# v2.0.0-debug.6 - 修复源码编译问题并增强诊断稳定性

## 修复

- 修复 Debug4 版本中 Java 源码插入错误转义字符导致的编译失败。
- 清理源码中的非法 `\\n` 字符。
- 修复 GitHub Actions `compileJava FAILED` 问题。

## 保留 Debug4 诊断能力

继续保留：

- baselineSpec 生成诊断；
- 多作者候选分析；
- Snapshot 检查；
- 正文读取检查；
- 活跃记录生成阶段诊断。

## 注意

本版本仍为 Debug 诊断版本，用于定位历史活跃度生成问题。
