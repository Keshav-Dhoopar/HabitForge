import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * FILE 2 of 3 : Database.java  (the "Backend" layer)
 * Uses JDBC + SQLite. Handles all data storage and the streak logic.
 * The database file "habits.db" is created automatically on first run.
 */
public class Database {

    private final Connection conn;

    public Database() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite driver not found. Put sqlite-jdbc jar inside the lib folder.");
        }
        conn = DriverManager.getConnection("jdbc:sqlite:habits.db");
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS habits ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT NOT NULL, "
                    + "type TEXT NOT NULL, "
                    + "target_minutes INTEGER DEFAULT 0)");
            st.execute("CREATE TABLE IF NOT EXISTS logs ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "habit_id INTEGER NOT NULL, "
                    + "log_date TEXT NOT NULL, "
                    + "minutes INTEGER DEFAULT 0, "
                    + "UNIQUE(habit_id, log_date))");
        }
    }

    // ------------------------------------------------------------------
    //  CRUD operations
    // ------------------------------------------------------------------
    public int addHabit(String name, String type, int targetMinutes) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO habits(name, type, target_minutes) VALUES(?,?,?)")) {
            ps.setString(1, name);
            ps.setString(2, type);
            ps.setInt(3, targetMinutes);
            ps.executeUpdate();
        }
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public List<Models.Habit> getAllHabits() throws SQLException {
        List<Models.Habit> list = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name, type, target_minutes FROM habits ORDER BY id")) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                if ("Study".equals(rs.getString("type"))) {
                    list.add(new Models.StudyHabit(id, name, rs.getInt("target_minutes")));
                } else {
                    list.add(new Models.DailyHabit(id, name));
                }
            }
        }
        return list;
    }

    public void deleteHabit(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM logs WHERE habit_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM habits WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // Daily habit: just mark the date as done
    public void markDone(int habitId, LocalDate date) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO logs(habit_id, log_date, minutes) VALUES(?,?,0)")) {
            ps.setInt(1, habitId);
            ps.setString(2, date.toString());
            ps.executeUpdate();
        }
    }

    // Study habit: add minutes to that date (keeps adding if logged again)
    public void addMinutes(int habitId, LocalDate date, int minutes) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO logs(habit_id, log_date, minutes) VALUES(?,?,?) "
                + "ON CONFLICT(habit_id, log_date) DO UPDATE SET minutes = minutes + excluded.minutes")) {
            ps.setInt(1, habitId);
            ps.setString(2, date.toString());
            ps.setInt(3, minutes);
            ps.executeUpdate();
        }
    }

    public void undo(int habitId, LocalDate date) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM logs WHERE habit_id=? AND log_date=?")) {
            ps.setInt(1, habitId);
            ps.setString(2, date.toString());
            ps.executeUpdate();
        }
    }

    public int getMinutes(int habitId, LocalDate date) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT minutes FROM logs WHERE habit_id=? AND log_date=?")) {
            ps.setInt(1, habitId);
            ps.setString(2, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // All dates on which this habit was "completed" (uses polymorphism: h.isCompleted)
    public Set<LocalDate> getCompletedDates(Models.Habit h) throws SQLException {
        Set<LocalDate> dates = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT log_date, minutes FROM logs WHERE habit_id=?")) {
            ps.setInt(1, h.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (h.isCompleted(rs.getInt("minutes"))) {
                        dates.add(LocalDate.parse(rs.getString("log_date")));
                    }
                }
            }
        }
        return dates;
    }

    public int getTotalStudyMinutes() throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COALESCE(SUM(l.minutes),0) FROM logs l "
                     + "JOIN habits h ON h.id = l.habit_id WHERE h.type='Study'")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // ------------------------------------------------------------------
    //  STREAK LOGIC
    // ------------------------------------------------------------------

    // Current streak = consecutive days ending today (or yesterday if today is still pending)
    public static int currentStreak(Set<LocalDate> dates) {
        LocalDate day = LocalDate.now();
        if (!dates.contains(day)) {
            day = day.minusDays(1);
        }
        int streak = 0;
        while (dates.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }

    // Best streak = longest run of consecutive days ever
    public static int bestStreak(Set<LocalDate> dates) {
        List<LocalDate> sorted = new ArrayList<>(dates);
        Collections.sort(sorted);
        int best = 0, run = 0;
        LocalDate prev = null;
        for (LocalDate d : sorted) {
            run = (prev != null && prev.plusDays(1).equals(d)) ? run + 1 : 1;
            best = Math.max(best, run);
            prev = d;
        }
        return best;
    }

    // ------------------------------------------------------------------
    //  DEMO DATA (handy for the practical presentation)
    // ------------------------------------------------------------------
    public void loadDemoData() throws SQLException {
        String[] names = {"Morning Workout", "Read 20 Pages", "DSA Practice", "Java Revision"};
        String[] types = {"Daily", "Daily", "Study", "Study"};
        int[] targets  = {0, 0, 60, 45};
        int[] streaks  = {9, 5, 12, 3};
        Random rnd = new Random(42);

        for (int i = 0; i < names.length; i++) {
            int id = addHabit(names[i], types[i], targets[i]);
            for (int d = 1; d <= 14; d++) {
                boolean log = d <= streaks[i] || (d != streaks[i] + 1 && rnd.nextInt(100) > 40);
                if (!log) continue;
                LocalDate date = LocalDate.now().minusDays(d);
                if (types[i].equals("Study")) {
                    addMinutes(id, date, targets[i] + rnd.nextInt(30));
                } else {
                    markDone(id, date);
                }
            }
        }
    }
}