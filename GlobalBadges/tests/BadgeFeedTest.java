package com.aliucord.plugins;

import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Path;

/** Standalone JVM checks, independent of Android and Aliucord. */
public class BadgeFeedTest {
    public static void main(String[] args) throws Exception {
        String json = "{\"users\":{\"347096063569559553\":["
            + "{\"mod\":\"revenge\",\"badge\":\"https://example.com/badge.png\",\"tooltip\":\"Developer\"},"
            + "{\"mod\":\"future-client\",\"badge\":\"http://example.com/new.png\",\"tooltip\":\"Future\"},"
            + "{\"mod\":\"bad\",\"badge\":\"file:///tmp/image\",\"tooltip\":\"Invalid\"},"
            + "{\"mod\":\"bad\",\"badge\":42,\"tooltip\":\"Invalid\"},"
            + "{\"mod\":\"bad\",\"badge\":\"https://example.com/missing.png\"},null],"
            + "\"123\":[],\"not-a-snowflake\":[],\"456\":{},\"789\":[false]}}";
        var filtered = GlobalBadgeData.parse(new JSONObject("{\"users\":{\"123\":["
            + "{\"mod\":\"aliucord\",\"badge\":\"https://example.com/a.png\",\"tooltip\":\"Contributor\"},"
            + "{\"mod\":\"Aliucord\",\"badge\":\"https://example.com/b.png\",\"tooltip\":\"Developer\"}]}}"));
        require(filtered.isEmpty(), "Aliucord badges were not filtered");
        require(!GlobalBadgeData.PROVIDERS.containsKey("aliucord"), "Aliucord still appears in provider settings");
        var parsed = GlobalBadgeData.parse(new JSONObject(json));
        require(parsed.size() == 1, "Invalid users were not skipped");
        var badges = parsed.get(347096063569559553L);
        require(badges != null && badges.size() == 2, "Malformed badge handling or snowflake precision failed");
        require(badges.get(0).mod.equals("revenge") && badges.get(0).tooltip.equals("Developer"), "Valid badge changed");
        require(badges.get(1).mod.equals("future-client"), "Unknown provider was lost");
        boolean rejected = false;
        try { GlobalBadgeData.parse(new JSONObject("{}")); }
        catch (IllegalArgumentException expected) { rejected = true; }
        require(rejected, "Missing users accepted");
        require(GlobalBadgeData.parse(new JSONObject("{\"users\":{}}")).isEmpty(), "Empty feed failed");
        if (args.length > 0) {
            var live = GlobalBadgeData.parse(new JSONObject(Files.readString(Path.of(args[0]))));
            require(!live.isEmpty(), "Live feed parsed empty");
            System.out.println("Live feed parsed: " + live.size() + " users");
        }
        System.out.println("Badge feed checks passed");
    }
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
