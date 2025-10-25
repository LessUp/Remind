# 2025-10-24 Android 项目骨架与最简 MVP 提交

- 新建 Android 工程骨架（Kotlin + Compose + Material3 + Hilt + Room + Navigation）。
- 配置 Gradle：AGP 8.4.2、Kotlin 1.9.24、Compose BOM、Hilt/Room/kotlinx-datetime 依赖。
- 应用结构：
  - Application：`LessupRemindApp`（Hilt）。
  - 主界面：`MainActivity` + `AppRoot`（底部导航：物品、会员）。
  - 主题与资源：`Theme.LessupRemind`、基础配色、启动图标。
- 数据层：
  - Room 实体与 Dao：`ItemEntity`/`ItemDao`、`SubscriptionEntity`/`SubscriptionDao`。
  - 转换器：`Converters` 支持 `LocalDate`/`Instant`。
  - 数据库：`AppDatabase`，索引默认按到期排。
  - 仓库：`ItemRepository`、`SubscriptionRepository`。
  - DI：`DatabaseModule` 提供数据库、Dao、Repo。
- 界面层（Compose）：
  - 物品：`ItemsScreen`（列表+FAB新建）、`ItemEditScreen`（新增/编辑）。
  - 会员：`SubsScreen`（列表+FAB新建）、`SubEditScreen`（新增/编辑）。
  - 计算逻辑：
    - 物品：已购天数、剩余天数、状态（无保质期/临期/过期/正常）。
    - 会员：剩余天数、临期提醒标签；价格仅支持 CNY。
- 导航：`NavRoutes` + 可选 `id` 参数（支持新建/编辑）。
- 兼容性：minSdk 24、targetSdk 35、JDK 17。

后续计划：
- 统计与设置页面（M2）。
- 数据导出/备份（M3）。
