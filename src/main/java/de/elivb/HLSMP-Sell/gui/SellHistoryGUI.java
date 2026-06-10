package de.elivb.donutSell.gui;

import de.elivb.donutSell.HexColorCode;
import de.elivb.donutSell.manager.PlayerDataManager;
import de.elivb.donutSell.manager.SellManager;
import de.elivb.donutSell.utils.CurrencyFormatter;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

public class SellHistoryGUI implements InventoryHolder {
   private Inventory inventory;
   private final PlayerDataManager playerDataManager;
   private final CurrencyFormatter currencyFormatter;
   private final Player targetPlayer;
   private final Player viewerPlayer;
   private final SellManager sellManager;
   private final boolean shulkerSupportEnabled;
   private final boolean isOffline;
   private final String offlinePlayerName;
   private final FileConfiguration offlineConfig;
   private final Map<String, Object> offlineStats;
   private String guiTitle;
   private int guiRows;
   private List<Integer> historyItemSlots;
   private String historyItemDisplayName;
   private List<String> historyItemLore;
   private int previousPageSlot;
   private Material previousPageMaterial;
   private String previousPageName;
   private List<String> previousPageLore;
   private int nextPageSlot;
   private Material nextPageMaterial;
   private String nextPageName;
   private List<String> nextPageLore;
   private int statisticsSlot;
   private Material statisticsMaterial;
   private String statisticsName;
   private List<String> statisticsLore;
   private int refreshButtonSlot;
   private Material refreshButtonMaterial;
   private String refreshButtonName;
   private List<String> refreshButtonLore;
   private int sortButtonSlot;
   private Material sortButtonMaterial;
   private String sortButtonName;
   private List<String> sortButtonLoreInactive;
   private List<String> sortButtonLoreActive;
   private int currentPage;
   private SortType currentSort;
   private List<SellHistoryEntry> allHistoryEntries;
   private List<SellHistoryEntry> sortedEntries;
   private final Map<Integer, Material> slotToMaterialMap;

   public SellHistoryGUI(PlayerDataManager playerDataManager, CurrencyFormatter currencyFormatter, Player player, SellManager sellManager) {
      this(playerDataManager, currencyFormatter, player, player, sellManager);
   }

   public SellHistoryGUI(PlayerDataManager playerDataManager, CurrencyFormatter currencyFormatter, Player targetPlayer, Player viewerPlayer, SellManager sellManager) {
      this.currentPage = 0;
      this.currentSort = SellHistoryGUI.SortType.PRICE_HIGHEST;
      this.allHistoryEntries = new ArrayList();
      this.sortedEntries = new ArrayList();
      this.slotToMaterialMap = new HashMap();
      this.playerDataManager = playerDataManager;
      this.currencyFormatter = currencyFormatter;
      this.targetPlayer = targetPlayer;
      this.viewerPlayer = viewerPlayer;
      this.sellManager = sellManager;
      this.shulkerSupportEnabled = sellManager.isShulkerSupportEnabled();
      this.isOffline = false;
      this.offlinePlayerName = null;
      this.offlineConfig = null;
      this.offlineStats = null;
      if (SellHistoryGUI.ConfigCache.instance == null) {
         synchronized(ConfigCache.class) {
            if (SellHistoryGUI.ConfigCache.instance == null) {
               SellHistoryGUI.ConfigCache.instance = SellHistoryGUI.ConfigCache.load();
            }
         }
      }

      this.applyConfigCache(SellHistoryGUI.ConfigCache.instance);
      this.loadHistoryEntries();
      this.sortEntries();
      this.createGUI();
   }

   public SellHistoryGUI(PlayerDataManager playerDataManager, CurrencyFormatter currencyFormatter, String offlinePlayerName, FileConfiguration offlineConfig, Map<String, Object> offlineStats, Player viewerPlayer, SellManager sellManager) {
      this.currentPage = 0;
      this.currentSort = SellHistoryGUI.SortType.PRICE_HIGHEST;
      this.allHistoryEntries = new ArrayList();
      this.sortedEntries = new ArrayList();
      this.slotToMaterialMap = new HashMap();
      this.playerDataManager = playerDataManager;
      this.currencyFormatter = currencyFormatter;
      this.targetPlayer = null;
      this.viewerPlayer = viewerPlayer;
      this.sellManager = sellManager;
      this.shulkerSupportEnabled = sellManager.isShulkerSupportEnabled();
      this.isOffline = true;
      this.offlinePlayerName = offlinePlayerName;
      this.offlineConfig = offlineConfig;
      this.offlineStats = offlineStats;
      if (SellHistoryGUI.ConfigCache.instance == null) {
         synchronized(ConfigCache.class) {
            if (SellHistoryGUI.ConfigCache.instance == null) {
               SellHistoryGUI.ConfigCache.instance = SellHistoryGUI.ConfigCache.load();
            }
         }
      }

      this.applyConfigCache(SellHistoryGUI.ConfigCache.instance);
      this.loadOfflineHistoryEntries();
      this.sortEntries();
      this.createGUI();
   }

