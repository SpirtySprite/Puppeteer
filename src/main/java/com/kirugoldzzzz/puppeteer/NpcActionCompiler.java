package com.kirugoldzzzz.puppeteer;

import com.kirugoldzzzz.puppeteer.common.text.Messages;
import com.kirugoldzzzz.puppeteer.common.text.Mini;
import com.kirugoldzzzz.puppeteer.common.text.Numbers;
import com.kirugoldzzzz.puppeteer.VaultEconomy;
import net.folianpc.api.Actions;
import net.folianpc.api.NpcAction;
import net.folianpc.api.NpcClickContext;
import net.kyori.adventure.text.Component;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class NpcActionCompiler {

    private static volatile Map<String, String> legacySounds;

    private static final int DEFAULT_PARTICLE_COUNT = 20;
    private static final long TICK_MILLIS = 50L;

    private final VaultEconomy economy;
    private final NpcText text;

    NpcActionCompiler(VaultEconomy economy, NpcText text) {
        this.economy = economy;
        this.text = text;
    }

    NpcAction compile(NpcActionSpec spec, Location base, String context, List<String> warnings) {
        NpcAction body = body(spec, base, context, warnings);
        if (body == null) {
            return null;
        }
        return ctx -> {
            if (!spec.sneak().accepts(ctx.sneaking())) {
                return;
            }
            Player player = ctx.player();
            if (spec.permission() != null && !player.hasPermission(spec.permission())) {
                if (spec.denyMessage() != null) {
                    player.sendMessage(text.chat(player, spec.denyMessage(), ctx.npc().name()));
                }
                return;
            }
            body.run(ctx);
        };
    }

    private NpcAction body(NpcActionSpec spec, Location base, String context, List<String> warnings) {
        return switch (spec.type()) {
            case MESSAGE -> ctx -> ctx.player().sendMessage(chat(ctx, spec.value()));
            case BROADCAST -> ctx -> Bukkit.getServer().sendMessage(chat(ctx, spec.value()));
            case ACTIONBAR -> ctx -> ctx.player().sendActionBar(chat(ctx, spec.value()));
            case TITLE -> title(spec);
            case PLAYER_COMMAND -> ctx -> ctx.player().performCommand(
                    NpcText.command(text.resolve(ctx.player(), spec.value(), ctx.npc().name())));
            case CONSOLE_COMMAND -> ctx -> {
                String command = NpcText.command(text.resolve(ctx.player(), spec.value(), ctx.npc().name()));
                ctx.runGlobal(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
            };
            case SOUND -> sound(spec, context, warnings);
            case TELEPORT -> teleport(spec, context, warnings);
            case SERVER -> Actions.connectToServer(spec.value().trim());
            case EFFECT -> effect(spec, context, warnings);
            case PARTICLE -> particle(spec, context, warnings);
            case GIVE_MONEY -> ctx -> {
                if (economy.deposit(ctx.player().getUniqueId(), spec.amount()) && spec.value() != null) {
                    ctx.player().sendMessage(chat(ctx, spec.value()));
                }
            };
            case TAKE_MONEY -> ctx -> {
                Player player = ctx.player();
                if (!economy.withdraw(player.getUniqueId(), spec.amount())) {
                    player.sendMessage(spec.denyMessage() != null
                            ? chat(ctx, spec.denyMessage())
                            : Messages.get("npc.no-money", Mini.value("amount", Numbers.money(spec.amount()))));
                    ctx.cancelRemaining();
                    return;
                }
                if (spec.value() != null) {
                    player.sendMessage(chat(ctx, spec.value()));
                }
            };
            case REQUIRE_PERMISSION -> ctx -> {
                if (ctx.player().hasPermission(spec.value().trim())) {
                    return;
                }
                ctx.player().sendMessage(spec.denyMessage() != null
                        ? chat(ctx, spec.denyMessage())
                        : Messages.get("npc.denied"));
                ctx.cancelRemaining();
            };
            case SWING -> ctx -> ctx.npc().swing();
        };
    }

    private Component chat(NpcClickContext ctx, String raw) {
        return text.chat(ctx.player(), raw, ctx.npc().name());
    }

    private NpcAction title(NpcActionSpec spec) {
        Title.Times times = Title.Times.times(ticks(spec.fadeIn()), ticks(spec.duration()), ticks(spec.fadeOut()));
        return ctx -> ctx.player().showTitle(Title.title(chat(ctx, spec.value()),
                spec.subtitle() == null ? Component.empty() : chat(ctx, spec.subtitle()), times));
    }

    private NpcAction sound(NpcActionSpec spec, String context, List<String> warnings) {
        String key = soundKey(spec.value());
        if (key == null) {
            warnings.add(context + " : son inconnu \"" + spec.value() + "\"");
            return null;
        }
        return ctx -> ctx.player().playSound(ctx.player().getLocation(), key, spec.volume(), spec.pitch());
    }

    private NpcAction teleport(NpcActionSpec spec, String context, List<String> warnings) {
        String[] parts = spec.value().trim().split("[\\s,]+");
        if (parts.length < 4) {
            warnings.add(context + " : destination invalide, attendu \"monde x y z [yaw pitch]\"");
            return null;
        }
        double[] coordinates = new double[5];
        try {
            for (int index = 1; index < parts.length && index < 6; index++) {
                coordinates[index - 1] = Double.parseDouble(parts[index]);
            }
        } catch (NumberFormatException invalid) {
            warnings.add(context + " : coordonnées de téléportation invalides \"" + spec.value() + "\"");
            return null;
        }
        String worldName = parts[0];
        return ctx -> {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                ctx.player().sendMessage(Messages.get("npc.world-missing", Mini.value("world", worldName)));
                ctx.cancelRemaining();
                return;
            }
            ctx.player().teleportAsync(new Location(world, coordinates[0], coordinates[1], coordinates[2],
                    (float) coordinates[3], (float) coordinates[4]));
        };
    }

    private NpcAction effect(NpcActionSpec spec, String context, List<String> warnings) {
        NamespacedKey effectKey = NamespacedKey.fromString(spec.value().trim().toLowerCase(Locale.ROOT));
        PotionEffectType type = effectKey == null ? null
                : RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT).get(effectKey);
        if (type == null) {
            warnings.add(context + " : effet inconnu \"" + spec.value() + "\"");
            return null;
        }
        int amplifier = Math.max(0, (int) Math.round(spec.amount()) - 1);
        return ctx -> ctx.player().addPotionEffect(
                new PotionEffect(type, spec.duration(), amplifier, false, true, true));
    }

    private NpcAction particle(NpcActionSpec spec, String context, List<String> warnings) {
        Particle particle;
        try {
            particle = Particle.valueOf(spec.value().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            warnings.add(context + " : particule inconnue \"" + spec.value() + "\"");
            return null;
        }
        if (particle.getDataType() != Void.class) {
            warnings.add(context + " : la particule " + particle + " demande des données, choisissez-en une autre");
            return null;
        }
        int count = spec.amount() > 0.0D ? (int) Math.round(spec.amount()) : DEFAULT_PARTICLE_COUNT;
        return ctx -> ctx.player().spawnParticle(particle, ctx.npc().x(), ctx.npc().y() + 1.0D,
                ctx.npc().z(), count, 0.35D, 0.6D, 0.35D, 0.02D);
    }

    static String soundKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.indexOf('.') >= 0 || value.indexOf(':') >= 0) {
            return value.toLowerCase(Locale.ROOT);
        }
        try {
            return legacySounds().get(value.toUpperCase(Locale.ROOT));
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    private static Map<String, String> legacySounds() {
        Map<String, String> cached = legacySounds;
        if (cached != null) {
            return cached;
        }
        Map<String, String> built = new HashMap<>();
        RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT).keyStream()
                .filter(key -> NamespacedKey.MINECRAFT.equals(key.getNamespace()))
                .forEach(key -> built.put(key.getKey().toUpperCase(Locale.ROOT).replace('.', '_'), key.toString()));
        legacySounds = Map.copyOf(built);
        return legacySounds;
    }

    private static Duration ticks(int ticks) {
        return Duration.ofMillis(ticks * TICK_MILLIS);
    }
}
