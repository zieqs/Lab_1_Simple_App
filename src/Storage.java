import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;

// Gson is provided via lib/gson-2.10.1.jar
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Storage {
    private static final String DATA_FILE = "data.json";
    private final Gson gson;

    public Storage() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public Optional<AppState> load() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            return Optional.empty();
        }

        try (FileReader reader = new FileReader(file)) {
            AppState state = gson.fromJson(reader, AppState.class);
            return Optional.ofNullable(state);
        } catch (IOException e) {
            System.err.println("Failed to load state: " + e.getMessage());
            return Optional.empty();
        }
    }

    public void save(AppState state) {
        try (FileWriter writer = new FileWriter(DATA_FILE)) {
            gson.toJson(state, writer);
        } catch (IOException e) {
            System.err.println("Failed to save state: " + e.getMessage());
        }
    }
}
