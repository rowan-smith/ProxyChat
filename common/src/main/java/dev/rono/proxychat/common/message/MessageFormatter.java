package dev.rono.proxychat.common.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class MessageFormatter {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    public static Component legacy(String message) {
        return LEGACY.deserialize(message);
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
