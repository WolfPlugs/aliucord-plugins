package com.aliucord.plugins;

public class ClapMessageTest {
    public static void main(String[] args) {
        String separator = " \uD83D\uDC4F ";
        check("hello world", "hello" + separator + "world");
        check("ab", separator + "a" + separator + "b" + separator);
        check("hello  world ", "hello" + separator + separator + "world");
        check("", separator);
        check("a ", separator + "a" + separator + " " + separator);
        System.out.println("Clap formatting checks passed");
    }
    private static void check(String input, String expected) {
        String actual = ClapMessage.format(input);
        if (!expected.equals(actual)) throw new AssertionError("Formatting changed for: " + input);
    }
}
