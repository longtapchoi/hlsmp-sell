package de.elivb.donutSell.gui;

import de.elivb.donutSell.HexColorCode;
import de.elivb.donutSell.Sell;
import de.elivb.donutSell.manager.LangManager;
import de.elivb.donutSell.manager.LevelManager;
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

public class MainGUI implements InventoryHolder {
   private Inventory inventory;
   private static String guiTitle = "&8ꜱᴇʟʟɢᴜɪ";
   private static int guiRows = 5;
   private static Map<String, CategoryButton> categoryButtons = new HashMap();
   private static String progressBarLoadingColor = "&f";
   private static String progressBarCompleteColor = "&#20f706";
   private static int progressBarLength = 21;
   private static String progressBarSymbol = "&m ";
   private final Player player;
   private final Sell plugin;

   public MainGUI(LangManager langManager) {
      this.player = null;
      this.plugin = null;
      this.createGUI();
   }

   public MainGUI(Player player, Sell plugin) {
      this.player = player;
      this.plugin = plugin;
      this.createGUI();
      this.addCategoryButtons();
   }

   private void createGUI() {
      this.inventory = Bukkit.createInventory(this, guiRows * 9, HexColorCode.translateAllColorCodes(guiTitle));
   }

   private void addCategoryButtons() {
      if (this.player != null && this.plugin != null) {
         LevelManager levelManager = this.plugin.getLevelManager();
         if (levelManager != null) {
            CurrencyFormatter currencyFormatter = this.plugin.getCurrencyFormatter();

            for(Map.Entry<String, CategoryButton> entry : categoryButtons.entrySet()) {
               CategoryButton btn = (CategoryButton)entry.getValue();
               int level = levelManager.getLevel(this.player, btn.category);
               double multiplier = levelManager.getMultiplier(this.player, btn.category);
               double totalEarned = levelManager.getTotalEarned(this.player, btn.category);
               int progress = levelManager.getProgress(this.player, btn.category);
               double nextCost = levelManager.getNextLevelCost(this.player, btn.category);
               double nextMulti = levelManager.getNextMultiplier(this.player, btn.category);
               String progressBar = this.createProgressBar(progress);
               ItemStack item = new ItemStack(btn.material);
               ItemMeta meta = item.getItemMeta();
               if (meta != null) {
                  meta.setDisplayName(HexColorCode.translateAllColorCodes(btn.name));
                  List<String> lore = new ArrayList();

                  for(String line : btn.lore) {
                     String processed = line.replace("%level%", String.valueOf(level)).replace("%multiplier%", String.format("%.1f", multiplier)).replace("%next_multiplier%", nextMulti > (double)0.0F ? String.format("%.1f", nextMulti) : "Max").replace("%total_earned%", currencyFormatter.format(totalEarned)).replace("%next_cost%", nextCost > (double)0.0F ? currencyFormatter.format(nextCost) : "Max").replace("%progress%", String.valueOf(progress)).replace("%progress-bar%", progressBar);
                     lore.add(HexColorCode.translateAllColorCodes(processed));
                  }

                  meta.setLore(lore);
                  item.setItemMeta(meta);
               }

               this.inventory.setItem(btn.slot, item);
            }

         }
      }
   }

   private String createProgressBar(int progress) {
      int filledLength = (int)Math.round((double)progress / (double)100.0F * (double)progressBarLength);
      int emptyLength = progressBarLength - filledLength;
      StringBuilder bar = new StringBuilder();

      for(int i = 0; i < filledLength; ++i) {
         bar.append(HexColorCode.translateAllColorCodes(progressBarCompleteColor + progressBarSymbol));
      }

      for(int i = 0; i < emptyLength; ++i) {
         bar.append(HexColorCode.translateAllColorCodes(progressBarLoadingColor + progressBarSymbol));
      }

      return bar.toString();
   }

