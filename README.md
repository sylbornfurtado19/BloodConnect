<div align="center">

# 🩸 Blood Donation Network

### *Because "I need blood NOW" shouldn't depend on a lucky WhatsApp forward.*

An Android app that turns blood donation from a panic-driven scramble into a **real-time, intelligent, life-saving network.**

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](#)
[![Backend](https://img.shields.io/badge/Backend-Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](#)
[![Status](https://img.shields.io/badge/Status-In%20Development-orange?style=for-the-badge)](#)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](#)
[![Lives](https://img.shields.io/badge/Impact-1%20donation%20%3D%203%20lives-crimson?style=for-the-badge)](#)

</div>

---

## ⚡ The Problem

> Every **2 seconds**, someone needs blood. Yet finding the *right* donor, at the *right* time, in the *right* place — still runs on phone trees and hope.

- 🩸 Donor discovery is slow and manual
- 📱 Requests scatter across WhatsApp/Instagram, causing duplication & panic
- 😮‍💨 First-time donors rarely become *repeat* donors — no visibility, no incentive
- 🏥 Hospitals have no verified, real-time channel to broadcast urgent needs

**We built the fix.**

---

## 🚀 What This App Actually Does

<table>
<tr>
<td width="50%" valign="top">

### 🔍 Smart Matching
Not just "nearest donor." A **composite score** — distance + reliability + response history + donor tier — surfaces the *best* match, not just the closest one.

### 🆘 SOS Critical Alert Mode
Life-threatening shortage? This isn't a polite ping. It's a high-priority alert engineered to cut through — even past silent mode.

### 🤖 AI Eligibility Pre-Screen
A conversational flow filters out ineligible donors *before* dispatch — saving precious minutes hospitals don't have.

</td>
<td width="50%" valign="top">

### 🏆 Donor Tiers & Streaks
Bronze → Silver → Gold → **Platinum**. Donate, level up, get recognized. Gamification that actually retains donors.

### 🪪 Digital Donor Passport
A QR-verifiable, shareable card of your donation history. Flex it, scan it, trust it.

### 📊 "Lives Saved" Counter
Every donation ≈ up to 3 lives. We show you the number. It hits different.

</td>
</tr>
</table>

---

## 🧠 How It Works

```
   DONOR                    MATCHING ENGINE                RECIPIENT / HOSPITAL
┌───────────┐         ┌──────────────────────┐         ┌───────────────────┐
│ Register  │────────▶│  Smart Ranking Algo   │◀────────│  Raise Request     │
│ Blood Grp │         │  (distance+reliability│         │  (urgency-tagged)  │
│ Location  │         │   +response history)  │         │                    │
│ Available?│         └──────────┬───────────┘         └─────────┬──────────┘
└───────────┘                    │                                │
                                  ▼                                ▼
                         🔔 FCM Push / SOS Alert  ──────▶  Live ETA Tracking
                                  │
                                  ▼
                     ✅ Accept → Donate → Passport Updated → Tier Progress
```

---

## 🛠️ Tech Stack

| Layer | Tech |
|---|---|
| **Frontend** | Android (Kotlin) / Flutter |
| **Backend** | Firebase — Auth, Firestore, Cloud Functions |
| **Notifications** | Firebase Cloud Messaging (FCM) |
| **Location** | Google Maps SDK, FusedLocationProvider |
| **AI Screening** | Dialogflow / rule-based conversational engine |
| **Verification** | QR-based Donor Passport scan-to-verify |

---

## ✨ Feature Roadmap

- [x] Donor registration + blood group & location capture
- [x] Location-radius, blood-group-based search
- [x] Emergency request broadcasting via FCM
- [x] Smart donor ranking engine
- [ ] SOS Critical Alert Mode
- [ ] Digital Donor Passport (QR)
- [ ] Donor tiers, streaks & "Lives Saved" counter
- [ ] Hospital verification dashboard
- [ ] Blood drive / camp event organizer
- [ ] WhatsApp bot bridge for low-connectivity access

---

## 📲 Getting Started

```bash
git clone https://github.com/<your-username>/blood-donation-network.git
cd blood-donation-network
# open in Android Studio, sync Gradle, hit run ▶️
```

You'll need a Firebase project with Authentication, Firestore, and Cloud Messaging enabled — drop your `google-services.json` in `app/`.

---

## 👥 Team

| Roll No. | Name |
|---|---|
| N024 | Ranish Devadiga |
| N025 | Yug Dhaigude |
| N027 | Sylborn Furtado |
| N042 | Hriday Jain |

*MBA Tech, Computer Engineering — MPSTME, NMIMS*

---

<div align="center">

### 🩸 One tap can save three lives. Let's make finding the tap effortless.

**⭐ Star this repo if you believe blood donation deserves better tech.**

</div>
