package io.github.cpearl0.jasonbot.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.cpearl0.jasonbot.Config;
import io.github.cpearl0.jasonbot.JasonBot;
import io.github.cpearl0.jasonbot.bot.GameTools;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Mod.EventBusSubscriber(modid = JasonBot.MODID)
public class DebugCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("jasontool")
                .requires(source -> Config.debugMode && source.hasPermission(2))
                .then(Commands.literal("list")
                        .executes(ctx -> listTools(ctx.getSource()))
                )
                .then(Commands.argument("toolName", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            var tools = GameTools.getToolDefinitions();
                            return net.minecraft.commands.SharedSuggestionProvider.suggest(
                                    StreamSupport.stream(tools.spliterator(), false)
                                            .map(tc -> tc.getAsJsonObject().getAsJsonObject("function").get("name").getAsString())
                                            .collect(Collectors.toList()),
                                    builder
                            );
                        })
                        .executes(ctx -> {
                            String toolName = StringArgumentType.getString(ctx, "toolName");
                            return executeTool(ctx.getSource(), toolName, "{}");
                        })
                        .then(Commands.argument("jsonArgs", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String toolName = StringArgumentType.getString(ctx, "toolName");
                                    String jsonArgs = StringArgumentType.getString(ctx, "jsonArgs");
                                    return executeTool(ctx.getSource(), toolName, jsonArgs);
                                })
                        )
                )
        );
    }

    private static int listTools(CommandSourceStack source) {
        JsonArray tools = GameTools.getToolDefinitions();
        source.sendSuccess(() -> Component.literal("§6=== AI Tools (" + tools.size() + ") ==="), false);

        for (JsonElement te : tools) {
            var fn = te.getAsJsonObject().getAsJsonObject("function");
            String name = fn.get("name").getAsString();
            String desc = fn.get("description").getAsString();
            source.sendSuccess(() -> Component.literal("§b" + name + "§r - §7" + desc), false);
        }

        source.sendSuccess(() -> Component.literal("§6Usage: /jasontool <toolName> [jsonArgs]"), false);
        return tools.size();
    }

    private static int executeTool(CommandSourceStack source, String toolName, String jsonArgs) {
        var player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("§cThis command can only be used by a player."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§eExecuting tool: §b" + toolName + " §ewith args: §7" + jsonArgs), false);

        try {
            String result = GameTools.executeTool(player, toolName, jsonArgs);

            String display = result.length() > 2000 ? result.substring(0, 1997) + "..." : result;
            source.sendSuccess(() -> Component.literal("§aResult: §f" + display), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cTool execution failed: " + e.getMessage()));
            JasonBot.LOGGER.error("Debug tool '{}' failed", toolName, e);
            return 0;
        }
    }
}