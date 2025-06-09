package me.crazycranberry.autodeposit;

import me.crazycranberry.autodeposit.commands.ADCommand;
import me.crazycranberry.autodeposit.commands.ReloadCommand;
import me.crazycranberry.autodeposit.config.AutoDepositConfig;
import me.crazycranberry.autodeposit.listeners.ADActionListener;
import me.crazycranberry.autodeposit.listeners.ADBlockListener;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Logger;

import static me.crazycranberry.autodeposit.config.AutoDepositConfig.AUTO_DEPOSITORS_YML;
import static me.crazycranberry.autodeposit.config.FileUtils.loadConfig;

public final class AutoDeposit extends JavaPlugin {
    private static AutoDeposit plugin;
    private static Logger logger;
    private static AutoDepositConfig config;
    public final NamespacedKey RANGE_KEY = new NamespacedKey(this, "range");
    public final NamespacedKey CHEST_LIST_KEY = new NamespacedKey(this, "chestList");
    private List<Block> adBlocks;

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        logger = this.getLogger();
        try {
            config = new AutoDepositConfig(loadConfig("auto_deposit.yml"), loadConfig(AUTO_DEPOSITORS_YML));
        } catch (InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
        registerCommands();
        registerListeners();
        adBlocks = config().adBlocksFromConfig();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        config().saveAdBlocks(adBlocks);
    }

    private void registerCommands() {
        // Register command
        this.getCommand("autodeposit").setExecutor(new ADCommand());
        this.getCommand("adreload").setExecutor(new ReloadCommand());
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ADBlockListener(), this);
        getServer().getPluginManager().registerEvents(new ADActionListener(), this);
    }

    public void addADBlock(Block ad) {
        adBlocks.add(ad);
    }

    public void removeAdBlock(Block ad) {
        adBlocks.remove(ad);
    }

    public void addChestToRespectiveADs(Block chest) {
        for (Block adBlock : adBlocks) {
            if (!adBlock.getWorld().equals(chest.getWorld())) {
                continue;
            }
            BlockState blockState = adBlock.getState();
            TileState skullState = (TileState) blockState;
            PersistentDataContainer skullPDC = skullState.getPersistentDataContainer();
            Integer range = skullPDC.get(getPlugin().RANGE_KEY, PersistentDataType.INTEGER);
            String chestList = skullPDC.get(getPlugin().CHEST_LIST_KEY, PersistentDataType.STRING);
            if (range == null || chestList == null) {
                logger().severe(String.format("ADBlock at %s does not have the range/chestList PDC value"));
                continue;
            }
            if (Math.abs(chest.getX() - adBlock.getX()) <= range &&
              Math.abs(chest.getY() - adBlock.getY()) <= range &&
              Math.abs(chest.getZ() - adBlock.getZ()) <= range) {
                chestList += chestString(chest);
                skullPDC.set(getPlugin().CHEST_LIST_KEY, PersistentDataType.STRING, chestList);
            }
            skullState.update();
        }
    }

    public void removeChestFromRespectiveADBlocks(Block chest) {
        for (Block adBlock : adBlocks) {
            if (!adBlock.getWorld().equals(chest.getWorld())) {
                continue;
            }
            BlockState blockState = adBlock.getState();
            TileState skullState = (TileState) blockState;
            PersistentDataContainer skullPDC = skullState.getPersistentDataContainer();
            String chestList = skullPDC.get(getPlugin().CHEST_LIST_KEY, PersistentDataType.STRING);
            if (chestList == null) {
                logger().severe(String.format("ADBlock at %s does not have the range/chestList PDC value"));
                continue;
            }
            String removedChestString = chestString(chest);
            if (chestList.contains(removedChestString)) {
                skullPDC.set(getPlugin().CHEST_LIST_KEY, PersistentDataType.STRING, chestList.replace(removedChestString, ""));
            }
            skullState.update();
        }
    }

    private String chestString(Block chest) {
        return String.format(";%s,%s,%s", chest.getX(), chest.getY(), chest.getZ());
    }

    public AutoDepositConfig config() {
        return config;
    }

    public String reloadConfigs() {
        try {
            config.reload(loadConfig("auto_deposit.yml"));
            return "Successfully loaded configs.";
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    public static Logger logger() {
        return logger;
    }

    public static AutoDeposit getPlugin() {
        return plugin;
    }
}
