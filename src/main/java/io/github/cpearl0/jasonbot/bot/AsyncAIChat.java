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
import java.util.regex.Pattern;

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
    private static final Pattern SENTENCE_PATTERN = Pattern.compile(
            "([^。！？.!?\\n]+[。！？.!?]+|[^\\n]+\\n+)"
    );

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

    public static CompletableFuture<String> chatProactive(String eventMessage) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                chatHistory.addUserMessage("server", eventMessage);

                List<JsonObject> messages = new ArrayList<>();
                JsonObject systemMsg = new JsonObject();
                systemMsg.addProperty("role", "system");
                systemMsg.addProperty("content", PromptGenerator.generateProactivePrompt());
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

                JsonObject requestBody = buildRequestWithoutTools(messages);
                JsonObject responseJson = sendRequest(requestBody);
                return parseResponseContent(responseJson);
            } catch (Exception e) {
                throw new CompletionException("Proactive API Failed", e);
            }
        }, executor);
    }

    private static String chatWithTools(ServerPlayer player, String userMessage) throws IOException {
        var userName = player.getDisplayName().getString();

        chatHistory.addUserMessage(userName, "[" + userName + "] " + userMessage);

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
                JasonBot.LOGGER.info("Chat round {}: no tool_calls, returning text directly", round + 1);
                String content = message.get("content").getAsString();
                JasonBot.LOGGER.info("Model response (first 200 chars): {}", content.substring(0, Math.min(content.length(), 200)));
                return content;
            }

            JasonBot.LOGGER.info("Chat round {}: {} tool call(s)", round + 1, toolCalls.size());
            messages.add(message);

            for (JsonElement tc : toolCalls) {
                JsonObject toolCall = tc.getAsJsonObject();
                String callId = toolCall.get("id").getAsString();
                JsonObject fnObj = toolCall.getAsJsonObject("function");
                String fnName = fnObj.get("name").getAsString();
                String fnArgs = fnObj.get("arguments").getAsString();

                String result = GameTools.executeTool(player, fnName, fnArgs);

                JasonBot.LOGGER.info("Tool {} result (first 500 chars): {}", fnName,
                        result.substring(0, Math.min(result.length(), 500)));

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

        chatHistory.addUserMessage(userName, "[" + userName + "] " + userMessage);

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
                    String formatted = ResponseFormatter.format(response);
                    responseHandler.accept(formatted);
                    chatHistory.addAssistantMessage(jasonBotPlayer.getDisplayName().getString(), formatted);
                    ProactiveChat.onAISpoke();
                })
                .exceptionally(ex -> {
                    JasonBot.LOGGER.error(ex.getCause().getMessage());
                    return null;
                });
    }

    public static void chatStreaming(ServerPlayer triggerPlayer, String userMessage,
                                      Consumer<String> onSentence, Runnable onComplete) {
        var userName = triggerPlayer.getDisplayName().getString();
        chatHistory.addUserMessage(userName, "[" + userName + "] " + userMessage);

        executor.submit(() -> {
            try {
                String fullResponse;
                if (!Config.useInGameInformation && !Config.webSearchEnabled) {
                    fullResponse = chatWithoutTools(triggerPlayer, userMessage);
                } else {
                    fullResponse = chatWithToolsForStreaming(triggerPlayer, userMessage);
                }

                if (fullResponse == null || fullResponse.isEmpty()) {
                    onComplete.run();
                    return;
                }

                String formatted = ResponseFormatter.format(fullResponse);
                chatHistory.addAssistantMessage(jasonBotPlayer.getDisplayName().getString(), formatted);
                ProactiveChat.onAISpoke();

                if (!Config.streamingEnabled) {
                    String aiName = jasonBotPlayer.getDisplayName().getString();
                    String chatContent = "[" + aiName + "] " + formatted;
                    onSentence.accept(chatContent);
                    onComplete.run();
                    return;
                }

                List<String> sentences = splitSentences(formatted);

                if (sentences.isEmpty()) {
                    String aiName = jasonBotPlayer.getDisplayName().getString();
                    onSentence.accept("[" + aiName + "] " + formatted);
                    onComplete.run();
                    return;
                }

                deliverChunks(triggerPlayer, sentences, onSentence, onComplete);

            } catch (Exception e) {
                JasonBot.LOGGER.error("Streaming failed: {}", e.getMessage());
                onComplete.run();
            }
        });
    }

    private static String chatWithToolsForStreaming(ServerPlayer player, String userMessage) throws IOException {
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
                JasonBot.LOGGER.info("Streaming round {}: no tool_calls, returning text directly", round + 1);
                String content = message.get("content").getAsString();
                JasonBot.LOGGER.info("Model response (first 200 chars): {}", content.substring(0, Math.min(content.length(), 200)));
                return content;
            }

            JasonBot.LOGGER.info("Streaming round {}: {} tool call(s)", round + 1, toolCalls.size());
            messages.add(message);

            for (JsonElement tc : toolCalls) {
                JsonObject toolCall = tc.getAsJsonObject();
                String callId = toolCall.get("id").getAsString();
                JsonObject fnObj = toolCall.getAsJsonObject("function");
                String fnName = fnObj.get("name").getAsString();
                String fnArgs = fnObj.get("arguments").getAsString();

                String result = GameTools.executeTool(player, fnName, fnArgs);

                JasonBot.LOGGER.info("Streaming tool {} result (first 500 chars): {}", fnName,
                        result.substring(0, Math.min(result.length(), 500)));

                JsonObject toolMsg = new JsonObject();
                toolMsg.addProperty("role", "tool");
                toolMsg.addProperty("tool_call_id", callId);
                toolMsg.addProperty("content", result);
                messages.add(toolMsg);
            }
            JasonBot.LOGGER.debug("Streaming tool call round {}: continuing...", round + 1);
        }

        return Component.translatable("info.jasonbot.APIfailed").getString();
    }

    private static List<String> splitSentences(String text) {
        List<String> result = new ArrayList<>();
        var matcher = SENTENCE_PATTERN.matcher(text);
        while (matcher.find()) {
            String sentence = matcher.group().trim();
            if (!sentence.isEmpty()) {
                result.add(sentence);
            }
        }
        if (result.isEmpty() && !text.trim().isEmpty()) {
            result.add(text.trim());
        }
        return result;
    }

    private static void deliverChunks(ServerPlayer triggerPlayer, List<String> sentences,
                                       Consumer<String> onSentence, Runnable onComplete) {
        ScheduledExecutorService chunkScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "JasonBot-ChunkDelivery");
            t.setDaemon(true);
            return t;
        });

        int delay = Config.streamingChunkDelayMs;
        String aiName = jasonBotPlayer.getDisplayName().getString();

        for (int i = 0; i < sentences.size(); i++) {
            final String sentence = sentences.get(i);
            final boolean isFirst = (i == 0);
            final boolean showName = isFirst || Config.showAINameOnEachChunk;

            long delayMs = (long) i * delay;
            chunkScheduler.schedule(() -> {
                try {
                    String colorPrefix = (Config.aiResponseColor != null && !Config.aiResponseColor.isEmpty() && Config.aiResponseColor != "\n")
                            ? Config.aiResponseColor : "§b";
                    String colored = sentence.startsWith("§") ? sentence : colorPrefix + sentence;
                    String actionBarContent = colored.length() > 60
                            ? colored.substring(0, 60) : colored;
                    Component actionBarText = Component.literal(actionBarContent);
                    triggerPlayer.displayClientMessage(actionBarText, true);

                    String chatContent = showName ? "[" + aiName + "] " + colored : colored;
                    onSentence.accept(chatContent);
                } catch (Exception e) {
                    JasonBot.LOGGER.error("Chunk delivery error: {}", e.getMessage());
                }
            }, delayMs, TimeUnit.MILLISECONDS);
        }

        long completionDelay = (long) sentences.size() * delay + delay;
        chunkScheduler.schedule(() -> {
            try {
                triggerPlayer.displayClientMessage(Component.literal(""), true);
            } catch (Exception ignored) {}
            chunkScheduler.shutdown();
            onComplete.run();
        }, completionDelay, TimeUnit.MILLISECONDS);
    }
}