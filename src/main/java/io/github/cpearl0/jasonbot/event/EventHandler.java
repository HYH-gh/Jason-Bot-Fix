package io.github.cpearl0.jasonbot.event;

import io.github.cpearl0.jasonbot.Config;
import io.github.cpearl0.jasonbot.JasonBot;
import io.github.cpearl0.jasonbot.bot.AsyncAIChat;
import io.github.cpearl0.jasonbot.bot.ProactiveChat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JasonBot.MODID)
public class EventHandler {
    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        String message = event.getMessage().getString();
        boolean wake = Config.wakeNames.stream().anyMatch(
                name -> message.toLowerCase().startsWith(name.toLowerCase()) ||
                        message.toLowerCase().endsWith(name.toLowerCase()));
        if (!wake) {
            ProactiveChat.onNonWakeChat(event.getPlayer(), message);
            return;
        }

        if (Config.thinkingIndicatorEnabled && !Config.thinkingIndicatorText.isEmpty()) {
            event.getPlayer().displayClientMessage(
                    Component.literal(Config.thinkingIndicatorText), true);
        }

        AsyncAIChat.chatStreaming(event.getPlayer(), message,
                chunk -> {
                    for (ServerPlayer p : event.getPlayer().getServer().getPlayerList().getPlayers()) {
                        p.displayClientMessage(Component.literal(chunk), false);
                    }
                },
                () -> {}
        );
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ProactiveChat.onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ProactiveChat.onPlayerDeath(player, event.getSource());
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        AsyncAIChat.start(event.getServer());
        ProactiveChat.start(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ProactiveChat.shutdown();
        AsyncAIChat.shutdown();
    }
}