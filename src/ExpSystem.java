import java.util.Random;

public class ExpSystem {
    private int exp;
    private int level;
    private int expToNextLevel;
    private int expPerTask;

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
    }

    public boolean addExp(int amount) {
        exp += amount;
        if (exp >= expToNextLevel) {
            level++;
            exp -= expToNextLevel;
            expToNextLevel = (int) (expToNextLevel * 1.5);
            return true;
        }
        return false;
    }

    public boolean completeTask() {
        return addExp(expPerTask);
    }

    public void uncompleteTask() {
        exp = Math.max(0, exp - expPerTask);
    }

    public int getExp() { return exp; }
    public int getLevel() { return level; }
    public int getExpToNextLevel() { return expToNextLevel; }
    public int getExpPerTask() { return expPerTask; }
    public void setExpPerTask(int expPerTask) { this.expPerTask = Math.max(1, expPerTask); }

    public String getRandomQuote() {
        return QUOTES[new Random().nextInt(QUOTES.length)];
    }
}