   private void applyConfigCache(ConfigCache c) {
      this.guiTitle = c.guiTitle;
      this.guiRows = c.guiRows;
      this.historyItemSlots = c.historyItemSlots;
      this.historyItemDisplayName = c.historyItemDisplayName;
      this.historyItemLore = c.historyItemLore;
      this.previousPageSlot = c.previousPageSlot;
      this.previousPageMaterial = c.previousPageMaterial;
      this.previousPageName = c.previousPageName;
      this.previousPageLore = c.previousPageLore;
      this.nextPageSlot = c.nextPageSlot;
      this.nextPageMaterial = c.nextPageMaterial;
      this.nextPageName = c.nextPageName;
      this.nextPageLore = c.nextPageLore;
      this.statisticsSlot = c.statisticsSlot;
      this.statisticsMaterial = c.statisticsMaterial;
      this.statisticsName = c.statisticsName;
      this.statisticsLore = c.statisticsLore;
      this.refreshButtonSlot = c.refreshButtonSlot;
      this.refreshButtonMaterial = c.refreshButtonMaterial;
      this.refreshButtonName = c.refreshButtonName;
      this.refreshButtonLore = c.refreshButtonLore;
      this.sortButtonSlot = c.sortButtonSlot;
      this.sortButtonMaterial = c.sortButtonMaterial;
      this.sortButtonName = c.sortButtonName;
      this.sortButtonLoreInactive = c.sortButtonLoreInactive;
      this.sortButtonLoreActive = c.sortButtonLoreActive;
   }

   private static List<Integer> parseSlotRangeStatic(String range) {
      List<Integer> slots = new ArrayList();

      try {
         if (range.contains("-")) {
            String[] p = range.split("-");
            int s = Integer.parseInt(p[0]);
            int e = Integer.parseInt(p[1]);

            for(int i = s; i <= e; ++i) {
               slots.add(i);
            }
         } else {
            slots.add(Integer.parseInt(range));
         }
      } catch (Exception var6) {
         for(int i = 0; i <= 44; ++i) {
            slots.add(i);
         }
      }

      return slots;
   }

