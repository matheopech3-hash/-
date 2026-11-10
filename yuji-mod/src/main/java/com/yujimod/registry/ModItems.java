package com.yujimod.registry;

import com.yujimod.YujiMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, YujiMod.MODID);

    // rose cheveux / bleu nuit veste
    public static final RegistryObject<Item> YUJI_SPAWN_EGG = ITEMS.register("yuji_itadori_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.YUJI, 0xE68C8C, 0x1C2540, new Item.Properties()));
}
