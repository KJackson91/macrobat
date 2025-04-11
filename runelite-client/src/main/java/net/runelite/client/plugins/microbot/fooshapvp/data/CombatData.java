package net.runelite.client.plugins.microbot.fooshapvp.data;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombatData {
    private int tick;
    private String hero;
    private String opponent;
    private int hero_hp;
    private int opponent_hp;
    private int hero_spec;
    private int[] hero_tile;
    private int[] opponent_tile;
    private int[][] hero_tile_access;
    private Integer hero_animation;
    private Integer opponent_animation;
    private Integer[] hero_projectile_id;
    private Integer[] opponent_projectile_id;
    private List<Hitsplat> hero_hitsplats;
    private List<Hitsplat> opponent_hitsplats;
    private Equipment hero_equipment;
    private Equipment opponent_equipment;
    private List<InventoryItem> hero_inventory;
    private Levels hero_levels;
    private List<String> hero_prayers;
    private boolean hero_cast_veng;
    private boolean opponent_cast_veng;
    private boolean hero_menaphite_proc;
    private boolean opponent_menaphite_proc;
    private boolean did_hero_pop_veng;
    private boolean did_opponent_pop_veng;
    private Map<String, Integer> hero_xp_drops;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Hitsplat {
        private int damage;
        private String type;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Equipment {
        private int head;
        private int neck;
        private int cape;
        private int weapon;
        private int body;
        private int shield;
        private int legs;
        private int ring;
        private int feet;
        private int hands;
        private int ammunition;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryItem {
        private int id;
        private int quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Levels {
        private SkillLevel attack;
        private SkillLevel strength;
        private SkillLevel defence;
        private SkillLevel ranged;
        private SkillLevel prayer;
        private SkillLevel magic;
        private SkillLevel hitpoints;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillLevel {
        private int base_level;
        private int current_level;
    }
}
