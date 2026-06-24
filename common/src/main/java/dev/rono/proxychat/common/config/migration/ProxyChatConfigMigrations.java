package dev.rono.proxychat.common.config.migration;

import dev.rono.proxychat.common.config.ProxyChatMessages;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Structural config migrations applied by BoostedYAML custom logic (not handled by defaults merge).
 * <p>
 * v1 release chain: {@code 0} → {@code 1} → {@code 1.1} … {@code 1.5}.
 * v2 starts at {@code 2} with the {@code chats/} folder split.
 */
public final class ProxyChatConfigMigrations {
  private static final String LEGACY_COOLDOWN_MESSAGE = "&cThis command is on cooldown!";

  /** {@code 0} → {@code 1}: BungeeChat era flattening and placeholder rename. */
  public static void migrateV0ToV1(Section document) {
    replaceInAllStrings(document, "%chat%", "%chat-name%");
    flattenReloadSection(document);
    document.remove("global-layout");
    document.remove("use-global-layout");
    setIfAbsent(document, "reload-permission", ProxyChatMessages.DEFAULTS.get("reload-permission"));
    setIfAbsent(document, "reload-message", ProxyChatMessages.DEFAULTS.get("reload-message"));
  }

  /** {@code 1} → {@code 1.1}: BungeeChat inline channel schema → BungeeCord Chat channel schema. */
  public static void migrateV1ToV1_1(Section document) {
    forEachChannel(document, ProxyChatConfigMigrations::migrateLegacyChannelSchema);
  }

  /** {@code 1.1} → {@code 1.2}: command cooldown message and per-channel prefix/delay keys. */
  public static void migrateV1_1ToV1_2(Section document) {
    setIfAbsent(document, "command-cooldown-message", ProxyChatMessages.DEFAULTS.get("command-cooldown-message"));
    forEachChannel(document, channel -> {
      setIfAbsent(channel, "command-prefix", "");
      setIfAbsent(channel, "use-command-prefix", false);
      migrateCommandDelayKey(channel);
      derivePermissionSuffix(channel, "command-delay-override-permission", ".override");
      derivePermissionSuffix(channel, "use-color-in-chat-permission", ".color");
    });
  }

  /** {@code 1.2} → {@code 1.3}: drop global layout, console messaging, and console channel formats. */
  public static void migrateV1_2ToV1_3(Section document) {
    document.remove("global-layout");
    document.remove("use-global-layout");
    setIfAbsent(document, "console-disabled-message", ProxyChatMessages.DEFAULTS.get("console-disabled-message"));

    String cooldownMessage = document.getString("command-cooldown-message");
    if (LEGACY_COOLDOWN_MESSAGE.equals(cooldownMessage)) {
      document.set("command-cooldown-message", ProxyChatMessages.DEFAULTS.get("command-cooldown-message"));
    }

    forEachChannel(document, channel -> {
      setIfAbsent(channel, "log-chat-to-console", false);
      setIfAbsent(channel, "console-chat-allowed", false);
      if (!channel.contains("console-format")) {
        String format = channel.getString("format");
        if (format != null) {
          channel.set("console-format", format.replace("[%server%] ", "").replace("[%server%]", ""));
        }
      }
    });
  }

  /** {@code 1.3} → {@code 1.4}: BungeeCord Chat → ProxyChat rename and flat reload keys. */
  public static void migrateV1_3ToV1_4(Section document) {
    flattenReloadSection(document);
    replaceInAllStrings(document, "bungeecordchat", "proxychat");
    replaceInAllStrings(document, "BungeecordChat", "ProxyChat");
    replaceInAllStrings(document, "&2BungeecordChat » ", ProxyChatMessages.DEFAULTS.get("prefix"));
    setIfAbsent(document, "prefix", ProxyChatMessages.DEFAULTS.get("prefix"));
    setIfAbsent(document, "reload-permission", ProxyChatMessages.DEFAULTS.get("reload-permission"));
    setIfAbsent(document, "reload-message", ProxyChatMessages.DEFAULTS.get("reload-message"));
  }

  /** {@code 1.4} → {@code 1.5}: local channel flag and {@code comand-delay} typo fix. */
  public static void migrateV1_4ToV1_5(Section document) {
    forEachChannel(document, ProxyChatConfigMigrations::normalizeChannelKeys);
  }

  /** {@code 1.5} → {@code 2}: inline {@code chats} → {@code chats/*.yml} (start of config v2). */
  public static void migrateInlineChatsToFolder(YamlDocument document, Path dataDirectory, Logger logger) throws IOException {
    Section chats = document.getSection("chats");
    if (chats == null) {
      return;
    }

    Path chatsDirectory = dataDirectory.resolve("chats");
    Files.createDirectories(chatsDirectory);

    for (Object keyObject : chats.getKeys()) {
      String key = String.valueOf(keyObject);
      Section channel = chats.getSection(key);
      if (channel == null) {
        continue;
      }

      String commandName = channel.getString("command-name");
      if (commandName == null || commandName.isEmpty()) {
        continue;
      }

      normalizeChannelKeys(channel);
      migrateV2ToV2_1(channel);

      Path target = chatsDirectory.resolve(commandName + ".yml");
      if (Files.exists(target)) {
        continue;
      }

      try {
        YamlDocument channelDocument = YamlDocument.create(new ByteArrayInputStream(new byte[0]));
        copySection(channel, channelDocument);
        Files.writeString(target, channelDocument.dump());
        logger.info("Migrated chats/" + commandName + ".yml");

      } catch (Exception exception) {
        logger.log(Level.WARNING, "Failed to migrate chats/" + commandName + ".yml", exception);
      }
    }

    document.remove("chats");
  }

