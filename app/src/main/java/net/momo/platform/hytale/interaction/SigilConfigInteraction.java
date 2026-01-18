package net.momo.platform.hytale.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.momo.silo.core.Services;
import net.momo.silo.interaction.HandlerRegistry;
import net.momo.platform.hytale.impl.HytaleInteractionContextAdapter;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;

/** Hytale SimpleInteraction wrapper for Sigil configuration (F key on placed sigil). */
public class SigilConfigInteraction extends SimpleInteraction {

    public static final BuilderCodec<SigilConfigInteraction> CODEC =
        BuilderCodec.builder(SigilConfigInteraction.class, SigilConfigInteraction::new).build();

    private static final UUID HANDLER_ID = UUID.fromString("00000000-0000-0000-0001-000000000005");

    @Override
    public void handle(
            @NonNullDecl Ref<EntityStore> ref,
            boolean firstRun,
            float time,
            @NonNullDecl InteractionType type,
            @NonNullDecl InteractionContext interactionContext) {
        super.handle(ref, firstRun, time, type, interactionContext);

        if (type != InteractionType.Use) return;
        if (!Services.has(HandlerRegistry.class)) return;

        var context = new HytaleInteractionContextAdapter(ref, interactionContext, firstRun);
        Services.get(HandlerRegistry.class).dispatch(HANDLER_ID, context);
    }
}
