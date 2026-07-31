# Data Model: Pagination on long/short-term statistics

## Entities

### Statistic / StatisticShortTem / StatisticMetaData (existing tables, reused — no schema change)

No new fields, no new DTO — `StatisticDto` unchanged. `limit`/`offset` only
narrow which rows of the existing join/filter/order query are returned.

## Query shape

```
Statistic (or StatisticShortTem)
  join StatisticMetaData on ...metadataId = StatisticMetaData.id
  where StatisticMetaData.statisticId = :statisticId
    and startTs is not null
    [and startTs >= :from]
    [and startTs <= :to]
  order by startTs asc
  [limit :limit]
  [offset :offset]
```

`limit`/`offset` are applied via Exposed's `Query.limit(count: Int)` /
`Query.offset(start: Long)`, appended after `.orderBy(...)`, so Postgres
performs the slicing — no in-memory `drop`/`take` in Kotlin (per FR-007).

Negative or non-numeric `limit`/`offset` values are rejected with 400
before the query runs (same validation shape as the existing `from`/`to`
parsing in `StatisticsRoutes.kt`).
