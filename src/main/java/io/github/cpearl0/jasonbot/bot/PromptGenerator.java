package io.github.cpearl0.jasonbot.bot;

import io.github.cpearl0.jasonbot.Config;
import net.minecraft.server.level.ServerPlayer;

public class PromptGenerator {
    public static String generatePrompt(ServerPlayer player) {
        StringBuilder prompt = new StringBuilder(Config.systemPrompt);

        if (Config.useInGameInformation) {
            prompt.append("\n你可以通过调用函数来获取当前玩家的游戏内信息。");
            prompt.append("请务必在回答需要游戏信息的问题前先调用相关函数获取最新数据。");
            prompt.append("\n你还可以通过 execute_command 函数来执行Minecraft原版指令，");
            prompt.append("如给予物品、修改时间、切换天气等。注意某些敏感指令（如op、ban、stop等）已被禁用。");
        }

        if (Config.webSearchEnabled) {
            prompt.append("\n你可以通过调用 web_search 函数来联网搜索实时信息。");
            prompt.append("当玩家询问的问题涉及最新Minecraft版本特性、模组攻略、合成配方、红石机械等超出你训练数据范围的内容时，");
            prompt.append("请主动调用 web_search 获取准确信息后再回答。");
        }

        return prompt.toString();
    }
}