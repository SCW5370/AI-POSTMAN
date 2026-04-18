# API Summary

## User

- `POST /api/users`

## Preference

- `POST /api/preferences/{userId}`
- `GET /api/preferences/{userId}`

## Source

- `POST /api/sources`
- `GET /api/sources`
- `PATCH /api/sources/{id}/enable`

## Digest

- `GET /api/digests/{userId}`
- `GET /api/digests/detail/{digestId}`

## Feedback

- `POST /api/feedback`
- `GET /api/feedback/click`

## Admin

- `POST /api/admin/fetch`
- `POST /api/admin/fetch-async`
- `GET /api/admin/fetch-async/{taskId}`
- `POST /api/admin/digests/build`
- `POST /api/admin/digests/build-async`
- `GET /api/admin/digests/build-async/{taskId}`
- `POST /api/admin/digests/prebuild-now`
- `POST /api/admin/digests/finalize-pending`
- `POST /api/admin/digests/finalize/{digestId}`
- `POST /api/admin/digests/send/{digestId}`

`POST /api/admin/digests/build` / `build-async` 请求体支持可选字段：

- `forceLlm: true|false`：为 `true` 时强制 LLM 参与（已有 draft 也会做一次强制精修）
