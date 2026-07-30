# Data Model: Statistics date-range summary

## Entities

### Statistic / StatisticMetaData (existing tables, reused — no schema change)

Source: `statistics` and `statistics_meta` tables, mapped by `Statistic` and
`StatisticMetaData` objects in
`src/main/kotlin/com/anjo/monhomeass/model/tables/Statistic.kt`. Joined the
same way `StatisticRepo.getLongTerm` already does (`Statistic.metadataId eq
StatisticMetaData.id`, filtered by `StatisticMetaData.statisticId`).

## New DTO

### StatisticSummaryDto

Matches the pre-existing `openapi.yaml` stub for this feature.

```kotlin
@Serializable
data class StatisticSummaryDto(
    val statisticId: String,
    val from: String?,
    val to: String?,
    val min: Double?,
    val max: Double?,
    val avg: Double?,
    val sum: Double?
)
```

`from`/`to` echo the request's parsed range as ISO-8601 instant strings
(`Instant.toString()`), `null` when that bound was omitted from the
request — no new serializer needed. `min`/`max`/`avg`/`sum` are `null`
together when the `statisticId` is known but no rows fall inside
`[from, to]` (per Clarifications — matches SQL `MIN`/`MAX`/`AVG`/`SUM` over
zero rows, which is `NULL` for each).

No validation rules beyond nullability. No state transitions — a derived,
non-persisted aggregate computed per request.

## Query shape

```
Statistic
  join StatisticMetaData on Statistic.metadataId = StatisticMetaData.id
  where StatisticMetaData.statisticId = :statisticId
    and Statistic.startTs is not null
    [and Statistic.startTs >= :from]
    [and Statistic.startTs <= :to]
  select MIN(Statistic.state), MAX(Statistic.state), AVG(Statistic.state), SUM(Statistic.state)
```

Existence check for `statisticId` (404 vs empty-aggregate) reuses the exact
`StatisticMetaData.select(id).where { statisticId eq :id }.limit(1).any()`
pattern already used by `getLongTerm`/`getShortTerm`, run before the
aggregate query.

`from`/`to` parsing reuses the existing `parseInstant` helper in
`StatisticsRoutes.kt` — no new format support.
