package net.blzinite.horsectrl.render;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.HorseArmorLayer;
import net.minecraft.client.renderer.entity.layers.HorseMarkingLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ControlledHorseRenderer extends AbstractHorseRenderer<Horse, HorseModel<Horse>> {
    private static final Map<Variant, ResourceLocation> LOCATION_BY_VARIANT = Util.make(Maps.newEnumMap(Variant.class), (v) -> {
        v.put(Variant.WHITE, ResourceLocation.parse("minecraft:textures/entity/horse/horse_white.png"));
        v.put(Variant.CREAMY, ResourceLocation.parse("minecraft:textures/entity/horse/horse_creamy.png"));
        v.put(Variant.CHESTNUT, ResourceLocation.parse("minecraft:textures/entity/horse/horse_chestnut.png"));
        v.put(Variant.BROWN, ResourceLocation.parse("minecraft:textures/entity/horse/horse_brown.png"));
        v.put(Variant.BLACK, ResourceLocation.parse("minecraft:textures/entity/horse/horse_black.png"));
        v.put(Variant.GRAY, ResourceLocation.parse("minecraft:textures/entity/horse/horse_gray.png"));
        v.put(Variant.DARK_BROWN, ResourceLocation.parse("minecraft:textures/entity/horse/horse_darkbrown.png"));
    });
    private float lean = 0;

    public ControlledHorseRenderer(EntityRendererProvider.Context context) {
        super(context, new HorseModel<>(context.bakeLayer(ModelLayers.HORSE)), 1.1F);
        this.addLayer(new HorseMarkingLayer(this));
        this.addLayer(new HorseArmorLayer(this, context.getModelSet()));
    }

    public @NotNull ResourceLocation getTextureLocation(Horse horse) {
        return LOCATION_BY_VARIANT.get(horse.getVariant());
    }

    @Override
    protected void setupRotations(Horse horse, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(horse, poseStack, bob, yBodyRot, partialTick, scale);
        if (horse.getControllingPassenger() instanceof Player rider) {
            if (rider.zza >= 0.0 && horse.getDeltaMovement().horizontalDistanceSqr() > 0) {
                float yawDiff = horse.getYRot() - horse.yHeadRot;
                float leanAngle = Mth.clamp(yawDiff * 2, -5.0f, 5.0f);
                this.lean += (leanAngle - this.lean) * 0.2f;
                poseStack.mulPose(Axis.ZP.rotationDegrees(this.lean));
            }
        }
    }
}
