package net.momo.silo.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

/** JSON file persistence with backup and atomic writes. */
public final class JsonPersistence implements PersistenceProvider {

    private static final HytaleLogger logger = HytaleLogger.forEnclosingClass();
    private static final int CURRENT_VERSION = 1;

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    private final Path dataFile;
    private final Path backupFile;
    private final Map<String, CollectionBinding<?>> bindings = new LinkedHashMap<>();

    public JsonPersistence(Path dataDirectory, String filename) {
        this.dataFile = dataDirectory.resolve(filename);
        this.backupFile = dataDirectory.resolve(filename + ".backup");
    }

    /** Binds a collection for persistence. */
    public <T> JsonPersistence bind(String key, Codec<T> codec, 
            Supplier<Iterable<T>> getter, Consumer<T> loader) {
        bindings.put(key, new CollectionBinding<>(codec, getter, loader));
        return this;
    }

    @Override
    public void load() throws IOException {
        if (!Files.exists(dataFile)) {
            logger.at(Level.INFO).log("No data file found at %s, starting fresh", dataFile);
            return;
        }

        try {
            String json = Files.readString(dataFile, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            for (Map.Entry<String, CollectionBinding<?>> entry : bindings.entrySet()) {
                String key = entry.getKey();
                CollectionBinding<?> binding = entry.getValue();

                if (root.has(key)) {
                    JsonArray array = root.getAsJsonArray(key);
                    binding.load(array);
                }
            }

            logger.at(Level.INFO).log("Loaded data from %s", dataFile);

        } catch (JsonSyntaxException e) {
            logger.at(Level.SEVERE).log("Invalid JSON in %s, attempting backup restore", dataFile);
            if (restoreFromBackup()) {
                load();
            } else {
                throw new IOException("Failed to load data and no backup available", e);
            }
        }
    }

    @Override
    public void save() throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("version", CURRENT_VERSION);

        for (Map.Entry<String, CollectionBinding<?>> entry : bindings.entrySet()) {
            String key = entry.getKey();
            CollectionBinding<?> binding = entry.getValue();
            root.add(key, binding.save());
        }

        Files.createDirectories(dataFile.getParent());

        if (Files.exists(dataFile)) {
            Files.copy(dataFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
        }

        Path tempFile = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");
        String json = GSON.toJson(root);
        Files.writeString(tempFile, json, StandardCharsets.UTF_8);

        Files.move(tempFile, dataFile,
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE);

        logger.at(Level.INFO).log("Saved data to %s", dataFile);
    }

    private boolean restoreFromBackup() {
        if (!Files.exists(backupFile)) {
            return false;
        }

        try {
            Files.copy(backupFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
            logger.at(Level.INFO).log("Restored from backup %s", backupFile);
            return true;
        } catch (IOException e) {
            logger.at(Level.SEVERE).withCause(e).log("Failed to restore from backup");
            return false;
        }
    }

    private static final class CollectionBinding<T> {
        private final Codec<T> codec;
        private final Supplier<Iterable<T>> getter;
        private final Consumer<T> loader;

        CollectionBinding(Codec<T> codec, Supplier<Iterable<T>> getter, Consumer<T> loader) {
            this.codec = codec;
            this.getter = getter;
            this.loader = loader;
        }

        void load(JsonArray array) {
            for (JsonElement element : array) {
                try {
                    T value = codec.deserialize(element);
                    loader.accept(value);
                } catch (Exception e) {
                    logger.at(Level.WARNING).withCause(e).log("Failed to deserialize element");
                }
            }
        }

        JsonArray save() {
            JsonArray array = new JsonArray();
            for (T value : getter.get()) {
                try {
                    array.add(codec.serialize(value));
                } catch (Exception e) {
                    logger.at(Level.WARNING).withCause(e).log("Failed to serialize element");
                }
            }
            return array;
        }
    }
}
