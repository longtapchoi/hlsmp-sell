package de.elivb.donutSell.manager;

import de.elivb.donutSell.HexColorCode;
import de.elivb.donutSell.Sell;
import de.elivb.donutSell.gui.MainGUI;
import de.elivb.donutSell.gui.ProgressGUI;
import de.elivb.donutSell.gui.ProgressItemListGUI;
import de.elivb.donutSell.gui.SellHistoryGUI;
import de.elivb.donutSell.gui.WorthGUI;
import de.elivb.donutSell.utils.CurrencyFormatter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.PotionMeta;

public class SellManager implements Listener {
   private final Sell plugin;
   private final Economy economy;
   private final CurrencyFormatter currencyFormatter;
   private final PriceManager priceManager;
   private final LangManager langManager;
   private final PlayerDataManager playerDataManager;
   private final boolean isFolia;
   private final Map<UUID, MainGUI> openGUIs;
   private final Map<UUID, WorthGUI> openWorthGUIs;
   private final Map<UUID, SellHistoryGUI> openHistoryGUIs;
   private final Map<UUID, ProgressGUI> openProgressGUIs;
   private final Map<UUID, ProgressItemListGUI> openProgressItemListGUIs;
   private Sound sellSuccessSound;
   private Sound noSellableItemsSound;
   private Sound pluginReloadedSound;
   private Sound clickSound;
   private final Map<String, Sound> soundConversionTable;
   private boolean shulkerSupportEnabled;

   public SellManager(Sell plugin, Economy economy, CurrencyFormatter currencyFormatter, LangManager langManager, PlayerDataManager playerDataManager, boolean isFolia) {
      this.plugin = plugin;
      this.economy = economy;
      this.currencyFormatter = currencyFormatter;
      this.priceManager = new PriceManager(plugin);
      this.langManager = langManager;
      this.playerDataManager = playerDataManager;
      this.isFolia = isFolia;
      this.openGUIs = new HashMap();
      this.openWorthGUIs = new HashMap();
      this.openHistoryGUIs = new HashMap();
      this.openProgressGUIs = new HashMap();
      this.openProgressItemListGUIs = new HashMap();
      this.soundConversionTable = this.createSoundConversionTable();
      this.shulkerSupportEnabled = plugin.isShulkerSupportEnabled();
      this.loadSounds();
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
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
      return table;
   }

