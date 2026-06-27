package dev.rono.proxychat.common.config.migration;

import java.util.ArrayList;

import lombok.experimental.UtilityClass;

import dev.dejvokep.boostedyaml.dvs.Pattern;
import dev.dejvokep.boostedyaml.dvs.segment.Segment;

/**
 * BoostedYAML version pattern aligned with public ProxyChat releases:
 * {@code 0} (BungeeChat) → {@code 1} → {@code 1.1}…{@code 1.5} → {@code 2} → {@code 2.1+}.
 */
@UtilityClass
public class ConfigVersionPattern {
    public static final String VERSION_ROUTE = "version";

    private static final Pattern PATTERN = new Pattern(
            Segment.literal(buildVersionIds())
    );

    public static Pattern pattern() {
        return PATTERN;
    }

    private static String[] buildVersionIds() {
        var ids = new ArrayList<>();
        ids.add("0");
        ids.add("1");

        for (var minor = 1; minor <= 5; minor++) {
            ids.add("1." + minor);
        }

        ids.add("2");

        for (var minor = 1; minor <= 99; minor++) {
            ids.add("2." + minor);
        }

        return ids.toArray(String[]::new);
    }
}
