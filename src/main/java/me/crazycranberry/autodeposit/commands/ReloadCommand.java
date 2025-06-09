package me.crazycranberry.autodeposit.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import static me.crazycranberry.autodeposit.AutoDeposit.getPlugin;

public class ReloadCommand implements CommandExecutor
{
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		getPlugin().reloadConfigs();
		sender.sendMessage(ChatColor.GRAY + "Config reloaded!");
		return true;
	}
}