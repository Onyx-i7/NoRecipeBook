package com.onyxi7.norecipebook;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = NoRecipeBook.MODID, name = NoRecipeBook.NAME, version = NoRecipeBook.VERSION)
public class NoRecipeBook {
    public static final String MODID = "norecipebook";
    public static final String NAME = "NoRecipeBook";
    public static final String VERSION = "1.2.0";
    
    public static Logger logger;
    
    @SidedProxy(clientSide = "com.onyxi7.norecipebook.ClientProxy", serverSide = "com.onyxi7.norecipebook.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        logger.info("[NoRecipeBook] Pre-initialization complete");
    }
    
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
        logger.info("[NoRecipeBook] Initialization complete");
    }
}
