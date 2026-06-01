# StyleMate Android Auth

Minimal notes for the login flow integration.

## Base URL
Update `local.properties` in project root:
```
STYLEMATE_BASE_URL=http://10.0.2.2:3000/
```

## Login flow
- App starts in `AppNavigation()`.
- If token exists in DataStore, it opens `MainScreen`.
- Otherwise it shows `LoginScreen`.

## Logout
Use the logout icon in the top bar of `MainScreen`.

