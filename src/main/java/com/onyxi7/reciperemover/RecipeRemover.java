package com.onyxi7.reciperemover;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = RecipeRemover.MODID, name = RecipeRemover.NAME, version = RecipeRemover.VERSION, updateJSON = "")
public class RecipeRemover {
    public static final String MODID = "reciperemover";
    public static final String NAME = "Recipe Remover";
    public static final String VERSION = "1.0.0";
    
    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        System.out.println("[RecipeRemover] Loaded correctly");
    }
}