package dev.rono.proxychat.common.config.migration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigVersionTest {

    @Test
    void parsesSemanticVersions() {

        // arrange & act & assert
        assertThat(ConfigVersion.parse("2.1")).isEqualTo(ConfigVersion.of(2, 1));
        assertThat(ConfigVersion.parse("1.1")).isEqualTo(ConfigVersion.of(1, 1));
        assertThat(ConfigVersion.parse("2")).isEqualTo(ConfigVersion.of(2, 0));
        assertThat(ConfigVersion.parse("0")).isEqualTo(ConfigVersion.of(0, 0));
    }

    @Test
    void mapsLegacyIntegerVersions() {

        // arrange & act & assert
        assertThat(ConfigVersion.fromLegacyInteger(2)).isEqualTo(ConfigVersion.of(1, 1));
        assertThat(ConfigVersion.fromLegacyInteger(3)).isEqualTo(ConfigVersion.of(2, 0));
        assertThat(ConfigVersion.fromLegacyInteger(4)).isEqualTo(ConfigVersion.of(2, 1));
    }

    @Test
    void mapsDeprecatedSchemaVersions() {

        // arrange & act & assert
        assertThat(ConfigVersion.remapDeprecatedSchema(ConfigVersion.of(1, 6))).isEqualTo(ConfigVersion.of(2, 0));
        assertThat(ConfigVersion.remapDeprecatedSchema(ConfigVersion.of(1, 7))).isEqualTo(ConfigVersion.of(2, 1));
    }

    @Test
    void comparesMajorAndMinor() {

        // arrange & act & assert
        assertThat(ConfigVersion.of(1, 0).compareTo(ConfigVersion.of(1, 1))).isNegative();
        assertThat(ConfigVersion.of(1, 1).compareTo(ConfigVersion.of(2, 0))).isNegative();
        assertThat(ConfigVersion.of(2, 0).compareTo(ConfigVersion.of(2, 1))).isNegative();
    }

    @Test
    void rendersWithoutTrailingZeroMinor() {

        // arrange & act & assert
        assertThat(ConfigVersion.of(2, 0).toString()).isEqualTo("2");
        assertThat(ConfigVersion.of(2, 2).toString()).isEqualTo("2.2");
    }
}
