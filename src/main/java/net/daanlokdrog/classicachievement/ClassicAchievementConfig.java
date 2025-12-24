package net.daanlokdrog.classicachievement;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClassicAchievementConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Boolean> OLDER_UI;
    public static final ForgeConfigSpec.ConfigValue<Boolean> HIDE_PAGE;

    static {
        BUILDER.push("General");

        OLDER_UI = BUILDER
                .comment("No arrows, lines will flicker, Zoom will be disabled. (Older achievement UI style)")
                .define("OlderUI", false);

        HIDE_PAGE = BUILDER
                .comment("Hide the page switching button")
                .define("HidePage", false);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
