# BabyBuy

BabyBuy is an Android application designed to help parents create a list of items they need to buy before and after the birth of their baby. The app allows users to store a list of items, including descriptions, prices, and pictures of the items. Additional features include optional location tracking of shops and item delegation.

## Table of Contents

- [Introduction](#introduction)
- [Features](#features)
- [User Flow](#user-flow)
- [Screenshots](#screenshots)
- [Technology Stack](#technology-stack)
- [Database Schema](#database-schema)
- [Installation](#installation)
- [Usage](#usage)
- [Contributing](#contributing)
- [License](#license)

## Introduction

BabyBuy is developed to assist expecting parents in organizing and managing the list of items required for their newborn. With a user-friendly interface and essential features, it simplifies the process of item management, delegation, and location tracking.

## Features

- User registration and login
- Create, edit, and delete items
- Upload or take a picture of the item
- Mark items as purchased
- Delegate items to contacts via SMS
- Optional geotagging of items to record and view shop locations on a map
- Gesture controls for improved item management (e.g., swipe to delete or mark as purchased)

## User Flow

1. **Home Screen**: Entry point of the app with options to register or log in.
2. **Registration/Login**: New users can register, and existing users can log in.
3. **Dashboard**: Main screen displaying the user's items and suggestions.
4. **Create Item**: Users can add new items, including pictures, descriptions, and prices.
5. **Manage Items**: Users can view, edit, delete, and mark items as purchased.
6. **Item Details**: Detailed view of each item with options to edit, delete, delegate, or geotag the item.
7. **Item Delegation**: Send item details via SMS to contacts.
8. **Geotagging**: Option to add and view the location of the shop on a map.

## Screenshots

### Wireframe
![image](https://github.com/dipeshkumarsah98/BabyBuy/assets/63381568/d38eb84e-b816-4de2-8b1f-d548dc2c7664)

### Flowchart
![diagram-export-6-29-2024-1_46_24-AM](https://github.com/dipeshkumarsah98/BabyBuy/assets/63381568/676be832-f372-4f3f-9ec2-16565bc9df92)

### Sequential Diagram
![diagram-export-6-29-2024-1_47_23-AM](https://github.com/dipeshkumarsah98/BabyBuy/assets/63381568/002b4998-2e6e-49e1-be0d-0e573c79061a)

### ER Diagram
![diagram-export-6-29-2024-1_48_46-AM](https://github.com/dipeshkumarsah98/BabyBuy/assets/63381568/e1615a45-5a28-4e36-8938-fe38ebd49ed7)

## Database Schema

```plaintext
users [icon: user, color: blue] {
  id string pk
  gender string
  name string
  email string
  profileImage string
  password string
}

products [icon: package, color: green] {
  productId string pk
  name string
  description string
  category string
  price int
  quantity int
  image string
  storeLocationLat double
  storeLocationLng double
  userId string
  markAsPurchased boolean
}

users.id <> products.userId
```

## Technology Stack
- **Programming Language**: Kotlin
- **Database**: Firebase
- **IDE**: Android Studio
- **Tools and Libraries**:
  - Firebase Authentication
  - Firebase Realtime Database
  - Firebase Storage
  - Google Maps API for location tracking
  - SMS API for item delegation


## Installation

To get a local copy up and running, follow these steps:

1. Clone the repository:
    ```bash
    git clone https://github.com/dipeshkumarsah98/BabyBuy.git
    ```
2. Open the project in Android Studio.
3. Set up Firebase for your project:
    - Add your Firebase project to the app.
    - Download the `google-services.json` file and place it in the `app` directory.
    - Enable Firebase Authentication, Realtime Database, and Storage.

4. Build and run the project on your Android device or emulator.

## Usage

1. **Register** or **Log in** to the app.
2. Navigate to the **Dashboard** to view and manage your items.
3. **Create new items** by providing details and uploading/taking pictures.
4. **Edit or delete items** from the item list.
5. **Mark items as purchased** once you buy them.
6. **Delegate items** to your contacts via SMS.
7. **Geotag items** to save and view the shop locations on a map.

## Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

Distributed under the MIT License. See `LICENSE` for more information.


