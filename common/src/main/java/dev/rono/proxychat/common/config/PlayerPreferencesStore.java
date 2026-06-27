package dev.rono.proxychat.common.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.rono.proxychat.common.channel.ChatChannel;

/**
 * Optional persistence for per-player toggle and ignore preferences across reconnects.
 */
public final class PlayerPreferencesStore {
    private static final String FILE_NAME = "player-preferences.yml";

    private final Logger logger;
    private final Path dataDirectory;
    private final Map<UUID, Map<String, Boolean>> toggledChannels = new HashMap<>();
    private final Map<UUID, Map<String, Boolean>> ignoredChannels = new HashMap<>();

    public PlayerPreferencesStore(Logger logger, Path dataDirectory) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    public void load() {
        toggledChannels.clear();
        ignoredChannels.clear();

        var file = dataDirectory.resolve(FILE_NAME);
        if (!Files.exists(file)) {
            return;
        }

        try {
            var document = YamlDocument.create(file.toFile());
            var players = document.getSection("players");
            if (players == null) {
                return;
            }

            for (Object playerKey : players.getKeys()) {
                var playerId = UUID.fromString(String.valueOf(playerKey));
                var playerSection = players.getSection(String.valueOf(playerKey));
                if (playerSection == null) {
                    continue;
                }

                readChannelFlags(playerSection.getSection("toggled"), toggledChannels, playerId);
                readChannelFlags(playerSection.getSection("ignored"), ignoredChannels, playerId);
            }

        } catch (Exception exception) {
            logger.log(Level.WARNING, "Failed to load " + FILE_NAME, exception);
        }
    }

    public void applyToChannels(UUID playerId, Iterable<ChatChannel> channels) {
        var toggled = toggledChannels.get(playerId);
        var ignored = ignoredChannels.get(playerId);
        if (toggled == null && ignored == null) {
            return;
        }

        for (ChatChannel channel : channels) {
            var commandName = channel.getCommandName();
            if (commandName == null || commandName.isEmpty()) {
                continue;
            }

            var toggleUtils = channel.getToggleUtils();
            if (toggled != null && Boolean.TRUE.equals(toggled.get(commandName))) {
                toggleUtils.setToggled(playerId, true);
            }

            if (ignored != null && Boolean.TRUE.equals(ignored.get(commandName))) {
                toggleUtils.setIgnored(playerId, true);
            }
        }
    }

    public void recordToggle(UUID playerId, String commandName, boolean enabled) {
        toggledChannels.computeIfAbsent(playerId, ignored -> new HashMap<>()).put(commandName, enabled);
        saveAsync();
    }

    public void recordIgnore(UUID playerId, String commandName, boolean enabled) {
        ignoredChannels.computeIfAbsent(playerId, ignored -> new HashMap<>()).put(commandName, enabled);
        saveAsync();
    }

    public void clearRuntimeState(UUID playerId, Iterable<ChatChannel> channels) {
        for (ChatChannel channel : channels) {
            channel.getToggleUtils().clearPlayer(playerId);
        }
    }

    private void saveAsync() {
        try {
            save();
        } catch (IOException exception) {
            logger.log(Level.WARNING, "Failed to save " + FILE_NAME, exception);
        }
    }

    private void save() throws IOException {
        var file = dataDirectory.resolve(FILE_NAME);
        Files.createDirectories(dataDirectory);

        var document = Files.exists(file)
                ? YamlDocument.create(file.toFile())
                : YamlDocument.create(new java.io.ByteArrayInputStream("players: {}\n".getBytes(StandardCharsets.UTF_8)));

        var players = document.getSection("players");
        if (players == null) {
            document.set("players", new HashMap<String, Object>());
            players = document.getSection("players");
        }

        writePlayerSection(players, toggledChannels, "toggled");
        writePlayerSection(players, ignoredChannels, "ignored");

        Files.writeString(file, document.dump());
    }

    private static void writePlayerSection(
            Section players,
            Map<UUID, Map<String, Boolean>> source,
            String route
    ) {
        for (Map.Entry<UUID, Map<String, Boolean>> entry : source.entrySet()) {
            var playerKey = entry.getKey().toString();
            var playerSection = players.getSection(playerKey);
            if (playerSection == null) {
                players.set(playerKey, new HashMap<String, Object>());
                playerSection = players.getSection(playerKey);
            }

            var flags = new HashMap<>();
            for (Map.Entry<String, Boolean> flag : entry.getValue().entrySet()) {
                if (Boolean.TRUE.equals(flag.getValue())) {
                    flags.put(flag.getKey(), true);
                }
            }

            if (flags.isEmpty()) {
                if (playerSection != null) {
                    playerSection.remove(route);
                }
                continue;
            }

            if (playerSection != null) {
                playerSection.set(route, flags);
            }
        }
    }

    private static void readChannelFlags(Section section, Map<UUID, Map<String, Boolean>> target, UUID playerId) {
        if (section == null) {
            return;
        }

        var flags = new HashMap<String, Boolean>();
        for (Object key : section.getKeys()) {
            var commandName = String.valueOf(key);
            var value = section.getBoolean(commandName);
            if (Boolean.TRUE.equals(value)) {
                flags.put(commandName, true);
            }
        }

        if (!flags.isEmpty()) {
            target.put(playerId, flags);
        }
    }
}
