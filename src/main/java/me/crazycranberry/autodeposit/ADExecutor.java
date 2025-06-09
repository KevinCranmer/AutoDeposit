package me.crazycranberry.autodeposit;

import me.crazycranberry.autodeposit.config.Grouping;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static me.crazycranberry.autodeposit.AutoDeposit.getPlugin;
import static me.crazycranberry.autodeposit.AutoDeposit.logger;

public final class ADExecutor {
    private static final Set<Integer> ARMOR_INDICES = Set.of(36, 37, 38, 39, 40);
    private static final Set<Integer> HOTBAR_INDICES = Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8);
    private final Player p;
    private final Integer radius;
    private final List<Grouping> groupings;
    private final Map<Material, List<InventorySlot>> inventory;
    private final Block adBlock;

    private ADExecutor(Player p, Integer radius, Block adBlock, boolean includeArmor, boolean includeHotbar) {
        this.p = p;
        this.radius = radius == null ? null : radius > 50 || radius < 0 ? 15 : radius;
        this.inventory = inventoryMap(p, includeArmor, includeHotbar);
        this.groupings = getPlugin().config().groupings();
        this.adBlock = adBlock;
    }

    public static ADExecutor of(Player p, Integer radius, boolean includeArmor, boolean includeHotbar) {
        return new ADExecutor(p, radius, null, includeArmor, includeHotbar);
    }

    public static ADExecutor of(Player p, Block adBlock, boolean includeArmor, boolean includeHotbar) {
        return new ADExecutor(p, null, adBlock, includeArmor, includeHotbar);
    }

    // Scan all blocks within nearby radius looking for chests/shulker boxes.
    public void executeWithScan() {
        if (p == null || radius == null) {
            logger().severe("Cannot auto deposit via scan without the player AND the radius.");
            return;
        }
        boolean atLeastOneChestFound = false;
        for (int x = radius * -1; x <= radius; x++) {
            for (int y = radius * -1; y <= radius; y++) {
                for (int z =  radius * -1; z <= radius; z++) {
                    Block block = p.getLocation().getBlock().getRelative(x, y, z);
                    if (block.getState() instanceof Container container) {
                        atLeastOneChestFound = true;
                        depositMatchingItems(container, p);
                    }
                }
            }
        }
        if (!atLeastOneChestFound) {
            p.sendMessage("Could not find any nearby chests to deposit into.");
        }
    }

    // Use the AD Block's memory to grab the chests it already knows are in range (no scan).
    public void executeFromMemory() {
        if (p == null || adBlock == null) {
            logger().severe("Cannot auto deposit from memory without the player AND the AD Block.");
            return;
        }
        TileState state = (TileState) adBlock.getState();
        PersistentDataContainer pdc = state.getPersistentDataContainer();
        String chestList = pdc.get(getPlugin().CHEST_LIST_KEY, PersistentDataType.STRING);
        if (chestList == null) {
            logger().severe("Could not load the \"chestList\" from " + adBlock.getLocation());
            return;
        }
        boolean atLeastOneChestFound = false;
        String[] chests = chestList.split(";");
        for (int i = 1; i < chests.length; i++) {
            String[] chestCoords = chests[i].split(",");
            if (chestCoords.length != 3) {
                logger().severe("Expected x,y,z. Instead got " + chests[i]);
                continue;
            }
            Block chest;
            try {
                chest = adBlock.getWorld().getBlockAt(Integer.parseInt(chestCoords[0]), Integer.parseInt(chestCoords[1]), Integer.parseInt(chestCoords[2]));
            } catch (NumberFormatException ex) {
                logger().severe("Could not parse an integer in " + chests[i]);
                continue;
            }
            if (chest.getState() instanceof Container container) {
                atLeastOneChestFound = true;
                depositMatchingItems(container, p);
            }
        }
        if (!atLeastOneChestFound) {
            p.sendMessage("Could not find any nearby chests to deposit into.");
        } else {
            makeEffect(adBlock.getLocation());
        }
    }

    private void makeEffect(Location location) {
        location.getWorld().spawnParticle(Particle.WITCH, location.clone(), 25, 0.8, 0.8, 0.8);
        location.getWorld().playSound(location, Sound.BLOCK_CHISELED_BOOKSHELF_PICKUP_ENCHANTED, 1, 1);
    }

    private Map<Material, List<InventorySlot>> inventoryMap(Player p, boolean includeArmor, boolean includeHotbar) {
        Map<Material, List<InventorySlot>> inv = new HashMap<>();
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] == null) {
                continue;
            }
            if (!includeArmor && ARMOR_INDICES.contains(i)) {
                continue;
            }
            if (!includeHotbar && HOTBAR_INDICES.contains(i)) {
                continue;
            }
            List<InventorySlot> materialInventorySlotList = inv.get(contents[i].getType());
            if (materialInventorySlotList == null) {
                materialInventorySlotList = new ArrayList<>();
            }
            materialInventorySlotList.add(InventorySlot.of(i, contents[i]));
            inv.put(contents[i].getType(), materialInventorySlotList);
        }
        return inv;
    }

    private void depositMatchingItems(Container chest, Player p) {
        Set<Material> chestMaterials = Arrays.stream(chest.getInventory().getContents())
            .filter(Objects::nonNull)
            .map(ItemStack::getType)
            .collect(Collectors.toSet());
        List<Material> emptyMaterials = new ArrayList<>();
        for (Map.Entry<Material, List<InventorySlot>> e : inventory.entrySet()) {
            if (itemBelongsInChest(e.getKey(), chestMaterials)) {
                List<InventorySlot> afterDepositingMaterialInventorySlotList = new ArrayList<>();
                for (InventorySlot is : e.getValue()) {
                    ItemStack remainingItems = chest.getInventory().addItem(is.item()).get(0);
                    p.getInventory().setItem(is.index(), remainingItems);
                    if (remainingItems != null && remainingItems.getAmount() > 0) {
                        afterDepositingMaterialInventorySlotList.add(InventorySlot.of(is.index(), remainingItems));
                    }
                }
                e.getValue().clear();
                e.getValue().addAll(afterDepositingMaterialInventorySlotList);
            }
            if (e.getValue().isEmpty()) {
                emptyMaterials.add(e.getKey());
            }
        }
        emptyMaterials.forEach(inventory::remove);
    }

    private boolean itemBelongsInChest(Material item, Set<Material> chest) {
        if (chest.contains(item)) {
            return true;
        }
        for (Grouping grouping : groupings) {
            if (grouping.items().contains(item) && chest.stream().anyMatch(chestMat -> grouping.items().contains(chestMat))) {
                return true;
            }
        }
        return false;
    }
}
