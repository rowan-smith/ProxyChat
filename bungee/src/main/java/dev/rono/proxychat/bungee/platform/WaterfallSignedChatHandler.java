package dev.rono.proxychat.bungee.platform;

import dev.rono.proxychat.bungee.BungeeProxyChatPlugin;
import dev.rono.proxychat.common.platform.ProxyPlayer;
import dev.rono.proxychat.common.platform.SignedChatHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Waterfall-specific signed chat support. Cancelling {@code ChatEvent} on 1.19.3+ is not
 * reliable with Paper backends; {@link #canInterceptChat} stays off so players use commands instead.
 */
public final class WaterfallSignedChatHandler implements SignedChatHandler {
    private static final String BOSS_HANDLER = "inbound-boss";
    private static final String HANDLER_NAME = "proxychat-signing-capture";

    private final Logger logger;
    private final Map<UUID, Integer> pendingOffsets = new ConcurrentHashMap<>();
    private boolean warnedAboutPaperBackends;

    private Boolean supported;
    private Constructor<?> acknowledgementConstructor;
    private Class<?> definedPacketClass;
    private int minimumProtocolVersion = 761;

    public WaterfallSignedChatHandler(BungeeProxyChatPlugin plugin) {
        this(plugin.getLogger());
    }

    WaterfallSignedChatHandler(Logger logger) {
        this.logger = logger;
        init();
    }

    private void init() {
        try {
            Class<?> acknowledgementClass = Class.forName("net.md_5.bungee.protocol.packet.ClientChatAcknowledgement");
            acknowledgementConstructor = acknowledgementClass.getConstructor(int.class);
            definedPacketClass = Class.forName("net.md_5.bungee.protocol.DefinedPacket");
            Field protocolField = Class.forName("net.md_5.bungee.protocol.ProtocolConstants").getField("MINECRAFT_1_19_3");
            minimumProtocolVersion = protocolField.getInt(null);
            supported = true;
            logger.info("Waterfall detected. Plain @prefix and toggle chat are disabled for 1.19.3+ clients (required for Paper backends). Use /channel or /<prefix><message> instead. Set signed-chat-interception: always to force plain-chat intercept (not recommended on Paper).");

        } catch (ReflectiveOperationException exception) {
            supported = false;
            logger.info("Running on vanilla BungeeCord: /channel commands work, but @prefix and toggle chat are disabled for 1.19.3+ clients. Use Waterfall (drop-in replacement) for full signed-chat support.");
        }
    }

    @Override
    public boolean canInterceptChat(ProxyPlayer player) {
        if (player.getProtocolVersion() < minimumProtocolVersion) {
            return true;
        }

        return false;
    }

    @Override
    public void onPlayerJoin(ProxyPlayer player) {
        if (!Boolean.TRUE.equals(supported) || !(player instanceof BungeePlayer bungeePlayer)) {
            return;
        }

        injectPacketCapture(bungeePlayer.handle());
    }

    @Override
    public void onPlayerQuit(ProxyPlayer player) {
        pendingOffsets.remove(player.getUniqueId());

        if (player instanceof BungeePlayer bungeePlayer) {
            removePacketCapture(bungeePlayer.handle());
        }
    }

    @Override
    public void acknowledgeCancelledChat(ProxyPlayer player) {
        if (!Boolean.TRUE.equals(supported) || !(player instanceof BungeePlayer bungeePlayer)) {
            return;
        }

        ProxiedPlayer handle = bungeePlayer.handle();
        if (handle.getServer() == null || handle.getPendingConnection().getVersion() < minimumProtocolVersion) {
            return;
        }

        Integer offset = pendingOffsets.remove(player.getUniqueId());
        if (offset == null) {
            if (!warnedAboutPaperBackends) {
                warnedAboutPaperBackends = true;
                logger.warning("Could not acknowledge cancelled chat for " + handle.getName()
                        + ". Toggle and @prefix may kick players on 1.19.3+ (especially Paper backends). Use /channel commands or signed-chat-interception: never.");
            }

            return;
        }

        try {
            Object packet = acknowledgementConstructor.newInstance(offset);
            Object unsafe = handle.getServer().unsafe();
            Method sendPacket = unsafe.getClass().getMethod("sendPacket", definedPacketClass);
            sendPacket.invoke(unsafe, packet);

        } catch (ReflectiveOperationException exception) {
            logger.log(Level.WARNING, "Failed to acknowledge cancelled chat for " + handle.getName(), exception);
        }
    }

    private void injectPacketCapture(ProxiedPlayer player) {
        try {
            Channel channel = getPlayerChannel(player);
            if (channel == null || channel.pipeline().get(HANDLER_NAME) != null) {
                return;
            }

            channel.pipeline().addBefore(BOSS_HANDLER, HANDLER_NAME, new SignedChatCaptureHandler(player.getUniqueId()));

        } catch (Exception exception) {
            logger.log(Level.FINE, "Failed to install signed chat capture handler", exception);
        }
    }

    private void removePacketCapture(ProxiedPlayer player) {
        try {
            Channel channel = getPlayerChannel(player);
            if (channel == null) {
                return;
            }

            ChannelHandler handler = channel.pipeline().get(HANDLER_NAME);
            if (handler != null) {
                channel.pipeline().remove(handler);
            }

        } catch (Exception exception) {
            logger.log(Level.FINE, "Failed to remove signed chat capture handler", exception);
        }
    }

    private Channel getPlayerChannel(ProxiedPlayer player) throws ReflectiveOperationException {
        Field channelField = player.getClass().getDeclaredField("ch");
        channelField.setAccessible(true);
        Object channelWrapper = channelField.get(player);
        Method getHandle = channelWrapper.getClass().getMethod("getHandle");
        return (Channel) getHandle.invoke(channelWrapper);
    }

    private void captureClientChatOffset(Object message, UUID playerId) {
        try {
            Object packet = unwrapPacket(message);
            if (packet == null) {
                return;
            }

            Class<?> clientChatClass = Class.forName("net.md_5.bungee.protocol.packet.ClientChat");
            if (!clientChatClass.isInstance(packet)) {
                return;
            }

            Object seenMessages = clientChatClass.getMethod("getSeenMessages").invoke(packet);
            if (seenMessages == null) {
                return;
            }

            int offset = (Integer) seenMessages.getClass().getMethod("getOffset").invoke(seenMessages);
            pendingOffsets.put(playerId, offset);

        } catch (ReflectiveOperationException exception) {
            logger.log(Level.FINE, "Failed to capture signed chat metadata", exception);
        }
    }

    private Object unwrapPacket(Object message) throws ReflectiveOperationException {
        Class<?> packetWrapperClass = Class.forName("net.md_5.bungee.protocol.PacketWrapper");
        if (packetWrapperClass.isInstance(message)) {
            return packetWrapperClass.getField("packet").get(message);
        }

        Class<?> definedPacketClass = Class.forName("net.md_5.bungee.protocol.DefinedPacket");
        if (definedPacketClass.isInstance(message)) {
            return message;
        }

        return null;
    }

    private final class SignedChatCaptureHandler extends ChannelInboundHandlerAdapter {
        private final UUID playerId;

        private SignedChatCaptureHandler(UUID playerId) {
            this.playerId = playerId;
        }

        @Override
        public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
            captureClientChatOffset(message, playerId);
            super.channelRead(context, message);
        }
    }
}
