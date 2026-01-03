package org.daanlokdrog.classicachievement;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClassicAchievementConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("classic_achievement.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static ConfigData data = new ConfigData();

    public static class ConfigData {
        public boolean olderUI = false;
        public boolean hidePage = false;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (var reader = Files.newBufferedReader(CONFIG_PATH)) {
                data = GSON.fromJson(reader, ConfigData.class);
            } catch (IOException e) {
                System.err.println("[ClassicAchievement] Failed to load config, using defaults.");
            }
        } else {
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (var writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            System.err.println("[ClassicAchievement] Failed to save config.");
        }
    }

    public static boolean isOlderUI() { return data.olderUI; }
    public static boolean isHidePage() { return data.hidePage; }
}