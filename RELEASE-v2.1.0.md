# v2.1.0 - 历史活跃度数据链修复版

## 修复

- 修正历史扫描流程中的数据生成稳定性问题。
- 优化 ActivityTracker 基线生成流程。
- 修复 Debug API 版本信息仍显示旧版本的问题。

## 改进

- 保留 Reactor 异步处理链。
- 增强历史活跃度诊断能力。
- 保持多作者文章支持。

## 测试

安装后访问：

```
/apis/api.activity.foxbridge.team/v1alpha1/calendar/debug?year=2026
```

检查：

- contentSuccessCount
- baselineRecordCount
- baselineTotalScore
- baselineErrors

