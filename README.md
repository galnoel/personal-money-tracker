# Money Tracker

Money Tracker is an Android application designed to help users track their personal finances and expenses easily. The app features a modern UI built with Jetpack Compose, a dashboard for overviews, transaction management, and a home screen widget for quick access.

## Features

- **Dashboard**: Get a quick overview of your current financial status, recent transactions, and summaries.
- **Transactions Management**: View, add, edit, and keep a history of all your income and expenses.
- **Quick Input**: Add transactions rapidly without hassle.
- **Accounts & Transfers**: Cash, cards, and custom accounts have independent balances; transfers do not change reports.
- **Home Screen Widget**: A one-tap Quick Add button opens a translucent form over the launcher.
- **Offline-first Supabase Sync**: Room updates immediately while WorkManager reconciles in the background.
- **Cash-flow Charts**: Touch-enabled Day, Month, and Year income/expense charts with period navigation.
- **Customization**: Synchronized accent color and one selectable base currency.
- **Monthly Reports**: Filter mutations by month, then print them or save an A4 PDF.
- **Light Neumorphic UI**: Simple soft-raised surfaces across the app and home-screen widget.

## Architecture & Tech Stack

This project follows modern Android development practices and uses an MVVM (Model-View-ViewModel)/Clean Architecture approach with separation of concerns (`data`, `domain`, `ui`, `widget`).

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) for building the native UI.
- **Navigation**: [Navigation Compose](https://developer.android.com/jetpack/compose/navigation) for handling screens and transitions.
- **Remote Database**: [Supabase](https://supabase.com/) Postgres with email/password Auth and Row Level Security.
- **Local Cache**: Room plus network-constrained WorkManager synchronization.
- **Dependency Injection**: [Hilt](https://dagger.dev/hilt/) for managing dependencies.
- **Asynchronous Programming**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & Flows.
- **Widgets**: [Jetpack Glance](https://developer.android.com/jetpack/compose/glance) for building App Widgets with Compose.

## Project Structure

- `app/src/main/java/com/tracker/data/`: Contains Supabase DTOs and the remote repository implementation.
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

1. Create a Supabase project.
2. Enable the **Email** provider under Authentication → Providers. Anonymous sign-in is not required.
   You may keep email confirmation on;
   after confirming in the browser, return to the app and sign in.
3. Run both migration SQL files in order, or use `supabase db push`:
   - `supabase/migrations/202607250001_create_transactions.sql`
   - `supabase/migrations/202607250002_accounts_transfers_preferences.sql`
4. Copy `.env.example` to `.env` and enter your project URL and publishable key.
5. Open the project in Android Studio, sync Gradle, and run the app.

From a terminal, install and launch on a connected phone with:

```powershell
.\gradlew.bat installDebug
adb shell am start -n com.tracker.moneytracker/com.tracker.ui.MainActivity
```

Only the publishable key is compiled into Android. Never place a secret or service-role key in client code.

Email/password sessions persist securely on the device and the same account can be used across devices.
If an older installation already has an anonymous account, use **Create account** in the app to
upgrade that user in place and preserve its transaction ownership.
