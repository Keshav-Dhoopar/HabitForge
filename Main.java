import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.table.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * FILE 3 of 3 : Main.java  (the "Frontend" layer)
 * Swing GUI: dashboard cards, habit table with streaks, weekly chart,
 * progress bar and action buttons.  Run this file to start the app.
 */
public class Main extends JFrame {

    // ---------- Theme colours ----------
    static final Color BG     = new Color(15, 23, 42);
    static final Color CARD   = new Color(30, 41, 59);
    static final Color FIELD  = new Color(51, 65, 85);
    static final Color ACCENT = new Color(34, 211, 238);
    static final Color PURPLE = new Color(139, 92, 246);
    static final Color GREEN  = new Color(34, 197, 94);
    static final Color ORANGE = new Color(249, 115, 22);
    static final Color RED    = new Color(239, 68, 68);
    static final Color TEXT   = new Color(226, 232, 240);
    static final Color MUTED  = new Color(148, 163, 184);

    private static final String[] QUOTES = {
        "Small steps every day beat big plans someday.",
        "Don't break the chain - keep your streak alive!",
        "Discipline is choosing what you want most over what you want now.",
        "Consistency is what turns average into excellent.",
        "Future you is watching. Make them proud."
    };

    private final Database db;
    private List<Models.Habit> habits = new ArrayList<>();
    private final List<Boolean> doneToday = new ArrayList<>();

    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel totalLbl, doneLbl, bestLbl, minutesLbl, quoteLbl;
    private JProgressBar todayBar;
    private ChartPanel chart;
    private JTextField nameField;
    private JComboBox<String> typeBox;
    private JSpinner targetSpin;

    // =================================================================
    //  Constructor : builds the whole window
    // =================================================================
    public Main(Database db) {
        this.db = db;
        setTitle("HabitForge - Habit & Study Tracker");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        setContentPane(root);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildCenter(), BorderLayout.CENTER);
        root.add(buildBottom(), BorderLayout.SOUTH);

