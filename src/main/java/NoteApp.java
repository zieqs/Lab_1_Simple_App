import javafx.application.Application;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NoteApp extends Application {

    private DatabaseHelper db;
    private List<Category> categories;
    private Category selectedCategory;
    private ExpSystem expSystem;

    // UI components that need refreshing
    private VBox categoryListContainer;
    private VBox taskListContainer;
    private Label levelLabel;
    private Label expLabel;
    private ProgressBar expBar;
    private VBox sidebar;
    private BorderPane root;
    private boolean sidebarVisible = true;
    private TextField searchField;
    private ComboBox<String> statusFilterBox;
    private ComboBox<String> priorityFilterBox;
    private Label analyticsLabel;
    private Timeline reminderTimeline;
    private LocalDate lastAutoReminderDate;

    @Override
    public void start(Stage primaryStage) {
        // Initialize database
        db = new DatabaseHelper();

        // Load data from database
        categories = db.getAllCategories();
        expSystem = db.loadExpSystem();

        // Create default category if database is empty
        if (categories.isEmpty()) {
            Category dailyTask = new Category("Daily Task");
            int id = db.insertCategory(dailyTask.getName());
            dailyTask.setId(id);
            categories.add(dailyTask);
        }
        selectedCategory = categories.isEmpty() ? null : categories.get(0);
        lastAutoReminderDate = null;

        root = new BorderPane();

        // ===== LEFT SIDEBAR =====
        sidebar = buildSidebar();
        root.setLeft(sidebar);

        // ===== CENTER CONTENT =====
        VBox center = buildCenter();
        root.setCenter(center);

        // ===== TOP BAR (EXP) =====
        HBox topBar = buildTopBar();
        root.setTop(topBar);

        refreshExpDisplay();

        Scene scene = new Scene(root, 850, 600);
        primaryStage.setTitle("Note Taking App");
        primaryStage.setScene(scene);
        primaryStage.show();

        showDueTaskReminder();
        startReminderScheduler();
        primaryStage.setOnCloseRequest(e -> stopReminderScheduler());
    }

    // ==================== TOP BAR ====================

    private HBox buildTopBar() {
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(10, 15, 10, 15));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #2c3e50;");

        Button toggleSidebarBtn = new Button("\u2630");
        toggleSidebarBtn.setStyle(
                "-fx-background-color: #3d566e; -fx-text-fill: white; -fx-font-size: 16; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 5 10;");
        toggleSidebarBtn.setOnAction(e -> {
            sidebarVisible = !sidebarVisible;
            if (sidebarVisible) {
                root.setLeft(sidebar);
            } else {
                root.setLeft(null);
            }
        });

        Label titleLabel = new Label("Note Taking App");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setStyle("-fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        levelLabel = new Label("Level: 1");
        levelLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-weight: bold; -fx-font-size: 13;");

        expLabel = new Label("EXP: 0 / 100");
        expLabel.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 12;");

        expBar = new ProgressBar(0);
        expBar.setPrefWidth(150);
        expBar.setStyle("-fx-accent: #f1c40f;");

        topBar.getChildren().addAll(toggleSidebarBtn, titleLabel, spacer, levelLabel, expBar, expLabel);
        return topBar;
    }

    // ==================== SIDEBAR ====================

    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(200);
        sidebar.setStyle("-fx-background-color: #34495e;");

        Label homeLabel = new Label("Home");
        homeLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        homeLabel.setStyle("-fx-text-fill: white;");
        homeLabel.setPadding(new Insets(15, 15, 5, 15));

        HBox filesHeader = new HBox();
        filesHeader.setAlignment(Pos.CENTER_LEFT);
        filesHeader.setPadding(new Insets(10, 15, 5, 15));

        Label filesLabel = new Label("Files");
        filesLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        filesLabel.setStyle("-fx-text-fill: #bdc3c7;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addCategoryBtn = new Button("+");
        addCategoryBtn.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14; -fx-background-radius: 15; -fx-min-width: 30; -fx-min-height: 30;");
        addCategoryBtn.setOnAction(e -> showCreateCategoryDialog());

        filesHeader.getChildren().addAll(filesLabel, spacer, addCategoryBtn);

        categoryListContainer = new VBox(2);
        categoryListContainer.setPadding(new Insets(5, 10, 10, 10));

        ScrollPane scrollPane = new ScrollPane(categoryListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #34495e; -fx-background-color: #34495e;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        sidebar.getChildren().addAll(homeLabel, filesHeader, scrollPane);
        refreshCategoryList();
        return sidebar;
    }

    // ==================== CENTER ====================

    private VBox buildCenter() {
        VBox center = new VBox();
        center.setStyle("-fx-background-color: #ecf0f1;");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 20, 10, 20));

        Label headerLabel = new Label("Tasks");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button menuBtn = new Button("...");
        menuBtn.setStyle(
                "-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-font-size: 14; -fx-background-radius: 20; -fx-min-width: 40; -fx-min-height: 40;");

        ContextMenu mainMenu = new ContextMenu();

        MenuItem renameCategoryItem = new MenuItem("Rename Category");
        renameCategoryItem.setOnAction(e -> {
            if (selectedCategory != null) {
                showRenameCategoryDialog(selectedCategory);
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No Category Selected");
                alert.setContentText("Please select a category first.");
                alert.showAndWait();
            }
        });

        MenuItem deleteCategoryItem = new MenuItem("Delete Category");
        deleteCategoryItem.setOnAction(e -> {
            if (selectedCategory != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Delete Category");
                confirm.setHeaderText("Delete \"" + selectedCategory.getName() + "\"?");
                confirm.setContentText("All tasks in this category will be lost.");
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    db.deleteCategory(selectedCategory.getId());
                    categories.remove(selectedCategory);
                    selectedCategory = categories.isEmpty() ? null : categories.get(0);
                    refreshCategoryList();
                    refreshTaskList();
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No Category Selected");
                alert.setContentText("Please select a category first.");
                alert.showAndWait();
            }
        });

        MenuItem reportsItem = new MenuItem("View Reports");
        reportsItem.setOnAction(e -> showReportsDialog());

        mainMenu.getItems().addAll(renameCategoryItem, deleteCategoryItem, new SeparatorMenuItem(), reportsItem);
        menuBtn.setOnAction(e -> mainMenu.show(menuBtn, javafx.geometry.Side.TOP, 0, 0));

        header.getChildren().addAll(headerLabel, headerSpacer, menuBtn);

        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(0, 20, 10, 20));

        searchField = new TextField();
        searchField.setPromptText("Search tasks...");
        searchField.setPrefWidth(240);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshTaskList());

        statusFilterBox = new ComboBox<>();
        statusFilterBox.getItems().addAll("All", "Active", "Completed");
        statusFilterBox.setValue("All");
        statusFilterBox.setOnAction(e -> refreshTaskList());

        priorityFilterBox = new ComboBox<>();
        priorityFilterBox.getItems().addAll("All Priorities", "Low (!)", "Medium (!!)", "High (!!!)");
        priorityFilterBox.setValue("All Priorities");
        priorityFilterBox.setOnAction(e -> refreshTaskList());

        filterBar.getChildren().addAll(searchField, statusFilterBox, priorityFilterBox);

        analyticsLabel = new Label();
        analyticsLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12;");
        analyticsLabel.setPadding(new Insets(0, 20, 8, 20));

        taskListContainer = new VBox(5);
        taskListContainer.setPadding(new Insets(5, 20, 10, 20));

        ScrollPane scrollPane = new ScrollPane(taskListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #ecf0f1; -fx-background-color: #ecf0f1;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        HBox bottomBar = new HBox(10);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(10, 20, 15, 20));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addTaskBtn = new Button("+");
        addTaskBtn.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 18; -fx-background-radius: 25; -fx-min-width: 50; -fx-min-height: 50;");
        addTaskBtn.setOnAction(e -> showCreateTaskDialog());

        bottomBar.getChildren().addAll(spacer, addTaskBtn);

        center.getChildren().addAll(header, filterBar, analyticsLabel, scrollPane, bottomBar);
        refreshTaskList();
        return center;
    }

    // ==================== REFRESH UI ====================

    private void refreshCategoryList() {
        categoryListContainer.getChildren().clear();
        for (Category cat : categories) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 10, 8, 10));
            row.setStyle(cat == selectedCategory
                    ? "-fx-background-color: #2c3e50; -fx-background-radius: 5;"
                    : "-fx-background-color: transparent; -fx-background-radius: 5;");

            Label nameLabel = new Label(cat.getName());
            nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13;");
            nameLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(nameLabel, Priority.ALWAYS);

            Label countLabel = new Label("(" + cat.getTasks().size() + ")");
            countLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11;");

            row.getChildren().addAll(nameLabel, countLabel);

            row.setOnMouseClicked(e -> {
                selectedCategory = cat;
                refreshCategoryList();
                refreshTaskList();
            });

            ContextMenu contextMenu = new ContextMenu();

            MenuItem renameItem = new MenuItem("Rename");
            renameItem.setOnAction(e -> showRenameCategoryDialog(cat));

            MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Delete Category");
                confirm.setHeaderText("Delete \"" + cat.getName() + "\"?");
                confirm.setContentText("All tasks in this category will be lost.");
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    db.deleteCategory(cat.getId());
                    categories.remove(cat);
                    if (selectedCategory == cat) {
                        selectedCategory = categories.isEmpty() ? null : categories.get(0);
                    }
                    refreshCategoryList();
                    refreshTaskList();
                }
            });

            contextMenu.getItems().addAll(renameItem, deleteItem);
            row.setOnContextMenuRequested(e -> contextMenu.show(row, e.getScreenX(), e.getScreenY()));

            categoryListContainer.getChildren().add(row);
        }
    }

    private void refreshTaskList() {
        taskListContainer.getChildren().clear();

        if (selectedCategory == null) {
            Label emptyLabel = new Label("No category selected. Create one first!");
            emptyLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 14;");
            taskListContainer.getChildren().add(emptyLabel);
            if (analyticsLabel != null) {
                analyticsLabel.setText("Analytics: no category selected");
            }
            return;
        }

        updateAnalytics(selectedCategory.getTasks());

        List<Task> visibleTasks = getFilteredTasks(selectedCategory.getTasks());

        if (visibleTasks.isEmpty()) {
            Label emptyLabel = new Label("No tasks yet. Click + to add one!");
            emptyLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 14;");
            if (!selectedCategory.getTasks().isEmpty()) {
                emptyLabel.setText("No tasks match your current search/filter.");
            }
            taskListContainer.getChildren().add(emptyLabel);
            return;
        }

        DateTimeFormatter dueDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        for (Task task : visibleTasks) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 15, 10, 15));
            row.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 1);");

            CheckBox checkBox = new CheckBox();
            checkBox.setSelected(task.isCompleted());
            checkBox.setOnAction(e -> {
                task.setCompleted(checkBox.isSelected());
                db.updateTask(task.getId(), task.getName(), task.getPriority(), task.isCompleted());
                if (checkBox.isSelected()) {
                    db.recordTaskCompleted(LocalDate.now());
                    boolean leveledUp = expSystem.completeTask(task.getPriority());
                    db.saveExpSystem(expSystem);
                    if (leveledUp) {
                        showLevelUpDialog();
                    }
                } else {
                    db.recordTaskUncompleted(LocalDate.now());
                    expSystem.uncompleteTask(task.getPriority());
                    db.saveExpSystem(expSystem);
                }
                refreshExpDisplay();
                refreshTaskList();
            });

            Label priorityLabel = new Label(task.getPriorityLabel());
            String priorityColor = task.getPriority() == 3 ? "#e74c3c"
                    : task.getPriority() == 2 ? "#f39c12" : "#3498db";
            priorityLabel.setStyle("-fx-text-fill: " + priorityColor + "; -fx-font-weight: bold; -fx-font-size: 14;");
            priorityLabel.setMinWidth(30);

            Label nameLabel = new Label(task.getName());
            nameLabel.setMaxWidth(Double.MAX_VALUE);
            nameLabel.setStyle(task.isCompleted()
                    ? "-fx-text-fill: #95a5a6; -fx-strikethrough: true; -fx-font-size: 14;"
                    : "-fx-text-fill: #2c3e50; -fx-font-size: 14;");
            HBox.setHgrow(nameLabel, Priority.ALWAYS);

            Label dueDateLabel = new Label();
            dueDateLabel.setMinWidth(110);
            if (task.hasDueDate()) {
                dueDateLabel.setText("Due " + task.getDueDate().format(dueDateFormatter));
                if (task.isOverdue()) {
                    dueDateLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold; -fx-font-size: 11;");
                } else {
                    dueDateLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11;");
                }
            } else {
                dueDateLabel.setText("No due date");
                dueDateLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11;");
            }

            Button editBtn = new Button("edit");
            editBtn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #3498db; -fx-border-color: #3498db; -fx-border-radius: 3; -fx-font-size: 11; -fx-cursor: hand;");
            editBtn.setOnAction(e -> showEditTaskDialog(task));

            Button deleteBtn = new Button("delete");
            deleteBtn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-border-color: #e74c3c; -fx-border-radius: 3; -fx-font-size: 11; -fx-cursor: hand;");
            deleteBtn.setOnAction(e -> {
                db.deleteTask(task.getId());
                selectedCategory.removeTask(task);
                refreshTaskList();
                refreshCategoryList();
            });

            row.getChildren().addAll(checkBox, priorityLabel, nameLabel, dueDateLabel, editBtn, deleteBtn);
            taskListContainer.getChildren().add(row);
        }
    }

    private List<Task> getFilteredTasks(List<Task> tasks) {
        List<Task> filtered = new ArrayList<>();
        String searchText = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        String statusFilter = statusFilterBox == null ? "All" : statusFilterBox.getValue();
        String priorityFilter = priorityFilterBox == null ? "All Priorities" : priorityFilterBox.getValue();

        for (Task task : tasks) {
            boolean matchesSearch = searchText.isEmpty() || task.getName().toLowerCase().contains(searchText);
            boolean matchesStatus = "All".equals(statusFilter)
                    || ("Active".equals(statusFilter) && !task.isCompleted())
                    || ("Completed".equals(statusFilter) && task.isCompleted());
            boolean matchesPriority = "All Priorities".equals(priorityFilter)
                    || ("Low (!)".equals(priorityFilter) && task.getPriority() == 1)
                    || ("Medium (!!)".equals(priorityFilter) && task.getPriority() == 2)
                    || ("High (!!!)".equals(priorityFilter) && task.getPriority() == 3);

            if (matchesSearch && matchesStatus && matchesPriority) {
                filtered.add(task);
            }
        }
        return filtered;
    }

    private void updateAnalytics(List<Task> tasks) {
        int total = tasks.size();
        int completed = 0;
        int overdue = 0;
        for (Task task : tasks) {
            if (task.isCompleted()) {
                completed++;
            }
            if (task.isOverdue()) {
                overdue++;
            }
        }
        if (analyticsLabel != null) {
            analyticsLabel.setText("Analytics: total " + total + " | completed " + completed + " | overdue " + overdue);
        }
    }

    private void refreshExpDisplay() {
        levelLabel.setText("Level: " + expSystem.getLevel());
        expLabel.setText("EXP: " + expSystem.getExp() + " / " + expSystem.getExpToNextLevel()
                + " | Streak: " + expSystem.getCurrentStreak());
        expBar.setProgress((double) expSystem.getExp() / expSystem.getExpToNextLevel());
    }

    // ==================== DIALOGS ====================

    private void showCreateCategoryDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Category");
        dialog.setHeaderText("New Category");
        dialog.setContentText("Name:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                Category cat = new Category(name.trim());
                int id = db.insertCategory(cat.getName());
                cat.setId(id);
                categories.add(cat);
                selectedCategory = cat;
                refreshCategoryList();
                refreshTaskList();
            }
        });
    }

    private void showRenameCategoryDialog(Category cat) {
        TextInputDialog dialog = new TextInputDialog(cat.getName());
        dialog.setTitle("Rename Category");
        dialog.setHeaderText("Rename \"" + cat.getName() + "\"");
        dialog.setContentText("New name:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                cat.setName(name.trim());
                db.updateCategory(cat.getId(), cat.getName());
                refreshCategoryList();
            }
        });
    }

    private void showCreateTaskDialog() {
        if (selectedCategory == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Category");
            alert.setContentText("Please create a category first.");
            alert.showAndWait();
            return;
        }

        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Create Task");
        dialog.setHeaderText("New Task in \"" + selectedCategory.getName() + "\"");

        ButtonType createButton = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Task name");

        ComboBox<String> priorityBox = new ComboBox<>();
        priorityBox.getItems().addAll("! (Low)", "!! (Medium)", "!!! (High)");
        priorityBox.setValue("! (Low)");

        DatePicker dueDatePicker = new DatePicker();
        dueDatePicker.setPromptText("Optional");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Priority:"), 0, 1);
        grid.add(priorityBox, 1, 1);
        grid.add(new Label("Due Date:"), 0, 2);
        grid.add(dueDatePicker, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButton && !nameField.getText().trim().isEmpty()) {
                int priority = priorityBox.getSelectionModel().getSelectedIndex() + 1;
                Task createdTask = new Task(nameField.getText().trim(), priority);
                createdTask.setDueDate(dueDatePicker.getValue());
                return createdTask;
            }
            return null;
        });

        Optional<Task> result = dialog.showAndWait();
        result.ifPresent(task -> {
            int id = db.insertTask(selectedCategory.getId(), task.getName(), task.getPriority(), task.getDueDate());
            task.setId(id);
            selectedCategory.addTask(task);
            db.recordTaskCreated(LocalDate.now());
            refreshTaskList();
            refreshCategoryList();
        });
    }

    private void showEditTaskDialog(Task task) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Task");
        dialog.setHeaderText("Edit Task");

        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(task.getName());

        ComboBox<String> priorityBox = new ComboBox<>();
        priorityBox.getItems().addAll("! (Low)", "!! (Medium)", "!!! (High)");
        priorityBox.getSelectionModel().select(task.getPriority() - 1);

        DatePicker dueDatePicker = new DatePicker(task.getDueDate());
        dueDatePicker.setPromptText("Optional");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Priority:"), 0, 1);
        grid.add(priorityBox, 1, 1);
        grid.add(new Label("Due Date:"), 0, 2);
        grid.add(dueDatePicker, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButton && !nameField.getText().trim().isEmpty()) {
            task.setName(nameField.getText().trim());
            task.setPriority(priorityBox.getSelectionModel().getSelectedIndex() + 1);
            task.setDueDate(dueDatePicker.getValue());
            db.updateTask(task.getId(), task.getName(), task.getPriority(), task.isCompleted(), task.getDueDate());
            refreshTaskList();
        }
    }

    private void showLevelUpDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Level Up!");
        alert.setHeaderText("Congratulations! You reached Level " + expSystem.getLevel() + "!");
        alert.setContentText("You gained +" + expSystem.getLastExpGain() + " EXP\n"
                + "Current streak: " + expSystem.getCurrentStreak() + " day(s)\n\n"
                + expSystem.getRandomQuote());
        alert.showAndWait();
    }

    private void startReminderScheduler() {
        reminderTimeline = new Timeline(new KeyFrame(Duration.seconds(30), event -> checkScheduledReminder()));
        reminderTimeline.setCycleCount(Timeline.INDEFINITE);
        reminderTimeline.play();
    }

    private void stopReminderScheduler() {
        if (reminderTimeline != null) {
            reminderTimeline.stop();
        }
    }

    private void checkScheduledReminder() {
        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();

        LocalTime configured = LocalTime.of(9, 0);

        boolean timeReached = !now.withSecond(0).withNano(0).isBefore(configured);
        boolean alreadyShownToday = today.equals(lastAutoReminderDate);

        if (timeReached && !alreadyShownToday) {
            lastAutoReminderDate = today;
            showDueTaskReminder();
        }
    }

    private void showDueTaskReminder() {
        List<String> overdueTasks = new ArrayList<>();
        List<String> dueTodayTasks = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Category category : categories) {
            for (Task task : category.getTasks()) {
                if (task.isCompleted() || !task.hasDueDate()) {
                    continue;
                }
                if (task.getDueDate().isBefore(today)) {
                    overdueTasks.add(task.getName() + " (" + category.getName() + ")");
                } else if (task.getDueDate().isEqual(today)) {
                    dueTodayTasks.add(task.getName() + " (" + category.getName() + ")");
                }
            }
        }

        if (overdueTasks.isEmpty() && dueTodayTasks.isEmpty()) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Task Reminders");
        alert.setHeaderText("You have upcoming task deadlines");

        StringBuilder content = new StringBuilder();
        if (!overdueTasks.isEmpty()) {
            content.append("Overdue tasks (" + overdueTasks.size() + "):\n");
            appendLimitedItems(content, overdueTasks, 5);
            content.append("\n");
        }
        if (!dueTodayTasks.isEmpty()) {
            content.append("Due today (" + dueTodayTasks.size() + "):\n");
            appendLimitedItems(content, dueTodayTasks, 5);
        }

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    private void appendLimitedItems(StringBuilder content, List<String> items, int maxItems) {
        int shown = Math.min(items.size(), maxItems);
        for (int i = 0; i < shown; i++) {
            content.append("- ").append(items.get(i)).append("\n");
        }
        if (items.size() > maxItems) {
            content.append("- ... and ").append(items.size() - maxItems).append(" more\n");
        }
    }

    private void showReportsDialog() {
        ReportDashboard.show(categories, expSystem, db);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
