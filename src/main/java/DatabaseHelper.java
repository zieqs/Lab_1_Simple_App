import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper {
    private static final String DB_URL = "jdbc:sqlite:noteapp.db";

    public DatabaseHelper() {
        createTables();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private void createTables() {
        String categoriesTable = "CREATE TABLE IF NOT EXISTS categories ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL)";

        String tasksTable = "CREATE TABLE IF NOT EXISTS tasks ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "category_id INTEGER NOT NULL,"
                + "name TEXT NOT NULL,"
                + "priority INTEGER DEFAULT 1,"
                + "completed INTEGER DEFAULT 0,"
                + "due_date TEXT,"
                + "FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE)";

        String expTable = "CREATE TABLE IF NOT EXISTS exp_system ("
                + "id INTEGER PRIMARY KEY CHECK (id = 1),"
                + "exp INTEGER DEFAULT 0,"
                + "level INTEGER DEFAULT 1,"
                + "exp_to_next_level INTEGER DEFAULT 100,"
            + "exp_per_task INTEGER DEFAULT 25,"
            + "current_streak INTEGER DEFAULT 0,"
            + "longest_streak INTEGER DEFAULT 0,"
            + "last_completed_date TEXT)";

        String historyTable = "CREATE TABLE IF NOT EXISTS task_history ("
            + "day TEXT PRIMARY KEY,"
            + "completed_count INTEGER DEFAULT 0,"
            + "created_count INTEGER DEFAULT 0)";

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(categoriesTable);
            stmt.execute(tasksTable);
            stmt.execute(expTable);
            stmt.execute(historyTable);
            ensureTasksDueDateColumn(conn);
            ensureExpSystemColumns(conn);
            // Insert default exp row if not exists
            stmt.execute("INSERT OR IGNORE INTO exp_system (id, exp, level, exp_to_next_level, exp_per_task, current_streak, longest_streak, last_completed_date) VALUES (1, 0, 1, 100, 25, 0, 0, NULL)");
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
        }
    }

    private void ensureTasksDueDateColumn(Connection conn) {
        String checkSql = "PRAGMA table_info(tasks)";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(checkSql)) {
            boolean hasDueDate = false;
            while (rs.next()) {
                if ("due_date".equalsIgnoreCase(rs.getString("name"))) {
                    hasDueDate = true;
                    break;
                }
            }
            if (!hasDueDate) {
                stmt.execute("ALTER TABLE tasks ADD COLUMN due_date TEXT");
            }
        } catch (SQLException e) {
            System.err.println("Error ensuring due_date column: " + e.getMessage());
        }
    }

    private void ensureExpSystemColumns(Connection conn) {
        String checkSql = "PRAGMA table_info(exp_system)";
        boolean hasCurrentStreak = false;
        boolean hasLongestStreak = false;
        boolean hasLastCompletedDate = false;
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(checkSql)) {
            while (rs.next()) {
                String col = rs.getString("name");
                if ("current_streak".equalsIgnoreCase(col)) {
                    hasCurrentStreak = true;
                } else if ("longest_streak".equalsIgnoreCase(col)) {
                    hasLongestStreak = true;
                } else if ("last_completed_date".equalsIgnoreCase(col)) {
                    hasLastCompletedDate = true;
                }
            }
            if (!hasCurrentStreak) {
                stmt.execute("ALTER TABLE exp_system ADD COLUMN current_streak INTEGER DEFAULT 0");
            }
            if (!hasLongestStreak) {
                stmt.execute("ALTER TABLE exp_system ADD COLUMN longest_streak INTEGER DEFAULT 0");
            }
            if (!hasLastCompletedDate) {
                stmt.execute("ALTER TABLE exp_system ADD COLUMN last_completed_date TEXT");
            }
        } catch (SQLException e) {
            System.err.println("Error ensuring exp_system columns: " + e.getMessage());
        }
    }

    // ==================== CATEGORY CRUD ====================

    public int insertCategory(String name) {
        String sql = "INSERT INTO categories (name) VALUES (?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error inserting category: " + e.getMessage());
        }
        return -1;
    }

    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT id, name FROM categories";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Category cat = new Category(rs.getString("name"));
                cat.setId(rs.getInt("id"));
                // Load tasks for this category
                for (Task task : getTasksByCategory(cat.getId())) {
                    cat.addTask(task);
                }
                categories.add(cat);
            }
        } catch (SQLException e) {
            System.err.println("Error loading categories: " + e.getMessage());
        }
        return categories;
    }

    public void updateCategory(int id, String name) {
        String sql = "UPDATE categories SET name = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating category: " + e.getMessage());
        }
    }

    public void deleteCategory(int id) {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM tasks WHERE category_id = " + id);
            stmt.execute("DELETE FROM categories WHERE id = " + id);
        } catch (SQLException e) {
            System.err.println("Error deleting category: " + e.getMessage());
        }
    }

    // ==================== TASK CRUD ====================

    public int insertTask(int categoryId, String name, int priority) {
        return insertTask(categoryId, name, priority, null);
    }

    public int insertTask(int categoryId, String name, int priority, LocalDate dueDate) {
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(
                     dueDate == null
                             ? "INSERT INTO tasks (category_id, name, priority, completed) VALUES (?, ?, ?, 0)"
                             : "INSERT INTO tasks (category_id, name, priority, completed, due_date) VALUES (?, ?, ?, 0, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, categoryId);
            pstmt.setString(2, name);
            pstmt.setInt(3, priority);
            if (dueDate != null) {
                pstmt.setString(4, dueDate.toString());
            }
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error inserting task: " + e.getMessage());
        }
        return -1;
    }

    public List<Task> getTasksByCategory(int categoryId) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT id, name, priority, completed, due_date FROM tasks WHERE category_id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, categoryId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Task task = new Task(rs.getString("name"), rs.getInt("priority"));
                task.setId(rs.getInt("id"));
                task.setCompleted(rs.getInt("completed") == 1);
                String dueDate = rs.getString("due_date");
                if (dueDate != null && !dueDate.isBlank()) {
                    task.setDueDate(LocalDate.parse(dueDate));
                }
                tasks.add(task);
            }
        } catch (SQLException e) {
            System.err.println("Error loading tasks: " + e.getMessage());
        }
        return tasks;
    }

    public void updateTask(int id, String name, int priority, boolean completed) {
        String sql = "UPDATE tasks SET name = ?, priority = ?, completed = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, priority);
            pstmt.setInt(3, completed ? 1 : 0);
            pstmt.setInt(4, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating task: " + e.getMessage());
        }
    }

    public void updateTask(int id, String name, int priority, boolean completed, LocalDate dueDate) {
        String sql = "UPDATE tasks SET name = ?, priority = ?, completed = ?, due_date = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, priority);
            pstmt.setInt(3, completed ? 1 : 0);
            if (dueDate != null) {
                pstmt.setString(4, dueDate.toString());
            } else {
                pstmt.setNull(4, Types.VARCHAR);
            }
            pstmt.setInt(5, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating task: " + e.getMessage());
        }
    }

    public void deleteTask(int id) {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting task: " + e.getMessage());
        }
    }

    // ==================== EXP SYSTEM ====================

    public ExpSystem loadExpSystem() {
        String sql = "SELECT exp, level, exp_to_next_level, exp_per_task, current_streak, longest_streak, last_completed_date FROM exp_system WHERE id = 1";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                ExpSystem sys = new ExpSystem();
                sys.setExp(rs.getInt("exp"));
                sys.setLevel(rs.getInt("level"));
                sys.setExpToNextLevel(rs.getInt("exp_to_next_level"));
                sys.setExpPerTask(rs.getInt("exp_per_task"));
                sys.setCurrentStreak(rs.getInt("current_streak"));
                sys.setLongestStreak(rs.getInt("longest_streak"));
                String lastCompletedDate = rs.getString("last_completed_date");
                if (lastCompletedDate != null && !lastCompletedDate.isBlank()) {
                    sys.setLastCompletedDate(LocalDate.parse(lastCompletedDate));
                }
                return sys;
            }
        } catch (SQLException e) {
            System.err.println("Error loading exp system: " + e.getMessage());
        }
        return new ExpSystem();
    }

    public void saveExpSystem(ExpSystem sys) {
        String sql = "UPDATE exp_system SET exp = ?, level = ?, exp_to_next_level = ?, exp_per_task = ?, current_streak = ?, longest_streak = ?, last_completed_date = ? WHERE id = 1";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sys.getExp());
            pstmt.setInt(2, sys.getLevel());
            pstmt.setInt(3, sys.getExpToNextLevel());
            pstmt.setInt(4, sys.getExpPerTask());
            pstmt.setInt(5, sys.getCurrentStreak());
            pstmt.setInt(6, sys.getLongestStreak());
            if (sys.getLastCompletedDate() != null) {
                pstmt.setString(7, sys.getLastCompletedDate().toString());
            } else {
                pstmt.setNull(7, Types.VARCHAR);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving exp system: " + e.getMessage());
        }
    }

    // ==================== HISTORY ====================

    public void recordTaskCreated(LocalDate day) {
        upsertHistoryDelta(day, 0, 1);
    }

    public void recordTaskCompleted(LocalDate day) {
        upsertHistoryDelta(day, 1, 0);
    }

    public void recordTaskUncompleted(LocalDate day) {
        upsertHistoryDelta(day, -1, 0);
    }

    private void upsertHistoryDelta(LocalDate day, int completedDelta, int createdDelta) {
        String insertSql = "INSERT OR IGNORE INTO task_history (day, completed_count, created_count) VALUES (?, 0, 0)";
        String updateSql = "UPDATE task_history SET completed_count = MAX(0, completed_count + ?), created_count = MAX(0, created_count + ?) WHERE day = ?";
        try (Connection conn = connect()) {
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, day.toString());
                insertStmt.executeUpdate();
            }
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setInt(1, completedDelta);
                updateStmt.setInt(2, createdDelta);
                updateStmt.setString(3, day.toString());
                updateStmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error updating task history: " + e.getMessage());
        }
    }

    public List<DailyStat> getDailyStats(int days) {
        List<DailyStat> stats = new ArrayList<>();
        if (days <= 0) {
            return stats;
        }

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);

        Map<LocalDate, DailyStat> statMap = new HashMap<>();
        String sql = "SELECT day, completed_count, created_count FROM task_history WHERE day >= ? AND day <= ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, start.toString());
            pstmt.setString(2, end.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                LocalDate day = LocalDate.parse(rs.getString("day"));
                int completed = Math.max(0, rs.getInt("completed_count"));
                int created = Math.max(0, rs.getInt("created_count"));
                statMap.put(day, new DailyStat(day, completed, created));
            }
        } catch (SQLException e) {
            System.err.println("Error loading task history: " + e.getMessage());
        }

        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            DailyStat stat = statMap.get(cursor);
            if (stat == null) {
                stats.add(new DailyStat(cursor, 0, 0));
            } else {
                stats.add(stat);
            }
            cursor = cursor.plusDays(1);
        }
        return stats;
    }
}
