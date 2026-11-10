package com.yujimod.registry;

import com.yujimod.YujiMod;
import com.yujimod.entity.YujiEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, YujiMod.MODID);

    public static final RegistryObject<EntityType<YujiEntity>> YUJI = ENTITIES.register("yuji_itadori",
            () -> EntityType.Builder.of(YujiEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.85F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(YujiMod.MODID, "yuji_itadori").toString()));
}
