# 🩸 BloodConnect — Final Project & Architecture Report

> **Project Name:** BloodConnect (Blood Donation & Emergency Management App)  
> **Platform:** Android (Kotlin & Jetpack Compose)  
> **Backend:** Firebase (Authentication, Cloud Firestore, Cloud Messaging)  
> **Status:** Fully Functional & Tested ✅  

---

## 📋 Executive Summary
BloodConnect is a state-of-the-art Android application engineered to transform blood donation and emergency medical requests from a panic-driven scramble into a real-time, intelligent, and reliable life-saving network. 

The project has been fully structured, architected, implemented, and verified with **zero build errors** and **all unit tests passing successfully**.

---

## 🏗️ Architectural Overview & Tech Stack

### Architecture Pattern
- **MVVM (Model-View-ViewModel)** with Unidirectional Data Flow (UDF).
- **Dependency Injection** powered by **Dagger Hilt** (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`).
- **Declarative UI** built with **Jetpack Compose** and **Material 3** design systems.
- **Navigation** managed via **Jetpack Navigation Compose** (`NavHost`, `NavGraph`, type-safe route arguments).

### Core Dependencies & Libraries
| Category | Libraries / Tools |
|---|---|
| **UI & Theming** | Jetpack Compose BOM, Material 3, Material Icons Extended |
| **Navigation** | Navigation Compose, Hilt Navigation Compose |
| **Lifecycle & Coroutines** | Lifecycle ViewModel Compose, Kotlinx Coroutines, Play Services Coroutines |
| **Backend & Database** | Firebase BoM, Firebase Auth (Email/Password & Google Sign-In), Cloud Firestore, Firebase Messaging |
| **Location & Permissions** | Google Play Services Location (`play-services-location`), Accompanist Permissions |
| **Splash Screen** | Android 12+ Core SplashScreen API (`androidx.core:core-splashscreen`) |

---

## 🛠️ Feature Implementation & Backend Integration

### 1. Branded Android 12+ Splash Screen
- Configured `Theme.BloodConnect.Splash` extending `Theme.SplashScreen`.
- Features the custom branded animated logo (`blood_drop_logo.png`) with deep crimson background (`#9A0007`).
- Implemented a smooth 350ms fade-out exit animation in `MainActivity`.

### 2. Authentication System (Email & Google Sign-In)
- **Email/Password Authentication**: Secure sign-in and registration via `AuthRepository` and `AuthViewModel`.
- **Google Sign-In**: Integrated `GoogleSignInClient` with Web Client ID (`R.string.default_web_client_id`) and debug SHA-1 verification (`8d9b4d477d6377f55b18d91562271688e35d4d5f`).
- **Firestore Profile Sync**: Automatically provisions and stores user profiles in the Firestore `users` collection upon first sign-in.

### 3. Cloud Firestore Data Models & Repositories
- **`User.kt`**: Manages donor profiles (`uid`, `email`, `fullName`, `bloodGroup`, `phone`, `latitude`, `longitude`, `isAvailable`, `livesSaved`, `donorTier`).
- **`BloodRequest.kt`**: Manages emergency blood requests (`requestId`, `requesterId`, `requesterName`, `bloodGroup`, `hospitalName`, `unitsRequired`, `urgency`, `status`, `timestamp`).
- **Repositories**: `AuthRepository`, `UserRepository`, `BloodRequestRepository`, and `DonorRepositoryImpl` fully bound via Hilt modules.

---

## ✅ Testing & Verification Report

- **Build Verification:** `./gradlew clean assembleDebug` executed successfully with **zero errors**.
- **Unit Testing:** All **31 unit tests** passed successfully (`testDebugUnitTest`).
- **Version Control:** All code changes committed and pushed to remote GitHub repository (`origin/main`).

---

## 👥 Team & Acknowledgments
* **Developers:** Ranish Devadiga, Yug Dhaigude, Sylborn Furtado, Hriday Jain  
* **Institution:** MBA Tech, Computer Engineering — MPSTME, NMIMS  
