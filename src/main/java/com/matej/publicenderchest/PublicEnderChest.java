package com.matej.publicenderchest;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PublicEnderChest implements ModInitializer {
    public static final String MOD_ID = "publicenderchest";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Component TITLE = Component.literal("Public Ender Chest");

    @Override
    public void onInitialize() {
        UseBlockCallback.EVENT.register(PublicEnderChest::onUseBlock);
        UseItemCallback.EVENT.register(PublicEnderChest::onUseItem);

    }

    private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.getBlockState(hit.getBlockPos()).is(Blocks.ENDER_CHEST)) return InteractionResult.PASS;
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) open(serverPlayer);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult onUseItem(Player player, Level level, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!player.getItemInHand(hand).is(Items.ENDER_CHEST)) return InteractionResult.PASS;
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) open(serverPlayer);
        // Consume the interaction so the held Ender Chest is not placed.
        return InteractionResult.SUCCESS;
    }

    public static void open(ServerPlayer player) {
        PublicChestData data = PublicChestData.get((ServerLevel) player.level());

        player.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, ignoredPlayer) ->
                        new PublicChestMenu(syncId, playerInventory, data),
                TITLE
        ));
    }
}
