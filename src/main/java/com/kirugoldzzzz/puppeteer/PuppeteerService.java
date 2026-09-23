package com.kirugoldzzzz.puppeteer;

import com.kirugoldzzzz.puppeteer.api.PuppeteerApi;
import com.kirugoldzzzz.puppeteer.common.scheduler.Scheduling;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

final class PuppeteerService implements PuppeteerApi {

    private final NpcService service;

    PuppeteerService(NpcService service) {
        this.service = service;
    }

    @Override
    public List<String> npcs() {
        return service.ids();
    }

    @Override
    public boolean exists(String id) {
        return service.ids().contains(id);
    }

    @Override
    public NpcState state(String id) {
        return NpcState.valueOf(service.status(id).name());
    }

    @Override
    public Optional<Location> location(String id) {
        return service.definition(id).flatMap(definition -> {
            World world = Bukkit.getWorld(definition.world());
            return world == null ? Optional.empty() : Optional.of(new Location(world, definition.x(), definition.y(),
                    definition.z(), definition.yaw(), definition.pitch()));
        });
    }

    @Override
    public long clicks(String id) {
        return service.clicks().getOrDefault(id, 0L);
    }

    @Override
    public CompletableFuture<Void> move(String id, Location location) {
        require(id);
        if (location == null || location.getWorld() == null) {
            throw new IllegalArgumentException("location must have a world");
        }
        String world = location.getWorld().getName();
        double[] values = {location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch()};
        return edit(root -> {
            ConfigurationSection target = root.createSection("npcs." + id + ".location");
            target.set("world", world);
            target.set("x", values[0]);
            target.set("y", values[1]);
            target.set("z", values[2]);
            target.set("yaw", values[3]);
            target.set("pitch", values[4]);
        });
    }

    @Override
    public CompletableFuture<Void> setEnabled(String id, boolean enabled) {
        require(id);
        return edit(root -> root.set("npcs." + id + ".enabled", enabled));
    }

    @Override
    public CompletableFuture<Void> reload() {
        return run(service::reload);
    }

    private CompletableFuture<Void> edit(Consumer<YamlConfiguration> change) {
        return run(() -> service.edit(change));
    }

    private static CompletableFuture<Void> run(Runnable task) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        Scheduling.async(() -> {
            try {
                task.run();
                done.complete(null);
            } catch (RuntimeException failure) {
                done.completeExceptionally(failure);
            }
        });
        return done;
    }

    private void require(String id) {
        if (!exists(id)) {
            throw new IllegalArgumentException("unknown npc: " + id);
        }
    }
}
