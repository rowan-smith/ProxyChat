package dev.rono.proxychat.common.channel;

import java.util.List;

import lombok.Getter;

import dev.rono.proxychat.common.config.ProxyChatYaml;
import dev.rono.proxychat.common.util.ToggleUtils;

@Getter
public final class ChatChannel {
    private final String commandName;
    private final String permission;
    private final String commandAlias;
    private final boolean useCommandPrefix;
    private final String commandPrefix;
    private final boolean toggleable;
    private final boolean ignorable;
    private final boolean local;
    private final String chatName;
    private final String format;
    private final String invalidArgs;
    private final String useColorInChatPermission;
    private final int commandDelay;
    private final String commandDelayOverridePermission;
    private final String consoleFormat;
    private final boolean consoleChatAllowed;
    private final boolean logChatToConsole;
    private final List<String> serverBlacklist;
    private final ToggleUtils toggleUtils = new ToggleUtils();

    public ChatChannel(ProxyChatYaml config) {
        this.commandName = config.getString("command-name");
        this.permission = config.getString("permission");
        this.commandAlias = config.getString("command-alias");
        this.useCommandPrefix = config.getBoolean("use-command-prefix");
        String prefix = config.getString("command-prefix");
        this.commandPrefix = prefix == null ? null : prefix.trim();
        this.toggleable = config.getBoolean("toggleable");
        this.ignorable = config.getBoolean("ignorable");
        this.local = config.getBoolean("local");
        this.chatName = config.getString("chat-name");
        this.format = config.getString("format");
        this.invalidArgs = config.getString("invalid-args");
        this.useColorInChatPermission = config.getString("use-color-in-chat-permission");
        this.commandDelay = config.getInt("command-delay");
        this.commandDelayOverridePermission = config.getString("command-delay-override-permission");
        this.consoleFormat = config.getString("console-format");
        this.consoleChatAllowed = config.getBoolean("console-chat-allowed");
        this.logChatToConsole = config.getBoolean("log-chat-to-console");
        this.serverBlacklist = config.getStringList("blacklist");
    }
}
