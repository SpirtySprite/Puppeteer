package com.kirugoldzzzz.puppeteer.api.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public final class NpcClickEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String npc;
    private final boolean rightClick;
    private final boolean sneaking;
    private boolean cancelled;

    public NpcClickEvent(Player player, String npc, boolean rightClick, boolean sneaking) {
        super(player, !Bukkit.isPrimaryThread());
        this.npc = npc;
        this.rightClick = rightClick;
        this.sneaking = sneaking;
    }

    public String npc() {
        return npc;
    }

    public boolean rightClick() {
        return rightClick;
    }

    public boolean sneaking() {
        return sneaking;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
