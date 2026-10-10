package com.aliucord.plugins;

final class ClapMessage {
    private static final String SEPARATOR = " \uD83D\uDC4F ";

    static String format(String message) {
        if (message.trim().indexOf(' ') == -1) {
            // Kotlin split("") includes empty entries before and after the characters.
            StringBuilder result = new StringBuilder(SEPARATOR);
            for (int index = 0; index < message.length(); index++)
                result.append(message.charAt(index)).append(SEPARATOR);
            return result.toString();
        }
        String[] words = message.split(" ", -1);
        int count = words.length;
        while (count > 0 && words[count - 1].isEmpty()) count--;
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < count; index++) {
            if (index > 0) result.append(SEPARATOR);
            result.append(words[index]);
        }
        return result.toString();
    }
}
