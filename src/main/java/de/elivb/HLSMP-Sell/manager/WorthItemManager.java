package de.elivb.donutSell.manager;

import de.elivb.donutSell.utils.CurrencyFormatter;
import java.util.HashMap;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.PotionMeta;

public class WorthItemManager {
   private final PriceManager priceManager;
   private final CurrencyFormatter currencyFormatter;
   private final LangManager langManager;
   private final SellManager sellManager;
   private final WorthManager worthManager;
   private boolean shulkerSupportEnabled;

   public WorthItemManager(PriceManager priceManager, CurrencyFormatter currencyFormatter, LangManager langManager, SellManager sellManager) {
      this.priceManager = priceManager;
      this.currencyFormatter = currencyFormatter;
      this.langManager = langManager;
      this.sellManager = sellManager;
      this.shulkerSupportEnabled = sellManager.isShulkerSupportEnabled();
      this.worthManager = sellManager.getPlugin().getWorthManager();
   }

   public void showItemWorth(Player player, Material material) {
      Double basePrice = this.priceManager.getPrice(material);
      if (basePrice != null && basePrice > (double)0.0F) {
         double finalPrice = basePrice;
         LevelManager levelManager = this.sellManager.getPlugin().getLevelManager();
         if (levelManager != null) {
            String category = levelManager.getCategory(material);
            if (category != null) {
               finalPrice = levelManager.calculatePriceWithMultiplier(player, category, basePrice);
            }
         }

         String formattedPrice = this.currencyFormatter.format(finalPrice);
         String itemName = this.formatMaterialName(material);
         HashMap<String, String> placeholders = new HashMap();
         placeholders.put("item", itemName);
         placeholders.put("price", formattedPrice);
         String message = this.langManager.getMessageWithoutPrefix("action-bars.item-worth", placeholders);
         player.sendActionBar(message);
         player.playSound(player.getLocation(), this.sellManager.getSellSuccessSound(), 1.0F, 1.0F);
      } else {
         String itemName = this.formatMaterialName(material);
         HashMap<String, String> placeholders = new HashMap();
         placeholders.put("item", itemName);
         String message = this.langManager.getMessageWithoutPrefix("action-bars.item-not-sellable", placeholders);
         player.sendActionBar(message);
         player.playSound(player.getLocation(), this.sellManager.getNoSellableItemsSound(), 1.0F, 1.0F);
      }

   }

   public void showHeldItemWorth(Player player) {
      ItemStack item = player.getInventory().getItemInMainHand();
      if (item != null && item.getType() != Material.AIR) {
         this.updateItemWorthLoreInstant(player, item);
         if (this.shulkerSupportEnabled && this.isShulkerBox(item)) {
            this.showShulkerBoxWorth(player, item);
         } else {
            this.showItemStackWorth(player, item);
         }
      } else {
         String message = this.langManager.getMessageWithoutPrefix("action-bars.no-item-in-hand");
         player.sendActionBar(message);
         player.playSound(player.getLocation(), this.sellManager.getNoSellableItemsSound(), 1.0F, 1.0F);
      }
   }

   private void updateItemWorthLoreInstant(Player player, ItemStack item) {
      if (this.worthManager != null && this.worthManager.isEnabled()) {
         this.worthManager.forceUpdatePlayer(player);
      }

   }

   public void showShulkerBoxWorth(Player player, ItemStack shulkerBox) {
      if (this.shulkerSupportEnabled && this.isShulkerBox(shulkerBox)) {
         double totalValue = this.calculateTotalShulkerBoxValue(player, shulkerBox);
         int itemCount = this.countItemsInShulkerBox(shulkerBox);
         String shulkerName = this.getItemDisplayName(shulkerBox);
         if (totalValue > (double)0.0F) {
            String formattedValue = this.currencyFormatter.format(totalValue);
            HashMap<String, String> placeholders = new HashMap();
            placeholders.put("item", shulkerName);
            placeholders.put("price", formattedValue);
            placeholders.put("count", String.valueOf(itemCount));
            String message;
            if (itemCount > 0) {
               message = this.langManager.getMessageWithoutPrefix("action-bars.shulker-worth", placeholders);
               if (message == null || message.isEmpty()) {
                  message = "&f" + shulkerName + " is &#0bf52b" + formattedValue + " &fworth &7(" + itemCount + " items)";
               }
            } else {
               message = this.langManager.getMessageWithoutPrefix("action-bars.shulker-empty", placeholders);
               if (message == null || message.isEmpty()) {
                  message = "&f" + shulkerName + " is &#0bf52b" + formattedValue + " &fworth &7(empty)";
               }
            }

            player.sendActionBar(message);
            player.playSound(player.getLocation(), this.sellManager.getSellSuccessSound(), 1.0F, 1.0F);
         } else {
            this.sendEmptyShulkerMessage(player, shulkerName);
         }

      } else {
         String message = this.langManager.getMessageWithoutPrefix("action-bars.invalid-item");
         player.sendActionBar(message);
         player.playSound(player.getLocation(), this.sellManager.getNoSellableItemsSound(), 1.0F, 1.0F);
      }
   }

