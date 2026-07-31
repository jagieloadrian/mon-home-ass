# Data Model: Search filter on statistics-meta

## Entities

### StatisticMetaData (existing table, reused — no schema change)

Source: `statistics_meta` table, mapped by `StatisticMetaData` object in
`src/main/kotlin/com/anjo/monhomeass/model/tables/Statistic.kt`. No new
fields; `search` only narrows which rows are returned. No new DTO —
existing `StatisticMetaDto` unchanged.

## Query shape

```
StatisticMetaData.selectAll()
  [where StatisticMetaData.statisticId.lowerCase() like "%${search.lowercase()}%"
     or  StatisticMetaData.name.lowerCase() like "%${search.lowercase()}%" ]
```

The `where` clause is applied only when `search` is present and non-blank
(per FR-003, absent/empty `search` returns every row, unchanged from
current behavior). Case-insensitivity via `.lowerCase()` on both the
column and the input, per spec Assumptions.
