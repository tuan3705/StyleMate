# Stylemate Backend

Minimal docs for local development and auth API.

## Env
Create `.env` based on `.env.example`.

Required for auth:
- `JWT_ACCESS_SECRET`
- `JWT_REFRESH_SECRET`

## Run
```powershell
npm install
npm run dev
```

## Auth API
Base path: `/api/auth`

### POST `/register`
Body:
```json
{
  "email": "user@example.com",
  "password": "123456"
}
```

### POST `/login`
Body:
```json
{
  "email": "user@example.com",
  "password": "123456"
}
```
Note: login KHÔNG tự tạo tài khoản. Nếu chưa có, dùng `/register`.
Response:
- `accessToken`, `refreshToken`, `user`, `isNewUser`

### POST `/refresh`
Body:
```json
{
  "refreshToken": "<token>"
}
```

### POST `/logout`
Header:
- `Authorization: Bearer <accessToken>`

## Smoke Test
```powershell
npm run test:auth
```
