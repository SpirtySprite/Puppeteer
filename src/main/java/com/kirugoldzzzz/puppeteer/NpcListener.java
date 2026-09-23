package com.kirugoldzzzz.puppeteer;

import com.kirugoldzzzz.puppeteer.api.event.NpcClickEvent;
import com.kirugoldzzzz.puppeteer.common.scheduler.Scheduling;
import net.folianpc.api.ClickType;
import net.folianpc.api.event.NpcInteractEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public final class NpcListener implements Listener {

    private final NpcService service;

    public NpcListener(NpcService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(NpcInteractEvent event) {
        String id = service.idOf(event.getNpc());
        if (id == null) {
            return;
        }
        NpcClickEvent click = new NpcClickEvent(event.getPlayer(), id, event.getClick() == ClickType.RIGHT,
                event.isSneaking());
        Bukkit.getPluginManager().callEvent(click);
        if (click.isCancelled()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        if (service.pendingCount() > 0) {
            Scheduling.async(() -> service.onWorldLoad(event.getWorld()));
        }
    }
}
