package dev.rono.proxychat.common.config;

import dev.rono.proxychat.common.config.migration.ConfigVersion;
import dev.rono.proxychat.common.config.migration.ConfigVersionPattern;
import dev.rono.proxychat.common.config.migration.LegacyVersionNormalizer;
import dev.rono.proxychat.common.config.migration.ProxyChatConfigMigrations;
import dev.rono.proxychat.common.config.ProxyChatMessages;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.AutomaticVersioning;
import dev.dejvokep.boostedyaml.settings.Settings;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings.OptionSorting;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public final class ProxyChatYamlDocuments {
  private static final String CONFIG_DEFAULT_RESOURCE = "config.yml";
  private static final String CHANNEL_DEFAULT_RESOURCE = "global.yml";

  private static byte[] bundledConfigDefaults;
  private static byte[] bundledChannelDefaults;

  private ProxyChatYamlDocuments() {
  }

  public static synchronized void configureDefaults(InputStream configDefaults, InputStream channelDefaults) throws IOException {
    if (configDefaults != null) {
      bundledConfigDefaults = configDefaults.readAllBytes();
    }

    if (channelDefaults != null) {
      bundledChannelDefaults = channelDefaults.readAllBytes();
    }
  }

  public static YamlDocument loadMainConfig(Path dataDirectory, Logger logger) throws IOException {
  ensureDefaultsLoaded();

    Path configPath = dataDirectory.resolve("config.yml");
    YamlDocument document = YamlDocument.create(
        configPath.toFile(),
        configDefaultsStream(),
        mainConfigSettings(dataDirectory, logger)
    );

    if (Files.exists(configPath) && Files.size(configPath) > 0) {
      LegacyVersionNormalizer.normalize(document);
      document.update();
      ProxyChatConfigMigrations.ensureLatestKeys(document);
      document.set(ConfigVersionPattern.VERSION_ROUTE, ConfigVersion.LATEST.toString());
      document.save();
    }

    return document;
  }

  public static YamlDocument loadChannel(Path channelFile) throws IOException {
    ensureDefaultsLoaded();

    YamlDocument document = YamlDocument.create(
        channelFile.toFile(),
        channelDefaultsStream(),
        channelSettings()
    );

    if (Files.exists(channelFile) && Files.size(channelFile) > 0) {
      document.update();
      document.save();
    }

    return document;
  }

  private static Settings[] mainConfigSettings(Path dataDirectory, Logger logger) {
    return new Settings[]{
        GeneralSettings.builder().setUseDefaults(false).build(),
        LoaderSettings.builder().setAutoUpdate(false).build(),
        DumperSettings.DEFAULT,
        UpdaterSettings.builder()
            .setVersioning(new AutomaticVersioning(ConfigVersionPattern.pattern(), ConfigVersionPattern.VERSION_ROUTE))
            .setOptionSorting(OptionSorting.SORT_BY_DEFAULTS)
            .addCustomLogic("1", ProxyChatConfigMigrations::migrateV0ToV1)
            .addCustomLogic("1.1", ProxyChatConfigMigrations::migrateV1ToV1_1)
            .addCustomLogic("1.2", ProxyChatConfigMigrations::migrateV1_1ToV1_2)
            .addCustomLogic("1.3", ProxyChatConfigMigrations::migrateV1_2ToV1_3)
            .addCustomLogic("1.4", ProxyChatConfigMigrations::migrateV1_3ToV1_4)
            .addCustomLogic("1.5", ProxyChatConfigMigrations::migrateV1_4ToV1_5)
            .addCustomLogic("2", document -> {
              try {
                ProxyChatConfigMigrations.migrateInlineChatsToFolder(document, dataDirectory, logger);
              } catch (IOException exception) {
                throw new IllegalStateException("Failed migrating inline chats", exception);
              }
            })
            .addCustomLogic("2.1", ProxyChatConfigMigrations::migrateV2ToV2_1)
            .addCustomLogic("2.2", ProxyChatConfigMigrations::migrateV2_1ToV2_2)
            .build()
    };
  }

  private static Settings[] channelSettings() {
    return new Settings[]{
        GeneralSettings.builder().setUseDefaults(false).build(),
        LoaderSettings.builder().setAutoUpdate(false).build(),
        DumperSettings.DEFAULT,
        UpdaterSettings.builder()
            .setOptionSorting(OptionSorting.SORT_BY_DEFAULTS)
            .build()
    };
  }

  private static void ensureDefaultsLoaded() throws IOException {
    if (bundledConfigDefaults == null) {
      bundledConfigDefaults = readResource(CONFIG_DEFAULT_RESOURCE);
    }

    if (bundledChannelDefaults == null) {
      bundledChannelDefaults = readResource(CHANNEL_DEFAULT_RESOURCE);
    }
  }

  private static byte[] readResource(String resourceName) throws IOException {
    try (InputStream input = ProxyChatYamlDocuments.class.getClassLoader().getResourceAsStream(resourceName)) {
      if (input == null) {
        throw new IOException("Missing bundled resource: " + resourceName);
      }

      return input.readAllBytes();
    }
  }

  private static InputStream configDefaultsStream() {
    return new ByteArrayInputStream(bundledConfigDefaults);
  }

  private static InputStream channelDefaultsStream() {
    return new ByteArrayInputStream(bundledChannelDefaults);
  }
}
