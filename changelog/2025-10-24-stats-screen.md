# 2025-10-24 统计页与导航接入

- 新增统计页：
  - `StatsViewModel`：基于 Room + DataStore 阈值计算近阈值天数内的临期与已过期数量。
  - `StatsScreen`：显示物品/会员的临期与已过期数量，并展示当前阈值。
- 导航与底部栏：
  - 在 `AppRoot` 中加入 `Stats` 目的地与底部 Tab（图标：AutoGraph）。
- 说明：当前统计按 DataStore 的临期阈值动态变更。
