package io.github.cpearl0.jasonbot.bot;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import io.github.cpearl0.jasonbot.Config;
import io.github.cpearl0.jasonbot.JasonBot;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;

public class AsyncAIChat {
    private static final String API_ENDPOINT = Config.APIEndpoint;

    private static ExecutorService executor;
    private static final Gson gson = new Gson();

    public static FakePlayer jasonBotPlayer;

    public static final ChatHistory chatHistory = new ChatHistory();

    public static void start(MinecraftServer server) {
        if (executor == null || executor.isTerminated()) {
            executor = Executors.newCachedThreadPool();
        }

        jasonBotPlayer = new FakePlayer(server.overworld(), new GameProfile(UUID.randomUUID(), Config.assistantName));
    }

    public static void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
        chatHistory.clear();
    }

    public static CompletableFuture<String> chatAsync(ServerPlayer player, String userMessage) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!Config.useInGameInformation && !Config.webSearchEnabled) {
                    return chatWithoutTools(player, userMessage);
                }
                return chatWithTools(player, userMessage);
            } catch (Exception e) {
                throw new CompletionException("API Failed", e);
            }
        }, executor);
    }

    private static String chatWithTools(ServerPlayer player, String userMessage) throws IOException {
        var userName = player.getDisplayName().getString();

        chatHistory.addUserMessage(userName, userMessage);

        List<JsonObject> messages = buildMessagesForApi(player);

        JsonArray tools = GameTools.getToolDefinitions();

        for (int round = 0; round < GameTools.getMaxToolRounds(); round++) {
            JsonObject requestBody = buildRequestWithTools(messages, tools);
            JsonObject responseJson = sendRequest(requestBody);

            var message = responseJson.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message");

            JsonArray toolCalls = message.getAsJsonArray("tool_calls");
            if (toolCalls == null || toolCalls.isEmpty()) {
                return message.get("content").getAsString();
            }

            messages.add(message);

            for (JsonElement tc : toolCalls) {
                JsonObject toolCall = tc.getAsJsonObject();
                String callId = toolCall.get("id").getAsString();
                JsonObject fnObj = toolCall.getAsJsonObject("function");
                String fnName = fnObj.get("name").getAsString();
                String fnArgs = fnObj.get("arguments").getAsString();

                String result = GameTools.executeTool(player, fnName, fnArgs);

                JsonObject toolMsg = new JsonObject();
                toolMsg.addProperty("role", "tool");
                toolMsg.addProperty("tool_call_id", callId);
                toolMsg.addProperty("content", result);
                messages.add(toolMsg);
            }

            JasonBot.LOGGER.debug("Tool call round {}: model called tools, continuing...", round + 1);
        }

        return Component.translatable("info.jasonbot.APIfailed").getString();
    }

    private static String chatWithoutTools(ServerPlayer player, String userMessage) throws IOException {
        var userName = player.getDisplayName().getString();

        chatHistory.addUserMessage(userName, userMessage);

        List<JsonObject> messages = buildMessagesForApi(player);

        JsonObject requestBody = buildRequestWithoutTools(messages);
        JsonObject responseJson = sendRequest(requestBody);

        return parseResponseContent(responseJson);
    }

    private static List<JsonObject> buildMessagesForApi(ServerPlayer player) {
        List<JsonObject> messages = new ArrayList<>();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", PromptGenerator.generatePrompt(player));
        messages.add(systemMsg);

        for (var cm : chatHistory.getHistoryOnly()) {
            JsonObject msg = new JsonObject();
            msg.addProperty("role", cm.role());
            if (cm.name() != null) {
                msg.addProperty("name", cm.name());
            }
            msg.addProperty("content", cm.content());
            messages.add(msg);
        }

        return messages;
    }

    private static JsonObject buildRequestWithTools(List<JsonObject> messages, JsonArray tools) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", Config.AIModel);
        requestBody.addProperty("temperature", Config.temperature);
        requestBody.addProperty("presence_penalty", Config.presencePenalty);
        requestBody.add("messages", gson.toJsonTree(messages).getAsJsonArray());
        requestBody.add("tools", tools);
        requestBody.addProperty("tool_choice", "auto");
        return requestBody;
    }

    private static JsonObject buildRequestWithoutTools(List<JsonObject> messages) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", Config.AIModel);
        requestBody.addProperty("temperature", Config.temperature);
        requestBody.addProperty("presence_penalty", Config.presencePenalty);
        requestBody.add("messages", gson.toJsonTree(messages).getAsJsonArray());
        return requestBody;
    }

    private static JsonObject sendRequest(JsonObject requestBody) throws IOException {
        HttpURLConnection connection = createConnection();
        try (OutputStream os = connection.getOutputStream()) {
            os.write(gson.toJson(requestBody).getBytes(StandardCharsets.UTF_8));
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            JsonObject response = gson.fromJson(reader, JsonObject.class);
            if (response == null) {
                throw new IOException("Empty response from API");
            }
            return response;
        }
    }

    private static String parseResponseContent(JsonObject response) {
        return response.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
    }

    private static HttpURLConnection createConnection() throws IOException {
        URL url = new URL(API_ENDPOINT);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + Config.APIKey);
        connection.setDoOutput(true);
        return connection;
    }

    public static void chat(ServerPlayer player, String userMessage, Consumer<String> responseHandler) {
        chatAsync(player, userMessage)
                .thenAccept(response -> {
                    responseHandler.accept(response);
                    chatHistory.addAssistantMessage(jasonBotPlayer.getDisplayName().getString(), response);
                })
                .exceptionally(ex -> {
                    JasonBot.LOGGER.error(ex.getCause().getMessage());
                    return null;
                });
    }
}