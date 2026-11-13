# Antrenamente Aikido — Aikido Training Tracker

*English · [Română](README.ro.md)*

An Android app that tracks attendance at Aikido training, instead of a manually filled-in spreadsheet. On every training evening, the phone asks whether you went, and the answer is saved in a calendar with statistics.

Built with the help of AI (Claude), from the author's requirements.

## What it does

- **It asks you, so you don't have to remember.** After training time, you get a notification with two buttons, **Attended** and **Did not attend**, which you answer directly, without opening the app.
- **It doesn't forget anything.** If a day is left unanswered, the app keeps track and asks again the next day, until you answer.
- **It accounts for stages and exams**, which can fall on any day, not just the usual training days.
- **It shows a colour-coded calendar**: green for attended, red for missed, grey for "not held", and gold for exams.
- **It calculates statistics** per month and per year: how many trainings you attended out of how many were scheduled, past and upcoming.
- **It backs itself up**, once a week, to a file you choose, for example on Google Drive.

## How it works, step by step

### Training schedule

The app assumes training on **Monday, Wednesday and Friday**. These days are fixed in the code and can't be changed from the app; if your dojo has a different schedule, the code needs to be changed.

### The daily question

Every training day, at a time you choose in Settings (21:00 by default), the app checks whether any days are still unanswered:

- **Today** only counts after the chosen time, so you aren't asked before the training has actually happened.
- **Past days**, if left unanswered, show up right away, oldest first.
- Each open day gets its **own notification**, with **Attended** / **Did not attend** buttons. Answering one immediately brings up the next one, if there's still an unresolved day.
- You can also answer later, from the app itself: days still open show up on the home screen.

Android can delay the notification by up to an hour, because of the phone's battery optimizations.

### Stages and exams

From the home screen or the calendar, you can add a stage or an exam on a single day or a range of days, with a name you choose (for example "Perpelici seminar"). These days get asked about the same way as regular training, even though they aren't a usual training day.

### Possible answers

- **Attended** — counted as present in the statistics.
- **Did not attend** — counted as absent in the statistics.
- **Not held** — for days when training was cancelled or didn't concern you (for example, someone else's exam). Not counted in the statistics.

Any answer can be changed at any time, from the calendar.

### Calendar and statistics

The calendar shows each day coloured by its answer. The home screen and the calendar show statistics as "x / total":

- **The total** covers every training day (Monday, Wednesday, Friday) in the period, plus stages and exams, **including days that haven't happened yet**. Days marked "not held" are left out of the total.
- **The percentage** is calculated only from days already answered, so it stays meaningful mid-month, instead of dropping artificially because of upcoming days.

### Backups

Data stays only on the phone, in a local database; the app has no server and doesn't send anything anywhere on its own.

- **Manual backup**, from Settings: save a file whenever you want, for example before switching phones.
- **Automatic backup**, also from Settings: choose a file once, usually on Google Drive, and the app overwrites it once a week, as long as the phone has internet.
- **Restore**: replaces all data in the app with what's in the chosen file. A file that isn't a valid backup is rejected, without changing anything.

A backup doesn't save the question time, only the attendance data.

## Installation

The app isn't on Google Play. It's installed from the APK file, downloaded from this repo's **Releases** section:

1. Download the `.apk` file from the latest published release.
2. Open it on your phone. If a warning about "unknown sources" appears, allow the install.
3. On first launch, allow notifications, otherwise the daily question can't appear.
4. Recommended: exclude the app from the phone's battery optimization, so notifications and the automatic backup aren't blocked.

Requires Android 8.0 or newer.

## Starting fresh

The app starts with no attendance history: the file loaded on first install (`app/src/main/assets/history.json`) is an empty template, with today's date as the tracking start. Anyone installing it begins with their own blank slate, not anyone else's data, and won't be asked about days before they installed it.

## Technologies used

Kotlin and Jetpack Compose (Material 3), a local SQLite database, native Android notifications, and backups through the Android file picker (no Google Cloud account or own server).
