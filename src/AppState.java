import java.util.ArrayList;
import java.util.List;

public class AppState {
    private List<Category> categories;
    private String selectedCategoryName;
    private int exp;
    private int level;
    private int expToNextLevel;
    private int expPerTask;

    public AppState() {
        // Needed for deserialization
    }

    public AppState(List<Category> categories, String selectedCategoryName, int exp, int level, int expToNextLevel,
            int expPerTask) {
        // Store shallow copies to avoid accidental mutation during serialization
        this.categories = new ArrayList<>(categories);
        this.selectedCategoryName = selectedCategoryName;
        this.exp = exp;
        this.level = level;
        this.expToNextLevel = expToNextLevel;
        this.expPerTask = expPerTask;
    }

    public List<Category> getCategories() { return categories == null ? new ArrayList<>() : categories; }
    public String getSelectedCategoryName() { return selectedCategoryName; }
    public int getExp() { return exp; }
    public int getLevel() { return level; }
    public int getExpToNextLevel() { return expToNextLevel; }
    public int getExpPerTask() { return expPerTask; }
}
