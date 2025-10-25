# 2025-10-24 设置模块与提醒模块实现

- 设置模块（DataStore）：
  - `SettingsRepository`（阈值与提醒时间：默认 7 天、09:00）。
  - `SettingsModule` 提供 DataStore；`SettingsViewModel` 与 `SettingsScreen`（阈值 3/7/14、时间选择）。
  - 列表状态与阈值联动：`ItemsViewModel`、`SubscriptionsViewModel` 使用动态阈值计算。
- 提醒模块（WorkManager + Hilt-Work）：
  - 依赖：`work-runtime-ktx`、`androidx.hilt:hilt-work`。
  - 通知：`NotificationHelper` 创建频道（物品/会员/每日概览）。
  - 调度：`ReminderScheduler`（每日概览、物品/会员单项 7/3/1/0/-1 天），支持重建与取消。
  - Worker：`OverviewWorker`、`ItemReminderWorker`、`SubReminderWorker`。
  - 启动与开机：`LessupRemindApp` 配置 `HiltWorkerFactory` 并初始化通知频道；`BootReceiver`（开机重建）。
  - DI：`WorkModule` 提供 WorkManager。
- 保存/删除联动：
  - 物品：`ItemEditViewModel` 保存后安排提醒；`ItemsViewModel` 删除后取消。
  - 会员：`SubEditViewModel` 保存后安排提醒；`SubscriptionsViewModel` 删除后取消。
- Manifest：
  - 权限：`POST_NOTIFICATIONS`、`RECEIVE_BOOT_COMPLETED`。
  - 注册：`BootReceiver`。

注意：Android 13+ 需要在运行时授予“通知权限”才会显示通知（当前最简版尚未弹出权限请求）。
