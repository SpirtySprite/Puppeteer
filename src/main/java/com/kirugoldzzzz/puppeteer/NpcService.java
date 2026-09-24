package com.kirugoldzzzz.puppeteer;

import com.kirugoldzzzz.puppeteer.common.config.ConfigFile;
import com.kirugoldzzzz.puppeteer.common.item.ItemSpec;
import com.kirugoldzzzz.puppeteer.common.log.LogTopic;
import com.kirugoldzzzz.puppeteer.common.log.PluginLog;
import com.kirugoldzzzz.puppeteer.common.scheduler.Scheduling;
import com.kirugoldzzzz.puppeteer.common.text.Tr;
import net.folianpc.api.ClickType;
import net.folianpc.api.FoliaNpc;
import net.folianpc.api.Npc;
import net.folianpc.api.NpcAction;
import net.folianpc.api.NpcBuilder;
import net.folianpc.api.Skin;
import net.folianpc.api.Stats;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

public final class NpcService {

    public enum Status {
        ACTIVE,
        PENDING,
        DISABLED,
        UNKNOWN
    }

    private static final String CHANNEL = "BungeeCord";

    private final Map<String, LongAdder> clicks = new ConcurrentHashMap<>();
    private final Plugin plugin;
    private final ConfigFile file;
    private final FoliaNpc npcs;
    private final NpcText text;
    private final NpcActionCompiler compiler;

    private final Map<String, Live> live = new ConcurrentHashMap<>();
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();
    private final Map<String, NpcDefinition> disabled = new ConcurrentHashMap<>();

    private volatile List<String> knownIds = List.of();
    private volatile List<String> lastWarnings = List.of();

    private record Live(NpcDefinition definition, String fingerprint, Npc npc) {
    }

    private record Pending(NpcDefinition definition, String fingerprint) {
    }

    private NpcService(Plugin plugin, ConfigFile file, VaultEconomy economy, FoliaNpc npcs) {
        this.plugin = plugin;
        this.file = file;
        this.npcs = npcs;
        this.text = new NpcText(plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI"));
        this.compiler = new NpcActionCompiler(economy, text);
        npcs.placeholders(text::nametag);
        if (!plugin.getServer().getMessenger().isOutgoingChannelRegistered(plugin, CHANNEL)) {
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        }
        List<String> missing = npcs.capabilities().missing();
        if (!missing.isEmpty()) {
            PluginLog.warn(LogTopic.NPC, Tr.t("Fonctions indisponibles sur ce serveur : ") + String.join(", ", missing));
        }
    }

    public static NpcService create(Plugin plugin, ConfigFile file, VaultEconomy economy) {
        try {
            return new NpcService(plugin, file, economy, FoliaNpc.create(plugin));
        } catch (RuntimeException failure) {
            PluginLog.error(LogTopic.NPC, Tr.t("Système de PNJ indisponible, il reste désactivé"), failure);
            return null;
        }
    }

    public synchronized NpcLoadReport reload() {
        file.load();
        return apply();
    }

    public synchronized NpcLoadReport edit(Consumer<YamlConfiguration> change) {
        change.accept(file.get());
        file.save();
        return apply();
    }

    public synchronized NpcLoadReport apply() {
        YamlConfiguration root = file.get();
        ConfigurationSection settings = root.getConfigurationSection("settings");
        applySettings(settings);

        List<String> warnings = new ArrayList<>();
        NpcDefaults defaults = NpcDefaults.read(settings, warnings);
        Map<String, NpcDefinition> wanted = new LinkedHashMap<>();
        Map<String, String> prints = new LinkedHashMap<>();
        disabled.clear();
        ConfigurationSection section = root.getConfigurationSection("npcs");
        TreeSet<String> ids = new TreeSet<>();
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ids.add(id);
                ConfigurationSection child = section.getConfigurationSection(id);
                if (child == null) {
                    warnings.add(id + Tr.t(" : entrée invalide, une section est attendue"));
                    continue;
                }
                NpcDefinition definition = NpcDefinition.parse(id, child, defaults, warnings);
                if (definition == null) {
                    continue;
                }
                if (!definition.enabled()) {
                    disabled.put(id, definition);
                    continue;
                }
                wanted.put(id, definition);
                prints.put(id, defaults.cooldownMillis() + "\n" + NpcDefinition.fingerprint(child));
            }
        }

        Map<String, String> current = new HashMap<>();
        live.forEach((id, entry) -> current.put(id, entry.fingerprint()));
        pending.forEach((id, entry) -> current.put(id, entry.fingerprint()));
        NpcReloadPlan plan = NpcReloadPlan.between(current, prints);

