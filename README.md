# AI Sarathi - Android Car Infotainment Launcher (Phase 1)

AI Sarathi turns your Android phone (Redmi 10 / MIUI 14) into a hands-free, voice-first car infotainment launcher powered by **Gemini Multimodal Live API**.

---

## Features (Phase 1)

1. **Native Car Launcher (Kiosk Mode)**
   - Default Home App support (`android.intent.category.HOME`).
   - Car-friendly tactile dashboard with large touch targets:
     - **Maps** (Google Maps Turn-by-Turn navigation)
     - **Spotify** (Deep music search & playback)
     - **YouTube Music** (Deep music search & playback)
     - **YouTube** (Video & search)
     - **Phone** (Direct call & dialer)
     - **WhatsApp**
     - **All Apps** (Full installed apps drawer)
     - **Settings** (API Key, music player choice, saved places, voice options)
   - Top status bar: Live 24-hr Clock, Mic State (Listening/Thinking/Speaking), Connection Status (SIM Data / Wi-Fi SSID), Battery % & Battery Temperature (°C), Active Conversation Mode Badge.
   - Connection Card: Hotspot / Wi-Fi indicator + 1-tap shortcut to Android Wi-Fi settings.

2. **Gemini Live Real-time Audio Engine**
   - Direct bidirectional WebSocket connection to `generativelanguage.googleapis.com` (`gemini-2.0-flash-exp`).
   - Audio Streaming: 16 kHz PCM microphone input & 24 kHz low-latency speaker playback.
   - Wake Word: "Sarathi" / "सारथी" activates hands-free listening on-device.
   - Auto-reconnect: Seamless reconnection on session duration limits.
   - Quota Fallback:
     - Spoken voice alert on rate-limits.
     - Option to handoff to official Gemini app if quota is exhausted.
     - Optional paid key backup (disabled by default in Advanced Settings).
   - Daily Usage Counter: Tracks minutes of active live audio per day.

3. **Three Conversation Modes**
   - **Command Mode:** Quick 1-2 sentence replies for navigation, music, and phone calls.
   - **Talk Mode:** Deep conversationalist & driving companion for long drives.
   - **Casual Chat Mode:** Friendly Hinglish banter, road-trip jokes, and trivia.
   - Switchable by voice (*"Sarathi, talk mode"*, *"baat karte hain"*, *"gup-shup mode"*) or by tapping the mode badge on screen.

4. **Hands-free Media & Phone Control**
   - Deep links for Spotify & YouTube Music.
   - Notification Listener Service (`CarNotificationListener`): verifies if music actually starts playing; if not, prompts: *"Should I search it in the other app?"*
   - Phone calls: Spoken countdown confirmation (*"Calling Ramesh..."*) before dialing.

---

## How to Build the APK (Automated GitHub Actions)

Since you do not have Android Studio installed, we have set up an automated **GitHub Actions Workflow** (`.github/workflows/build-apk.yml`).

### Step 1: Push Code to your GitHub
Run these commands in your Mac terminal (or let Antigravity run them with your approval):
```bash
git add .
git commit -m "Initial commit: AI Sarathi Phase 1"
```

Create a new repository named `aisarathi` on your GitHub account (`drkrishnkantpandey-gif`), then push:
```bash
git remote add origin https://github.com/drkrishnkantpandey-gif/aisarathi.git
git branch -M main
git push -u origin main
```

### Step 2: Download Ready-to-Install APK
1. As soon as you push, go to your GitHub repository in your browser: `https://github.com/drkrishnkantpandey-gif/aisarathi/actions`
2. You will see **Build AI Sarathi APK** running.
3. In ~2 minutes, the build will complete with a green checkmark!
4. Under the **Releases** tab (or **Artifacts**), download `AI-Sarathi-Debug-APK` directly to your Redmi 10.

---

## Car Setup on Redmi 10 (MIUI 14)

1. **Install APK:**
   - On your Redmi 10, open the downloaded APK and tap **Install** (Allow "Install from unknown sources" if MIUI asks).
2. **Grant Permissions:**
   - Open AI Sarathi. It will ask for Microphone, Phone, Contacts, and Location permissions. Tap **Allow**.
   - In **Settings > Music Detection Permission**, tap **Grant** and turn on Notification Access for *Sarathi Media Detection* (this lets Sarathi verify if Spotify/YT Music songs actually play).
3. **Enter Gemini API Key:**
   - Tap **Settings** on the home screen.
   - Tap **Paste** to paste your free Gemini API key from Google AI Studio.
4. **Set as Default Launcher:**
   - Go to Redmi 10 **Settings > Apps > Manage Apps > 3 dots (top right) > Default apps > Launcher**.
   - Select **AI Sarathi**.
5. **Ready to Drive!**
   - Mount phone on dashboard charger, connect Bluetooth to car stereo, and say:
     - *"Sarathi, take me home"*
     - *"Sarathi, play Kishore Kumar songs on Spotify"*
     - *"Sarathi, let's talk"* / *"Baat karte hain"*
     - *"Sarathi, find petrol pump nearby"*
