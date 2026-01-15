package com.example.plugin;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;

public class LifeIndicator extends JavaPlugin {
    // Plugin implementation goes here
    public LifeIndicator(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    public void setup() {
        super.setup();
        this.getCommandRegistry().registerCommand(
            new HelloCommand(
                "hello",
                "Sends a hello message to the player.",
                false
            )
        );
    }
}