   private void loadHistoryEntries() {
      this.allHistoryEntries.clear();
      Map<String, Object> logs = this.playerDataManager.getPlayerLogs(this.targetPlayer);
      SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
      Map<String, CombinedItemData> combinedMap = new HashMap();

      for(Map.Entry<String, Object> entry : logs.entrySet()) {
         try {
            String logKey = (String)entry.getKey();
            ConfigurationSection section = this.playerDataManager.getPlayerConfig(this.targetPlayer).getConfigurationSection("logs." + logKey);
            if (section != null) {
               String timestamp = section.getString("timestamp", "");
               String itemsSold = section.getString("itemsSold", "");
               long timeMillis = 0L;

               try {
                  Date date = sdf.parse(timestamp);
                  timeMillis = date.getTime();
               } catch (Exception var20) {
                  timeMillis = Long.parseLong(logKey);
               }

               String[] soldItems = itemsSold.split(", ");

               for(String itemData : soldItems) {
                  String[] parts = itemData.split(":");
                  if (parts.length == 2) {
                     String materialName = parts[0];
                     int amount = Integer.parseInt(parts[1]);
                     this.processNormalItem(materialName, amount, timeMillis, timestamp, combinedMap);
                  }
               }
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

      for(CombinedItemData data : combinedMap.values()) {
         this.allHistoryEntries.add(new SellHistoryEntry(data.material, data.itemStack, data.displayName, data.lastTimestamp, data.lastFormattedDate, data.totalAmount, data.totalPrice));
      }

   }

   private void loadOfflineHistoryEntries() {
      this.allHistoryEntries.clear();
      if (this.offlineConfig != null) {
         SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
         Map<String, Object> logs = new HashMap();
         if (this.offlineConfig.contains("logs")) {
            logs = this.offlineConfig.getConfigurationSection("logs").getValues(false);
         }

         Map<String, CombinedItemData> combinedMap = new HashMap();

         for(Map.Entry<String, Object> entry : logs.entrySet()) {
            try {
               String logKey = (String)entry.getKey();
               ConfigurationSection section = this.offlineConfig.getConfigurationSection("logs." + logKey);
               if (section != null) {
                  String timestamp = section.getString("timestamp", "");
                  String itemsSold = section.getString("itemsSold", "");
                  long timeMillis = 0L;

                  try {
                     Date date = sdf.parse(timestamp);
                     timeMillis = date.getTime();
                  } catch (Exception var20) {
                     timeMillis = Long.parseLong(logKey);
                  }

                  String[] soldItems = itemsSold.split(", ");

                  for(String itemData : soldItems) {
                     String[] parts = itemData.split(":");
                     if (parts.length == 2) {
                        String materialName = parts[0];
                        int amount = Integer.parseInt(parts[1]);
                        this.processNormalItem(materialName, amount, timeMillis, timestamp, combinedMap);
                     }
                  }
               }
            } catch (Exception e) {
               e.printStackTrace();
            }
         }

         for(CombinedItemData data : combinedMap.values()) {
            this.allHistoryEntries.add(new SellHistoryEntry(data.material, data.itemStack, data.displayName, data.lastTimestamp, data.lastFormattedDate, data.totalAmount, data.totalPrice));
         }

      }
   }

   private void processNormalItem(String materialName, int amount, long timestamp, String formattedDate, Map<String, CombinedItemData> combinedMap) {
      Double price = null;
      ItemStack displayItem = null;
      String displayName = "";
      if (materialName.equals("ENCHANTED_BOOK")) {
         displayItem = new ItemStack(Material.ENCHANTED_BOOK);
         displayName = "Enchanted Book";
         Map<String, Double> enchPrices = this.sellManager.getPriceManager().getEnchantedBookPrices();
         if (enchPrices != null && !enchPrices.isEmpty()) {
            price = (Double)enchPrices.values().iterator().next();
         }
      } else if (materialName.contains("POTION")) {
         Material mat = Material.getMaterial(materialName);
         if (mat != null) {
            displayItem = new ItemStack(mat);
            PotionMeta pm = (PotionMeta)displayItem.getItemMeta();
            if (pm != null) {
               pm.setBasePotionType(PotionType.WATER);
               displayItem.setItemMeta(pm);
            }

            displayName = this.formatPotionName(mat);
            Map<String, Double> potPrices = this.sellManager.getPriceManager().getPotionPrices();
            if (potPrices != null && !potPrices.isEmpty()) {
               price = (Double)potPrices.values().iterator().next();
            }
         }
      } else {
         Material material = Material.getMaterial(materialName);
         if (material != null) {
            displayItem = new ItemStack(material);
            displayName = this.formatMaterialName(material);
            price = this.sellManager.getPriceManager().getPrice(material);
         }
      }

      if (displayItem != null && price != null) {
         double totalPrice = price * (double)amount;
         CombinedItemData existing = (CombinedItemData)combinedMap.get(materialName);
         if (existing != null) {
            existing.totalAmount += amount;
            existing.totalPrice += totalPrice;
            if (timestamp > existing.lastTimestamp) {
               existing.lastTimestamp = timestamp;
               existing.lastFormattedDate = formattedDate;
            }
         } else {
            combinedMap.put(materialName, new CombinedItemData(displayItem.getType(), displayItem, displayName, timestamp, formattedDate, amount, totalPrice));
         }

      }
   }

   private void sortEntries() {
      this.sortedEntries = new ArrayList(this.allHistoryEntries);
      switch (this.currentSort.ordinal()) {
         case 0 -> this.sortedEntries.sort((a, b) -> Double.compare(b.getTotalPrice(), a.getTotalPrice()));
         case 1 -> this.sortedEntries.sort(Comparator.comparingDouble(SellHistoryEntry::getTotalPrice));
         case 2 -> this.sortedEntries.sort(Comparator.comparing((SellHistoryEntry a) -> a.getDisplayName().toLowerCase()));
         case 3 -> this.sortedEntries.sort(Comparator.comparing((SellHistoryEntry a) -> a.getDisplayName().toLowerCase()).reversed());
      }

   }

   private void createGUI() {
      String title = HexColorCode.translateAllColorCodes(this.guiTitle);
      this.inventory = Bukkit.createInventory(this, this.guiRows * 9, title);
      this.updateGUI();
   }

   private void updateHistoryItems() {
      this.slotToMaterialMap.clear();
      if (this.sortedEntries != null && !this.sortedEntries.isEmpty()) {
         int perPage = Math.max(1, this.historyItemSlots.size());
         int start = this.currentPage * perPage;
         int end = Math.min(start + perPage, this.sortedEntries.size());

         for(int i = start; i < end; ++i) {
            int idx = i - start;
            int slot = (Integer)this.historyItemSlots.get(idx);
            SellHistoryEntry entry = (SellHistoryEntry)this.sortedEntries.get(i);
            ItemStack item = this.createHistoryItem(entry);
            this.inventory.setItem(slot, item);
            this.slotToMaterialMap.put(slot, entry.getMaterial());
         }

      }
   }

   private ItemStack createHistoryItem(SellHistoryEntry entry) {
      ItemStack item = entry.getItemStack().clone();
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         String displayName = this.historyItemDisplayName.replace("%item_name%", "&f" + entry.getDisplayName());
         meta.setDisplayName(HexColorCode.translateAllColorCodes(displayName));
         List<String> configLore = new ArrayList();

         for(String line : this.historyItemLore) {
            String processed = line.replace("%item_lore%", meta.hasLore() ? String.join("\n", meta.getLore()) : "").replace("%total_price%", this.currencyFormatter.format(entry.getTotalPrice())).replace("%total_amount%", String.valueOf(entry.getTotalAmount()));
            configLore.add(processed);
         }

         List<String> finalLore = new ArrayList();

         for(String line : configLore) {
            String[] lines = line.split("\n");

            for(String l : lines) {
               if (!l.isEmpty()) {
                  finalLore.add(HexColorCode.translateAllColorCodes(l));
               }
            }
         }

         meta.setLore(finalLore);
         item.setItemMeta(meta);
      }

      return item;
   }

   private String formatMaterialName(Material material) {
      String name = material.name().toLowerCase().replace('_', ' ');
      String[] parts = name.split(" ");
      StringBuilder sb = new StringBuilder();

      for(String p : parts) {
         if (!p.isEmpty()) {
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
         }
      }

      return sb.toString().trim();
   }

   private String formatPotionName(Material material) {
      if (material == Material.POTION) {
         return "Potion";
      } else if (material == Material.SPLASH_POTION) {
         return "Splash Potion";
      } else {
         return material == Material.LINGERING_POTION ? "Lingering Potion" : "Potion";
      }
   }

   private String getPlayerName() {
      if (!this.isOffline && this.targetPlayer != null) {
         return this.targetPlayer.getName();
      } else {
         return this.offlinePlayerName != null ? this.offlinePlayerName : "Unknown";
      }
   }

   private double getTotalSold() {
      if (!this.isOffline && this.targetPlayer != null) {
         return this.playerDataManager.getTotalSold(this.targetPlayer);
      } else {
         return this.offlineStats != null && this.offlineStats.containsKey("totalSold") ? (Double)this.offlineStats.get("totalSold") : (double)0.0F;
      }
   }

   private int getTotalItemsSold() {
      if (!this.isOffline && this.targetPlayer != null) {
         return this.playerDataManager.getTotalSoldItems(this.targetPlayer);
      } else {
         return this.offlineStats != null && this.offlineStats.containsKey("totalItems") ? (Integer)this.offlineStats.get("totalItems") : 0;
      }
   }

   private ItemStack createNavigationItem(Material material, String name, List<String> lore) {
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         String playerName = this.getPlayerName();
         String processedName = name.replace("%player%", playerName);
         meta.setDisplayName(HexColorCode.translateAllColorCodes(processedName));
         if (material == Material.PLAYER_HEAD && meta instanceof SkullMeta && !this.isOffline && this.targetPlayer != null) {
            SkullMeta skullMeta = (SkullMeta)meta;
            skullMeta.setOwningPlayer(this.targetPlayer);
         }

         List<String> translatedLore = new ArrayList();
         if (lore != null) {
            for(String line : lore) {
               String processed = line.replace("%donutsell_total_items%", String.valueOf(this.getTotalItemsSold())).replace("%donutsell_formatted_total%", this.currencyFormatter.format(this.getTotalSold())).replace("%player%", playerName);
               translatedLore.add(HexColorCode.translateAllColorCodes(processed));
            }
         }

         meta.setLore(translatedLore);
         item.setItemMeta(meta);
      }

      return item;
   }

   private void addNavigationButtons() {
      this.inventory.setItem(this.previousPageSlot, this.createNavigationItem(this.previousPageMaterial, this.previousPageName, this.previousPageLore));
      this.inventory.setItem(this.nextPageSlot, this.createNavigationItem(this.nextPageMaterial, this.nextPageName, this.nextPageLore));
      this.inventory.setItem(this.statisticsSlot, this.createNavigationItem(this.statisticsMaterial, this.statisticsName, this.statisticsLore));
      this.inventory.setItem(this.refreshButtonSlot, this.createNavigationItem(this.refreshButtonMaterial, this.refreshButtonName, this.refreshButtonLore));
      this.inventory.setItem(this.sortButtonSlot, this.createNavigationItem(this.sortButtonMaterial, this.sortButtonName, this.createActiveSortLore()));
   }

   private List<String> createActiveSortLore() {
      List<String> out = new ArrayList();

      for(int i = 0; i < this.sortButtonLoreInactive.size(); ++i) {
         String base = (String)this.sortButtonLoreInactive.get(i);
         String active = this.sortButtonLoreActive.size() > i ? (String)this.sortButtonLoreActive.get(i) : base;
         String chosen = base;
         String lower = base.toLowerCase();
         if ((lower.contains("highest") || lower.contains("ʜɪɢʜᴇsᴛ")) && this.currentSort == SellHistoryGUI.SortType.PRICE_HIGHEST) {
            chosen = active;
         } else if ((lower.contains("lowest") || lower.contains("ʟᴏᴡᴇsᴛ")) && this.currentSort == SellHistoryGUI.SortType.PRICE_LOWEST) {
            chosen = active;
         } else if ((lower.contains("a-z") || lower.contains("ᴀ-ᴢ")) && this.currentSort == SellHistoryGUI.SortType.NAME_A_Z) {
            chosen = active;
         } else if ((lower.contains("z-a") || lower.contains("ᴢ-ᴀ")) && this.currentSort == SellHistoryGUI.SortType.NAME_Z_A) {
            chosen = active;
         }

         out.add(HexColorCode.translateAllColorCodes(chosen));
      }

      return out;
   }

   public void updateGUI() {
      if (this.inventory == null) {
         this.createGUI();
      }

      this.inventory.clear();
      this.updateHistoryItems();
      this.addNavigationButtons();
   }

   public void refreshHistory() {
      if (!this.isOffline && this.targetPlayer != null) {
         this.loadHistoryEntries();
      } else {
         this.loadOfflineHistoryEntries();
      }

      this.sortEntries();
      this.currentPage = 0;
      this.updateGUI();
      if (this.viewerPlayer != null) {
         this.viewerPlayer.playSound(this.viewerPlayer.getLocation(), this.sellManager.getClickSound(), 1.0F, 1.0F);
      }

   }

   public void nextPage() {
      int perPage = Math.max(1, this.historyItemSlots.size());
      int pages = (int)Math.ceil((double)(this.sortedEntries != null ? this.sortedEntries.size() : 0) / (double)perPage);
      if (this.currentPage < pages - 1) {
         ++this.currentPage;
         this.updateGUI();
         if (this.viewerPlayer != null) {
            this.viewerPlayer.playSound(this.viewerPlayer.getLocation(), this.sellManager.getClickSound(), 1.0F, 1.0F);
         }
      }

   }

   public void previousPage() {
      if (this.currentPage > 0) {
         --this.currentPage;
         this.updateGUI();
         if (this.viewerPlayer != null) {
            this.viewerPlayer.playSound(this.viewerPlayer.getLocation(), this.sellManager.getClickSound(), 1.0F, 1.0F);
         }
      }

   }

   public void switchSort() {
      switch (this.currentSort.ordinal()) {
         case 0 -> this.currentSort = SellHistoryGUI.SortType.PRICE_LOWEST;
         case 1 -> this.currentSort = SellHistoryGUI.SortType.NAME_A_Z;
         case 2 -> this.currentSort = SellHistoryGUI.SortType.NAME_Z_A;
         case 3 -> this.currentSort = SellHistoryGUI.SortType.PRICE_HIGHEST;
      }

      this.currentPage = 0;
      this.sortEntries();
      this.updateGUI();
      if (this.viewerPlayer != null) {
         this.viewerPlayer.playSound(this.viewerPlayer.getLocation(), this.sellManager.getClickSound(), 1.0F, 1.0F);
      }

   }

   public boolean isPreviousPageSlot(int slot) {
      return slot == this.previousPageSlot;
   }

   public boolean isNextPageSlot(int slot) {
      return slot == this.nextPageSlot;
   }

   public boolean isStatisticsSlot(int slot) {
      return slot == this.statisticsSlot;
   }

   public boolean isRefreshButtonSlot(int slot) {
      return slot == this.refreshButtonSlot;
   }

   public boolean isSortButtonSlot(int slot) {
      return slot == this.sortButtonSlot;
   }

   public boolean isNavigationSlot(int slot) {
      return slot == this.previousPageSlot || slot == this.nextPageSlot || slot == this.statisticsSlot || slot == this.refreshButtonSlot || slot == this.sortButtonSlot;
   }

   public Material getMaterialInSlot(int slot) {
      return (Material)this.slotToMaterialMap.get(slot);
   }

   public @NotNull Inventory getInventory() {
      return this.inventory;
   }

   public static void reloadConfig() {
      SellHistoryGUI.ConfigCache.instance = null;
   }

   private static class SellHistoryEntry {
      final Material material;
      final ItemStack itemStack;
      final String displayName;
      final long timestamp;
      final String formattedDate;
      final int totalAmount;
      final double totalPrice;

      SellHistoryEntry(Material material, ItemStack itemStack, String displayName, long timestamp, String formattedDate, int totalAmount, double totalPrice) {
         this.material = material;
         this.itemStack = itemStack;
         this.displayName = displayName;
         this.timestamp = timestamp;
         this.formattedDate = formattedDate;
         this.totalAmount = totalAmount;
         this.totalPrice = totalPrice;
      }

      Material getMaterial() {
         return this.material;
      }

      ItemStack getItemStack() {
         return this.itemStack;
      }

      String getDisplayName() {
         return this.displayName;
      }

      long getTimestamp() {
         return this.timestamp;
      }

      String getFormattedDate() {
         return this.formattedDate;
      }

      int getTotalAmount() {
         return this.totalAmount;
      }

      double getTotalPrice() {
         return this.totalPrice;
      }
   }

   public static enum SortType {
      PRICE_HIGHEST,
      PRICE_LOWEST,
      NAME_A_Z,
      NAME_Z_A;

      // $FF: synthetic method
      private static SortType[] $values() {
         return new SortType[]{PRICE_HIGHEST, PRICE_LOWEST, NAME_A_Z, NAME_Z_A};
      }
   }

   private static class ConfigCache {
      static volatile ConfigCache instance;
      String guiTitle;
      int guiRows;
      List<Integer> historyItemSlots;
      String historyItemDisplayName;
      List<String> historyItemLore;
      int previousPageSlot;
      Material previousPageMaterial;
      String previousPageName;
      List<String> previousPageLore;
      int nextPageSlot;
      Material nextPageMaterial;
      String nextPageName;
      List<String> nextPageLore;
      int statisticsSlot;
      Material statisticsMaterial;
      String statisticsName;
      List<String> statisticsLore;
      int refreshButtonSlot;
      Material refreshButtonMaterial;
      String refreshButtonName;
      List<String> refreshButtonLore;
      int sortButtonSlot;
      Material sortButtonMaterial;
      String sortButtonName;
      List<String> sortButtonLoreInactive;
      List<String> sortButtonLoreActive;

      static ConfigCache load() {
         ConfigCache c = new ConfigCache();
         File configFile = new File("plugins/HLSMP-Sell/gui/sellhistory.yml");

         try {
            if (!configFile.exists()) {
               configFile.getParentFile().mkdirs();
               YamlConfiguration cfg = new YamlConfiguration();
               cfg.set("sellhistory.title", "&8ꜱᴇʟʟ ʜɪꜱᴛᴏʀʏ");
               cfg.set("sellhistory.rows", 6);
               cfg.set("sellhistory.source-item.slots", "0-44");
               cfg.set("sellhistory.source-item.displayname", "%item_name%");
               cfg.set("sellhistory.source-item.lore", Arrays.asList("%item_lore%", "", "&fTotal price: &#04fc84%total_price%", "&fTotal amount: &#04fc84%total_amount%"));
               cfg.set("sellhistory.navigation.previous-page-slot", 45);
               cfg.set("sellhistory.navigation.previous-page.material", "ARROW");
               cfg.set("sellhistory.navigation.previous-page.displayname", "&#04fc84ᴘʀᴇᴠɪᴏᴜꜱ");
               cfg.set("sellhistory.navigation.previous-page.lore", Arrays.asList("&fClick to go to the previous page"));
               cfg.set("sellhistory.navigation.next-page-slot", 53);
               cfg.set("sellhistory.navigation.next-page.material", "ARROW");
               cfg.set("sellhistory.navigation.next-page.displayname", "&#04fc84ɴᴇxᴛ");
               cfg.set("sellhistory.navigation.next-page.lore", Arrays.asList("&fClick to go to the next page"));
               cfg.set("sellhistory.navigation.statistics.slot", 48);
               cfg.set("sellhistory.navigation.statistics.material", "PLAYER_HEAD");
               cfg.set("sellhistory.navigation.statistics.displayname", "&#04fc84ꜱᴛᴀᴛɪꜱᴛɪᴄꜱ");
               cfg.set("sellhistory.navigation.statistics.lore", Arrays.asList("&fTotal Items Sold: &#04fc84%donutsell_total_items%", "&fTotal Money Earned: &#04fc84%donutsell_formatted_total%"));
               cfg.set("sellhistory.navigation.refresh.slot", 49);
               cfg.set("sellhistory.navigation.refresh.material", "ANVIL");
               cfg.set("sellhistory.navigation.refresh.displayname", "&#04fc84ʀᴇꜰʀᴇꜱʜ");
               cfg.set("sellhistory.navigation.refresh.lore", Arrays.asList("&fClick to refresh"));
               cfg.set("sellhistory.navigation.sort-button.slot", 50);
               cfg.set("sellhistory.navigation.sort-button.material", "CAULDRON");
               cfg.set("sellhistory.navigation.sort-button.displayname", "&#04fc84ꜱᴏʀᴛɪɴɢ");
               cfg.set("sellhistory.navigation.sort-button.lore-inactive", Arrays.asList("&f● ᴘʀɪᴄᴇ: ʜɪɢʜᴇsᴛ ғɪʀsᴛ", "&f● ᴘʀɪᴄᴇ: ʟᴏᴡᴇsᴛ ғɪʀsᴛ", "&f● ɴᴀᴍᴇ: ᴀ-ᴢ", "&f● ɴᴀᴍᴇ: ᴢ-ᴀ"));
               cfg.set("sellhistory.navigation.sort-button.lore-active", Arrays.asList("&#04fc84● ᴘʀɪᴄᴇ: ʜɪɢʜᴇsᴛ ғɪʀsᴛ", "&#04fc84● ᴘʀɪᴄᴇ: ʟᴏᴡᴇsᴛ ғɪʀsᴛ", "&#04fc84● ɴᴀᴍᴇ: ᴀ-ᴢ", "&#04fc84● ɴᴀᴍᴇ: ᴢ-ᴀ"));
               cfg.save(configFile);
            }

            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
            c.guiTitle = cfg.getString("sellhistory.title", "&8ꜱᴇʟʟ ʜɪꜱᴛᴏʀʏ");
            c.guiRows = cfg.getInt("sellhistory.rows", 6);
            c.historyItemSlots = SellHistoryGUI.parseSlotRangeStatic(cfg.getString("sellhistory.source-item.slots", "0-44"));
            c.historyItemDisplayName = cfg.getString("sellhistory.source-item.displayname", "%item_name%");
            c.historyItemLore = cfg.getStringList("sellhistory.source-item.lore");
            c.previousPageSlot = cfg.getInt("sellhistory.navigation.previous-page-slot", 45);
            c.previousPageMaterial = Material.valueOf(cfg.getString("sellhistory.navigation.previous-page.material", "ARROW"));
            c.previousPageName = cfg.getString("sellhistory.navigation.previous-page.displayname", "&#04fc84ᴘʀᴇᴠɪᴏᴜꜱ");
            c.previousPageLore = translateList(cfg.getStringList("sellhistory.navigation.previous-page.lore"));
            c.nextPageSlot = cfg.getInt("sellhistory.navigation.next-page-slot", 53);
            c.nextPageMaterial = Material.valueOf(cfg.getString("sellhistory.navigation.next-page.material", "ARROW"));
            c.nextPageName = cfg.getString("sellhistory.navigation.next-page.displayname", "&#04fc84ɴᴇxᴛ");
            c.nextPageLore = translateList(cfg.getStringList("sellhistory.navigation.next-page.lore"));
            c.statisticsSlot = cfg.getInt("sellhistory.navigation.statistics.slot", 48);
            c.statisticsMaterial = Material.valueOf(cfg.getString("sellhistory.navigation.statistics.material", "PLAYER_HEAD"));
            c.statisticsName = cfg.getString("sellhistory.navigation.statistics.displayname", "&#04fc84ꜱᴛᴀᴛɪꜱᴛɪᴄꜱ");
            c.statisticsLore = translateList(cfg.getStringList("sellhistory.navigation.statistics.lore"));
            c.refreshButtonSlot = cfg.getInt("sellhistory.navigation.refresh.slot", 49);
            c.refreshButtonMaterial = Material.valueOf(cfg.getString("sellhistory.navigation.refresh.material", "ANVIL"));
            c.refreshButtonName = cfg.getString("sellhistory.navigation.refresh.displayname", "&#04fc84ʀᴇꜰʀᴇꜱʜ");
            c.refreshButtonLore = translateList(cfg.getStringList("sellhistory.navigation.refresh.lore"));
            c.sortButtonSlot = cfg.getInt("sellhistory.navigation.sort-button.slot", 50);
            c.sortButtonMaterial = Material.valueOf(cfg.getString("sellhistory.navigation.sort-button.material", "CAULDRON"));
            c.sortButtonName = cfg.getString("sellhistory.navigation.sort-button.displayname", "&#04fc84ꜱᴏʀᴛɪɴɢ");
            c.sortButtonLoreInactive = translateList(cfg.getStringList("sellhistory.navigation.sort-button.lore-inactive"));
            c.sortButtonLoreActive = translateList(cfg.getStringList("sellhistory.navigation.sort-button.lore-active"));
         } catch (Exception var3) {
            c = defaultCache();
         }

         return c;
      }

      private static ConfigCache defaultCache() {
         ConfigCache c = new ConfigCache();
         c.guiTitle = "&8ꜱᴇʟʟ ʜɪꜱᴛᴏʀʏ";
         c.guiRows = 6;
         c.historyItemSlots = SellHistoryGUI.parseSlotRangeStatic("0-44");
         c.historyItemDisplayName = "%item_name%";
         c.historyItemLore = Arrays.asList("%item_lore%", "", "&fTotal price: &#04fc84%total_price%", "&fTotal amount: &#04fc84%total_amount%");
         c.previousPageSlot = 45;
         c.previousPageMaterial = Material.ARROW;
         c.previousPageName = "&#04fc84ᴘʀᴇᴠɪᴏᴜꜱ";
         c.previousPageLore = translateList(Arrays.asList("&fClick to go to the previous page"));
         c.nextPageSlot = 53;
         c.nextPageMaterial = Material.ARROW;
         c.nextPageName = "&#04fc84ɴᴇxᴛ";
         c.nextPageLore = translateList(Arrays.asList("&fClick to go to the next page"));
         c.statisticsSlot = 48;
         c.statisticsMaterial = Material.PLAYER_HEAD;
         c.statisticsName = "&#04fc84ꜱᴛᴀᴛɪꜱᴛɪᴄꜱ";
         c.statisticsLore = translateList(Arrays.asList("&fTotal Items Sold: &#04fc84%donutsell_total_items%", "&fTotal Money Earned: &#04fc84%donutsell_formatted_total%"));
         c.refreshButtonSlot = 49;
         c.refreshButtonMaterial = Material.ANVIL;
         c.refreshButtonName = "&#04fc84ʀᴇꜰʀᴇꜱʜ";
         c.refreshButtonLore = translateList(Arrays.asList("&fClick to refresh"));
         c.sortButtonSlot = 50;
         c.sortButtonMaterial = Material.CAULDRON;
         c.sortButtonName = "&#04fc84ꜱᴏʀᴛɪɴɢ";
         c.sortButtonLoreInactive = translateList(Arrays.asList("&f● ᴘʀɪᴄᴇ: ʜɪɢʜᴇsᴛ ғɪʀsᴛ", "&f● ᴘʀɪᴄᴇ: ʟᴏᴡᴇsᴛ ғɪʀsᴛ", "&f● ɴᴀᴍᴇ: ᴀ-ᴢ", "&f● ɴᴀᴍᴇ: ᴢ-ᴀ"));
         c.sortButtonLoreActive = translateList(Arrays.asList("&#04fc84● ᴘʀɪᴄᴇ: ʜɪɢʜᴇsᴛ ғɪʀsᴛ", "&#04fc84● ᴘʀɪᴄᴇ: ʟᴏᴡᴇsᴛ ғɪʀsᴛ", "&#04fc84● ɴᴀᴍᴇ: ᴀ-ᴢ", "&#04fc84● ɴᴀᴍᴇ: ᴢ-ᴀ"));
         return c;
      }

      private static List<String> translateList(List<String> input) {
         if (input == null) {
            return Collections.emptyList();
         } else {
            List<String> out = new ArrayList(input.size());

            for(String s : input) {
               out.add(HexColorCode.translateAllColorCodes(s));
            }

            return out;
         }
      }
   }

   private static class CombinedItemData {
      Material material;
      ItemStack itemStack;
      String displayName;
      long lastTimestamp;
      String lastFormattedDate;
      int totalAmount;
      double totalPrice;

      CombinedItemData(Material material, ItemStack itemStack, String displayName, long lastTimestamp, String lastFormattedDate, int totalAmount, double totalPrice) {
         this.material = material;
         this.itemStack = itemStack;
         this.displayName = displayName;
         this.lastTimestamp = lastTimestamp;
         this.lastFormattedDate = lastFormattedDate;
         this.totalAmount = totalAmount;
         this.totalPrice = totalPrice;
      }
   }
}
