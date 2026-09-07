package net.blzinite.horsectrl.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@EventBusSubscriber
public class HorseCtrlConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue SPUR_RECHARGE_RATE;
    private static final ModConfigSpec.IntValue SPRINT_DECAY_RATE;
    private static final ModConfigSpec.BooleanValue ALLOW_BAREBACK;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> MOUNT_LIST;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("Horse Cruise Control Config");
        ALLOW_BAREBACK = BUILDER.comment("Allow mounts to be controlled without a saddle").define("allow_bareback", true);
        SPUR_RECHARGE_RATE = BUILDER.comment("How many ticks should it take to regain a spur").defineInRange("spur_recharge", 250, 0, Integer.MAX_VALUE);
        SPRINT_DECAY_RATE = BUILDER.comment("How many ticks should a boost last").defineInRange("sprint_decay", 180, 0, Integer.MAX_VALUE);
        MOUNT_LIST = BUILDER.comment("What mobs should use the new control scheme, must be child of AbstractHorse")
                .defineListAllowEmpty("mount_list",
                        List.of(
                                "minecraft:horse",
                                "minecraft:donkey",
                                "minecraft:mule",
                                "minecraft:skeleton_horse",
                                "minecraft:zombie_horse"
                        ),
                        () -> "minecraft:horse", HorseCtrlConfig::validateEntity);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private static boolean validateEntity(final Object obj) {
        return obj instanceof final String entityType && BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(entityType));
    }

    public static final float rearSpeedThreshold = 2;
    public static boolean allowBareback;
    public static float spurRechargeRate;
    public static float sprintDecayRate;
    private static Set<EntityType<?>> mountList;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent configEvent) {
        allowBareback = ALLOW_BAREBACK.get();
        spurRechargeRate = 1f/SPUR_RECHARGE_RATE.get();
        sprintDecayRate = 1f/SPRINT_DECAY_RATE.get();
        mountList = MOUNT_LIST.get().stream()
            .map(entityType -> BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(entityType)))
            .collect(Collectors.toSet());
    }


    public static boolean isControlledMount(Object obj) {
        if (obj instanceof AbstractHorse horse) {
            return mountList.contains(horse.getType());
        }
        return false;
    }
}
