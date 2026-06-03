# 🛺 Yatri-Mitra — How to Run

## Step 1: Start the Backend Server

The backend is a pure Node.js server — no `npm install` needed.

### Windows:
```
cd backend
start.bat
```

### Mac / Linux:
```
cd backend
./start.sh
```

Or manually:
```
cd backend
node server.js
```

You should see:
```
🛺  Yatri-Mitra Backend Running
================================
   URL  : http://localhost:3000
   Test : http://localhost:3000/api/health
```

Test it: open http://localhost:3000/api/health in your browser.

---

## Step 2: Configure IP Address

**For Emulator (default — works out of the box):**
`ApiClient.kt` already uses `10.0.2.2:3000` which maps to your PC's localhost.

**For Real Device:**
Edit `app/src/main/java/com/yatrimitra/app/network/ApiClient.kt`:
```kotlin
const val BASE_URL = "http://YOUR_PC_IP:3000"
```
Find your PC IP: run `ipconfig` (Windows) or `ifconfig` (Mac/Linux).
Your phone and PC must be on the same WiFi network.

---

## Step 3: Open in Android Studio

1. Unzip → open the `YatriMitra` folder in Android Studio
2. Wait for Gradle sync (first time ~2 min)
3. Run on emulator or device (API 24+)

---

## App Flow

```
Splash Screen (1.5s)
     ↓ (not logged in)          ↓ (already logged in)
Login Screen              Main App Screen
     ↓ success
Register Screen
     ↓ success
Main App Screen (with logout in top-right menu)
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/auth/register | Register new user |
| POST | /api/auth/login | Login, returns JWT token |
| GET | /api/auth/profile | Get user profile (needs Bearer token) |
| GET | /api/health | Server health check |
