package com.kirugoldzzzz.puppeteer;

import com.kirugoldzzzz.puppeteer.common.text.Tr;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.EquipmentSlot;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record NpcEquipment(String material, ConfigurationSection item) {

    public static Map<EquipmentSlot, NpcEquipment> read(ConfigurationSection section, String id,
                                                        List<String> warnings) {
        Map<EquipmentSlot, NpcEquipment> equipment = new EnumMap<>(EquipmentSlot.class);
        if (section == null) {
            return equipment;
        }
        for (String key : section.getKeys(false)) {
            EquipmentSlot slot = slot(key);
            if (slot == null) {
                warnings.add(id + Tr.t(" : emplacement d'équipement inconnu \"") + key + "\"");
                continue;
            }
            ConfigurationSection item = section.getConfigurationSection(key);
            if (item != null) {
                equipment.put(slot, new NpcEquipment(null, item));
                continue;
            }
            String material = section.getString(key);
            if (material != null && !material.isBlank()) {
                equipment.put(slot, new NpcEquipment(material.trim(), null));
            }
        }
        return equipment;
    }

    public static EquipmentSlot slot(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT).replace('-', '_')) {
            case "HAND", "MAIN_HAND", "MAINHAND", "MAIN" -> EquipmentSlot.HAND;
            case "OFF_HAND", "OFFHAND", "OFF" -> EquipmentSlot.OFF_HAND;
            case "HEAD", "HELMET", "CASQUE" -> EquipmentSlot.HEAD;
            case "CHEST", "CHESTPLATE", "PLASTRON" -> EquipmentSlot.CHEST;
            case "LEGS", "LEGGINGS", "JAMBIERES" -> EquipmentSlot.LEGS;
            case "FEET", "BOOTS", "BOTTES" -> EquipmentSlot.FEET;
            case "BODY" -> EquipmentSlot.BODY;
            default -> null;
        };
    }
}
