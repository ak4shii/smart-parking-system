# Smart Parking Mobile App

Android mobile app for the Smart Parking System built with Kotlin and Jetpack Compose.

## Features

- 🔐 **Authentication** - Login and Register with JWT authentication
- 📊 **Dashboard** - Real-time parking slot status with occupancy grid
- 📋 **Entry Logs** - View vehicle entry/exit history
- 📡 **Devices** - Manage IoT sensors and microcontrollers
- 🔄 **Real-time Updates** - WebSocket integration for live updates

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Networking**: Retrofit + OkHttp
- **Navigation**: Jetpack Navigation Compose
- **State Management**: StateFlow + ViewModel
- **Local Storage**: DataStore Preferences

## Project Structure

```
app/src/main/java/com/smartparking/mobile/
├── SmartParkingApp.kt          # Application class
├── MainActivity.kt             # Main activity
├── data/
│   ├── api/                    # Retrofit API services
│   ├── local/                  # DataStore for local storage
│   ├── model/                  # Data models/DTOs
│   └── repository/             # Repository layer
├── di/                         # Hilt modules
└── ui/
    ├── navigation/             # Navigation setup
    ├── screens/                # Screen composables + ViewModels
    └── theme/                  # Material3 theme
```

## Setup

1. Open the project in Android Studio (Hedgehog or newer)
2. Sync Gradle files
3. Update API URL in `app/build.gradle.kts` if needed:
   ```kotlin
   buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/sps\"")
   ```
4. Run on emulator or device

## API Configuration

- **Development (Emulator)**: `http://10.0.2.2:8080/sps` (10.0.2.2 is localhost from emulator)
- **Development (Device)**: Use your machine's IP address
- **Production**: Update the release build config

## Backend Requirements

The mobile app requires the Smart Parking System backend to be running. See the main project README for backend setup.

## Screenshots

*Coming soon*

## License

This project is part of the Smart Parking System IoT project.
