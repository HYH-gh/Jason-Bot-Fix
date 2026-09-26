package io.github.cpearl0.jasonbot.bot;

import io.github.cpearl0.jasonbot.Config;
import io.github.cpearl0.jasonbot.JasonBot;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ProactiveChat {
    private static final Deque<String> chatBuffer = new ConcurrentLinkedDeque<>();
    private static volatile long lastChatActivityTime = System.currentTimeMillis();
    private static volatile long lastAISpeakTime = System.currentTimeMillis();
    private static ScheduledExecutorService idleTimer;
    private static MinecraftServer server;
    private static boolean running = false;

    public static void start(MinecraftServer mcServer) {
        if (!Config.proactiveEnabled) {
            return;
        }

        server = mcServer;
        running = true;
        chatBuffer.clear();
        lastChatActivityTime = System.currentTimeMillis();
        lastAISpeakTime = System.currentTimeMillis();

        if (Config.proactiveIdleEnabled && Config.proactiveIdleIntervalSeconds > 0) {
            idleTimer = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "JasonBot-IdleTimer");
                t.setDaemon(true);
                return t;
            });
            int interval = Math.min(Config.proactiveIdleIntervalSeconds, 60);
            idleTimer.scheduleAtFixedRate(ProactiveChat::checkIdle, interval, interval, TimeUnit.SECONDS);
            JasonBot.LOGGER.info("Proactive idle timer started, check interval: {}s, idle threshold: {}s",
                    interval, Config.proactiveIdleIntervalSeconds);
        }

        JasonBot.LOGGER.info("ProactiveChat started. Welcome={}, DeathComment={}, BacklogThreshold={}, Idle={}s",
                Config.proactiveWelcomeEnabled, Config.proactiveDeathCommentEnabled,
                Config.proactiveBacklogThreshold, Config.proactiveIdleIntervalSeconds);
    }

    public static void shutdown() {
        running = false;
        if (idleTimer != null) {
            idleTimer.shutdownNow();
            idleTimer = null;
        }
        chatBuffer.clear();
        server = null;
        JasonBot.LOGGER.info("ProactiveChat shutdown.");
    }

    static void onAISpoke() {
        lastAISpeakTime = System.currentTimeMillis();
    }

    static void onPlayerChat() {
        lastChatActivityTime = System.currentTimeMillis();
    }

    public static void onPlayerJoin(ServerPlayer player) {
        if (!running || !Config.proactiveWelcomeEnabled) {
            return;
        }

        String playerName = player.getDisplayName().getString();
        String eventMessage = String.format("玩家 %s 加入了游戏。请自然地欢迎他/她，简短一点。", playerName);
        triggerProactive(eventMessage);
    }

    public static void onPlayerDeath(ServerPlayer player, DamageSource source) {
        if (!running || !Config.proactiveDeathCommentEnabled) {
            return;
        }

        String playerName = player.getDisplayName().getString();
        String deathMessage = source.getLocalizedDeathMessage(player).getString();
        String biome = player.level().getBiome(player.blockPosition()).unwrap()
                .map(key -> key.location().toString(), u -> "unknown");
        var pos = player.blockPosition();

        String eventMessage = String.format(
                "玩家 %s 刚刚死了。死因: %s。位置: %s (x:%d,y:%d,z:%d)。请发表简短的评论或吐槽，要自然有趣。",
                playerName, deathMessage, biome, pos.getX(), pos.getY(), pos.getZ()
        );
        triggerProactive(eventMessage);
    }

    public static void onNonWakeChat(ServerPlayer player, String message) {
        if (!running || Config.proactiveBacklogThreshold <= 0) {
            return;
        }

        onPlayerChat();

        String playerName = player.getDisplayName().getString();
        String formatted = String.format("[%s] %s", playerName, message);
        chatBuffer.addLast(formatted);

        while (chatBuffer.size() > Config.proactiveBacklogThreshold) {
            chatBuffer.removeFirst();
        }

        if (chatBuffer.size() >= Config.proactiveBacklogThreshold) {
            String eventMessage;
            if (Config.proactiveBacklogIncludeChat) {
                StringBuilder sb = new StringBuilder("玩家们刚才在聊天，以下是最近几条消息：\n");
                for (String msg : chatBuffer) {
                    sb.append(msg).append('\n');
                }
                sb.append("请自然地加入他们的对话，发表你的看法。要简短有趣。");
                eventMessage = sb.toString();
            } else {
                eventMessage = String.format(
                        "玩家们刚才聊了%d条消息，挺活跃的。请自然地加入他们的对话，发表你的看法。不用复述他们说了什么，简短有趣地插句话就行。",
                        chatBuffer.size()
                );
            }
            chatBuffer.clear();
            triggerProactive(eventMessage);
        }
    }

    private static void triggerProactive(String eventMessage) {
        if (server == null) {
            return;
        }

        AsyncAIChat.chatProactive(eventMessage)
                .thenAccept(response -> {
                    onAISpoke();
                    String formatted = ResponseFormatter.format(response);
                    AsyncAIChat.chatHistory.addAssistantMessage(
                            AsyncAIChat.jasonBotPlayer.getDisplayName().getString(), formatted);
                    server.getPlayerList().broadcastChatMessage(
                            PlayerChatMessage.system(formatted),
                            AsyncAIChat.jasonBotPlayer,
                            net.minecraft.network.chat.ChatType.bind(
                                    net.minecraft.network.chat.ChatType.CHAT,
                                    AsyncAIChat.jasonBotPlayer)
                    );
                })
                .exceptionally(ex -> {
                    JasonBot.LOGGER.error("Proactive chat failed: {}", ex.getCause().getMessage());
                    return null;
                });
    }

    private static void checkIdle() {
        if (!running || !Config.proactiveIdleEnabled || server == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long idleMs = Config.proactiveIdleIntervalSeconds * 1000L;

        if (now - lastChatActivityTime < idleMs) {
            return;
        }

        if (now - lastAISpeakTime < idleMs) {
            return;
        }

        if (server.getPlayerCount() == 0) {
            return;
        }

        String eventMessage = String.format(
                "你已经沉默了好几分钟了。目前服务器有%d个玩家在线，主动和玩家们聊点什么吧。可以说说游戏技巧、开个玩笑，或者问问大家在做什么。简短自然即可。",
                server.getPlayerCount()
        );
        triggerProactive(eventMessage);
    }
}