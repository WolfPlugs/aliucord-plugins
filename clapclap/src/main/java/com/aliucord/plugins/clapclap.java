/*
 * Wolf's Aliucord Plugins
 * Copyright (C) 2021 Wolfkid200444
 * Licensed under the Apache License, Version 2.0.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package com.aliucord.plugins;

import android.content.Context;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;
import java.util.Collections;

@AliucordPlugin
public class clapclap extends Plugin {
    @Override public void start(Context context) {
        commands.registerCommand("clap", "Clap your messages",
            Collections.singletonList(CommandsAPI.requiredMessageOption),
            ctx -> new CommandsAPI.CommandResult(ClapMessage.format(ctx.getRequiredString("message"))));
    }

    @Override public void stop(Context context) {
        commands.unregisterAll();
    }
}
