package com.onyxi7.norecipebook;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = NoRecipeBook.MODID, name = NoRecipeBook.NAME, version = NoRecipeBook.VERSION, updateJSON = "https://raw.githubusercontent.com/Onyx-i7/NoRecipeBook/refs/heads/main/modupdatechecker.json")
public class RecipeRemover {
    public static final String MODID = "norecipebook";
    public static final String NAME = "NoRecipeBook";
    public static final String VERSION = "1.1.0";
    
    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        System.out.println("[NoRecipeBook] Loaded correctly");
    }
}
