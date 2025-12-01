# AttendanceHub - Smart Attendance System

## 📋 Project Overview

**AttendanceHub** is an Android-based attendance management system that uses WiFi Direct technology to enable seamless, offline attendance tracking. The app consists of two variants: a **Teacher** app and a **Student** app that work together to manage classroom attendance without requiring internet connectivity.

## 🎯 Core Concept

Teachers create a WiFi hotspot from their device, and students connect to it. The teacher's device acts as a local server, automatically logging student attendance when they connect. The system uses QR codes for easy connection and provides real-time attendance tracking.

<img width="860" height="920" alt="Screenshot 2025-12-01 040320" src="https://github.com/user-attachments/assets/c35565f1-68a3-41ac-ac77-91a97fa2db86" />
<img width="843" height="915" alt="Screenshot 2025-12-01 040341" src="https://github.com/user-attachments/assets/24f55cef-a8e3-46eb-9846-ed082b73c3ba" />
<img width="870" height="931" alt="Screenshot 2025-12-01 040402" src="https://github.com/user-attachments/assets/9606eba5-06c5-41a9-98f2-a047f24d5cbb" />
<img width="834" height="918" alt="Screenshot 2025-12-01 040423" src="https://github.com/user-attachments/assets/41057eaf-70c5-43e4-9c34-44fadda8279b" />
<img width="862" height="923" alt="Screenshot 2025-12-01 040303" src="https://github.com/user-attachments/assets/9306c705-2e07-48f1-bc24-e2b1b5e99c42" />

---

## 📱 App Variants

### 1. **Teacher App** (Variant: `teacher`)
- Creates and manages WiFi hotspot
- Runs local attendance server
- Tracks connected students in real-time
- Generates QR codes for easy student connection
- Exports attendance lists (CSV format)
- Manages attendance sessions

### 2. **Student App** (Variant: `student`)
- Scans QR codes to connect to teacher's hotspot
- Automatically submits attendance upon connection
- Displays connection status
- Shows session information

---

## 🛠️ Technologies Used

### **Programming Languages**
- **Kotlin** - Primary language for Android development
- **Java** - Supporting libraries and Android SDK

### **Android Framework & Libraries**
- **Jetpack Compose** - Modern UI toolkit
- **Material Design 3** - UI components and theming
- **ViewModel** - UI state management
- **StateFlow** - Reactive state management
- **Coroutines** - Asynchronous programming
- **Room Database** - Local data persistence
- **FileProvider** - Secure file sharing

### **Networking & Communication**
- **Ktor Server** - Lightweight HTTP server for teacher app
- **Ktor Client** - HTTP client for student app
- **WiFi Direct API** - Android hotspot management
- **Network Callback API** - Network connectivity monitoring

### **Additional Technologies**
- **QR Code Generation** - ZXing library for QR code creation
- **QR Code Scanning** - CameraX + ML Kit for QR scanning
- **CSV Export** - Apache Commons CSV library
- **Gson** - JSON serialization/deserialization

### **Build Tools**
- **Gradle** (Kotlin DSL) - Build automation
- **Android Gradle Plugin** - Android-specific build configurations
- **Product Flavors** - Multi-variant build system

---

## 🎨 Features

### **Teacher Features**
✅ **Hotspot Management**
- Start/Stop WiFi hotspot with custom credentials
- Automatic hotspot configuration
- Real-time connection monitoring

✅ **Session Management**
- Create and manage attendance sessions
- Track session metadata (SSID, start/end time)
- Persistent session storage

✅ **QR Code Generation**
- Automatic QR code generation with session info
- Encrypted connection credentials
- Expiry timestamp for security

✅ **Real-Time Student Tracking**
- Live list of connected students
- Connection timestamps
- Student device information

✅ **Data Export**
- CSV export of attendance lists
- Share via Android's native share dialog
- Email, cloud storage, messaging integration

✅ **Attendance Server**
- Local HTTP server (port 8080)
- RESTful API for student check-in
- Real-time student data collection

### **Student Features**
✅ **QR Code Scanning**
- Camera-based QR code scanner
- Automatic WiFi connection
- Session validation

✅ **Automatic Check-In**
- Auto-submit attendance on connection
- Device information collection
- Timestamp recording

✅ **Connection Status**
- Real-time connection feedback
- Session information display
- Error handling and retry logic

---

## 🏗️ Architecture

