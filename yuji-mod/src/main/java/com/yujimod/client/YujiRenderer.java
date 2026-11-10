package com.yujimod.client;

import com.yujimod.YujiMod;
import com.yujimod.entity.YujiEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Charge automatiquement :
 *   geo/entity/yuji_itadori.geo.json
 *   animations/entity/yuji_itadori.animation.json
 *   textures/entity/yuji_itadori.png
 * Le 2e argument (true) fait tourner l'os "head" vers la cible.
 */
public class YujiRenderer extends GeoEntityRenderer<YujiEntity> {
    public YujiRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(new ResourceLocation(YujiMod.MODID, "yuji_itadori"), true));
        this.shadowRadius = 0.5F;
    }
}
