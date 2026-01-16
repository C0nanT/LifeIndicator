package com.example.plugin;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class LifeIndicatorTask implements Runnable {

    private static final double MAX_DISTANCE = 30.0;
    private static final double MIN_DOT_PRODUCT = 0.9;

    // Mapa de jogador -> última entidade olhada (para limpar nameplate quando mudar de alvo)
    private final Map<UUID, Ref<EntityStore>> lastLookedEntity = new ConcurrentHashMap<>();

    @Override
    public void run() {
        try {
            Universe universe = Universe.get();
            if (universe == null) return;

            for (World world : universe.getWorlds().values()) {
                world.execute(() -> processWorld(world));
            }
        } catch (Exception e) {
            // Ignorar erros silenciosamente
        }
    }

    private void processWorld(World world) {
        Store<EntityStore> store = world.getEntityStore().getStore();

        for (PlayerRef playerRef : world.getPlayerRefs()) {
            try {
                processPlayer(store, playerRef);
            } catch (Exception e) {
                // Ignorar erros por jogador
            }
        }
    }

    private void processPlayer(Store<EntityStore> store, PlayerRef playerRef) {
        Ref<EntityStore> playerEntityRef = playerRef.getReference();
        if (playerEntityRef == null) return;

        UUID playerUuid = playerRef.getUuid();

        // Obter posição e direção do olhar do jogador
        TransformComponent playerTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        HeadRotation headRotation = store.getComponent(playerEntityRef, HeadRotation.getComponentType());

        if (playerTransform == null || headRotation == null) return;

        Vector3d playerPos = playerTransform.getPosition();
        Vector3d lookDirection = headRotation.getDirection();

        // Encontrar entidade na direção do olhar
        EntityResult result = findEntityInLookDirection(store, playerEntityRef, playerPos, lookDirection);

        Ref<EntityStore> lastEntity = lastLookedEntity.get(playerUuid);

        if (result == null) {
            // Não está olhando para nenhuma entidade - remover nameplate da última
            if (lastEntity != null && lastEntity.isValid()) {
                store.tryRemoveComponent(lastEntity, Nameplate.getComponentType());
            }
            lastLookedEntity.remove(playerUuid);
            return;
        }

        // Se mudou de entidade, remover nameplate da anterior
        if (lastEntity != null && lastEntity.isValid() && !lastEntity.equals(result.entityRef)) {
            store.tryRemoveComponent(lastEntity, Nameplate.getComponentType());
        }

        // Atualizar nameplate na entidade atual
        String healthBar = createHealthBar(result.health, result.maxHealth);
        Nameplate nameplate = store.ensureAndGetComponent(result.entityRef, Nameplate.getComponentType());
        nameplate.setText(healthBar);

        // Salvar referência da entidade atual
        lastLookedEntity.put(playerUuid, result.entityRef);
    }

    private String createHealthBar(float health, float maxHealth) {
        int barLength = 15;
        float percentage = health / maxHealth;
        int filledBars = Math.round(percentage * barLength);

        StringBuilder bar = new StringBuilder();
        bar.append("[");
        for (int i = 0; i < barLength; i++) {
            if (i < filledBars) {
                bar.append("|");
            } else {
                bar.append(" ");
            }
        }
        bar.append("]");

        return bar.toString();
    }

    private EntityResult findEntityInLookDirection(
        Store<EntityStore> store,
        Ref<EntityStore> playerRef,
        Vector3d playerPos,
        Vector3d lookDirection
    ) {
        AtomicReference<EntityResult> closestResult = new AtomicReference<>(null);
        AtomicReference<Double> closestDistance = new AtomicReference<>(MAX_DISTANCE);

        store.forEachChunk((ArchetypeChunk<EntityStore> chunk, CommandBuffer<EntityStore> buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                Ref<EntityStore> entityRef = chunk.getReferenceTo(i);

                // Ignorar o próprio jogador
                if (entityRef.equals(playerRef)) {
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

                // Calcular distância
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

                // Calcular dot product
                double dot = lookDirection.dot(toEntity);

                // Verificar se está na direção do olhar
                if (dot > MIN_DOT_PRODUCT && distance < closestDistance.get()) {
                    // Obter vida
                    int healthIndex = DefaultEntityStatTypes.getHealth();
                    EntityStatValue healthValue = entityStats.get(healthIndex);

                    if (healthValue != null) {
                        closestDistance.set(distance);
                        closestResult.set(new EntityResult(
                            entityRef,
                            healthValue.get(),
                            healthValue.getMax()
                        ));
                    }
                }
            }
        });

        return closestResult.get();
    }

    private static class EntityResult {
        final Ref<EntityStore> entityRef;
        final float health;
        final float maxHealth;

        EntityResult(Ref<EntityStore> entityRef, float health, float maxHealth) {
            this.entityRef = entityRef;
            this.health = health;
            this.maxHealth = maxHealth;
        }
    }
}
