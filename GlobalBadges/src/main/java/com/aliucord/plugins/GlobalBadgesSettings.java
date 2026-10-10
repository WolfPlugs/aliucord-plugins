package com.aliucord.plugins;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;
import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.fragments.SettingsPage;
import com.discord.views.CheckedSetting;
import com.lytefast.flexinput.R;

public class GlobalBadgesSettings extends SettingsPage {
    private final SettingsAPI settings;
    private boolean updatingStyle;

    public GlobalBadgesSettings(SettingsAPI settings) { this.settings = settings; }

    @Override public void onViewBound(View view) {
        super.onViewBound(view);
        setActionBarTitle("Global Badges");
        var context = view.getContext();
        text(context, "Show profile badges from other Discord clients and BadgeVault.",
            R.i.UiKit_Settings_Item_SubText);

        text(context, "Appearance", R.i.UiKit_Settings_Item_Header);
        switchRow(context, "Show custom badges first",
            "Place global badges before Discord badges.", "left", false);

        text(context, "Client names", R.i.UiKit_Settings_Item_Header);
        text(context, "Choose how client names appear when you tap a badge.",
            R.i.UiKit_Settings_Item_SubText);
        String[] labels = {"Hide client names", "Before badge name", "After badge name"};
        String[] examples = {"Developer", "Revenge - Developer", "Developer - Revenge"};
        CheckedSetting[] choices = new CheckedSetting[labels.length];
        int selected = Math.max(0, Math.min(2, settings.getInt("modStyle", 0)));
        for (int index = 0; index < labels.length; index++) {
            var row = Utils.createCheckedSetting(context, CheckedSetting.ViewType.RADIO,
                labels[index], examples[index]);
            row.setChecked(index == selected);
            choices[index] = row;
            addView(row);
        }
        for (int index = 0; index < choices.length; index++) {
            final int value = index;
            choices[index].setOnCheckedListener(checked -> {
                if (updatingStyle) return;
                updatingStyle = true;
                try {
                    if (checked) settings.setInt("modStyle", value);
                    int current = Math.max(0, Math.min(2, settings.getInt("modStyle", 0)));
                    for (int option = 0; option < choices.length; option++)
                        choices[option].setChecked(option == current);
                } finally { updatingStyle = false; }
            });
        }

        text(context, "Badge providers", R.i.UiKit_Settings_Item_Header);
        text(context, "Turn off a provider to hide its badges. Reopen a profile to apply changes.",
            R.i.UiKit_Settings_Item_SubText);
        for (var provider : GlobalBadgeData.PROVIDERS.entrySet())
            switchRow(context, provider.getValue(), null, "provider." + provider.getKey(), true);

        text(context, "BadgeVault", R.i.UiKit_Settings_Item_Header);
        var vault = text(context, "Get a BadgeVault badge", R.i.UiKit_Settings_Item);
        vault.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW,
            Uri.parse("https://discord.gg/eTvYv95PCG"))));
        text(context, "Opens the BadgeVault Discord server. Badges refresh every 30 minutes.",
            R.i.UiKit_Settings_Item_SubText);
    }

    private TextView text(Context context, String content, int style) {
        var row = new TextView(context, null, 0, style);
        row.setText(content);
        addView(row);
        return row;
    }

    private void switchRow(Context context, String title, String subtitle, String key, boolean defaultValue) {
        var row = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, title, subtitle);
        row.setChecked(settings.getBool(key, defaultValue));
        row.setOnCheckedListener(checked -> settings.setBool(key, checked));
        addView(row);
    }
}