   public double calculateTotalShulkerBoxValue(Player player, ItemStack shulkerBox) {
      if (this.shulkerSupportEnabled && this.isShulkerBox(shulkerBox)) {
         double totalValue = (double)0.0F;
         LevelManager levelManager = this.sellManager.getPlugin().getLevelManager();

         try {
            Double shulkerBoxBasePrice = this.getItemPrice(shulkerBox);
            if (shulkerBoxBasePrice != null && shulkerBoxBasePrice > (double)0.0F) {
               double finalPrice = shulkerBoxBasePrice;
               if (levelManager != null) {
                  String category = levelManager.getCategoryForItem(shulkerBox);
                  if (category != null) {
                     finalPrice = levelManager.calculatePriceWithMultiplier(player, category, shulkerBoxBasePrice);
                  }
               }

               totalValue += finalPrice;
            }

            BlockStateMeta meta = (BlockStateMeta)shulkerBox.getItemMeta();
            if (meta == null) {
               return totalValue;
            } else {
               BlockState state = meta.getBlockState();
               if (!(state instanceof ShulkerBox)) {
                  return totalValue;
               } else {
                  ShulkerBox shulker = (ShulkerBox)state;
                  Inventory shulkerInventory = shulker.getInventory();

                  for(ItemStack item : shulkerInventory.getContents()) {
                     if (item != null && item.getType() != Material.AIR) {
                        Double basePrice = this.getItemPrice(item);
                        if (basePrice != null && basePrice > (double)0.0F) {
                           double finalPrice = basePrice;
                           if (levelManager != null) {
                              String category = levelManager.getCategoryForItem(item);
                              if (category != null) {
                                 finalPrice = levelManager.calculatePriceWithMultiplier(player, category, basePrice);
                              }
                           }

                           totalValue += finalPrice * (double)item.getAmount();
                        }
                     }
                  }

                  return totalValue;
               }
            }
         } catch (Exception var19) {
            return totalValue;
         }
      } else {
         return (double)0.0F;
      }
   }

   public void showItemStackWorth(Player player, ItemStack item) {
      if (item != null && item.getType() != Material.AIR) {
         Double basePrice = this.getItemPrice(item);
         if (basePrice != null && basePrice > (double)0.0F) {
            double finalPrice = basePrice;
            LevelManager levelManager = this.sellManager.getPlugin().getLevelManager();
            if (levelManager != null) {
               String category = levelManager.getCategoryForItem(item);
               if (category != null) {
                  finalPrice = levelManager.calculatePriceWithMultiplier(player, category, basePrice);
               }
            }

            String formattedSinglePrice = this.currencyFormatter.format(finalPrice);
            String itemName = this.getItemDisplayName(item);
            double totalValue = finalPrice * (double)item.getAmount();
            String totalFormatted = this.currencyFormatter.format(totalValue);
            HashMap<String, String> placeholders = new HashMap();
            placeholders.put("item", itemName);
            placeholders.put("price", formattedSinglePrice);
            String message;
            if (item.getAmount() > 1) {
               HashMap<String, String> stackPlaceholders = new HashMap();
               stackPlaceholders.put("item", itemName);
               stackPlaceholders.put("amount", String.valueOf(item.getAmount()));
               stackPlaceholders.put("total", totalFormatted);
               stackPlaceholders.put("single", formattedSinglePrice);
               message = this.langManager.getMessageWithoutPrefix("action-bars.stack-worth", stackPlaceholders);
               if (message == null || message.isEmpty()) {
                  message = "&f" + itemName + " &7(x" + item.getAmount() + ") is &#0bf52b" + totalFormatted + " &fworth &7(" + formattedSinglePrice + " each)";
               }
            } else {
               message = this.langManager.getMessageWithoutPrefix("action-bars.item-worth", placeholders);
               if (message == null || message.isEmpty()) {
                  message = "&f" + itemName + " is &#0bf52b" + formattedSinglePrice + " &fworth";
               }
            }

            player.sendActionBar(message);
            player.playSound(player.getLocation(), this.sellManager.getSellSuccessSound(), 1.0F, 1.0F);
         } else {
            String itemName = this.getItemDisplayName(item);
            HashMap<String, String> placeholders = new HashMap();
            placeholders.put("item", itemName);
            String message = this.langManager.getMessageWithoutPrefix("action-bars.item-not-sellable", placeholders);
            player.sendActionBar(message);
            player.playSound(player.getLocation(), this.sellManager.getNoSellableItemsSound(), 1.0F, 1.0F);
         }

      } else {
         String message = this.langManager.getMessageWithoutPrefix("action-bars.invalid-item");
         player.sendActionBar(message);
         player.playSound(player.getLocation(), this.sellManager.getNoSellableItemsSound(), 1.0F, 1.0F);
      }
   }

