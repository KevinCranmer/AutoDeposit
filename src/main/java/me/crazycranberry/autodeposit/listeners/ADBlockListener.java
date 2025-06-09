package me.crazycranberry.autodeposit.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import java.util.List;

import static me.crazycranberry.autodeposit.AutoDeposit.getPlugin;
import static me.crazycranberry.autodeposit.AutoDeposit.logger;

public class ADBlockListener implements Listener {

    @EventHandler
    public void onBlockDropItemEvent(BlockDropItemEvent event) {
        BlockState blockState = event.getBlockState();
        if (blockState.getType() != Material.PLAYER_HEAD && blockState.getType() != Material.PLAYER_WALL_HEAD) {
            return;
        }
        TileState skullState = (TileState) blockState;
        PersistentDataContainer skullPDC = skullState.getPersistentDataContainer();
        Integer range = skullPDC.get(getPlugin().RANGE_KEY, PersistentDataType.INTEGER);
        if (range == null) {
            return;
        }
        // This should only ever be the one item but loop through anyway I guess
        for (Item item: event.getItems()) {
            ItemStack itemstack = item.getItemStack();
            if (itemstack.getType() != Material.PLAYER_HEAD) {
                continue;
            }
            ItemMeta meta = itemstack.getItemMeta();
            meta.setDisplayName(getPlugin().config().blockName(range.toString()));
            meta.getPersistentDataContainer().set(getPlugin().RANGE_KEY, PersistentDataType.INTEGER, range);
            meta.setLore(List.of(getPlugin().config().blockLore(range.toString())));
            itemstack.setItemMeta(meta);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getState() instanceof Container) {
            getPlugin().removeChestFromRespectiveADBlocks(block);
        } else if (block.getType().equals(Material.PLAYER_HEAD) || block.getType().equals(Material.PLAYER_WALL_HEAD)) {
            getPlugin().removeAdBlock(block);
        }
    }

    @EventHandler
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        Block placedBlock = event.getBlockPlaced();
        if (placedBlock.getType().equals(Material.PLAYER_HEAD) || placedBlock.getType().equals(Material.PLAYER_WALL_HEAD)) {
            handleSkullPlacement(event);
        } else if (placedBlock.getState() instanceof Container) {
            handleChestPlacement(placedBlock);
        }
    }

    private void handleChestPlacement(Block chest) {
        getPlugin().addChestToRespectiveADs(chest);
    }

    private void handleSkullPlacement(BlockPlaceEvent event) {
        ItemStack headItem = event.getItemInHand();
        ItemMeta meta = headItem.getItemMeta();
        if (headItem.getType() != Material.PLAYER_HEAD || meta == null) {
            return;
        }
        Integer range = meta.getPersistentDataContainer().get(getPlugin().RANGE_KEY, PersistentDataType.INTEGER);
        if (range == null) {
            logger().severe(event.getBlockPlaced().getLocation() + " did not have a range pdc value");
            return;
        }
        Block block = event.getBlockPlaced();
        TileState skullState = (TileState) block.getState();
        PersistentDataContainer skullPDC = skullState.getPersistentDataContainer();
        skullPDC.set(getPlugin().RANGE_KEY, PersistentDataType.INTEGER, range);
        skullPDC.set(getPlugin().CHEST_LIST_KEY, PersistentDataType.STRING, scanForChestListString(block, range));
        skullState.update();
        getPlugin().addADBlock(block);
    }

    private String scanForChestListString(Block adBlock, Integer range) {
        StringBuilder chestListString = new StringBuilder();
        for (int x = range * -1; x <= range; x++) {
            for (int y = range * -1; y <= range; y++) {
                for (int z =  range * -1; z <= range; z++) {
                    Block block = adBlock.getRelative(x, y, z);
                    if (block.getState() instanceof Container) {
                        chestListString.append(String.format(";%s,%s,%s", block.getX(), block.getY(), block.getZ()));
                    }
                }
            }
        }
        return chestListString.toString();
    }
}
