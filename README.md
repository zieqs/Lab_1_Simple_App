## Note Taking App

This project is a JavaFX desktop note/task manager with category organization, task priorities, and a gamified EXP/level system backed by SQLite.

## High-Level Use Cases

### Current Use Cases (Implemented)

1. **Manage Categories**
	- Create, rename, delete, and select categories.

2. **Manage Tasks**
	- Create, edit, and delete tasks inside a selected category.

3. **Track Task Progress**
	- Mark tasks complete or incomplete using task checkboxes.

4. **Manage Gamification**
	- Gain EXP from completing tasks, lose EXP on uncomplete, progress levels, and receive level-up feedback.

5. **Persist Data**
	- Save and load categories, tasks, and EXP state through SQLite.

6. **Navigate Interface**
	- Use sidebar/category navigation, top controls, and context menus.

7. **Validate User Input**
	- Prevent empty category/task names and block task creation when no category exists.

8. **Search and Filter Tasks**
	- Filter by keyword, task status, and priority.

9. **Set Deadlines**
	- Assign an optional due date for each task and highlight overdue tasks.

10. **Basic Analytics**
	- Show per-category totals for task count, completed tasks, and overdue tasks.

11. **Task Reminders**
	- Show startup reminders for overdue and due-today active tasks, and trigger daily reminders.

12. **Reports**
	- View global summary metrics (completion rate, priorities, due/overdue, and EXP state).

13. **Advanced Analytics and Visualization**
	- Open a dedicated report dashboard with summary cards and charts (completion status and priority breakdown).

14. **Historical Trends**
	- Track daily created/completed task counts and view weekly/monthly trend lines.

### Future Use Cases (Planned)

- No pending items in the current scope.

## Folder Structure

- `src/main/java`: Java source files (`NoteApp`, models, database helper, launcher)
- `pom.xml`: Maven build configuration and dependencies
- `noteapp.db`: SQLite database file (created at runtime)

## Running the App

Use the launcher class when running from VS Code:

- Main class: `NoteAppLauncher`

Or with Maven (if installed):

```bash
mvn javafx:run
```
