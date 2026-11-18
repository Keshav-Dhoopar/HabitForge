# HabitForge - Habit & Study Tracker with Streaks

A desktop application built in **Java** that helps you build daily habits and study consistently. Track habits, log study minutes, and watch your **streaks** grow, with a 7-day progress chart. All data is stored permanently in a **SQLite** database.

Made for the **OOPs with Java** final practical.

<!-- After uploading a screenshot named screenshot.png to this repo, remove the arrows around the line below:
![HabitForge Dashboard](screenshot.png)
-->

---

## Features

- Two habit types: **Daily Habit** (done / pending) and **Study Habit** (minutes vs daily goal)
- **Streak tracking**: current streak and best streak for every habit
- **Badges** that unlock with streaks: Starter, Rising, On Fire, Champion, Legend (study habits also earn Scholar and Scholar Pro)
- **Dashboard** cards: total habits, done today, best streak, total study time
- **Today's progress bar** and a **7-day bar chart** drawn with `Graphics2D`
- Complete, undo and delete habits with one click
- **Load Demo Data** button to fill the app with sample data for presentations
- Permanent storage with SQLite, with no server setup needed

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java (JDK 17 or newer) |
| Frontend | Java Swing + custom Graphics2D chart |
| Backend | Java + JDBC |
| Database | SQLite (`sqlite-jdbc-3.36.0.3.jar`) |
| Editor | Visual Studio Code |

---

## Project Structure

```
Habit-Study-Tracker-with-streaks/
|-- Main.java                    # Frontend: Swing GUI
|-- Database.java                # Backend: JDBC queries + streak logic
|-- Models.java                  # Model: OOP classes (interface, abstract class, inheritance)
|-- habits.db                    # SQLite database (auto-created on first run)
|-- sqlite-jdbc-3.36.0.3.jar     # SQLite JDBC driver
```

---

## OOP Concepts Used

| Concept | Where |
|---------|-------|
| Interface | `Trackable` |
| Abstract class | `Habit` |
| Inheritance | `DailyHabit` and `StudyHabit` extend `Habit` |
| Polymorphism | `isCompleted()`, `progressText()`, `needsMinutes()` behave differently per habit type |
| Method overriding | `StudyHabit.getBadge()` adds Scholar badges |
| Encapsulation | Private fields with getters in `Habit` |
| Exception handling | `SQLException`, `NumberFormatException` |
| Collections | `List`, `Set`, `HashSet<LocalDate>` |

```
        <<interface>> Trackable
                |
        abstract class Habit
           /            \
    DailyHabit      StudyHabit
```

---

## Database Design

**`habits`** - one row per habit

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER (PK) | Unique habit id |
| name | TEXT | Habit name |
| type | TEXT | `Daily` or `Study` |
| target_minutes | INTEGER | Daily goal for study habits |

**`logs`** - one row each time a habit is completed on a date

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER (PK) | Unique log id |
| habit_id | INTEGER | Links to `habits.id` |
| log_date | TEXT | Date in `YYYY-MM-DD` format |
| minutes | INTEGER | Minutes studied (0 for daily habits) |

`UNIQUE(habit_id, log_date)` prevents duplicate entries for the same day. Streaks are **calculated** from the dates in `logs`, not stored.

---

## How the Streak Works

- **Current streak:** consecutive days counted backwards from today. If today is still pending, counting starts from yesterday so the streak is not shown as broken.
- **Best streak:** the longest run of consecutive days found in the sorted dates.
- A **study habit** counts as completed only when the minutes logged that day reach the daily goal.

---

## How to Run

### Requirements
- JDK 17 or newer
- The SQLite driver jar (already included in this repo)

### Option 1: VS Code
1. Install the **Extension Pack for Java**.
2. Open this folder in VS Code (File, then Open Folder).
3. Open `Main.java`.
4. If the driver is not found, open **Java Projects, Referenced Libraries**, click **+** and add `sqlite-jdbc-3.36.0.3.jar`.
5. Click the **Run** link above `public static void main`.

### Option 2: Terminal

**Mac / Linux**
```bash
javac -cp ".:sqlite-jdbc-3.36.0.3.jar" *.java
java -cp ".:sqlite-jdbc-3.36.0.3.jar" Main
```

**Windows**
```bash
javac -cp ".;sqlite-jdbc-3.36.0.3.jar" *.java
java -cp ".;sqlite-jdbc-3.36.0.3.jar" Main
```

---

## How to Use

1. Click **Load Demo Data** once to see sample habits, streaks and the chart.
2. Type a name and click **+ Add Habit**. Choose **Study Habit** to set a daily goal in minutes.
3. Select a habit in the table and click **Complete Today**. Study habits ask for the minutes you studied.
4. Use **Undo Today** to reverse today's entry or **Delete Habit** to remove a habit.

To start fresh, close the app and delete `habits.db`. A new empty one is created on the next run.

---

## Future Improvements

- Login system for multiple users
- Export progress reports to CSV
- Reminder notifications using a `Thread`
- Monthly progress charts

---

## Author

**Keshav DHoopar**
B.Tech, Artificial Intelligence & Data Science, CGC Jhanjeri
