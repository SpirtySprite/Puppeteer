package com.kirugoldzzzz.puppeteer;

import com.kirugoldzzzz.puppeteer.common.scheduler.Scheduling;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public final class NpcListener implements Listener {

    private final NpcService service;

    public NpcListener(NpcService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        if (service.pendingCount() > 0) {
            Scheduling.async(() -> service.onWorldLoad(event.getWorld()));
        }
    }
}
