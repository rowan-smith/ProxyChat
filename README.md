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
* **Adventure** components for outbound messages (`&` colour codes)

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
```

## Issues

https://github.com/rowan-smith/ProxyChat/issues