   public double calculateStackValue(Player player, ItemStack item) {
      if (item != null && item.getType() != Material.AIR) {
         Double basePrice = this.getItemPrice(item);
         if (basePrice != null && basePrice > (double)0.0F) {
            double finalPrice = basePrice;
            LevelManager levelManager = this.sellManager.getPlugin().getLevelManager();
            if (levelManager != null) {
               String category = levelManager.getCategoryForItem(item);
               if (category != null) {
                  finalPrice = levelManager.calculatePriceWithMultiplier(player, category, basePrice);
               }
            }

            return finalPrice * (double)item.getAmount();
         } else {
            return (double)0.0F;
         }
      } else {
         return (double)0.0F;
      }
   }

   public Double getSingleItemPrice(Player player, ItemStack item) {
      if (item != null && item.getType() != Material.AIR) {
         Double basePrice = this.getItemPrice(item);
         if (basePrice != null && basePrice > (double)0.0F) {
            double finalPrice = basePrice;
            LevelManager levelManager = this.sellManager.getPlugin().getLevelManager();
            if (levelManager != null) {
               String category = levelManager.getCategoryForItem(item);
               if (category != null) {
                  finalPrice = levelManager.calculatePriceWithMultiplier(player, category, basePrice);
               }
            }

            return finalPrice;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public Double getSingleItemPrice(ItemStack item) {
      return item != null && item.getType() != Material.AIR ? this.getItemPrice(item) : null;
   }

   public Double getTotalItemPrice(Player player, ItemStack item) {
      Double singlePrice = this.getSingleItemPrice(player, item);
      return singlePrice != null && singlePrice > (double)0.0F ? singlePrice * (double)item.getAmount() : null;
   }

   public int countItemsInShulkerBox(ItemStack shulkerBox) {
      if (this.shulkerSupportEnabled && this.isShulkerBox(shulkerBox)) {
         int itemCount = 0;

         try {
            BlockStateMeta meta = (BlockStateMeta)shulkerBox.getItemMeta();
            if (meta == null) {
               return itemCount;
            } else {
               BlockState state = meta.getBlockState();
               if (!(state instanceof ShulkerBox)) {
                  return itemCount;
               } else {
                  ShulkerBox shulker = (ShulkerBox)state;
                  Inventory shulkerInventory = shulker.getInventory();

                  for(ItemStack item : shulkerInventory.getContents()) {
                     if (item != null && item.getType() != Material.AIR) {
                        itemCount += item.getAmount();
                     }
                  }

                  return itemCount;
               }
            }
         } catch (Exception var11) {
            return itemCount;
         }
      } else {
         return 0;
      }
   }

   private String formatMaterialName(Material material) {
      String name = material.name().toLowerCase().replace("_", " ");
      String[] words = name.split(" ");
      StringBuilder formatted = new StringBuilder();

      for(String word : words) {
         if (!word.isEmpty()) {
            formatted.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
         }
      }

      return formatted.toString().trim();
   }

   private String getItemDisplayName(ItemStack item) {
      return item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : this.formatMaterialName(item.getType());
   }

   private boolean isPotion(ItemStack item) {
      return item != null && (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION) && item.hasItemMeta() && item.getItemMeta() instanceof PotionMeta;
   }

   private boolean isShulkerBox(ItemStack item) {
      if (item != null && item.hasItemMeta()) {
         Material type = item.getType();
         return type == Material.SHULKER_BOX || type == Material.WHITE_SHULKER_BOX || type == Material.ORANGE_SHULKER_BOX || type == Material.MAGENTA_SHULKER_BOX || type == Material.LIGHT_BLUE_SHULKER_BOX || type == Material.YELLOW_SHULKER_BOX || type == Material.LIME_SHULKER_BOX || type == Material.PINK_SHULKER_BOX || type == Material.GRAY_SHULKER_BOX || type == Material.LIGHT_GRAY_SHULKER_BOX || type == Material.CYAN_SHULKER_BOX || type == Material.PURPLE_SHULKER_BOX || type == Material.BLUE_SHULKER_BOX || type == Material.BROWN_SHULKER_BOX || type == Material.GREEN_SHULKER_BOX || type == Material.RED_SHULKER_BOX || type == Material.BLACK_SHULKER_BOX;
      } else {
         return false;
      }
   }

   private void sendEmptyShulkerMessage(Player player, String shulkerName) {
      HashMap<String, String> placeholders = new HashMap();
      placeholders.put("item", shulkerName);
      String message = this.langManager.getMessageWithoutPrefix("action-bars.item-not-sellable", placeholders);
      if (message == null || message.isEmpty()) {
         message = "&#FF0000" + shulkerName + " this Item can't sell";
      }

      player.sendActionBar(message);
      player.playSound(player.getLocation(), this.sellManager.getNoSellableItemsSound(), 1.0F, 1.0F);
   }

   public Double getItemPrice(Material material) {
      return this.priceManager.getPrice(material);
   }

   public Double getItemPrice(ItemStack item) {
      return item != null && item.getType() != Material.AIR ? this.priceManager.getPriceWithDurability(item) : null;
   }

   public boolean isShulkerSupportEnabled() {
      return this.shulkerSupportEnabled;
   }

   public void onInventoryItemsChanged(Player player) {
      if (this.worthManager != null) {
         this.worthManager.onInventoryChanged(player);
      }

   }
}
