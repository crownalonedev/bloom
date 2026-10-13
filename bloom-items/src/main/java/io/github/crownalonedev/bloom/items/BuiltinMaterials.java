package io.github.crownalonedev.bloom.items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Built-in modern ↔ legacy material table for common items whose names or data values changed in 1.13.
 *
 * <p>Each row is {@code modernName, legacyName, legacyData}. The table intentionally covers the materials plugins use
 * most (colours, woods, stone variants, renamed tools/items, heads, flowers). It is not exhaustive: potions, spawn
 * eggs, banners and other items whose identity moved into NBT are not mapped. Developers can add rows through
 * {@link MaterialMappings#register(String, String, String, int)}.
 */
final class BuiltinMaterials {

    /** Dye colour order used by legacy data values 0-15 for wool, glass, clay and carpet. */
    static final String[] COLORS = {
        "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY",
        "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"
    };

    private static final String[] WOODS = {"OAK", "SPRUCE", "BIRCH", "JUNGLE", "ACACIA", "DARK_OAK"};

    private BuiltinMaterials() {}

    static final class Row {
        final String modern;
        final String legacy;
        final short data;

        Row(String modern, String legacy, int data) {
            this.modern = modern;
            this.legacy = legacy;
            this.data = (short) data;
        }
    }

    static List<Row> rows() {
        List<Row> rows = new ArrayList<Row>();
        for (int i = 0; i < COLORS.length; i++) {
            rows.add(new Row(COLORS[i] + "_WOOL", "WOOL", i));
            rows.add(new Row(COLORS[i] + "_CARPET", "CARPET", i));
            rows.add(new Row(COLORS[i] + "_STAINED_GLASS", "STAINED_GLASS", i));
            rows.add(new Row(COLORS[i] + "_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", i));
            rows.add(new Row(COLORS[i] + "_TERRACOTTA", "STAINED_CLAY", i));
        }
        String[] dyes = {
            "INK_SAC", "RED_DYE", "GREEN_DYE", "COCOA_BEANS", "LAPIS_LAZULI", "PURPLE_DYE", "CYAN_DYE",
            "LIGHT_GRAY_DYE", "GRAY_DYE", "PINK_DYE", "LIME_DYE", "YELLOW_DYE", "LIGHT_BLUE_DYE", "MAGENTA_DYE",
            "ORANGE_DYE", "BONE_MEAL"
        };
        for (int i = 0; i < dyes.length; i++) {
            rows.add(new Row(dyes[i], "INK_SACK", i));
        }
        for (int i = 0; i < WOODS.length; i++) {
            rows.add(new Row(WOODS[i] + "_PLANKS", "WOOD", i));
            rows.add(new Row(WOODS[i] + "_SAPLING", "SAPLING", i));
            rows.add(new Row(WOODS[i] + "_SLAB", "WOOD_STEP", i));
            if (i < 4) {
                rows.add(new Row(WOODS[i] + "_LOG", "LOG", i));
                rows.add(new Row(WOODS[i] + "_LEAVES", "LEAVES", i));
            } else {
                rows.add(new Row(WOODS[i] + "_LOG", "LOG_2", i - 4));
                rows.add(new Row(WOODS[i] + "_LEAVES", "LEAVES_2", i - 4));
            }
        }
        String[][] variants = {
            {"GRANITE", "STONE", "1"},
            {"POLISHED_GRANITE", "STONE", "2"},
            {"DIORITE", "STONE", "3"},
            {"POLISHED_DIORITE", "STONE", "4"},
            {"ANDESITE", "STONE", "5"},
            {"POLISHED_ANDESITE", "STONE", "6"},
            {"STONE_BRICKS", "SMOOTH_BRICK", "0"},
            {"MOSSY_STONE_BRICKS", "SMOOTH_BRICK", "1"},
            {"CRACKED_STONE_BRICKS", "SMOOTH_BRICK", "2"},
            {"CHISELED_STONE_BRICKS", "SMOOTH_BRICK", "3"},
            {"RED_SAND", "SAND", "1"},
            {"COARSE_DIRT", "DIRT", "1"},
            {"PODZOL", "DIRT", "2"},
            {"CHISELED_SANDSTONE", "SANDSTONE", "1"},
            {"CUT_SANDSTONE", "SANDSTONE", "2"},
            {"CHISELED_QUARTZ_BLOCK", "QUARTZ_BLOCK", "1"},
            {"QUARTZ_PILLAR", "QUARTZ_BLOCK", "2"},
            {"PRISMARINE_BRICKS", "PRISMARINE", "1"},
            {"DARK_PRISMARINE", "PRISMARINE", "2"},
            {"WET_SPONGE", "SPONGE", "1"},
            {"COBBLESTONE_WALL", "COBBLE_WALL", "0"},
            {"MOSSY_COBBLESTONE_WALL", "COBBLE_WALL", "1"},
            {"SHORT_GRASS", "LONG_GRASS", "1"},
            {"FERN", "LONG_GRASS", "2"},
            {"SUNFLOWER", "DOUBLE_PLANT", "0"},
            {"LILAC", "DOUBLE_PLANT", "1"},
            {"TALL_GRASS", "DOUBLE_PLANT", "2"},
            {"LARGE_FERN", "DOUBLE_PLANT", "3"},
            {"ROSE_BUSH", "DOUBLE_PLANT", "4"},
            {"PEONY", "DOUBLE_PLANT", "5"},
            {"POPPY", "RED_ROSE", "0"},
            {"BLUE_ORCHID", "RED_ROSE", "1"},
            {"ALLIUM", "RED_ROSE", "2"},
            {"AZURE_BLUET", "RED_ROSE", "3"},
            {"RED_TULIP", "RED_ROSE", "4"},
            {"ORANGE_TULIP", "RED_ROSE", "5"},
            {"WHITE_TULIP", "RED_ROSE", "6"},
            {"PINK_TULIP", "RED_ROSE", "7"},
            {"OXEYE_DAISY", "RED_ROSE", "8"},
            {"DANDELION", "YELLOW_FLOWER", "0"},
            {"CHARCOAL", "COAL", "1"},
            {"ENCHANTED_GOLDEN_APPLE", "GOLDEN_APPLE", "1"},
            {"COD", "RAW_FISH", "0"},
            {"SALMON", "RAW_FISH", "1"},
            {"TROPICAL_FISH", "RAW_FISH", "2"},
            {"PUFFERFISH", "RAW_FISH", "3"},
            {"COOKED_COD", "COOKED_FISH", "0"},
            {"COOKED_SALMON", "COOKED_FISH", "1"},
            {"SKELETON_SKULL", "SKULL_ITEM", "0"},
            {"WITHER_SKELETON_SKULL", "SKULL_ITEM", "1"},
            {"ZOMBIE_HEAD", "SKULL_ITEM", "2"},
            {"PLAYER_HEAD", "SKULL_ITEM", "3"},
            {"CREEPER_HEAD", "SKULL_ITEM", "4"},
        };
        for (String[] v : variants) {
            rows.add(new Row(v[0], v[1], Integer.parseInt(v[2])));
        }
        String[][] renames = {
            {"GRASS_BLOCK", "GRASS"},
            {"CLOCK", "WATCH"},
            {"EXPERIENCE_BOTTLE", "EXP_BOTTLE"},
            {"FIRE_CHARGE", "FIREBALL"},
            {"SNOWBALL", "SNOW_BALL"},
            {"WRITABLE_BOOK", "BOOK_AND_QUILL"},
            {"END_PORTAL_FRAME", "ENDER_PORTAL_FRAME"},
            {"CRAFTING_TABLE", "WORKBENCH"},
            {"ENCHANTING_TABLE", "ENCHANTMENT_TABLE"},
            {"GUNPOWDER", "SULPHUR"},
            {"NETHER_WART", "NETHER_STALK"},
            {"CHICKEN", "RAW_CHICKEN"},
            {"BEEF", "RAW_BEEF"},
            {"PORKCHOP", "PORK"},
            {"COOKED_PORKCHOP", "GRILLED_PORK"},
            {"MUSHROOM_STEW", "MUSHROOM_SOUP"},
            {"GOLDEN_SWORD", "GOLD_SWORD"},
            {"GOLDEN_AXE", "GOLD_AXE"},
            {"GOLDEN_PICKAXE", "GOLD_PICKAXE"},
            {"GOLDEN_SHOVEL", "GOLD_SPADE"},
            {"GOLDEN_HOE", "GOLD_HOE"},
            {"GOLDEN_HELMET", "GOLD_HELMET"},
            {"GOLDEN_CHESTPLATE", "GOLD_CHESTPLATE"},
            {"GOLDEN_LEGGINGS", "GOLD_LEGGINGS"},
            {"GOLDEN_BOOTS", "GOLD_BOOTS"},
            {"WOODEN_SWORD", "WOOD_SWORD"},
            {"WOODEN_AXE", "WOOD_AXE"},
            {"WOODEN_PICKAXE", "WOOD_PICKAXE"},
            {"WOODEN_SHOVEL", "WOOD_SPADE"},
            {"WOODEN_HOE", "WOOD_HOE"},
            {"STONE_SHOVEL", "STONE_SPADE"},
            {"IRON_SHOVEL", "IRON_SPADE"},
            {"DIAMOND_SHOVEL", "DIAMOND_SPADE"},
            {"OAK_SIGN", "SIGN"},
            {"RED_BED", "BED"},
            {"OAK_FENCE", "FENCE"},
            {"OAK_FENCE_GATE", "FENCE_GATE"},
            {"OAK_TRAPDOOR", "TRAP_DOOR"},
            {"OAK_DOOR", "WOOD_DOOR"},
            {"OAK_BOAT", "BOAT"},
            {"COBWEB", "WEB"},
            {"GLASS_PANE", "THIN_GLASS"},
            {"IRON_BARS", "IRON_FENCE"},
            {"NETHER_BRICK", "NETHER_BRICK_ITEM"},
            {"NETHER_BRICKS", "NETHER_BRICK"},
            {"NETHER_BRICK_FENCE", "NETHER_FENCE"},
            {"SPAWNER", "MOB_SPAWNER"},
            {"REDSTONE_TORCH", "REDSTONE_TORCH_ON"},
            {"REPEATER", "DIODE"},
            {"COMPARATOR", "REDSTONE_COMPARATOR"},
            {"CARROT", "CARROT_ITEM"},
            {"POTATO", "POTATO_ITEM"},
            {"GLISTERING_MELON_SLICE", "SPECKLED_MELON"},
            {"MELON_SLICE", "MELON"},
            {"MELON", "MELON_BLOCK"},
            {"ENDER_EYE", "EYE_OF_ENDER"},
            {"FIREWORK_ROCKET", "FIREWORK"},
            {"FIREWORK_STAR", "FIREWORK_CHARGE"},
            {"LEAD", "LEASH"},
            {"MAP", "EMPTY_MAP"},
            {"FILLED_MAP", "MAP"},
            {"CARROT_ON_A_STICK", "CARROT_STICK"},
            {"MUSIC_DISC_13", "GOLD_RECORD"},
            {"MUSIC_DISC_CAT", "GREEN_RECORD"},
            {"CHEST_MINECART", "STORAGE_MINECART"},
            {"FURNACE_MINECART", "POWERED_MINECART"},
            {"TNT_MINECART", "EXPLOSIVE_MINECART"},
            {"COMMAND_BLOCK", "COMMAND"},
            {"IRON_HORSE_ARMOR", "IRON_BARDING"},
            {"GOLDEN_HORSE_ARMOR", "GOLD_BARDING"},
            {"DIAMOND_HORSE_ARMOR", "DIAMOND_BARDING"},
            {"STONE_BRICK_STAIRS", "SMOOTH_STAIRS"},
            {"NETHER_QUARTZ_ORE", "QUARTZ_ORE"},
            {"FARMLAND", "SOIL"},
            {"BRICKS", "BRICK"},
            {"BRICK", "CLAY_BRICK"},
            {"BREWING_STAND", "BREWING_STAND_ITEM"},
            {"CAULDRON", "CAULDRON_ITEM"},
            {"FLOWER_POT", "FLOWER_POT_ITEM"},
            {"REDSTONE_LAMP", "REDSTONE_LAMP_OFF"},
            {"PISTON", "PISTON_BASE"},
            {"STICKY_PISTON", "PISTON_STICKY_BASE"},
            {"OAK_PRESSURE_PLATE", "WOOD_PLATE"},
            {"STONE_PRESSURE_PLATE", "STONE_PLATE"},
            {"HEAVY_WEIGHTED_PRESSURE_PLATE", "IRON_PLATE"},
            {"LIGHT_WEIGHTED_PRESSURE_PLATE", "GOLD_PLATE"},
            {"OAK_BUTTON", "WOOD_BUTTON"},
            {"RAIL", "RAILS"},
            {"MYCELIUM", "MYCEL"},
            {"LILY_PAD", "WATER_LILY"},
            {"END_STONE", "ENDER_STONE"},
            {"TERRACOTTA", "HARD_CLAY"},
            {"COBBLESTONE_STAIRS", "COBBLESTONE_STAIRS"},
            {"OAK_STAIRS", "WOOD_STAIRS"},
            {"SUGAR_CANE", "SUGAR_CANE"},
            {"POWERED_RAIL", "POWERED_RAIL"},
        };
        for (String[] r : renames) {
            if (!r[0].equals(r[1])) {
                rows.add(new Row(r[0], r[1], 0));
            }
        }
        return Collections.unmodifiableList(rows);
    }
}
