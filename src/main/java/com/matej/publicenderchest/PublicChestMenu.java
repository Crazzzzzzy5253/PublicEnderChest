
package com.matej.publicenderchest;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class PublicChestMenu extends ChestMenu {
    private final PublicChestData data;

    public PublicChestMenu(
            int syncId,
            Inventory playerInventory,
            PublicChestData data
    ) {
        super(
                net.minecraft.world.inventory.MenuType.GENERIC_9x6,
                syncId,
                playerInventory,
                data.inventory(),
                6
        );
        this.data = data;
    }

    @Override
    public void clicked(
            int slotId,
            int button,
            ContainerInput containerInput,
            Player player
    ) {
        List<ItemStack> before = data.snapshot();
        super.clicked(slotId, button, containerInput, player);

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            data.recordChanges(serverPlayer, before);
            data.removeAuditLoreFromPlayer(serverPlayer);
        }
    }
}
