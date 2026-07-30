# Data Model: Short-term (5-minute) statistics for one entity

## Entities

### StatisticShortTem (existing table object — needs fixing, no schema change)

Source: `statistics_short_term` table, mapped by `StatisticShortTem` object
in `src/main/kotlin/com/anjo/monhomeass/model/tables/Statistic.kt`. Fields
identical to `Statistic` (Phase 2's long-term table):

| Field (Kotlin) | Column | Type | Notes |
|---|---|---|---|
| id | id | Int | row id, not exposed in DTO |
| metadataId | metadata_id | Int? | FK → `statistics_meta.id` (**currently wrong** — points at `states_meta.metadata_id`, must fix per Phase 2's precedent) |
| startTs | start_ts | Instant? | period start — filter target for `from`/`to` |
| mean | mean | Double? | |
| min | min | Double? | |
| max | max | Double? | |
| sum | sum | Double? | |
| state | state | Double? | |

Two fixes required before this table is usable (see plan.md "Pre-existing
bugs" section):
1. `metadataId` must reference `StatisticMetaData.id`, not `StateMetadata.id`.
2. `Table()` must take the explicit name `"statistics_short_term"` — no
   constructor arg currently means Exposed derives the name from the class
   name, which does not match the real table.

### StatisticMetaData (existing, unchanged)

Same role as in Phase 2: resolves `statisticId` (path param) to
`metadata_id`, and detects "unknown statisticId" (→ 404).

## Reused DTO

### StatisticDto (existing, from Phase 2 — no changes)

```kotlin
@Serializable
data class StatisticDto(
    val startTs: Long,
    val mean: Double?,
    val min: Double?,
    val max: Double?,
    val sum: Double?,
    val state: Double?
)
```

Roadmap is explicit: short-term rows have the identical shape to long-term
rows, so this phase adds zero new DTOs — only a new repo method returning
the same `List<StatisticDto>?`.

## Query shape

```
StatisticShortTem
  .innerJoin(StatisticMetaData, additionalConstraint = { StatisticShortTem.metadataId eq StatisticMetaData.id })
  .selectAll()
  .where { StatisticMetaData.statisticId eq statisticId }
  .andWhere { StatisticShortTem.startTs.isNotNull() }
  .andWhere from/to bounds, when present
  .orderBy(StatisticShortTem.startTs, ASC)
```

Identical shape to Phase 2's `getLongTerm` query, just against
`StatisticShortTem` instead of `Statistic`. `statisticId` existence check
(404) reuses the same `StatisticMetaData` lookup Phase 2 already added.
