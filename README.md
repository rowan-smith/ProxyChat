# ProxyChat

Cross-server proxy chat for **BungeeCord**, **Waterfall**, and **Velocity**.

## Downloads

| Proxy                  | JAR                        | Install location |
|------------------------|----------------------------|------------------|
| Waterfall / BungeeCord | `ProxyChat-Bungee-*.jar`   | `plugins/`       |
| Velocity               | `ProxyChat-Velocity-*.jar` | `plugins/`       |

Requires **Java 17+** on the proxy.

## Features

* Cross-server and local (same-backend) channels
* Per-channel YAML configs in `plugins/ProxyChat/chats/`
* Custom commands, aliases, permissions, and message formats
* Prefix chat (`@message`) and toggle mode (plain chat routes to a channel)
* Toggle on/off and ignore send/receive per channel
* Command cooldowns with permission bypass
* Colour codes in chat (permission-gated)
* Console chat and console logging (per channel)
* Server blacklist per channel
* Tab completion for `toggle` and `ignore`
* Placeholder replacement in messages
* Legacy `chats:` section in `config.yml` auto-migrates to `chats/*.yml`
* **Adventure** components for outbound messages (`&` colour codes or MiniMessage)
* Channel priority, duplicate-config validation, blacklist feedback, and message length limits
* Optional toggle/ignore persistence across reconnects

## Global config (`config.yml`)

| Key                          | Default          | Description                                               |
|------------------------------|------------------|-----------------------------------------------------------|
| `prefix`                     | `&2ProxyChat » ` | Prepended to most plugin messages                         |
| `signed-chat-interception`   | `auto`           | `auto`, `never`, or `always`                              |
| `blacklist-message`          | (see file)       | Sent when a player uses a channel on a blacklisted server |
| `max-message-length`         | `256`            | Max chat body length (`0` disables)                       |
| `message-too-long-message`   | (see file)       | Sent when a message exceeds the limit                     |
| `persist-player-preferences` | `false`          | Remember toggle/ignore across reconnects                  |
| `message-format`             | `legacy`         | `legacy` (`&` codes) or `minimessage`                     |

## Channel config (`chats/*.yml`)

Each file defines one channel. Important keys:

| Key                        | Description                                      |
|----------------------------|--------------------------------------------------|
| `command-name`             | Primary command (required, must be unique)       |
| `command-alias`            | Optional alias (must be unique)                  |
| `command-prefix`           | Prefix for `@message` style chat                 |
| `use-command-prefix`       | Enable prefix intercept                          |
| `priority`                 | Higher values win when multiple channels match   |
| `permission`               | Permission to use the channel                    |
| `format`                   | Outbound chat format                             |
| `local`                    | Restrict delivery to the sender's backend server |
| `blacklist`                | Backend servers where the channel is blocked     |
| `toggleable` / `ignorable` | Enable `/channel toggle` and `/channel ignore`   |
| `command-delay`            | Cooldown in milliseconds                         |


## Development

```
ProxyChat/
├── common/     Shared chat logic, config, formatting, tests
├── bungee/     Waterfall/BungeeCord adapter
├── velocity/   Velocity adapter
└── config/     Checkstyle and SpotBugs rules
```

Run the full suite:

```bash
mvn clean verify
```

## Signed Chat Feature Matrix

ProxyChat intercepts plain chat on the proxy and rebroadcasts formatted **unsigned** Adventure messages. This breaks signed chat on 1.19.3+ clients.

| Feature                                    | BungeeCord                     | Waterfall                 | Velocity                  |
|--------------------------------------------|--------------------------------|---------------------------|---------------------------|
| Prefix intercept (`@message`)              | 1.19.3+ clients: **No** (auto) | `/<prefix>` command only¹ | Yes, with SignedVelocity² |
| Toggle mode (plain chat intercept)         | 1.19.3+ clients: **No** (auto) | **No** on 1.19.3+¹        | Yes, with SignedVelocity² |
| Signed-chat acknowledgement to backend     | No                             | N/A¹                      | Via SignedVelocity²       |

¹ **Waterfall + Paper (typical):** plain `@message` and toggle **cannot** cancel signed chat without kicks. Use `/global hello`, `/g hello`, or `/@ hello` instead. Toggle is unavailable on 1.19.3+ unless you set `signed-chat-interception: always` (not recommended on Paper).

² **[SignedVelocity](https://modrinth.com/plugin/signedvelocity)** must be installed on the **proxy and every backend**. Without it, prefix and toggle interception are skipped for **1.19.1+** clients and a one-time warning is logged.

#### Velocity

Velocity denies cancelled signed chat unless SignedVelocity is present. Install it on the proxy and all backends for prefix/toggle on 1.19.1+.

Plain `@prefix` chat on Velocity uses `ChatResult.message()` (SignedVelocity MODIFY) on the sender's server and proxy-broadcasts to other servers only, so Paper does not show the message twice. `/@prefix` and `/channel` commands still use full proxy broadcast.

## Commands

### Default global channel (from `chats/global.yml`):

```
Global Chat
- Commands: /global & /g
- Permission: proxychat.global (or set to '' for no permission)
- Prefix: @message (when use-command-prefix is true)
- Toggle: /global toggle
- Ignore: /global ignore
```

### Admin:

* `/proxychat reload` — reload config and channels (requires `proxychat.reload`)
* `/proxychat version` — plugin info (aliases: `/pc`)

### Placeholders

```
%player%
%prefix%
%server%
%command-name%
%command-alias%
%command-prefix%
%chat-name%
%message%
%chat-cooldown%
%max-length%
```

## Issues

https://github.com/rowan-smith/ProxyChat/issues
