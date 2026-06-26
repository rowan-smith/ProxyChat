package dev.rono.proxychat.common.message;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageFormatterTest {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    @Test
    void deserializesLegacyAmpersandColors() {
        String rendered = PLAIN.serialize(MessageFormatter.legacy("&aHello &cWorld"));

        assertThat(rendered).isEqualTo("Hello World");
    }

    @Test
    void stripsLegacyColorsFromPlainText() {
        assertThat(MessageFormatter.stripLegacyColors("&a&lColored")).isEqualTo("Colored");
    }

    @Test
    void leavesPlainTextUntouched() {
        assertThat(MessageFormatter.stripLegacyColors("plain message")).isEqualTo("plain message");
    }

    @Test
    void serializesLegacyTextWithSectionSigns() {
        String serialized = MessageFormatter.legacySectionSerialized("&8[&9G&8] &9Alice");

        assertThat(serialized).doesNotContain("&");
        assertThat(serialized).contains("§");
        assertThat(serialized).contains("Alice");
    }
}
