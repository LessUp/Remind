# Lessup Remind

一款简洁优雅的提醒类 Android 应用：
- 记录物品购买与保质期，显示距到期还有多久；若无保质期，显示已购买多久
- 记录会员订阅：名称、购买日期、价格（CNY）、到期时间、剩余天数
- 每日概览与单项到期提醒（WorkManager + 通知）
- 支持搜索与筛选、CSV 导出

## 功能特性
- 物品管理：名称、购买日期、保质期天数（可自动推算到期日）、备注
- 会员管理：名称、提供方、购买日期、价格（CNY）、到期日、自动续费、备注
- 列表状态：正常/临期/已过期/无保质期（按 DataStore 的临期阈值实时生效）
- 提醒：
  - 每日概览（在设置的提醒时间触发）
  - 单项提醒（到期前 7/3/1 天、当天、过期后 1 天）
- 搜索与筛选：按名称关键字，按状态过滤
- 导出：在设置页导出物品/会员 CSV

## 技术栈
- Kotlin、Jetpack Compose、Material3
- MVVM、Hilt、Room、WorkManager、Navigation、DataStore
- kotlinx-datetime 统一处理日期

## 架构概览
- data：Room 数据库、DAO 与 Repository；DataStore 保存阈值与提醒时刻
- ui：Compose 界面，ViewModel 提供 UI 状态
- reminder：通知与 WorkManager 调度（每日概览与单项提醒）
- 详细设计见 docs/ARCHITECTURE.md

## 权限说明
- POST_NOTIFICATIONS：用于显示提醒通知（Android 13+ 启动时会请求权限）
- RECEIVE_BOOT_COMPLETED：设备重启后重建定时任务

## 开发与运行
1. 要求：Android Studio（Giraffe+）、JDK 17、Android SDK 35
2. 克隆仓库并用 Android Studio 打开
3. 直接运行 app 模块（minSdk 24）
4. 首次启动授予“通知权限”，以确保可以接收提醒

## 导出 CSV
- 设置页中提供“导出物品 CSV / 导出会员 CSV”，通过系统文件选择器创建文件
- 价格单位为 CNY，保留两位小数

## 路线图（Roadmap）
- 功能扩展
  - CSV 导入与数据校验
  - 本地/云备份与恢复
  - 列表高级筛选/排序、滑动操作
  - 桌面小部件、通知 Snooze、通知开关细化
  - 多语言（zh / en）与深色模式优化
- 工程与质量
  - Ktlint/Detekt 规范检查
  - 单元测试（日期/阈值/DAO）与基础 UI 测试
  - Proguard 与发布签名流程文档
  - CI：构建/测试/Lint 集成；Dependabot 依赖更新

## 贡献
- 欢迎提交 Issue 与 Pull Request
- 详细流程与规范见 CONTRIBUTING.md 与 CODE_OF_CONDUCT.md

## 许可协议
- 本项目基于 MIT 协议开源，详见 LICENSE
