
# FFinder – Project Description  
*Share locations in real time, find friends instantly.*

---

## 1. Vision

FFinder aims to be a **privacy-first, real-time location-sharing platform** that feels as smooth and polished as a flagship social app.  
Key pillars:

* **Instant value** – real-time map with live friend pins, no fiddly setup.  
* **Granular privacy** – sharing toggles, visibility scopes, upcoming audit.  
* **High accessibility** – WCAG 2.1 AA, 48 dp targets, TalkBack coverage.  
* **Premium look & feel** – coherent brand palette, micro-animations, performance at 60 FPS.  

---

## 2. Technical Stack

| Layer | Technology |
|------|-------------|
| **UI** | Jetpack Compose (Material 3) + Compose Anim. |
| **Architecture** | **Clean Architecture + MVVM** – UI → ViewModel → UseCases → Repositories → DataSources |
| **DI** | Hilt 2.52 (KSP) |
| **Realtime** | Firebase Firestore (offline cache on), Firebase Auth (wired but UI TBD) |
| **Maps** | google-maps-compose 6.2.1 |
| **Location** | play-services-location 21.3.0 |
| **Testing** | JUnit 5, MockK, Compose UITest (light smoke tests) |
| **Build** | Gradle 8.x, Android API 34, Debug/Release variants |

---

## 3. Repository & Package Layout


android/app
└── src/main/java/com/locationsharing/app
├── ui/                 ← Compose screens & components
│   ├── home/
│   ├── map/
│   ├── friends/
│   ├── invite/
│   └── settings/       (placeholder)
├── domain/             ← (minimal) business rules / use-cases
├── data/
│   ├── location/
│   ├── friends/
│   └── contacts/
├── di/                 ← Hilt modules
└── navigation/         ← NavHost + NavigationManager

```

*Single-module* for now; future plan is feature-modules (`:feature-map`, `:feature-friends`, …) for faster build and isolation.

---

## 4. Key Features

### 4.1 Home Screen
* **Hero Section** – animated `logo_full.png` zooms every 4 s.  
* **Map Preview Card** – mini-map placeholder before permission.  
* **CTA** – “Start Live Sharing” Extended FAB.  
* **Secondary Row** – Friends / Settings.  
* **What’s New Card** – teaser with 🏳️‍🌈 slide-in.

### 4.2 Map Screen
* **Google Map** full-screen, custom brand styling.  
* **Quick Share FAB** – green + `ic_pin_finder`; toggles share.  
* **Self-Location FAB** – centers camera on user.  
* **Nearby Friends Drawer** – 280 dp side panel, search + list.  
* **ShareStatusSheet** – Modal bottom sheet “Active/Off” + Lat/Lng.  
* **Debug FAB** (debug builds) – purple flask adds mock friends.  
* **Micro-Animations** – pin bounce, FAB scale, overshoot drawer.

### 4.3 Friends List
* Real-time online/offline list, staggered animations, pull-to-refresh.

### 4.4 Invite Flow
* Phone-contact import, hashed discovery via Firestore, dual lists:
  * “On FFinder” → **Add Friend**  
  * “Invite to FFinder” → **SMS share**

### 4.5 Settings (Roadmap)
* Privacy, notifications, location accuracy, account.

---

## 5. Brand & Assets

| Asset | Usage |
|-------|-------|
| `logo_full.png` | Launcher foreground, Home Hero |
| `ic_pin_finder.png` | Map pin, Quick-Share FAB |
| `GPS_Animation.json` | ShareStatusSheet while acquiring GPS |
| `Travel_Somewhere.json` | Lottie backdrop on Home Hero |
| `Location_animation.json` | Empty-state in Nearby Friends drawer |

> **Notes**:  
> • JSON files currently reside in `res/drawable/`; Lottie prefers `res/raw/`.  
> • Vectorized duplicates (`*_vector.xml`) exist for DPI independence.  

---

## 6. Known Gaps / Current Issues

* **Auth UI missing** – Firebase Auth wired but no login/register screens.  
* **Quick-Share toggle half-implemented** – presently toggles state but no intent share.  
* **Nearby Friends debug-only** – real matching by distance pending.  
* **Green AppBar** regression – needs transparent AppBar per spec.  
* **FAB overlaps on small DPIs** – require speed-dial or re-layout.  
* **Lottie placement** – JSONs found, must be moved to `res/raw/` and loaded.  
* **Unit tests sparse** – rely on manual QA for now to save tokens.  

---

## 7. Roadmap (Q3-Q4 2025)

1. **Quick-Fix Pass** – restore all broken buttons & flows.  
2. **Visual Polish Pass** – implement MapScreen redesign spec.  
3. **Auth Sprint** – login, register, profile editing.  
4. **Settings Sprint** – privacy toggles, notifications, accuracy.  
5. **Feature Modularization** – split into dynamic features.  
6. **Offline & Caching** – Room/Datastore layers.  
7. **CI/CD** – GitHub Actions, Firebase Test Lab matrix.  

---

## 8. Contribution Guidelines

* **Branch naming**: `feat/<feature>`, `fix/<bug>`, `refactor/<area>`.  
* **Commit style**: `type(scope): message` (e.g., `feat(map): pin bounce anim`).  
* **Pull Requests**: small, focused; include before/after screenshot or GIF.  
* **Testing**: at least one smoke UI test per screen; deeper tests optional.  

---

## 9. Credits

* **Project Lead** – *Nati Levy
* **Design & Brand** – `logo_full.png`, `ic_pin_finder.png` by *Nati Levy
* **Animations** – Lottie by *Nati Levy
* **Open-source** – Built with Jetpack Compose, Firebase, Maps SDK.  

> _“Find friends, share moments, stay private—FFinder shows where you are, only when you want.”_

---
```