  /** {@code 2} → {@code 2.1}: per-channel {@code local} and {@code blacklist} defaults. */
  public static void migrateV2ToV2_1(Section document) {
    forEachChannel(document, channel -> {
      normalizeChannelKeys(channel);
      setIfAbsent(channel, "local", false);
      if (!channel.contains("blacklist")) {
        channel.set("blacklist", new ArrayList<String>());
      }
    });
  }

  /** {@code 2.1} → {@code 2.2}: signed chat policy and admin help messages. */
  public static void migrateV2_1ToV2_2(Section document) {
    migrateV2_0Keys(document);
    migrateV2_1Keys(document);
  }

  public static void migrateV2_0Keys(Section document) {
    setIfAbsent(document, "signed-chat-interception", "auto");
  }

  public static void ensureLatestKeys(Section document) {
    migrateV2_1ToV2_2(document);
  }

  public static void migrateV2_1Keys(Section document) {
    setIfAbsent(document, "toggle-unsupported-message", ProxyChatMessages.DEFAULTS.get("toggle-unsupported-message"));
    setIfAbsent(document, "help-header", ProxyChatMessages.DEFAULTS.get("help-header"));
    setIfAbsent(document, "help-reload", ProxyChatMessages.DEFAULTS.get("help-reload"));
    setIfAbsent(document, "help-version", ProxyChatMessages.DEFAULTS.get("help-version"));
    setIfAbsent(document, "help-channels-header", ProxyChatMessages.DEFAULTS.get("help-channels-header"));
  }

  private static void migrateLegacyChannelSchema(Section channel) {
    if (channel.contains("alias") && !channel.contains("command-alias")) {
      channel.set("command-alias", channel.getString("alias"));
      channel.remove("alias");
    }

    channel.remove("chat-prefix");

    if (!channel.contains("chat-name")) {
      String commandName = channel.getString("command-name");
      if (commandName != null && !commandName.isEmpty()) {
        channel.set("chat-name", titleCase(commandName));
      }
    }

    replaceInSectionStrings(channel, "%chat-prefix%", "%command-alias%");
    replaceInSectionStrings(channel, "%alias%", "%command-name%");
  }

  private static void migrateCommandDelayKey(Section channel) {
    if (channel.contains("comand-delay") && !channel.contains("command-delay")) {
      channel.set("command-delay", channel.get("comand-delay"));
    }

    channel.remove("comand-delay");
    setIfAbsent(channel, "command-delay", 0);
  }

  private static void normalizeChannelKeys(Section channel) {
    migrateCommandDelayKey(channel);
    setIfAbsent(channel, "local", false);
  }

  private static void flattenReloadSection(Section document) {
    Section reload = document.getSection("reload");
    if (reload == null) {
      return;
    }

    String permission = reload.getString("permission");
    String reloadMessage = reload.getString("reload-message");

    if (permission != null && !document.contains("reload-permission")) {
      document.set("reload-permission", permission);
    }

    if (reloadMessage != null && !document.contains("reload-message")) {
      document.set("reload-message", reloadMessage);
    }

    document.remove("reload");
  }

  private static void derivePermissionSuffix(Section channel, String targetKey, String suffix) {
    if (channel.contains(targetKey)) {
      return;
    }

    String permission = channel.getString("permission");
    if (permission == null || permission.isEmpty()) {
      return;
    }

    channel.set(targetKey, permission + suffix);
  }

  private static void forEachChannel(Section document, Consumer<Section> action) {
    Section chats = document.getSection("chats");
    if (chats == null) {
      return;
    }

    for (Object keyObject : chats.getKeys()) {
      String key = String.valueOf(keyObject);
      Section channel = chats.getSection(key);
      if (channel != null) {
        action.accept(channel);
      }
    }
  }

  private static String titleCase(String commandName) {
    String[] parts = commandName.split("[-_]");
    StringBuilder builder = new StringBuilder();
    for (String part : parts) {
      if (part.isEmpty()) {
        continue;
      }

      if (!builder.isEmpty()) {
        builder.append(' ');
      }

      builder.append(Character.toUpperCase(part.charAt(0)));
      if (part.length() > 1) {
        builder.append(part.substring(1).toLowerCase(Locale.ROOT));
      }
    }

    return builder.isEmpty() ? commandName : builder.toString();
  }

  private static void copySection(Section source, Section target) {
    for (Object keyObject : source.getKeys()) {
      String key = String.valueOf(keyObject);
      target.set(key, source.get(key));
    }
  }

  private static void replaceInSectionStrings(Section section, String from, String to) {
    replaceInAllStrings(section, from, to);
  }

  private static void replaceInAllStrings(Section section, String from, String to) {
    for (Object keyObject : section.getKeys()) {
      String key = String.valueOf(keyObject);
      Object value = section.get(key);
      if (value instanceof String string && string.contains(from)) {
        section.set(key, string.replace(from, to));
        continue;
      }

      Section nested = section.getSection(key);
      if (nested != null) {
        replaceInAllStrings(nested, from, to);
      }
    }
  }

  private static void setIfAbsent(Section section, String route, Object value) {
    if (!section.contains(route)) {
      section.set(route, value);
    }
  }
}
