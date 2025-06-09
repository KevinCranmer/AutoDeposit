package me.crazycranberry.autodeposit.commands;

import me.crazycranberry.autodeposit.events.AutoDepositFromMemoryRequestedEvent;
import me.crazycranberry.autodeposit.events.AutoDepositViaScanRequestedEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ADCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            int radius = getRadius(player, args);
            Bukkit.getPluginManager().callEvent(new AutoDepositViaScanRequestedEvent(player, radius));
            return true;
        }
        return false;
    }

    private int getRadius(Player player, String[] args) {
        if (args.length < 1) {
            return 15;
        }
        try {
            return Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            player.sendMessage(String.format("Could not parse the radius: \"%s\", using 15 instead.", args[0]));
            return 15;
        }
    }
}
