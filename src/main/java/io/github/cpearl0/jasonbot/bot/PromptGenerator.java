package io.github.cpearl0.jasonbot.bot;

import io.github.cpearl0.jasonbot.Config;
import net.minecraft.server.level.ServerPlayer;

public class PromptGenerator {
    public static String generatePrompt(ServerPlayer player) {
        StringBuilder prompt = new StringBuilder(Config.systemPrompt);

        prompt.append("\n这是一个Minecraft多人服务器，你可能同时与多个玩家对话。");
        prompt.append("每条玩家消息的格式为 [玩家名] 消息内容，请通过消息前缀 [玩家名] 来区分是谁在和你说话。");
        prompt.append("当玩家要求你执行操作（如给予物品、查询信息）时，请确认是在为消息中标注的该玩家操作。");

        if (Config.useInGameInformation) {
            prompt.append("\n你可以通过调用函数来获取当前与你对话的玩家的游戏内信息。");
            prompt.append("请务必在回答需要游戏信息的问题前先调用相关函数获取最新数据。");
            prompt.append("\n你还可以通过 execute_command 函数来执行Minecraft原版指令，");
            prompt.append("如给予物品、修改时间、切换天气等。注意某些敏感指令（如op、ban、stop等）已被禁用。");
            prompt.append("\n你还可以通过 get_online_players 函数来获取服务器所有在线玩家的完整列表（包括名称和UUID）。");
        }

        if (Config.webSearchEnabled) {
            prompt.append("\n你可以通过调用 web_search 函数来联网搜索实时信息。");
            prompt.append("当玩家询问的问题涉及最新Minecraft版本特性、模组攻略、合成配方、红石机械等超出你训练数据范围的内容时，");
            prompt.append("请主动调用 web_search 获取准确信息后再回答。");
        }

        return prompt.toString();
    }

    public static String generateProactivePrompt() {
        StringBuilder prompt = new StringBuilder(Config.systemPrompt);

        prompt.append("\n这是一个Minecraft多人服务器，你可能同时与多个玩家对话。");
        prompt.append("每条玩家消息的格式为 [玩家名] 消息内容，名字为 \"server\" 的消息是服务器系统事件通知。");
        prompt.append("当收到服务器事件通知时（如玩家加入、死亡、聊天活跃等），请自然地发表评论、欢迎或吐槽。");
        prompt.append("回复要简短、有趣、有活人感，不要长篇大论，像普通玩家一样自然地说话。");

        return prompt.toString();
    }
}