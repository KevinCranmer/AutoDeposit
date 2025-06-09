package me.crazycranberry.autodeposit.config;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static me.crazycranberry.autodeposit.AutoDeposit.getPlugin;
import static me.crazycranberry.autodeposit.AutoDeposit.logger;
import static me.crazycranberry.autodeposit.config.FileUtils.loadOriginalConfig;

public class AutoDepositConfig {
    public static final String AUTO_DEPOSITORS_YML = "auto_depositors.yml";
    private final YamlConfiguration originalConfig;
    private String autoDepositorBlockName;
    private String autoDepositorBlockLore;
    private final List<Grouping> groupings;
    private final List<NamespacedKey> recipes;
    private final YamlConfiguration adBlocksConfig;
    private boolean autoDepositArmor;
    private boolean autoDepositHotbar;

    public AutoDepositConfig(YamlConfiguration config, YamlConfiguration adBlocksConfig) {
        originalConfig = loadOriginalConfig("auto_deposit.yml");
        groupings = new ArrayList<>();
        recipes = new ArrayList<>();
        this.adBlocksConfig = adBlocksConfig;
        updateOutOfDateConfig(config);
        loadConfig(config, originalConfig);
    }

    public void reload(YamlConfiguration config) {
        loadConfig(config, originalConfig);
    }

    private void loadConfig(YamlConfiguration config, YamlConfiguration originalConfig) {
        this.autoDepositorBlockName = config.getString("auto_depositing_block_name", originalConfig.getString("auto_depositing_block_name"));
        this.autoDepositorBlockLore = config.getString("auto_depositing_block_lore", originalConfig.getString("auto_depositing_block_lore"));
        this.autoDepositHotbar = config.getBoolean("auto_deposit_hotbar", originalConfig.getBoolean("auto_deposit_hotbar"));
        this.autoDepositArmor = config.getBoolean("auto_deposit_armor", originalConfig.getBoolean("auto_deposit_armor"));
        loadGroupings(config.getConfigurationSection("groupings"));
        loadRecipes(config.getConfigurationSection("auto_depositing_blocks"));
    }

    private void loadRecipes(ConfigurationSection autoDepositingBlocks) {
        if (autoDepositingBlocks == null) {
            logger().severe("The \"auto_depositing_blocks\" config is missing!!");
            return;
        }
        recipes.forEach(Bukkit::removeRecipe);
        recipes.clear();
        for (String recipeConfig : autoDepositingBlocks.getKeys(false)) {
            String skullTexture = autoDepositingBlocks.getString(recipeConfig + ".skull_texture");
            String range = recipeConfig.substring(recipeConfig.indexOf("_") + 1);
            ADRecipe adRecipe = ADRecipe.of(range, autoDepositingBlocks.getStringList(recipeConfig + ".recipe"), skullTexture, blockName(range), blockLore(range));
            Bukkit.addRecipe(adRecipe.recipe());
            recipes.add(adRecipe.key());
        }
    }

    private void loadGroupings(ConfigurationSection groupingsConfigSection) {
        if (groupingsConfigSection == null) {
            logger().severe("The \"groupings\" config is missing!!");
            return;
        }
        groupings.clear();
        for (String grouping : groupingsConfigSection.getKeys(false)) {
            List<String> itemList = groupingsConfigSection.getStringList(grouping);
            groupings.add(Grouping.of(grouping, new HashSet<>(itemList)));
        }
    }

    public List<Block> adBlocksFromConfig() {
        List<Block> mutableList = new ArrayList<>();
        adBlocksConfig.getStringList("auto_depositors")
            .stream()
            .map(AutoDepositConfig::toBlock)
            .filter(Objects::nonNull)
            .forEach(mutableList::add);
        return mutableList;
    }

    // blockString format: '<world>,<x>,<y>,<z>'
    private static Block toBlock(String blockString) {
        String[] pieces = blockString.split(",");
        if (pieces.length != 4) {
            return null;
        }
        try {
            World world = Bukkit.getWorld(pieces[0]);
            double x = Double.parseDouble(pieces[1]);
            double y = Double.parseDouble(pieces[2]);
            double z = Double.parseDouble(pieces[3]);
            return new Location(world, x, y, z).getBlock();
        } catch (NumberFormatException ex) {
            logger().severe("Could not parse the auto_depositors.yml location: " + blockString);
            return null;
        }
    }

    private void updateOutOfDateConfig(YamlConfiguration config) {
        boolean madeAChange = false;
        for (String key : originalConfig.getKeys(true)) {
            if (!config.isString(key) && !config.isConfigurationSection(key) && !config.isBoolean(key) && !config.isDouble(key) && !config.isInt(key) && !config.isList(key)) {
                logger().info("The " + key + " is missing from auto_deposit.yml, adding it now.");
                config.set(key, originalConfig.get(key));
                madeAChange = true;
            }
        }

        if (madeAChange) {
            try {
                config.save(getPlugin().getDataFolder() + "" + File.separatorChar + "auto_deposit.yml");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public List<Grouping> groupings() {
        return groupings;
    }

    public String blockName(String range) {
        return String.format("%s%s%s", ChatColor.WHITE, autoDepositorBlockName.replace("{RANGE}", range), ChatColor.RESET);
    }

    public String blockLore(String range) {
        return String.format("%s%s%s", ChatColor.GRAY, autoDepositorBlockLore.replace("{RANGE}", range), ChatColor.RESET);
    }

    public boolean autoDepositArmor() {
        return autoDepositArmor;
    }

    public boolean autoDepositHotbar() {
        return autoDepositHotbar;
    }

    public void saveAdBlocks(List<Block> adLocations) {
        List<String> adLocationStrings = adLocations.stream()
            .map(l -> String.format("%s,%s,%s,%s", l.getWorld().getName(), l.getX(), l.getY(), l.getZ()))
            .toList();
        adBlocksConfig.set("auto_depositors", adLocationStrings);
        try {
            adBlocksConfig.save(getPlugin().getDataFolder() + "" + File.separatorChar + AUTO_DEPOSITORS_YML);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
