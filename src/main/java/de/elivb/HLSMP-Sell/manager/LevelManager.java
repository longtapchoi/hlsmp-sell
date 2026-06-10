package de.elivb.donutSell.manager;

import de.elivb.donutSell.Sell;
import de.elivb.donutSell.models.PriceModel;
import de.elivb.donutSell.utils.CurrencyFormatter;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class LevelManager {
   private final Sell plugin;
   private final CurrencyFormatter currencyFormatter;
   private final PriceManager priceManager;
   private double baseMultiplier = (double)1.0F;
   private double baseVipMultiplier = (double)1.0F;
   private final Map<String, CategoryLevelConfig> categoryConfigs = new HashMap();
   private final Map<String, Set<String>> categoryMaterials = new HashMap();
   private final Set<String> availableCategories = new HashSet();

   public LevelManager(Sell plugin, CurrencyFormatter currencyFormatter, PriceManager priceManager) {
      this.plugin = plugin;
      this.currencyFormatter = currencyFormatter;
      this.priceManager = priceManager;
      this.loadCategoriesFromPriceManager();
      this.loadLevelConfig();
   }

   private void loadCategoriesFromPriceManager() {
      this.categoryMaterials.clear();
      this.availableCategories.clear();
      Map<String, PriceModel> priceCategories = this.priceManager.getPriceCategories();

      for(Map.Entry<String, PriceModel> entry : priceCategories.entrySet()) {
         String categoryName = ((String)entry.getKey()).toLowerCase();
         PriceModel priceModel = (PriceModel)entry.getValue();
         Set<String> materials = new HashSet();

         for(Material material : priceModel.getAllPrices().keySet()) {
            materials.add(material.name());
         }

         this.categoryMaterials.put(categoryName, materials);
         this.availableCategories.add(categoryName);
      }

      String[] defaultCategories = new String[]{"ores", "blocks", "mobs", "books", "crops", "fish", "natural", "potions", "tools"};

      for(String cat : defaultCategories) {
         if (!this.categoryMaterials.containsKey(cat)) {
            this.categoryMaterials.put(cat, new HashSet());
            this.availableCategories.add(cat);
         }
      }

   }

   private void loadLevelConfig() {
      File levelFile = new File(this.plugin.getDataFolder(), "level.yml");
      if (!levelFile.exists()) {
         this.createDefaultLevelConfig(levelFile);
      }

      YamlConfiguration config = YamlConfiguration.loadConfiguration(levelFile);
      this.baseMultiplier = config.getDouble("base-multiplier", (double)1.0F);
      this.baseVipMultiplier = config.getDouble("base-vip-multiplier", (double)1.0F);
      this.categoryConfigs.clear();

      for(String category : this.availableCategories) {
         CategoryLevelConfig catConfig = new CategoryLevelConfig(category, this.baseMultiplier);
         if (config.contains(category)) {
            for(String levelKey : config.getConfigurationSection(category).getKeys(false)) {
               try {
                  int level = Integer.parseInt(levelKey);
                  double amountNeeded = config.getDouble(category + "." + levelKey + ".amountNeeded");
                  double multiplier = config.getDouble(category + "." + levelKey + ".multi");
                  catConfig.addLevel(level, amountNeeded, multiplier);
               } catch (NumberFormatException var13) {
               }
            }
         }

         if (catConfig.getMaxLevel() == 0) {
            this.createDefaultLevels(catConfig);
         }

         this.categoryConfigs.put(category, catConfig);
      }

   }

   private void createDefaultLevelConfig(File levelFile) {
      try {
         levelFile.getParentFile().mkdirs();
         YamlConfiguration config = new YamlConfiguration();
         config.set("base-multiplier", (double)1.0F);
         config.set("base-vip-multiplier", 1.1);

         for(String category : this.availableCategories) {
            for(int i = 1; i <= 20; ++i) {
               double amountNeeded;
               double multiplier;
               if (i == 1) {
                  amountNeeded = (double)25000.0F;
                  multiplier = 1.1;
               } else if (i == 2) {
                  amountNeeded = (double)150000.0F;
                  multiplier = 1.2;
               } else if (i == 3) {
                  amountNeeded = (double)500000.0F;
                  multiplier = 1.3;
               } else if (i == 4) {
                  amountNeeded = (double)1000000.0F;
                  multiplier = 1.4;
               } else if (i == 5) {
                  amountNeeded = (double)5000000.0F;
                  multiplier = (double)1.5F;
               } else if (i == 6) {
                  amountNeeded = (double)2.5E7F;
                  multiplier = 1.6;
               } else if (i == 7) {
                  amountNeeded = (double)2.5E8F;
                  multiplier = 1.7;
               } else if (i == 8) {
                  amountNeeded = (double)5.5E8F;
                  multiplier = 1.8;
               } else if (i == 9) {
                  amountNeeded = (double)8.5E8F;
                  multiplier = 1.9;
               } else if (i == 10) {
                  amountNeeded = (double)1.0E9F;
                  multiplier = (double)2.0F;
               } else if (i == 11) {
                  amountNeeded = (double)2.0E9F;
                  multiplier = 2.1;
               } else if (i == 12) {
                  amountNeeded = (double)4.0E9F;
                  multiplier = 2.2;
               } else if (i == 13) {
                  amountNeeded = (double)8.0E9F;
                  multiplier = 2.3;
               } else if (i == 14) {
                  amountNeeded = (double)1.0E10F;
                  multiplier = 2.4;
               } else if (i == 15) {
                  amountNeeded = (double)2.0E10F;
                  multiplier = (double)2.5F;
               } else if (i == 16) {
                  amountNeeded = (double)4.0E10F;
                  multiplier = 2.6;
               } else if (i == 17) {
                  amountNeeded = (double)8.0E10F;
                  multiplier = 2.7;
               } else if (i == 18) {
                  amountNeeded = (double)1.6E11F;
                  multiplier = 2.8;
               } else if (i == 19) {
                  amountNeeded = (double)3.2E11F;
                  multiplier = 2.9;
               } else {
                  amountNeeded = (double)6.4E11F;
                  multiplier = (double)3.0F;
               }

               config.set(category + "." + i + ".amountNeeded", amountNeeded);
               config.set(category + "." + i + ".multi", multiplier);
            }
         }

         config.save(levelFile);
      } catch (Exception var10) {
      }

   }

   private void createDefaultLevels(CategoryLevelConfig catConfig) {
      catConfig.addLevel(1, (double)25000.0F, 1.1);
      catConfig.addLevel(2, (double)150000.0F, 1.2);
      catConfig.addLevel(3, (double)500000.0F, 1.3);
      catConfig.addLevel(4, (double)1000000.0F, 1.4);
      catConfig.addLevel(5, (double)5000000.0F, (double)1.5F);
      catConfig.addLevel(6, (double)2.5E7F, 1.6);
      catConfig.addLevel(7, (double)2.5E8F, 1.7);
      catConfig.addLevel(8, (double)5.5E8F, 1.8);
      catConfig.addLevel(9, (double)8.5E8F, 1.9);
      catConfig.addLevel(10, (double)1.0E9F, (double)2.0F);
      catConfig.addLevel(11, (double)2.0E9F, 2.1);
      catConfig.addLevel(12, (double)4.0E9F, 2.2);
      catConfig.addLevel(13, (double)8.0E9F, 2.3);
      catConfig.addLevel(14, (double)1.0E10F, 2.4);
      catConfig.addLevel(15, (double)2.0E10F, (double)2.5F);
      catConfig.addLevel(16, (double)4.0E10F, 2.6);
      catConfig.addLevel(17, (double)8.0E10F, 2.7);
      catConfig.addLevel(18, (double)1.6E11F, 2.8);
      catConfig.addLevel(19, (double)3.2E11F, 2.9);
      catConfig.addLevel(20, (double)6.4E11F, (double)3.0F);
   }

   public String getCategory(Material material) {
      if (material == null) {
         return "natural";
      } else {
         Map<String, PriceModel> priceCategories = this.priceManager.getPriceCategories();

         for(Map.Entry<String, PriceModel> entry : priceCategories.entrySet()) {
            if (((PriceModel)entry.getValue()).hasPrice(material)) {
               return ((String)entry.getKey()).toLowerCase();
            }
         }

         for(Map.Entry<String, Set<String>> entry : this.categoryMaterials.entrySet()) {
            if (((Set)entry.getValue()).contains(material.name())) {
               return (String)entry.getKey();
            }
         }

         return "natural";
      }
   }

   public String getCategoryForItem(ItemStack item) {
      if (item == null) {
         return "natural";
      } else if (item.getType() == Material.ENCHANTED_BOOK) {
         return "books";
      } else if (item.getType().toString().contains("POTION")) {
         return "potions";
      } else {
         return this.isShulkerBox(item) ? "blocks" : this.getCategory(item.getType());
      }
   }

   private boolean isShulkerBox(ItemStack item) {
      if (item == null) {
         return false;
      } else {
         String name = item.getType().name();
         return name.contains("SHULKER_BOX");
      }
   }

   private CategoryPlayerData getPlayerCategoryData(Player player, String category) {
      FileConfiguration config = this.plugin.getPlayerDataManager().getPlayerConfig(player);
      return new CategoryPlayerData(config, category);
   }

   private void savePlayerCategoryData(Player player, String category, CategoryPlayerData data) {
      FileConfiguration config = this.plugin.getPlayerDataManager().getPlayerConfig(player);
      data.save(config, category);
      this.plugin.getPlayerDataManager().savePlayerConfig(player);
   }

   public int getLevel(Player player, String category) {
      CategoryPlayerData data = this.getPlayerCategoryData(player, category);
      return data.getLevel();
   }

   public double getMultiplier(Player player, String category) {
      CategoryPlayerData data = this.getPlayerCategoryData(player, category);
      return data.getCurrentMultiplier();
   }

   public double getTotalEarned(Player player, String category) {
      CategoryPlayerData data = this.getPlayerCategoryData(player, category);
      return data.getTotalEarned();
   }

   public int getProgress(Player player, String category) {
      CategoryPlayerData data = this.getPlayerCategoryData(player, category);
      double totalEarned = data.getTotalEarned();
      int currentLevel = data.getLevel();
      CategoryLevelConfig config = (CategoryLevelConfig)this.categoryConfigs.get(category);
      if (config == null) {
         return 0;
      } else {
         Double currentNeeded = config.getAmountNeededForLevel(currentLevel);
         Double nextNeeded = config.getAmountNeededForLevel(currentLevel + 1);
         if (currentLevel == 0) {
            currentNeeded = (double)0.0F;
         }

         if (currentNeeded != null && nextNeeded != null) {
            double needed = nextNeeded - currentNeeded;
            double earned = totalEarned - currentNeeded;
            return needed <= (double)0.0F ? 100 : (int)Math.min((double)100.0F, earned / needed * (double)100.0F);
         } else {
            return 100;
         }
      }
   }

   public double getNextLevelCost(Player player, String category) {
      int currentLevel = this.getLevel(player, category);
      CategoryLevelConfig config = (CategoryLevelConfig)this.categoryConfigs.get(category);
      if (config == null) {
         return (double)-1.0F;
      } else {
         Double nextNeeded = config.getAmountNeededForLevel(currentLevel + 1);
         if (nextNeeded == null) {
            return (double)-1.0F;
         } else {
            double totalEarned = this.getTotalEarned(player, category);
            return Math.max((double)0.0F, nextNeeded - totalEarned);
         }
      }
   }

   public double getNextMultiplier(Player player, String category) {
      int currentLevel = this.getLevel(player, category);
      CategoryLevelConfig config = (CategoryLevelConfig)this.categoryConfigs.get(category);
      if (config == null) {
         return (double)-1.0F;
      } else {
         Double nextMulti = config.getMultiplierForLevel(currentLevel + 1);
         return nextMulti != null ? nextMulti : (double)-1.0F;
      }
   }

   public double getRequiredForLevel(String category, int level) {
      CategoryLevelConfig config = (CategoryLevelConfig)this.categoryConfigs.get(category);
      if (config == null) {
         return (double)-1.0F;
      } else {
         Double needed = config.getAmountNeededForLevel(level);
         return needed != null ? needed : (double)-1.0F;
      }
   }

   public double getRequiredForNextLevel(Player player, String category) {
      int currentLevel = this.getLevel(player, category);
      CategoryLevelConfig config = (CategoryLevelConfig)this.categoryConfigs.get(category);
      if (config == null) {
         return (double)-1.0F;
      } else {
         Double needed = config.getAmountNeededForLevel(currentLevel + 1);
         return needed != null ? needed : (double)-1.0F;
      }
   }

   public double getMultiplierForLevel(String category, int level) {
      CategoryLevelConfig config = (CategoryLevelConfig)this.categoryConfigs.get(category);
      if (config == null) {
         return this.baseMultiplier;
      } else {
         Double multiplier = config.getMultiplierForLevel(level);
         return multiplier != null ? multiplier : this.baseMultiplier;
      }
   }

   public boolean isMaxLevel(Player player, String category) {
      int currentLevel = this.getLevel(player, category);
      CategoryLevelConfig config = (CategoryLevelConfig)this.categoryConfigs.get(category);
      if (config == null) {
         return true;
      } else {
         return currentLevel >= config.getMaxLevel();
      }
   }

   public void addEarnings(Player player, String category, double amount) {
      if (category != null) {
         CategoryPlayerData data = this.getPlayerCategoryData(player, category);
         double oldTotal = data.getTotalEarned();
         double newTotal = oldTotal + amount;
         data.setTotalEarned(newTotal);
         CategoryLevelConfig config = (CategoryLevelConfig)this.categoryConfigs.get(category);
         if (config != null) {
            int currentLevel = data.getLevel();
            int newLevel = config.getLevelForAmount(newTotal);
            double newMultiplier = config.getMultiplierForLevel(newLevel);
            if (newLevel > currentLevel) {
               data.setLevel(newLevel);
               data.setCurrentMultiplier(newMultiplier);
               this.onLevelUp(player, category, currentLevel, newLevel, newMultiplier);
            }
         }

         this.savePlayerCategoryData(player, category, data);
      }
   }

   public double calculatePriceWithMultiplier(Player player, String category, double basePrice) {
      double multiplier = this.getMultiplier(player, category);
      if (player.hasPermission("sell.vip") && this.baseVipMultiplier > (double)1.0F) {
         multiplier *= this.baseVipMultiplier;
      }

      return basePrice * multiplier;
   }

   public double getVipMultiplier() {
      return this.baseVipMultiplier;
   }

   public boolean isVip(Player player) {
      return player.hasPermission("sell.vip");
   }

   private void onLevelUp(Player player, String category, int oldLevel, int newLevel, double newMultiplier) {
   }

   public String getCategoryDisplayName(String category) {
      switch (category.toLowerCase()) {
         case "ores":
            return "Ores";
         case "mobs":
            return "Mobs";
         case "blocks":
            return "Blocks";
         case "books":
            return "Books";
         case "crops":
            return "Crops";
         case "fish":
            return "Fish";
         case "natural":
            return "Natural";
         case "potions":
            return "Potions";
         case "tools":
            return "Tools";
         default:
            String var10000 = category.substring(0, 1).toUpperCase();
            return var10000 + category.substring(1);
      }
   }

   public Set<String> getCategories() {
      return this.availableCategories;
   }

   public void reloadConfig() {
      this.categoryConfigs.clear();
      this.loadCategoriesFromPriceManager();
      this.loadLevelConfig();
   }

   private static class CategoryLevelConfig {
      private final String category;
      private final double baseMultiplier;
      private final TreeMap<Integer, LevelData> levels = new TreeMap();

      public CategoryLevelConfig(String category, double baseMultiplier) {
         this.category = category;
         this.baseMultiplier = baseMultiplier;
      }

      public void addLevel(int level, double amountNeeded, double multiplier) {
         this.levels.put(level, new LevelData(amountNeeded, multiplier));
      }

      public int getLevelForAmount(double amount) {
         int highestLevel = 0;

         for(Map.Entry<Integer, LevelData> entry : this.levels.entrySet()) {
            if (!(amount >= ((LevelData)entry.getValue()).amountNeeded)) {
               break;
            }

            highestLevel = (Integer)entry.getKey();
         }

         return highestLevel;
      }

      public double getMultiplierForLevel(int level) {
         LevelData data = (LevelData)this.levels.get(level);
         return data != null ? data.multiplier : this.baseMultiplier;
      }

      public Double getAmountNeededForLevel(int level) {
         LevelData data = (LevelData)this.levels.get(level);
         return data != null ? data.amountNeeded : null;
      }

      public int getMaxLevel() {
         return this.levels.isEmpty() ? 0 : (Integer)this.levels.lastKey();
      }

      private static class LevelData {
         final double amountNeeded;
         final double multiplier;

         LevelData(double amountNeeded, double multiplier) {
            this.amountNeeded = amountNeeded;
            this.multiplier = multiplier;
         }
      }
   }

   private static class CategoryPlayerData {
      private final FileConfiguration config;
      private double totalEarned;
      private int level;
      private double currentMultiplier;

      public CategoryPlayerData(FileConfiguration config, String category) {
         this.config = config;
         String path = "levels." + category;
         this.totalEarned = config.getDouble(path + ".totalEarned", (double)0.0F);
         this.level = config.getInt(path + ".level", 0);
         this.currentMultiplier = config.getDouble(path + ".multiplier", (double)1.0F);
      }

      public void save(FileConfiguration cfg, String category) {
         String path = "levels." + category;
         cfg.set(path + ".totalEarned", this.totalEarned);
         cfg.set(path + ".level", this.level);
         cfg.set(path + ".multiplier", this.currentMultiplier);
      }

      public double getTotalEarned() {
         return this.totalEarned;
      }

      public void setTotalEarned(double totalEarned) {
         this.totalEarned = totalEarned;
      }

      public int getLevel() {
         return this.level;
      }

      public void setLevel(int level) {
         this.level = level;
      }

      public double getCurrentMultiplier() {
         return this.currentMultiplier;
      }

      public void setCurrentMultiplier(double currentMultiplier) {
         this.currentMultiplier = currentMultiplier;
      }
   }
}
