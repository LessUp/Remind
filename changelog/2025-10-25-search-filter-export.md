# 2025-10-25 列表搜索与筛选、CSV 导出

- 列表搜索与筛选：
  - 物品列表 ItemsScreen：新增搜索框与状态筛选 Chip（全部/临期/已过期/无保质期/正常）。
  - 会员列表 SubsScreen：新增搜索框与状态筛选 Chip（全部/临期/已过期/正常）。
  - 过滤逻辑在界面层执行，基于 ViewModel 提供的状态与名称关键字。
- CSV 导出：
  - SettingsViewModel 新增 `buildItemsCsv()` 与 `buildSubsCsv()`，生成 UTF-8 CSV 文本。
  - SettingsScreen 新增“导出物品 CSV / 导出会员 CSV”按钮，使用 SAF CreateDocument 写出文件。
  - Gradle 依赖：引入 `androidx.activity:activity-compose` 以支持 ActivityResult API。
- 其他：
  - 清理 Gradle 依赖重复声明（Compose BOM 与 activity-compose）。
