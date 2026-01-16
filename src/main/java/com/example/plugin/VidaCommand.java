package com.example.plugin;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicReference;

public class VidaCommand extends AbstractPlayerCommand {

    private static final double MAX_DISTANCE = 30.0;
    private static final double MIN_DOT_PRODUCT = 0.9;

    public VidaCommand(@Nonnull String name, @Nonnull String description, boolean requiresConfirmation) {
        super(name, description, requiresConfirmation);
    }

    @Override
    protected void execute(
        @Nonnull CommandContext commandContext,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> playerEntityRef,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        // Obter posição e direção do olhar do jogador
        TransformComponent playerTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        HeadRotation headRotation = store.getComponent(playerEntityRef, HeadRotation.getComponentType());

        if (playerTransform == null || headRotation == null) {
            playerRef.sendMessage(Message.raw("Erro ao obter posição do jogador.").color("#FF0000"));
            return;
        }

        Vector3d playerPos = playerTransform.getPosition();
        Vector3d lookDirection = headRotation.getDirection();

        // Variáveis para armazenar a entidade mais próxima
        AtomicReference<Ref<EntityStore>> closestEntityRef = new AtomicReference<>(null);
        AtomicReference<Double> closestDistance = new AtomicReference<>(MAX_DISTANCE);

        // Iterar sobre todos os chunks de entidades
        store.forEachChunk((ArchetypeChunk<EntityStore> chunk, CommandBuffer<EntityStore> buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                Ref<EntityStore> entityRef = chunk.getReferenceTo(i);

                // Ignorar o próprio jogador
                if (entityRef.equals(playerEntityRef)) {
                    continue;
                }

                // Verificar se a entidade tem TransformComponent
                TransformComponent entityTransform = chunk.getComponent(i, TransformComponent.getComponentType());
                if (entityTransform == null) {
                    continue;
                }

                // Verificar se a entidade tem stats (vida)
                EntityStatMap entityStats = chunk.getComponent(i, EntityStatMap.getComponentType());
                if (entityStats == null) {
                    continue;
                }

                Vector3d entityPos = entityTransform.getPosition();

                // Calcular distância até a entidade
                double distance = playerPos.distanceTo(entityPos);

                // Verificar se está dentro do alcance
                if (distance > MAX_DISTANCE || distance < 0.5) {
                    continue;
                }

                // Calcular vetor normalizado do jogador até a entidade
                Vector3d toEntity = new Vector3d(
                    entityPos.getX() - playerPos.getX(),
                    entityPos.getY() - playerPos.getY(),
                    entityPos.getZ() - playerPos.getZ()
                ).normalize();

                // Calcular dot product (ângulo entre a direção do olhar e a direção da entidade)
                double dot = lookDirection.dot(toEntity);

                // Se o dot product for maior que MIN_DOT_PRODUCT, a entidade está na direção do olhar
                if (dot > MIN_DOT_PRODUCT && distance < closestDistance.get()) {
                    closestDistance.set(distance);
                    closestEntityRef.set(entityRef);
                }
            }
        });

        Ref<EntityStore> targetRef = closestEntityRef.get();

        if (targetRef == null) {
            playerRef.sendMessage(Message.raw("Nenhuma entidade encontrada na direção do olhar.").color("#FFAA00"));
            return;
        }

        // Obter stats da entidade encontrada
        EntityStatMap stats = store.getComponent(targetRef, EntityStatMap.getComponentType());

        if (stats == null) {
            playerRef.sendMessage(Message.raw("Esta entidade não possui stats de vida.").color("#FFAA00"));
            return;
        }

        // Obter valor de vida
        int healthIndex = DefaultEntityStatTypes.getHealth();
        EntityStatValue healthValue = stats.get(healthIndex);

        if (healthValue == null) {
            playerRef.sendMessage(Message.raw("Não foi possível obter a vida da entidade.").color("#FFAA00"));
            return;
        }

        float vidaAtual = healthValue.get();
        float vidaMaxima = healthValue.getMax();

        // Enviar mensagem no chat
        playerRef.sendMessage(
            Message.raw("Vida: ")
                .color("#00FF00")
                .insert(Message.raw(String.format("%.1f / %.1f", vidaAtual, vidaMaxima)).color("#FFFFFF"))
        );
    }
}
