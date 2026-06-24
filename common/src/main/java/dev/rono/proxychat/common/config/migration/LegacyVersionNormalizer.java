package dev.rono.proxychat.common.config.migration;

import dev.dejvokep.boostedyaml.block.implementation.Section;

/**
 * Maps legacy integer {@code version} keys and missing versions to semantic IDs before BoostedYAML updates.
 */
public final class LegacyVersionNormalizer {
  private LegacyVersionNormalizer() {
  }

  public static void normalize(Section document) {
    if (document.getSection("reload") != null) {
      ProxyChatConfigMigrations.migrateV0ToV1(document);
    }

    ConfigVersion resolved = resolve(document);
    document.set(ConfigVersionPattern.VERSION_ROUTE, resolved.toString());
  }

  private static ConfigVersion resolve(Section document) {
    String storedVersion = document.contains(ConfigVersionPattern.VERSION_ROUTE)
            ? document.getString(ConfigVersionPattern.VERSION_ROUTE)
            : null;

    if (isBungeeChatEra(document, storedVersion)) {
      return ConfigVersion.of(0, 0);
    }

    if (storedVersion != null) {
      return ConfigVersion.resolveStored(
              storedVersion,
              document.contains("signed-chat-interception"),
              document.contains("help-header") || document.contains("toggle-unsupported-message")
      );
    }

    if (document.contains("chats")) {
      return ConfigVersion.of(1, 5);
    }

    if (document.contains("help-header") || document.contains("toggle-unsupported-message")
            || document.contains("signed-chat-interception")) {
      return ConfigVersion.of(2, 2);
    }

    return ConfigVersion.of(2, 0);
  }

  private static boolean isBungeeChatEra(Section document, String storedVersion) {
    if (!document.contains("global-layout")) {
      String toggleMessage = document.getString("toggle-enable-message");
      if (toggleMessage == null || !toggleMessage.contains("%chat%")) {
        return false;
      }
    }

    return storedVersion == null || storedVersion.isBlank() || "1".equals(storedVersion.trim());
  }
}