   public static void loadConfig() {
      File configFile = new File("plugins/HLSMP-Sell/gui/sell.gui.yml");

      try {
         if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            YamlConfiguration config = new YamlConfiguration();
            config.set("inventory.title", "&8ꜱᴇʟʟɢᴜɪ");
            config.set("inventory.rows", 5);
            config.set("buttons.crops.name", "&#00f986ᴄʀᴏᴘꜱ");
            config.set("buttons.crops.slot", 36);
            config.set("buttons.crops.material", "WHEAT");
            config.set("buttons.crops.lore", Arrays.asList("&fSell crops and farming materials to", "&fupgrade ur sell multiplier!", "", "&7Progress to &f%next_multiplier%x", "&8%progress-bar%&r &#00f986%progress%%"));
            config.set("buttons.ores.name", "&#00f986ᴏʀᴇѕ");
            config.set("buttons.ores.slot", 37);
            config.set("buttons.ores.material", "DIAMOND");
            config.set("buttons.ores.lore", Arrays.asList("&fSell ores and mining materials to", "&fupgrade ur sell multiplier!", "", "&7Progress to &f%next_multiplier%x", "&8%progress-bar%&r &#00f986%progress%%"));
            config.set("buttons.mobs.name", "&#00f986ᴍᴏʙ ᴅʀᴏᴘѕ");
            config.set("buttons.mobs.slot", 38);
            config.set("buttons.mobs.material", "BONE");
            config.set("buttons.mobs.lore", Arrays.asList("&fSell mob drops and loot materials to", "&fupgrade ur sell multiplier!", "", "&7Progress to &f%next_multiplier%x", "&8%progress-bar%&r &#00f986%progress%%"));
            config.set("buttons.natural.name", "&#00f986ɴᴀᴛʀᴜᴀʟ ɪᴛᴇᴍѕ");
            config.set("buttons.natural.slot", 39);
            config.set("buttons.natural.material", "OAK_LEAVES");
            config.set("buttons.natural.lore", Arrays.asList("&fSell natural materials and trees materials to", "&fupgrade ur sell multiplier!", "", "&7Progress to &f%next_multiplier%x", "&8%progress-bar%&r &#00f986%progress%%"));
            config.set("buttons.tools.name", "&#00f986ᴀʀᴍᴏʀ ᴀɴᴅ ᴛᴏᴏʟѕ");
            config.set("buttons.tools.slot", 40);
            config.set("buttons.tools.material", "NETHERITE_HELMET");
            config.set("buttons.tools.lore", Arrays.asList("&fSell armor and tools materials to", "&fupgrade ur sell multiplier!", "", "&7Progress to &f%next_multiplier%x", "&8%progress-bar%&r &#00f986%progress%%"));
            config.set("buttons.fish.name", "&#00f986ꜰɪѕʜ");
            config.set("buttons.fish.slot", 41);
            config.set("buttons.fish.material", "TROPICAL_FISH");
            config.set("buttons.fish.lore", Arrays.asList("&fSell fish and sea drops to", "&fupgrade ur sell multiplier!", "", "&7Progress to &f%next_multiplier%x", "&8%progress-bar%&r &#00f986%progress%%"));
            config.set("buttons.books.name", "&#00f986ᴇɴᴄʜᴀɴᴛᴇᴅ ʙᴏᴏᴋѕ");
            config.set("buttons.books.slot", 42);
            config.set("buttons.books.material", "ENCHANTED_BOOK");
            config.set("buttons.books.lore", Arrays.asList("&fSell books and enchanted books to", "&fupgrade ur sell multiplier!", "", "&7Progress to &f%next_multiplier%x", "&8%progress-bar%&r &#00f986%progress%%"));
            config.set("buttons.potions.name", "&#00f986ᴘᴏᴛɪᴏɴѕ");
            config.set("buttons.potions.slot", 43);
            config.set("buttons.potions.material", "POTION");
            config.set("buttons.potions.lore", Arrays.asList("&fSell potions and brewing materials to", "&fupgrade ur sell multiplier!", "", "&7Progress to &f%next_multiplier%x", "&8%progress-bar%&r &#00f986%progress%%"));
            config.set("buttons.blocks.name", "&#00f986ʙʟᴏᴄᴋѕ");
            config.set("buttons.blocks.slot", 44);
            config.set("buttons.blocks.material", "BRICK");
            config.set("buttons.blocks.lore", Arrays.asList("&fSell blocks and placeable items to", "&fupgrade ur sell multiplier!", "", "&7Progress to &f%next_multiplier%x", "&8%progress-bar%&r &#00f986%progress%%"));
            config.save(configFile);
         }

         YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
         guiTitle = config.getString("inventory.title", "&8ꜱᴇʟʟɢᴜɪ");
         guiRows = config.getInt("inventory.rows", 5);
         guiRows = Math.max(1, Math.min(6, guiRows));
         File mainConfig = new File("plugins/DonutSell/config.yml");
         if (mainConfig.exists()) {
            YamlConfiguration mainCfg = YamlConfiguration.loadConfiguration(mainConfig);
            progressBarLoadingColor = mainCfg.getString("progress-bar.loading-color", "&f");
            progressBarCompleteColor = mainCfg.getString("progress-bar.complete-loading-color", "&#20f706");
            progressBarLength = mainCfg.getInt("progress-bar.bar-length", 21);
            progressBarSymbol = mainCfg.getString("progress-bar.bar-symbol", "&m ");
         }

         categoryButtons.clear();
         if (config.contains("buttons")) {
            for(String category : config.getConfigurationSection("buttons").getKeys(false)) {
               String path = "buttons." + category;
               String name = config.getString(path + ".name", category);
               int slot = config.getInt(path + ".slot", -1);
               String materialName = config.getString(path + ".material", "STONE");
               List<String> lore = config.getStringList(path + ".lore");
               Material material = Material.getMaterial(materialName);
               if (material == null) {
                  material = Material.STONE;
               }

               if (slot >= 0 && slot < guiRows * 9) {
                  categoryButtons.put(category, new CategoryButton(category, name, slot, material, lore));
               }
            }
         }
      } catch (Exception var11) {
         guiTitle = "&8ꜱᴇʟʟɢᴜɪ";
         guiRows = 5;
      }

   }

   public static void reloadConfig() {
      loadConfig();
   }

   public boolean isSellSlot(int slot) {
      for(CategoryButton btn : categoryButtons.values()) {
         if (btn.slot == slot) {
            return false;
         }
      }

      return slot >= 0 && slot < guiRows * 9;
   }

   public boolean isBorderSlot(int slot) {
      return false;
   }

   public boolean isInfoSlot(int slot) {
      return this.getCategoryAtSlot(slot) != null;
   }

   public String getCategoryAtSlot(int slot) {
      for(Map.Entry<String, CategoryButton> entry : categoryButtons.entrySet()) {
         if (((CategoryButton)entry.getValue()).slot == slot) {
            return (String)entry.getKey();
         }
      }

      return null;
   }

   public void updateCategoryButtons() {
      if (this.player != null && this.plugin != null) {
         for(CategoryButton btn : categoryButtons.values()) {
            this.inventory.setItem(btn.slot, (ItemStack)null);
         }

         this.addCategoryButtons();
      }

   }

   public @NotNull Inventory getInventory() {
      return this.inventory;
   }

   public static String createItemsSoldString(ItemStack[] items) {
      Map<String, Integer> itemCounts = new HashMap();

      for(ItemStack item : items) {
         if (item != null && !item.getType().isAir()) {
            String itemName = item.getType().name();
            int amount = item.getAmount();
            itemCounts.put(itemName, (Integer)itemCounts.getOrDefault(itemName, 0) + amount);
         }
      }

      StringBuilder sb = new StringBuilder();

      for(Map.Entry entry : itemCounts.entrySet()) {
         if (sb.length() > 0) {
            sb.append(", ");
         }

         sb.append((String)entry.getKey()).append(":").append(entry.getValue());
      }

      return sb.toString();
   }

   private static class CategoryButton {
      final String category;
      final String name;
      final int slot;
      final Material material;
      final List<String> lore;

      CategoryButton(String category, String name, int slot, Material material, List<String> lore) {
         this.category = category;
         this.name = name;
         this.slot = slot;
         this.material = material;
         this.lore = lore;
      }
   }
}
