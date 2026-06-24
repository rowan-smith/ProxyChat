package dev.rono.proxychat.common.config.migration;

import java.util.Objects;

/**
 * Semantic config schema version ({@code major} or {@code major.minor}).
 * <p>
 * History:
 * <ul>
 *   <li>{@code 0} — BungeeChat era</li>
 *   <li>{@code 1} — ProxyChat v1.0 rename</li>
 *   <li>{@code 1.1}–{@code 1.5} — ProxyChat v1.x releases (inline {@code chats:})</li>
 *   <li>{@code 2} — ProxyChat v2.0 config schema (per-channel {@code chats/*.yml} files)</li>
 *   <li>{@code 2.1} — channel {@code local} / {@code blacklist} defaults</li>
 *   <li>{@code 2.2+} — signed chat policy, admin help, and later minor additions</li>
 * </ul>
 */
public final class ConfigVersion implements Comparable<ConfigVersion> {
    public static final ConfigVersion LATEST = parse("2.2");

    private final int major;
    private final int minor;

    private ConfigVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    public static ConfigVersion of(int major, int minor) {
        return new ConfigVersion(major, minor);
    }

    public static ConfigVersion parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return of(0, 0);
        }

        String normalized = raw.trim().replace("\"", "").replace("'", "");
        if (!normalized.contains(".")) {
            try {
                return of(Integer.parseInt(normalized), 0);
            } catch (NumberFormatException exception) {
                return of(0, 0);
            }
        }

        String[] parts = normalized.split("\\.", 2);
        return of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    /**
     * Maps integer {@code version} keys written by the pre-semver migrator (v1.4.5 era).
     */
    public static ConfigVersion fromLegacyInteger(int legacy) {
        return switch (legacy) {
            case 0 -> of(0, 0);
            case 1 -> of(1, 0);
            case 2 -> of(1, 1);
            case 3 -> of(2, 0);
            case 4 -> of(2, 1);
            default -> of(legacy, 0);
        };
    }

    /**
     * Interprets a stored {@code version} value, including legacy integer keys from v1.4.5 configs.
     */
    public static ConfigVersion resolveStored(String raw, boolean hasSignedChatInterception, boolean hasV2_1Keys) {
        if (raw == null || raw.isBlank()) {
            return of(0, 0);
        }

        String normalized = raw.trim().replace("\"", "").replace("'", "");
        if (!normalized.contains(".")) {
            try {
                int legacy = Integer.parseInt(normalized);
                if (legacy == 2 && !hasSignedChatInterception) {
                    return of(1, 1);
                }

                if (legacy == 3) {
                    if (hasSignedChatInterception || hasV2_1Keys) {
                        return of(2, 2);
                    }

                    return of(2, 0);
                }

                if (legacy == 4) {
                    return of(2, 2);
                }
            } catch (NumberFormatException ignored) {
                return of(0, 0);
            }
        }

        return remapDeprecatedSchema(parse(raw));
    }

    /**
     * Maps short-lived schema IDs ({@code 1.6}/{@code 1.7}) to the v2 chain.
     */
    public static ConfigVersion remapDeprecatedSchema(ConfigVersion version) {
        if (version.equals(of(1, 6))) {
            return of(2, 0);
        }

        if (version.equals(of(1, 7))) {
            return of(2, 1);
        }

        return version;
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    @Override
    public String toString() {
        if (minor == 0) {
            return Integer.toString(major);
        }

        return major + "." + minor;
    }

    @Override
    public int compareTo(ConfigVersion other) {
        if (major != other.major) {
            return Integer.compare(major, other.major);
        }

        return Integer.compare(minor, other.minor);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof ConfigVersion that)) {
            return false;
        }

        return major == that.major && minor == that.minor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor);
    }
}
