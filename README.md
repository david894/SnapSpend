<img width="1536" height="1024" alt="ChatGPT Image Aug 17, 2025, 10_59_21 PM" src="https://github.com/user-attachments/assets/1d94a3cf-a52c-4c13-9ec6-ef889ec43571" />
## SnapSpend  budgeting App 💸

SnapSpend is a modern, offline-first, and collaborative expense tracker for Android, built with the latest technologies. It's designed to be fast, intuitive, and intelligent, helping you manage your finances with minimal effort.

---

## ✨ Core Features

* **Effortless Expense Tracking**: Add expenses in seconds with a dynamic home screen widget and a clean, dialog-based input.
* **Smart Categorization**: Automatically categorizes your spending by fetching your location in the background and identifying the venue (e.g., "Restaurant", "Supermarket").
* **Intelligent Dashboard**: An at-a-glance view of your finances, featuring:
    * A beautiful donut chart visualizing your spending breakdown.
    * Month-over-month spending comparison with percentage change.
    * A historical line chart to track your spending trends over time.
    * Clickable cards to drill down into detailed transaction lists.
* **Collaborative Budgeting**: Share collections with other users via a simple PIN system. All transactions, budgets, and collection details sync in real-time using Firebase.
* **Full Customization**:
    * Create, rename, and delete your own spending collections.
    * Set monthly budgets for each collection and track your progress with visual bars.
    * Personalize collections with a wide range of icons and beautiful, theme-aware colors.
* **Automated Tracking**: A background service can read notifications from banking and e-wallet apps (like Maybank and TNG) to create expense entries automatically.
* **Offline-First**: The app is built to work seamlessly without an internet connection. All data is stored locally and syncs to the cloud when you're back online.

---

## 🛠️ Tech Stack & Architecture

SnapSpend is built using a modern, 100% Kotlin stack, following Google's recommended best practices.

* **UI**: Jetpack Compose for a declarative, modern UI.
* **Architecture**: MVVM (Model-View-ViewModel) with a Repository pattern.
* **Local Database**: Room for robust, offline data persistence.
* **Background Processing**: WorkManager for reliable, guaranteed background tasks like location fetching and budget alerts.
* **Cloud Backend**: Firebase for real-time data synchronization and user authentication.
    * **Firestore**: The real-time, NoSQL database for shared collections.
    * **Firebase Auth**: For anonymous user authentication.
* **Asynchronous Programming**: Kotlin Coroutines and Flow for managing background threads and data streams.
* **Navigation**: Jetpack Navigation for Compose.
* **Charting**: A custom `Canvas`-based pie chart for a unique and beautiful look.

---

## 🚀 Setup & Installation

To get this project running, you'll need to set up your own Firebase backend.

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/david894/SnapSpend.git
    ```
2.  **Firebase Setup**:
    * Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project.
    * Add an Android app to the project with the package name `com.kx.snapspend`.
    * Download the `google-services.json` file and place it in the `app/` directory of the project.
    * In the Firebase Console, go to **Build > Firestore Database**, create a database, and in the **Rules** tab, paste the following and publish:
        ```
        rules_version = '2';
        service cloud.firestore {
          match /databases/{database}/documents {
            match /shared_collections/{pin} {
              allow read, create, update: if request.auth != null;
            }
          }
        }
        ```
3.  **Google Maps API Key**:
    * Go to the [Google Cloud Console](https://console.cloud.google.com/) and enable the **Geocoding API**.
    * Get your API key and paste it into the `API_KEY` constant in the `workers/LocationProcessingWorker.kt` file.
4.  **Build and Run**: Open the project in Android Studio and run it on an emulator or a physical device.

---

## 🔮 Future Enhancements

* **Backup & Restore**: Add functionality to manually back up the local database to Google Drive or a local file.
* **Advanced Reports**: Create a dedicated reports screen with more detailed charts and the ability to export data to CSV.
* **On-Device ML**: Use on-device machine learning to suggest categories based on your personal spending habits.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.
