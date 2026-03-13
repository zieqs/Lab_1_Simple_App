public class Task {
    private String name;
    private boolean completed;
    private int priority; // 1 = !, 2 = !!, 3 = !!!

    // No-arg constructor for JSON deserialization
    public Task() {
        this.completed = false;
        this.priority = 1;
    }

    public Task(String name, int priority) {
        this.name = name;
        this.priority = Math.max(1, Math.min(3, priority));
        this.completed = false;
    }

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
