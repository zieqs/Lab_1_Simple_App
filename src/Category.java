import java.util.ArrayList;
import java.util.List;

public class Category {
    private int id;
    private String name;
    private List<Task> tasks;

    public Category(String name) {
        this.id = -1;
        this.name = name;
        this.tasks = new ArrayList<>();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Task> getTasks() { return tasks; }

    public void addTask(Task task) { tasks.add(task); }
    public void removeTask(Task task) { tasks.remove(task); }
}
