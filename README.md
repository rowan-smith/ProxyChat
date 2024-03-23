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

By default, this example is included:
```
Global Chat
- Commands: /global & /g
- Permission: proxychat.global
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

#### Configuration (config.yml):

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

# This is what is shown when is ignored messages from a chat.
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

version: 2
```

#### Example Chat (global.yml):

```yaml
# This is the name of the chat, used in messages.
chat-name: "Global Chat"

# This is the name of the command.
command-name: "global"

# You need this permission to use the command above.
permission: "proxychat.global"

# You can also use this command.
command-alias: "G"

# This is an alternative to using the command.
command-prefix: "@"
use-command-prefix: true

# Log the chat to console, so you see chat in console
log-chat-to-console: false

# This is a delay on how quickly you can use the command!
# This is in milliseconds, 1 second = 1000 milliseconds
command-delay: 5000

# This overrides the command delay.
command-delay-override-permission: "proxychat.global.override"

# This shows when you enter command wrong.
invalid-args: "&c/%command-name% <message> or %command-prefix%<message>"

# This is the format the chat will show in chat.
format: "&8[&9%command-alias%&8] [&9%server%&8] &9%player% &8» &7%message%"

# This is the format when talking from console
console-format: "&8[&9%command-alias%&8] &9%player% &8» &7%message%"
console-chat-allowed: false

# Add this permission so users can use color codes in messages.
use-color-in-chat-permission: "proxychat.global.color"

# Toggle the chat so you don't need commands!
toggleable: true

# Ignore the chat, so you can't send or receive messages!
ignorable: true

# Is this a local server chat
local: false

# blacklist this chat on specific servers
blacklist:
  - server1
```

### Issues

If you have any issues, before giving a bad rating please report them here:
https://github.com/rowan-smith/ProxyChat/issues
