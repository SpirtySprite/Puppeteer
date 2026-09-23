package com.kirugoldzzzz.puppeteer;

import com.kirugoldzzzz.puppeteer.common.text.Tr;

import com.kirugoldzzzz.puppeteer.common.command.NexusCommand;
import com.kirugoldzzzz.puppeteer.common.config.Sections;
import com.kirugoldzzzz.puppeteer.common.scheduler.Scheduling;
import com.kirugoldzzzz.puppeteer.common.text.Messages;
import com.kirugoldzzzz.puppeteer.common.text.Mini;
import net.folianpc.api.Stats;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public final class NpcCommand extends NexusCommand {

    private static final String PERMISSION = "puppeteer.admin.npc";
    private static final List<String> ACTIONS = List.of("reload", "list", "info", "create", "delete",
            "movehere", "tp", "rename", "skin", "copy", "enable", "disable", "stats");
    private static final List<String> ID_ARGUMENT = List.of("info", "delete", "movehere", "tp", "rename",
            "skin", "copy", "enable", "disable");
    private static final List<String> SKIN_OPTIONS = List.of("mirror", "none");
    private static final List<String> TYPES = Arrays.stream(EntityType.values())
            .filter(type -> type == EntityType.PLAYER || type.isAlive())
            .map(type -> type.name().toLowerCase(Locale.ROOT))
            .toList();

    private final NpcService service;
    private Runnable reloadSettings = () -> {
    };

    public NpcCommand(NpcService service) {
        super(PERMISSION, false);
        this.service = service;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (service == null) {
            Messages.send(sender, "npc.unavailable");
            return;
        }
        if (args.length == 0) {
            Messages.send(sender, "npc.usage");
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "list", "liste" -> list(sender);
            case "info" -> existing(sender, args, 2).ifPresent(id -> info(sender, id));
            case "create", "creer" -> create(sender, args);
            case "delete", "supprimer" -> existing(sender, args, 2).ifPresent(id -> edit(sender,
                    root -> root.set(path(id), null), "npc.deleted", Mini.value("id", id)));
            case "movehere", "deplacer" -> movehere(sender, args);
            case "tp" -> teleport(sender, args);
            case "rename", "renommer" -> rename(sender, args);
            case "skin" -> skin(sender, args);
            case "copy", "copier" -> copy(sender, args);
            case "enable", "activer" -> toggle(sender, args, true);
            case "disable", "desactiver" -> toggle(sender, args, false);
            case "stats" -> stats(sender);
            default -> Messages.send(sender, "npc.usage");
        }
    }

    public void onReload(Runnable action) {
        this.reloadSettings = action;
    }

    private void reload(CommandSender sender) {
        reloadSettings.run();
        Scheduling.async(() -> report(sender, service.reload(), "npc.reloaded"));
    }

    private void list(CommandSender sender) {
        List<String> ids = service.ids();
        if (ids.isEmpty()) {
            Messages.send(sender, "npc.list-empty");
            return;
        }
        Messages.send(sender, "npc.list-header", Mini.value("count", String.valueOf(ids.size())),
                Mini.value("active", String.valueOf(service.activeCount())));
        for (String id : ids) {
            NpcService.Status status = service.status(id);
            Optional<NpcDefinition> definition = service.definition(id);
            Messages.send(sender, "npc.list-entry",
                    Mini.value("id", id),
                    Mini.component("status", Messages.get("npc.status." + key(status))),
                    Mini.value("type", definition.map(value -> value.type().name().toLowerCase(Locale.ROOT)).orElse("?")),
                    Mini.value("position", definition.map(NpcCommand::position).orElse("?")));
        }
    }

    private void info(CommandSender sender, String id) {
        Optional<NpcDefinition> found = service.definition(id);
        if (found.isEmpty()) {
            Messages.send(sender, "npc.unknown", Mini.value("id", id));
            return;
        }
        NpcDefinition definition = found.get();
        Messages.lines("npc.info",
                Mini.value("id", id),
                Mini.component("status", Messages.get("npc.status." + key(service.status(id)))),
                Mini.value("name", definition.name()),
                Mini.value("type", definition.type().name().toLowerCase(Locale.ROOT)),
                Mini.value("position", position(definition)),
                Mini.value("skin", definition.skin().describe()),
                Mini.value("style", definition.nametagStyle().describe()),
                Mini.value("left", String.valueOf(definition.actionCount(NpcTrigger.LEFT))),
                Mini.value("right", String.valueOf(definition.actionCount(NpcTrigger.RIGHT))),
                Mini.value("both", String.valueOf(definition.actionCount(NpcTrigger.BOTH))),
                Mini.value("cooldown", String.valueOf(definition.cooldownMillis())),
                Mini.value("permission", definition.permission() == null ? Tr.t("aucune") : definition.permission()))
                .forEach(sender::sendMessage);
    }

    private void create(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            Messages.send(sender, "npc.usage");
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (!NpcDefinition.validId(id)) {
            Messages.send(sender, "npc.invalid-id", Mini.value("id", args[1]));
            return;
        }
        if (service.ids().contains(id)) {
            Messages.send(sender, "npc.exists", Mini.value("id", id));
            return;
        }
        EntityType type = args.length > 2 ? NpcDefinition.entityType(args[2]) : EntityType.PLAYER;
        if (type == null) {
            Messages.send(sender, "npc.invalid-type", Mini.value("type", args[2]));
            return;
        }
        Location location = player.getLocation();
        String skin = player.getName();
        edit(sender, root -> {
            ConfigurationSection section = root.createSection(path(id));
            section.set("enabled", true);
            section.set("name", id);
            section.set("type", type.name());
            writeLocation(section, location);
            if (type == EntityType.PLAYER) {
                section.set("skin", skin);
            }
            section.set("nametag", List.of("<primary><b>%npc%", Tr.t("<muted>Clic droit")));
            section.set("look-at-players", true);
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("click", "RIGHT");
            action.put("type", "MESSAGE");
            action.put("value", Tr.t("<primary>%npc% <muted>| <text>Bonjour %player% !"));
            section.set("actions", List.of(action));
        }, "npc.created", Mini.value("id", id));
    }

    private void movehere(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null) {
            return;
        }
        Optional<String> id = existing(sender, args, 2);
        if (id.isEmpty()) {
            return;
        }
        Location location = player.getLocation();
        edit(sender, root -> writeLocation(section(root, id.get()), location),
                "npc.moved", Mini.value("id", id.get()));
    }

    private void teleport(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null) {
            return;
        }
        Optional<NpcDefinition> definition = existing(sender, args, 2).flatMap(service::definition);
        if (definition.isEmpty()) {
            return;
        }
        NpcDefinition target = definition.get();
        World world = Bukkit.getWorld(target.world());
        if (world == null) {
            Messages.send(sender, "npc.world-missing", Mini.value("world", target.world()));
            return;
        }
        player.teleportAsync(new Location(world, target.x(), target.y(), target.z(), target.yaw(), target.pitch()))
                .thenAccept(done -> {
                    if (done) {
                        Messages.send(sender, "npc.teleported", Mini.value("id", target.id()));
                    }
                });
    }

    private void rename(CommandSender sender, String[] args) {
        Optional<String> id = existing(sender, args, 3);
        if (id.isEmpty()) {
            return;
        }
        String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        edit(sender, root -> section(root, id.get()).set("name", name),
                "npc.renamed", Mini.value("id", id.get()), Mini.value("name", name));
    }

    private void skin(CommandSender sender, String[] args) {
        Optional<String> id = existing(sender, args, 3);
        if (id.isEmpty()) {
            return;
        }
        String value = args[2];
        if (value.equalsIgnoreCase("none") || value.equalsIgnoreCase("aucun")) {
            edit(sender, root -> section(root, id.get()).set("skin", null),
                    "npc.skin-cleared", Mini.value("id", id.get()));
            return;
        }
        String stored = value.equalsIgnoreCase("mirror") ? "mirror" : value;
        edit(sender, root -> section(root, id.get()).set("skin", stored),
                "npc.skin-set", Mini.value("id", id.get()), Mini.value("skin", stored));
    }

    private void copy(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null) {
            return;
        }
        Optional<String> id = existing(sender, args, 3);
        if (id.isEmpty()) {
            return;
        }
        String target = args[2].toLowerCase(Locale.ROOT);
        if (!NpcDefinition.validId(target)) {
            Messages.send(sender, "npc.invalid-id", Mini.value("id", args[2]));
            return;
        }
        if (service.ids().contains(target)) {
            Messages.send(sender, "npc.exists", Mini.value("id", target));
            return;
        }
        Location location = player.getLocation();
        edit(sender, root -> {
            ConfigurationSection source = section(root, id.get());
            ConfigurationSection copy = root.createSection(path(target));
            Sections.copy(source, copy);
            writeLocation(copy, location);
        }, "npc.copied", Mini.value("id", id.get()), Mini.value("copy", target));
    }

    private void toggle(CommandSender sender, String[] args, boolean enabled) {
        existing(sender, args, 2).ifPresent(id -> edit(sender,
                root -> section(root, id).set("enabled", enabled),
                enabled ? "npc.enabled" : "npc.disabled", Mini.value("id", id)));
    }

    private void stats(CommandSender sender) {
        Stats stats = service.stats();
        List<String> missing = service.missingCapabilities();
        Messages.lines("npc.stats",
                Mini.value("active", String.valueOf(service.activeCount())),
                Mini.value("pending", String.valueOf(service.pendingCount())),
                Mini.value("disabled", String.valueOf(service.disabledCount())),
                Mini.value("shows", String.valueOf(stats.viewerShows())),
                Mini.value("packets", String.valueOf(stats.packetsSent())),
                Mini.value("tick", String.format(Locale.ROOT, "%.3f", stats.lastTickMillis())),
                Mini.value("missing", missing.isEmpty() ? Tr.t("aucune") : String.join(", ", missing)),
                Mini.value("warnings", String.valueOf(service.lastWarnings().size())))
                .forEach(sender::sendMessage);
    }

    private void edit(CommandSender sender, Consumer<YamlConfiguration> change, String key,
                      TagResolver... resolvers) {
        Scheduling.async(() -> {
            NpcLoadReport report = service.edit(change);
            Messages.send(sender, key, resolvers);
            warnings(sender, report);
        });
    }

    private void report(CommandSender sender, NpcLoadReport report, String key) {
        Messages.send(sender, key,
                Mini.value("active", String.valueOf(report.active())),
                Mini.value("spawned", String.valueOf(report.spawned())),
                Mini.value("respawned", String.valueOf(report.respawned())),
                Mini.value("removed", String.valueOf(report.removed())),
                Mini.value("kept", String.valueOf(report.kept())),
                Mini.value("restyled", String.valueOf(report.restyled())));
        warnings(sender, report);
    }

    private static void warnings(CommandSender sender, NpcLoadReport report) {
        if (report.pending() > 0) {
            Messages.send(sender, "npc.pending", Mini.value("count", String.valueOf(report.pending())));
        }
        if (report.warnings().isEmpty()) {
            return;
        }
        Messages.send(sender, "npc.warnings", Mini.value("count", String.valueOf(report.warnings().size())));
        report.warnings().stream().limit(8)
                .forEach(warning -> Messages.send(sender, "npc.warning", Mini.value("warning", warning)));
    }

    private Optional<String> existing(CommandSender sender, String[] args, int required) {
        if (args.length < required) {
            Messages.send(sender, "npc.usage");
            return Optional.empty();
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (!service.ids().contains(id)) {
            Messages.send(sender, "npc.unknown", Mini.value("id", args[1]));
            return Optional.empty();
        }
        return Optional.of(id);
    }

    private static Player player(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            Messages.send(sender, "general.players-only");
        }
        return player;
    }

    private static ConfigurationSection section(YamlConfiguration root, String id) {
        ConfigurationSection section = root.getConfigurationSection(path(id));
        return section == null ? root.createSection(path(id)) : section;
    }

    private static String path(String id) {
        return "npcs." + id;
    }

    private static void writeLocation(ConfigurationSection section, Location location) {
        ConfigurationSection target = section.createSection("location");
        target.set("world", location.getWorld().getName());
        target.set("x", round(location.getX()));
        target.set("y", round(location.getY()));
        target.set("z", round(location.getZ()));
        target.set("yaw", round(location.getYaw()));
        target.set("pitch", round(location.getPitch()));
    }

    private static double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    private static String position(NpcDefinition definition) {
        return String.format(Locale.ROOT, "%s %.1f %.1f %.1f", definition.world(),
                definition.x(), definition.y(), definition.z());
    }

    private static String key(NpcService.Status status) {
        return status.name().toLowerCase(Locale.ROOT);
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (service == null) {
            return List.of();
        }
        if (args.length == 1) {
            return match(ACTIONS, args[0]);
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && ID_ARGUMENT.contains(action)) {
            return match(service.ids(), args[1]);
        }
        if (args.length == 3 && action.equals("create")) {
            return match(TYPES, args[2]);
        }
        if (args.length == 3 && action.equals("skin")) {
            List<String> options = new ArrayList<>(SKIN_OPTIONS);
            Bukkit.getOnlinePlayers().forEach(online -> options.add(online.getName()));
            return match(options, args[2]);
        }
        return List.of();
    }
}