   private void loadSounds() {
      try {
         String sellSuccess = this.plugin.getConfig().getString("sounds.sell-success", "entity.experience_orb.pickup");
         String noSellable = this.plugin.getConfig().getString("sounds.no-sellable-items", "entity.villager.no");
         String reloaded = this.plugin.getConfig().getString("sounds.plugin-reloaded", "entity.player.levelup");
         String click = this.plugin.getConfig().getString("sounds.click-sound", "UI_BUTTON_CLICK");
         this.sellSuccessSound = this.getSoundCompat(sellSuccess, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
         this.noSellableItemsSound = this.getSoundCompat(noSellable, Sound.ENTITY_VILLAGER_NO);
         this.pluginReloadedSound = this.getSoundCompat(reloaded, Sound.ENTITY_PLAYER_LEVELUP);
         this.clickSound = this.getSoundCompat(click, Sound.UI_BUTTON_CLICK);
      } catch (Exception var5) {
         this.sellSuccessSound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
         this.noSellableItemsSound = Sound.ENTITY_VILLAGER_NO;
         this.pluginReloadedSound = Sound.ENTITY_PLAYER_LEVELUP;
         this.clickSound = Sound.UI_BUTTON_CLICK;
      }

   }

   private Sound getSoundCompat(String soundName, Sound fallback) {
      try {
         Sound modernSound = this.getModernSound(soundName);
         if (modernSound != null) {
            return modernSound;
         } else {
            Sound convertedSound = this.getSoundFromConversionTable(soundName);
            if (convertedSound != null) {
               return convertedSound;
            } else {
               String convertedName = this.convertSoundNameToLegacy(soundName);
               if (convertedName != null) {
                  Sound legacySound = this.getSoundFromConversionTable(convertedName);
                  if (legacySound != null) {
                     return legacySound;
                  }
               }

               return fallback;
            }
         }
      } catch (Exception var7) {
         return fallback;
      }
   }

   private Sound getSoundFromConversionTable(String soundName) {
      return (Sound)this.soundConversionTable.get(soundName);
   }

   private String convertSoundNameToLegacy(String modernName) {
      if (modernName.startsWith("minecraft:")) {
         modernName = modernName.substring(10);
      }

      return modernName.replace(".", "_").toUpperCase();
   }

   private Sound getModernSound(String soundName) {
      try {
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
         if (sound instanceof Sound) {
            return (Sound)sound;
         }
      } catch (Exception var7) {
      }

      return null;
   }

   public void reloadConfig() {
      this.priceManager.reloadPrices();
      this.langManager.reloadLangConfig();
      MainGUI.reloadConfig();
      WorthGUI.reloadConfig();
      SellHistoryGUI.reloadConfig();
      ProgressGUI.reloadConfig();
      ProgressItemListGUI.reloadConfig();
      this.loadSounds();
      this.shulkerSupportEnabled = this.plugin.isShulkerSupportEnabled();
   }

   public void openSellGUI(Player player) {
      if (this.plugin.isPluginEnabled()) {
         try {
            MainGUI gui = new MainGUI(player, this.plugin);
            this.openGUIs.put(player.getUniqueId(), gui);
            if (this.isFolia) {
               player.getScheduler().run(this.plugin, (scheduledTask) -> player.openInventory(gui.getInventory()), (Runnable)null);
            } else {
               player.openInventory(gui.getInventory());
            }
         } catch (Exception var3) {
         }
      }

   }

   public void openWorthGUI(Player player) {
      if (this.plugin.isPluginEnabled()) {
         try {
            WorthGUI gui = new WorthGUI(this.priceManager, this.currencyFormatter, player, this);
            this.openWorthGUIs.put(player.getUniqueId(), gui);
            if (this.isFolia) {
               player.getScheduler().run(this.plugin, (scheduledTask) -> player.openInventory(gui.getInventory()), (Runnable)null);
            } else {
               player.openInventory(gui.getInventory());
            }
         } catch (Exception var3) {
         }
      }

   }

   public void openSellHistoryGUI(Player player) {
      if (this.plugin.isPluginEnabled()) {
         try {
            SellHistoryGUI gui = new SellHistoryGUI(this.playerDataManager, this.currencyFormatter, player, this);
            this.openHistoryGUIs.put(player.getUniqueId(), gui);
            if (this.isFolia) {
               player.getScheduler().run(this.plugin, (scheduledTask) -> player.openInventory(gui.getInventory()), (Runnable)null);
            } else {
               player.openInventory(gui.getInventory());
            }
         } catch (Exception var3) {
         }
      }

   }

   public void openSellHistoryForPlayer(Player viewer, Player target) {
      if (this.plugin.isPluginEnabled()) {
         try {
            SellHistoryGUI gui = new SellHistoryGUI(this.playerDataManager, this.currencyFormatter, target, viewer, this);
            this.openHistoryGUIs.put(viewer.getUniqueId(), gui);
            if (this.isFolia) {
               viewer.getScheduler().run(this.plugin, (scheduledTask) -> viewer.openInventory(gui.getInventory()), (Runnable)null);
            } else {
               viewer.openInventory(gui.getInventory());
            }
         } catch (Exception var4) {
         }
      }

   }

   public void openOfflineSellHistoryGUI(Player viewer, String targetName, FileConfiguration offlineConfig, Map<String, Object> offlineStats) {
      if (this.plugin.isPluginEnabled()) {
         try {
            SellHistoryGUI gui = new SellHistoryGUI(this.playerDataManager, this.currencyFormatter, targetName, offlineConfig, offlineStats, viewer, this);
            this.openHistoryGUIs.put(viewer.getUniqueId(), gui);
            if (this.isFolia) {
               viewer.getScheduler().run(this.plugin, (scheduledTask) -> viewer.openInventory(gui.getInventory()), (Runnable)null);
            } else {
               viewer.openInventory(gui.getInventory());
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player) {
         Player player = (Player)event.getWhoClicked();
         if (this.openProgressItemListGUIs.containsKey(player.getUniqueId())) {
            ProgressItemListGUI gui = (ProgressItemListGUI)this.openProgressItemListGUIs.get(player.getUniqueId());
            if (event.getView().getTopInventory().equals(gui.getInventory())) {
               event.setCancelled(true);
               if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                  int slot = event.getRawSlot();
                  if (gui.isPreviousPageSlot(slot)) {
                     gui.previousPage();
                     player.playSound(player.getLocation(), this.clickSound, 1.0F, 1.0F);
                  } else if (gui.isNextPageSlot(slot)) {
                     gui.nextPage();
                     player.playSound(player.getLocation(), this.clickSound, 1.0F, 1.0F);
                  } else if (gui.isCloseButtonSlot(slot)) {
                     gui.close();
                     player.playSound(player.getLocation(), this.clickSound, 1.0F, 1.0F);
                  } else if (!gui.isNavigationSlot(slot)) {
                     Material material = gui.getMaterialInSlot(slot);
                     if (material != null) {
                        Double price = this.priceManager.getPrice(material);
                        if (price != null && price > (double)0.0F) {
                           String itemName = this.formatMaterialName(material);
                           String formattedPrice = this.currencyFormatter.format(price);
                           String actionBarMessage = this.langManager.getMessageWithoutPrefix("action-bars.item-worth").replace("%item%", itemName).replace("%price%", formattedPrice);
                           player.sendActionBar(HexColorCode.translateAllColorCodes(actionBarMessage));
                           player.playSound(player.getLocation(), this.clickSound, 1.0F, 1.0F);
                        }
                     }
                  }

                  return;
               }

               return;
            }
         }

         if (this.openProgressGUIs.containsKey(player.getUniqueId())) {
            ProgressGUI gui = (ProgressGUI)this.openProgressGUIs.get(player.getUniqueId());
            if (event.getView().getTopInventory().equals(gui.getInventory())) {
               event.setCancelled(true);
               if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                  int slot = event.getRawSlot();
                  if (gui.isBackButtonSlot(slot)) {
                     this.openSellGUI(player);
                     player.playSound(player.getLocation(), this.clickSound, 1.0F, 1.0F);
                  } else if (gui.isCategoryIconSlot(slot)) {
                     ProgressItemListGUI itemListGUI = new ProgressItemListGUI(player, this.plugin, this, gui.getCategory(), gui.getCategoryKey(), gui.getCategoryMaterial());
                     this.openProgressItemListGUIs.put(player.getUniqueId(), itemListGUI);
                     player.openInventory(itemListGUI.getInventory());
                     player.playSound(player.getLocation(), this.clickSound, 1.0F, 1.0F);
                  }

                  return;
               }

               return;
            }
         }

         if (this.openHistoryGUIs.containsKey(player.getUniqueId())) {
            SellHistoryGUI gui = (SellHistoryGUI)this.openHistoryGUIs.get(player.getUniqueId());
            if (event.getView().getTopInventory().equals(gui.getInventory())) {
               event.setCancelled(true);
               if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                  int slot = event.getRawSlot();
                  if (gui.isPreviousPageSlot(slot)) {
                     gui.previousPage();
                     player.playSound(player.getLocation(), this.clickSound, 1.0F, 1.0F);
                  } else if (gui.isNextPageSlot(slot)) {
                     gui.nextPage();
                     player.playSound(player.getLocation(), this.clickSound, 1.0F, 1.0F);
                  } else if (gui.isSortButtonSlot(slot)) {
                     gui.switchSort();
                     player.playSound(player.getLocation(), this.clickSound, 1.0F, 1.0F);
                  } else if (gui.isRefreshButtonSlot(slot)) {
                     gui.refreshHistory();
                     player.playSound(player.getLocation(), this.clickSound, 1.0F, 1.0F);
                  } else if (gui.isStatisticsSlot(slot)) {
                     player.playSound(player.getLocation(), this.clickSound, 1.0F, 1.0F);
                  }

                  return;
               }

               return;
            }
         } else if (this.openWorthGUIs.containsKey(player.getUniqueId())) {
            WorthGUI gui = (WorthGUI)this.openWorthGUIs.get(player.getUniqueId());
            if (event.getView().getTopInventory().equals(gui.getInventory())) {
               event.setCancelled(true);
               if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                  int slot = event.getRawSlot();
                  if (gui.isPreviousPageSlot(slot)) {
                     gui.previousPage();
                     player.playSound(player.getLocation(), this.clickSound, 1.0F, 1.0F);
                  } else if (gui.isNextPageSlot(slot)) {
                     gui.nextPage();
                     player.playSound(player.getLocation(), this.clickSound, 1.0F, 1.0F);
                  } else if (gui.isSortButtonSlot(slot)) {
                     gui.switchSort();
                     player.playSound(player.getLocation(), this.clickSound, 1.0F, 1.0F);
                  } else if (gui.isRefreshButtonSlot(slot)) {
                     gui.refreshGUI();
                     player.playSound(player.getLocation(), this.clickSound, 1.0F, 1.0F);
                  } else if (gui.isFilterButtonSlot(slot)) {
                     gui.switchFilter();
                     player.playSound(player.getLocation(), this.clickSound, 1.0F, 1.0F);
                  } else if (gui.isCloseButtonSlot(slot)) {
                     player.closeInventory();
                     player.playSound(player.getLocation(), this.clickSound, 1.0F, 1.0F);
                  } else if (!gui.isNavigationSlot(slot)) {
                     ItemStack clickedItem = event.getCurrentItem();
                     Double price = null;
                     String itemName = "";
                     if (this.shulkerSupportEnabled && this.isShulkerBox(clickedItem)) {
                        price = this.calculateShulkerBoxTotalValue(clickedItem);
                        if (price != null && price > (double)0.0F) {
                           if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
                              itemName = clickedItem.getItemMeta().getDisplayName();
                           } else {
                              itemName = "Shulker";
                           }
                        }
                     } else if (clickedItem.getType() == Material.ENCHANTED_BOOK) {
                        price = this.priceManager.getEnchantedBookPrice(clickedItem);
                        if (price != null && price > (double)0.0F) {
                           if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
                              itemName = clickedItem.getItemMeta().getDisplayName();
                           } else {
                              itemName = "Enchanted Book";
                           }
                        }
                     } else if (this.isPotion(clickedItem)) {
                        price = this.priceManager.getPotionPrice(clickedItem);
                        if (price != null && price > (double)0.0F) {
                           if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
                              itemName = clickedItem.getItemMeta().getDisplayName();
                           } else {
                              itemName = this.formatMaterialName(clickedItem.getType());
                           }
                        }
                     } else {
                        Material material = gui.getMaterialInSlot(slot);
                        if (material != null) {
                           price = this.priceManager.getPrice(material);
                           itemName = this.formatMaterialName(material);
                        }
                     }

                     if (price != null && price > (double)0.0F) {
                        String formattedPrice = this.currencyFormatter.format(price);
                        String actionBarMessage = this.langManager.getMessageWithoutPrefix("action-bars.item-worth").replace("%item%", itemName).replace("%price%", formattedPrice);
                        player.sendActionBar(HexColorCode.translateAllColorCodes(actionBarMessage));
                        player.playSound(player.getLocation(), this.clickSound, 1.0F, 1.0F);
                     }
                  }

                  return;
               }

