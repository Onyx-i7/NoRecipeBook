package com.onyxi7.norecipebook.event;

import com.onyxi7.norecipebook.NoRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@SideOnly(Side.CLIENT)
public class RecipeBookEventHandler {
    
    // Cached reflection fields and methods (Lazy Loading)
    private static Field recipeBookField = null;
    private static Field recipesField = null;
    private static Method clearMethod = null;
    private static boolean fieldsInitialized = false;
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.getGui();

        // Only process inventory and crafting screens
        if (gui instanceof GuiInventory || gui instanceof GuiCrafting) {
            
            // 1. Hide the recipe book button (ID 10 in 1.12.2)
            for (GuiButton button : event.getButtonList()) {
                if (button.id == 10) {
                    button.visible = false;
                    button.enabled = false;
                    break; // Early exit
                }
            }
            
            // 2. Clear the GUI's recipe book
            if (gui instanceof GuiContainer) {
                clearGuiRecipeBook((GuiContainer) gui);
            }
            
            // 3. Clear the player's recipe book
            clearPlayerRecipeBook();
        }
    }
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        // Clear recipe book when player joins world
        if (event.getEntity() instanceof EntityPlayer && event.getEntity().world.isRemote) {
            clearPlayerRecipeBook();
        }
    }
    
    /**
     * Clears the recipe book from the GUI container
     */
    private static void clearGuiRecipeBook(GuiContainer gui) {
        try {
            // Initialize cached fields on first run
            if (!fieldsInitialized) {
                initializeFields(gui.getClass());
                fieldsInitialized = true;
            }
            
            if (recipeBookField != null) {
                Object book = recipeBookField.get(gui);
                if (book != null) {
                    clearRecipeBookObject(book);
                }
            }
        } catch (Exception e) {
            NoRecipeBook.logger.error("[NoRecipeBook] Error clearing GUI recipe book", e);
        }
    }
    
    /**
     * Clears the recipe book from the player
     */
    private static void clearPlayerRecipeBook() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.player != null) {
                Object playerRecipeBook = mc.player.getRecipeBook();
                if (playerRecipeBook != null) {
                    clearRecipeBookObject(playerRecipeBook);
                }
            }
        } catch (Exception e) {
            NoRecipeBook.logger.error("[NoRecipeBook] Error clearing player recipe book", e);
        }
    }
    
    /**
     * Clears a recipe book object using cached reflection
     */
    private static void clearRecipeBookObject(Object book) {
        try {
            // Method 1: Try to clear the recipes list
            if (recipesField != null) {
                List<?> recipesList = (List<?>) recipesField.get(book);
                if (recipesList != null && !recipesList.isEmpty()) {
                    recipesList.clear();
                }
            }
            
            // Method 2: Try to call clearRecipes() method if it exists
            if (clearMethod != null) {
                clearMethod.invoke(book);
            }
        } catch (Exception e) {
            NoRecipeBook.logger.error("[NoRecipeBook] Error clearing recipe book object", e);
        }
    }
    
    /**
     * Initializes cached reflection fields
     */
    private static void initializeFields(Class<?> guiClass) {
        try {
            // Find recipeBook field in GUI
            recipeBookField = findField(guiClass, 
                "recipeBook", "field_194399_u", "field_194400_v", "field_193982_w");
            
            if (recipeBookField != null) {
                recipeBookField.setAccessible(true);
                
                // Get the recipe book object to find its fields
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.currentScreen instanceof GuiContainer) {
                    Object book = recipeBookField.get(mc.currentScreen);
                    if (book != null) {
                        // Find recipes field
                        recipesField = findField(book.getClass(), 
                            "recipes", "field_194396_w", "field_193019_k", "field_194199_g");
                        if (recipesField != null) {
                            recipesField.setAccessible(true);
                        }
                        
                        // Find clearRecipes method
                        try {
                            clearMethod = book.getClass().getDeclaredMethod("clearRecipes");
                            clearMethod.setAccessible(true);
                        } catch (NoSuchMethodException ignored) {
                            // Method doesn't exist, that's okay
                        }
                    }
                }
            }
        } catch (Exception e) {
            NoRecipeBook.logger.error("[NoRecipeBook] Error initializing reflection fields", e);
        }
    }
    
    /**
     * Searches for a field with multiple possible names (MCP and SRG)
     */
    private static Field findField(Class<?> clazz, String... names) {
        for (String name : names) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                // Continue to next name
            }
        }
        // Search in parent class
        if (clazz.getSuperclass() != null) {
            return findField(clazz.getSuperclass(), names);
        }
        return null;
    }
}
