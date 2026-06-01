# Money Tracker

Money Tracker is an Android application designed to help users track their personal finances and expenses easily. The app features a modern UI built with Jetpack Compose, a dashboard for overviews, transaction management, and a home screen widget for quick access.

## Features

- **Dashboard**: Get a quick overview of your current financial status, recent transactions, and summaries.
- **Transactions Management**: View, add, edit, and keep a history of all your income and expenses.
- **Quick Input**: Add transactions rapidly without hassle.
- **Home Screen Widget**: A Glance-based app widget allowing you to view details or input transactions quickly right from your home screen.
- **Local Storage**: All data is securely stored locally on your device.

## Architecture & Tech Stack

This project follows modern Android development practices and uses an MVVM (Model-View-ViewModel)/Clean Architecture approach with separation of concerns (`data`, `domain`, `ui`, `widget`).

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) for building the native UI.
- **Navigation**: [Navigation Compose](https://developer.android.com/jetpack/compose/navigation) for handling screens and transitions.
- **Local Database**: [Room](https://developer.android.com/training/data-storage/room) for robust local SQLite data storage.
- **Dependency Injection**: [Hilt](https://dagger.dev/hilt/) for managing dependencies.
- **Asynchronous Programming**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & Flows.
- **Widgets**: [Jetpack Glance](https://developer.android.com/jetpack/compose/glance) for building App Widgets with Compose.

## Project Structure

- `app/src/main/java/com/tracker/data/`: Contains database configurations (Room), DAOs, entities, and repository implementations.
- `app/src/main/java/com/tracker/domain/`: Contains domain models and repository interfaces.
- `app/src/main/java/com/tracker/ui/`: Contains the UI layer (Screens like dashboard, transactions, quick input, MainActivity) and Compose theme logic.
- `app/src/main/java/com/tracker/widget/`: Contains app widget implementations built with Glance.
- `app/src/main/java/com/tracker/di/`: Contains Hilt modules for dependency injection.

## Requirements

- Android Studio (latest)
- Min SDK 26
- Target SDK 35
- JDK 17

## Building and Running

1. Clone the repository.
2. Open the project in Android Studio.
3. Sync the project with Gradle files.
4. Select a connected device or emulator and run the app.
