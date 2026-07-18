# v2.0.1 - Reactor 异步历史活跃度修复

## 修复

- 修复历史活跃度生成流程中 Reactor 链返回 null 导致的异常。
- 使用 `flatMap + Mono.empty()` 替代 Reactor mapper 中的 null 返回。
- 优化 baseline ActivityRecord.Spec 生成流程。

## 改进

- 保留历史扫描诊断接口。
- 保留 baselineErrors 错误输出。
- 提升历史文章转换为活跃记录的稳定性。

## 目标

修复：

- baselineRecordCount = 0
- baselineTotalScore = 0
- mapper returned a null value

问题。
