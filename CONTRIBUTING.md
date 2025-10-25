# Contributing to Lessup Remind

感谢你的贡献！请先阅读以下说明以便更高效地协作。

## 开发流程
- Fork 仓库并创建分支（feature/xxx 或 fix/xxx）
- 保持提交小而清晰，提交信息采用英文或中文的祈使句态
- 提交 PR 前确保：
  - 编译通过（Android Studio / Gradle）
  - 功能自测通过
  - 更新必要的文档与 changelog

## 代码规范
- Kotlin 代码遵循官方风格（建议启用 Ktlint/Detekt，本仓库将后续引入）
- 不在中间位置新增 import；保持 import 在文件顶部
- 禁止在 PR 中混入无关的格式化改动

## 提交 PR
- 描述本次变更的动机、实现方案与影响范围
- 附上截图或录屏（如涉及 UI 变更）
- 关联 Issue（如有）

## Issue 报告
- 使用模板，提供必需信息：版本、设备、复现步骤、期望与实际结果
- 如为新功能建议，请描述场景与动机

## 安全报告
- 请勿在公开 Issue 报告安全问题，按 SECURITY.md 说明私下报告

谢谢你的贡献！
