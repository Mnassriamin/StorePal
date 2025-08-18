StorePal - Android Inventory Scanner

StorePal is a modern Android application designed for small store owners to manage their inventory effortlessly. It features real-time database synchronization with Firebase, barcode scanning for quick price checks, and a clean, user-friendly interface built with Material Design 3.

✨ Features

Real-Time Database: Built with Firebase Firestore as the backend, ensuring data is always synced across devices and persists even if the app is uninstalled.

Offline First: Uses a local Room database as an offline cache, allowing the app to be fully functional without an internet connection.

Barcode Scanning: Integrated with ML Kit and CameraX to provide a fast and reliable barcode scanner.

Scan existing items to quickly view their price and picture.

Scan new, unknown items to instantly begin the process of adding them to the inventory.

Inventory Management:

Add, edit, or delete items.

Attach a product image by taking a new photo with the camera.

Images are uploaded to Firebase Cloud Storage for permanent, online access.

Dynamic Search: A search bar on the main screen to filter the inventory list by name in real-time.

Modern UI: A clean, grid-based layout with a bottom navigation bar, built using the latest Material Design 3 components and principles.

🛠️ Tech Stack & Architecture

This project is built using 100% Kotlin and follows modern Android development practices.

Architecture: MVVM (Model-View-ViewModel)

Asynchronous Programming: Kotlin Coroutines and Flow for managing background tasks and reactive data streams.

Dependency Injection: Manual DI using an Application class and ViewModelFactory.

Networking & Database:

Room: For local database caching.

Firebase Firestore: As the single source of truth for online data.

Firebase Cloud Storage: For storing and retrieving product images.

UI & Navigation:

AndroidX Navigation Component: For managing fragment navigation.

View Binding: For safe and efficient view access.

Material Components 3: For all UI elements (Cards, Dialogs, Bottom Navigation, etc.).

Image Loading: Coil for efficiently loading and caching images.

Hardware Integration:

CameraX: For a modern, lifecycle-aware camera implementation.

ML Kit: For high-performance on-device barcode detection.

FileProvider: For securely handling image files.

🚀 Setup and Installation

To build and run this project yourself, you will need to set up your own Firebase project.

Clone the repository:

Bash

git clone https://github.com/Mnassriamin/InventoryApp.git

Firebase Setup:

Go to the Firebase Console and create a new project.

In your new project, add a new Android app with the package name com.example.elmnassri.

Follow the setup steps to download the google-services.json file.

Place the downloaded google-services.json file in the app/ directory of the project.

In the Firebase Console, enable Firestore Database and Cloud Storage.

For Cloud Storage, go to the Rules tab and update the rules to allow reads and writes for development:

rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read, write: if true;
    }
  }
}
Build the App:

Open the project in the latest version of Android Studio.

Let Gradle sync the dependencies.

Build and run the app.

📄 License

This project is licensed under the MIT License. See the LICENSE file for details.

MIT License

Copyright (c) 2025 [Amine Mnasri]

Permission is hereby granted, free of charge, to any person obtaining a copy
...
