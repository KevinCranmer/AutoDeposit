package me.crazycranberry.autodeposit.config;

import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.crazycranberry.autodeposit.AutoDeposit.getPlugin;
import static me.crazycranberry.autodeposit.AutoDeposit.logger;

public class ADRecipe {
    private final NamespacedKey key;
    private final Recipe recipe;

    private ADRecipe(String range, List<String> materialStrings, String skullTexture, String blockName, String blockLore) {
        this.key = new NamespacedKey(getPlugin(), range);
        List<Material> materials = materialStrings.stream()
            .map(m -> {
                try {
                    return Material.valueOf(m);
                } catch (IllegalArgumentException ex) {
                    logger().warning(String.format("Unknown material \"%s\". Excluding.", m));
                    return null;
                }
            }).toList();
        this.recipe = createAutoDepositBlockRecipe(materials, skullTexture, range, blockName, blockLore);
    }

    public static ADRecipe of(String range, List<String> materialStrings, String skullTexture, String blockName, String blockLore) {
        return new ADRecipe(range, materialStrings, skullTexture, blockName, blockLore);
    }

    private org.bukkit.inventory.Recipe createAutoDepositBlockRecipe(List<Material> recipeMaterials, String skullTexture, String range, String blockName, String blockLore) {
        ItemStack block = createAutoDepositSkull(skullTexture, range, blockName, blockLore);
        ShapedRecipe recipe = new ShapedRecipe(this.key, block);
        Map<Material, Character> recipeMap = new HashMap<>();
        char currChar = 'A';
        String topRow = "", middleRow = "", bottomRow = "";
        for (int i = 0; i < recipeMaterials.size(); i++) {
            Material recipeMaterial = recipeMaterials.get(i);
            Character c = recipeMap.get(recipeMaterial);
            if (c == null) {
                recipeMap.put(recipeMaterial, currChar);
                currChar++;
            }
            if (i < 3) {
                topRow += recipeMap.get(recipeMaterial);
            } else if (i < 6) {
                middleRow += recipeMap.get(recipeMaterial);
            } else {
                bottomRow += recipeMap.get(recipeMaterial);
            }
        }
        recipe.shape(topRow, middleRow, bottomRow);
        recipeMap.forEach((key1, value) -> recipe.setIngredient(value, key1));
        return recipe;
    }

    private ItemStack createAutoDepositSkull(String skullTexture, String range, String blockName, String blockLore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        PlayerProfile profile = Bukkit.createPlayerProfile("Skull");
        PlayerTextures textures = profile.getTextures();
        String jsonTexture = new String(Base64.getDecoder().decode(skullTexture), StandardCharsets.UTF_8);
        String url = JsonParser.parseString(jsonTexture).getAsJsonObject().getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
        try {
            textures.setSkin(new URL(url));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        profile.setTextures(textures);
        meta.setOwnerProfile(profile);
        meta.setDisplayName(blockName);
        meta.setLore(List.of(blockLore));
        meta.getPersistentDataContainer().set(getPlugin().RANGE_KEY, PersistentDataType.INTEGER, Integer.parseInt(range));
        item.setItemMeta(meta);
        return item;
    }

    public NamespacedKey key() {
        return key;
    }

    public Recipe recipe() {
        return recipe;
    }
}
