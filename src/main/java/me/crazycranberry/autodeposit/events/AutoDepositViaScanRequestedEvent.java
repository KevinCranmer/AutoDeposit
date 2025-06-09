package me.crazycranberry.autodeposit.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AutoDepositViaScanRequestedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Integer range;

    public AutoDepositViaScanRequestedEvent(Player player, Integer range) {
        this.player = player;
        this.range = range;
    }

    public Player getPlayer() {
        return player;
    }

    public Integer range() {
        return range;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