               return;
            }
         } else if (this.openGUIs.containsKey(player.getUniqueId())) {
            MainGUI gui = (MainGUI)this.openGUIs.get(player.getUniqueId());
            if (event.getView().getTopInventory().equals(gui.getInventory())) {
               int slot = event.getRawSlot();
               if (gui.isInfoSlot(slot)) {
                  event.setCancelled(true);
                  String categoryKey = gui.getCategoryAtSlot(slot);
                  if (categoryKey != null) {
                     Material categoryMaterial = this.getMaterialForCategory(categoryKey);
                     ProgressGUI progressGUI = new ProgressGUI(player, this.plugin, this, categoryKey, categoryKey, categoryMaterial);
                     this.openProgressGUIs.put(player.getUniqueId(), progressGUI);
                     player.openInventory(progressGUI.getInventory());
                     player.playSound(player.getLocation(), this.clickSound, 1.0F, 1.0F);
                  }

                  return;
               }

               if (event.getClickedInventory() != null && event.getClickedInventory().equals(gui.getInventory()) && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && this.isGUIBackgroundItem(event.getCurrentItem())) {
                  event.setCancelled(true);
                  return;
               }
            }
         }
      }

   }

   private Material getMaterialForCategory(String category) {
      switch (category.toLowerCase()) {
         case "crops" -> {
            return Material.WHEAT;
         }
         case "ores" -> {
            return Material.DIAMOND;
         }
         case "mobs" -> {
            return Material.BONE;
         }
         case "natural" -> {
            return Material.OAK_LEAVES;
         }
         case "tools" -> {
            return Material.NETHERITE_HELMET;
         }
         case "fish" -> {
            return Material.TROPICAL_FISH;
         }
         case "books" -> {
            return Material.ENCHANTED_BOOK;
         }
         case "potions" -> {
            return Material.POTION;
         }
         case "blocks" -> {
            return Material.BRICK;
         }
         default -> {
            return Material.STONE;
         }
      }
   }

   @EventHandler
   public void onInventoryDrag(InventoryDragEvent event) {
      if (event.getWhoClicked() instanceof Player) {
         Player player = (Player)event.getWhoClicked();
         if (this.openProgressItemListGUIs.containsKey(player.getUniqueId())) {
            ProgressItemListGUI gui = (ProgressItemListGUI)this.openProgressItemListGUIs.get(player.getUniqueId());
            if (event.getView().getTopInventory().equals(gui.getInventory())) {
               event.setCancelled(true);
               return;
            }
         }

         if (this.openProgressGUIs.containsKey(player.getUniqueId())) {
            ProgressGUI gui = (ProgressGUI)this.openProgressGUIs.get(player.getUniqueId());
            if (event.getView().getTopInventory().equals(gui.getInventory())) {
               event.setCancelled(true);
               return;
            }
         }

         if (this.openHistoryGUIs.containsKey(player.getUniqueId())) {
            SellHistoryGUI gui = (SellHistoryGUI)this.openHistoryGUIs.get(player.getUniqueId());
            if (event.getView().getTopInventory().equals(gui.getInventory())) {
               event.setCancelled(true);
               return;
            }
         } else if (this.openWorthGUIs.containsKey(player.getUniqueId())) {
            WorthGUI gui = (WorthGUI)this.openWorthGUIs.get(player.getUniqueId());
            if (event.getView().getTopInventory().equals(gui.getInventory())) {
               event.setCancelled(true);
               return;
            }
         } else if (this.openGUIs.containsKey(player.getUniqueId())) {
            MainGUI gui = (MainGUI)this.openGUIs.get(player.getUniqueId());
            if (event.getView().getTopInventory().equals(gui.getInventory())) {
               for(int slot : event.getRawSlots()) {
                  if (slot < gui.getInventory().getSize()) {
                     ItemStack item = gui.getInventory().getItem(slot);
                     if (this.isGUIBackgroundItem(item)) {
                        event.setCancelled(true);
                        return;
                     }
                  }
               }
            }
         }
      }

   }

   private boolean isGUIBackgroundItem(ItemStack item) {
      if (item == null) {
         return false;
      } else {
         String typeName = item.getType().name();
         return typeName.endsWith("STAINED_GLASS_PANE") || typeName.equals("GLASS_PANE") || typeName.equals("BARRIER");
      }
   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      Player player = (Player)event.getPlayer();
      if (this.openProgressItemListGUIs.containsKey(player.getUniqueId())) {
         ProgressItemListGUI gui = (ProgressItemListGUI)this.openProgressItemListGUIs.get(player.getUniqueId());
         if (event.getView().getTopInventory().equals(gui.getInventory())) {
            this.openProgressItemListGUIs.remove(player.getUniqueId());
         }
      }

      if (this.openProgressGUIs.containsKey(player.getUniqueId())) {
         ProgressGUI gui = (ProgressGUI)this.openProgressGUIs.get(player.getUniqueId());
         if (event.getView().getTopInventory().equals(gui.getInventory())) {
            this.openProgressGUIs.remove(player.getUniqueId());
         }
      }

      if (this.openHistoryGUIs.containsKey(player.getUniqueId())) {
         SellHistoryGUI gui = (SellHistoryGUI)this.openHistoryGUIs.get(player.getUniqueId());
         if (event.getView().getTopInventory().equals(gui.getInventory())) {
            this.openHistoryGUIs.remove(player.getUniqueId());
         }
      } else if (this.openWorthGUIs.containsKey(player.getUniqueId())) {
         WorthGUI gui = (WorthGUI)this.openWorthGUIs.get(player.getUniqueId());
         if (event.getView().getTopInventory().equals(gui.getInventory())) {
            this.openWorthGUIs.remove(player.getUniqueId());
         }
      } else if (this.openGUIs.containsKey(player.getUniqueId())) {
         MainGUI gui = (MainGUI)this.openGUIs.get(player.getUniqueId());
         if (event.getView().getTopInventory().equals(gui.getInventory())) {
            this.openGUIs.remove(player.getUniqueId());
            if (this.isFolia) {
               player.getScheduler().run(this.plugin, (scheduledTask) -> this.sellItems(player, gui), (Runnable)null);
            } else {
               this.sellItems(player, gui);
            }
         }
      }

   }

   private void sellItems(Player player, MainGUI gui) {
      double totalMoney = (double)0.0F;
      int totalItems = 0;
      Map<String, Integer> soldItemsMap = new HashMap();
      Map<String, Map<String, Integer>> shulkerContentsMap = new HashMap();
      Map<String, Double> categoryEarnings = new HashMap();
      List<ItemStack> unsellableItems = new ArrayList();
      LevelManager levelManager = this.plugin.getLevelManager();

      for(int slot = 0; slot < gui.getInventory().getSize(); ++slot) {
         ItemStack item = gui.getInventory().getItem(slot);
         if ((item == null || !gui.isInfoSlot(slot)) && item != null && item.getType() != Material.AIR && !this.isGUIBackgroundItem(item)) {
            if (this.shulkerSupportEnabled && this.isShulkerBox(item)) {
               double contentValue = (double)0.0F;
               int contentItemCount = 0;
               double boxValue = (double)0.0F;
               int boxItemCount = 0;
               Map<String, Double> boxCategoryEarnings = new HashMap();
               Map<String, Integer> boxContents = new HashMap();
               List<ItemStack> unsoldShulkerItems = new ArrayList();

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
                                    contentValue += itemTotal;
                                    contentItemCount += amount;
                                    String itemKey = shulkerItem.getType().name();
                                    soldItemsMap.merge(itemKey, amount, Integer::sum);
                                    boxContents.merge(itemKey, amount, Integer::sum);
                                 } else {
                                    unsoldShulkerItems.add(shulkerItem.clone());
                                 }
                              }
                           }
                        }
                     }
                  }

                  Double shulkerBoxPrice = this.getItemPrice(item);
                  if (shulkerBoxPrice != null && shulkerBoxPrice > (double)0.0F) {
                     boxValue = shulkerBoxPrice * (double)item.getAmount();
                     boxItemCount = item.getAmount();
                     String shulkerType = item.getType().name();
                     soldItemsMap.merge(shulkerType, item.getAmount(), Integer::sum);
                  }
               } catch (Exception var39) {
               }

               boolean hasContentToSell = contentValue > (double)0.0F;
               boolean hasBoxToSell = boxValue > (double)0.0F;
               boolean hasUnsoldItems = !unsoldShulkerItems.isEmpty();
               if (!hasContentToSell && !hasBoxToSell && !hasUnsoldItems) {
                  unsellableItems.add(item);
                  gui.getInventory().setItem(slot, (ItemStack)null);
               } else {
                  if (hasContentToSell) {
                     totalMoney += contentValue;
                     totalItems += contentItemCount;

                     for(Map.Entry<String, Double> entry : boxCategoryEarnings.entrySet()) {
                        categoryEarnings.merge((String)entry.getKey(), (Double)entry.getValue(), Double::sum);
                     }

                     if (!boxContents.isEmpty()) {
                        String boxKey = UUID.randomUUID().toString();
                        shulkerContentsMap.put(boxKey, new HashMap(boxContents));
                     }
                  }

                  if (hasBoxToSell) {
                     totalMoney += boxValue;
                     totalItems += boxItemCount;

                     for(ItemStack unsoldItem : unsoldShulkerItems) {
                        if (unsoldItem != null && unsoldItem.getType() != Material.AIR) {
                           unsellableItems.add(unsoldItem);
                        }
                     }

                     gui.getInventory().setItem(slot, (ItemStack)null);
                  } else if (hasContentToSell || hasUnsoldItems) {
                     ItemStack resultBox = item.clone();

                     try {
                        BlockStateMeta boxMeta = (BlockStateMeta)resultBox.getItemMeta();
                        if (boxMeta != null) {
                           BlockState boxState = boxMeta.getBlockState();
                           if (boxState instanceof ShulkerBox) {
                              ShulkerBox shulker = (ShulkerBox)boxState;
                              shulker.getInventory().clear();

                              for(ItemStack unsoldItem : unsoldShulkerItems) {
                                 if (unsoldItem != null && unsoldItem.getType() != Material.AIR) {
                                    shulker.getInventory().addItem(new ItemStack[]{unsoldItem});
                                 }
                              }

                              boxMeta.setBlockState(shulker);
                              resultBox.setItemMeta(boxMeta);
                           }
                        }
                     } catch (Exception var38) {
                        resultBox = item.clone();
                     }

                     unsellableItems.add(resultBox);
                     gui.getInventory().setItem(slot, (ItemStack)null);
                  }
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
                  gui.getInventory().setItem(slot, (ItemStack)null);
               } else {
                  unsellableItems.add(item);
                  gui.getInventory().setItem(slot, (ItemStack)null);
               }
            }
         }
      }

      if (levelManager != null) {
         for(Map.Entry<String, Double> entry : categoryEarnings.entrySet()) {
            levelManager.addEarnings(player, (String)entry.getKey(), (Double)entry.getValue());
         }
      }

      for(ItemStack item : unsellableItems) {
         if (item != null && item.getType() != Material.AIR) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack[]{item});
            if (!leftover.isEmpty()) {
               for(ItemStack left : leftover.values()) {
                  player.getWorld().dropItemNaturally(player.getLocation(), left);
               }
            }
         }
      }

      if (totalMoney > (double)0.0F) {
         if (this.isFolia) {
            Map<String, Integer> soldCopy = new HashMap(soldItemsMap);
            Map<String, Map<String, Integer>> shulkerCopy = new HashMap(shulkerContentsMap);
            final double finalTotalMoney = totalMoney;
            final int finalTotalItems = totalItems;
            player.getScheduler().run(this.plugin, (scheduledTask) -> {
               this.economy.depositPlayer(player, finalTotalMoney);
               this.sendSellSuccess(player, finalTotalItems, finalTotalMoney, soldCopy, shulkerCopy);
            }, (Runnable)null);
         } else {
            this.economy.depositPlayer(player, totalMoney);
            this.sendSellSuccess(player, totalItems, totalMoney, soldItemsMap, shulkerContentsMap);
         }
      }

   }

   private void sendSellSuccess(Player player, int totalItems, double totalMoney, Map<String, Integer> soldItemsMap, Map<String, Map<String, Integer>> shulkerContentsMap) {
      Map<String, String> placeholders = new HashMap();
      placeholders.put("amount", String.valueOf(totalItems));
      placeholders.put("price", this.currencyFormatter.format(totalMoney));
      placeholders.put("item", "Items");
      this.langManager.sendSellSuccess(player, totalItems, totalMoney);
      player.playSound(player.getLocation(), this.sellSuccessSound, 1.0F, 1.0F);
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

   private double calculateShulkerBoxTotalValue(ItemStack shulkerBox) {
      if (this.shulkerSupportEnabled && this.isShulkerBox(shulkerBox)) {
         double totalValue = (double)0.0F;

         try {
            Double shulkerBoxValue = this.getItemPrice(shulkerBox);
            if (shulkerBoxValue != null && shulkerBoxValue > (double)0.0F) {
               totalValue += shulkerBoxValue * (double)shulkerBox.getAmount();
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
                        Double price = this.getItemPrice(item);
                        if (price != null && price > (double)0.0F) {
                           totalValue += price * (double)item.getAmount();
                        }
                     }
                  }

                  return totalValue;
               }
            }
         } catch (Exception var14) {
            return totalValue;
         }
      } else {
         return (double)0.0F;
      }
   }

   private int countAllItemsInShulkerBox(ItemStack shulkerBox) {
      if (this.shulkerSupportEnabled && this.isShulkerBox(shulkerBox)) {
         int itemCount = shulkerBox.getAmount();

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

   private Double getItemPrice(ItemStack item) {
      return item != null && item.getType() != Material.AIR ? this.priceManager.getPriceWithDurability(item) : null;
   }

   private boolean isShulkerBox(ItemStack item) {
      if (item != null && item.hasItemMeta()) {
         Material type = item.getType();
         return type == Material.SHULKER_BOX || type == Material.WHITE_SHULKER_BOX || type == Material.ORANGE_SHULKER_BOX || type == Material.MAGENTA_SHULKER_BOX || type == Material.LIGHT_BLUE_SHULKER_BOX || type == Material.YELLOW_SHULKER_BOX || type == Material.LIME_SHULKER_BOX || type == Material.PINK_SHULKER_BOX || type == Material.GRAY_SHULKER_BOX || type == Material.LIGHT_GRAY_SHULKER_BOX || type == Material.CYAN_SHULKER_BOX || type == Material.PURPLE_SHULKER_BOX || type == Material.BLUE_SHULKER_BOX || type == Material.BROWN_SHULKER_BOX || type == Material.GREEN_SHULKER_BOX || type == Material.RED_SHULKER_BOX || type == Material.BLACK_SHULKER_BOX;
      } else {
         return false;
      }
   }

   public void playReloadSound(Player player) {
      player.playSound(player.getLocation(), this.pluginReloadedSound, 1.0F, 1.0F);
   }

   public WorthManager getWorthManager() {
      return new WorthManager(this.priceManager, this.currencyFormatter, this, this.isFolia);
   }

   private String getItemDisplayName(ItemStack item) {
      return item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : this.formatMaterialName(item.getType());
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

   private boolean isPotion(ItemStack item) {
      return item != null && (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION) && item.hasItemMeta() && item.getItemMeta() instanceof PotionMeta;
   }

   private void returnItem(Player player, ItemStack item) {
   }

   public PriceManager getPriceManager() {
      return this.priceManager;
   }

   public LangManager getLangManager() {
      return this.langManager;
   }

   public PlayerDataManager getPlayerDataManager() {
      return this.playerDataManager;
   }

   public Map<UUID, MainGUI> getOpenGUIs() {
      return new HashMap(this.openGUIs);
   }

   public Map<UUID, WorthGUI> getOpenWorthGUIs() {
      return new HashMap(this.openWorthGUIs);
   }

   public Map<UUID, SellHistoryGUI> getOpenHistoryGUIs() {
      return new HashMap(this.openHistoryGUIs);
   }

   public Map<UUID, ProgressGUI> getOpenProgressGUIs() {
      return new HashMap(this.openProgressGUIs);
   }

   public Map<UUID, ProgressItemListGUI> getOpenProgressItemListGUIs() {
      return new HashMap(this.openProgressItemListGUIs);
   }

   public void registerProgressGUI(Player player, ProgressGUI gui) {
      this.openProgressGUIs.put(player.getUniqueId(), gui);
   }

   public void unregisterProgressItemListGUI(Player player) {
      this.openProgressItemListGUIs.remove(player.getUniqueId());
   }

   public Sound getSellSuccessSound() {
      return this.sellSuccessSound;
   }

   public Sound getNoSellableItemsSound() {
      return this.noSellableItemsSound;
   }

   public Sound getPluginReloadedSound() {
      return this.pluginReloadedSound;
   }

   public Sound getClickSound() {
      return this.clickSound;
   }

   public Sell getPlugin() {
      return this.plugin;
   }

   public boolean isShulkerSupportEnabled() {
      return this.shulkerSupportEnabled;
   }

   public boolean isFolia() {
      return this.isFolia;
   }
}
