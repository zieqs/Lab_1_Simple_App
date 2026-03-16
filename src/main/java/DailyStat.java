import java.time.LocalDate;

public class DailyStat {
    private final LocalDate day;
    private final int completedCount;
    private final int createdCount;

    public DailyStat(LocalDate day, int completedCount, int createdCount) {
        this.day = day;
        this.completedCount = Math.max(0, completedCount);
        this.createdCount = Math.max(0, createdCount);
    }

    public LocalDate getDay() {
        return day;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public int getCreatedCount() {
        return createdCount;
    }
}
