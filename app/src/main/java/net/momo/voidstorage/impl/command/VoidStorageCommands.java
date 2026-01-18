package net.momo.voidstorage.impl.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import net.momo.silo.core.ModConfig;
import net.momo.silo.core.Services;
import net.momo.silo.storage.StorageRegistry;
import net.momo.silo.storage.Storage;
import net.momo.voidstorage.internal.anchor.AnchorRegistry;
import net.momo.voidstorage.internal.anchor.StorageAnchor;
import net.momo.voidstorage.internal.transfer.TransferRegistry;

import java.util.Optional;

/** Command implementations for Void Storage. */
public final class VoidStorageCommands {

    public CommandBase createHelpCommand() {
        return new CommandBase(ModConfig.NAMESPACE, ModConfig.NAMESPACE + ".command.help") {
            @Override
            protected void executeSync(CommandContext ctx) {
                ctx.sendMessage(Message.raw(ModConfig.DISPLAY_NAME + " Commands:"));
                ctx.sendMessage(Message.raw("  /" + ModConfig.NAMESPACE + " - Show this help"));
                ctx.sendMessage(Message.raw("  /vsstatus - Show system status"));
            }
        };
    }

    public CommandBase createStatusCommand() {
        return new CommandBase("vsstatus", ModConfig.NAMESPACE + ".command.status") {
            @Override
            protected void executeSync(CommandContext ctx) {
                AnchorRegistry anchorRegistry = Services.get(AnchorRegistry.class);
                TransferRegistry transferRegistry = Services.get(TransferRegistry.class);
                StorageRegistry storageRegistry = Services.get(StorageRegistry.class);

                int anchorCount = anchorRegistry.size();
                int transferCount = transferRegistry.size();

                long totalItems = 0;
                for (StorageAnchor anchor : anchorRegistry.getAll()) {
                    Optional<Storage> storageOpt = storageRegistry.get(anchor.id());
                    if (storageOpt.isPresent()) {
                        totalItems += storageOpt.get().getTotalItems();
                    }
                }

                ctx.sendMessage(Message.raw("=== " + ModConfig.DISPLAY_NAME + " Status ==="));
                ctx.sendMessage(Message.raw(String.format("%s: %d", ModConfig.ANCHOR_NAME_PLURAL, anchorCount)));
                ctx.sendMessage(Message.raw(String.format("%s: %d", ModConfig.TRANSFER_NAME_PLURAL, transferCount)));
                ctx.sendMessage(Message.raw(String.format("Total Items: %d", totalItems)));
            }
        };
    }
}