        plan.remove().forEach(this::despawn);
        for (String id : plan.respawn()) {
            despawn(id);
            place(wanted.get(id), prints.get(id), warnings);
        }
        for (String id : plan.spawn()) {
            place(wanted.get(id), prints.get(id), warnings);
        }
        int restyled = 0;
        for (String id : plan.keep()) {
            if (refresh(wanted.get(id), prints.get(id))) {
                restyled++;
            }
        }
        if (!plan.idle()) {
            text.clear();
        }

        knownIds = List.copyOf(ids);
        lastWarnings = List.copyOf(warnings);
        warnings.forEach(warning -> PluginLog.warn(LogTopic.NPC, warning));
        return new NpcLoadReport(live.size(), plan.spawn().size(), plan.respawn().size(),
                plan.remove().size(), plan.keep().size(), restyled, pending.size(), disabled.size(),
                List.copyOf(warnings));
    }

    public synchronized void onWorldLoad(World world) {
        List<String> warnings = new ArrayList<>();
        for (Pending entry : List.copyOf(pending.values())) {
            if (entry.definition().world().equals(world.getName())) {
                place(entry.definition(), entry.fingerprint(), warnings);
            }
        }
        warnings.forEach(warning -> PluginLog.warn(LogTopic.NPC, warning));
    }

    public synchronized void close() {
        live.clear();
        pending.clear();
        npcs.close();
    }

    public List<String> ids() {
        return knownIds;
    }

    public Status status(String id) {
        if (live.containsKey(id)) {
            return Status.ACTIVE;
        }
        if (pending.containsKey(id)) {
            return Status.PENDING;
        }
        if (disabled.containsKey(id)) {
            return Status.DISABLED;
        }
        return Status.UNKNOWN;
    }

    public Optional<NpcDefinition> definition(String id) {
        Live active = live.get(id);
        if (active != null) {
            return Optional.of(active.definition());
        }
        Pending waiting = pending.get(id);
        if (waiting != null) {
            return Optional.of(waiting.definition());
        }
        return Optional.ofNullable(disabled.get(id));
    }

    public String idOf(Npc npc) {
        for (Map.Entry<String, Live> entry : live.entrySet()) {
            if (entry.getValue().npc() == npc) {
                return entry.getKey();
            }
        }
        return null;
    }

    public int activeCount() {
        return live.size();
    }

    public int pendingCount() {
        return pending.size();
    }

    public int disabledCount() {
        return disabled.size();
    }

    private NpcAction counting(String id, NpcAction action) {
        return context -> {
            clicks.computeIfAbsent(id, key -> new LongAdder()).increment();
            action.run(context);
        };
    }

    public Map<String, Long> clicks() {
        Map<String, Long> counts = new LinkedHashMap<>();
        clicks.forEach((id, count) -> counts.put(id, count.sum()));
        return counts;
    }

    public Stats stats() {
        return npcs.stats();
    }

    public List<String> missingCapabilities() {
        return npcs.capabilities().missing();
    }

    public List<String> lastWarnings() {
        return lastWarnings;
    }

    private void applySettings(ConfigurationSection settings) {
        double viewDistance = settings == null ? 48.0D : settings.getDouble("view-distance", 48.0D);
        npcs.viewDistance(Math.max(8.0D, Math.min(256.0D, viewDistance)));
        long cacheMinutes = settings == null ? 30L : settings.getLong("skin-cache-minutes", 30L);
        npcs.skinCacheTtl(Duration.ofMinutes(Math.max(1L, cacheMinutes)));
        npcs.setDebug(settings != null && settings.getBoolean("debug", false));
    }

    private void place(NpcDefinition definition, String fingerprint, List<String> warnings) {
        World world = Bukkit.getWorld(definition.world());
        if (world == null) {
            pending.put(definition.id(), new Pending(definition, fingerprint));
            return;
        }
        pending.remove(definition.id());
        try {
            live.put(definition.id(), new Live(definition, fingerprint, spawn(definition, world, warnings)));
        } catch (RuntimeException failure) {
            warnings.add(definition.id() + Tr.t(" : apparition impossible, ") + failure.getMessage());
        }
    }

    private boolean refresh(NpcDefinition definition, String fingerprint) {
        Pending waiting = pending.get(definition.id());
        if (waiting != null) {
            pending.put(definition.id(), new Pending(definition, fingerprint));
            return false;
        }
        Live entry = live.get(definition.id());
        if (entry == null) {
            return false;
        }
        boolean restyle = !entry.definition().nametagStyle().equals(definition.nametagStyle());
        if (restyle) {
            entry.npc().nametagStyle(definition.nametagStyle().toLibrary());
        }
        live.put(definition.id(), new Live(definition, fingerprint, entry.npc()));
        return restyle;
    }

    private void despawn(String id) {
        pending.remove(id);
        Live entry = live.remove(id);
        if (entry != null && !entry.npc().removed()) {
            entry.npc().remove();
        }
    }

    private Npc spawn(NpcDefinition definition, World world, List<String> warnings) {
        NpcBuilder builder = npcs.builder()
                .name(definition.name())
                .type(definition.type())
                .location(new Location(world, definition.x(), definition.y(), definition.z(),
                        definition.yaw(), definition.pitch()))
                .lookAtPlayers(definition.lookAtPlayers())
                .viewDistance(definition.viewDistance())
                .cooldown(definition.cooldownMillis())
                .glowing(definition.glowing())
                .invisible(definition.invisible())
                .skinLayers(definition.skinLayers())
                .scale(definition.scale())
                .collidable(definition.collidable())
                .showInTabList(definition.showInTab())
                .pose(definition.pose())
                .baby(definition.baby())
                .mirrorSkin(definition.skin().mirror())
                .nametagStyle(definition.nametagStyle().toLibrary());
        if (definition.glowColor() != null) {
            builder.glowColor(definition.glowColor());
        }
        if (!definition.nametag().isEmpty()) {
            builder.nametag(definition.nametag().stream()
                    .map(line -> line.replace("%npc%", definition.name()))
                    .toArray(String[]::new));
        }
        if (definition.variant() != null) {
            builder.variant(definition.variant());
        }
        if (definition.variantName() != null) {
            builder.variant(definition.variantName());
        }
        if (definition.villager()) {
            builder.villagerProfession(definition.villagerProfession() == null ? "none" : definition.villagerProfession())
                    .villagerType(definition.villagerType() == null ? "plains" : definition.villagerType())
                    .villagerLevel(definition.villagerLevel());
        }
        if (definition.skin().kind() == NpcSkinSource.Kind.TEXTURE) {
            builder.skin(Skin.of(definition.skin().value(), definition.skin().signature()));
        }
        definition.equipment().forEach((slot, source) -> {
            ItemStack item = item(source);
            if (item == null) {
                warnings.add(definition.id() + Tr.t(" : objet d'équipement invalide pour ") + slot);
            } else {
                builder.equipment(slot, item);
            }
        });

        Npc npc = builder.spawn();
        String permission = definition.permission();
        if (permission != null) {
            npc.visibleWhen(player -> player.hasPermission(permission));
        }
        if (definition.nametagVisible() != null) {
            npc.nametagVisible(definition.nametagVisible());
        }
        if (definition.nametagRefreshTicks() > 0L) {
            npc.autoRefreshNametag(definition.nametagRefreshTicks());
        }
        Set<ClickType> counted = EnumSet.noneOf(ClickType.class);
        for (int index = 0; index < definition.actions().size(); index++) {
            NpcActionSpec spec = definition.actions().get(index);
            NpcAction action = compiler.compile(spec, new Location(world, definition.x(), definition.y(),
                    definition.z(), definition.yaw(), definition.pitch()), definition.id() + " action " + (index + 1), warnings);
            if (action == null) {
                continue;
            }
            for (ClickType click : spec.trigger().clicks()) {
                if (counted.add(click)) {
                    npc.addAction(click, counting(definition.id(), action), spec.delayTicks());
                } else {
                    npc.addAction(click, action, spec.delayTicks());
                }
            }
        }
        fetchSkin(definition, npc);
        return npc;
    }

    private void fetchSkin(NpcDefinition definition, Npc npc) {
        CompletableFuture<Skin> future = switch (definition.skin().kind()) {
            case PLAYER -> npcs.fetchSkin(definition.skin().value());
            case URL -> npcs.fetchSkinFromUrl(definition.skin().value());
            default -> null;
        };
        if (future == null) {
            return;
        }
        future.thenAccept(skin -> {
            if (skin != null && !npc.removed()) {
                npc.skin(skin);
            }
        }).exceptionally(failure -> {
            PluginLog.warn(LogTopic.NPC, definition.id() + Tr.t(" : skin introuvable pour \"") + definition.skin().value() + "\"");
            return null;
        });
    }

    private static ItemStack item(NpcEquipment source) {
        if (source.item() != null) {
            ItemStack item = ItemSpec.read(source.item(), Material.AIR);
            return item.getType().isAir() ? null : item;
        }
        Material material = ItemSpec.material(source.material(), null);
        return material == null || material.isAir() ? null : new ItemStack(material);
    }

}
