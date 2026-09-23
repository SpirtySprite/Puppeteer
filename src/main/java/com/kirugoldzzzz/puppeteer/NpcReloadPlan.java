package com.kirugoldzzzz.puppeteer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record NpcReloadPlan(List<String> spawn, List<String> respawn, List<String> remove,
                            List<String> keep) {

    public static NpcReloadPlan between(Map<String, String> current, Map<String, String> desired) {
        List<String> spawn = new ArrayList<>();
        List<String> respawn = new ArrayList<>();
        List<String> keep = new ArrayList<>();
        List<String> remove = new ArrayList<>();
        for (Map.Entry<String, String> wanted : desired.entrySet()) {
            String existing = current.get(wanted.getKey());
            if (existing == null) {
                spawn.add(wanted.getKey());
            } else if (Objects.equals(existing, wanted.getValue())) {
                keep.add(wanted.getKey());
            } else {
                respawn.add(wanted.getKey());
            }
        }
        for (String id : current.keySet()) {
            if (!desired.containsKey(id)) {
                remove.add(id);
            }
        }
        return new NpcReloadPlan(List.copyOf(spawn), List.copyOf(respawn), List.copyOf(remove),
                List.copyOf(keep));
    }

    public boolean idle() {
        return spawn.isEmpty() && respawn.isEmpty() && remove.isEmpty();
    }
}
