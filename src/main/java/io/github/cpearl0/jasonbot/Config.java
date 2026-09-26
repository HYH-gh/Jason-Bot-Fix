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

    private static final ForgeConfigSpec.BooleanValue PROACTIVE_ENABLED = BUILDER
            .comment("Master switch for proactive (auto-initiated) chat. When false, all proactive features are disabled.")
            .define("proactiveEnabled", true);

    private static final ForgeConfigSpec.BooleanValue PROACTIVE_WELCOME_ENABLED = BUILDER
            .comment("Whether the bot automatically welcomes players when they join the server.")
            .define("proactiveWelcomeEnabled", true);

    private static final ForgeConfigSpec.BooleanValue PROACTIVE_DEATH_COMMENT_ENABLED = BUILDER
            .comment("Whether the bot comments when a player dies.")
            .define("proactiveDeathCommentEnabled", true);

    private static final ForgeConfigSpec.IntValue PROACTIVE_BACKLOG_THRESHOLD = BUILDER
            .comment("Number of non-wake chat messages to buffer before the bot chimes in. Set to 0 to disable backlog-based proactive chat.")
            .defineInRange("proactiveBacklogThreshold", 8, 0, 100);

    private static final ForgeConfigSpec.BooleanValue PROACTIVE_BACKLOG_INCLUDE_CHAT = BUILDER
            .comment("Whether to include the buffered chat messages as context when triggering backlog proactive chat. True = smarter responses but uses more tokens.")
            .define("proactiveBacklogIncludeChat", false);

    private static final ForgeConfigSpec.BooleanValue PROACTIVE_IDLE_ENABLED = BUILDER
            .comment("Whether the bot initiates conversation when the chat has been idle for a while.")
            .define("proactiveIdleEnabled", true);

    private static final ForgeConfigSpec.IntValue PROACTIVE_IDLE_INTERVAL_SECONDS = BUILDER
            .comment("How many seconds of chat inactivity before the bot proactively speaks.")
            .defineInRange("proactiveIdleIntervalSeconds", 600, 60, 7200);

    private static final ForgeConfigSpec.BooleanValue THINKING_INDICATOR_ENABLED = BUILDER
            .comment("Whether to show a 'thinking...' indicator when the bot is processing a request.")
            .define("thinkingIndicatorEnabled", true);

    private static final ForgeConfigSpec.ConfigValue<String> THINKING_INDICATOR_TEXT = BUILDER
            .comment("The text to show as the thinking indicator. Supports § color codes.")
            .define("thinkingIndicatorText", "§7§o[Jason 正在思考...]§r");

    private static final ForgeConfigSpec.BooleanValue STREAMING_ENABLED = BUILDER
            .comment("Whether to use SSE streaming + sentence chunking for chat responses. When disabled, the full response is sent at once.")
            .define("streamingEnabled", true);

    private static final ForgeConfigSpec.IntValue STREAMING_CHUNK_DELAY_MS = BUILDER
            .comment("Delay in milliseconds between each sentence chunk when streaming.")
            .defineInRange("streamingChunkDelayMs", 500, 100, 5000);

    private static final ForgeConfigSpec.BooleanValue SHOW_AI_NAME_ON_EACH_CHUNK = BUILDER
            .comment("Whether to show the AI name ([Jason]) on every sentence chunk. When false, only the first chunk shows the name.")
            .define("showAINameOnEachChunk", false);

    private static final ForgeConfigSpec.ConfigValue<String> AI_RESPONSE_COLOR = BUILDER
            .comment("The § color code prefix applied to all AI responses. Use § followed by a color code (e.g. §b for aqua, §a for green, §d for pink). Set empty to disable.")
            .define("aiResponseColor", "§b");

    private static final ForgeConfigSpec.BooleanValue DEBUG_MODE = BUILDER
            .comment("Enable debug mode. When enabled, the /jasontool command is available to test all AI tools directly.")
            .define("debugMode", false);

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
    public static boolean proactiveEnabled;
    public static boolean proactiveWelcomeEnabled;
    public static boolean proactiveDeathCommentEnabled;
    public static int proactiveBacklogThreshold;
    public static boolean proactiveBacklogIncludeChat;
    public static boolean proactiveIdleEnabled;
    public static int proactiveIdleIntervalSeconds;
    public static boolean thinkingIndicatorEnabled;
    public static String thinkingIndicatorText;
    public static boolean streamingEnabled;
    public static int streamingChunkDelayMs;
    public static boolean showAINameOnEachChunk;
    public static String aiResponseColor;
    public static boolean debugMode;

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
        proactiveEnabled = PROACTIVE_ENABLED.get();
        proactiveWelcomeEnabled = PROACTIVE_WELCOME_ENABLED.get();
        proactiveDeathCommentEnabled = PROACTIVE_DEATH_COMMENT_ENABLED.get();
        proactiveBacklogThreshold = PROACTIVE_BACKLOG_THRESHOLD.get();
        proactiveBacklogIncludeChat = PROACTIVE_BACKLOG_INCLUDE_CHAT.get();
        proactiveIdleEnabled = PROACTIVE_IDLE_ENABLED.get();
        proactiveIdleIntervalSeconds = PROACTIVE_IDLE_INTERVAL_SECONDS.get();
        thinkingIndicatorEnabled = THINKING_INDICATOR_ENABLED.get();
        thinkingIndicatorText = THINKING_INDICATOR_TEXT.get();
        streamingEnabled = STREAMING_ENABLED.get();
        streamingChunkDelayMs = STREAMING_CHUNK_DELAY_MS.get();
        showAINameOnEachChunk = SHOW_AI_NAME_ON_EACH_CHUNK.get();
        aiResponseColor = AI_RESPONSE_COLOR.get();
        debugMode = DEBUG_MODE.get();
    }
}