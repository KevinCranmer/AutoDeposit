package me.crazycranberry.autodeposit.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AutoDepositFromMemoryRequestedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Block adBlock;

    public AutoDepositFromMemoryRequestedEvent(Player player, Block adBlock) {
        this.player = player;
        this.adBlock = adBlock;
    }

    public Player getPlayer() {
        return player;
    }

    public Block adBlock() {
        return adBlock;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
