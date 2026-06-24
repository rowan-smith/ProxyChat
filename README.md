# ProxyChat

Cross-server proxy chat for **Waterfall**, **BungeeCord**, and **Velocity**. Version **2.0.0** is a multi-module build: shared logic in `common`, with thin Bungee and Velocity adapters.

## Downloads

| Proxy | JAR | Install location |
|-------|-----|------------------|
| Waterfall / BungeeCord | `ProxyChat-Bungee-*.jar` | `plugins/` |
| Velocity | `ProxyChat-Velocity-*.jar` | `plugins/` |

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
* **Adventure** components for outbound messages (`&` colour codes)

## Platform feature matrix

Features that work the same everywhere are marked **All**. Features that depend on signed-chat handling list per platform.

| Feature | Waterfall | BungeeCord | Velocity |
|---------|-----------|------------|----------|
| Channel commands (`/global`, `/g`, etc.) | Yes | Yes | Yes |
| Command aliases | Yes | Yes | Yes |
| Cross-server broadcast | Yes | Yes | Yes |
| Local (same-server) channels | Yes | Yes | Yes |
| Toggle / ignore | Yes | Yes | Yes |
| Cooldowns & override permission | Yes | Yes | Yes |
| Colour codes (with permission) | Yes | Yes | Yes |
| Console chat (when enabled per channel) | Yes | Yes | Yes |
| Console logging (when enabled per channel) | Yes | Yes | Yes |
| Server blacklist | Yes | Yes | Yes |
| Tab complete (`toggle`, `ignore`) | Yes | Yes | Yes |
| `/proxychat reload` & `/pc version` | Yes | Yes | Yes |
| Prefix intercept (`@message`) | Yes | 1.19.3+ clients: **No** (auto) | Yes, with SignedVelocity¹ |
| Toggle mode (plain chat intercept) | Yes | 1.19.3+ clients: **No** (auto) | Yes, with SignedVelocity¹ |
| Signed-chat acknowledgement to backend | Yes | No | Via SignedVelocity¹ |

¹ **[SignedVelocity](https://modrinth.com/plugin/signedvelocity)** must be installed on the **proxy and every backend**. Without it, prefix and toggle interception are skipped for **1.19.1+** clients and a one-time warning is logged.

### Signed chat notes

ProxyChat intercepts plain chat on the proxy and rebroadcasts formatted **unsigned** Adventure messages. That is the correct model for custom proxy channels.

**Adventure does not fix signed-chat cancellation.** Cancelling a player's signed chat packet still requires platform-specific acknowledgement (Waterfall) or SignedVelocity (Velocity). Otherwise the client's signature chain breaks and players can be kicked.

#### BungeeCord vs Waterfall

Vanilla BungeeCord lacks Waterfall's `ClientChatAcknowledgement` packet. With default `signed-chat-interception: auto`, prefix and toggle modes are **automatically disabled** for 1.19.3+ clients on BungeeCord. `/channel` commands still work.

| Option | Effect |
|--------|--------|
| `signed-chat-interception: auto` (default) | Intercept only when the platform can handle signed chat safely |
| `signed-chat-interception: never` | Never cancel plain chat; use `/channel` commands only |
| `signed-chat-interception: always` | Always intercept — **not recommended** on vanilla BungeeCord 1.19.3+ |

**Recommended:** use [Waterfall](https://papermc.io/software/waterfall) (drop-in BungeeCord replacement) for full prefix/toggle support.

#### Velocity

Velocity denies cancelled signed chat unless SignedVelocity is present. Install it on the proxy and all backends for prefix/toggle on 1.19.1+.

## Project layout

```
ProxyChat/
├── common/     Shared chat logic, config, Adventure formatting
├── bungee/     Waterfall/BungeeCord adapter
├── velocity/   Velocity adapter
└── pom.xml     Parent POM (proxychat-parent)
```

## Build & test

```bash
mvn clean verify
```

`verify` runs unit tests (`*Test.java`) and integration tests (`*IT.java`), then packages both JARs:

* `bungee/target/ProxyChat-Bungee-2.0.0.jar`
* `velocity/target/ProxyChat-Velocity-2.0.0.jar`

## Commands

Default global channel (from `chats/global.yml`):

```
Global Chat
- Commands: /global & /g
- Permission: proxychat.global
- Prefix: @message (when use-command-prefix is true)
- Toggle: /global toggle
- Ignore: /global ignore
```

Admin:

* `/proxychat reload` — reload config and channels (requires `proxychat.reload`)
* `/proxychat version` — plugin info (aliases: `/pc`)

## Configuration

On first run, `config.yml` and `chats/global.yml` are copied to `plugins/ProxyChat/`. Add more channels as `plugins/ProxyChat/chats/<name>.yml`.

Example files: `bungee/src/main/resources/config.yml` and `global.yml`.

### Global options (`config.yml`)

| Key | Description |
|-----|-------------|
| `prefix` | Prepended to plugin feedback messages |
| `signed-chat-interception` | `auto`, `never`, or `always` — see signed chat section |
| `reload-permission` | Permission for `/proxychat reload` |

### Per-channel options (`chats/*.yml`)

| Key | Description |
|-----|-------------|
| `command-name` / `command-alias` | Command and alias |
| `permission` | Required to send/receive |
| `use-command-prefix` / `command-prefix` | Prefix intercept (e.g. `@`) |
| `toggleable` / `ignorable` | Enable toggle and ignore subcommands |
| `local` | Restrict broadcast to same backend server |
| `format` / `console-format` | Chat format templates |
| `command-delay` | Cooldown in milliseconds |
| `command-delay-override-permission` | Bypass cooldown |
| `use-color-in-chat-permission` | Allow `&` codes in messages |
| `console-chat-allowed` | Allow console to use the channel |
| `log-chat-to-console` | Log formatted messages to proxy console |
| `blacklist` | Server names where the channel is disabled |

### Placeholders

```
%player% %prefix% %server% %command-name% %command-alias%
%command-prefix% %chat-name% %message% %chat-cooldown%
```

## Issues

https://github.com/rowan-smith/ProxyChat/issues
