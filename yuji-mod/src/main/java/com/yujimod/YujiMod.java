package com.yujimod;

import com.yujimod.registry.ModEntities;
import com.yujimod.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(YujiMod.MODID)
public class YujiMod {
    public static final String MODID = "yujimod";

    public YujiMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
    }
}
