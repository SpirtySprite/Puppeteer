package com.kirugoldzzzz.puppeteer;

import java.util.List;

public record NpcLoadReport(int active, int spawned, int respawned, int removed, int kept,
                            int restyled, int pending, int disabled, List<String> warnings) {
}
