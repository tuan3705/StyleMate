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

## AI Auto-Fill (Lykdat)
Base path: `/api/images`

### POST `/ai-fill`
Form-data:
- `image`: file ảnh

Response:
```json
{
  "success": true,
  "data": {
    "category": "Tops",
    "categoryConfidence": 0.96,
    "categorySource": "labels",
    "color": "silver",
    "colorConfidence": 0.44,
    "name": "set-in sleeve",
    "nameConfidence": 0.56,
    "candidates": {
      "categories": []
    }
  }
}
```

## Smoke Test
```powershell
npm run test:auth
```

## Weather Push Notifications

### Env
Add the following to `.env`:
```
WEATHER_API_KEY=...
WEATHER_DEFAULT_LAT=21.0285
WEATHER_DEFAULT_LON=105.8542
FIREBASE_SERVICE_ACCOUNT_PATH=./config/firebase-service-account.json
```

Optional alternative (avoid file path):
```
FIREBASE_SERVICE_ACCOUNT_JSON={...}
```

### Cron schedule
- Runs daily at **07:00** (Asia/Ho_Chi_Minh).
- Implemented by `node-cron` in `cron/scheduler.js`.

### Manual trigger (simulate cron)
```
npm run push:preview
npm run push:weather
npm run push:weather -- --userId <userId>
```

### Payload format
```
notification: { title, body }
data: { type: "weather", weatherCode, temp }
```

### Notes
- FCM tokens are read from `UserDevice` (all users or by `userId`).
- If no tokens found, job returns `NO_TOKENS`.
- Optional: send `latitude`/`longitude` when saving FCM token to enable per-device weather.
