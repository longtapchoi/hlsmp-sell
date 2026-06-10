package de.elivb.donutSell.manager;

import de.elivb.donutSell.HexColorCode;
import de.elivb.donutSell.Sell;
import de.elivb.donutSell.utils.CurrencyFormatter;
import java.io.File;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;

public class SellAxeManager implements Listener {
   private final Sell plugin;
   private final PriceManager priceManager;
   private final CurrencyFormatter currencyFormatter;
   private final LangManager langManager;
   private final PlayerDataManager playerDataManager;
   private final boolean isFolia;
   private FileConfiguration sellAxeConfig;
   private final File sellAxeFile;
   private String displayName;
   private List<String> lore;
   private int timeInDays;
   private int customModelData;
   private Material material;
   private boolean soundEnabled;
   private Sound soundType;
   private boolean particleEnabled;
   private Particle particleType;
   private boolean glowEnabled;
   private boolean shulkerSupportEnabled;
   private boolean enchantmentsEnabled;
   private List<String> enchantmentList;
   private final NamespacedKey expirationKey;
   private final Map<String, Sound> soundConversionTable;

   public SellAxeManager(Sell plugin, PriceManager priceManager, CurrencyFormatter currencyFormatter, LangManager langManager, PlayerDataManager playerDataManager, boolean isFolia) {
      this.plugin = plugin;
      this.priceManager = priceManager;
      this.currencyFormatter = currencyFormatter;
      this.langManager = langManager;
      this.playerDataManager = playerDataManager;
      this.isFolia = isFolia;
      this.expirationKey = new NamespacedKey(plugin, "sellaxe_expiration");
      this.sellAxeFile = new File(plugin.getDataFolder(), "sellaxe.yml");
      this.soundConversionTable = this.createSoundConversionTable();
      this.shulkerSupportEnabled = plugin.isShulkerSupportEnabled();
      if (!this.sellAxeFile.exists()) {
         this.createDefaultConfig();
      }

      this.sellAxeConfig = YamlConfiguration.loadConfiguration(this.sellAxeFile);
      this.loadConfig();
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   public void loadConfig() {
      this.displayName = this.sellAxeConfig.getString("displayname", "&#A20AD6ᴀᴍᴇᴛʜʏꜱᴛ ꜱᴇʟʟ ᴀxᴇ");
      this.lore = this.sellAxeConfig.getStringList("lore");
      String timeString = this.sellAxeConfig.getString("time", "3d");
      this.timeInDays = this.parseTimeToDays(timeString);
      this.customModelData = this.sellAxeConfig.getInt("custom-model-data", 1000);
      String materialName = this.sellAxeConfig.getString("material", "NETHERITE_AXE");
      this.material = Material.getMaterial(materialName);
      if (this.material == null) {
         this.material = Material.NETHERITE_PICKAXE;
      }

      this.soundEnabled = this.sellAxeConfig.getBoolean("sound.enabled", true);
      String soundName = this.sellAxeConfig.getString("sound.type", "ENTITY_EXPERIENCE_ORB_PICKUP");
      this.soundType = this.getSoundCompat(soundName, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
      this.particleEnabled = this.sellAxeConfig.getBoolean("particle.enabled", true);
      String particleName = this.sellAxeConfig.getString("particle.type", "CLOUD");
      this.particleType = this.matchParticleOrDefault(particleName, Particle.CLOUD);
      this.glowEnabled = this.sellAxeConfig.getBoolean("glow", true);
      this.enchantmentsEnabled = this.sellAxeConfig.getBoolean("enchantments.enabled", true);
      this.enchantmentList = this.sellAxeConfig.getStringList("enchantments.list");
      this.shulkerSupportEnabled = this.plugin.isShulkerSupportEnabled();
   }

   private int parseTimeToDays(String timeString) {
      if (timeString != null && !timeString.isEmpty()) {
         try {
            if (timeString.toLowerCase().endsWith("d")) {
               return Integer.parseInt(timeString.substring(0, timeString.length() - 1));
            } else if (timeString.toLowerCase().endsWith("h")) {
               int hours = Integer.parseInt(timeString.substring(0, timeString.length() - 1));
               return (int)Math.ceil((double)hours / (double)24.0F);
            } else if (timeString.toLowerCase().endsWith("m")) {
               int minutes = Integer.parseInt(timeString.substring(0, timeString.length() - 1));
               return (int)Math.ceil((double)minutes / (double)1440.0F);
            } else {
               return Integer.parseInt(timeString);
            }
         } catch (NumberFormatException var3) {
            return 7;
         }
      } else {
         return 7;
      }
   }

   private String formatRemainingTime(long secondsRemaining) {
      if (secondsRemaining <= 0L) {
         return "&#FF0000Eliminated";
      } else {
         long days = secondsRemaining / 86400L;
         long hours = secondsRemaining % 86400L / 3600L;
         long minutes = secondsRemaining % 3600L / 60L;
         if (days > 0L) {
            return String.format("%dd %dh %dm", days, hours, minutes);
         } else {
            return hours > 0L ? String.format("%dh %dm", hours, minutes) : String.format("%dm", minutes);
         }
      }
   }

   private void createDefaultConfig() {
      try {
         if (!this.plugin.getDataFolder().exists()) {
            boolean ok = this.plugin.getDataFolder().mkdirs();
            if (!ok) {
            }
         }

         this.plugin.saveResource("sellaxe.yml", false);
      } catch (Exception var2) {
      }

   }

   public ItemStack createSellAxe() {
      ItemStack sellAxe = new ItemStack(this.material);
      ItemMeta meta = sellAxe.getItemMeta();
      if (meta == null) {
         return sellAxe;
      } else {
         Instant expirationDate = Instant.now().plus((long)this.timeInDays, ChronoUnit.DAYS);
         long expirationTimestamp = expirationDate.getEpochSecond();
         String translatedDisplayName = HexColorCode.translateAllColorCodes(this.displayName);
         meta.setDisplayName(translatedDisplayName);
         List<String> formattedLore = new ArrayList();
         long secondsRemaining = expirationTimestamp - Instant.now().getEpochSecond();
         String timeDisplay = this.formatRemainingTime(secondsRemaining);

         for(String line : this.lore) {
            String processedLine = line.replace("%time%", timeDisplay);
            String translatedLine = HexColorCode.translateAllColorCodes(processedLine);
            formattedLore.add(translatedLine);
         }

         meta.setLore(formattedLore);

         try {
            meta.setCustomModelData(this.customModelData);
         } catch (Throwable var16) {
         }

         if (this.enchantmentsEnabled && this.enchantmentList != null && !this.enchantmentList.isEmpty()) {
            this.applyEnchantments(meta);
            if (meta.getItemFlags().contains(ItemFlag.HIDE_ENCHANTS)) {
               meta.removeItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
            }
         } else if (this.glowEnabled) {
            try {
               meta.addEnchant(Enchantment.LURE, 1, true);
               meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
            } catch (Exception var15) {
            }
         }

         meta.getPersistentDataContainer().set(this.expirationKey, PersistentDataType.LONG, expirationTimestamp);
         sellAxe.setItemMeta(meta);
         return sellAxe;
      }
   }

   private void applyEnchantments(ItemMeta meta) {
      if (meta != null && this.enchantmentList != null) {
         for(String enchantEntry : this.enchantmentList) {
            try {
               String[] parts = enchantEntry.split(":");
               if (parts.length == 2) {
                  String enchantName = parts[0].trim();
                  int level = Integer.parseInt(parts[1].trim());
                  Enchantment enchant = this.getEnchantment(enchantName);
                  if (enchant != null) {
                     meta.addEnchant(enchant, level, true);
                  }
               }
            } catch (Exception var8) {
            }
         }

      }
   }

   private Enchantment getEnchantment(String name) {
      try {
         return Enchantment.getByName(name.toUpperCase());
      } catch (Exception var5) {
         try {
            NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
            return Enchantment.getByKey(key);
         } catch (Exception var4) {
            return null;
         }
      }
   }

   public boolean isSellAxe(ItemStack item) {
      if (item != null && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return false;
         } else {
            boolean hasExpiration = meta.getPersistentDataContainer().has(this.expirationKey, PersistentDataType.LONG);
            boolean customMatches = true;

            try {
               if (meta.hasCustomModelData()) {
                  Integer custom = meta.getCustomModelData();
                  customMatches = custom == this.customModelData;
               }
            } catch (AbstractMethodError | NoSuchMethodError var6) {
            }

            return hasExpiration && customMatches;
         }
      } else {
         return false;
      }
   }

   private boolean isExpired(ItemStack sellAxe) {
      if (!this.isSellAxe(sellAxe)) {
         return true;
      } else {
         ItemMeta meta = sellAxe.getItemMeta();
         if (meta == null) {
            return true;
         } else {
            Long expirationTimestamp = (Long)meta.getPersistentDataContainer().get(this.expirationKey, PersistentDataType.LONG);
            if (expirationTimestamp == null) {
               return true;
            } else {
               return Instant.now().getEpochSecond() > expirationTimestamp;
            }
         }
      }
   }

   @EventHandler
   public void onPlayerInteract(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      ItemStack item = event.getItem();
      if (item != null && this.isSellAxe(item)) {
         this.updateSellAxeLore(item);
         if (this.isExpired(item)) {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
            player.getInventory().removeItem(new ItemStack[]{item});
            player.sendMessage(this.langManager.getMessage("messages.sellaxe-broken"));
         } else {
            Action action = event.getAction();
            if (action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK) {
               Block clickedBlock = event.getClickedBlock();
               if (clickedBlock != null && clickedBlock.getState() instanceof Container) {
                  event.setCancelled(true);
                  Container container = (Container)clickedBlock.getState();
                  this.sellContainerContents(player, container, item);
               }
            }
         }
      }
   }

   public void checkPlayerInventory(Player player) {
      boolean inventoryChanged = false;

      for(int i = 0; i < 36; ++i) {
         ItemStack item = player.getInventory().getItem(i);
         if (item != null && this.isSellAxe(item)) {
            this.updateSellAxeLore(item);
            if (this.isExpired(item)) {
               player.getInventory().setItem(i, (ItemStack)null);
               inventoryChanged = true;
               this.notifyExpiration(player, item);
            }
         }
      }

      if (inventoryChanged) {
         player.updateInventory();
      }

   }

   private void notifyExpiration(Player player, ItemStack expiredAxe) {
      player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
      player.sendMessage(this.langManager.getMessage("messages.sellaxe-broken"));

      try {
         player.sendActionBar(this.langManager.getMessage("action-bars.sellaxe-broken"));
      } catch (Exception var4) {
      }

   }

   private void sellContainerContents(Player player, Container container, ItemStack sellAxe) {
      this.updateSellAxeLore(sellAxe);
      if (this.isExpired(sellAxe)) {
         player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
         player.getInventory().removeItem(new ItemStack[]{sellAxe});
         player.sendMessage(this.langManager.getMessage("messages.sellaxe-broken"));
      } else {
         double totalMoney = (double)0.0F;
         int totalItems = 0;
         Map<String, Integer> soldItemsMap = new HashMap();
         Map<String, Map<String, Integer>> shulkerContentsMap = new HashMap();
         Map<String, Double> categoryEarnings = new HashMap();
         LevelManager levelManager = this.plugin.getLevelManager();

         for(ItemStack item : container.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
               if (this.shulkerSupportEnabled && this.isShulkerBox(item)) {
                  double shulkerValue = (double)0.0F;
                  int itemCount = 0;
                  Map<String, Double> boxCategoryEarnings = new HashMap();

                  try {
                     BlockStateMeta meta = (BlockStateMeta)item.getItemMeta();
                     if (meta != null) {
                        BlockState state = meta.getBlockState();
                        if (state instanceof ShulkerBox) {
                           ShulkerBox shulker = (ShulkerBox)state;
                           Inventory shulkerInventory = shulker.getInventory();
                           if (shulkerInventory != null) {
                              for(ItemStack shulkerItem : shulkerInventory.getContents()) {
                                 if (shulkerItem != null && shulkerItem.getType() != Material.AIR) {
                                    Double basePrice = this.getItemPrice(shulkerItem);
                                    if (basePrice != null && basePrice > (double)0.0F) {
                                       int amount = shulkerItem.getAmount();
                                       double finalPrice = basePrice;
                                       String category = null;
                                       if (levelManager != null) {
                                          category = levelManager.getCategoryForItem(shulkerItem);
                                          if (category != null) {
                                             finalPrice = levelManager.calculatePriceWithMultiplier(player, category, basePrice);
                                             boxCategoryEarnings.merge(category, finalPrice * (double)amount, Double::sum);
                                          }
                                       }

                                       double itemTotal = finalPrice * (double)amount;
                                       shulkerValue += itemTotal;
                                       itemCount += amount;
                                       String itemKey = shulkerItem.getType().name();
                                       soldItemsMap.merge(itemKey, amount, Integer::sum);
                                    }
                                 }
                              }
                           }
                        }
                     }

                     Double shulkerBoxPrice = this.getItemPrice(item);
                     if (shulkerBoxPrice != null && shulkerBoxPrice > (double)0.0F) {
                        shulkerValue += shulkerBoxPrice;
                        itemCount += item.getAmount();
                        String shulkerType = item.getType().name();
                        soldItemsMap.merge(shulkerType, item.getAmount(), Integer::sum);
                     }
                  } catch (Exception var35) {
                  }

                  if (shulkerValue > (double)0.0F) {
                     totalMoney += shulkerValue;
                     totalItems += itemCount;

                     for(Map.Entry<String, Double> entry : boxCategoryEarnings.entrySet()) {
                        categoryEarnings.merge((String)entry.getKey(), (Double)entry.getValue(), Double::sum);
                     }

                     Map<String, Integer> boxContents = this.getShulkerBoxContents(item);
                     if (!boxContents.isEmpty()) {
                        String boxKey = UUID.randomUUID().toString();
                        shulkerContentsMap.put(boxKey, new HashMap(boxContents));
                     }

                     container.getInventory().removeItem(new ItemStack[]{item});
                  }
               } else {
                  Double basePrice = this.getItemPrice(item);
                  if (basePrice != null && basePrice > (double)0.0F) {
                     int amount = item.getAmount();
                     double finalPrice = basePrice;
                     String category = null;
                     if (levelManager != null) {
                        category = levelManager.getCategoryForItem(item);
                        if (category != null) {
                           finalPrice = levelManager.calculatePriceWithMultiplier(player, category, basePrice);
                        }
                     }

                     double itemTotal = finalPrice * (double)amount;
                     totalMoney += itemTotal;
                     totalItems += amount;
                     if (levelManager != null && category != null) {
                        categoryEarnings.merge(category, itemTotal, Double::sum);
                     }

                     String itemKey = item.getType().name();
                     soldItemsMap.merge(itemKey, amount, Integer::sum);
                     container.getInventory().removeItem(new ItemStack[]{item});
                  }
               }
            }
         }

         if (levelManager != null) {
            for(Map.Entry<String, Double> entry : categoryEarnings.entrySet()) {
               levelManager.addEarnings(player, (String)entry.getKey(), (Double)entry.getValue());
            }
         }

         if (totalMoney > (double)0.0F) {
            this.plugin.getEconomy().depositPlayer(player, totalMoney);
            this.updateSellAxeLore(sellAxe);
            Map<String, String> placeholders = new HashMap();
            placeholders.put("amount", String.valueOf(totalItems));
            placeholders.put("price", this.currencyFormatter.format(totalMoney));
            this.langManager.sendSellSuccess(player, totalItems, totalMoney);
            if (this.soundEnabled && this.soundType != null) {
               player.playSound(player.getLocation(), this.soundType, 1.0F, 1.0F);
            }

            if (this.particleEnabled && this.particleType != null) {
               this.spawnParticles(container.getLocation());
            }

            String itemsSoldString = this.createItemsSoldString(soldItemsMap, shulkerContentsMap);
            Map<String, Integer> flattenedShulkerContents = new HashMap();

            for(Map.Entry<String, Map<String, Integer>> entry : shulkerContentsMap.entrySet()) {
               for(Map.Entry<String, Integer> contentEntry : entry.getValue().entrySet()) {
                  flattenedShulkerContents.put(contentEntry.getKey(), flattenedShulkerContents.getOrDefault(contentEntry.getKey(), 0) + contentEntry.getValue());
               }
            }

            if (!flattenedShulkerContents.isEmpty()) {
               this.playerDataManager.addSellLog(player.getUniqueId(), totalItems, totalMoney, itemsSoldString, flattenedShulkerContents);
            } else {
               this.playerDataManager.addSellLog(player.getUniqueId(), totalItems, totalMoney, itemsSoldString);
            }
         } else if (this.soundEnabled) {
         }

      }
   }

