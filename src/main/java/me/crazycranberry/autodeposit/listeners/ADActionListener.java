package me.crazycranberry.autodeposit.listeners;

import me.crazycranberry.autodeposit.ADExecutor;
import me.crazycranberry.autodeposit.config.AutoDepositConfig;
import me.crazycranberry.autodeposit.events.AutoDepositFromMemoryRequestedEvent;
import me.crazycranberry.autodeposit.events.AutoDepositViaScanRequestedEvent;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

import static me.crazycranberry.autodeposit.AutoDeposit.getPlugin;

public class ADActionListener implements Listener {
    @EventHandler
    public void onInteraction(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getClickedBlock() == null || Objects.equals(event.getHand(), EquipmentSlot.OFF_HAND)) {
            return;
        }
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock.getState() instanceof TileState state) {
            PersistentDataContainer pdc = state.getPersistentDataContainer();
            if (pdc.get(getPlugin().CHEST_LIST_KEY, PersistentDataType.STRING) != null) {
                Bukkit.getPluginManager().callEvent(new AutoDepositFromMemoryRequestedEvent(event.getPlayer(), clickedBlock));
            }
        }
    }

    @EventHandler
    public void onAutoDepositFromMemoryRequested(AutoDepositFromMemoryRequestedEvent event) {
        AutoDepositConfig config = getPlugin().config();
        ADExecutor.of(event.getPlayer(), event.adBlock(), config.autoDepositArmor(), config.autoDepositHotbar()).executeFromMemory();
    }

    @EventHandler
    public void onAutoDepositViaScanRequested(AutoDepositViaScanRequestedEvent event) {
        AutoDepositConfig config = getPlugin().config();
        ADExecutor.of(event.getPlayer(), event.range(), config.autoDepositArmor(), config.autoDepositHotbar()).executeWithScan();
    }
}
