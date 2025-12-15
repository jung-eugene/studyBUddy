# 📚 studyBUddy: Find Your Study Match

**studyBUddy** is an Android app built with **Kotlin** and **Jetpack Compose** that helps **Boston University students** find compatible study partners using their **courses, availability, and study preferences**.

Instead of browsing Discord or Reddit, students can swipe through curated profiles of peers with similar academic goals. When two students mutually match, studyBUddy securely **reveals their BU email**, enabling them to coordinate study sessions directly.

---

## 🎯 App Concept
studyBUddy aims to connect students in **large or lecture-heavy classes** such as CS, DS, and Economics. Students can use the app to:
- Form small study groups
- Prepare for exams together
- Stay accountable through regular check-ins

By combining familiar swipe mechanics with academic matching, studyBUddy makes collaboration simple, engaging, and secure.

---

## 👥 Target Users & Problem
**Target Users:**  
BU undergraduate/graduate students seeking collaborative study partners

**Problem:**  
BU is a large campus, and students often struggle to find reliable peers who share:
- The same courses
- Compatible study habits
- Overlapping schedules

studyBUddy addresses this by providing:
- Fast academic matching
- Built-in accountability
- A safe, BU-verified community
- Tools to schedule and plan study sessions

---
## 💫 Features

### 1. Home Screen
<img width="240" alt="home" src="https://github.com/user-attachments/assets/8a626e8c-3d08-4d70-a046-1f8f8670022c" />

* Users swipe through curated profiles of students with similar study goals
* Swipe left to skip, right to like
* Each profile card displays:
   * Name, major, year
   * Current courses
   * Study preferences
   * Availability
 
### 2. Matches Screen
<img width="240" alt="matches" src="https://github.com/user-attachments/assets/9ae27de1-cd66-44d8-94dc-b458db3a7873" />
<img width="240" alt="matches-card" src="https://github.com/user-attachments/assets/0bed20b2-b53a-4386-a898-10a1a04b60cf" />
<img width="240" alt="matches-invite" src="https://github.com/user-attachments/assets/d9744dfc-6a9f-4107-aff8-b527574b1f41" />

* View all current matches and past matches
* Expand a match to view their full profile card
* Schedule a Google Calendar event
* Email is revealed only after a mutual match
* Users can “unmatch” (archive) past matches

### 3. Calendar Screen
<img width="240" alt="calender-signin" src="https://github.com/user-attachments/assets/bbe17181-21ee-4515-b0fc-a24ccdb95e87" />
<img width="240" alt="calendar" src="https://github.com/user-attachments/assets/6305fa53-382e-465c-9a66-3db822e5b47a" />
<img width="240" alt="calendar-event" src="https://github.com/user-attachments/assets/4beaa3a6-7871-4b2c-bd41-97a6ee657642" />

* Users sign in with their Google account
* Displays all study sessions that have been:
   * sent to them
   * accepted
   * added to Google Calendar
* Note: Event creation happens in the Matches screen, not here

### 4. Profile Screen
<img width="240" alt="profile" src="https://github.com/user-attachments/assets/6f58b6c2-81a7-4fe0-90b9-75ee5af0d24a" />
<img width="240" alt="profile-edit" src="https://github.com/user-attachments/assets/4f0de39f-d378-481f-a947-51174c91eb13" />
<img width="240" alt="profile-darkmode" src="https://github.com/user-attachments/assets/7c625b01-8b50-4ddf-886a-a58eded851b3" />

* Displays user information (major, year, bio, courses, availability)
* Users can edit profile details
* Upload or change profile photo
* Toggle light/dark mode
* Track streaks
* Log out

---
## 🗂️ Branch Overview

| **Feature**                              | **Branch**                      | **Status**     |
|------------------------------------------|---------------------------------|----------------|
| Google Calendar event creation           | `feature/oauth-and-event`       | Complete       |
| Core UI layout + navigation              | `feature/onboarding-profile`    | Complete       |
| Home screen with swipe deck animation    | `feature/home-screen`           | Complete       |
| Profile picture upload                   | `feature/camera-sensor`         | Complete       |
| Core backend (NavGraph/ViewModel)        | `feature/onboarding-v2`         | Complete       |
| Dark mode support                        | `feature/dark-mode`             | Complete       |
| Vertification email screen               | `feature/vertification-optional`| Complete       |
| Filter algorithm for Home screen         | `feature/filtering`             | Complete       | 

---
## 🧩 App Architecture
<img width="700" alt="architecture" src="https://github.com/user-attachments/assets/516b4381-a580-4a0a-b8a2-31d272447d6d" />

* The app uses a layered MVVM architecture built with Jetpack Compose, StateFlow, Firebase, and the Google Calendar API.
* The UI Layer contains all composable screens (Login, Setup, Home, Matches, Calendar, Profile) and sends user actions to ViewModels.
* The ViewModel Layer holds screen state, handles business logic (auth, profile, matches, swipe behavior, calendar events), and exposes reactive StateFlows back to the UI.
* The Data/Services Layer contains repositories and integrations — Firebase Auth for credentials, Firestore for user/match data, MatchRepository for pairing logic, and the Google Calendar API for scheduling study sessions.
* State updates flow upward from data → ViewModels → UI, ensuring clean separation and consistent automatic recomposition.

