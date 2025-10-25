# Lessup Remind 产品设计方案 v0.1

日期：2025-10-24

## 1. 产品愿景
- 用最少操作成本记录“物品购买/会员订阅”，优雅可靠地提供到期与已购时长提醒。

## 2. 目标用户与场景
- 经常采购/囤货，关注食物/日用品保质期的人群。
- 拥有多项付费会员（流媒体、云服务等），需要管理价格、到期与续费的人群。

## 3. 核心功能
- 物品记录：名称、购买日期（默认今天，可修改）、保质期（按天数或到期日期二选一）、备注。
- 会员记录：名称、平台、购买日期、价格与币种、到期日期/周期、自动续费、备注。
- 计算与展示：距到期剩余天数；若未设保质期，显示已购买天数。
- 提醒：临期阈值可配置、每日概览、到期与逾期提醒。
- 列表：状态优先排序（临期/逾期置顶）、搜索与筛选（后续可加标签/类别）。

## 4. 信息架构与导航
- 底部导航：
  - 物品
  - 会员
  - 统计
  - 设置
- 顶部：搜索与筛选入口。
- 全局 FAB：新建物品/新建会员（根据当前 Tab）。

## 5. 交互与视觉（Material 3）
- 配色：优先使用 Android 动态色，品牌感=高级、雅致；暗色模式适配。
- 字体：系统默认（Roboto/Noto）；字号层级 14/16/20sp。
- 组件：卡片列表、状态 Chip（临期/已过期）、分段控制（“按天数/按日期”）。
- 间距：8/12/16dp；圆角 12dp；微动效（进入、列表项状态变化、FAB 反馈）。
- 空状态：简洁插画与引导文案。

## 6. 可用性与无障碍
- 对比度 >= 4.5。
- TalkBack 可读：为关键图标与 Chip 提供 contentDescription。
- 触控目标 >= 48dp；表单容错校验清晰。

## 7. 数据模型（Room）
### ItemEntity（物品）
- id: Long（主键）
- name: String
- purchasedAt: LocalDate
- shelfLifeDays: Int?（可空）
- expiryAt: LocalDate?（可空）
- notes: String?（可空）
- createdAt: Instant
- updatedAt: Instant
- 索引：expiryAt

### SubscriptionEntity（会员）
- id: Long（主键）
- name: String
- provider: String?（可空）
- purchasedAt: LocalDate
- priceCents: Long
- currency: String（ISO-4217）
- endAt: LocalDate
- autoRenew: Boolean
- notes: String?（可空）
- createdAt: Instant
- updatedAt: Instant
- 索引：endAt

## 8. 计算与状态
- daysSincePurchase = today - purchasedAt。
- expiryAt 计算：若 shelfLifeDays != null，则 expiryAt = purchasedAt + shelfLifeDays；否则使用手动输入的 expiryAt。
- daysToExpire = expiryAt - today。
- 物品状态：
  - 已过期：daysToExpire < 0
  - 临期：0 <= daysToExpire <= threshold
  - 正常：daysToExpire > threshold 或无保质期
- 会员剩余天数 = endAt - today；状态同上（以 endAt 计算）。

## 9. 提醒与通知策略（WorkManager）
- 每日概览：09:00 发送，包含“临期/到期/逾期”的汇总与 Top3 列表。
- 单项提醒：提前 7、3、1 天（阈值可在设置中配置）；到期当日提醒；逾期第 1 天补提醒。
- 编辑/删除条目时：重建或取消对应 OneTime 任务；概览为 Periodic 任务（Unique）。
- 通知分组：按“物品/会员”频道分组，支持静默/高优先级配置。

## 10. 技术架构
- Kotlin + Jetpack Compose + Material 3。
- 架构：MVVM + Repository，单 Activity 多 Destination（Navigation Compose）。
- 依赖：Hilt（依赖注入）、Room（本地库）、WorkManager（任务/提醒）、DataStore（设置持久化）、kotlinx-datetime。
- 本地化：简体中文优先，结构预留多语言。

## 11. 设置项
- 提醒时间（默认 09:00）。
- 临期阈值（默认 7 天；可选 3/7/14）。
- 默认币种（如 CNY）。
- 列表排序（临期优先/按购买时间/按到期时间）。
- 导入/导出（CSV/JSON，后续）。

## 12. 隐私与数据
- 数据仅存本地；不上传服务器。
- 未来可选：本地备份/云盘导出（需用户显式操作）。

## 13. 路线图与验收
- M1（≈2 周）：物品/会员 CRUD、到期/已购天数计算、每日概览、临期提醒、设置阈值与时间。
- M2（≈1 周）：统计页、搜索/筛选。
- M3（≈1 周）：导出与基础备份。
- 验收：
  - 物品：输入名称+购买日期+保质期后，正确显示剩余天数；未设保质期显示已购天数。
  - 会员：显示剩余天数与价格；按到期排序。
  - 通知：在设定时间收到概览与临期提醒，成功率 ≥ 95%。
