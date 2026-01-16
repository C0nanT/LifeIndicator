package com.example.plugin;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
        this.getCommandRegistry().registerCommand(
            new VidaCommand(
                "vida",
                "Mostra a vida da entidade que você está olhando.",
                false
            )
        );

        // Registrar tarefa periódica para mostrar vida automaticamente
        @SuppressWarnings("unchecked")
        ScheduledFuture<Void> task = (ScheduledFuture<Void>) HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
            new LifeIndicatorTask(),
            0,
            200,
            TimeUnit.MILLISECONDS
        );
        this.getTaskRegistry().registerTask(task);
    }
}
