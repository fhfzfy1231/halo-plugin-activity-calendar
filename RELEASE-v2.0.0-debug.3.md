# v2.0.0-debug.3 - 修复空 Snapshot 崩溃并增强完整诊断

## 修复
- 修复 `Flux.just(...)` 遇到 null Snapshot 时抛出 `NullPointerException: The 2th array element was null` 的问题。
- Snapshot 列表改为先过滤空值，再通过 `Flux.fromIterable(...)` 读取。

## 调试增强
- 每篇内容新增 STEP1–STEP6 诊断标记。
- 新增 `failAt`，直接显示失败阶段。
- 新增 `allAuthors` / `authorCandidates`，允许一篇文章出现多个作者候选。
- 新增正文长度、标准化长度、统计单位数量。
- 新增 `activityWouldGenerate`，判断该内容是否具备生成活跃记录的条件。
- 汇总新增 `snapshotReadFailureItems` 与 `activityCandidateCount`。

> 此版本仍为 Debug 诊断版，仅用于定位活跃度统计问题。
