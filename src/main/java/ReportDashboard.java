import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ReportDashboard {
    private ReportDashboard() {
    }

    public static void show(List<Category> categories, ExpSystem expSystem, DatabaseHelper db) {
        ReportMetrics metrics = calculateReportMetrics(categories);

        Stage reportStage = new Stage();
        reportStage.setTitle("Reports");

        BorderPane reportRoot = new BorderPane();
        reportRoot.setStyle("-fx-background-color: #f7f9fb;");

        Label header = new Label("Analytics Dashboard");
        header.setFont(Font.font("System", FontWeight.BOLD, 20));
        Label subHeader = new Label(
            "Completion: " + String.format("%.1f%%", metrics.completionRate)
                    + " | Streak: " + expSystem.getCurrentStreak()
                    + " | Longest: " + expSystem.getLongestStreak());
        subHeader.setStyle("-fx-text-fill: #566573;");

        VBox headerBox = new VBox(4, header, subHeader);
        headerBox.setPadding(new Insets(16, 20, 8, 20));
        reportRoot.setTop(headerBox);

        GridPane summaryGrid = new GridPane();
        summaryGrid.setHgap(12);
        summaryGrid.setVgap(12);
        summaryGrid.setPadding(new Insets(0, 20, 12, 20));
        summaryGrid.add(createMetricCard("Categories", String.valueOf(categories.size()), "#2471a3"), 0, 0);
        summaryGrid.add(createMetricCard("Total Tasks", String.valueOf(metrics.total), "#2e86c1"), 1, 0);
        summaryGrid.add(createMetricCard("Completed", String.valueOf(metrics.completed), "#27ae60"), 2, 0);
        summaryGrid.add(createMetricCard("Active", String.valueOf(metrics.active), "#f39c12"), 3, 0);
        summaryGrid.add(createMetricCard("Overdue", String.valueOf(metrics.overdue), "#c0392b"), 0, 1);
        summaryGrid.add(createMetricCard("Due Today", String.valueOf(metrics.dueToday), "#8e44ad"), 1, 1);
        summaryGrid.add(createMetricCard("Level", String.valueOf(expSystem.getLevel()), "#16a085"), 2, 1);
        summaryGrid.add(createMetricCard("EXP", expSystem.getExp() + " / " + expSystem.getExpToNextLevel(), "#34495e"), 3, 1);

        PieChart completionChart = new PieChart();
        completionChart.setTitle("Completion Status");
        completionChart.getData().addAll(
                new PieChart.Data("Completed", metrics.completed),
                new PieChart.Data("Active", metrics.active));
        completionChart.setLabelsVisible(true);
        completionChart.setLegendVisible(true);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Priority");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Tasks");
        BarChart<String, Number> priorityChart = new BarChart<>(xAxis, yAxis);
        priorityChart.setTitle("Priority Breakdown");
        priorityChart.setLegendVisible(false);
        XYChart.Series<String, Number> prioritySeries = new XYChart.Series<>();
        prioritySeries.getData().add(new XYChart.Data<>("Low (!)", metrics.low));
        prioritySeries.getData().add(new XYChart.Data<>("Medium (!!)", metrics.medium));
        prioritySeries.getData().add(new XYChart.Data<>("High (!!!)", metrics.high));
        priorityChart.getData().add(prioritySeries);

        HBox chartRow = new HBox(12, completionChart, priorityChart);
        chartRow.setPadding(new Insets(0, 20, 14, 20));
        HBox.setHgrow(completionChart, Priority.ALWAYS);
        HBox.setHgrow(priorityChart, Priority.ALWAYS);

        Label trendLabel = new Label("Historical Trend");
        trendLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        trendLabel.setPadding(new Insets(0, 20, 0, 20));

        ComboBox<String> periodBox = new ComboBox<>();
        periodBox.getItems().addAll("Weekly (7 days)", "Monthly (30 days)");
        periodBox.setValue("Weekly (7 days)");

        LineChart<String, Number> trendChart = createTrendChart(db, 7);
        trendChart.setPrefHeight(250);
        periodBox.setOnAction(e -> {
            int days = "Monthly (30 days)".equals(periodBox.getValue()) ? 30 : 7;
            updateTrendChart(db, trendChart, days);
        });

        HBox trendControl = new HBox(10, new Label("Period:"), periodBox);
        trendControl.setAlignment(Pos.CENTER_LEFT);
        trendControl.setPadding(new Insets(0, 20, 0, 20));

        VBox centerBox = new VBox(10, summaryGrid, chartRow, trendLabel, trendControl, trendChart);
        reportRoot.setCenter(centerBox);

        Scene reportScene = new Scene(reportRoot, 980, 620);
        reportStage.setScene(reportScene);
        reportStage.show();
    }

    private static VBox createMetricCard(String title, String value, String accentColor) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #566573; -fx-font-size: 11;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 20; -fx-font-weight: bold;");

        VBox card = new VBox(3, titleLabel, valueLabel);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setMinWidth(180);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e4e7eb;");
        return card;
    }

    private static ReportMetrics calculateReportMetrics(List<Category> categories) {
        ReportMetrics metrics = new ReportMetrics();
        LocalDate today = LocalDate.now();

        for (Category category : categories) {
            for (Task task : category.getTasks()) {
                metrics.total++;
                if (task.isCompleted()) {
                    metrics.completed++;
                }
                if (task.hasDueDate()) {
                    if (!task.isCompleted() && task.getDueDate().isBefore(today)) {
                        metrics.overdue++;
                    }
                    if (!task.isCompleted() && task.getDueDate().isEqual(today)) {
                        metrics.dueToday++;
                    }
                }
                if (task.getPriority() == 1) {
                    metrics.low++;
                } else if (task.getPriority() == 2) {
                    metrics.medium++;
                } else {
                    metrics.high++;
                }
            }
        }

        metrics.active = metrics.total - metrics.completed;
        metrics.completionRate = metrics.total == 0 ? 0.0 : (metrics.completed * 100.0 / metrics.total);
        return metrics;
    }

    private static LineChart<String, Number> createTrendChart(DatabaseHelper db, int days) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Tasks");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Daily Productivity");
        chart.setLegendVisible(true);
        chart.setCreateSymbols(true);
        chart.setAnimated(false);

        updateTrendChart(db, chart, days);
        return chart;
    }

    private static void updateTrendChart(DatabaseHelper db, LineChart<String, Number> chart, int days) {
        List<DailyStat> stats = db.getDailyStats(days);
        XYChart.Series<String, Number> completedSeries = new XYChart.Series<>();
        completedSeries.setName("Completed");
        XYChart.Series<String, Number> createdSeries = new XYChart.Series<>();
        createdSeries.setName("Created");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        for (DailyStat stat : stats) {
            String label = stat.getDay().format(fmt);
            completedSeries.getData().add(new XYChart.Data<>(label, stat.getCompletedCount()));
            createdSeries.getData().add(new XYChart.Data<>(label, stat.getCreatedCount()));
        }

        chart.getData().clear();
        chart.getData().add(completedSeries);
        chart.getData().add(createdSeries);
    }

    private static class ReportMetrics {
        int total;
        int completed;
        int active;
        int overdue;
        int dueToday;
        int low;
        int medium;
        int high;
        double completionRate;
    }
}
