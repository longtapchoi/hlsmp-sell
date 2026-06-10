package de.elivb.donutSell.manager;

import de.elivb.donutSell.Sell;
import de.elivb.donutSell.models.PriceModel;
import de.elivb.donutSell.utils.CurrencyFormatter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public class SetWorthManager {
   private final Sell plugin;
   private final PriceManager priceManager;
   private final CurrencyFormatter currencyFormatter;
   private final LangManager langManager;
   private final File pricesFolder;
   private final Map<String, File> categoryFiles;
   private Sound successSound;
   private Sound errorSound;
   private Sound clickSound;
   private final Map<String, Sound> soundConversionTable;

   public SetWorthManager(Sell plugin, PriceManager priceManager, CurrencyFormatter currencyFormatter, LangManager langManager) {
      this.plugin = plugin;
      this.priceManager = priceManager;
      this.currencyFormatter = currencyFormatter;
      this.langManager = langManager;
      this.pricesFolder = new File("plugins/HLSMP-Sell/prices");
      this.categoryFiles = new HashMap();
      this.soundConversionTable = this.createSoundConversionTable();
      this.loadSounds();
      this.loadCategoryFiles();
   }

   private Map<String, Sound> createSoundConversionTable() {
      Map<String, Sound> table = new HashMap();
      table.put("ENTITY_PLAYER_LEVELUP", Sound.ENTITY_PLAYER_LEVELUP);
      table.put("ENTITY_VILLAGER_NO", Sound.ENTITY_VILLAGER_NO);
      table.put("UI_BUTTON_CLICK", Sound.UI_BUTTON_CLICK);
      table.put("ENTITY_EXPERIENCE_ORB_PICKUP", Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
      table.put("ENTITY_VILLAGER_YES", Sound.ENTITY_VILLAGER_YES);
      table.put("ENTITY_PLAYER_BURP", Sound.ENTITY_PLAYER_BURP);
      table.put("ENTITY_PLAYER_HURT", Sound.ENTITY_PLAYER_HURT);
      table.put("ENTITY_ITEM_PICKUP", Sound.ENTITY_ITEM_PICKUP);
      table.put("ENTITY_ARROW_HIT", Sound.ENTITY_ARROW_HIT);
      table.put("ENTITY_CHICKEN_EGG", Sound.ENTITY_CHICKEN_EGG);
      table.put("ENTITY_CREEPER_PRIMED", Sound.ENTITY_CREEPER_PRIMED);
      table.put("ENTITY_ENDER_DRAGON_DEATH", Sound.ENTITY_ENDER_DRAGON_DEATH);
      table.put("ENTITY_GENERIC_EXPLODE", Sound.ENTITY_GENERIC_EXPLODE);
      table.put("ENTITY_GENERIC_EAT", Sound.ENTITY_GENERIC_EAT);
      table.put("ENTITY_GENERIC_DRINK", Sound.ENTITY_GENERIC_DRINK);
      table.put("BLOCK_NOTE_BLOCK_PLING", Sound.BLOCK_NOTE_BLOCK_PLING);
      table.put("BLOCK_NOTE_BLOCK_BELL", Sound.BLOCK_NOTE_BLOCK_BELL);
      table.put("BLOCK_NOTE_BLOCK_HARP", Sound.BLOCK_NOTE_BLOCK_HARP);
      table.put("BLOCK_ANVIL_LAND", Sound.BLOCK_ANVIL_LAND);
      table.put("BLOCK_ANVIL_USE", Sound.BLOCK_ANVIL_USE);
      table.put("BLOCK_CHEST_OPEN", Sound.BLOCK_CHEST_OPEN);
      table.put("BLOCK_CHEST_CLOSE", Sound.BLOCK_CHEST_CLOSE);
      table.put("BLOCK_ENDER_CHEST_OPEN", Sound.BLOCK_ENDER_CHEST_OPEN);
      table.put("BLOCK_ENDER_CHEST_CLOSE", Sound.BLOCK_ENDER_CHEST_CLOSE);
      table.put("BLOCK_WOODEN_DOOR_OPEN", Sound.BLOCK_WOODEN_DOOR_OPEN);
      table.put("BLOCK_WOODEN_DOOR_CLOSE", Sound.BLOCK_WOODEN_DOOR_CLOSE);
      table.put("BLOCK_IRON_DOOR_OPEN", Sound.BLOCK_IRON_DOOR_OPEN);
      table.put("BLOCK_IRON_DOOR_CLOSE", Sound.BLOCK_IRON_DOOR_CLOSE);
      table.put("BLOCK_STONE_BREAK", Sound.BLOCK_STONE_BREAK);
      table.put("BLOCK_STONE_PLACE", Sound.BLOCK_STONE_PLACE);
      table.put("BLOCK_GRASS_BREAK", Sound.BLOCK_GRASS_BREAK);
      table.put("BLOCK_GRASS_PLACE", Sound.BLOCK_GRASS_PLACE);
      table.put("BLOCK_GLASS_BREAK", Sound.BLOCK_GLASS_BREAK);
      table.put("BLOCK_GLASS_PLACE", Sound.BLOCK_GLASS_PLACE);
      table.put("AMBIENT_CAVE", Sound.AMBIENT_CAVE);
      table.put("AMBIENT_UNDERWATER_ENTER", Sound.AMBIENT_UNDERWATER_ENTER);
      table.put("AMBIENT_UNDERWATER_EXIT", Sound.AMBIENT_UNDERWATER_EXIT);
      table.put("MUSIC_CREATIVE", Sound.MUSIC_CREATIVE);
      table.put("MUSIC_END", Sound.MUSIC_END);
      table.put("MUSIC_GAME", Sound.MUSIC_GAME);
      table.put("MUSIC_MENU", Sound.MUSIC_MENU);
      table.put("MUSIC_DRAGON", Sound.MUSIC_DRAGON);
      table.put("WEATHER_RAIN", Sound.WEATHER_RAIN);
      table.put("WEATHER_RAIN_ABOVE", Sound.WEATHER_RAIN_ABOVE);
      return table;
   }

   private void loadSounds() {
      try {
         String success = this.plugin.getConfig().getString("sounds.setworth-success", "ENTITY_PLAYER_LEVELUP");
         String error = this.plugin.getConfig().getString("sounds.setworth-error", "ENTITY_VILLAGER_NO");
         String click = this.plugin.getConfig().getString("sounds.click-sound", "UI_BUTTON_CLICK");
         this.successSound = this.getSoundCompat(success, Sound.ENTITY_PLAYER_LEVELUP);
         this.errorSound = this.getSoundCompat(error, Sound.ENTITY_VILLAGER_NO);
         this.clickSound = this.getSoundCompat(click, Sound.UI_BUTTON_CLICK);
      } catch (Exception var4) {
         this.successSound = Sound.ENTITY_PLAYER_LEVELUP;
         this.errorSound = Sound.ENTITY_VILLAGER_NO;
         this.clickSound = Sound.UI_BUTTON_CLICK;
      }

   }

   private Sound getSoundCompat(String soundName, Sound fallback) {
      Sound modernSound = this.getSoundFromRegistry(soundName);
      if (modernSound != null) {
         return modernSound;
      } else {
         Sound convertedSound = (Sound)this.soundConversionTable.get(soundName);
         return convertedSound != null ? convertedSound : fallback;
      }
   }

   private Sound getSoundFromRegistry(String soundName) {
      try {
         String key = soundName;
         if (soundName.startsWith("minecraft:")) {
            key = soundName.substring(10);
         }

         key = key.toLowerCase();
         NamespacedKey namespacedKey = NamespacedKey.minecraft(key);
         Sound sound = (Sound)Registry.SOUNDS.get(namespacedKey);
         return sound != null ? sound : this.getSoundByReflection(namespacedKey);
      } catch (Exception var5) {
         return null;
      }
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

   private void loadCategoryFiles() {
      if (!this.pricesFolder.exists()) {
         this.pricesFolder.mkdirs();
      }

      File[] files = this.pricesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
      if (files != null) {
         for(File file : files) {
            String categoryName = file.getName().replace(".yml", "");
            this.categoryFiles.put(categoryName, file);
         }
      }

   }

   public String findCategoryForMaterial(Material material) {
      for(Map.Entry<String, PriceModel> entry : this.priceManager.getPriceCategories().entrySet()) {
         if (((PriceModel)entry.getValue()).hasPrice(material)) {
            return (String)entry.getKey();
         }
      }

      return null;
   }

   public String findCategoryForEnchantedBook(String enchantmentKey) {
      if (this.priceManager.getEnchantedBookPrices().containsKey(enchantmentKey)) {
         for(Map.Entry<String, File> entry : this.categoryFiles.entrySet()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration((File)entry.getValue());
            if (config.contains("prices.ENCHANTED_BOOK:" + enchantmentKey)) {
               return (String)entry.getKey();
            }
         }
      }

      return null;
   }

   public String findCategoryForPotion(String potionKey) {
      if (this.priceManager.getPotionPrices().containsKey(potionKey)) {
         for(Map.Entry<String, File> entry : this.categoryFiles.entrySet()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration((File)entry.getValue());
            if (config.contains("prices." + potionKey)) {
               return (String)entry.getKey();
            }
         }
      }

      return null;
   }

   public boolean categoryExists(String category) {
      return this.categoryFiles.containsKey(category);
   }

   public boolean setMaterialPrice(Player player, Material material, double price, String targetCategory) {
      String currentCategory = this.findCategoryForMaterial(material);
      if (currentCategory == null && targetCategory == null) {
         Map<String, String> placeholders = new HashMap();
         placeholders.put("material", material.name());
         placeholders.put("price", String.valueOf(price));
         player.sendMessage(this.langManager.getMessage("messages.material-not-exists", placeholders));
         player.playSound(player.getLocation(), this.errorSound, 1.0F, 1.0F);
         player.sendMessage(this.langManager.getMessageWithoutPrefix("messages.available-categories"));

         for(String cat : this.categoryFiles.keySet()) {
            Map<String, String> catPlaceholders = new HashMap();
            catPlaceholders.put("category", cat);
            player.sendMessage(this.langManager.getMessageWithoutPrefix("messages.category-list", catPlaceholders));
         }

         return false;
      } else if (currentCategory == null && targetCategory != null) {
         if (this.categoryExists(targetCategory)) {
            return this.saveMaterialPrice(player, material, price, targetCategory, true);
         } else {
            Map<String, String> placeholders = new HashMap();
            placeholders.put("category", targetCategory);
            player.sendMessage(this.langManager.getMessage("messages.material-not-exists", placeholders));
            player.playSound(player.getLocation(), this.errorSound, 1.0F, 1.0F);
            player.sendMessage(this.langManager.getMessageWithoutPrefix("messages.available-categories"));

            for(String cat : this.categoryFiles.keySet()) {
               Map<String, String> catPlaceholders = new HashMap();
               catPlaceholders.put("category", cat);
               player.sendMessage(this.langManager.getMessageWithoutPrefix("messages.category-list", catPlaceholders));
            }

            return false;
         }
      } else if (currentCategory != null && targetCategory == null) {
         return this.saveMaterialPrice(player, material, price, currentCategory, false);
      } else if (currentCategory != null && targetCategory != null) {
         if (this.categoryExists(targetCategory)) {
            if (currentCategory.equals(targetCategory)) {
               return this.saveMaterialPrice(player, material, price, currentCategory, false);
            } else {
               boolean removed = this.removeMaterialPrice(currentCategory, material);
               if (removed) {
                  return this.saveMaterialPrice(player, material, price, targetCategory, true);
               } else {
                  Map<String, String> placeholders = new HashMap();
                  placeholders.put("category", currentCategory);
                  player.playSound(player.getLocation(), this.errorSound, 1.0F, 1.0F);
                  return false;
               }
            }
         } else {
            Map<String, String> placeholders = new HashMap();
            placeholders.put("category", targetCategory);
            player.sendMessage(this.langManager.getMessage("messages.material-not-exists", placeholders));
            player.playSound(player.getLocation(), this.errorSound, 1.0F, 1.0F);
            player.sendMessage(this.langManager.getMessageWithoutPrefix("messages.available-categories"));

            for(String cat : this.categoryFiles.keySet()) {
               Map<String, String> catPlaceholders = new HashMap();
               catPlaceholders.put("category", cat);
               player.sendMessage(this.langManager.getMessageWithoutPrefix("messages.category-list", catPlaceholders));
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private boolean saveMaterialPrice(Player player, Material material, double price, String category, boolean isNew) {
      File categoryFile = (File)this.categoryFiles.get(category);
      if (categoryFile == null) {
         Map<String, String> placeholders = new HashMap();
         placeholders.put("category", category);
         player.playSound(player.getLocation(), this.errorSound, 1.0F, 1.0F);
         return false;
      } else {
         try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(categoryFile);
            String materialName = material.name();
            config.set("prices." + materialName, price);
            config.save(categoryFile);
            this.priceManager.reloadPrices();
            Map<String, String> placeholders = new HashMap();
            placeholders.put("material", this.formatMaterialName(material));
            placeholders.put("price", this.currencyFormatter.format(price));
            placeholders.put("category", category);
            if (isNew) {
               player.sendMessage(this.langManager.getMessage("messages.material-created", placeholders));
            } else {
               player.sendMessage(this.langManager.getMessage("messages.material-updated", placeholders));
            }

            player.playSound(player.getLocation(), this.successSound, 1.0F, 1.0F);
            return true;
         } catch (IOException e) {
            Map<String, String> placeholders = new HashMap();
            placeholders.put("error", e.getMessage());
            player.playSound(player.getLocation(), this.errorSound, 1.0F, 1.0F);
            e.printStackTrace();
            return false;
         }
      }
   }

   private boolean removeMaterialPrice(String category, Material material) {
      File categoryFile = (File)this.categoryFiles.get(category);
      if (categoryFile == null) {
         return false;
      } else {
         try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(categoryFile);
            config.set("prices." + material.name(), (Object)null);
            config.save(categoryFile);
            return true;
         } catch (IOException e) {
            e.printStackTrace();
            return false;
         }
      }
   }

   public boolean setEnchantedBookPrice(Player player, String enchantmentKey, double price, String targetCategory) {
      String currentCategory = this.findCategoryForEnchantedBook(enchantmentKey);
      if (currentCategory == null && targetCategory == null) {
         Map<String, String> placeholders = new HashMap();
         placeholders.put("enchantment", enchantmentKey);
         placeholders.put("price", String.valueOf(price));
         player.sendMessage(this.langManager.getMessage("messages.material-not-exists", placeholders));
         player.playSound(player.getLocation(), this.errorSound, 1.0F, 1.0F);
         player.sendMessage(this.langManager.getMessageWithoutPrefix("messages.available-categories"));

         for(String cat : this.categoryFiles.keySet()) {
            Map<String, String> catPlaceholders = new HashMap();
            catPlaceholders.put("category", cat);
            player.sendMessage(this.langManager.getMessageWithoutPrefix("messages.category-list", catPlaceholders));
         }

         return false;
      } else if (currentCategory == null && targetCategory != null) {
         if (!this.categoryExists(targetCategory)) {
            Map<String, String> placeholders = new HashMap();
            placeholders.put("category", targetCategory);
            player.playSound(player.getLocation(), this.errorSound, 1.0F, 1.0F);
            return false;
         } else {
            return this.saveEnchantedBookPrice(player, enchantmentKey, price, targetCategory, true);
         }
      } else if (currentCategory != null && targetCategory == null) {
         return this.saveEnchantedBookPrice(player, enchantmentKey, price, currentCategory, false);
      } else {
         if (currentCategory != null && targetCategory != null) {
            if (!this.categoryExists(targetCategory)) {
               Map<String, String> placeholders = new HashMap();
               placeholders.put("category", targetCategory);
               player.playSound(player.getLocation(), this.errorSound, 1.0F, 1.0F);
               return false;
            }

            if (currentCategory.equals(targetCategory)) {
               return this.saveEnchantedBookPrice(player, enchantmentKey, price, currentCategory, false);
            }

            boolean removed = this.removeEnchantedBookPrice(currentCategory, enchantmentKey);
            if (removed) {
               return this.saveEnchantedBookPrice(player, enchantmentKey, price, targetCategory, true);
            }
         }

         return false;
      }
   }

   private boolean saveEnchantedBookPrice(Player player, String enchantmentKey, double price, String category, boolean isNew) {
      File categoryFile = (File)this.categoryFiles.get(category);
      if (categoryFile == null) {
         player.playSound(player.getLocation(), this.errorSound, 1.0F, 1.0F);
         return false;
      } else {
         try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(categoryFile);
            String key = "ENCHANTED_BOOK:" + enchantmentKey;
            config.set("prices." + key, price);
            config.save(categoryFile);
            this.priceManager.reloadPrices();
            Map<String, String> placeholders = new HashMap();
            placeholders.put("enchantment", enchantmentKey);
            placeholders.put("price", this.currencyFormatter.format(price));
            placeholders.put("category", category);
            if (isNew) {
               player.sendMessage(this.langManager.getMessage("messages.enchantedbook-created", placeholders));
            } else {
               player.sendMessage(this.langManager.getMessage("messages.enchantedbook-updated", placeholders));
            }

            player.playSound(player.getLocation(), this.successSound, 1.0F, 1.0F);
            return true;
         } catch (IOException e) {
            Map<String, String> placeholders = new HashMap();
            placeholders.put("error", e.getMessage());
            player.playSound(player.getLocation(), this.errorSound, 1.0F, 1.0F);
            return false;
         }
      }
   }

   private boolean removeEnchantedBookPrice(String category, String enchantmentKey) {
      File categoryFile = (File)this.categoryFiles.get(category);
      if (categoryFile == null) {
         return false;
      } else {
         try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(categoryFile);
            config.set("prices.ENCHANTED_BOOK:" + enchantmentKey, (Object)null);
            config.save(categoryFile);
            return true;
         } catch (IOException e) {
            e.printStackTrace();
            return false;
         }
      }
   }

   public boolean setPotionPrice(Player player, String potionKey, double price, String targetCategory) {
      String currentCategory = this.findCategoryForPotion(potionKey);
      if (currentCategory == null && targetCategory == null) {
         Map<String, String> placeholders = new HashMap();
         placeholders.put("potion", potionKey);
         placeholders.put("price", String.valueOf(price));
         player.sendMessage(this.langManager.getMessage("messages.material-not-exists", placeholders));
         player.playSound(player.getLocation(), this.errorSound, 1.0F, 1.0F);
         player.sendMessage(this.langManager.getMessageWithoutPrefix("messages.available-categories"));

         for(String cat : this.categoryFiles.keySet()) {
            Map<String, String> catPlaceholders = new HashMap();
            catPlaceholders.put("category", cat);
            player.sendMessage(this.langManager.getMessageWithoutPrefix("messages.category-list", catPlaceholders));
         }

         return false;
      } else if (currentCategory == null && targetCategory != null) {
         if (!this.categoryExists(targetCategory)) {
            Map<String, String> placeholders = new HashMap();
            placeholders.put("category", targetCategory);
            player.playSound(player.getLocation(), this.errorSound, 1.0F, 1.0F);
            return false;
         } else {
            return this.savePotionPrice(player, potionKey, price, targetCategory, true);
         }
      } else if (currentCategory != null && targetCategory == null) {
         return this.savePotionPrice(player, potionKey, price, currentCategory, false);
      } else {
         if (currentCategory != null && targetCategory != null) {
            if (!this.categoryExists(targetCategory)) {
               Map<String, String> placeholders = new HashMap();
               placeholders.put("category", targetCategory);
               player.playSound(player.getLocation(), this.errorSound, 1.0F, 1.0F);
               return false;
            }

            if (currentCategory.equals(targetCategory)) {
               return this.savePotionPrice(player, potionKey, price, currentCategory, false);
            }

            boolean removed = this.removePotionPrice(currentCategory, potionKey);
            if (removed) {
               return this.savePotionPrice(player, potionKey, price, targetCategory, true);
            }
         }

         return false;
      }
   }

   private boolean savePotionPrice(Player player, String potionKey, double price, String category, boolean isNew) {
      File categoryFile = (File)this.categoryFiles.get(category);
      if (categoryFile == null) {
         player.playSound(player.getLocation(), this.errorSound, 1.0F, 1.0F);
         return false;
      } else {
         try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(categoryFile);
            config.set("prices." + potionKey, price);
            config.save(categoryFile);
            this.priceManager.reloadPrices();
            Map<String, String> placeholders = new HashMap();
            placeholders.put("potion", potionKey);
            placeholders.put("price", this.currencyFormatter.format(price));
            placeholders.put("category", category);
            if (isNew) {
               player.sendMessage(this.langManager.getMessage("messages.potion-created", placeholders));
            } else {
               player.sendMessage(this.langManager.getMessage("messages.potion-updated", placeholders));
            }

            player.playSound(player.getLocation(), this.successSound, 1.0F, 1.0F);
            return true;
         } catch (IOException e) {
            Map<String, String> placeholders = new HashMap();
            placeholders.put("error", e.getMessage());
            player.playSound(player.getLocation(), this.errorSound, 1.0F, 1.0F);
            return false;
         }
      }
   }

   private boolean removePotionPrice(String category, String potionKey) {
      File categoryFile = (File)this.categoryFiles.get(category);
      if (categoryFile == null) {
         return false;
      } else {
         try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(categoryFile);
            config.set("prices." + potionKey, (Object)null);
            config.save(categoryFile);
            return true;
         } catch (IOException e) {
            e.printStackTrace();
            return false;
         }
      }
   }

   public String getPotionKeyFromItem(ItemStack item) {
      if (item != null && item.getItemMeta() instanceof PotionMeta) {
         PotionMeta meta = (PotionMeta)item.getItemMeta();
         String potionKey = null;
         if (!meta.getCustomEffects().isEmpty()) {
            PotionEffectType effectType = ((PotionEffect)meta.getCustomEffects().get(0)).getType();
            String var10000 = item.getType().name();
            potionKey = var10000 + ":" + effectType.getKey().getKey().toUpperCase();
         } else {
            try {
               if (meta.getBasePotionType() != null) {
                  PotionType baseType = meta.getBasePotionType();
                  String var7 = item.getType().name();
                  potionKey = var7 + ":" + baseType.name();
               }
            } catch (Exception var5) {
               potionKey = item.getType().name() + ":WATER";
            }
         }

         return potionKey;
      } else {
         return null;
      }
   }

   public String getEnchantedBookKeyFromItem(ItemStack item) {
      if (item != null && item.getType() == Material.ENCHANTED_BOOK && item.hasItemMeta()) {
         EnchantmentStorageMeta meta = (EnchantmentStorageMeta)item.getItemMeta();
         if (meta.hasStoredEnchants() && !meta.getStoredEnchants().isEmpty()) {
            Map.Entry<Enchantment, Integer> enchant = (Map.Entry)meta.getStoredEnchants().entrySet().iterator().next();
            String enchantName = ((Enchantment)enchant.getKey()).getKey().getKey().toUpperCase();
            return enchantName + "_" + String.valueOf(enchant.getValue());
         } else {
            return null;
         }
      } else {
         return null;
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

   public Map<String, File> getCategoryFiles() {
      return this.categoryFiles;
   }

   public void reloadSounds() {
      this.loadSounds();
   }
}
