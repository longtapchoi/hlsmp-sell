package de.elivb.donutSell.gui;

import de.elivb.donutSell.HexColorCode;
import de.elivb.donutSell.Sell;
import de.elivb.donutSell.manager.LevelManager;
import de.elivb.donutSell.manager.SellManager;
import de.elivb.donutSell.utils.CurrencyFormatter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class ProgressGUI implements InventoryHolder {
   private Inventory inventory;
   private final Player player;
   private final Sell plugin;
   private final SellManager sellManager;
   private final String category;
   private final String categoryKey;
   private final Material categoryMaterial;
   private static boolean configLoaded = false;
   private static Map<String, String> titles = new HashMap();
   private static boolean fillerEnabled = true;
   private static Material fillerMaterial;
   private static String fillerDisplayName;
   private static List<String> fillerLore;
   private static List<Integer> progressSlots;
   private static Material incompleteMaterial;
   private static String incompleteDisplayName;
   private static List<String> incompleteLore;
   private static Material workingMaterial;
   private static String workingDisplayName;
   private static List<String> workingLore;
   private static Material completeMaterial;
   private static String completeDisplayName;
   private static List<String> completeLore;
   private static boolean backButtonEnabled;
   private static int backButtonSlot;
   private static Material backButtonMaterial;
   private static String backButtonDisplayName;
   private static List<String> backButtonLore;
   private static Map<String, CategoryIconConfig> categoryIcons;
   private static String progressBarLoadingColor;
   private static String progressBarCompleteColor;
   private static String progressBarWorkingColor;
   private static int progressBarLength;
   private static String progressBarSymbol;
   private static int guiRows;

   public ProgressGUI(Player player, Sell plugin, SellManager sellManager, String category, String categoryKey, Material categoryMaterial) {
      this.player = player;
      this.plugin = plugin;
      this.sellManager = sellManager;
      this.category = category;
      this.categoryKey = categoryKey;
      this.categoryMaterial = categoryMaterial;
      this.createGUI();
      this.fillGUI();
   }

   private void createGUI() {
      String titleKey = this.categoryMaterial.name();
      String title = (String)titles.getOrDefault(titleKey, "&8Progress");
      this.inventory = Bukkit.createInventory(this, guiRows * 9, HexColorCode.translateAllColorCodes(title));
   }

   private void fillGUI() {
      LevelManager levelManager = this.plugin.getLevelManager();
      CurrencyFormatter currencyFormatter = this.plugin.getCurrencyFormatter();
      if (levelManager != null) {
         this.addCategoryIcon();
         int currentLevel = levelManager.getLevel(this.player, this.category);
         double totalEarned = levelManager.getTotalEarned(this.player, this.category);
         double requiredForNext = levelManager.getRequiredForNextLevel(this.player, this.category);
         int progressPercent = levelManager.getProgress(this.player, this.category);
         boolean isMaxLevel = levelManager.isMaxLevel(this.player, this.category);

         for(int i = 0; i < progressSlots.size(); ++i) {
            int slot = (Integer)progressSlots.get(i);
            int level = i + 1;
            boolean levelReached = level <= currentLevel;
            boolean isWorkingLevel = level == currentLevel + 1 && !isMaxLevel;
            ItemStack displayItem;
            if (isWorkingLevel) {
               double nextMultiplier = levelManager.getMultiplierForLevel(this.category, level);
               displayItem = this.createWorkingItem(level, nextMultiplier, totalEarned, requiredForNext, progressPercent, currencyFormatter);
            } else if (levelReached) {
               double levelMultiplier = levelManager.getMultiplierForLevel(this.category, level);
               displayItem = this.createCompleteItem(level, levelMultiplier, currencyFormatter);
            } else {
               double levelRequired = levelManager.getRequiredForLevel(this.category, level);
               double levelMultiplier = levelManager.getMultiplierForLevel(this.category, level);
               displayItem = this.createIncompleteItem(level, levelRequired, levelMultiplier, currencyFormatter);
            }

            if (slot < this.inventory.getSize()) {
               this.inventory.setItem(slot, displayItem);
            }
         }

         if (fillerEnabled && fillerMaterial != null && fillerMaterial != Material.AIR) {
            for(int slot = 0; slot < this.inventory.getSize(); ++slot) {
               if (this.inventory.getItem(slot) == null || this.inventory.getItem(slot).getType() == Material.AIR) {
                  this.inventory.setItem(slot, this.createFillerItem());
               }
            }
         }

         if (backButtonEnabled && backButtonSlot < this.inventory.getSize()) {
            this.inventory.setItem(backButtonSlot, this.createBackButton());
         }

      }
   }

   private void addCategoryIcon() {
      CategoryIconConfig iconConfig = (CategoryIconConfig)categoryIcons.get(this.categoryKey);
      int iconSlot = iconConfig != null ? iconConfig.slot : 4;
      if (iconSlot < this.inventory.getSize()) {
         if (iconConfig == null) {
            ItemStack defaultIcon = new ItemStack(this.categoryMaterial);
            ItemMeta meta = defaultIcon.getItemMeta();
            if (meta != null) {
               String var10000 = this.category.substring(0, 1).toUpperCase();
               String displayName = "&#34ee80" + var10000 + this.category.substring(1);
               meta.setDisplayName(HexColorCode.translateAllColorCodes(displayName));
               List<String> lore = new ArrayList(Arrays.asList("&7Sell items to upgrade", "&7your sell multiplier!", "", "&fView your progress below"));
               List<String> translatedLore = new ArrayList();

               for(String line : lore) {
                  translatedLore.add(HexColorCode.translateAllColorCodes(line));
               }

               meta.setLore(translatedLore);
               defaultIcon.setItemMeta(meta);
            }

            this.inventory.setItem(iconSlot, defaultIcon);
         } else {
            ItemStack icon = new ItemStack(iconConfig.material);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
               meta.setDisplayName(HexColorCode.translateAllColorCodes(iconConfig.displayName));
               List<String> lore = new ArrayList();

               for(String line : iconConfig.lore) {
                  lore.add(HexColorCode.translateAllColorCodes(line));
               }

               meta.setLore(lore);
               icon.setItemMeta(meta);
            }

            this.inventory.setItem(iconConfig.slot, icon);
         }
      }
   }

   private ItemStack createFillerItem() {
      ItemStack item = new ItemStack(fillerMaterial);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(HexColorCode.translateAllColorCodes(fillerDisplayName));
         List<String> lore = new ArrayList();

         for(String line : fillerLore) {
            lore.add(HexColorCode.translateAllColorCodes(line));
         }

         meta.setLore(lore);
         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createIncompleteItem(int level, double required, double multiplier, CurrencyFormatter currencyFormatter) {
      ItemStack item = new ItemStack(incompleteMaterial);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(HexColorCode.translateAllColorCodes(incompleteDisplayName.replace("%level%", String.valueOf(level))));
         List<String> lore = new ArrayList();

         for(String line : incompleteLore) {
            String processed = line.replace("%level%", String.valueOf(level)).replace("%multiplier%", String.format("%.1f", multiplier)).replace("%progress%", "0").replace("%amount-have%", "0").replace("%amount-needed%", currencyFormatter.format(required)).replace("%loading-bar%", this.createEmptyProgressBar());
            lore.add(HexColorCode.translateAllColorCodes(processed));
         }

         meta.setLore(lore);
         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createWorkingItem(int level, double multiplier, double totalEarned, double required, int progressPercent, CurrencyFormatter currencyFormatter) {
      ItemStack item = new ItemStack(workingMaterial);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(HexColorCode.translateAllColorCodes(workingDisplayName.replace("%level%", String.valueOf(level))));
         String progressBar = this.createProgressBar(progressPercent);
         String remaining = required > (double)0.0F ? currencyFormatter.format(required - totalEarned) : "0";
         List<String> lore = new ArrayList();

         for(String line : workingLore) {
            String processed = line.replace("%level%", String.valueOf(level)).replace("%multiplier%", String.format("%.1f", multiplier)).replace("%progress%", String.valueOf(progressPercent)).replace("%amount-have%", currencyFormatter.format(totalEarned)).replace("%amount-needed%", currencyFormatter.format(required)).replace("%remaining%", remaining).replace("%loading-bar%", progressBar);
            lore.add(HexColorCode.translateAllColorCodes(processed));
         }

         meta.setLore(lore);
         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createCompleteItem(int level, double multiplier, CurrencyFormatter currencyFormatter) {
      ItemStack item = new ItemStack(completeMaterial);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(HexColorCode.translateAllColorCodes(completeDisplayName.replace("%level%", String.valueOf(level))));
         String fullProgressBar = this.createFullProgressBar();
         List<String> lore = new ArrayList();

         for(String line : completeLore) {
            String processed = line.replace("%level%", String.valueOf(level)).replace("%multiplier%", String.format("%.1f", multiplier)).replace("%progress%", "100").replace("%amount-have%", "Complete").replace("%amount-needed%", "Complete").replace("%loading-bar%", fullProgressBar);
            lore.add(HexColorCode.translateAllColorCodes(processed));
         }

         meta.setLore(lore);
         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createBackButton() {
      ItemStack item = new ItemStack(backButtonMaterial);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(HexColorCode.translateAllColorCodes(backButtonDisplayName));
         List<String> lore = new ArrayList();

         for(String line : backButtonLore) {
            lore.add(HexColorCode.translateAllColorCodes(line));
         }

         meta.setLore(lore);
         item.setItemMeta(meta);
      }

      return item;
   }

   private String createProgressBar(int progress) {
      int filledLength = (int)Math.round((double)progress / (double)100.0F * (double)progressBarLength);
      int emptyLength = progressBarLength - filledLength;
      StringBuilder bar = new StringBuilder();

      for(int i = 0; i < filledLength; ++i) {
         bar.append(HexColorCode.translateAllColorCodes(progressBarWorkingColor + progressBarSymbol));
      }

      for(int i = 0; i < emptyLength; ++i) {
         bar.append(HexColorCode.translateAllColorCodes(progressBarLoadingColor + progressBarSymbol));
      }

      return bar.toString();
   }

   private String createEmptyProgressBar() {
      StringBuilder bar = new StringBuilder();

      for(int i = 0; i < progressBarLength; ++i) {
         bar.append(HexColorCode.translateAllColorCodes(progressBarLoadingColor + progressBarSymbol));
      }

      return bar.toString();
   }

   private String createFullProgressBar() {
      StringBuilder bar = new StringBuilder();

      for(int i = 0; i < progressBarLength; ++i) {
         bar.append(HexColorCode.translateAllColorCodes(progressBarCompleteColor + progressBarSymbol));
      }

      return bar.toString();
   }

   public static void loadConfig() {
      File configFile = new File("plugins/HLSMP-Sell/gui/progress.gui.yml");

      try {
         if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            YamlConfiguration config = new YamlConfiguration();
            config.set("rows", 6);
            config.set("titles-progress-gui.WHEAT", "&8ᴄʀᴏᴘѕ ᴘʀᴏɢʀᴇѕѕ");
            config.set("titles-progress-gui.BONE", "&8ᴍᴏʙ ᴅʀᴏᴘѕ ᴘʀᴏɢʀᴇѕѕ");
            config.set("titles-progress-gui.DIAMOND", "&8ᴏʀᴇ ᴘʀᴏɢʀᴇѕѕ");
            config.set("titles-progress-gui.OAK_LEAVES", "&8ɴᴀᴛᴜʀᴀʟ ɪᴛᴇᴍѕ ᴘʀᴏɢʀᴇѕѕ");
            config.set("titles-progress-gui.NETHERITE_HELMET", "&8ᴀʀᴍᴏʀ ᴀɴᴅ ᴛᴏᴏʟѕ ᴘʀᴏɢʀᴇѕѕ");
            config.set("titles-progress-gui.TROPICAL_FISH", "&8ꜰɪѕʜ ᴘʀᴏɢʀᴇѕѕ");
            config.set("titles-progress-gui.ENCHANTED_BOOK", "&8ᴇɴᴄʜᴀɴᴛᴇᴅ ʙᴏᴏᴋѕ ᴘʀᴏɢʀᴇѕѕ");
            config.set("titles-progress-gui.POTION", "&8ᴘᴏᴛɪᴏɴѕ ᴘʀᴏɢʀᴇѕѕ");
            config.set("titles-progress-gui.BRICK", "&8ʙʟᴏᴄᴋѕ ᴘʀᴏɢʀᴇѕѕ");
            config.set("filler.enabled", true);
            config.set("filler.material", "GRAY_STAINED_GLASS_PANE");
            config.set("filler.displayname", "&7 ");
            config.set("filler.lore", new ArrayList());
            config.set("progress-slots", Arrays.asList(10, 19, 28, 37, 38, 39, 30, 21, 12, 13, 14, 23, 32, 41, 42, 43, 34, 25, 16, 7));
            config.set("incomplete.material", "WHITE_STAINED_GLASS_PANE");
            config.set("incomplete.displayname", "&fɪɴᴄᴏᴍᴘʟᴇᴛᴇ");
            config.set("incomplete.lore", Arrays.asList("&f%loading-bar% &r &f%multiplier%x &f%progress%%", "&7$%amount-have%/%amount-needed%"));
            config.set("working.material", "YELLOW_STAINED_GLASS_PANE");
            config.set("working.displayname", "&#eee70aᴡᴏʀᴋɪɴɢ");
            config.set("working.lore", Arrays.asList("&#eee70a%loading-bar% &r &f%multiplier%x &f%progress%%", "&7%amount-have%/%amount-needed%"));
            config.set("complete.material", "LIME_STAINED_GLASS_PANE");
            config.set("complete.displayname", "&#20f706ᴄᴏᴍᴘʟᴇᴛᴇ");
            config.set("complete.lore", Arrays.asList("&#20f706%loading-bar% &r &#20f706%multiplier%x %progress%%", "&7%amount-have%/%amount-needed%"));
            config.set("back-button.enabled", true);
            config.set("back-button.slot", 45);
            config.set("back-button.material", "RED_STAINED_GLASS_PANE");
            config.set("back-button.displayname", "&#ee300aʙᴀᴄᴋ");
            config.set("back-button.lore", Arrays.asList("&fClick to go back"));
            config.set("categories.crops.icon.material", "WHEAT");
            config.set("categories.crops.icon.slot", 1);
            config.set("categories.crops.icon.displayname", "&#34ee80ᴄʀᴏᴘѕ");
            config.set("categories.crops.icon.lore", Arrays.asList("&7Sell items to upgrade", "&7your sell multiplier!", "", "&fView your progress below"));
            config.set("categories.ores.icon.material", "DIAMOND");
            config.set("categories.ores.icon.slot", 1);
            config.set("categories.ores.icon.displayname", "&#34ee80ᴏʀᴇs");
            config.set("categories.ores.icon.lore", Arrays.asList("&7Sell items to upgrade", "&7your sell multiplier!", "", "&fView your progress below"));
            config.set("categories.mobs.icon.material", "BONE");
            config.set("categories.mobs.icon.slot", 1);
            config.set("categories.mobs.icon.displayname", "&#34ee80ᴍᴏʙ ᴅʀᴏᴘѕ");
            config.set("categories.mobs.icon.lore", Arrays.asList("&7Sell items to upgrade", "&7your sell multiplier!", "", "&fView your progress below"));
            config.set("categories.natural.icon.material", "OAK_LEAVES");
            config.set("categories.natural.icon.slot", 1);
            config.set("categories.natural.icon.displayname", "&#34ee80ɴᴀᴛᴜʀᴀʟ ɪᴛᴇᴍѕ");
            config.set("categories.natural.icon.lore", Arrays.asList("&7Sell items to upgrade", "&7your sell multiplier!", "", "&fView your progress below"));
            config.set("categories.tools.icon.material", "NETHERITE_HELMET");
            config.set("categories.tools.icon.slot", 1);
            config.set("categories.tools.icon.displayname", "&#34ee80ᴀʀᴍᴏʀ ᴀɴᴅ ᴛᴏᴏʟѕ");
            config.set("categories.tools.icon.lore", Arrays.asList("&7Sell items to upgrade", "&7your sell multiplier!", "", "&fView your progress below"));
            config.set("categories.fish.icon.material", "TROPICAL_FISH");
            config.set("categories.fish.icon.slot", 1);
            config.set("categories.fish.icon.displayname", "&#34ee80ꜰɪѕʜ");
            config.set("categories.fish.icon.lore", Arrays.asList("&7Sell items to upgrade", "&7your sell multiplier!", "", "&fView your progress below"));
            config.set("categories.books.icon.material", "ENCHANTED_BOOK");
            config.set("categories.books.icon.slot", 1);
            config.set("categories.books.icon.displayname", "&#34ee80ᴇɴᴄʜᴀɴᴛᴇᴅ ʙᴏᴏᴋѕ");
            config.set("categories.books.icon.lore", Arrays.asList("&7Sell items to upgrade", "&7your sell multiplier!", "", "&fView your progress below"));
            config.set("categories.potions.icon.material", "POTION");
            config.set("categories.potions.icon.slot", 1);
            config.set("categories.potions.icon.displayname", "&#34ee80ᴘᴏᴛɪᴏɴѕ");
            config.set("categories.potions.icon.lore", Arrays.asList("&7Sell items to upgrade", "&7your sell multiplier!", "", "&fView your progress below"));
            config.set("categories.blocks.icon.material", "BRICK");
            config.set("categories.blocks.icon.slot", 1);
            config.set("categories.blocks.icon.displayname", "&#34ee80ʙʟᴏᴄᴋѕ");
            config.set("categories.blocks.icon.lore", Arrays.asList("&7Sell items to upgrade", "&7your sell multiplier!", "", "&fView your progress below"));
            config.save(configFile);
         }

         YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
         guiRows = config.getInt("rows", 6);
         guiRows = Math.max(1, Math.min(6, guiRows));
         titles.clear();
         if (config.contains("titles-progress-gui")) {
            for(String key : config.getConfigurationSection("titles-progress-gui").getKeys(false)) {
               titles.put(key, config.getString("titles-progress-gui." + key, "&8Progress"));
            }
         }

         fillerEnabled = config.getBoolean("filler.enabled", true);
         String fillerMat = config.getString("filler.material", "GRAY_STAINED_GLASS_PANE");
         fillerMaterial = Material.getMaterial(fillerMat);
         if (fillerMaterial == null) {
            fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;
         }

         fillerDisplayName = config.getString("filler.displayname", "&7 ");
         fillerLore = config.getStringList("filler.lore");
         progressSlots = config.getIntegerList("progress-slots");
         String incompleteMat = config.getString("incomplete.material", "WHITE_STAINED_GLASS_PANE");
         incompleteMaterial = Material.getMaterial(incompleteMat);
         if (incompleteMaterial == null) {
            incompleteMaterial = Material.WHITE_STAINED_GLASS_PANE;
         }

         incompleteDisplayName = config.getString("incomplete.displayname", "&fɪɴᴄᴏᴍᴘʟᴇᴛᴇ");
         incompleteLore = config.getStringList("incomplete.lore");
         String workingMat = config.getString("working.material", "YELLOW_STAINED_GLASS_PANE");
         workingMaterial = Material.getMaterial(workingMat);
         if (workingMaterial == null) {
            workingMaterial = Material.YELLOW_STAINED_GLASS_PANE;
         }

         workingDisplayName = config.getString("working.displayname", "&#eee70aᴡᴏʀᴋɪɴɢ");
         workingLore = config.getStringList("working.lore");
         String completeMat = config.getString("complete.material", "LIME_STAINED_GLASS_PANE");
         completeMaterial = Material.getMaterial(completeMat);
         if (completeMaterial == null) {
            completeMaterial = Material.LIME_STAINED_GLASS_PANE;
         }

         completeDisplayName = config.getString("complete.displayname", "&#20f706ᴄᴏᴍᴘʟᴇᴛᴇ");
         completeLore = config.getStringList("complete.lore");
         backButtonEnabled = config.getBoolean("back-button.enabled", true);
         backButtonSlot = config.getInt("back-button.slot", 45);
         String backMat = config.getString("back-button.material", "RED_STAINED_GLASS_PANE");
         backButtonMaterial = Material.getMaterial(backMat);
         if (backButtonMaterial == null) {
            backButtonMaterial = Material.RED_STAINED_GLASS_PANE;
         }

         backButtonDisplayName = config.getString("back-button.displayname", "&#ee300aʙᴀᴄᴋ");
         backButtonLore = config.getStringList("back-button.lore");
         categoryIcons.clear();
         if (config.contains("categories")) {
            for(String catKey : config.getConfigurationSection("categories").getKeys(false)) {
               String path = "categories." + catKey + ".icon";
               if (config.contains(path)) {
                  String materialName = config.getString(path + ".material", "STONE");
                  int slot = config.getInt(path + ".slot", 4);
                  String displayName = config.getString(path + ".displayname", catKey);
                  List<String> lore = config.getStringList(path + ".lore");
                  Material material = Material.getMaterial(materialName);
                  if (material == null) {
                     material = Material.STONE;
                  }

                  categoryIcons.put(catKey, new CategoryIconConfig(material, slot, displayName, lore));
               }
            }
         }

         File mainConfig = new File("plugins/HLSMP-Sell/config.yml");
         if (mainConfig.exists()) {
            YamlConfiguration mainCfg = YamlConfiguration.loadConfiguration(mainConfig);
            progressBarLoadingColor = mainCfg.getString("progress-bar.loading-color", "&f");
            progressBarCompleteColor = mainCfg.getString("progress-bar.complete-loading-color", "&#20f706");
            progressBarLength = mainCfg.getInt("progress-bar.bar-length", 21);
            progressBarSymbol = mainCfg.getString("progress-bar.bar-symbol", "&m ");
            progressBarWorkingColor = mainCfg.getString("progress-bar.working-color", "&#eee70a");
         }

         configLoaded = true;
      } catch (Exception e) {
         e.printStackTrace();
         System.out.println("");
      }

   }

   public static void reloadConfig() {
      loadConfig();
   }

   public boolean isBackButtonSlot(int slot) {
      return backButtonEnabled && slot == backButtonSlot;
   }

   public boolean isCategoryIconSlot(int slot) {
      CategoryIconConfig iconConfig = (CategoryIconConfig)categoryIcons.get(this.categoryKey);
      int iconSlot = iconConfig != null ? iconConfig.slot : 4;
      return slot == iconSlot;
   }

   public String getCategory() {
      return this.category;
   }

   public String getCategoryKey() {
      return this.categoryKey;
   }

   public Material getCategoryMaterial() {
      return this.categoryMaterial;
   }

   public @NotNull Inventory getInventory() {
      return this.inventory;
   }

   static {
      fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;
      fillerDisplayName = "&7 ";
      fillerLore = new ArrayList();
      progressSlots = new ArrayList();
      incompleteMaterial = Material.WHITE_STAINED_GLASS_PANE;
      incompleteDisplayName = "&fɪɴᴄᴏᴍᴘʟᴇᴛᴇ";
      incompleteLore = new ArrayList();
      workingMaterial = Material.YELLOW_STAINED_GLASS_PANE;
      workingDisplayName = "&#eee70aᴡᴏʀᴋɪɴɢ";
      workingLore = new ArrayList();
      completeMaterial = Material.LIME_STAINED_GLASS_PANE;
      completeDisplayName = "&#20f706ᴄᴏᴍᴘʟᴇᴛᴇ";
      completeLore = new ArrayList();
      backButtonEnabled = true;
      backButtonSlot = 45;
      backButtonMaterial = Material.RED_STAINED_GLASS_PANE;
      backButtonDisplayName = "&#ee300aʙᴀᴄᴋ";
      backButtonLore = new ArrayList();
      categoryIcons = new HashMap();
      progressBarLoadingColor = "&f";
      progressBarCompleteColor = "&#20f706";
      progressBarWorkingColor = "&#eee70a";
      progressBarLength = 21;
      progressBarSymbol = "&m ";
      guiRows = 6;
      loadConfig();
   }

   private static class CategoryIconConfig {
      final Material material;
      final int slot;
      final String displayName;
      final List<String> lore;

      CategoryIconConfig(Material material, int slot, String displayName, List<String> lore) {
         this.material = material;
         this.slot = slot;
         this.displayName = displayName;
         this.lore = lore;
      }
   }
}
