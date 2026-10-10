package com.aliucord.plugins;

import android.content.Context;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import com.aliucord.Http;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.discord.databinding.UserProfileHeaderBadgeBinding;
import com.discord.utilities.views.SimpleRecyclerAdapter;
import com.discord.widgets.user.Badge;
import com.discord.widgets.user.profile.UserProfileHeaderView;
import com.discord.widgets.user.profile.UserProfileHeaderViewModel;
import org.json.JSONObject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@AliucordPlugin
public class GlobalBadges extends Plugin {
    private static final String MARKER = "wolf.globalbadges:";
    private static final String FEED = "https://badges.equicord.org/users";
    private final Field adapterField = field(UserProfileHeaderView.class, "badgesAdapter");
    private final Field dataField = field(SimpleRecyclerAdapter.class, "data");
    private final Field bindingField = field(UserProfileHeaderView.BadgeViewHolder.class, "binding");
    private final Map<UserProfileHeaderView, Long> headers = new WeakHashMap<>();
    private final GlobalBadgeImages images = new GlobalBadgeImages();
    private Map<Long, List<GlobalBadgeData.Entry>> users = Collections.emptyMap();
    private volatile int generation;
    private boolean loading;
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            fetch();
            Utils.mainThread.postDelayed(this, 30 * 60 * 1000L);
        }
    };

    public GlobalBadges() {
        settingsTab = new SettingsTab(GlobalBadgesSettings.class).withArgs(settings);
    }

    private static Field field(Class<?> type, String name) {
        try {
            Field result = type.getDeclaredField(name);
            result.setAccessible(true);
            return result;
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }

    @Override public void start(Context context) {
        generation++;
        patcher.patch(UserProfileHeaderView.class, "updateViewState",
            new Class<?>[]{UserProfileHeaderViewModel.ViewState.Loaded.class}, new Hook(frame -> {
                try {
                    var header = (UserProfileHeaderView) frame.thisObject;
                    var state = (UserProfileHeaderViewModel.ViewState.Loaded) frame.args[0];
                    long id = state.getUser().getId();
                    headers.put(header, id);
                    update(header, id);
                } catch (Exception error) { logger.error("Could not update global badges", error); }
            }));
        patcher.patch(UserProfileHeaderView.BadgeViewHolder.class, "bind", new Class<?>[]{Badge.class}, new Hook(frame -> {
            try {
                var badge = (Badge) frame.args[0];
                var binding = (UserProfileHeaderBadgeBinding) bindingField.get(frame.thisObject);
                images.unbind(binding.b);
                if (!isOurs(badge)) return;
                binding.b.setVisibility(View.VISIBLE);
                binding.b.setContentDescription(badge.getTooltip());
                binding.b.setOnClickListener(v -> Utils.showToast(badge.getTooltip().toString()));
                images.bind(binding.b, badge.getText().toString().substring(MARKER.length()));
            } catch (Exception error) { logger.error("Could not bind global badge image", error); }
        }));
        refresh.run();
    }

    private void fetch() {
        if (loading) return;
        loading = true;
        int requestGeneration = generation;
        Utils.threadPool.execute(() -> {
            try (var request = new Http.Request(FEED).setRequestTimeout(30_000)) {
                var response = request.execute();
                if (!response.ok()) throw new IllegalStateException("Badge service HTTP " + response.statusCode);
                var parsed = GlobalBadgeData.parse(new JSONObject(response.text()));
                Utils.mainThread.post(() -> {
                    if (generation != requestGeneration) return;
                    users = parsed;
                    for (var entry : headers.entrySet()) {
                        try { update(entry.getKey(), entry.getValue()); }
                        catch (Exception error) { logger.error("Could not refresh profile badges", error); }
                    }
                });
            } catch (Exception error) {
                if (generation == requestGeneration) logger.error("Could not refresh global badges", error);
            } finally {
                Utils.mainThread.post(() -> { if (generation == requestGeneration) loading = false; });
            }
        });
    }

    private List<Badge> nativeBadges(Object value) {
        List<Badge> badges = new ArrayList<>();
        if (value instanceof List<?>) for (Object entry : (List<?>) value)
            if (entry instanceof Badge && !isOurs((Badge) entry)) badges.add((Badge) entry);
        return badges;
    }

    private void update(UserProfileHeaderView header, long id) throws IllegalAccessException {
        var adapter = (RecyclerView.Adapter<?>) adapterField.get(header);
        Object current = dataField.get(adapter);
        List<Badge> nativeBadges = nativeBadges(current);
        List<Badge> custom = new ArrayList<>();
        for (var badge : users.getOrDefault(id, Collections.emptyList())) {
            if (!settings.getBool("provider." + badge.mod, true)) continue;
            String provider = GlobalBadgeData.PROVIDERS.getOrDefault(badge.mod, badge.mod);
            String tooltip = badge.tooltip;
            int style = settings.getInt("modStyle", 0);
            if (style == 1) tooltip = provider + " - " + tooltip;
            else if (style == 2) tooltip += " - " + provider;
            // Null objectType leaves image loading to this plugin instead of the core loader.
            custom.add(new Badge(0, MARKER + badge.url, tooltip, false, null));
        }
        List<Badge> merged = new ArrayList<>();
        boolean left = settings.getBool("left", false);
        merged.addAll(left ? custom : nativeBadges);
        merged.addAll(left ? nativeBadges : custom);
        if (!merged.equals(current)) {
            dataField.set(adapter, merged);
            adapter.notifyDataSetChanged();
        }
    }

    @Override public void stop(Context context) {
        generation++;
        Utils.mainThread.removeCallbacks(refresh);
        patcher.unpatchAll();
        for (var header : headers.keySet()) {
            try {
                var adapter = (RecyclerView.Adapter<?>) adapterField.get(header);
                dataField.set(adapter, nativeBadges(dataField.get(adapter)));
                adapter.notifyDataSetChanged();
            } catch (Exception error) { logger.error("Could not remove global badges", error); }
        }
        headers.clear();
        users = Collections.emptyMap();
        images.clear();
        loading = false;
    }

    private static boolean isOurs(Badge badge) {
        return badge.getText() != null && badge.getText().toString().startsWith(MARKER);
    }
}
