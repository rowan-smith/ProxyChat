package dev.rono.proxychat.common.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

import lombok.experimental.UtilityClass;

/**
 * Validates loaded channel YAML before {@link dev.rono.proxychat.common.channel.ChatChannel} construction.
 */
@UtilityClass
public final class ChannelConfigValidator {
    public record ValidatedChannel(ProxyChatYaml config, String sourceName, int sortOrder) { }

    public static List<ValidatedChannel> validateAndOrder(List<ValidatedChannel> channels, Logger logger) {
        var valid = new ArrayList<ValidatedChannel>();
        var reservedCommands = new HashSet<String>();
        var prefixes = new HashMap<>();

        for (ValidatedChannel channel : channels) {
            var commandName = normalizeKey(channel.config().getString("command-name"));
            if (commandName.isEmpty()) {
                logger.warning("Skipping " + channel.sourceName() + ": missing command-name.");
                continue;
            }

            var alias = normalizeKey(channel.config().getString("command-alias"));
            if (isDuplicate(reservedCommands, commandName)) {
                logger.warning("Skipping " + channel.sourceName() + ": duplicate command-name '" + commandName + "'.");
                continue;
            }

            if (!alias.isEmpty() && isDuplicate(reservedCommands, alias)) {
                logger.warning("Skipping " + channel.sourceName() + ": duplicate command-alias '" + alias + "'.");
                continue;
            }

            reservedCommands.add(commandName);
            if (!alias.isEmpty()) {
                reservedCommands.add(alias);
            }

            if (channel.config().getBoolean("use-command-prefix")) {
                var prefix = channel.config().getString("command-prefix");
                if (prefix != null && !prefix.isBlank()) {
                    var normalizedPrefix = prefix.trim();
                    var existing = prefixes.get(normalizedPrefix);
                    if (existing != null) {
                        logger.warning("Skipping " + channel.sourceName() + ": duplicate command-prefix '"
                                + normalizedPrefix + "' (already used by " + existing + ").");
                        continue;
                    }

                    prefixes.put(normalizedPrefix, channel.sourceName());
                }
            }

            valid.add(channel);
        }

        valid.sort((left, right) -> {
            var priorityCompare = Integer.compare(right.config().getInt("priority"), left.config().getInt("priority"));
            if (priorityCompare != 0) {
                return priorityCompare;
            }

            return Integer.compare(left.sortOrder(), right.sortOrder());
        });

        return valid;
    }

    private static boolean isDuplicate(Set<String> reserved, String key) {
        return !key.isEmpty() && reserved.contains(key);
    }

    private static String normalizeKey(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }
}
