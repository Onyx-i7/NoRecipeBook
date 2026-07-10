package com.onyxi7.norecipebook.event;

import com.onyxi7.norecipebook.NoRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.gui.recipebook.GuiRecipeBook;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
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
    
    // Track if we already checked for the stuck recipe book on this world session
    private static boolean checkedStuckBook = false;
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onGuiOpen(GuiOpenEvent event) {
        // Prevent the recipe book from opening in the future
        if (event.getGui() instanceof GuiRecipeBook) {
            event.setCanceled(true);
            NoRecipeBook.logger.debug("[NoRecipeBook] Blocked GuiRecipeBook from opening");
        }
    }
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // Only run on the end phase to avoid double execution
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        
        // Check if the player is in a world
        if (mc.player == null || mc.world == null) {
            checkedStuckBook = false;
            return;
        }
        
        // Run only once per world session to close any stuck recipe book
        if (!checkedStuckBook) {
            checkedStuckBook = true;
            
            // If the current screen is the recipe book, close it immediately
            if (mc.currentScreen instanceof GuiRecipeBook) {
                mc.displayGuiScreen(null);
                NoRecipeBook.logger.info("[NoRecipeBook] Closed stuck GuiRecipeBook on world join");
            }
            
            // Also check if we're in an inventory/crafting screen with the book button visible
            if (mc.currentScreen instanceof GuiInventory || mc.currentScreen instanceof GuiCrafting) {
                closeRecipeBookButton(mc.currentScreen);
            }
        }
    }
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.getGui();

        // Only process inventory and crafting screens
        if (gui instanceof GuiInventory || gui instanceof GuiCrafting) {
            
            // 1. Hide the recipe book button (ID 10 in 1.12.2)
            closeRecipeBookButton(gui);
            
            // 2. Clear the GUI's recipe book (if accessible)
            if (gui instanceof GuiContainer) {
                clearGuiRecipeBook((GuiContainer) gui);
            }
            
            // 3. Clear the player's recipe book (main source of lag)
            clearPlayerRecipeBook();
        }
    }
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        // Clear recipe book when player joins world
        if (event.getEntity() instanceof EntityPlayer && event.getEntity().world.isRemote) {
            clearPlayerRecipeBook();
            checkedStuckBook = false; // Reset flag so we check again on next world join
        }
    }
    
    /**
     * Hides the recipe book button in the given GUI screen
     */
    private static void closeRecipeBookButton(GuiScreen gui) {
        if (gui == null) return;
        
        try {
            // Use reflection to get the button list since event.getButtonList() is only available in events
            Field buttonListField = GuiScreen.class.getDeclaredField("buttonList");
            buttonListField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<GuiButton> buttons = (List<GuiButton>) buttonListField.get(gui);
            
            if (buttons != null) {
                for (GuiButton button : buttons) {
                    if (button.id == 10) {
                        button.visible = false;
                        button.enabled = false;
                        break; // Early exit - no need to check other buttons
                    }
                }
            }
        } catch (Exception e) {
            NoRecipeBook.logger.debug("[NoRecipeBook] Could not hide recipe book button: " + e.getMessage());
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
     * Clears the recipe book from the player (main lag source)
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
     * Initializes cached reflection fields (runs only once)
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
