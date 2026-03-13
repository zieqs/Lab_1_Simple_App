## Getting Started

Welcome to the VS Code Java world. Here is a guideline to help you get started to write Java code in Visual Studio Code.

### Quick run (JavaFX)
- Build: `javac --module-path lib --add-modules javafx.controls,javafx.fxml -d bin src/*.java`
- Run NoteApp: `java --module-path lib --add-modules javafx.controls,javafx.fxml -cp bin NoteApp`

The bundled VS Code launch configs also point `classPaths` at `bin`, so clicking Run/Debug on `NoteApp` or `HelloJavaFX` should work once the classes are built.

### Persistence
Task data, categories, and EXP are saved to `data.json` next to the project files. Saves happen after each change; on startup the app will restore from that file if present.

## Folder Structure

The workspace contains two folders by default, where:

- `src`: the folder to maintain sources
- `lib`: the folder to maintain dependencies

Meanwhile, the compiled output files will be generated in the `bin` folder by default.

> If you want to customize the folder structure, open `.vscode/settings.json` and update the related settings there.

## Dependency Management

The `JAVA PROJECTS` view allows you to manage your dependencies. More details can be found [here](https://github.com/microsoft/vscode-java-dependency#manage-dependencies).
