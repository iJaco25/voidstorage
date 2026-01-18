package net.momo.platform.hytale.impl;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.momo.platform.hytale.adapter.InteractionContextAdapter;
import net.momo.platform.hytale.adapter.InventoryAdapter;
import net.momo.platform.hytale.adapter.WorldAdapter;
import net.momo.silo.util.Position;

import javax.annotation.Nullable;
import java.util.UUID;

/** Hytale implementation of InteractionContextAdapter. */
public final class HytaleInteractionContextAdapter implements InteractionContextAdapter {

    private final Ref<EntityStore> ref;
    private final InteractionContext context;
    private final boolean firstRun;

    public HytaleInteractionContextAdapter(Ref<EntityStore> ref, InteractionContext context, boolean firstRun) {
        this.ref = ref;
        this.context = context;
        this.firstRun = firstRun;
    }

    @Override
    public UUID getPlayerId() {
        PlayerRef playerRef = ref.getStore().getComponent(ref, PlayerRef.getComponentType());
        return playerRef != null ? playerRef.getUuid() : null;
    }

    @Override
    @Nullable
    public Position getTargetBlockPosition() {
        var target = context.getTargetBlock();
        if (target == null) return null;
        return Position.of(target.x, target.y, target.z);
    }

    @Override
    @Nullable
    public Position getPlayerPosition() {
        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        if (player == null) return null;

        var transform = player.getTransformComponent();
        if (transform == null) return null;

        var pos = transform.getPosition();
        return Position.of((int) pos.getX(), (int) pos.getY(), (int) pos.getZ());
    }

    @Override
    public WorldAdapter getWorld() {
        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        if (player == null) return null;
        return new HytaleWorldAdapter(player.getWorld());
    }

    @Override
    public InventoryAdapter getInventory() {
        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        if (player == null) return null;
        return new HytaleInventoryAdapter(player.getInventory());
    }

    @Override
    public boolean isFirstRun() {
        return firstRun;
    }
}
