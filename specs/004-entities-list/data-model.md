# Data Model: Entities list

## Entities

### StateMetadata (existing table, reused — no schema change)

Source: `states_meta` table, mapped by `StateMetadata` object in
`src/main/kotlin/com/anjo/monhomeass/model/tables/State.kt`.

| Field (Kotlin) | Column | Type | Notes |
|---|---|---|---|
| id | metadata_id | Int | primary key |
| entityId | entity_id | String? | nullable per schema (`notatki.txt`) |

## New DTO

### EntityDto

```kotlin
@Serializable
data class EntityDto(
    val id: Int,
    val entityId: String?
)
```

No validation rules beyond nullability already present in the DB schema.
No state transitions — entity metadata rows are stable reference data.

## Query shape

```
StateMetadata.selectAll().map { it.toEntityDto() }
```

Direct table scan, no join, no filter — identical shape to Phase 1's
`StatisticMetaRepo.getAll()`.
