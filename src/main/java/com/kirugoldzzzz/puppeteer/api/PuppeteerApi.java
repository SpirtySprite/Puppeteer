package com.kirugoldzzzz.puppeteer.api;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface PuppeteerApi {

    static Optional<PuppeteerApi> get() {
        return Optional.ofNullable(Bukkit.getServicesManager().load(PuppeteerApi.class));
    }

    List<String> npcs();

    boolean exists(String id);

    NpcState state(String id);

    Optional<Location> location(String id);

    long clicks(String id);

    CompletableFuture<Void> move(String id, Location location);

    CompletableFuture<Void> setEnabled(String id, boolean enabled);

    CompletableFuture<Void> reload();

    enum NpcState {
        ACTIVE,
        PENDING,
        DISABLED,
        UNKNOWN
    }
}
