package dev.rono.proxychat.common.message;

import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import static org.assertj.core.api.Assertions.assertThat;

class MessageFormatterTest {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    @Test
    void deserializesLegacyAmpersandColors() {

        // arrange & act
        String rendered = PLAIN.serialize(MessageFormatter.legacy("&aHello &cWorld"));

        // assert
        assertThat(rendered).isEqualTo("Hello World");
    }

    @Test
    void stripsLegacyColorsFromPlainText() {

        // arrange & act & assert
        assertThat(MessageFormatter.stripLegacyColors("&a&lColored")).isEqualTo("Colored");
    }

    @Test
    void leavesPlainTextUntouched() {

        // arrange & act & assert
        assertThat(MessageFormatter.stripLegacyColors("plain message")).isEqualTo("plain message");
    }

    @Test
    void serializesLegacyTextWithSectionSigns() {

        // arrange & act
        String serialized = MessageFormatter.legacySectionSerialized("&8[&9G&8] &9Alice");

        // assert
        assertThat(serialized).doesNotContain("&");
        assertThat(serialized).contains("§");
        assertThat(serialized).contains("Alice");
    }
}
