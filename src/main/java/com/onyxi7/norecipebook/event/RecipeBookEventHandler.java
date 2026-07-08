package com.onyxi7.norecipebook.event;

import com.onyxi7.norecipebook.norecipebook;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.List;

@Mod.EventBusSubscriber(modid = NoRecipeBook.MODID, value = Side.CLIENT)
public class RecipeBookEventHandler {

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.getGui();

        // Check whether the open screen is the Inventory or the Workbench
        if (gui instanceof GuiInventory || gui instanceof GuiCrafting) {
            
            // 1. SIMPLE AND STABLE METHOD: Hide the book button (ID 10 in 1.12.2)
            for (GuiButton button : event.getButtonList()) {
                if (button.id == 10) {
                    button.visible = false;
                    button.enabled = false;
                }
            }
            
            // 2. ANTILAG METHOD: Try to clear the internal recipe list
            // This prevents the game from processing thousands of mod recipes when opening the inventory
            if (gui instanceof GuiContainer) {
                try {
                    Field recipeBookField = GuiContainer.class.getDeclaredField("recipeBook");
                    recipeBookField.setAccessible(true);
                    Object book = recipeBookField.get(gui);
                    
                    if (book != null) {
                        // Search for the ‘recipes’ field (it may have different names depending on the mapping)
                        Field recipesField = findField(book.getClass(), "recipes", "field_194396_w", "field_193019_k");
                        if (recipesField != null) {
                            recipesField.setAccessible(true);
                            List<?> recipesList = (List<?>) recipesField.get(book);
                            if (recipesList != null) {
                                recipesList.clear();
                            }
                        }
                    }
                } catch (Exception e) {
                    // If it fails, the game doesn't crash it just logs it
                    System.err.println("[RecipeRemover] The internal recipes could not be cleared, but the button is hidden");
                }
            }
        }
    }
    
    // Auxiliary method for searching fields with multiple possible names (MCP and SRG)
    private static Field findField(Class<?> clazz, String... names) {
        for (String name : names) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                // Continue with the next name
            }
        }
        return null;
    }
}