---
## 🛠️ Run Instructions

**Prerequisites:**
Your Google account must be added as a test user in the project’s Google Cloud Platform (GCP) console to allow OAuth and Calendar integration.

**Steps:**

1. **Clone the Repository**
   ```bash
   git clone https://github.com/jung-eugene/studyBUddy.git
   cd studyBUddy
   ```

2. **Fetch All Branches**
   ```bash
   git fetch --all
   ```

3. **List Remote Branches**
   ```bash
   git branch -r
   ```

4. **Checkout a Feature Branch**
   ```bash
   git checkout <branch-name>
   ```

5. **Open the project in Android Studio**
   - Make sure you have the latest version of Android Studio installed.
   - Open the project folder and let Gradle sync.

6. **Run the App**
   - Connect an Android device or use an emulator.
   - Click **Run** in Android Studio.

---

## ⚙️ Feature Roadmap

### **MVP Features**
- **Authentication:** Login restricted to verified BU emails  
- **Profile Management:** Create and edit a study profile (major, year, courses, availability)  
- **Swipe Matching:** Swipe left/right to skip or like potential study partners  
- **Matched List:** View and contact matched users  
- **Match Reveal:** Once two users match, BU email is revealed
- **Google Calendar:** Schedule and plan study sessions directly through the app
- **UI/UX Design:** Built with Material 3 theming and accessibility support

### **Stretch Features**
- **Dark Mode:** Optional dark theme for improved user comfort
- **In-App Chat System:** Direct messaging between matched users
- **Streak Tracking System:** Encourage regular study communication
- **Group Matching:** Match 3-4 students from the same course
- **Google Maps API:** Suggest nearby study locations (e.g., Mugar Library, Questrom Cafe)  

---
## 🌐 APIs & Sensors

**External APIs**
- **Google Calendar API:** Create study session events 
- **Firebase Authentication:** Secure BU-email login
- **Firestore**: User profiles, matches, availability, preferences

**Onboard Sensors**
- **Camera:** Uploading profile photos  

---

## 🧭 Navigation Flow

| Screen | Function |
|--------|-----------|
| **Login / Signup** | BU email verification + account creation|
| **Profile Setup** | Add major, year, courses, and availability |
| **Home** | Swipe through recommended study partners |
| **Matches** | View matched users + contact info |
| **Calendar** | Schedule and plan study sessions with partners |
| **Profile** | Edit personal info, toggle light/dark mode, logout |

---

## 🧑‍💻 Team Roles and Responsibilities

| **Name** | **Role** | **Responsibilities** |
|-----------|-----------|----------------------|
| **Jerry Teixeira** | Data & Authentication | Firebase setup, Google Calendar API, authentication logic |
| **Eugene Jung** | UI/UX & Frontend | Compose layouts, swipe interface, Material 3 theming, API integration |
| **Aaron Huang** | Backend & Integration | Database setup, API connections, filtering, app logic, testing |

---

## 🧪 Testing Strategy

### **1. Console Log Tracing**

* Added detailed `Log.d()` and `Log.e()` statements for:
  * **Firestore reads/writes** (profile updates, course lists, dark mode toggle)
  * **Google Calendar API** requests and OAuth responses
* Helped verify asynchronous operations and identify incorrect document paths or null states.

### **2. Invalid Input & Error Handling Tests**

* Tested incorrect login attempts, missing fields, and broken network cases.
* Verified Snackbar feedback for user-facing errors.
* Added `try/catch` blocks in ViewModels

### **3. Manual Device & Emulator Testing**

* UI layout testing on multiple emulator sizes + physical devices.
* Verified:
  * Navigation state restoration
  * Orientation changes
  * Swipe gestures
  * Profile editing UX
  * Camera/gallery image upload flow
* Confirmed no crashes during onboarding, swiping, or data persistence.

### **4. Feature-Specific Testing**

* **Camera Sensor:** Verified gallery picker + camera intent on supported devices, fallback behavior on emulators.
* **Google Calendar:** Ensured test accounts correctly received event creation requests.
* **Firestore Sync:** Confirmed real-time updates to user profile fields and matches.

---

## 🤖 LLM Usage Disclosure

Throughout development, we used Large Language Models (LLMs) such as ChatGPT and Gemini to support implementation and debugging, but not to auto-generate full features.

**LLM assistance included:**

* Troubleshooting Android Studio / Gradle build errors
* Debugging Jetpack Compose UI layout issues
* Suggesting improvements for ViewModel + NavGraph architecture
* Helping rewrite or optimize Kotlin snippets we already wrote
* Providing explanations for Firebase, Google Calendar API, and OAuth behavior
* Reviewing code for edge cases and error-handling gaps
* Improving documentation and README clarity

All final implementations were reviewed, tested, and refined manually by the development team to ensure correctness, security, and maintainability.
