package net.momo.voidstorage.impl.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.momo.silo.core.ModConfig;
import net.momo.silo.core.Services;
import net.momo.voidstorage.internal.transfer.FilterMode;
import net.momo.voidstorage.internal.transfer.Transfer;
import net.momo.voidstorage.internal.transfer.TransferRegistry;
import net.momo.silo.ui.UIPageProvider;
import net.momo.voidstorage.impl.interaction.TransferConfigHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** UI provider for transfer configuration page. */
public final class TransferConfigPageProvider implements UIPageProvider {

    private static final int ITEMS_PER_ROW = 8;
    private final Map<UUID, State> states = new ConcurrentHashMap<>();

    @Override
    public String pageType() {
        return "transfer_config";
    }

    @Override
    public void open(UUID playerId, Map<String, Object> params) {
        Transfer transfer = (Transfer) params.get("transfer");
        String heldItemId = (String) params.get("heldItemId");
        if (transfer == null) {
            return;
        }

        resolvePlayer(playerId, (player, store) -> {
            TransferRegistry transferRegistry = Services.get(TransferRegistry.class);
            Transfer refreshed = transferRegistry.get(transfer.id()).orElse(transfer);
            State state = states.computeIfAbsent(playerId, id -> new State());
            state.lastHeldItem = heldItemId;
            player.getPageManager().openCustomPage(
                player.getReference(), store,
                new TransferPage(player, store, refreshed, state, this)
            );
        });
    }

    private void handleEvent(UUID playerId, Transfer transfer, State state, UIEventData data, Player player, Store<EntityStore> store) {
        if (data.action != null) {
            switch (data.action) {
                case "addHeld" -> {
                    if (state.lastHeldItem != null && !state.lastHeldItem.isBlank()) {
                        TransferConfigHandler.addFilter(transfer.id(), state.lastHeldItem);
                    }
                }
                case "clearFilters" -> TransferConfigHandler.clearFilters(transfer.id());
                case "toggleMode" -> TransferConfigHandler.toggleFilterMode(transfer.id());
            }
        }

        if (data.removeFilter != null && !data.removeFilter.isEmpty()) {
            TransferConfigHandler.removeFilter(transfer.id(), data.removeFilter);
        }

        TransferRegistry transferRegistry = Services.get(TransferRegistry.class);
        Transfer refreshed = transferRegistry.get(transfer.id()).orElse(transfer);
        player.getPageManager().openCustomPage(
            player.getReference(), store,
            new TransferPage(player, store, refreshed, state, this)
        );
    }

    private void resolvePlayer(UUID playerId, PlayerConsumer consumer) {
        for (World world : Universe.get().getWorlds().values()) {
            Player player = (Player) world.getEntity(playerId);
            if (player == null) continue;
            Ref<EntityStore> ref = player.getReference();
            if (ref == null || !ref.isValid()) continue;
            Store<EntityStore> store = ref.getStore();
            consumer.accept(player, store);
            return;
        }
    }

    @FunctionalInterface
    private interface PlayerConsumer {
        void accept(Player player, Store<EntityStore> store);
    }

    private static final class State {
        String lastHeldItem;
    }

    private static final class TransferPage extends InteractiveCustomUIPage<UIEventData> {
        private final Player player;
        private final Store<EntityStore> store;
        private final Transfer transfer;
        private final State state;
        private final TransferConfigPageProvider provider;

        TransferPage(Player player, Store<EntityStore> store, Transfer transfer, State state, TransferConfigPageProvider provider) {
            super(player.getPlayerRef(), CustomPageLifetime.CanDismiss, UIEventData.CODEC);
            this.player = player;
            this.store = store;
            this.transfer = transfer;
            this.state = state;
            this.provider = provider;
        }

        @Override
        public void build(Ref<EntityStore> ref, UICommandBuilder cmd, UIEventBuilder evt, Store<EntityStore> store) {
            cmd.append(ModConfig.uiPath("SigilConfigPage.ui"));

            cmd.set("#SigilType.Text", transfer.mode().displayName() + " Sigil");
            cmd.set("#ToggleMode.Text", transfer.filterMode().displayName());

            if (state.lastHeldItem != null && !state.lastHeldItem.isBlank()) {
                cmd.set("#HeldItemIcon.ItemId", state.lastHeldItem);
            }

            List<String> filters = new ArrayList<>(transfer.itemFilters());
            buildFilterGrid(cmd, evt, filters);

            evt.addEventBinding(CustomUIEventBindingType.Activating, "#AddHeld",
                new EventData().append("Action", "addHeld"), false);
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#ClearFilters",
                new EventData().append("Action", "clearFilters"), false);
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#ToggleMode",
                new EventData().append("Action", "toggleMode"), false);
        }

        private void buildFilterGrid(UICommandBuilder cmd, UIEventBuilder evt, List<String> filters) {
            cmd.clear("#FilterGrid");

            int rowIndex = 0;
            int cardsInCurrentRow = 0;

            for (String itemId : filters) {
                if (cardsInCurrentRow == 0) {
                    cmd.appendInline("#FilterGrid", "Group { LayoutMode: Left; Anchor: (Bottom: 0); }");
                }

                cmd.append("#FilterGrid[" + rowIndex + "]", ModConfig.uiPath("FilterItemCard.ui"));

                String cardPath = "#FilterGrid[" + rowIndex + "][" + cardsInCurrentRow + "]";
                cmd.set(cardPath + " #ItemIcon.ItemId", itemId);

                // evt.addEventBinding(CustomUIEventBindingType.RightClicking, cardPath + " #ItemIcon",
                //     EventData.of("RemoveFilter", itemId), false);

                evt.addEventBinding(CustomUIEventBindingType.RightClicking, cardPath + " #IconButton",
                           EventData.of("RemoveFilter", itemId), false
                );

                cardsInCurrentRow++;
                if (cardsInCurrentRow >= ITEMS_PER_ROW) {
                    cardsInCurrentRow = 0;
                    rowIndex++;
                }
            }
        }

        @Override
        public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, UIEventData data) {
            provider.handleEvent(player.getUuid(), transfer, state, data, player, this.store);
        }
    }

    public static final class UIEventData {
        public static final BuilderCodec<UIEventData> CODEC = BuilderCodec.builder(UIEventData.class, UIEventData::new)
            .addField(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
            .addField(new KeyedCodec<>("RemoveFilter", Codec.STRING), (d, v) -> d.removeFilter = v, d -> d.removeFilter)
            .build();

        public String action;
        public String removeFilter;
    }
}
