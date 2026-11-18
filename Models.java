/**
 * FILE 1 of 3 : Models.java  (the "Model" layer)
 * Shows the core OOP concepts: Interface, Abstract class, Inheritance,
 * Encapsulation, Polymorphism and Method overriding.
 */
public class Models {

    // ---------- INTERFACE ----------
    public interface Trackable {
        String getBadge(int streak);
    }

    // ---------- ABSTRACT CLASS ----------
    public static abstract class Habit implements Trackable {
        private final int id;          // Encapsulation: private fields
        private final String name;

        protected Habit(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId()      { return id; }
        public String getName() { return name; }

        // Each child class MUST implement these (polymorphism)
        public abstract String getType();
        public abstract boolean needsMinutes();
        public abstract boolean isCompleted(int minutesLogged);
        public abstract String progressText(int minutesToday, boolean done);

        // Default badge logic (child classes can override it)
        @Override
        public String getBadge(int streak) {
            if (streak >= 30) return "Legend";
            if (streak >= 14) return "Champion";
            if (streak >= 7)  return "On Fire";
            if (streak >= 3)  return "Rising";
            return "Starter";
        }
    }

    // ---------- INHERITANCE : DailyHabit ----------
    public static class DailyHabit extends Habit {
        public DailyHabit(int id, String name) {
            super(id, name);
        }

        @Override public String getType() { return "Daily Habit"; }
        @Override public boolean needsMinutes() { return false; }
        @Override public boolean isCompleted(int minutesLogged) { return true; }

        @Override
        public String progressText(int minutesToday, boolean done) {
            return done ? "Done" : "Pending";
        }
    }

    // ---------- INHERITANCE : StudyHabit ----------
    public static class StudyHabit extends Habit {
        private final int targetMinutes;

        public StudyHabit(int id, String name, int targetMinutes) {
            super(id, name);
            this.targetMinutes = targetMinutes;
        }

        public int getTargetMinutes() { return targetMinutes; }

        @Override public String getType() { return "Study Habit"; }
        @Override public boolean needsMinutes() { return true; }

        @Override
        public boolean isCompleted(int minutesLogged) {
            return minutesLogged >= targetMinutes;
        }

        @Override
        public String progressText(int minutesToday, boolean done) {
            return minutesToday + " / " + targetMinutes + " min";
        }

        // Method overriding: study habits get a special badge
        @Override
        public String getBadge(int streak) {
            if (streak >= 14) return "Scholar Pro";
            if (streak >= 7)  return "Scholar";
            return super.getBadge(streak);
        }
    }
}