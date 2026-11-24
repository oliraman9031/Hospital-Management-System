# Hospital Management System

A JavaFX-based desktop application designed to simplify hospital administration and patient record management. The system uses PostgreSQL for persistent data storage and provides role-based access for administrators and regular users.

## Overview

The Hospital Management System provides a centralized desktop interface for managing hospital records, including patients, doctors, receptionists, and other hospital workers.

It features account registration, authentication, patient record management, staff management, and an administrative dashboard with real-time record counts.

The application is built using JavaFX for its graphical interface and PostgreSQL for database management.

## Features

### 1. User Authentication
- User and administrator account types.
- Account registration and login functionality.
- Password confirmation during registration.
- Role-based navigation and access to application modules.
- Sign-out functionality.

### 2. Patient Management
- Register new patient records.
- View and search existing patient information.
- Search patients by first name or last name.
- Update patient details (administrator access).
- Delete patient records (administrator access).
- Store personal and contact information, including name, gender, age, city, email, address, and mobile number.

### 3. Doctor Management
- Add and maintain doctor records.
- Search doctors by first name or last name.
- Store professional and personal information.
- Maintain doctor identification and qualification details.

### 4. Receptionist Management
- Register receptionist information.
- Maintain receptionist records.
- Search receptionist records.

### 5. Worker Management
- Register hospital worker information.
- Maintain worker records.
- Search worker records.

### 6. Administrative Dashboard
- View the total number of registered patients.
- Monitor the number of doctors.
- View receptionist and worker counts.
- Navigate between different management modules.

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 17 | Core application development |
| JavaFX 21 | Desktop graphical user interface |
| PostgreSQL | Relational database |
| JDBC | Database connectivity |
| Maven | Dependency and project management |
| CSS | Application styling |

## Project Structure

```text
Hospital-Management-System/
│
├── .mvn/
│
├── src/
│   └── main/
│       ├── java/
│       │   └── hospitalmanagement/
│       │       ├── DatabaseConnection.java
│       │       ├── HospitalManagementApp.java
│       │       └── HospitalRepository.java
│       │
│       └── resources/
│           └── hospitalmanagement/
│               └── hospitalmanagement.css
│
├── hospital-postgresql.sql
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

### Main Components

- **HospitalManagementApp.java:** Main JavaFX application containing the graphical interface, authentication screens, dashboard, navigation, and management forms.
- **HospitalRepository.java:** Handles database operations, including authentication, registration, patient records, doctor records, staff records, and dashboard statistics.
- **DatabaseConnection.java:** Establishes PostgreSQL connections using JDBC and environment-based database configuration.
- **hospitalmanagement.css:** Defines the visual styling of the desktop application.
- **hospital-postgresql.sql:** Contains the SQL schema required to initialize the database.

## Prerequisites

Before running the application, make sure you have installed:

- Java Development Kit (JDK) 17 or newer.
- PostgreSQL 15 or newer.
- Git.
- Internet connection for the initial Maven dependency download.

## Installation and Setup

### 1. Clone the Repository

```bash
git clone https://github.com/oliraman9031/Hospital-Management-System.git
```

Navigate to the project directory:

```bash
cd Hospital-Management-System
```

### 2. Create the PostgreSQL Database

Open PostgreSQL or PowerShell and create a database named `hospital`.

```sql
CREATE DATABASE hospital;
```

### 3. Initialize the Database Schema

Run the provided SQL script:

**Windows PowerShell:**

```powershell
psql -U postgres -d hospital -f hospital-postgresql.sql
```

This creates the following tables:

- `adminreg` – Administrator accounts.
- `login` – Regular user accounts.
- `patient` – Patient information.
- `doctor` – Doctor information.
- `recp` – Receptionist information.
- `worker` – Hospital worker information.

The SQL script also inserts a default administrator account.

### 4. Configure Database Connection

The application uses environment variables to configure PostgreSQL connectivity.

The default database configuration is:

| Setting | Default Value |
|---|---|
| Host | localhost |
| Port | 5432 |
| Database | hospital |
| Username | postgres |
| Password | Set through environment variable |

Configure the following environment variables in PowerShell:

```powershell
$env:HOSPITAL_DB_URL = "jdbc:postgresql://localhost:5432/hospital"
$env:HOSPITAL_DB_USER = "postgres"
$env:HOSPITAL_DB_PASSWORD = "your_postgresql_password"
```

Replace the password with your actual PostgreSQL password.

### 5. Run the Application

On Windows:

```powershell
.\mvnw.cmd clean javafx:run
```

On Linux or macOS:

```bash
chmod +x mvnw
./mvnw clean javafx:run
```

Maven will download the required dependencies during the initial execution and launch the JavaFX desktop application.

## Default Administrator Account

The database initialization script provides a default administrator account:

| Field | Value |
|---|---|
| Username | admin |
| Password | admin |

**Important:** These are demonstration credentials. Change the default password and improve password storage security before using the application in a real environment.

## Application Workflow

1. Launch the desktop application.
2. Select the account type.
3. Register a new account or sign in.
4. Access the appropriate dashboard.
5. Navigate to the available management modules.
6. Add, search, or manage hospital records according to account permissions.
7. Sign out when finished.

## Database Architecture

The application uses PostgreSQL as its relational database.

The database stores information in separate tables for authentication, patients, doctors, receptionists, and workers.

Java communicates with PostgreSQL through JDBC, while `HospitalRepository` encapsulates the database queries and record-management operations.

Prepared statements are used for database operations to provide parameterized SQL queries.

## Future Improvements

Potential enhancements include:

- Secure password hashing and improved authentication.
- More granular role-based access control.
- Appointment scheduling and management.
- Doctor availability and department management.
- Patient medical history and prescription management.
- Billing and payment management.
- Reporting and analytics.
- Automated database backups.

## Author

**Raman Oli**

GitHub: [@oliraman9031](https://github.com/oliraman9031)

## License

This project is available for educational and learning purposes. No formal open-source license has currently been specified.
