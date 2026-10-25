
package com.matej.publicenderchest;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class PublicChestData extends SavedData {
    private static final int SIZE = 54;

    private static final String PLAYER_PREFIX =
            "[Public Ender Chest] Deposited by: ";
    private static final String TIME_PREFIX =
            "[Public Ender Chest] Deposited at: ";

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
                    .withZone(ZoneId.systemDefault());

    private final SimpleContainer inventory = new SimpleContainer(SIZE);

    private static final Codec<PublicChestData> CODEC =
            ItemStack.OPTIONAL_CODEC.listOf().xmap(
                    PublicChestData::new,
                    data -> data.inventory.getItems()
            );

    private static final SavedDataType<PublicChestData> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath(
                            PublicEnderChest.MOD_ID,
                            "inventory"
                    ),
                    PublicChestData::new,
                    CODEC,
                    null
            );

    public PublicChestData() {
    }

    private PublicChestData(List<ItemStack> items) {
        for (int i = 0; i < Math.min(items.size(), SIZE); i++) {
            inventory.setItem(i, items.get(i));
        }
    }

    public static PublicChestData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(TYPE);
    }

    public Container inventory() {
        return new DirtyTrackingContainer(inventory, this);
    }

    public List<ItemStack> snapshot() {
        List<ItemStack> result = new ArrayList<>(SIZE);

        for (int i = 0; i < SIZE; i++) {
            result.add(inventory.getItem(i).copy());
        }

        return result;
    }


    public void recordChanges(
            ServerPlayer player,
            List<ItemStack> before
    ) {
        boolean changed = false;

        for (int slot = 0; slot < SIZE; slot++) {
            ItemStack oldStack = before.get(slot);
            ItemStack newStack = inventory.getItem(slot);

            boolean oldEmpty = oldStack.isEmpty();
            boolean newEmpty = newStack.isEmpty();

            boolean sameItem =
                    !oldEmpty && !newEmpty
                            && ItemStack.isSameItemSameComponents(oldStack, newStack);

            int oldCount = oldEmpty ? 0 : oldStack.getCount();
            int newCount = newEmpty ? 0 : newStack.getCount();

            if (oldEmpty && newEmpty) {
                continue;
            }

            if (!sameItem || oldCount != newCount) {
                changed = true;
            }

            if ((!sameItem && !newEmpty) ||
                    (sameItem && newCount > oldCount)) {
                addAuditLore(newStack, player);
                setDirty();
            }
        }

        if (changed) {
            writeAudit(player);
        }
    }

    private void addAuditLore(ItemStack stack, ServerPlayer player) {
        List<Component> lines = getLoreLines(stack);
        lines.removeIf(this::isAuditLine);

        String time = TIME_FORMAT.format(Instant.now());

        lines.add(Component.literal(
                player.getGameProfile().name() + ", " + time
        ).withStyle(style -> style
                .withColor(0x55FFFF)
                .withItalic(false)));

        stack.set(DataComponents.LORE, new ItemLore(lines));
    }

    public void removeAuditLoreFromPlayer(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            removeAuditLore(player.getInventory().getItem(i));
        }

        removeAuditLore(player.containerMenu.getCarried());
    }

    private void removeAuditLore(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) {
            return;
        }

        List<Component> lines = new ArrayList<>(lore.lines());
        boolean changed = lines.removeIf(this::isAuditLine);

        if (changed) {
            if (lines.isEmpty()) {
                stack.remove(DataComponents.LORE);
            } else {
                stack.set(DataComponents.LORE, new ItemLore(lines));
            }
        }
    }

    private List<Component> getLoreLines(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);

        if (lore == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(lore.lines());
    }

    private boolean isAuditLine(Component line) {
        String text = line.getString();

        return text.matches(".*\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.*");
    }

    private void writeAudit(ServerPlayer player) {
        try {
            Path path = player.level().getServer()
                    .getWorldPath(LevelResource.ROOT)
                    .resolve("publicenderchest-audit.csv");

            boolean needsHeader = !Files.exists(path) || Files.size(path) == 0;

            try (var writer = Files.newBufferedWriter(
                    path,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND)) {

                if (needsHeader) {
                    writer.write("Player Name,Time");
                    writer.newLine();
                }

                writer.write("\"" + player.getName().getString().replace("\"", "\"\"")
                        + "\"," + java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                writer.newLine();
            }
        } catch (IOException e) {
            PublicEnderChest.LOGGER.error("Failed to write audit log", e);
        }
    }


    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static final class DirtyTrackingContainer
            implements Container {

        private final SimpleContainer delegate;
        private final PublicChestData owner;

        private DirtyTrackingContainer(
                SimpleContainer delegate,
                PublicChestData owner
        ) {
            this.delegate = delegate;
            this.owner = owner;
        }

        @Override
        public int getContainerSize() {
            return delegate.getContainerSize();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return delegate.getItem(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack result = delegate.removeItem(slot, amount);
            owner.setDirty();
            return result;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack result = delegate.removeItemNoUpdate(slot);
            owner.setDirty();
            return result;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            delegate.setItem(slot, stack);
            owner.setDirty();
        }

        @Override
        public void setChanged() {
            delegate.setChanged();
            owner.setDirty();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            delegate.clearContent();
            owner.setDirty();
        }
    }
}
