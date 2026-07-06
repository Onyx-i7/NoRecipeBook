package com.onyxi7.reciperemover.event;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.List;

@Mod.EventBusSubscriber(modid = RecipeRemover.MODID, value = Side.CLIENT)
public class RecipeBookEventHandler {

    private static Field cachedRecipeBookField = null;
    private static Field cachedVisibleField = null;
    private static Field cachedRecipesField = null;
    private static boolean fieldsInitialized = false;

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof GuiContainer)) return;
        
        GuiContainer gui = (GuiContainer) event.getGui();
        
        try {
            if (!fieldsInitialized) {
                initializeFields(gui);
                fieldsInitialized = true;
            }
            
            if (cachedRecipeBookField != null) {
                Object book = cachedRecipeBookField.get(gui);
                
                if (book != null) {
                    if (cachedVisibleField != null) {
                        cachedVisibleField.setBoolean(book, false);
                    }
                    
                    if (cachedRecipesField != null) {
                        List<?> recipesList = (List<?>) cachedRecipesField.get(book);
                        if (recipesList != null) {
                            recipesList.clear();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[RecipeRemover] An error occurred while processing RecipeBook, but the game is still running.");
            e.printStackTrace();
        }
    }
    
    private static void initializeFields(GuiContainer gui) {
        try {
            cachedRecipeBookField = findField(gui.getClass(), "recipeBook", "field_194399_u");
            
            if (cachedRecipeBookField != null) {
                cachedRecipeBookField.setAccessible(true);
                Object book = cachedRecipeBookField.get(gui);
                
                if (book != null) {
                    cachedVisibleField = findField(book.getClass(), "visible", "field_193018_j", "field_194395_v");
                    if (cachedVisibleField != null) {
                        cachedVisibleField.setAccessible(true);
                    }
                    
                    cachedRecipesField = findField(book.getClass(), "recipes", "field_194396_w", "field_193019_k");
                    if (cachedRecipesField != null) {
                        cachedRecipesField.setAccessible(true);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[RecipeRemover] The RecipeBook fields could not be initialized.");
        }
    }
    
    private static Field findField(Class<?> clazz, String... names) {
        for (String name : names) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
            }
        }
        return null;
    }
}