   private Map<String, Integer> getShulkerBoxContents(ItemStack shulkerBox) {
      Map<String, Integer> contents = new HashMap();
      if (this.shulkerSupportEnabled && this.isShulkerBox(shulkerBox)) {
         try {
            BlockStateMeta meta = (BlockStateMeta)shulkerBox.getItemMeta();
            if (meta == null) {
               return contents;
            } else {
               BlockState state = meta.getBlockState();
               if (!(state instanceof ShulkerBox)) {
                  return contents;
               } else {
                  ShulkerBox shulker = (ShulkerBox)state;
                  Inventory shulkerInventory = shulker.getInventory();
                  if (shulkerInventory == null) {
                     return contents;
                  } else {
                     for(ItemStack item : shulkerInventory.getContents()) {
                        if (item != null && item.getType() != Material.AIR) {
                           String itemKey = item.getType().name();
                           contents.put(itemKey, (Integer)contents.getOrDefault(itemKey, 0) + item.getAmount());
                        }
                     }

                     return contents;
                  }
               }
            }
         } catch (Exception var12) {
            return contents;
         }
      } else {
         return contents;
      }
   }

   private double calculateShulkerBoxTotalValue(ItemStack shulkerBox) {
      if (this.shulkerSupportEnabled && this.isShulkerBox(shulkerBox)) {
         double totalValue = (double)0.0F;

         try {
            Double shulkerBoxValue = this.getItemPrice(shulkerBox);
            if (shulkerBoxValue != null && shulkerBoxValue > (double)0.0F) {
               totalValue += shulkerBoxValue;
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
                  if (shulkerInventory == null) {
                     return totalValue;
                  } else {
                     for(ItemStack item : shulkerInventory.getContents()) {
                        if (item != null && item.getType() != Material.AIR) {
                           Double price = this.getItemPrice(item);
                           if (price != null && price > (double)0.0F) {
                              totalValue += price * (double)item.getAmount();
                           }
                        }
                     }

                     return totalValue;
                  }
               }
            }
         } catch (Exception var14) {
            return totalValue;
         }
      } else {
         return (double)0.0F;
      }
   }

   private int countItemsInShulkerBox(ItemStack shulkerBox) {
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
                  if (shulkerInventory == null) {
                     return itemCount;
                  } else {
                     for(ItemStack item : shulkerInventory.getContents()) {
                        if (item != null && item.getType() != Material.AIR) {
                           itemCount += item.getAmount();
                        }
                     }

                     return itemCount;
                  }
               }
            }
         } catch (Exception var11) {
            return itemCount;
         }
      } else {
         return 0;
      }
   }

   public void updateSellAxeLore(ItemStack sellAxe) {
      if (this.isSellAxe(sellAxe)) {
         ItemMeta meta = sellAxe.getItemMeta();
         if (meta != null) {
            Long expirationTimestamp = (Long)meta.getPersistentDataContainer().get(this.expirationKey, PersistentDataType.LONG);
            if (expirationTimestamp != null) {
               long secondsRemaining = expirationTimestamp - Instant.now().getEpochSecond();
               String timeDisplay = this.formatRemainingTime(secondsRemaining);
               List<String> updatedLore = new ArrayList();

               for(String line : this.lore) {
                  String processedLine = line.replace("%time%", timeDisplay);
                  String translatedLine = HexColorCode.translateAllColorCodes(processedLine);
                  updatedLore.add(translatedLine);
               }

               meta.setLore(updatedLore);
               sellAxe.setItemMeta(meta);
            }
         }
      }
   }

   private Double getItemPrice(ItemStack item) {
      if (item != null && item.getType() != Material.AIR) {
         if (item.getType() == Material.ENCHANTED_BOOK) {
            return this.priceManager.getEnchantedBookPrice(item);
         } else {
            return this.isPotion(item) ? this.priceManager.getPotionPrice(item) : this.priceManager.getPrice(item.getType());
         }
      } else {
         return null;
      }
   }

   private boolean isShulkerBox(ItemStack item) {
      if (item != null && item.hasItemMeta()) {
         Material type = item.getType();
         return type == Material.SHULKER_BOX || type == Material.WHITE_SHULKER_BOX || type == Material.ORANGE_SHULKER_BOX || type == Material.MAGENTA_SHULKER_BOX || type == Material.LIGHT_BLUE_SHULKER_BOX || type == Material.YELLOW_SHULKER_BOX || type == Material.LIME_SHULKER_BOX || type == Material.PINK_SHULKER_BOX || type == Material.GRAY_SHULKER_BOX || type == Material.LIGHT_GRAY_SHULKER_BOX || type == Material.CYAN_SHULKER_BOX || type == Material.PURPLE_SHULKER_BOX || type == Material.BLUE_SHULKER_BOX || type == Material.BROWN_SHULKER_BOX || type == Material.GREEN_SHULKER_BOX || type == Material.RED_SHULKER_BOX || type == Material.BLACK_SHULKER_BOX;
      } else {
         return false;
      }
   }

   private boolean isPotion(ItemStack item) {
      return item != null && (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION) && item.hasItemMeta() && item.getItemMeta() instanceof PotionMeta;
   }

   private void spawnParticles(Location location) {
      World world = location.getWorld();
      if (world != null) {
         if (this.isFolia) {
            this.plugin.runTask(() -> world.spawnParticle(this.particleType, location.add((double)0.5F, (double)1.0F, (double)0.5F), 20, 0.3, (double)0.5F, 0.3, 0.1));
         } else {
            world.spawnParticle(this.particleType, location.add((double)0.5F, (double)1.0F, (double)0.5F), 20, 0.3, (double)0.5F, 0.3, 0.1);
         }
      }

   }

   private String createItemsSoldString(Map<String, Integer> soldItems, Map<String, Map<String, Integer>> shulkerContents) {
      StringBuilder sb = new StringBuilder();

      for(Map.Entry<String, Integer> entry : soldItems.entrySet()) {
         if (sb.length() > 0) {
            sb.append(", ");
         }

         sb.append((String)entry.getKey()).append(":").append(entry.getValue());
      }

      for(Map.Entry<String, Map<String, Integer>> shulkerEntry : shulkerContents.entrySet()) {
         for(Map.Entry<String, Integer> contentEntry : shulkerEntry.getValue().entrySet()) {
            if (sb.length() > 0) {
               sb.append(", ");
            }

            sb.append((String)contentEntry.getKey()).append(":").append(contentEntry.getValue());
         }
      }

      return sb.toString();
   }

   private String createItemsSoldString(Map<String, Integer> soldItems) {
      return this.createItemsSoldString(soldItems, new HashMap());
   }

   public void reloadConfig() {
      this.sellAxeConfig = YamlConfiguration.loadConfiguration(this.sellAxeFile);
      this.loadConfig();
      this.shulkerSupportEnabled = this.plugin.isShulkerSupportEnabled();
   }

   public long getRemainingTime(ItemStack sellAxe) {
      if (!this.isSellAxe(sellAxe)) {
         return 0L;
      } else {
         ItemMeta meta = sellAxe.getItemMeta();
         if (meta == null) {
            return 0L;
         } else {
            Long expiration = (Long)meta.getPersistentDataContainer().get(this.expirationKey, PersistentDataType.LONG);
            if (expiration == null) {
               return 0L;
            } else {
               long remaining = expiration - Instant.now().getEpochSecond();
               return Math.max(0L, remaining);
            }
         }
      }
   }

   public boolean repairSellAxe(ItemStack sellAxe) {
      if (!this.isSellAxe(sellAxe)) {
         return false;
      } else {
         ItemMeta meta = sellAxe.getItemMeta();
         if (meta == null) {
            return false;
         } else {
            Instant newExpiration = Instant.now().plus((long)this.timeInDays, ChronoUnit.DAYS);
            meta.getPersistentDataContainer().set(this.expirationKey, PersistentDataType.LONG, newExpiration.getEpochSecond());
            this.updateSellAxeLore(sellAxe);
            return true;
         }
      }
   }

   private Map<String, Sound> createSoundConversionTable() {
      Map<String, Sound> table = new HashMap();
      table.put("entity.experience_orb.pickup", Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
      table.put("entity.villager.no", Sound.ENTITY_VILLAGER_NO);
      table.put("entity.villager.yes", Sound.ENTITY_VILLAGER_YES);
      table.put("entity.player.levelup", Sound.ENTITY_PLAYER_LEVELUP);
      table.put("entity.player.burp", Sound.ENTITY_PLAYER_BURP);
      table.put("entity.player.hurt", Sound.ENTITY_PLAYER_HURT);
      table.put("entity.item.pickup", Sound.ENTITY_ITEM_PICKUP);
      table.put("entity.arrow.hit", Sound.ENTITY_ARROW_HIT);
      table.put("entity.chicken.egg", Sound.ENTITY_CHICKEN_EGG);
      table.put("entity.creeper.primed", Sound.ENTITY_CREEPER_PRIMED);
      table.put("entity.ender_dragon.death", Sound.ENTITY_ENDER_DRAGON_DEATH);
      table.put("entity.generic.explode", Sound.ENTITY_GENERIC_EXPLODE);
      table.put("entity.generic.eat", Sound.ENTITY_GENERIC_EAT);
      table.put("entity.generic.drink", Sound.ENTITY_GENERIC_DRINK);
      table.put("block.note_block.pling", Sound.BLOCK_NOTE_BLOCK_PLING);
      table.put("block.note_block.bell", Sound.BLOCK_NOTE_BLOCK_BELL);
      table.put("block.note_block.harp", Sound.BLOCK_NOTE_BLOCK_HARP);
      table.put("block.anvil.land", Sound.BLOCK_ANVIL_LAND);
      table.put("block.anvil.use", Sound.BLOCK_ANVIL_USE);
      table.put("block.chest.open", Sound.BLOCK_CHEST_OPEN);
      table.put("block.chest.close", Sound.BLOCK_CHEST_CLOSE);
      table.put("block.ender_chest.open", Sound.BLOCK_ENDER_CHEST_OPEN);
      table.put("block.ender_chest.close", Sound.BLOCK_ENDER_CHEST_CLOSE);
      table.put("block.wooden_door.open", Sound.BLOCK_WOODEN_DOOR_OPEN);
      table.put("block.wooden_door.close", Sound.BLOCK_WOODEN_DOOR_CLOSE);
      table.put("block.iron_door.open", Sound.BLOCK_IRON_DOOR_OPEN);
      table.put("block.iron_door.close", Sound.BLOCK_IRON_DOOR_CLOSE);
      table.put("block.stone.break", Sound.BLOCK_STONE_BREAK);
      table.put("block.stone.place", Sound.BLOCK_STONE_PLACE);
      table.put("block.grass.break", Sound.BLOCK_GRASS_BREAK);
      table.put("block.grass.place", Sound.BLOCK_GRASS_PLACE);
      table.put("block.glass.break", Sound.BLOCK_GLASS_BREAK);
      table.put("block.glass.place", Sound.BLOCK_GLASS_PLACE);
      table.put("ui.button.click", Sound.UI_BUTTON_CLICK);
      table.put("ui.toast.in", Sound.UI_TOAST_IN);
      table.put("ui.toast.out", Sound.UI_TOAST_OUT);
      table.put("ui.toast.challenge_complete", Sound.UI_TOAST_CHALLENGE_COMPLETE);
      table.put("ambient.cave", Sound.AMBIENT_CAVE);
      table.put("ambient.underwater.enter", Sound.AMBIENT_UNDERWATER_ENTER);
      table.put("ambient.underwater.exit", Sound.AMBIENT_UNDERWATER_EXIT);
      table.put("music.creative", Sound.MUSIC_CREATIVE);
      table.put("music.end", Sound.MUSIC_END);
      table.put("music.game", Sound.MUSIC_GAME);
      table.put("music.menu", Sound.MUSIC_MENU);
      table.put("music.dragon", Sound.MUSIC_DRAGON);
      table.put("music.disc.11", Sound.MUSIC_DISC_11);
      table.put("music.disc.13", Sound.MUSIC_DISC_13);
      table.put("music.disc.blocks", Sound.MUSIC_DISC_BLOCKS);
      table.put("music.disc.cat", Sound.MUSIC_DISC_CAT);
      table.put("music.disc.chirp", Sound.MUSIC_DISC_CHIRP);
      table.put("music.disc.far", Sound.MUSIC_DISC_FAR);
      table.put("music.disc.mall", Sound.MUSIC_DISC_MALL);
      table.put("music.disc.mellohi", Sound.MUSIC_DISC_MELLOHI);
      table.put("music.disc.stal", Sound.MUSIC_DISC_STAL);
      table.put("music.disc.strad", Sound.MUSIC_DISC_STRAD);
      table.put("music.disc.wait", Sound.MUSIC_DISC_WAIT);
      table.put("music.disc.ward", Sound.MUSIC_DISC_WARD);
      table.put("weather.rain", Sound.WEATHER_RAIN);
      table.put("weather.rain.above", Sound.WEATHER_RAIN_ABOVE);
      table.put("entity.item.break", Sound.ENTITY_ITEM_BREAK);
      table.put("item.break", Sound.ENTITY_ITEM_BREAK);
      return table;
   }

   private Sound getSoundCompat(String soundName, Sound fallback) {
      try {
         Sound modernSound = this.getModernSound(soundName);
         if (modernSound != null) {
            return modernSound;
         } else {
            Sound converted = (Sound)this.soundConversionTable.get(soundName);
            if (converted != null) {
               return converted;
            } else {
               String convertedName = this.convertSoundNameToLegacy(soundName);
               if (convertedName != null) {
                  Sound legacy = (Sound)this.soundConversionTable.get(convertedName);
                  if (legacy != null) {
                     return legacy;
                  }
               }

               return fallback;
            }
         }
      } catch (Exception var7) {
         return fallback;
      }
   }

   private String convertSoundNameToLegacy(String modernName) {
      if (modernName == null) {
         return null;
      } else {
         if (modernName.startsWith("minecraft:")) {
            modernName = modernName.substring(10);
         }

         return modernName.replace('.', '_').toLowerCase(Locale.ROOT);
      }
   }

   private Sound getModernSound(String soundName) {
      try {
         if (soundName == null) {
            return null;
         }

         if (soundName.contains(".")) {
            String[] parts = soundName.split("\\.");
            if (parts.length >= 2) {
               String namespace = parts[0];
               String key = soundName.substring(namespace.length() + 1);
               NamespacedKey namespacedKey = NamespacedKey.fromString(key, this.plugin);
               if (namespacedKey != null) {
                  return this.getSoundByReflection(namespacedKey);
               }
            }
         }
      } catch (Exception var6) {
      }

      return null;
   }

   private Sound getSoundByReflection(NamespacedKey key) {
      try {
         Class<?> registryClass = Class.forName("org.bukkit.Registry");
         Method getRegistryMethod = registryClass.getMethod("getRegistry", Class.class);
         Object soundRegistry = getRegistryMethod.invoke((Object)null, Sound.class);
         Method getMethod = soundRegistry.getClass().getMethod("get", NamespacedKey.class);
         Object sound = getMethod.invoke(soundRegistry, key);
         if (sound != null && sound instanceof Sound) {
            return (Sound)sound;
         }
      } catch (Exception var7) {
      }

      return null;
   }

   private Particle matchParticleOrDefault(String name, Particle def) {
      if (name == null) {
         return def;
      } else {
         String normalized = name.trim().toUpperCase(Locale.ROOT);

         for(Particle p : Particle.values()) {
            if (p.name().equalsIgnoreCase(normalized)) {
               return p;
            }
         }

         return def;
      }
   }

   public boolean isShulkerSupportEnabled() {
      return this.shulkerSupportEnabled;
   }
}
