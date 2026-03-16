import java.time.LocalDate;
import java.util.Random;

public class ExpSystem {
    private int exp;
    private int level;
    private int expToNextLevel;
    private int expPerTask;
    private int currentStreak;
    private int longestStreak;
    private LocalDate lastCompletedDate;
    private int lastExpGain;

    private static final String[] QUOTES = {
        "The only way to do great work is to love what you do. - Steve Jobs",
        "Believe you can and you're halfway there. - Theodore Roosevelt",
        "It does not matter how slowly you go as long as you do not stop. - Confucius",
        "Success is not final, failure is not fatal: it is the courage to continue that counts. - Winston Churchill",
        "The secret of getting ahead is getting started. - Mark Twain",
        "Don't watch the clock; do what it does. Keep going. - Sam Levenson",
        "Everything you've ever wanted is on the other side of fear. - George Addair",
        "Hardships often prepare ordinary people for an extraordinary destiny. - C.S. Lewis",
        "You are never too old to set another goal or to dream a new dream. - C.S. Lewis",
        "The future belongs to those who believe in the beauty of their dreams. - Eleanor Roosevelt"
    };

    public ExpSystem() {
        this.exp = 0;
        this.level = 1;
        this.expToNextLevel = 100;
        this.expPerTask = 25;
        this.currentStreak = 0;
        this.longestStreak = 0;
        this.lastCompletedDate = null;
        this.lastExpGain = 0;
    }

    public boolean addExp(int amount) {
        exp += Math.max(0, amount);
        boolean leveledUp = false;
        while (exp >= expToNextLevel) {
            level++;
            exp -= expToNextLevel;
            expToNextLevel = calculateNextLevelThreshold(expToNextLevel, level);
            leveledUp = true;
        }
        return leveledUp;
    }

    public boolean completeTask() {
        return completeTask(1);
    }

    public boolean completeTask(int priority) {
        updateStreak(LocalDate.now());
        int gainedExp = getExpGainForPriority(priority);
        lastExpGain = gainedExp;
        return addExp(gainedExp);
    }

    public void uncompleteTask() {
        uncompleteTask(1);
    }

    public void uncompleteTask(int priority) {
        int refundedExp = Math.max(expPerTask, getExpGainForPriority(priority) - getStreakBonus());
        exp = Math.max(0, exp - refundedExp);
        lastExpGain = -refundedExp;
    }

    private int calculateNextLevelThreshold(int currentThreshold, int newLevel) {
        int scaled = (int) Math.round(currentThreshold * 1.25);
        int flatBonus = 10 + (newLevel * 2);
        return Math.max(100, scaled + flatBonus);
    }

    private void updateStreak(LocalDate completionDate) {
        if (lastCompletedDate == null) {
            currentStreak = 1;
        } else if (completionDate.equals(lastCompletedDate)) {
            // Keep streak unchanged for multiple same-day completions.
        } else if (completionDate.equals(lastCompletedDate.plusDays(1))) {
            currentStreak++;
        } else {
            currentStreak = 1;
        }
        if (currentStreak > longestStreak) {
            longestStreak = currentStreak;
        }
        lastCompletedDate = completionDate;
    }

    private int getPriorityBonus(int priority) {
        int normalized = Math.max(1, Math.min(3, priority));
        if (normalized == 1) {
            return 0;
        }
        if (normalized == 2) {
            return 8;
        }
        return 16;
    }

    public int getStreakBonus() {
        return Math.min(20, Math.max(0, currentStreak - 1) * 2);
    }

    public int getExpGainForPriority(int priority) {
        return expPerTask + getPriorityBonus(priority) + getStreakBonus();
    }

    public int getExp() { return exp; }
    public void setExp(int exp) { this.exp = exp; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getExpToNextLevel() { return expToNextLevel; }
    public void setExpToNextLevel(int expToNextLevel) { this.expToNextLevel = expToNextLevel; }

    public int getExpPerTask() { return expPerTask; }
    public void setExpPerTask(int expPerTask) { this.expPerTask = Math.max(1, expPerTask); }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = Math.max(0, currentStreak); }

    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = Math.max(0, longestStreak); }

    public LocalDate getLastCompletedDate() { return lastCompletedDate; }
    public void setLastCompletedDate(LocalDate lastCompletedDate) { this.lastCompletedDate = lastCompletedDate; }

    public int getLastExpGain() { return lastExpGain; }

    public String getRandomQuote() {
        return QUOTES[new Random().nextInt(QUOTES.length)];
    }
}
