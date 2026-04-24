# Building ProxyChat

## Multi-Module Maven Project

ProxyChat is organized as a multi-module Maven project that produces platform-specific JAR files for different proxy servers.

### Project Structure

```
ProxyChat/
├── pom.xml                      # Parent POM - defines shared configuration
├── core/                         # Shared utilities and interfaces
│   ├── pom.xml
│   └── src/main/kotlin/
│       └── dev/rono/proxychat/core/
│           ├── ProxyChatInstance.kt      # Base interface
│           └── utils/
│               ├── ToggleUtils.kt        # Chat toggle management
│               ├── CooldownRunnable.kt   # Command cooldown handler
│               └── Helpers.kt            # Configuration utilities
│
├── api/                          # Public API and contracts
│   ├── pom.xml
│   └── src/main/kotlin/
│       └── dev/rono/proxychat/api/
│           └── ProxyChatPlugin.kt
│
├── bungee/                       # BungeeCord/Waterfall implementation
│   ├── pom.xml
│   ├── src/main/kotlin/
│   │   └── dev/rono/proxychat/bungee/
│   │       ├── ProxyChatBungee.kt         # Main plugin class
│   │       ├── commands/
│   │       │   ├── ChatCommand.kt         # Chat command handler
│   │       │   └── ProxyChatCommand.kt    # /proxychat command
│   │       └── listeners/
│   │           └── PlayerChatEvent.kt     # Chat event handler
│   └── src/main/resources/
│       ├── bungee.yml                    # Plugin metadata
│       ├── config.yml                    # Main configuration
│       └── global.yml                    # Default chat configuration
│
└── velocity/                     # Velocity implementation
    ├── pom.xml
    ├── src/main/kotlin/
    │   └── dev/rono/proxychat/velocity/
    │       └── ProxyChatVelocity.kt       # Main plugin class
    └── src/main/resources/
        └── velocity-plugin.json          # Plugin metadata
```

## Prerequisites

- Java 8 or higher
- Maven 3.6.0 or higher
- Git (optional, for version control)

## Building

### Build All Modules

```bash
mvn clean package
```

This command will:
1. Clean any previous builds
2. Compile all modules in order (core → api → bungee → velocity)
3. Package each module into a JAR file
4. Output two deployable JAR files

### Output JAR Files

After a successful build, the following JAR files will be created:

```
bungee/target/ProxyChat-Bungee-1.6.0.jar       (for BungeeCord/Waterfall)
velocity/target/ProxyChat-Velocity-1.6.0.jar   (for Velocity)
```

### Building Individual Modules

To build a specific module:

```bash
# Build only core
mvn -pl core clean package

# Build only bungee
mvn -pl bungee clean package

# Build only velocity
mvn -pl velocity clean package

# Build bungee and its dependencies
mvn -am -pl bungee clean package
```

### Build Options

**Skip Tests** (if any exist):
```bash
mvn clean package -DskipTests
```

**Offline Build** (uses cached dependencies):
```bash
mvn -o clean package
```

**Verbose Output**:
```bash
mvn clean package -X
```

## Module Dependencies

- **core**: No internal dependencies
- **api**: Depends on core
- **bungee**: Depends on core and api, BungeeCord API (provided)
- **velocity**: Depends on core and api, Velocity API (provided)

## Deployment

### BungeeCord/Waterfall

1. Copy `ProxyChat-Bungee-1.6.0.jar` to your `plugins/` folder
2. Restart your BungeeCord/Waterfall server
3. Configuration files will be created in `plugins/ProxyChat/`

### Velocity

1. Copy `ProxyChat-Velocity-1.6.0.jar` to your `plugins/` folder
2. Restart your Velocity server
3. Configuration files will be created in `plugins/proxychat/`

## Troubleshooting

### Build Fails: "Cannot find module"

Ensure all parent modules are being built:
```bash
mvn clean package -am
```

### Compilation Errors in IDE

Regenerate IDE project files:
```bash
# For IntelliJ IDEA
File → Invalidate Caches / Restart

# For Eclipse
mvn eclipse:eclipse

# For VS Code with Maven extension
Run Maven build → clean package
```

### Missing Dependencies

Update Maven cache:
```bash
mvn -U clean package
```

## Development

### Adding New Features

1. Core utilities → add to `core/` module
2. API interfaces → add to `api/` module
3. Platform-specific implementation → add to `bungee/` or `velocity/` module

### Running Single Server Tests

Each module can be built independently while keeping dependencies in the reactor:
```bash
mvn -am -pl bungee clean package
```

This builds core, api, and bungee with their dependencies resolved correctly.
