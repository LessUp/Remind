# 架构说明

## 概览
- 层次：UI（Compose）→ ViewModel（状态与业务）→ Repository（数据）→ Room（本地数据库）
- 配置：DataStore 保存“临期阈值”和“每日提醒时间”
- 通知与调度：WorkManager + Hilt-Work 统一管理每日概览与单项提醒

## 目录结构
- app/src/main/java/app/lessup/remind
  - data
    - db：`AppDatabase`、`ItemDao`、`SubscriptionDao`、`Entity`
    - repo：`ItemRepository`、`SubscriptionRepository`
    - settings：`SettingsRepository`
  - di：Hilt 模块（`DatabaseModule`、`WorkModule` 等）
  - reminder：通知渠道、`ReminderScheduler`、各 `Worker`
  - ui：
    - items：列表与编辑页
    - subs：列表与编辑页
    - settings：设置页、导出
    - stats：统计页
    - navigation：`NavRoutes`、`AppRoot`
  - MainActivity：请求通知权限，注入并安排每日概览

## 关键数据结构
- `ItemEntity`
  - `purchasedAt: LocalDate`
  - `shelfLifeDays: Int?`
  - `expiryAt: LocalDate?`（无则按 `purchasedAt + shelfLifeDays` 推算）
- `SubscriptionEntity`
  - `priceCents: Long`（单位分，CNY）
  - `endAt: LocalDate`
  - `autoRenew: Boolean`

## 业务规则
- 临期阈值：默认 7 天；在 Items/Subscriptions ViewModel 中用于计算状态
- 提醒策略：
  - 每日概览：在设置时间触发，聚合当天到期项与临期
  - 单项提醒：到期前 7/3/1 天、当天、过期后 1 天
- 删除条目：同时取消对应定时任务

## 通知与 WorkManager
- `ReminderScheduler` 负责：
  - scheduleDailyOverview()
  - scheduleForItem()/cancelForItem()
  - scheduleForSub()/cancelForSub()
  - rebuildAll()（配合 `BootReceiver`）
- `NotificationHelper` 建立三类通知渠道（物品/会员/每日概览）

## 扩展点
- 导入 CSV：与现有导出对称
- 本地/云备份：基于文件或 `BackupAgent`
- 多语言：strings.xml 国际化
- UI 主题：深色模式与品牌色