        refresh();
    }

    // =================================================================
    //  UI building
    // =================================================================
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("HabitForge");
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(ACCENT);

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy"));
        JLabel sub = new JLabel("Habit & Study Tracker   |   " + date);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(MUTED);

        quoteLbl = new JLabel(" ");
        quoteLbl.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        quoteLbl.setForeground(PURPLE.brighter());

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(title);
        left.add(sub);
        left.add(Box.createVerticalStrut(4));
        left.add(quoteLbl);

        JButton demo = new RoundedButton("Load Demo Data", PURPLE);
        demo.addActionListener(e -> {
            try {
                db.loadDemoData();
                refresh();
            } catch (SQLException ex) {
                showError(ex);
            }
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));
        right.setOpaque(false);
        right.add(demo);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);

        // ---- stat cards ----
        totalLbl   = new JLabel("0");
        doneLbl    = new JLabel("0/0");
        bestLbl    = new JLabel("0 days");
        minutesLbl = new JLabel("0 min");

        JPanel stats = new JPanel(new GridLayout(1, 4, 14, 0));
        stats.setOpaque(false);
        stats.add(statCard("TOTAL HABITS", totalLbl, ACCENT));
        stats.add(statCard("DONE TODAY", doneLbl, GREEN));
        stats.add(statCard("BEST STREAK", bestLbl, ORANGE));
        stats.add(statCard("STUDY TIME", minutesLbl, PURPLE));
        center.add(stats, BorderLayout.NORTH);

        // ---- habit table ----
        String[] cols = {"Habit", "Type", "Today", "Streak", "Best", "Badge"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        styleTable();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD);
        scroll.setOpaque(false);

        RoundedPanel tableCard = new RoundedPanel(new BorderLayout(), CARD);
        tableCard.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tableCard.add(scroll, BorderLayout.CENTER);

        // ---- right column: today's progress + chart ----
        JLabel pTitle = new JLabel("Today's Progress");
        pTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        pTitle.setForeground(TEXT);

        todayBar = new JProgressBar(0, 100);
        todayBar.setStringPainted(true);
        todayBar.setBorderPainted(false);
        todayBar.setBackground(FIELD);
        todayBar.setForeground(GREEN);
        todayBar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        todayBar.setPreferredSize(new Dimension(100, 26));
        todayBar.setUI(new BasicProgressBarUI() {
            @Override protected Color getSelectionBackground() { return TEXT; }
            @Override protected Color getSelectionForeground() { return Color.WHITE; }
        });

        RoundedPanel progressCard = new RoundedPanel(new BorderLayout(0, 10), CARD);
        progressCard.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        progressCard.add(pTitle, BorderLayout.NORTH);
        progressCard.add(todayBar, BorderLayout.CENTER);

        chart = new ChartPanel();

        JPanel rightCol = new JPanel(new BorderLayout(0, 16));
        rightCol.setOpaque(false);
        rightCol.setPreferredSize(new Dimension(340, 100));
        rightCol.add(progressCard, BorderLayout.NORTH);
        rightCol.add(chart, BorderLayout.CENTER);

        JPanel middle = new JPanel(new BorderLayout(16, 0));
        middle.setOpaque(false);
        middle.add(tableCard, BorderLayout.CENTER);
        middle.add(rightCol, BorderLayout.EAST);
        center.add(middle, BorderLayout.CENTER);

        return center;
    }

    private JPanel buildBottom() {
        // ---- row 1 : add new habit ----
        nameField = new JTextField(16);
        typeBox = new JComboBox<>(new String[]{"Daily Habit", "Study Habit"});
        targetSpin = new JSpinner(new SpinnerNumberModel(60, 5, 600, 5));
        targetSpin.setEnabled(false);
        typeBox.addActionListener(e -> targetSpin.setEnabled(typeBox.getSelectedIndex() == 1));

        styleInput(nameField);
        styleInput(typeBox);
        styleInput(targetSpin);

        JButton addBtn = new RoundedButton("+ Add Habit", ACCENT.darker());
        addBtn.addActionListener(e -> addHabit());

        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        addRow.setOpaque(false);
        addRow.add(label("New habit:"));
        addRow.add(nameField);
        addRow.add(typeBox);
        addRow.add(label("Daily goal (min):"));
        addRow.add(targetSpin);
        addRow.add(addBtn);

        // ---- row 2 : actions on selected habit ----
        JButton doneBtn = new RoundedButton("Complete Today", GREEN.darker());
        JButton undoBtn = new RoundedButton("Undo Today", ORANGE.darker());
        JButton delBtn  = new RoundedButton("Delete Habit", RED.darker());
        doneBtn.addActionListener(e -> completeToday());
        undoBtn.addActionListener(e -> undoToday());
        delBtn.addActionListener(e -> deleteSelected());

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionRow.setOpaque(false);
        actionRow.add(label("Selected habit:"));
        actionRow.add(doneBtn);
        actionRow.add(undoBtn);
        actionRow.add(delBtn);

        RoundedPanel bottom = new RoundedPanel(new GridLayout(2, 1, 0, 12), CARD);
        bottom.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        bottom.add(addRow);
        bottom.add(actionRow);
        return bottom;
    }

    private JPanel statCard(String title, JLabel value, Color color) {
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.setForeground(MUTED);

        value.setFont(new Font("Segoe UI", Font.BOLD, 28));
        value.setForeground(color);

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 6), CARD);
        card.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        card.add(t, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        return l;
    }

    private void styleInput(JComponent c) {
        c.setBackground(FIELD);
        c.setForeground(TEXT);
        c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        if (c instanceof JTextField) {
            ((JTextField) c).setCaretColor(TEXT);
        }
    }

    private void styleTable() {
        table.setBackground(CARD);
        table.setForeground(TEXT);
        table.setRowHeight(40);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setSelectionBackground(new Color(51, 65, 85));
        table.setSelectionForeground(Color.WHITE);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);

        // header
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(100, 36));
        header.setDefaultRenderer((tbl, val, sel, foc, row, col) -> {
            JLabel l = new JLabel(String.valueOf(val));
            l.setOpaque(true);
            l.setBackground(BG);
            l.setForeground(ACCENT);
            l.setFont(new Font("Segoe UI", Font.BOLD, 13));
            l.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            l.setHorizontalAlignment(col >= 2 ? SwingConstants.CENTER : SwingConstants.LEFT);
            return l;
        });

        // cells
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, false, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                setBackground(sel ? t.getSelectionBackground() : CARD);
                setForeground(TEXT);
                setFont(new Font("Segoe UI", Font.PLAIN, 14));
                setHorizontalAlignment(col >= 2 ? SwingConstants.CENTER : SwingConstants.LEFT);

                boolean done = row < doneToday.size() && doneToday.get(row);
                if (col == 2) {
                    setForeground(done ? GREEN : ORANGE);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if (col == 3) {
                    setForeground(ORANGE);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if (col == 5) {
                    setForeground(PURPLE.brighter());
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if (col == 1) {
                    setForeground(MUTED);
                }
                return this;
            }
        });

        int[] widths = {230, 100, 120, 90, 90, 110};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    // =================================================================
    //  Actions (button logic)
    // =================================================================
    private void addHabit() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a habit name.",
                    "Missing name", JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean study = typeBox.getSelectedIndex() == 1;
        try {
            db.addHabit(name, study ? "Study" : "Daily", study ? (int) targetSpin.getValue() : 0);
            nameField.setText("");
            refresh();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private Models.Habit selectedHabit() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= habits.size()) {
            JOptionPane.showMessageDialog(this, "Select a habit from the table first.",
                    "No selection", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return habits.get(row);
    }

    private void completeToday() {
        Models.Habit h = selectedHabit();
        if (h == null) return;
        try {
            if (h.needsMinutes()) {                      // polymorphism in action
                String in = JOptionPane.showInputDialog(this,
                        "Minutes studied for \"" + h.getName() + "\":", "30");
                if (in == null) return;
                int minutes = Integer.parseInt(in.trim());
                if (minutes <= 0) throw new NumberFormatException();
                db.addMinutes(h.getId(), LocalDate.now(), minutes);
            } else {
                db.markDone(h.getId(), LocalDate.now());
            }
            refresh();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number of minutes.",
                    "Invalid input", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void undoToday() {
        Models.Habit h = selectedHabit();
        if (h == null) return;
        try {
            db.undo(h.getId(), LocalDate.now());
            refresh();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void deleteSelected() {
        Models.Habit h = selectedHabit();
        if (h == null) return;
        int ok = JOptionPane.showConfirmDialog(this,
                "Delete \"" + h.getName() + "\" and all its history?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            db.deleteHabit(h.getId());
            refresh();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    // =================================================================
    //  Refresh : reload everything from the database
    // =================================================================
    private void refresh() {
        try {
            habits = db.getAllHabits();
            tableModel.setRowCount(0);
            doneToday.clear();

            LocalDate today = LocalDate.now();
            List<Set<LocalDate>> allDates = new ArrayList<>();
            int done = 0, bestOverall = 0;

            for (Models.Habit h : habits) {
                Set<LocalDate> dates = db.getCompletedDates(h);
                allDates.add(dates);

                int current = Database.currentStreak(dates);
                int best = Database.bestStreak(dates);
                boolean isDone = dates.contains(today);
                if (isDone) done++;
                bestOverall = Math.max(bestOverall, best);
                doneToday.add(isDone);

                String todayText = h.progressText(db.getMinutes(h.getId(), today), isDone);
                tableModel.addRow(new Object[]{
                        h.getName(), h.getType(), todayText,
                        current + " days", best + " days", h.getBadge(current)});
            }

            int total = habits.size();
            totalLbl.setText(String.valueOf(total));
            doneLbl.setText(done + "/" + total);
            bestLbl.setText(bestOverall + " days");
            minutesLbl.setText(db.getTotalStudyMinutes() + " min");

            int pct = total == 0 ? 0 : done * 100 / total;
            todayBar.setValue(pct);
            todayBar.setString(pct + "%  completed");

            // weekly chart data
            String[] labels = new String[7];
            int[] values = new int[7];
            for (int i = 0; i < 7; i++) {
                LocalDate day = today.minusDays(6 - i);
                labels[i] = day.format(DateTimeFormatter.ofPattern("EEE"));
                int count = 0;
                for (Set<LocalDate> s : allDates) {
                    if (s.contains(day)) count++;
                }
                values[i] = total == 0 ? 0 : count * 100 / total;
            }
            chart.setData(labels, values);

            quoteLbl.setText("\"" + QUOTES[new Random().nextInt(QUOTES.length)] + "\"");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    // =================================================================
    //  Custom components
    // =================================================================
    static class RoundedPanel extends JPanel {
        private final Color color;

        RoundedPanel(LayoutManager layout, Color color) {
            super(layout);
            this.color = color;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class RoundedButton extends JButton {
        private final Color base;

        RoundedButton(String text, Color base) {
            super(text);
            this.base = base;
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setRolloverEnabled(true);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getModel().isRollover() ? base.brighter() : base);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Bar chart drawn manually with Graphics2D (last 7 days completion %)
    static class ChartPanel extends JPanel {
        private String[] labels = {"", "", "", "", "", "", ""};
        private int[] values = new int[7];

        ChartPanel() {
            setOpaque(false);
        }

        void setData(String[] labels, int[] values) {
            this.labels = labels;
            this.values = values;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            g2.setColor(CARD);
            g2.fillRoundRect(0, 0, w, h, 20, 20);

            g2.setColor(TEXT);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.drawString("Last 7 Days (% completed)", 18, 28);

            int left = 18, right = 18, top = 50, bottom = 34;
            int chartH = h - top - bottom;
            int n = values.length;
            int slot = (w - left - right) / n;
            int barW = slot - 14;

            Font small = new Font("Segoe UI", Font.PLAIN, 12);
            g2.setFont(small);
            FontMetrics fm = g2.getFontMetrics();

            for (int i = 0; i < n; i++) {
                int x = left + i * slot + 7;
                int barH = values[i] * chartH / 100;
                int y = top + chartH - barH;

                g2.setColor(FIELD);                                   // background track
                g2.fillRoundRect(x, top, barW, chartH, 10, 10);

                if (barH > 0) {                                       // coloured bar
                    g2.setPaint(new GradientPaint(x, y, ACCENT, x, y + barH, PURPLE));
                    g2.fillRoundRect(x, y, barW, barH, 10, 10);
                }

                g2.setColor(TEXT);                                    // % text
                String pct = values[i] + "%";
                int ty = Math.max(y - 5, top + 14);
                g2.drawString(pct, x + (barW - fm.stringWidth(pct)) / 2, ty);

                g2.setColor(MUTED);                                   // day label
                g2.drawString(labels[i], x + (barW - fm.stringWidth(labels[i])) / 2, h - 12);
            }
            g2.dispose();
        }
    }

    // =================================================================
    //  Program entry point
    // =================================================================
   public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) { }

            try {
                Database db = new Database();
                new Main(db).setVisible(true);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(),
                        "Database error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}