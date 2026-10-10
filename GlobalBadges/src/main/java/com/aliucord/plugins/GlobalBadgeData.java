package com.aliucord.plugins;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class GlobalBadgeData {
    static final Map<String, String> PROVIDERS = new LinkedHashMap<>();
    static {
        String[][] names = {{"aero", "Aero"}, {"raincord", "Raincord"},
            {"badgevault", "BadgeVault"}, {"betterdiscord", "BetterDiscord"}, {"bunny", "Bunny"},
            {"enmity", "Enmity"}, {"equicord", "Equicord"}, {"goosemod", "GooseMod"},
            {"nekocord", "Nekocord"}, {"paicord", "Paicord"}, {"record", "ReCord"},
            {"replugged", "Replugged"}, {"revenge", "Revenge"}, {"reviewdb", "ReviewDB"},
            {"velocity", "Velocity"}, {"vencord", "Vencord"}, {"vendroidenhanced", "Vendroid Enhanced"}};
        for (String[] name : names) PROVIDERS.put(name[0], name[1]);
    }

    static final class Entry {
        final String mod, url, tooltip;
        Entry(String mod, String url, String tooltip) {
            this.mod = mod;
            this.url = url;
            this.tooltip = tooltip;
        }
    }

    static Map<Long, List<Entry>> parse(JSONObject root) throws Exception {
        JSONObject users = root.optJSONObject("users");
        if (users == null) throw new IllegalArgumentException("Invalid global badges response");
        Map<Long, List<Entry>> parsed = new HashMap<>();
        var keys = users.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            long id;
            try { id = Long.parseLong(key); }
            catch (NumberFormatException ignored) { continue; }
            JSONArray entries = users.optJSONArray(key);
            if (entries == null) continue;
            List<Entry> badges = new ArrayList<>();
            for (int index = 0; index < entries.length(); index++) {
                JSONObject entry = entries.optJSONObject(index);
                if (entry == null) continue;
                Object mod = entry.opt("mod"), url = entry.opt("badge"), tooltip = entry.opt("tooltip");
                if (!(mod instanceof String) || !(url instanceof String) || !(tooltip instanceof String)) continue;
                if ("aliucord".equalsIgnoreCase((String) mod)) continue;
                String image = (String) url;
                if (((String) mod).isEmpty() || !(image.regionMatches(true, 0, "https://", 0, 8)
                    || image.regionMatches(true, 0, "http://", 0, 7))) continue;
                badges.add(new Entry((String) mod, image, (String) tooltip));
            }
            if (!badges.isEmpty()) parsed.put(id, badges);
        }
        return parsed;
    }
}
