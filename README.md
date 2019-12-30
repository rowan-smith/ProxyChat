# ProxyChat
A chat system for Bungeecord and Waterfall!

### Features

* Talk to players on other servers
* Toggleable chat! So you don't have to keep typing the command!
* Ignorable chats! So you can concentrate without seeing the chat!
* Fully customisable messages, commands and permissions!
* Add new chat's and permissions!
* Cooldown on messages and permission overrides!
* Alternative prefix like #message, instead of /<command>
* Tab completion of toggle and ignore!
* Color messages with permission override!
* Toggleable console chat!
* Log chat to console!

### Coming Soon!

* Per server chats!
* Join and Leave messages!
* Switch server messages!

### Commands

By default these commands are included in the config:
```
Staff Chat
- Commands: /staffchat & /sc
- Permission: proxychat.staff

Global Chat
- Commands: /global & /g
- Permission: proxychat.global

Shout:
- Commands: /shout & /s
- Permissions: proxychat.shout
```

### Resources

#### Placeholders:

```
%player% - The player's name.
%prefix% - The prefix in the config.
%server% - The server the player is on.
%command-name% - The name of the chat used.
%command-alias% - The alias of the chat used.
%command-prefix% - The prefix of the chat used.
%chat-name% - The name of the chat used.
%message% - The message sent.
```

#### Configuration:

```yaml
############################################################
# +------------------------------------------------------+ #
# |                      Proxy Chat                      | #
# +------------------------------------------------------+ #
############################################################

# Prefix used in front of all messages
prefix: "&2ProxyChat » "

# This is what is shown when you toggle a command.
toggle-enable-message: "&aYou have toggled &2on &a%chat-name%"
toggle-disable-message: "&aYou have toggled &4off &a%chat-name%"

# This is what is shown when is ignore messages from a chat.
ignore-enable-message: "&cYou have ignored %chat-name%!"
ignore-disable-message: "&aYou have un-ignored %chat-name%!"

# This is what is shown when you try chat in a channel when ignored.
chat-disabled-message: "&cYou cannot send a message while %chat-name% is ignored!"

# This is what message is shown when you use a command too quickly.
command-cooldown-message: "&cChat on cooldown for &e%chat-cooldown% &csecond(s)!"

# Message sent to console when disabled.
console-disabled-message: "&cYou have to be a player to use this command!"

# Reload information
reload-permission: "proxychat.reload"
reload-message: "&aConfiguration reloaded!"


############################################################
# +------------------------------------------------------+ #
# |                        Chats                         | #
# +------------------------------------------------------+ #
############################################################
chats:
  1:
    # This is the name of the chat, used in messages.
    chat-name: "Staff Chat"

    # This is the name of the command.
    command-name: "staffchat"

    # You need this permission to use the command above.
    permission: "proxychat.staff"

    # You can also use this command.
    command-alias: "SC"

    # This is an alternative to using the command.
    command-prefix: "!"
    use-command-prefix: false

    # Log the chat to console so you see chat in console
    log-chat-to-console: true

    # This is a delay on how quickly you can use the command!
    # This is in milliseconds, 1 second = 1000 milliseconds
    comand-delay: 0

    # This overrides the command delay.
    command-delay-override-permission: "proxychat.staff.override"

    # This shows when you enter command wrong.
    invalid-args: "&c/%command-name% <message>"

    # This is the format the chat will show in chat.
    format: "&8[&c%command-alias%&8] [&c%server%&8] &c%player% &8» &7%message%"

    # This is the format when talking from console
    console-format: "&8[&c%command-alias%&8] &c%player% &8» &7%message%"
    console-chat-allowed: true

    # Add this permission so users can use color codes in messages.
    use-color-in-chat-permission: "proxychat.staff.color"

    # Toggle the chat so you don't need commands!
    toggleable: true

    # Ignore the chat so you can't send or receive messages!
    ignorable: false
  2:
    chat-name: "Global Chat"
    command-name: "global"
    permission: "proxychat.global"
    command-alias: "G"
    command-prefix: "@"
    log-chat-to-console: false
    command-delay: 5000
    command-delay-override-permission: "proxychat.global.override"
    use-command-prefix: true
    invalid-args: "&c/%command-name% <message> or %command-prefix%<message>"
    format: "&8[&9%command-alias%&8] [&9%server%&8] &9%player% &8» &7%message%"
    console-format: "&8[&9%command-alias%&8] &9%player% &8» &7%message%"
    console-chat-allowed: false
    use-color-in-chat-permission: "proxychat.global.color"
    toggleable: true
    ignorable: true
  3:
    chat-name: "Shout"
    command-name: "shout"
    permission: "proxychat.shout"
    command-alias: "S"
    command-prefix: "#"
    log-chat-to-console: false
    command-delay: 10000
    command-delay-override-permission: "proxychat.shout.override"
    use-command-prefix: false
    invalid-args: "&c/%command-name% <message>"
    format: "&8[&e%command-alias%&8] [&e%server%&8] &e%player% &8» &7%message%"
    console-format: "&8[&e%command-alias%&8] &e%player% &8» &7%message%"
    console-chat-allowed: false
    use-color-in-chat-permission: "proxychat.shout.color"
    toggleable: false
    ignorable: false

version: 1
```

### Issues

If you have any issues, before giving a bad rating please report them here:
https://github.com/rowan-smith/ProxyChat/issues