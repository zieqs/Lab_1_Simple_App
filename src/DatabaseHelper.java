import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
                + "FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE)";

        String expTable = "CREATE TABLE IF NOT EXISTS exp_system ("
                + "id INTEGER PRIMARY KEY CHECK (id = 1),"
                + "exp INTEGER DEFAULT 0,"
                + "level INTEGER DEFAULT 1,"
                + "exp_to_next_level INTEGER DEFAULT 100,"
                + "exp_per_task INTEGER DEFAULT 25)";

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(categoriesTable);
            stmt.execute(tasksTable);
            stmt.execute(expTable);
            // Insert default exp row if not exists
            stmt.execute("INSERT OR IGNORE INTO exp_system (id, exp, level, exp_to_next_level, exp_per_task) VALUES (1, 0, 1, 100, 25)");
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
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
        String sql = "INSERT INTO tasks (category_id, name, priority, completed) VALUES (?, ?, ?, 0)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, categoryId);
            pstmt.setString(2, name);
            pstmt.setInt(3, priority);
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
        String sql = "SELECT id, name, priority, completed FROM tasks WHERE category_id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, categoryId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Task task = new Task(rs.getString("name"), rs.getInt("priority"));
                task.setId(rs.getInt("id"));
                task.setCompleted(rs.getInt("completed") == 1);
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
        String sql = "SELECT exp, level, exp_to_next_level, exp_per_task FROM exp_system WHERE id = 1";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                ExpSystem sys = new ExpSystem();
                sys.setExp(rs.getInt("exp"));
                sys.setLevel(rs.getInt("level"));
                sys.setExpToNextLevel(rs.getInt("exp_to_next_level"));
                sys.setExpPerTask(rs.getInt("exp_per_task"));
                return sys;
            }
        } catch (SQLException e) {
            System.err.println("Error loading exp system: " + e.getMessage());
        }
        return new ExpSystem();
    }

    public void saveExpSystem(ExpSystem sys) {
        String sql = "UPDATE exp_system SET exp = ?, level = ?, exp_to_next_level = ?, exp_per_task = ? WHERE id = 1";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sys.getExp());
            pstmt.setInt(2, sys.getLevel());
            pstmt.setInt(3, sys.getExpToNextLevel());
            pstmt.setInt(4, sys.getExpPerTask());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving exp system: " + e.getMessage());
        }
    }
}
