# Data Model: Statistic Entities List

No schema changes — this feature only reads an already-modeled table
(`StatisticMetaData` in `src/main/kotlin/com/anjo/monhomeass/model/tables/Statistic.kt`,
backing the recorder DB's `statistics_meta` table).

## Entity: Statistic Metadata

One row = one trackable statistic known to the Home Assistant recorder.

| Field (DTO) | Source column | Type | Nullable | Notes |
|---|---|---|---|---|
| id | `data_id` | Int | no | Internal recorder identifier (PK) |
| statisticId | `statistic_id` | String | yes | External/business identifier, e.g. `sensor.living_room_temperature` |
| name | `name` | String | yes | Display name |
| unitOfMeasurement | `unit_of_measurement` | String | yes | e.g. `°C`, `kWh` |
| source | `source` | String | yes | Recorder-internal source label |
| hasMean | `has_mean` | Boolean | yes | Whether mean aggregation applies |
| hasSum | `has_sum` | Boolean | yes | Whether sum aggregation applies |

**Identity & uniqueness**: `statistic_id` is unique per the recorder DB's own
`ix_statistics_meta_statistic_id` index (see `notatki.txt`) — not
re-validated by this app, since the DB is the readonly source of truth.

**Lifecycle**: No state transitions — rows are managed entirely by Home
Assistant's recorder; this feature never writes to them (FR-004).

**Relationships**: `statistics`/`statistics_short_term` reference this
table's `id` as `metadata_id` (used by later roadmap phases, not this one).
