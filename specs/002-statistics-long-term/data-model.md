# Data Model: Long-term statistics for one entity

## Entities

### Statistic (existing table, reused — no schema change)

Source: `statistics` table, mapped by `Statistic` object in
`src/main/kotlin/com/anjo/monhomeass/model/tables/Statistic.kt`.

| Field (Kotlin) | Column | Type | Notes |
|---|---|---|---|
| id | id | Int | row id, not exposed in DTO |
| metadataId | metadata_id | Int? | FK → `statistics_meta.id`; join key to resolve `statisticId` |
| startTs | start_ts | Instant? | period start — filter target for `from`/`to` |
| mean | mean | Double? | |
| min | min | Double? | |
| max | max | Double? | |
| sum | sum | Double? | |
| state | state | Double? | |

### StatisticMetaData (existing table, reused — no schema change)

Already modeled and exposed via Phase 1 (`/statistics-meta`). Used here only
to resolve `statisticId` (path param, a string like `sensor.foo`) to the
`metadata_id` used to filter `Statistic` rows, and to detect "unknown
statisticId" (→ 404).

## New DTO

### StatisticDto

```kotlin
@Serializable
data class StatisticDto(
    val startTs: Long,   // epoch seconds, period start
    val mean: Double?,
    val min: Double?,
    val max: Double?,
    val sum: Double?,
    val state: Double?
)
```

No validation rules beyond nullability already present in the DB schema
(FR-006: readonly, no writes — this DTO is read/serialize only). No state
transitions — statistic rows are immutable historical records.

## Query shape

```
Statistic
  .innerJoin(StatisticMetaData, { Statistic.metadataId }, { StatisticMetaData.id })
  .selectAll()
  .where { StatisticMetaData.statisticId eq statisticId }
  .andWhere from/to bounds on Statistic.startTs, when present
  .orderBy(Statistic.startTs, ASC)
```

`statisticId` existence check (for 404) is a prior lookup on
`StatisticMetaData` by `statisticId`, reusing the same table Phase 1 already
queries — no new table access pattern introduced.
