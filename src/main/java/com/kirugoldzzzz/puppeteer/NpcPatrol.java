package com.kirugoldzzzz.puppeteer;

import com.kirugoldzzzz.puppeteer.common.scheduler.Scheduling;
import com.kirugoldzzzz.puppeteer.common.text.Tr;
import net.folianpc.api.Npc;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public record NpcPatrol(double speed, long pauseTicks, List<Point> points) {

    public static final NpcPatrol NONE = new NpcPatrol(1.0D, 40L, List.of());

    private static final long TICK_MILLIS = 50L;

    public record Point(double x, double y, double z) {
    }

    public NpcPatrol {
        speed = Math.max(0.1D, Math.min(10.0D, speed));
        pauseTicks = Math.max(0L, Math.min(12_000L, pauseTicks));
        points = points == null ? List.of() : List.copyOf(points);
    }

    public boolean active() {
        return !points.isEmpty();
    }

    public static NpcPatrol read(ConfigurationSection section, String id, List<String> warnings) {
        if (section == null) {
            return NONE;
        }
        List<Point> points = new ArrayList<>();
        for (String raw : section.getStringList("points")) {
            Point point = parse(raw);
            if (point == null) {
                warnings.add(id + Tr.t(" : point de patrouille invalide \"") + raw + Tr.t("\", attendu x y z"));
            } else {
                points.add(point);
            }
        }
        return new NpcPatrol(section.getDouble("speed", 1.0D), section.getLong("pause-ticks", 40L), points);
    }

    static Point parse(String raw) {
        if (raw == null) {
            return null;
        }
        String[] parts = raw.strip().split("[\\s,]+");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new Point(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
        } catch (NumberFormatException invalid) {
            return null;
        }
    }

    public List<Location> route(World world, double x, double y, double z, float yaw, float pitch) {
        List<Location> route = new ArrayList<>(points.size() + 1);
        route.add(new Location(world, x, y, z, yaw, pitch));
        for (Point point : points) {
            route.add(new Location(world, point.x(), point.y(), point.z()));
        }
        return route;
    }

    public void start(Npc npc, List<Location> route) {
        if (route.size() < 2) {
            return;
        }
        step(npc, route, 1);
    }

    private void step(Npc npc, List<Location> route, int index) {
        if (npc.removed()) {
            return;
        }
        Location target = route.get(index);
        int next = (index + 1) % route.size();
        npc.navigateTo(target, speed).whenComplete((reached, failure) -> {
            if (npc.removed()) {
                return;
            }
            long wait = pauseTicks * TICK_MILLIS;
            if (failure != null || !Boolean.TRUE.equals(reached)) {
                npc.walkTo(target, speed);
                Location from = route.get((index - 1 + route.size()) % route.size());
                wait += (long) (from.distance(target) / speed * 1000.0D);
            }
            Scheduling.asyncLater(() -> step(npc, route, next), Math.max(TICK_MILLIS, wait));
        });
    }
}
