# 📚 studyBUddy: Find Your Study Match

studyBUddy is a mobile app built with Kotlin and Jetpack Compose that helps **Boston University students** find compatible study partners based on their **courses, availability, and study habits**.

Instead of searching through Discord servers or Reddit posts, students can **swipe** through curated profiles of peers who share similar study goals. When two students mutually match, the app securely **reveals their BU email or phone number**, allowing them to connect and coordinate study sessions.

---

## 🎯 App Concept and Primary Use Case
The primary goal of studyBUddy is to connect students in **large or lecture-heavy courses** (such as computer science, data science, or economics) who want to:
- Form small study groups
- Review together before exams
- Stay accountable through regular check-ins

By combining familiar social mechanics (swiping) with an academic focus, studyBUddy turns collaboration into a simple, fun, and secure experience.

---

## 👥 Target Users and Problem Being Solved
**Target Users:**  
Current BU undergraduate students who want to study collaboratively.

**Problem:**  
Studying alone is hard, and BU’s large campus makes it challenging to find reliable study partners. Students need a quick, safe way to connect with others who share similar study goals, schedules, and classes.

studyBUddy helps users:
- Build academic accountability
- Share resources and motivation
- Collaborate effectively across courses

---

## ⚙️ Planned Features

### **MVP**
- **Authentication:** Login restricted to verified BU emails  
- **Profile Management:** Create and edit a study profile (major, year, courses, availability)  
- **Swipe Matching:** Swipe left/right to skip or like potential study partners  
- **Matched List:** View and contact matched users  
- **Match Reveal:** Once two users match, BU email or phone number is revealed
- **Google Calendar:** Schedule and plan study sessions directly through the app
- **UI/UX Design:** Built with Material 3 theming and accessibility support

### **Stretch Goals**
- **Dark Mode:** Optional dark theme for improved user comfort
- **In-App Chat System:** Direct messaging between matched users
- **Streak Tracking System:** Encourage regular study communication
- **Group Matching:** Match 3-4 students from the same course
- **Google Maps API:** Suggest nearby study locations (e.g., Mugar Library, Questrom Cafe)  

---

## 🌐 External APIs and Onboard Sensors

**External APIs**
- **Google Calendar API:** For planning and scheduling study sessions  
- **Firebase Authentication:** For secure user login and account management  

**Onboard Sensors**
- **Camera:** For uploading and updating profile photos  

---

## 🧭 Rough Navigation Map

| Screen | Function |
|--------|-----------|
| **Login / Signup** | BU email verification |
| **Profile Setup** | Input major, year, courses, and availability |
| **Home (Discover)** | Swipe through study partner cards |
| **Matches** | View and contact matched users |
| **Profile** | Edit personal info, toggle light/dark mode, logout |

---

## 🧑‍💻 Team Roles and Responsibilities

| **Name** | **Role** | **Responsibilities** |
|-----------|-----------|----------------------|
| **Jerry Teixeira** | Data & Authentication | Firebase setup, Google Calendar API, authentication logic |
| **Eugene Jung** | UI/UX & Frontend | Compose layouts, swipe interface, Material 3 theming, API integration |
| **Aaron Huang** | Backend & Integration | Database setup, API connections, app logic, testing |

---

## 🧩 Tech Stack
- **Language:** Kotlin  
- **Framework:** Jetpack Compose  
- **Backend:** Firebase Firestore  
- **APIs:** Firebase Auth, Google Calendar 
- **UI:** Material 3 with dark/light mode support  


