# StyleMate Weather Push Notifications

Minimal setup notes for daily weather push notifications (Backend + Android).

## Backend setup

1) Add environment variables (example):
```
WEATHER_API_KEY=...
WEATHER_DEFAULT_LAT=21.0285
WEATHER_DEFAULT_LON=105.8542
FIREBASE_SERVICE_ACCOUNT_PATH=./stylemate-backend/config/firebase-service-account.json
```

2) Install dependencies & run:
```
cd stylemate-backend
npm install
npm run dev
```

3) Test push payload (no FCM needed):
```
npm run push:preview
```

4) Trigger push manually:
```
npm run push:weather
npm run push:weather -- --userId <userId>
```

## Android setup

1) Add Firebase project and download `google-services.json`.
2) Place it at `app/google-services.json`.
3) Run the app and login to register FCM token.

## Payload contract

Backend sends:
```
notification: { title, body }
data: { type: "weather", weatherCode, temp }
```

Android behavior:
- Foreground: show in-app snackbar.
- Background: show system notification.

## Location behavior
- App sends `latitude`/`longitude` with FCM token when location is resolved.
- Cron groups devices by rounded coordinates and sends weather by location.
- If no location is available, it falls back to `WEATHER_DEFAULT_LAT/LON`.
