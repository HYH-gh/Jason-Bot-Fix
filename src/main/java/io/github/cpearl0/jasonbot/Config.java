package io.github.cpearl0.jasonbot;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = JasonBot.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<String> API_ENDPOINT = BUILDER
            .comment("API Endpoint. Use DeepSeek by default.")
            .define("APIEndpoint", "https://api.deepseek.com/v1/chat/completions");

    private static final ForgeConfigSpec.ConfigValue<String> AI_MODEL = BUILDER
            .comment("AI Model. Use DeepSeek v3 by default.")
            .define("AIModel", "deepseek-chat");

    private static final ForgeConfigSpec.ConfigValue<String> API_KEY = BUILDER
            .comment("API Key")
            .define("APIKey", "Enter your api key here");

    private static final ForgeConfigSpec.DoubleValue TEMPERATURE = BUILDER
            .comment("Model Temperature")
            .defineInRange("temperature", 1.0, 0, 2.0);

    private static final ForgeConfigSpec.DoubleValue PRESENCE_PENALTY = BUILDER
            .comment("Model Presence Penalty")
            .defineInRange("presencePenalty", 1.0, -2.0, 2.0);

    private static final ForgeConfigSpec.ConfigValue<String> ASSISTANT_NAME = BUILDER
            .comment("Assistant Name")
            .define("assistantName", "Jason");

    private static final ForgeConfigSpec.ConfigValue<String> SYSTEM_PROMPT = BUILDER
            .comment("System Prompt")
            .define("systemPrompt",
                    "你叫Jason，中文名杰森，是知名Minecraft整合包作者。这里是一个Minecraft服务器，玩家会向你提问、和你聊天，请和他们友善而活泼地互动。");

    private static final ForgeConfigSpec.IntValue MAX_HISTORY_SIZE = BUILDER
            .comment("Max History Size. Set to 0 if you don't want the bot to use history.")
            .defineInRange("maxHistorySize", 24, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> WAKE_NAMES = BUILDER
            .comment("Names to wake up the bot")
            .defineList("wakeNames", List.of("Jason", "杰森"), o -> !((String)o).startsWith("/"));

    private static final ForgeConfigSpec.BooleanValue USE_IN_GAME_INFORMATION = BUILDER
            .comment("Whether to provide in-game information to the bot")
            .define("useInGameInformation", true);

    private static final ForgeConfigSpec.IntValue COMMAND_PERMISSION_LEVEL = BUILDER
            .comment("Permission level for commands executed by the bot. 0-4, default 2 (same as command blocks).")
            .defineInRange("commandPermissionLevel", 2, 0, 4);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> COMMAND_BLACKLIST = BUILDER
            .comment("Commands that the bot is not allowed to execute. Commands are matched by their root name (e.g. \"op\" blocks \"/op player\").")
            .defineList("commandBlacklist",
                    List.of("stop", "kick", "ban", "ban-ip", "pardon", "pardon-ip",
                            "op", "deop", "whitelist", "save-all", "save-off", "save-on", "debug"),
                    o -> o instanceof String);

    private static final ForgeConfigSpec.BooleanValue WEB_SEARCH_ENABLED = BUILDER
            .comment("Whether to allow the bot to search the web for real-time information.")
            .define("webSearchEnabled", false);

    private static final ForgeConfigSpec.ConfigValue<String> WEB_SEARCH_ENDPOINT = BUILDER
            .comment("Web search API endpoint. Uses POST with JSON body in the format {\"query\":\"...\"}. Compatible with AnySearch API.")
            .define("webSearchEndpoint", "Enter your search API endpoint here");

    private static final ForgeConfigSpec.ConfigValue<String> WEB_SEARCH_API_KEY = BUILDER
            .comment("API key for the web search service. Sent as 'Authorization: Bearer' header.")
            .define("webSearchAPIKey", "Enter your web search API key here");

    public static final ForgeConfigSpec CONFIG = BUILDER.build();

    public static String APIEndpoint;
    public static String AIModel;
    public static String APIKey;
    public static double temperature;
    public static double presencePenalty;
    public static String assistantName;
    public static String systemPrompt;
    public static int maxHistorySize;
    public static List<? extends String> wakeNames;
    public static boolean useInGameInformation;
    public static int commandPermissionLevel;
    public static List<? extends String> commandBlacklist;
    public static boolean webSearchEnabled;
    public static String webSearchEndpoint;
    public static String webSearchAPIKey;

    @SubscribeEvent
    public static void onLoad(ModConfigEvent event) {
        APIEndpoint = API_ENDPOINT.get();
        AIModel = AI_MODEL.get();
        APIKey = API_KEY.get();
        temperature = TEMPERATURE.get();
        presencePenalty = PRESENCE_PENALTY.get();
        assistantName = ASSISTANT_NAME.get();
        systemPrompt = SYSTEM_PROMPT.get();
        maxHistorySize = MAX_HISTORY_SIZE.get();
        wakeNames = WAKE_NAMES.get();
        useInGameInformation = USE_IN_GAME_INFORMATION.get();
        commandPermissionLevel = COMMAND_PERMISSION_LEVEL.get();
        commandBlacklist = COMMAND_BLACKLIST.get();
        webSearchEnabled = WEB_SEARCH_ENABLED.get();
        webSearchEndpoint = WEB_SEARCH_ENDPOINT.get();
        webSearchAPIKey = WEB_SEARCH_API_KEY.get();
    }
}