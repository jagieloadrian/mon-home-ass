# Contract: GET /statistics-meta

Full OpenAPI definition lives in the repo-root `openapi.yaml` (single source
of truth — not duplicated here per project convention):

- Path: `/statistics-meta` in `../../../openapi.yaml`
- Request: no parameters (Phase 1 scope — `?search=` is Phase 6, trigger-gated, not in this contract yet)
- Response `200`: `application/json`, array of `StatisticMetaDto`
- Response `503`: `application/json`, `ErrorDto` — recorder DB unreachable or query failed (FR-006)

`StatisticMetaDto` fields: see [data-model.md](../data-model.md).
