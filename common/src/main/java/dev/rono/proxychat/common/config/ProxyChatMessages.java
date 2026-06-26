package dev.rono.proxychat.common.config;

import java.util.Map;

/**
 * Built-in defaults for config keys added after a server first generated {@code config.yml}.
 * Runtime reads should use {@link #resolve(ProxyChatYaml, String)} so older files stay valid.
 */
public final class ProxyChatMessages {
    public static final Map<String, String> DEFAULTS = Map.ofEntries(
            Map.entry("toggle-enable-message", "&aYou have toggled &2on &a%chat-name%"),
            Map.entry("toggle-disable-message", "&aYou have toggled &4off &a%chat-name%"),
            Map.entry("toggle-unsupported-message", "&cToggle chat is unavailable on this proxy for your client version. Use /%command-name% <message> instead."),
            Map.entry("ignore-enable-message", "&cYou have ignored %chat-name%!"),
            Map.entry("ignore-disable-message", "&aYou have un-ignored %chat-name%!"),
            Map.entry("chat-disabled-message", "&cYou cannot send a message while %chat-name% is ignored!"),
            Map.entry("command-cooldown-message", "&cChat on cooldown for &e%chat-cooldown% &csecond(s)!"),
            Map.entry("console-disabled-message", "&cYou have to be a player to use this command!"),
            Map.entry("reload-message", "&aConfiguration reloaded!"),
            Map.entry("reload-permission", "proxychat.reload"),
            Map.entry("signed-chat-interception", "auto"),
            Map.entry("prefix", "&2ProxyChat » "),
            Map.entry("help-header", "&7ProxyChat commands:"),
            Map.entry("help-reload", "&7/proxychat reload &8- Reload configuration and channels"),
            Map.entry("help-version", "&7/proxychat version &8- Plugin information"),
            Map.entry("help-channels-header", "&7Chat Channels:")
    );

    public static String resolve(ProxyChatYaml config, String key) {
        String value = config.getString(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }

        return DEFAULTS.getOrDefault(key, "");
    }
}
