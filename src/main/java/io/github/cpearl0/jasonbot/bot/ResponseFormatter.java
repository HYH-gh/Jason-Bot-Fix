package io.github.cpearl0.jasonbot.bot;

import io.github.cpearl0.jasonbot.Config;

import java.util.regex.Pattern;

public class ResponseFormatter {

    private static final String R = "§r";

    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern BOLD_ITALIC_PATTERN = Pattern.compile("\\*\\*\\*(.+?)\\*\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<![*])\\*([^*\\n]+?)\\*(?![*])");
    private static final Pattern CODE_PATTERN = Pattern.compile("`([^`\\n]+)`");
    private static final Pattern HEADING_PATTERN = Pattern.compile("(?m)^#{1,3}\\s+(.+)$");
    private static final Pattern LIST_PATTERN = Pattern.compile("(?m)^[-*]\\s");

    public static String format(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;

        result = BOLD_ITALIC_PATTERN.matcher(result).replaceAll("§l§o$1" + R);
        result = BOLD_PATTERN.matcher(result).replaceAll("§l$1" + R);
        result = ITALIC_PATTERN.matcher(result).replaceAll("§o$1" + R);
        result = CODE_PATTERN.matcher(result).replaceAll("§7$1" + R);
        result = HEADING_PATTERN.matcher(result).replaceAll("§l§n$1" + R);
        result = LIST_PATTERN.matcher(result).replaceAll("§7• ");

        if (Config.aiResponseColor != null && !Config.aiResponseColor.isEmpty()) {
            result = Config.aiResponseColor + result;
        }

        return result;
    }
}