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
import net.momo.silo.storage.StoredItem;
import net.momo.silo.storage.Storage;
import net.momo.silo.ui.UIPageProvider;
import net.momo.silo.util.NumberFormat;
import net.momo.platform.hytale.impl.HytaleInventoryAdapter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** UI provider for the storage page with grid-based item display. */
public final class StoragePageProvider implements UIPageProvider {

    private static final int ITEMS_PER_ROW = 8;
    private final Map<UUID, State> states = new ConcurrentHashMap<>();

    @Override
    public String pageType() {
        return "storage";
    }

    @Override
    public void open(UUID playerId, Map<String, Object> params) {
        Storage storage = (Storage) params.get("storage");
        if (storage == null) {
            return;
        }

        resolvePlayer(playerId, (player, store) -> {
            State state = states.computeIfAbsent(playerId, id -> new State());
            player.getPageManager().openCustomPage(
                player.getReference(), store,
                new StoragePage(player, store, storage, state, this)
            );
        });
    }

    private void handleEvent(UUID playerId, Storage storage, State state, UIEventData data, Player player, Store<EntityStore> store) {
        // Handle search
        if (data.searchQuery != null) {
            state.search = data.searchQuery;
            player.getPageManager().openCustomPage(
                player.getReference(), store,
                new StoragePage(player, store, storage, state, this)
            );
            return;
        }

        // Handle get item
        if (data.getItem != null && !data.getItem.isEmpty()) {
            String itemId = data.getItem;
            var stored = storage.getItem(itemId);
            if (!stored.isEmpty()) {
                int toRetrieve = (int) Math.min(stored.get().quantity(), 64);
                storage.withdraw(itemId, toRetrieve);
                HytaleInventoryAdapter inventory = new HytaleInventoryAdapter(player.getInventory());
                inventory.giveItem(itemId, toRetrieve);
            }
            player.getPageManager().openCustomPage(
                player.getReference(), store,
                new StoragePage(player, store, storage, state, this)
            );
        }
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
        String search = "";
    }

    private static final class StoragePage extends InteractiveCustomUIPage<UIEventData> {
        private final Player player;
        private final Store<EntityStore> store;
        private final Storage storage;
        private final State state;
        private final StoragePageProvider provider;

        StoragePage(Player player, Store<EntityStore> store, Storage storage, State state, StoragePageProvider provider) {
            super(player.getPlayerRef(), CustomPageLifetime.CanDismiss, UIEventData.CODEC);
            this.player = player;
            this.store = store;
            this.storage = storage;
            this.state = state;
            this.provider = provider;
        }

        @Override
        public void build(Ref<EntityStore> ref, UICommandBuilder cmd, UIEventBuilder evt, Store<EntityStore> store) {
            cmd.append(ModConfig.uiPath("VoidStoragePage.ui"));

            // Set search value and bind search event
            cmd.set("#SearchInput.Value", state.search);
            evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput",
                EventData.of("@SearchQuery", "#SearchInput.Value"), false);

            // Get items (filtered if searching)
            List<StoredItem> items = state.search.isEmpty()
                ? storage.getItemsSorted()
                : storage.searchItems(state.search);

            // Build the item grid
            buildItemGrid(cmd, evt, items);
        }

        private void buildItemGrid(UICommandBuilder cmd, UIEventBuilder evt, List<StoredItem> items) {
            cmd.clear("#SubcommandCards");
            cmd.set("#SubcommandSection.Visible", true);

            int rowIndex = 0;
            int cardsInCurrentRow = 0;

            for (StoredItem item : items) {
                if (cardsInCurrentRow == 0) {
                    cmd.appendInline("#SubcommandCards", "Group { LayoutMode: Left; Anchor: (Bottom: 0); }");
                }

                cmd.append("#SubcommandCards[" + rowIndex + "]", ModConfig.uiPath("VoidStorageItemCard.ui"));

                String cardPath = "#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "]";
                cmd.set(cardPath + " #ItemIcon.ItemId", item.itemId());
                cmd.set(cardPath + " #ItemAmount.Text", NumberFormat.compact(item.quantity()));

                // Bind get button
                evt.addEventBinding(CustomUIEventBindingType.Activating, cardPath + " #GetButton",
                    EventData.of("GetItem", item.itemId()), false);

                cardsInCurrentRow++;
                if (cardsInCurrentRow >= ITEMS_PER_ROW) {
                    cardsInCurrentRow = 0;
                    rowIndex++;
                }
            }
        }

        @Override
        public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, UIEventData data) {
            provider.handleEvent(player.getUuid(), storage, state, data, player, this.store);
        }
    }

    public static final class UIEventData {
        static final String KEY_SEARCH_QUERY = "@SearchQuery";
        static final String KEY_GET_ITEM = "GetItem";

        public static final BuilderCodec<UIEventData> CODEC = BuilderCodec.builder(UIEventData.class, UIEventData::new)
            .addField(new KeyedCodec<>(KEY_SEARCH_QUERY, Codec.STRING), (d, v) -> d.searchQuery = v, d -> d.searchQuery)
            .addField(new KeyedCodec<>(KEY_GET_ITEM, Codec.STRING), (d, v) -> d.getItem = v, d -> d.getItem)
            .build();

        public String searchQuery;
        public String getItem;
    }
}
