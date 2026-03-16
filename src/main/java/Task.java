public class Task {
    private int id;
    private String name;
    private boolean completed;
    private int priority; // 1 = !, 2 = !!, 3 = !!!

    public Task(String name, int priority) {
        this.id = -1;
        this.name = name;
        this.priority = Math.max(1, Math.min(3, priority));
        this.completed = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = Math.max(1, Math.min(3, priority)); }

    public String getPriorityLabel() {
        switch (priority) {
            case 1: return "!";
            case 2: return "!!";
            case 3: return "!!!";
            default: return "!";
        }
    }
}
