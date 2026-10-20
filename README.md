<div align="center">

<img src="docs/freshlink-logo.svg" alt="FreshLink leaf logo" width="88" height="88">

# FreshLink

### Rescue food. Feed community.

**A local-first food rescue coordination desktop app for neighbourhoods, food banks, kitchens, bakeries, and community groups.**

![Java 17+](https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk&logoColor=white)
![Java Swing](https://img.shields.io/badge/UI-Java%20Swing-2563EB?logo=java&logoColor=white)
![Offline first](https://img.shields.io/badge/Mode-Offline--first-16A34A)
![Dependencies](https://img.shields.io/badge/Dependencies-JDK%20only-15803D)

</div>

FreshLink helps local organizations offer surplus food for pickup, coordinate reservations, and record collections. It runs as a Java desktop application, stores its data on the local computer, and does not require a server, database, web service, or network connection.

> **Project status:** Active prototype. The core listing, reservation, collection, reporting, and local persistence workflows are implemented. Known limitations are listed below.

## Contents

- [Features](#features)
- [Roles and demo accounts](#roles-and-demo-accounts)
- [Requirements](#requirements)
- [Build and run](#build-and-run)
- [Screenshots](#screenshots)
- [Data storage and CSV format](#data-storage-and-csv-format)
- [Project structure](#project-structure)
- [GitHub topics](#github-topics)
- [Known limitations](#known-limitations)

## Features

### Workspace and dashboard

- Role-aware sign-in and registration for Donors and Collectors, plus an Admin account.
- Sidebar navigation for Dashboard, Food Board, Urgent Rescue, My Reservations, Add Listing, Reports, Notifications, Users, and Settings.
- Dashboard totals for active, available, reserved, expiring, and rescued-meal estimates.
- Java2D category donut and rescue-status bar charts, an attention panel, and a recent-listing activity panel.
- Color-coded category, urgency, reservation, collection, and expiry states.
- Rounded cards, a consistent green community-focused palette, adjustable accent color and text size, and a minimum window size suitable for desktop use.

### Food listings and rescue workflow

- Create, edit, and remove listings according to the signed-in role.
- Store food name, category, quantity, unit, best-before date, donor, donor contact, pickup location, pickup window, priority, and notes.
- Browse listings in either card or table view.
- Filter by status and category; search names, donors, locations, notes, categories, and status; sort by expiry, name, quantity, or pickup location.
- Collectors can reserve available listings and record collection. Donors and Admins can cancel a reservation.
- Urgent Rescue view brings priority and expiring listings forward.
- CSV export for the inventory and a current impact summary.

### Notifications, accounts, and preferences

- Local notification center for reservation, cancellation, collection, and admin expiry alerts.
- Admin user directory for viewing local accounts and their roles.
- Preferences for accent color, text size, pickup city, default pickup duration, admin expiry alerts, and notification sounds.
- Admin backup restore and automatic rolling inventory backups.

### Data safety

- Inventory, users, settings, and notifications remain local in the user's home directory.
- User passwords are stored as salted SHA-256 hashes, not plain text.
- Inventory and account writes use temporary files and atomic replacement when supported by the filesystem.
- Inventory CSV quoting supports commas, quotes, and embedded newlines; malformed rows are skipped with a warning.
- Legacy inventory formats from earlier FreshLink versions are read and upgraded on save.

## Roles and demo accounts

The first run creates fictional demo accounts. The sign-in screen starts with the admin email filled in and displays the demo admin credentials.

| Role | Email | Password |
| --- | --- | --- |
| Admin | `admin@freshlink.app` | `admin123` |
| Donor | `bakery@freshlink.app` | `donor1234` |
| Donor | `farms@freshlink.app` | `donor1234` |
| Donor | `market@freshlink.app` | `donor1234` |
| Collector | `hope@freshlink.app` | `collect1234` |
| Collector | `foodbank@freshlink.app` | `collect1234` |
| Collector | `kitchen@freshlink.app` | `collect1234` |

- **Donors** can add listings and edit or remove their own listings. They can cancel reservations for their listings.
- **Collectors** can browse listings, reserve available food, cancel their reservations, and mark reserved food as collected.
- **Admins** can review all listings, access reports and users, manage listings, restore inventory backups, and receive expiry alerts.

These credentials are for local demonstration only. Use a unique password for any account you register. FreshLink currently does not provide password reset or account deletion.

## Requirements

- JDK 17 or newer (JDK 26 also compiles the current source).
- Windows, macOS, or Linux with a graphical desktop environment for the Swing interface.
- No Maven or Gradle setup and no third-party Java dependencies.

## Build and run

Run commands from the repository root. The source layout is `src/main/java/freshlink/`.

### Windows PowerShell

```powershell
New-Item -ItemType Directory -Force out | Out-Null
$sources = Get-ChildItem -Path src/main/java/freshlink -Filter *.java | ForEach-Object FullName
javac -encoding UTF-8 -d out $sources
java -cp out freshlink.Main
```

### macOS / Linux

```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src/main/java/freshlink -name '*.java')
java -cp out freshlink.Main
```

To remove compiled output and rebuild:

```bash
rm -rf out
mkdir -p out
javac -encoding UTF-8 -d out $(find src/main/java/freshlink -name '*.java')
```

On Windows PowerShell, replace the `rm -rf` command with:

```powershell
Remove-Item -Recurse -Force out -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force out | Out-Null
$sources = Get-ChildItem -Path src/main/java/freshlink -Filter *.java | ForEach-Object FullName
javac -encoding UTF-8 -d out $sources
```

The application opens a sign-in window. The first run creates the local demo inventory, accounts, and data folders automatically.

## Screenshots

Add current application screenshots here before publishing the repository. Suggested captures:

| Dashboard | Food Board |
| --- | --- |
| `docs/screenshots/dashboard.png` | `docs/screenshots/food-board.png` |

| Reservation workflow | Impact report |
| --- | --- |
| `docs/screenshots/reservations.png` | `docs/screenshots/reports.png` |

## Data storage and CSV format

FreshLink stores files under `~/.freshlink/` (the current user's home directory):

| File | Purpose |
| --- | --- |
| `listings.csv` | Food inventory and reservation/collection state. |
| `users.csv` | Account name, email, role, random salt, and password hash. |
| `notifications.csv` | Notification recipient, message, timestamp, and read state. |
| `settings.properties` | Local appearance and pickup preferences. |
| `backups/listings-YYYY-MM-DD-HH-mm-ss.csv` | Rolling inventory backups; newest ten are retained. |

The current inventory CSV header is:

```text
id,name,category,quantity,unit,expiry,donor,location,notes,collectedBy,reservedBy,contactNumber,pickupFrom,pickupUntil,priority
```

Dates use ISO `yyyy-MM-dd`; pickup times use `HH:mm`. Fields use standard CSV quoting. Older ten-column Base64 inventory files and the previous eleven-column CSV files are supported during migration. If the old inventory is recognized as the untouched nine-listing demo, the expanded fictional sample inventory is added while preserving the old listings.

## Project structure

```text
FreshLink/
├── README.md
├── .gitignore
└── src/main/java/freshlink/
    ├── Main.java                 Swing application entry point
    ├── FreshLinkApp.java         UI, navigation, and user workflows
    ├── FoodItem.java             Food listing model
    ├── DemoData.java             Fictional first-run listing data
    ├── User.java                 Local account model
    ├── AppNotification.java      Notification model
    ├── FoodStore.java            Inventory CSV, backups, and restore
    ├── UserStore.java            Account CSV and password hashing
    ├── NotificationStore.java    Notification CSV storage
    ├── SettingsStore.java        Properties-backed preferences
    └── CsvUtil.java              CSV escaping and parsing
```

## GitHub topics

Suggested repository topics: `java`, `java-swing`, `food-rescue`, `food-waste`, `sustainability`, `community`, `offline-first`, `csv`, `desktop-app`.

## Known limitations

- Expiry urgency is based on calendar dates; it does not track exact expiry timestamps.
- Reports are all-time summaries and do not yet include custom date ranges, donor/collector rankings, or wastage estimates.
- The activity panel is derived from current listing state rather than a complete immutable audit log.
- The UI supports accent and text-size preferences; a full dark theme is not implemented yet.
- Account editing, password reset, and deletion are not implemented.
- Reservations store the collector's account name but do not have a separate pickup-contact field or reservation timestamp.
- The app is a local desktop tool; it does not synchronize data between computers.

## License

No license is currently included. Add a `LICENSE` file before distributing or accepting reuse under an open-source license.
