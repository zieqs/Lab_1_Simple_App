import java.util.ArrayList;
import java.util.List;

public class Category {
    private String name;
    private List<Task> tasks;

    // No-arg constructor for JSON deserialization
    public Category() {
        this.tasks = new ArrayList<>();
    }

    public Category(String name) {
        this.name = name;
        this.tasks = new ArrayList<>();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Task> getTasks() {
        if (tasks == null) {
            tasks = new ArrayList<>();
        }
        return tasks;
    }

    public void addTask(Task task) { tasks.add(task); }
    public void removeTask(Task task) { tasks.remove(task); }
}
