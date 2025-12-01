# AttendanceHub - Smart Attendance Management System

<div align="center">

**Modern Android Application for Classroom Attendance Management**

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Language](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-28-orange.svg)](https://developer.android.com/about/versions/pie)

</div>

---

## 📖 Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
- [Architecture](#-architecture)
- [Technologies Used](#-technologies-used)
- [Project Structure](#-project-structure)
- [How It Works](#-how-it-works)
- [Build Variants](#-build-variants)
- [Installation & Setup](#-installation--setup)
- [Usage Guide](#-usage-guide)
- [Data Export](#-data-export)
- [Security & Privacy](#-security--privacy)
- [Known Issues](#-known-issues)
- [Development](#-development)
- [Documentation](#-documentation)

---

## 🎯 Overview

**AttendanceHub** is a modern, offline-first Android application designed to streamline classroom attendance management. Built with Jetpack Compose and following Material Design 3 guidelines, the app provides a seamless experience for both teachers and students.

### The Problem It Solves

Traditional attendance systems often require:
- Internet connectivity
- Manual roll calls (time-consuming)
- Paper-based records (prone to loss)
- Complex hardware setups

### Our Solution

AttendanceHub uses **WiFi Direct** technology to create a local network where:
- ✅ **No internet required** - Works completely offline
- ✅ **Automatic check-in** - Students connect, attendance is logged instantly
- ✅ **Real-time tracking** - Teachers see students as they connect
- ✅ **Digital records** - Export to CSV for permanent storage
- ✅ **QR code convenience** - Scan to connect automatically

---

## ✨ Key Features

### For Teachers 👨‍🏫

#### 1. **Hotspot Management**
- Create WiFi hotspot with one tap
- Automatic credential generation
- Custom SSID with random suffix for security
- Real-time connection monitoring
- Easy start/stop controls

#### 2. **Session Management**
- Create and track attendance sessions
- Session metadata (start time, duration, SSID)
- Persistent storage of all sessions
- Historical session viewing
- Session statistics

#### 3. **QR Code Generation**
- Automatic QR code creation
- Embedded session information
- Encrypted WiFi credentials
- Expiry timestamp for security
- Large, scannable display

#### 4. **Real-Time Student Tracking**
- Live list of connected students
- Student details (name, ID, device info)
- Connection timestamps (HH:mm:ss format)
- Student count display
- Visual indicators (avatars, status badges)

#### 5. **Data Export & Sharing**
- Export attendance to CSV format
- Share via email, messaging, cloud storage
- Session metadata included
- Formatted, readable output
- Android native share integration

#### 6. **Local HTTP Server**
- Runs on port 8080
- RESTful API endpoints
- Handles student check-ins
- Real-time data synchronization
- Lightweight implementation

### For Students 👩‍🎓

#### 1. **QR Code Scanning**
- Camera-based QR scanner
- ML Kit barcode detection
- Automatic WiFi connection
- Session validation
- Error handling

#### 2. **Automatic Check-In**
- Connect to teacher's hotspot
- Auto-submit attendance
- Device information collection
- Timestamp recording
- Confirmation feedback

#### 3. **Manual Entry (Fallback)**
- Enter session details manually
- Input SSID, password, server IP
- Validate and connect
- Useful when QR scanning fails
- User-friendly forms

#### 4. **Connection Status**
- Real-time connection feedback
- Session information display
- Error messages
- Retry mechanisms
- Success confirmation

---

## 🏗️ Architecture

### Design Pattern: MVVM (Model-View-ViewModel)

```
┌─────────────────────────────────────────────┐
│                    View                      │
│         (Jetpack Compose UI)                 │
│  - HotspotActiveScreen                       │
│  - StudentScannerScreen                      │
│  - SessionHistoryScreen                      │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│               ViewModel                      │
│  - TeacherViewModel                          │
│  - StudentViewModel                          │
│  - State Management (StateFlow)              │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│           Repository Layer                   │
│  - SessionRepository                         │
│  - NetworkManager                            │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│             Data Sources                     │
│  - Local Storage (Files)                     │
│  - Network (HTTP Server)                     │
│  - WiFi Direct API                           │
└─────────────────────────────────────────────┘
```

### Key Architectural Principles

- ✅ **Unidirectional Data Flow** - Data flows one way through the app
- ✅ **Separation of Concerns** - Each layer has a single responsibility
- ✅ **Reactive Programming** - StateFlow for reactive state management
- ✅ **Dependency Injection** - Manual DI with constructor injection
- ✅ **Immutable State** - UI state is immutable and predictable

---

## 🛠️ Technologies Used

### **Core Technologies**

| Technology | Purpose | Version |
|------------|---------|---------|
| **Kotlin** | Primary programming language | 2.0.21 |
| **Jetpack Compose** | Modern declarative UI toolkit | 2024.09.00 |
| **Material Design 3** | UI components and theming | Latest |
| **Coroutines** | Asynchronous programming | Latest |
| **StateFlow** | Reactive state management | Latest |

### **Android Jetpack Components**

- **ViewModel** - UI state management with lifecycle awareness
- **Navigation Compose** - Type-safe navigation between screens
- **Lifecycle** - Lifecycle-aware components
- **Activity Compose** - Compose integration with activities
- **CameraX** - Camera functionality for QR scanning

### **Networking & Communication**

- **ServerSocket** - Custom HTTP server implementation
- **WiFi Direct API** - Android hotspot management
- **Network Callback API** - Network connectivity monitoring
- **JSON Serialization** - kotlinx-serialization for data exchange

### **QR Code & Scanning**

- **ZXing** - QR code generation (Teacher app)
  - `com.journeyapps:zxing-android-embedded:4.3.0`
- **ML Kit** - Barcode scanning (Student app)
  - `com.google.mlkit:barcode-scanning:17.3.0`
- **Google Code Scanner** - Alternative QR scanner
  - `com.google.android.gms:play-services-code-scanner:16.1.0`

### **Data Storage & Export**

- **File System** - Session storage in app files directory
- **FileProvider** - Secure file sharing
- **CSV Export** - Manual CSV generation for attendance lists

### **Build & Development Tools**

- **Gradle (Kotlin DSL)** - Build automation
- **Android Gradle Plugin** - 8.13.1
- **Product Flavors** - Multi-variant build system (teacher/student)
- **BuildConfig** - Build-time configuration

---

## 📁 Project Structure

### Module Organization

```
AttendanceHub/
│
├── 📦 app/                          # Main application module
│   ├── src/
│   │   ├── main/                    # Shared code
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/org/wahid/attendancehub/
│   │   │   │   └── MainActivity.kt
│   │   │   └── res/                 # Shared resources
│   │   │
│   │   ├── teacher/                 # Teacher variant
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/org/wahid/attendancehub/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── HotspotActiveScreen.kt
│   │   │   │   │   │   └── SessionHistoryScreen.kt
│   │   │   │   │   └── viewmodel/
│   │   │   │   │       └── TeacherViewModel.kt
│   │   │   │   ├── net/
│   │   │   │   │   ├── AttendanceServer.kt
│   │   │   │   │   └── TeacherHotspotManager.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── SessionRepository.kt
│   │   │   │   │   └── AttendanceSession.kt
│   │   │   │   └── navigation/
│   │   │   │       └── TeacherNavHost.kt
│   │   │   └── res/
│   │   │       ├── values/strings.xml
│   │   │       └── xml/file_paths.xml
│   │   │
│   │   └── student/                 # Student variant
│   │       ├── AndroidManifest.xml
│   │       ├── java/org/wahid/attendancehub/
│   │       │   ├── ui/
│   │       │   │   ├── screens/
│   │       │   │   │   ├── ScannerScreen.kt
│   │       │   │   │   └── ManualEntryScreen.kt
│   │       │   │   └── viewmodel/
│   │       │   │       └── StudentViewModel.kt
│   │       │   ├── net/
│   │       │   │   └── AttendanceClient.kt
│   │       │   └── navigation/
│   │       │       └── StudentNavHost.kt
│   │       └── res/
│   │           └── values/strings.xml
│   │
│   └── build.gradle.kts             # App build configuration
│
├── 📦 core/                         # Shared interfaces module
│   ├── src/main/java/com/attendancehub/
│   │   ├── net/
│   │   │   ├── HotspotManager.kt    # Interface
│   │   │   └── HotspotInfo.kt       # Data class
│   │   └── models/
│   │       └── StudentAttendance.kt
│   └── build.gradle.kts
│
├── 📦 student/                      # Student feature module
│   ├── src/main/java/com/attendancehub/net/
│   │   └── StudentHotspotManager.kt # Implementation
│   └── build.gradle.kts
│
├── 📄 build.gradle.kts              # Root build file
├── 📄 settings.gradle.kts           # Project settings
├── 📄 gradle.properties             # Gradle properties
│
└── 📚 Documentation/
    ├── PROJECT_README.md            # This file
    ├── ARCHITECTURE.md
    ├── MODULARIZATION_SUMMARY.md
    ├── NAVIGATION_COMPLETE.md
    ├── HTTP_SERVER_COMPLETE.md
    ├── QR_AND_THEME_COMPLETE.md
    ├── STUDENT_QR_SCANNING_COMPLETE.md
    ├── DOWNLOAD_STUDENT_LIST_COMPLETE.md
    ├── CONNECTED_STUDENTS_LIST_BUG_FIX.md
    └── ... (other documentation files)
```

### Key Directories Explained

| Directory | Purpose |
|-----------|---------|
| `app/src/main/` | Code and resources shared by both variants |
| `app/src/teacher/` | Teacher-specific implementation |
| `app/src/student/` | Student-specific implementation |
| `core/` | Shared interfaces and models |
| `student/` | Standalone student module |

---

## 🔄 How It Works

### Teacher Workflow

```
1. Teacher opens app
   ↓
2. Tap "Start Hotspot"
   ↓
3. System creates WiFi hotspot
   ↓
4. QR code generated with:
   - SSID
   - Password
   - Server IP (192.168.49.1)
   - Session ID
   - Expiry timestamp
   ↓
5. HTTP server starts on port 8080
   ↓
6. Display QR code to students
   ↓
7. Students connect → Server receives check-in
   ↓
8. Real-time UI update with student list
   ↓
9. Teacher taps "Download" to export CSV
   ↓
10. Share attendance via email/cloud
    ↓
11. Tap "End Session" to stop
```

### Student Workflow

```
1. Student opens app
   ↓
2. Camera opens for QR scanning
   ↓
3. Scan teacher's QR code
   ↓
4. App parses session info
   ↓
5. Connect to WiFi hotspot
   ↓
6. Send POST request to server:
   POST http://192.168.49.1:8080/join
   {
     "studentId": "STU123",
     "name": "John Doe",
     "deviceId": "Samsung-ABC123"
   }
   ↓
7. Server responds with success
   ↓
8. Display confirmation to student
```

### Communication Protocol

#### **Teacher → Student (QR Code)**

```json
{
  "ssid": "AndroidShare_3912",
  "password": "y9tm7mjfjm9heds",
  "serverIp": "192.168.49.1",
  "sessionId": "uuid-here",
  "expiryTimestamp": 1764578454446
}
```

#### **Student → Teacher (HTTP POST)**

```json
POST /join HTTP/1.1
Host: 192.168.49.1:8080
Content-Type: application/json

{
  "studentId": "STU123",
  "name": "John Doe",
  "deviceId": "Samsung-ABC123",
  "timestamp": 1733040600000
}
```

#### **Server Response**

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "status": "success",
  "message": "Attendance recorded",
  "studentName": "John Doe"
}
```

---

## 📱 Build Variants

### Product Flavors

The app uses Gradle product flavors to create two distinct applications:

#### **Teacher Variant**

```kotlin
applicationId: "com.attendancehub.teacher"
Features:
  - Hotspot management
  - QR code generation
  - HTTP server
  - Student list display
  - CSV export
```

#### **Student Variant**

```kotlin
applicationId: "com.attendancehub.student"
Features:
  - QR code scanning
  - WiFi connection
  - Attendance submission
  - Status display
```

### Build Types

- **Debug**: Development builds with logging enabled
- **Release**: Production builds with ProGuard (currently disabled)

### Available Build Configurations

| Configuration | Description |
|---------------|-------------|
| `teacherDebug` | Teacher app for testing |
| `teacherRelease` | Teacher app for production |
| `studentDebug` | Student app for testing |
| `studentRelease` | Student app for production |

---

## 🚀 Installation & Setup

### Prerequisites

- **Android Studio** Otter (2025.2.1) or later
- **JDK** 11 or higher
- **Android SDK** 28 or higher
- **Gradle** 8.13.1 (wrapper included)

### Clone & Build

```bash
# Clone the repository
git clone https://github.com/yourusername/AttendanceHub.git
cd AttendanceHub

# Build all variants
./gradlew build

# Build specific variant
./gradlew assembleTeacherDebug
./gradlew assembleStudentDebug

# Install on device
./gradlew installTeacherDebug
./gradlew installStudentDebug
```

### Configuration

#### Teacher App Configuration

No additional configuration required. The app will:
- Auto-generate hotspot credentials
- Use default port 8080
- Store sessions in app files directory

#### Student App Configuration

No additional configuration required. The app will:
- Request camera permissions on first launch
- Auto-detect session info from QR codes

---

## 📖 Usage Guide

### Teacher Guide

#### Starting a Session

1. **Launch the teacher app**
2. **Tap "Start Hotspot"** on the home screen
3. **Wait for QR code to appear** (2-3 seconds)
4. **Display the QR code** to students (or share SSID/password manually)
5. **Monitor the student list** as students connect
6. **Session is now active**

#### During a Session

- **View connected students** in real-time
- **See student count** at the top
- **Check connection times** for each student
- **Download attendance** anytime during the session

#### Ending a Session

1. **Tap "End Session & Disable Hotspot"** button
2. **Session is automatically saved**
3. **Students are disconnected**
4. **Return to home screen**

#### Exporting Attendance

1. **Tap the download icon** (📥) next to student count
2. **Choose sharing method** (Email, Drive, etc.)
3. **CSV file is shared** with session data

### Student Guide

#### Joining a Session

1. **Launch the student app**
2. **Point camera at teacher's QR code**
3. **Wait for automatic scanning** (1-2 seconds)
4. **App connects to WiFi automatically**
5. **Attendance is submitted**
6. **See confirmation message**

#### Manual Entry (if QR fails)

1. **Tap "Enter Manually"** on scanner screen
2. **Enter session details**:
   - SSID (from teacher's screen)
   - Password (from teacher's screen)
   - Server IP (usually 192.168.49.1)
3. **Tap "Connect"**
4. **Submit attendance**

---

## 📊 Data Export

### CSV Format

```csv
Session Name,Session 2025-12-01 08:30
Session ID,4b977823-cdf3-4ffd-ab15-5440df2f3f32
SSID,AndroidShare_3912
Start Time,2025-12-01 08:30:00
End Time,2025-12-01 10:15:00
Total Students,25

No.,Student ID,Name,Device ID,Connected At,Timestamp
1,STU001,John Doe,Samsung-AB,08:40:54,1733040654000
2,STU002,Jane Smith,Pixel-XYZ,08:41:12,1733040672000
3,STU003,Bob Johnson,OnePlus-99,08:41:45,1733040705000
```

### Export Options

The CSV can be shared via:
- 📧 **Email** (Gmail, Outlook, etc.)
- ☁️ **Cloud Storage** (Google Drive, Dropbox, OneDrive)
- 💬 **Messaging Apps** (WhatsApp, Telegram, Slack)
- 💾 **Local Storage** (Downloads folder)
- 📱 **Nearby Share** (Android's built-in sharing)

### File Location

Exported files are saved to:
```
/Android/data/com.attendancehub.teacher/files/exports/
attendance_2025-12-01_08-30-00.csv
```

---

## 🔒 Security & Privacy

### Security Features

✅ **WPA2 Encryption** - Hotspot uses WPA2 for WiFi security
✅ **Random Credentials** - Auto-generated passwords for each session
✅ **Session Expiry** - QR codes expire after session ends
✅ **Local-Only Server** - No internet connectivity required
✅ **Scoped Storage** - Files stored in app-specific directories
✅ **FileProvider** - Secure file sharing with URI permissions

### Privacy Considerations

- ✅ **No cloud storage** - All data stays on device
- ✅ **No user accounts** - No registration required
- ✅ **No tracking** - No analytics or telemetry
- ✅ **No ads** - Completely ad-free
- ✅ **Minimal permissions** - Only essential permissions requested

### Permissions Required

#### Teacher App
- `ACCESS_WIFI_STATE` - Check WiFi status
- `CHANGE_WIFI_STATE` - Create hotspot
- `ACCESS_FINE_LOCATION` - Required for WiFi APIs (Android 10+)
- `NEARBY_WIFI_DEVICES` - WiFi Direct (Android 13+)

#### Student App
- `CAMERA` - QR code scanning
- `ACCESS_WIFI_STATE` - Check WiFi status
- `CHANGE_WIFI_STATE` - Connect to hotspot
- `ACCESS_FINE_LOCATION` - Required for WiFi APIs (Android 10+)
- `INTERNET` - HTTP requests to local server

---

## 🐛 Known Issues

### Fixed Issues ✅

| Issue | Status | Date Fixed | Solution |
|-------|--------|------------|----------|
| Connected students not displaying | ✅ Fixed | 2025-12-01 | Type mismatch in timestamp conversion |
| Window leak on QR scanner | ✅ Fixed | 2024 | Proper lifecycle management |
| FileProvider resource not found | ✅ Fixed | 2025-12-01 | IDE cache issue, build successful |

### Open Issues ⚠️

| Issue | Priority | Status |
|-------|----------|--------|
| Date constructor deprecation warning | Low | Non-critical |
| Room database migration needed | Medium | Planned |
| PDF export support | Low | Feature request |

### Bug Reports

For bug reports, please check:
- `CONNECTED_STUDENTS_LIST_BUG_FIX.md`
- `WINDOW_LEAK_EXPLANATION.md`
- Other documentation files in the project root

---

## 👨‍💻 Development

### Code Style

- **Language**: Kotlin (prefer idiomatic Kotlin)
- **Formatting**: Default Android Studio formatting
- **Naming**: camelCase for variables, PascalCase for classes
- **Comments**: Clear, concise documentation

### Testing

```bash
# Run all tests
./gradlew test

# Run specific variant tests
./gradlew testTeacherDebugUnitTest
./gradlew testStudentDebugUnitTest
```

### Debugging

Enable verbose logging:
```kotlin
// In TeacherViewModel or StudentViewModel
private val TAG = "YourTag"
Log.d(TAG, "Your debug message")
```

### Building Release

```bash
# Build release APK
./gradlew assembleTeacherRelease
./gradlew assembleStudentRelease

# APKs will be in:
# app/build/outputs/apk/teacher/release/
# app/build/outputs/apk/student/release/
```

---

## 📚 Documentation

### Available Documentation Files

| File | Description |
|------|-------------|
| `PROJECT_README.md` | This file - comprehensive overview |
| `ARCHITECTURE.md` | Detailed architecture documentation |
| `MODULARIZATION_SUMMARY.md` | Module structure explanation |
| `NAVIGATION_COMPLETE.md` | Navigation implementation details |
| `HTTP_SERVER_COMPLETE.md` | Server implementation guide |
| `QR_AND_THEME_COMPLETE.md` | QR code and theming documentation |
| `STUDENT_QR_SCANNING_COMPLETE.md` | QR scanning implementation |
| `DOWNLOAD_STUDENT_LIST_COMPLETE.md` | Export feature documentation |
| `CONNECTED_STUDENTS_LIST_BUG_FIX.md` | Bug fix documentation |
| `CAMERA_DEBUG_GUIDE.md` | Camera troubleshooting |
| `QR_DEBUG_GUIDE.md` | QR code debugging |

### API Documentation

#### Teacher App APIs

**TeacherViewModel**
```kotlin
class TeacherViewModel(application: Application) : AndroidViewModel(application) {
    // State
    val uiState: StateFlow<TeacherUiState>
    
    // Actions
    fun startHotspot()
    fun stopHotspot()
    fun downloadStudentList()
}
```

**AttendanceServer**
```kotlin
class AttendanceServer(private val port: Int = 8080) {
    val connectedStudents: StateFlow<List<StudentAttendance>>
    
    fun startServer(): Result<Unit>
    fun stopServer()
    fun clearStudents()
}
```

#### Student App APIs

**StudentViewModel**
```kotlin
class StudentViewModel(application: Application) : AndroidViewModel(application) {
    val uiState: StateFlow<StudentUiState>
    
    fun connectToSession(sessionInfo: SessionInfo)
    fun submitAttendance(studentInfo: StudentInfo)
}
```

---

## 🎓 Use Cases

### Classroom Attendance
- **Primary Use**: Daily classroom attendance tracking
- **Benefits**: Fast, automated, no manual roll call
- **Capacity**: Supports 30+ students per session

### Workshop/Training Sessions
- **Use**: Track participants in workshops
- **Benefits**: Professional attendance records
- **Export**: Easy sharing with organizers

### Lab Sessions
- **Use**: Monitor student presence in labs
- **Benefits**: Timestamp-based tracking
- **Records**: Permanent digital records

### Events & Seminars
- **Use**: Attendee check-in at events
- **Benefits**: Quick QR-based check-in
- **Analytics**: Real-time attendance numbers

---

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. **Fork the repository**
2. **Create a feature branch** (`feature/amazing-feature`)
3. **Commit your changes** with clear messages
4. **Push to your branch**
5. **Open a Pull Request**

### Development Setup

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Run `assembleDebug` to ensure everything builds
5. Make your changes
6. Test thoroughly on both variants
7. Submit PR

---

## 📄 License

*License information to be added*

---

## 📞 Contact & Support

- **Developer**: WAHID-QANDIL
- **GitHub**: [Repository Link]
- **Email**: [Contact Email]

---

## 🙏 Acknowledgments

- **Android Team** - Jetpack Compose and modern Android development
- **ZXing** - QR code generation library
- **Google ML Kit** - Barcode scanning capabilities
- **Material Design** - UI/UX guidelines and components

---

## 📈 Project Status

| Aspect | Status |
|--------|--------|
| **Development** | ✅ Active |
| **Teacher App** | ✅ Functional |
| **Student App** | ✅ Functional |
| **CSV Export** | ✅ Complete |
| **QR Scanning** | ✅ Complete |
| **Documentation** | ✅ Comprehensive |
| **Testing** | ⚠️ In Progress |
| **Release** | 🔄 Pending |

---

## 🗺️ Roadmap

### Version 1.0 (Current)
- ✅ Basic hotspot management
- ✅ QR code generation and scanning
- ✅ Real-time attendance tracking
- ✅ CSV export
- ✅ Material Design 3 UI

### Version 1.1 (Planned)
- 📅 PDF export support
- 📅 Session history UI
- 📅 Statistics and analytics
- 📅 Dark mode improvements
- 📅 Multi-language support

### Version 2.0 (Future)
- 📅 Room database integration
- 📅 Offline sync
- 📅 Advanced reporting
- 📅 Customizable themes
- 📅 Attendance analytics

---

<div align="center">

**Made with ❤️ using Kotlin & Jetpack Compose**

*Last Updated: December 1, 2025*

**Version 1.0.0**

</div>

