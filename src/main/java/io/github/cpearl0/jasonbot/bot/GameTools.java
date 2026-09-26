package io.github.cpearl0.jasonbot.bot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cpearl0.jasonbot.Config;
import io.github.cpearl0.jasonbot.JasonBot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GameTools {

    private static final int MAX_TOOL_ROUNDS = 5;

    public static int getMaxToolRounds() {
        return MAX_TOOL_ROUNDS;
    }

    public static JsonArray getToolDefinitions() {
        JsonArray tools = new JsonArray();

        tools.add(createTool(
                "get_player_info",
                "获取当前与你对话的玩家的游戏内基本信息，包括：游戏模式、所在维度、坐标、生物群系、生命值、饥饿值、饱腹度、经验等级。当玩家询问\"我在哪\"\"我的状态\"等问题时使用。",
                new JsonObject()
        ));

        tools.add(createTool(
                "get_player_equipment",
                "获取当前与你对话的玩家的装备信息，包括：主手物品、副手物品、护甲栏物品。当玩家询问\"我拿着什么\"\"我穿了什么\"等问题时使用。可通过detailed参数控制是否返回详细NBT数据，默认为false仅返回基础信息。参数示例（注意格式）：{\"detailed\": false}",
                createEquipmentParams()
        ));

        tools.add(createTool(
                "get_looking_at",
                "获取当前与你对话的玩家视线正指向的方块或流体。当玩家询问\"我面前是什么\"\"这是什么方块\"等问题时使用。可通过detailed参数控制是否返回方块实体（如箱子内容、告示牌文字等）的NBT数据。参数示例（注意格式）：{\"detailed\": true}",
                createLookingAtParams()
        ));

        tools.add(createTool(
                "get_server_info",
                "获取当前服务器信息，包括：在线玩家人数、距离当前玩家最近的玩家姓名与位置。",
                new JsonObject()
        ));

        tools.add(createTool(
                "get_online_players",
                "获取服务器所有在线玩家的完整列表，包括每个玩家的名称和UUID。当玩家询问\"有哪些人在线\"\"服务器都有谁\"时使用。",
                new JsonObject()
        ));

        tools.add(createTool(
                "get_real_time",
                "获取现实世界当前时间。仅在玩家与你讨论现实世界话题时使用，与游戏内时间无关。",
                new JsonObject()
        ));

        tools.add(createTool(
                "execute_command",
                "执行一条Minecraft原版指令。当玩家要求你执行具体操作（如给予物品、修改时间、传送等）时使用。注意：指令执行结果会在聊天栏中显示，你可以告知玩家查看。某些敏感指令已被禁用。",
                createCommandParams()
        ));

        if (Config.webSearchEnabled) {
            tools.add(createTool(
                    "web_search",
                    "搜索互联网获取实时信息。当玩家询问的问题超出你的知识范围（如最新Minecraft版本特性、模组攻略、合成配方或者闲聊提问等需要联网查询的内容）时使用。",
                    createSearchParams()
            ));
        }

        return tools;
    }

    private static JsonObject createCommandParams() {
        JsonObject params = new JsonObject();
        params.addProperty("type", "object");

        JsonObject properties = new JsonObject();
        JsonObject commandProp = new JsonObject();
        commandProp.addProperty("type", "string");
        commandProp.addProperty("description", "要执行的Minecraft指令，不需要带斜杠(/)前缀。例如: give Steve minecraft:diamond 1");
        properties.add("command", commandProp);

        JsonArray required = new JsonArray();
        required.add("command");

        params.add("properties", properties);
        params.add("required", required);
        return params;
    }

    private static JsonObject createSearchParams() {
        JsonObject params = new JsonObject();
        params.addProperty("type", "object");

        JsonObject properties = new JsonObject();
        JsonObject queryProp = new JsonObject();
        queryProp.addProperty("type", "string");
        queryProp.addProperty("description", "搜索关键词，尽量使用Minecraft相关的英文关键词以获得更好的搜索结果");
        properties.add("query", queryProp);

        JsonArray required = new JsonArray();
        required.add("query");

        params.add("properties", properties);
        params.add("required", required);
        return params;
    }

    private static JsonObject createEquipmentParams() {
        JsonObject params = new JsonObject();
        params.addProperty("type", "object");

        JsonObject properties = new JsonObject();
        JsonObject detailedProp = new JsonObject();
        detailedProp.addProperty("type", "boolean");
        detailedProp.addProperty("description", "是否返回物品的详细NBT数据。默认为false，仅返回物品名称和数量。设为true时额外返回完整NBT标签。");
        properties.add("detailed", detailedProp);

        params.add("properties", properties);
        return params;
    }

    private static JsonObject createLookingAtParams() {
        JsonObject params = new JsonObject();
        params.addProperty("type", "object");

        JsonObject properties = new JsonObject();
        JsonObject detailedProp = new JsonObject();
        detailedProp.addProperty("type", "boolean");
        detailedProp.addProperty("description", "是否返回方块实体（如箱子、告示牌、熔炉等）的详细NBT数据。默认为false，仅返回方块/流体名称。");
        properties.add("detailed", detailedProp);

        params.add("properties", properties);
        return params;
    }

    public static String executeTool(ServerPlayer player, String toolName, String arguments) {
        JasonBot.LOGGER.info("Tool call: {} args={}", toolName, arguments);

        MinecraftServer server = player.getServer();
        if (server != null && !server.isSameThread()) {
            JasonBot.LOGGER.info("Dispatching tool {} to server thread (current: {})",
                    toolName, Thread.currentThread().getName());
            try {
                return server.submit(() -> executeToolDirectly(player, toolName, arguments))
                        .get(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "{\"error\": \"工具执行被中断\"}";
            } catch (Exception e) {
                JasonBot.LOGGER.error("Tool {} execution on server thread failed: {}", toolName, e.getMessage());
                return "{\"error\": \"工具执行失败: " + e.getMessage() + "\"}";
            }
        }
        return executeToolDirectly(player, toolName, arguments);
    }

    private static String executeToolDirectly(ServerPlayer player, String toolName, String arguments) {
        return switch (toolName) {
            case "get_player_info" -> executeGetPlayerInfo(player);
            case "get_player_equipment" -> executeGetPlayerEquipment(player, arguments);
            case "get_looking_at" -> executeGetLookingAt(player, arguments);
            case "get_server_info" -> executeGetServerInfo(player);
            case "get_online_players" -> executeGetOnlinePlayers(player);
            case "get_real_time" -> executeGetRealTime();
            case "execute_command" -> executeCommandTool(player, arguments);
            case "web_search" -> executeWebSearch(arguments);
            default -> "{\"error\": \"未知工具: " + toolName + "\"}";
        };
    }

    private static JsonObject createTool(String name, String description, JsonObject parameters) {
        JsonObject tool = new JsonObject();
        tool.addProperty("type", "function");

        JsonObject function = new JsonObject();
        function.addProperty("name", name);
        function.addProperty("description", description);
        function.add("parameters", parameters);

        tool.add("function", function);
        return tool;
    }

    private static String executeGetPlayerInfo(ServerPlayer player) {
        var level = player.level();
        var pos = player.blockPosition();

        JsonObject result = new JsonObject();
        result.addProperty("dimension", level.dimension().location().toString());
        result.addProperty("game_time", level.getDayTime());
        result.addProperty("position", "x:%d y:%d z:%d".formatted(pos.getX(), pos.getY(), pos.getZ()));

        var biome = level.getBiome(pos).unwrap()
                .map(key -> key.location().toString(), ubiome -> "unknown");
        result.addProperty("biome", biome);

        result.addProperty("health", player.getHealth());
        result.addProperty("max_health", player.getMaxHealth());
        result.addProperty("hunger", player.getFoodData().getFoodLevel());
        result.addProperty("saturation", player.getFoodData().getSaturationLevel());
        result.addProperty("experience_level", player.experienceLevel);
        result.addProperty("game_mode", player.gameMode.getGameModeForPlayer().getName());

        return result.toString();
    }

    private static String executeGetPlayerEquipment(ServerPlayer player, String arguments) {
        boolean detailed = parseDetailedFlag(arguments);
        JasonBot.LOGGER.info("get_player_equipment: detailed={}", detailed);

        JsonObject result = new JsonObject();
        result.addProperty("detailed", detailed);

        var mainhand = player.getMainHandItem();
        result.add("mainhand", buildItemJson(mainhand, detailed));

        var offhand = player.getOffhandItem();
        result.add("offhand", buildItemJson(offhand, detailed));

        JsonArray armorArr = new JsonArray();
        for (var armorPiece : player.getArmorSlots()) {
            armorArr.add(buildItemJson(armorPiece, detailed));
        }
        result.add("armor", armorArr);

        return result.toString();
    }

    /**
     * Robust parser for the "detailed" boolean flag from AI tool arguments.
     * AI models may send non-standard formats like bare "true", "\"true\"", etc.
     * Handles: {"detailed":true}, {"detailed":"true"}, true, "true"
     */
    private static boolean parseDetailedFlag(String arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return false;
        }
        String trimmed = arguments.trim();

        // Standard path: {"detailed": true/false}
        try {
            JsonElement parsed = JsonParser.parseString(trimmed);
            if (parsed.isJsonObject()) {
                JsonObject args = parsed.getAsJsonObject();
                if (args.has("detailed")) {
                    return args.get("detailed").getAsBoolean();
                }
                return false;
            }
            // Bare boolean/string: the entire arguments is just "true" or "\"true\""
            if (parsed.isJsonPrimitive()) {
                boolean val = parsed.getAsBoolean();
                JasonBot.LOGGER.warn("Tool 'detailed' flag received non-standard format: '{}', parsed as: {}", trimmed, val);
                return val;
            }
        } catch (Exception e) {
            JasonBot.LOGGER.warn("Tool 'detailed' flag parse failed for '{}': {}", trimmed, e.getMessage());
        }
        return false;
    }

    private static JsonObject buildItemJson(net.minecraft.world.item.ItemStack stack, boolean detailed) {
        JsonObject obj = new JsonObject();
        obj.addProperty("item", stack.getItem().toString());
        obj.addProperty("count", stack.getCount());
        if (detailed) {
            obj.addProperty("nbt", stack.serializeNBT().toString());
        }
        return obj;
    }

    private static String executeGetLookingAt(ServerPlayer player, String arguments) {
        boolean detailed = parseDetailedFlag(arguments);
        JasonBot.LOGGER.info("get_looking_at: detailed={}, looking from {}", detailed, player.blockPosition());

        var level = player.level();
        JsonObject result = new JsonObject();
        result.addProperty("detailed", detailed);

        var block = player.pick(20.0, 0.0F, false);
        if (block.getType() == HitResult.Type.BLOCK) {
            var blockpos = ((BlockHitResult) block).getBlockPos();
            var blockstate = level.getBlockState(blockpos);
            var blockname = BuiltInRegistries.BLOCK.getKey(blockstate.getBlock());
            result.addProperty("looking_at_block", blockname != null ? blockname.toString() : "unknown");

            if (detailed) {
                var blockEntity = level.getBlockEntity(blockpos);
                JasonBot.LOGGER.info("get_looking_at: detailed=true, blockEntity at {}: {}",
                        blockpos, blockEntity != null ? blockEntity.getClass().getSimpleName() : "null");
                if (blockEntity != null) {
                    var tag = blockEntity.saveWithFullMetadata();
                    String nbtStr = tag.toString();
                    JasonBot.LOGGER.info("get_looking_at: NBT (first 300 chars): {}",
                            nbtStr.substring(0, Math.min(nbtStr.length(), 300)));
                    result.addProperty("block_entity_nbt", nbtStr);
                }
            }
        } else {
            JasonBot.LOGGER.info("get_looking_at: no block hit, type={}", block.getType());
            result.addProperty("looking_at_block", "none");
        }

        var liquid = player.pick(20.0, 0.0F, true);
        if (liquid.getType() == HitResult.Type.BLOCK) {
            var blockpos = ((BlockHitResult) liquid).getBlockPos();
            var fluidstate = level.getFluidState(blockpos);
            var fluidname = BuiltInRegistries.FLUID.getKey(fluidstate.getType());
            if (fluidname != null && !fluidname.toString().equals("minecraft:empty")) {
                result.addProperty("looking_at_fluid", fluidname.toString());
            }
        }

        return result.toString();
    }

    private static String executeGetServerInfo(ServerPlayer player) {
        var level = player.level();
        JsonObject result = new JsonObject();

        result.addProperty("online_players", level.players().size());

        var nearestPlayer = level.getNearestPlayer(
                player.getX(), player.getY(), player.getZ(), -1.0,
                p -> p != player && EntitySelector.NO_SPECTATORS.test(p));
        if (nearestPlayer != null) {
            JsonObject nearest = new JsonObject();
            nearest.addProperty("name", nearestPlayer.getDisplayName().getString());
            var pos = nearestPlayer.blockPosition();
            nearest.addProperty("position", "x:%d y:%d z:%d".formatted(pos.getX(), pos.getY(), pos.getZ()));
            result.add("nearest_player", nearest);
        } else {
            result.add("nearest_player", null);
        }

        return result.toString();
    }

    private static String executeGetOnlinePlayers(ServerPlayer player) {
        var level = player.level();
        JsonObject result = new JsonObject();
        JsonArray playersArr = new JsonArray();

        for (var p : level.players()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", p.getDisplayName().getString());
            entry.addProperty("uuid", p.getUUID().toString());
            playersArr.add(entry);
        }

        result.addProperty("online_count", level.players().size());
        result.add("players", playersArr);
        return result.toString();
    }

    private static String executeGetRealTime() {
        JsonObject result = new JsonObject();
        result.addProperty("real_time", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return result.toString();
    }

    private static String executeCommandTool(ServerPlayer player, String arguments) {
        JsonObject args = JsonParser.parseString(arguments).getAsJsonObject();
        String command = args.get("command").getAsString().trim();

        if (command.startsWith("/")) {
            command = command.substring(1).trim();
        }

        if (command.isEmpty()) {
            return "{\"error\": \"指令为空\"}";
        }

        String commandRoot = command.split(" ")[0].toLowerCase();
        for (String blacklisted : Config.commandBlacklist) {
            if (blacklisted.equalsIgnoreCase(commandRoot)) {
                JsonObject blocked = new JsonObject();
                blocked.addProperty("command", command);
                blocked.addProperty("success", false);
                blocked.addProperty("error", "指令 " + commandRoot + " 在黑名单中，已被禁止执行");
                return blocked.toString();
            }
        }

        try {
            var server = player.getServer();
            int result = server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack().withPermission(Config.commandPermissionLevel),
                    command
            );

            JsonObject response = new JsonObject();
            response.addProperty("command", command);
            response.addProperty("success", result > 0);
            response.addProperty("result_count", result);
            return response.toString();
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("command", command);
            error.addProperty("success", false);
            error.addProperty("error", "指令执行异常: " + e.getMessage());
            return error.toString();
        }
    }

    private static String executeWebSearch(String arguments) {
        JsonObject args = JsonParser.parseString(arguments).getAsJsonObject();
        String query = args.get("query").getAsString().trim();

        if (query.isEmpty()) {
            return "{\"error\": \"搜索关键词为空\"}";
        }

        String endpoint = Config.webSearchEndpoint;
        if (endpoint == null || endpoint.isBlank() || endpoint.startsWith("Enter")) {
            return "{\"error\": \"未配置搜索API端点\"}";
        }

        String apiKey = Config.webSearchAPIKey;
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("Enter")) {
            return "{\"error\": \"未配置搜索API密钥\"}";
        }

        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("query", query);
            requestBody.addProperty("max_results", 5);
            String bodyJson = requestBody.toString();

            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(bodyJson.getBytes(StandardCharsets.UTF_8));
            }

            int statusCode = connection.getResponseCode();
            if (statusCode != 200) {
                StringBuilder errorBody = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorBody.append(line);
                    }
                } catch (Exception ignored) {
                }

                JsonObject error = new JsonObject();
                error.addProperty("query", query);
                error.addProperty("success", false);
                error.addProperty("error", "搜索API返回状态码: " + statusCode);
                if (errorBody.length() > 0) {
                    error.addProperty("detail", errorBody.toString());
                }
                return error.toString();
            }

            StringBuilder responseBody = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBody.append(line);
                }
            }

            return parseSearchResults(responseBody.toString(), query);

        } catch (IOException e) {
            JsonObject error = new JsonObject();
            error.addProperty("query", query);
            error.addProperty("success", false);
            error.addProperty("error", "搜索请求失败: " + e.getMessage());
            return error.toString();
        } catch (Exception e) {
            JasonBot.LOGGER.error("Web search error", e);
            JsonObject error = new JsonObject();
            error.addProperty("query", query);
            error.addProperty("success", false);
            error.addProperty("error", "搜索异常: " + e.getMessage());
            return error.toString();
        }
    }

    private static String parseSearchResults(String rawJson, String query) {
        try {
            JsonObject response = JsonParser.parseString(rawJson).getAsJsonObject();

            JsonArray results = null;
            for (String key : new String[]{"results", "data", "items", "documents", "organic_results"}) {
                if (response.has(key) && response.get(key).isJsonArray()) {
                    results = response.getAsJsonArray(key);
                    break;
                }
            }

            if (results != null && results.size() > 0) {
                JsonObject structured = new JsonObject();
                structured.addProperty("query", query);
                structured.addProperty("success", true);
                structured.addProperty("result_count", results.size());

                JsonArray parsedResults = new JsonArray();
                int usedChars = 0;
                int maxChars = 6000;

                for (JsonElement elem : results) {
                    if (!elem.isJsonObject()) continue;
                    JsonObject item = elem.getAsJsonObject();
                    JsonObject parsed = new JsonObject();

                    if (item.has("title")) parsed.add("title", item.get("title"));
                    else if (item.has("name")) parsed.add("title", item.get("name"));

                    if (item.has("url")) parsed.add("url", item.get("url"));
                    else if (item.has("link")) parsed.add("url", item.get("link"));

                    if (item.has("snippet")) parsed.add("snippet", item.get("snippet"));
                    else if (item.has("description")) parsed.add("snippet", item.get("description"));
                    else if (item.has("content")) {
                        String content = item.get("content").getAsString();
                        if (content.length() > 300) {
                            content = content.substring(0, 300) + "...";
                        }
                        parsed.addProperty("snippet", content);
                    } else if (item.has("snippet_content")) {
                        parsed.add("snippet", item.get("snippet_content"));
                    }

                    String parsedStr = parsed.toString();
                    if (usedChars + parsedStr.length() > maxChars) break;
                    parsedResults.add(parsed);
                    usedChars += parsedStr.length();
                }

                structured.add("results", parsedResults);
                return structured.toString();
            }
        } catch (Exception e) {
            JasonBot.LOGGER.debug("Failed to parse structured search results, returning raw", e);
        }

        String truncated = rawJson.length() > 8000 ? rawJson.substring(0, 8000) : rawJson;
        JsonObject fallback = new JsonObject();
        fallback.addProperty("query", query);
        fallback.addProperty("success", true);
        fallback.addProperty("raw_response", truncated);
        return fallback.toString();
    }
}