package dev.rono.proxychat.common.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MessageFormatter {
    public enum Format {
        LEGACY,
        MINIMESSAGE
    }

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public static Component format(String message, Format format) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }

        if (format == Format.MINIMESSAGE) {
            return MINI_MESSAGE.deserialize(message);
        }

        return LEGACY.deserialize(message);
    }

    public static Component legacy(String message) {
        return format(message, Format.LEGACY);
    }

    public static Format parseFormat(String raw) {
        if (raw != null && raw.equalsIgnoreCase("minimessage")) {
            return Format.MINIMESSAGE;
        }

        return Format.LEGACY;
    }

    /**
     * Serializes {@code &}-style legacy text for signed-chat rewrites on backends (Paper expects {@code §}).
     */
    public static String legacySectionSerialized(String ampersandLegacy) {
        return LEGACY_SECTION.serialize(LEGACY.deserialize(ampersandLegacy));
    }

    public static String stripLegacyColors(String message) {
        return PLAIN.serialize(LEGACY.deserialize(message));
    }
}
