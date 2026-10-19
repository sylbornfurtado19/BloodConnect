# 🩸 BloodConnect — Features & Roadmap Report

> **Project Status:** Core Architecture, Firebase Backend, Google Sign-In, and Google Maps API fully integrated! ✅

---

## 🚀 Implemented Features

### 1. Modern Architecture & Setup
- **MVVM & UDF**: Clean separation of concerns with reactive state flows.
- **Dependency Injection**: Dagger Hilt for seamless object provisioning (`@HiltAndroidApp`, `@HiltViewModel`).
- **Declarative UI**: Built entirely with Jetpack Compose and Material 3 design systems.
- **Type-Safe Navigation**: Navigation Compose managing all app screens and route parameters.

### 2. Branded Android 12+ Splash Screen
- Utilizes the official Android 12+ SplashScreen API (`androidx.core:core-splashscreen`).
- Features the custom branded `blood_drop_logo.png` icon on a deep crimson background (`#9A0007`).
- Smooth 350ms fade-out transition into the authentication flow.

### 3. Comprehensive Firebase Backend
- **Authentication**:
  - Email/Password login and registration.
  - **Google Sign-In**: Fully integrated with `GoogleSignInClient`, Web Client ID (`default_web_client_id`), and debug SHA-1 verification.
- **Cloud Firestore**:
  - `users` collection for volunteer donor profiles, availability, and tier tracking.
  - `blood_requests` collection for broadcasting and managing emergency blood requests.

### 4. Interactive Google Maps Integration
- Configured with Google Maps API key (`AIzaSyAjA0dOTqz6xx8V4aVt-He1VewH3JmmpMo`) and package restrictions.
- `DonorMapScreen` powered by `maps-compose`, displaying interactive maps, zoom controls, and custom blood center markers.

### 5. Rich UI Feature Suite
- **Home Dashboard**: Quick overview, emergency actions, and navigation hub.
- **Donor Search & Detail**: Filter donors by blood group and locality.
- **Blood Requests**: View and create urgent blood requests.
- **User Profile**: Track lives saved, donor tier (Bronze/Silver/Gold/Platinum), and account management.

---

## 📋 What Is Still Needed (Roadmap & Next Steps)

1. **Firebase Cloud Messaging (FCM) Push Notifications**:
   - Implement `FirebaseMessagingService` to receive and display high-priority emergency broadcast notifications for critical blood shortages.
2. **GPS Proximity Calculation & Sorting**:
   - Use `FusedLocationProviderClient` to calculate exact distance (in km) between the user and nearby donors/hospitals using the Haversine formula.
3. **Offline Persistence & Caching**:
   - Enable Firestore offline data caching so users can view emergency requests even with intermittent network connectivity.
4. **Comprehensive Unit & UI Testing**:
   - Add automated test coverage for ViewModels, Repositories, and Compose UI components.