### **Design Pattern**
- **MVVM (Model-View-ViewModel)** architecture
- Unidirectional data flow
- Separation of concerns

### **Project Structure**
attendancehub/ ├── app/ │ ├── src/ │ │ ├── main/ # Shared code │ │ ├── teacher/ # Teacher-specific code │ │ └── student/ # Student-specific code │ ├── build.gradle.kts # App-level build config │ └── libs.versions.toml # Dependency versions ├── gradle/ │ └── wrapper/ # Gradle wrapper ├── build.gradle.kts # Project-level build config └── settings.gradle.kts # Project settings
### **Key Components**

#### **Data Layer**
- `SessionRepository` - Manages session data and database operations
- `SessionEntity` - Room database entity for sessions
- `SessionDao` - Database access object

#### **Network Layer**
- `AttendanceServer` - Ktor server for teacher app
- `AttendanceServerImpl` - Server implementation
- `NetworkManager` - WiFi/hotspot management

#### **UI Layer**
- `TeacherViewModel` - Teacher app state management
- `StudentViewModel` - Student app state management
- Compose screens for both variants

#### **Domain Models**
- `ConnectedStudent` - Represents a connected student
- `SessionInfo` - Session metadata
- `AttendanceRecord` - Individual attendance record

---

## 📊 Data Flow

### **Teacher Side:**
1. Teacher starts hotspot → Creates session
2. Session generates QR code → Displays to students
3. Students connect → Server receives check-in requests
4. Server updates connected students list → UI updates in real-time
5. Teacher exports data → CSV file generated and shared

### **Student Side:**
1. Student scans QR code → Extracts session info
2. App connects to WiFi → Joins teacher's hotspot
3. App submits attendance → POST request to server
4. Receives confirmation → Displays success message

---

## 🔒 Security Features

- **Session Expiry** - QR codes expire after set time
- **WiFi Encryption** - WPA2 encryption for hotspot
- **Local-Only Server** - No internet connectivity required
- **FileProvider** - Secure file sharing with scoped storage
- **Permission Handling** - Runtime permission requests

---

## 📝 File Formats

### **CSV Export Format**
```csv
Session Name,Session 2025-12-01 08:30
Session ID,abc123-def456
SSID,AndroidShare_3912
Start Time,2025-12-01 08:30:00
End Time,2025-12-01 10:15:00
Total Students,25

No.,Student ID,Name,Device ID,Connected At,Timestamp
1,STU001,John Doe,ABC123DEV,08:40:54,1733040600000

{
  "ssid": "AndroidShare_3912",
  "password": "y9tm7mjfjm9heds",
  "serverIp": "192.168.49.1",
  "sessionId": "05d54f14-d681-401b-93b1-2b4e4eba8263",
  "expiryTimestamp": 1764578454446
}
✅ Connected Students List Not Displaying (2025-12-01)
Issue: Students connecting but not appearing in UI
Cause: Type mismatch in timestamp conversion
Fix: Added proper date formatting for connectedAt field
Open Issues:
Date constructor deprecation warning (non-critical)
<hr></hr>
🚀 Build Configuration
Build Variants
teacherDebug - Teacher app (debug)
teacherRelease - Teacher app (release)
studentDebug - Student app (debug)
studentRelease - Student app (release)
Minimum Requirements
Min SDK: 24 (Android 7.0)
Target SDK: 34 (Android 14)
Compile SDK: 35
Kotlin: 2.0.0
Build Commands
# Build teacher variant
./gradlew assembleTeacherDebug

# Build student variant
./gradlew assembleStudentDebug

# Build all variants
./gradlew assembleDebug

<hr></hr>
📦 Dependencies
Core Android
androidx.core:core-ktx
androidx.lifecycle:lifecycle-runtime-ktx
androidx.activity:activity-compose
Jetpack Compose
androidx.compose.ui:ui
androidx.compose.material3:material3
androidx.navigation:navigation-compose
Room Database
androidx.room:room-runtime
androidx.room:room-ktx
Ktor (Networking)
io.ktor:ktor-server-core
io.ktor:ktor-client-android
io.ktor:ktor-serialization-gson
QR Code & Camera
com.google.zxing:core (ZXing)
androidx.camera:camera-camera2 (CameraX)
com.google.mlkit:barcode-scanning (ML Kit)
CSV Export
org.apache.commons:commons-csv
<hr></hr>
