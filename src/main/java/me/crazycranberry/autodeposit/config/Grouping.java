package me.crazycranberry.autodeposit.config;

import me.crazycranberry.autodeposit.InventorySlot;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static me.crazycranberry.autodeposit.AutoDeposit.logger;

public class Grouping {
    private final String name;
    private final Set<Material> items;

    private Grouping(String name, Set<String> items) {
        this.name = name;
        this.items = items.stream()
            .map(m -> {
                try {
                    return Material.valueOf(m);
                } catch (IllegalArgumentException ex) {
                    logger().warning(String.format("Unknown material \"%s\". Excluding.", m));
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    public static Grouping of(String name, Set<String> items) {
        return new Grouping(name, items);
    }

    public String name() {
        return name;
    }

    public Set<Material> items() {
        return items;
    }
}
