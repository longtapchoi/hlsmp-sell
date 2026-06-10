package de.elivb.donutSell.gui;

import de.elivb.donutSell.HexColorCode;
import de.elivb.donutSell.manager.PriceManager;
import de.elivb.donutSell.manager.SellManager;
import de.elivb.donutSell.models.PriceModel;
import de.elivb.donutSell.utils.CurrencyFormatter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

public class WorthGUI implements InventoryHolder {
   private Inventory inventory;
   private final PriceManager priceManager;
   private final CurrencyFormatter currencyFormatter;
   private final Player player;
   private final SellManager sellManager;
   private String guiTitle;
   private int guiRows;
   private List<Integer> worthItemSlots;
   private int previousPageSlot;
   private Material previousPageMaterial;
   private String previousPageName;
   private List<String> previousPageLore;
   private int nextPageSlot;
   private Material nextPageMaterial;
   private String nextPageName;
   private List<String> nextPageLore;
   private int sortButtonSlot;
   private Material sortButtonMaterial;
   private String sortButtonName;
   private List<String> sortButtonLoreInactive;
   private List<String> sortButtonLoreActive;
   private int refreshButtonSlot;
   private Material refreshButtonMaterial;
   private String refreshButtonName;
   private List<String> refreshButtonLore;
   private int filterButtonSlot;
   private Material filterButtonMaterial;
   private String filterButtonName;
   private List<String> filterButtonLoreInactive;
   private List<String> filterButtonLoreActive;
   private int closeButtonSlot;
   private Material closeButtonMaterial;
   private String closeButtonName;
   private List<String> closeButtonLore;
   private String itemDisplayName;
   private List<String> itemLore;
   private int currentPage = 0;
   private SortType currentSort;
   private FilterType currentFilter;
   private List<WorthItem> allWorthItems;
   private List<WorthItem> filteredItems;
   private final Map<Integer, Material> slotToMaterialMap;
   private final Map<FilterType, List<WorthItem>> categoryItems;

   public WorthGUI(PriceManager priceManager, CurrencyFormatter currencyFormatter, Player player, SellManager sellManager) {
      this.currentSort = WorthGUI.SortType.PRICE_HIGHEST;
      this.currentFilter = WorthGUI.FilterType.ALL;
      this.allWorthItems = Collections.emptyList();
      this.filteredItems = Collections.emptyList();
      this.slotToMaterialMap = new HashMap();
      this.categoryItems = new EnumMap(FilterType.class);
      this.priceManager = priceManager;
      this.currencyFormatter = currencyFormatter;
      this.player = player;
      this.sellManager = sellManager;
      if (WorthGUI.ConfigCache.instance == null) {
         synchronized(ConfigCache.class) {
            if (WorthGUI.ConfigCache.instance == null) {
               WorthGUI.ConfigCache.instance = WorthGUI.ConfigCache.load();
            }
         }
      }

      this.applyConfigCache(WorthGUI.ConfigCache.instance);
      this.loadSellableItems(false);
      this.categorizeItems();
      this.applyFilter();
      this.createGUI();
   }

   private void applyConfigCache(ConfigCache c) {
      this.guiTitle = c.guiTitle;
      this.guiRows = c.guiRows;
      this.worthItemSlots = c.worthItemSlots;
      this.previousPageSlot = c.previousPageSlot;
      this.previousPageMaterial = c.previousPageMaterial;
      this.previousPageName = c.previousPageName;
      this.previousPageLore = c.previousPageLore;
      this.nextPageSlot = c.nextPageSlot;
      this.nextPageMaterial = c.nextPageMaterial;
      this.nextPageName = c.nextPageName;
      this.nextPageLore = c.nextPageLore;
      this.sortButtonSlot = c.sortButtonSlot;
      this.sortButtonMaterial = c.sortButtonMaterial;
      this.sortButtonName = c.sortButtonName;
      this.sortButtonLoreInactive = c.sortButtonLoreInactive;
      this.sortButtonLoreActive = c.sortButtonLoreActive;
      this.refreshButtonSlot = c.refreshButtonSlot;
      this.refreshButtonMaterial = c.refreshButtonMaterial;
      this.refreshButtonName = c.refreshButtonName;
      this.refreshButtonLore = c.refreshButtonLore;
      this.filterButtonSlot = c.filterButtonSlot;
      this.filterButtonMaterial = c.filterButtonMaterial;
      this.filterButtonName = c.filterButtonName;
      this.filterButtonLoreInactive = c.filterButtonLoreInactive;
      this.filterButtonLoreActive = c.filterButtonLoreActive;
      this.closeButtonSlot = c.closeButtonSlot;
      if (this.closeButtonSlot != -1) {
         this.closeButtonMaterial = c.closeButtonMaterial;
         this.closeButtonName = c.closeButtonName;
         this.closeButtonLore = c.closeButtonLore;
      }

      this.itemDisplayName = c.itemDisplayName;
      this.itemLore = c.itemLore;
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

   private void loadSellableItems(boolean force) {
      if (force || this.allWorthItems == null || this.allWorthItems.isEmpty()) {
         List<WorthItem> items = new ArrayList();

         for(Material m : Material.values()) {
            try {
               Double price = this.priceManager.getPrice(m);
               if (price != null && !(price <= (double)0.0F)) {
                  ItemStack is = new ItemStack(m);
                  if (is.getType() == m) {
                     String dn = this.formatMaterialName(m);
                     String cat = this.findMaterialCategory(m);
                     items.add(new WorthItem(m, is, dn, price, cat));
                  }
               }
            } catch (Throwable var16) {
            }
         }

         try {
            Map<String, Double> ench = this.priceManager.getEnchantedBookPrices();
            if (ench != null) {
               Iterator var23 = ench.entrySet().iterator();

               label153:
               while(true) {
                  Double price;
                  String[] parts;
                  int lvl;
                  while(true) {
                     if (!var23.hasNext()) {
                        break label153;
                     }

                     Map.Entry<String, Double> e = (Map.Entry)var23.next();
                     String key = (String)e.getKey();
                     price = (Double)e.getValue();
                     if (price != null && !(price <= (double)0.0F)) {
                        parts = key.split("_");
                        if (parts.length >= 2) {
                           try {
                              lvl = Integer.parseInt(parts[parts.length - 1]);
                              break;
                           } catch (NumberFormatException var19) {
                           }
                        }
                     }
                  }

                  StringBuilder nameBuilder = new StringBuilder();

                  for(int i = 0; i < parts.length - 1; ++i) {
                     if (i > 0) {
                        nameBuilder.append('_');
                     }

                     nameBuilder.append(parts[i]);
                  }

                  String enchName = nameBuilder.toString();
                  Enchantment enchant = Enchantment.getByName(enchName);
                  if (enchant != null) {
                     ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
                     EnchantmentStorageMeta meta = (EnchantmentStorageMeta)book.getItemMeta();
                     if (meta != null) {
                        meta.addStoredEnchant(enchant, lvl, true);
                        String disp = this.formatEnchantmentName(enchant, lvl);
                        book.setItemMeta(meta);
                        items.add(new WorthItem(Material.ENCHANTED_BOOK, book, "Enchanted Book (" + disp + ")", price, "books"));
                     }
                  }
               }
            }
         } catch (Throwable var20) {
         }

         try {
            Map<String, Double> pot = this.priceManager.getPotionPrices();
            if (pot != null) {
               for(Map.Entry<String, Double> en : pot.entrySet()) {
                  String key = (String)en.getKey();
                  Double price = (Double)en.getValue();
                  if (price != null && !(price <= (double)0.0F)) {
                     String[] parts = key.split(":");
                     if (parts.length >= 2) {
                        String matStr = parts[0];
                        String eff = parts[1];
                        Material mat = Material.getMaterial(matStr);
                        if (mat != null && (mat == Material.POTION || mat == Material.SPLASH_POTION || mat == Material.LINGERING_POTION)) {
                           ItemStack potion = new ItemStack(mat);
                           PotionMeta pm = (PotionMeta)potion.getItemMeta();
                           if (pm != null) {
                              try {
                                 PotionType pt = PotionType.valueOf(eff);
                                 pm.setBasePotionType(pt);
                              } catch (IllegalArgumentException var17) {
                                 PotionEffectType pet = PotionEffectType.getByName(eff);
                                 if (pet != null) {
                                    pm.addCustomEffect(new PotionEffect(pet, 3600, 0), true);
                                 } else {
                                    pm.setBasePotionType(PotionType.WATER);
                                 }
                              }

                              String pn = this.getPotionDisplayName(mat, eff);
                              pm.setDisplayName(HexColorCode.translateAllColorCodes("&f" + pn));
                              potion.setItemMeta(pm);
                              items.add(new WorthItem(mat, potion, pn, price, "potions"));
                           }
                        }
                     }
                  }
               }
            }
         } catch (Throwable var18) {
         }

         this.allWorthItems = items;
         this.sortItems();
      }
   }

   private void categorizeItems() {
      this.categoryItems.clear();

      for(FilterType f : WorthGUI.FilterType.values()) {
         this.categoryItems.put(f, new ArrayList());
      }

      for(WorthItem wi : this.allWorthItems) {
         FilterType ft = this.mapCategoryToFilter(wi.getCategory());
         if (ft == null) {
            ft = WorthGUI.FilterType.ALL;
         }

         ((List)this.categoryItems.get(ft)).add(wi);
         ((List)this.categoryItems.get(WorthGUI.FilterType.ALL)).add(wi);
      }

   }

   private FilterType mapCategoryToFilter(String category) {
      if (category == null) {
         return WorthGUI.FilterType.ALL;
      } else {
         switch (category.toLowerCase()) {
            case "blocks" -> {
               return WorthGUI.FilterType.BLOCKS;
            }
            case "books" -> {
               return WorthGUI.FilterType.BOOKS;
            }
            case "crops" -> {
               return WorthGUI.FilterType.CROPS;
            }
            case "fish" -> {
               return WorthGUI.FilterType.FISH;
            }
            case "mobs" -> {
               return WorthGUI.FilterType.MOBS;
            }
            case "natural" -> {
               return WorthGUI.FilterType.NATURAL;
            }
            case "ores" -> {
               return WorthGUI.FilterType.ORES;
            }
            case "potions" -> {
               return WorthGUI.FilterType.POTIONS;
            }
            case "tools" -> {
               return WorthGUI.FilterType.TOOLS;
            }
            default -> {
               return WorthGUI.FilterType.ALL;
            }
         }
      }
   }

   private void applyFilter() {
      List<WorthItem> src = (List)this.categoryItems.get(this.currentFilter);
      if (src == null) {
         this.filteredItems = Collections.emptyList();
      } else {
         this.filteredItems = src;
      }

      this.sortItems();
   }

   private void sortItems() {
      List<WorthItem> list = this.filteredItems != null && !this.filteredItems.isEmpty() ? this.filteredItems : this.allWorthItems;
      if (list != null) {
         switch (this.currentSort.ordinal()) {
            case 0 -> list.sort((a, b) -> Double.compare(b.getPrice() != null ? b.getPrice() : (double)0.0F, a.getPrice() != null ? a.getPrice() : (double)0.0F));
            case 1 -> list.sort(Comparator.comparingDouble((a) -> a.getPrice() != null ? a.getPrice() : (double)0.0F));
            case 2 -> list.sort(Comparator.comparing((a) -> a.getDisplayName().toLowerCase()));
            case 3 -> list.sort(Comparator.comparing((a) -> a.getDisplayName().toLowerCase()).reversed());
         }

      }
   }

   private void createGUI() {
      String title = HexColorCode.translateAllColorCodes(this.guiTitle);
      this.inventory = Bukkit.createInventory(this, this.guiRows * 9, title);
      this.updateGUI();
   }

   private void updateItems() {
      this.slotToMaterialMap.clear();
      if (this.filteredItems != null) {
         List<WorthItem> itemsToDisplay = this.filteredItems;
         int perPage = Math.max(1, this.worthItemSlots.size());
         int start = this.currentPage * perPage;
         int end = Math.min(start + perPage, itemsToDisplay.size());

         for(int i = start; i < end; ++i) {
            int idx = i - start;
            int slot = (Integer)this.worthItemSlots.get(idx);
            WorthItem wi = (WorthItem)itemsToDisplay.get(i);

            try {
               ItemStack it = this.createWorthItem(wi);
               this.inventory.setItem(slot, it);
               this.slotToMaterialMap.put(slot, wi.getMaterial());
            } catch (Exception var10) {
            }
         }

      }
   }

   private ItemStack createWorthItem(WorthItem wi) {
      ItemStack item = wi.getItemStack().clone();
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         String name = wi.getDisplayName();
         if (!meta.hasDisplayName()) {
            meta.setDisplayName(HexColorCode.translateAllColorCodes(this.itemDisplayName.replace("%item-name%", name)));
         }

         List<String> lore = new ArrayList();
         if (meta.hasLore()) {
            List<String> existing = meta.getLore();
            if (existing != null && !existing.isEmpty()) {
               lore.addAll(existing);
               lore.add("");
            }
         }

         String formattedPrice = wi.getPrice() != null ? this.currencyFormatter.format(wi.getPrice()) : "0";

         for(String line : this.itemLore) {
            lore.add(HexColorCode.translateAllColorCodes(line.replace("%price%", formattedPrice).replace("%item-name%", name)));
         }

         meta.setLore(lore);
         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createNavigationItem(Material material, String name, List<String> lore) {
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(HexColorCode.translateAllColorCodes(name));
         List<String> l = new ArrayList();
         if (lore != null) {
            for(String s : lore) {
               l.add(s);
            }
         }

         meta.setLore(l);
         item.setItemMeta(meta);
      }

      return item;
   }

   private void addNavigationButtons() {
      int total = this.filteredItems != null ? this.filteredItems.size() : 0;
      int perPage = Math.max(1, this.worthItemSlots.size());
      int pages = (int)Math.ceil((double)total / (double)perPage);
      if (this.currentPage > 0) {
         this.inventory.setItem(this.previousPageSlot, this.createNavigationItem(this.previousPageMaterial, this.previousPageName, this.previousPageLore));
      }

      if (this.currentPage < pages - 1) {
         this.inventory.setItem(this.nextPageSlot, this.createNavigationItem(this.nextPageMaterial, this.nextPageName, this.nextPageLore));
      }

      this.inventory.setItem(this.sortButtonSlot, this.createNavigationItem(this.sortButtonMaterial, this.sortButtonName, this.createActiveSortLore()));
      this.inventory.setItem(this.refreshButtonSlot, this.createNavigationItem(this.refreshButtonMaterial, this.refreshButtonName, this.refreshButtonLore));
      this.inventory.setItem(this.filterButtonSlot, this.createNavigationItem(this.filterButtonMaterial, this.filterButtonName, this.createActiveFilterLore()));
      if (this.closeButtonSlot != -1) {
         this.inventory.setItem(this.closeButtonSlot, this.createNavigationItem(this.closeButtonMaterial, this.closeButtonName, this.closeButtonLore));
      }

   }

   private List<String> createActiveSortLore() {
      List<String> out = new ArrayList();

      for(int i = 0; i < this.sortButtonLoreInactive.size(); ++i) {
         String base = (String)this.sortButtonLoreInactive.get(i);
         String active = this.sortButtonLoreActive.size() > i ? (String)this.sortButtonLoreActive.get(i) : base;
         String chosen = base;
         if ((base.toLowerCase().contains("highest") || base.contains("ʜɪɡʜᴇsᴛ")) && this.currentSort == WorthGUI.SortType.PRICE_HIGHEST) {
            chosen = active;
         } else if ((base.toLowerCase().contains("lowest") || base.contains("ʟᴏᴡᴇsᴛ")) && this.currentSort == WorthGUI.SortType.PRICE_LOWEST) {
            chosen = active;
         } else if ((base.toLowerCase().contains("a-z") || base.contains("ᴀ-ᴢ")) && this.currentSort == WorthGUI.SortType.NAME_A_Z) {
            chosen = active;
         } else if ((base.toLowerCase().contains("z-a") || base.contains("ᴢ-ᴀ")) && this.currentSort == WorthGUI.SortType.NAME_Z_A) {
            chosen = active;
         }

         out.add(HexColorCode.translateAllColorCodes(chosen));
      }

      return out;
   }

   private List<String> createActiveFilterLore() {
      List<String> out = new ArrayList();
      FilterType[] filterOrder = new FilterType[]{WorthGUI.FilterType.ALL, WorthGUI.FilterType.BLOCKS, WorthGUI.FilterType.BOOKS, WorthGUI.FilterType.CROPS, WorthGUI.FilterType.FISH, WorthGUI.FilterType.MOBS, WorthGUI.FilterType.NATURAL, WorthGUI.FilterType.ORES, WorthGUI.FilterType.POTIONS, WorthGUI.FilterType.TOOLS};

      for(int i = 0; i < this.filterButtonLoreInactive.size() && i < filterOrder.length; ++i) {
         String base = (String)this.filterButtonLoreInactive.get(i);
         String active = this.filterButtonLoreActive.size() > i ? (String)this.filterButtonLoreActive.get(i) : base;
         if (filterOrder[i] == this.currentFilter) {
            out.add(HexColorCode.translateAllColorCodes(active));
         } else {
            out.add(HexColorCode.translateAllColorCodes(base));
         }
      }

      return out;
   }

   public void nextPage() {
      int perPage = Math.max(1, this.worthItemSlots.size());
      int pages = (int)Math.ceil((double)(this.filteredItems != null ? this.filteredItems.size() : 0) / (double)perPage);
      if (this.currentPage < pages - 1) {
         ++this.currentPage;
         this.updateGUI();
         this.player.playSound(this.player.getLocation(), this.sellManager.getClickSound(), 1.0F, 1.0F);
      }

   }

   public void previousPage() {
      if (this.currentPage > 0) {
         --this.currentPage;
         this.updateGUI();
         this.player.playSound(this.player.getLocation(), this.sellManager.getClickSound(), 1.0F, 1.0F);
      }

   }

   public void switchSort() {
      switch (this.currentSort.ordinal()) {
         case 0 -> this.currentSort = WorthGUI.SortType.PRICE_LOWEST;
         case 1 -> this.currentSort = WorthGUI.SortType.NAME_A_Z;
         case 2 -> this.currentSort = WorthGUI.SortType.NAME_Z_A;
         case 3 -> this.currentSort = WorthGUI.SortType.PRICE_HIGHEST;
      }

      this.currentPage = 0;
      this.sortItems();
      this.updateGUI();
      this.player.playSound(this.player.getLocation(), this.sellManager.getClickSound(), 1.0F, 1.0F);
   }

   public void switchFilter() {
      FilterType[] arr = WorthGUI.FilterType.values();
      int idx = (this.currentFilter.ordinal() + 1) % arr.length;
      this.currentFilter = arr[idx];
      this.currentPage = 0;
      this.applyFilter();
      this.updateGUI();
      this.player.playSound(this.player.getLocation(), this.sellManager.getClickSound(), 1.0F, 1.0F);
   }

   public void refreshGUI() {
      this.loadSellableItems(true);
      this.categorizeItems();
      this.applyFilter();
      this.currentPage = 0;
      this.updateGUI();
      this.player.playSound(this.player.getLocation(), this.sellManager.getClickSound(), 1.0F, 1.0F);
   }

   public void updateGUI() {
      if (this.inventory == null) {
         this.createGUI();
      }

      this.inventory.clear();
      this.updateItems();
      this.addNavigationButtons();
   }

   public Material getMaterialInSlot(int slot) {
      return (Material)this.slotToMaterialMap.get(slot);
   }

   public boolean isNavigationSlot(int slot) {
      boolean close = this.closeButtonSlot != -1 && slot == this.closeButtonSlot;
      return slot == this.previousPageSlot || slot == this.nextPageSlot || slot == this.sortButtonSlot || slot == this.refreshButtonSlot || slot == this.filterButtonSlot || close;
   }

   public boolean isPreviousPageSlot(int slot) {
      return slot == this.previousPageSlot;
   }

   public boolean isNextPageSlot(int slot) {
      return slot == this.nextPageSlot;
   }

   public boolean isSortButtonSlot(int slot) {
      return slot == this.sortButtonSlot;
   }

   public boolean isRefreshButtonSlot(int slot) {
      return slot == this.refreshButtonSlot;
   }

   public boolean isFilterButtonSlot(int slot) {
      return slot == this.filterButtonSlot;
   }

   public boolean isCloseButtonSlot(int slot) {
      return this.closeButtonSlot != -1 && slot == this.closeButtonSlot;
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

   private String formatEnchantmentName(Enchantment enchantment, int level) {
      if (enchantment == null) {
         return "Unknown";
      } else {
         String key = enchantment.getKey().getKey();
         String friendly = key.replace('_', ' ');
         String[] parts = friendly.split(" ");
         StringBuilder sb = new StringBuilder();

         for(String p : parts) {
            if (!p.isEmpty()) {
               sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
            }
         }

         String var10000 = sb.toString().trim();
         return var10000 + " " + this.convertToRoman(level);
      }
   }

   private String convertToRoman(int n) {
      switch (n) {
         case 1 -> {
            return "I";
         }
         case 2 -> {
            return "II";
         }
         case 3 -> {
            return "III";
         }
         case 4 -> {
            return "IV";
         }
         case 5 -> {
            return "V";
         }
         default -> {
            return String.valueOf(n);
         }
      }
   }

   private String getPotionDisplayName(Material material, String effectStr) {
      if (effectStr == null) {
         return material.name();
      } else {
         String friendly = effectStr.replace('_', ' ').toLowerCase();
         String[] parts = friendly.split(" ");
         StringBuilder sb = new StringBuilder();

         for(String p : parts) {
            if (!p.isEmpty()) {
               sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
            }
         }

         String base = sb.toString().trim();
         if (material == Material.POTION) {
            return base.equalsIgnoreCase("water") ? "Water Bottle" : base + " Potion";
         } else if (material == Material.SPLASH_POTION) {
            return base.equalsIgnoreCase("water") ? "Splash Water Bottle" : "Splash Potion of " + base;
         } else if (material == Material.LINGERING_POTION) {
            return base.equalsIgnoreCase("water") ? "Lingering Water Bottle" : "Lingering Potion of " + base;
         } else {
            return base;
         }
      }
   }

   private String findMaterialCategory(Material material) {
      if (material == null) {
         return "natural";
      } else if (material == Material.ENCHANTED_BOOK) {
         return "books";
      } else if (material != Material.POTION && material != Material.SPLASH_POTION && material != Material.LINGERING_POTION) {
         Map<String, PriceModel> cats = this.priceManager.getPriceCategories();
         if (cats != null) {
            for(Map.Entry<String, PriceModel> e : cats.entrySet()) {
               try {
                  if (((PriceModel)e.getValue()).hasPrice(material)) {
                     return (String)e.getKey();
                  }
               } catch (Throwable var6) {
               }
            }
         }

         if (material.isBlock()) {
            return "blocks";
         } else {
            String name = material.name().toLowerCase();
            if (name.contains("ore")) {
               return "ores";
            } else if (!name.contains("fish") && !name.contains("salmon") && !name.contains("cod")) {
               return !name.contains("sword") && !name.contains("pickaxe") && !name.contains("axe") && !name.contains("shovel") && !name.contains("hoe") ? "natural" : "tools";
            } else {
               return "fish";
            }
         }
      } else {
         return "potions";
      }
   }

   public @NotNull Inventory getInventory() {
      return this.inventory;
   }

   public static void reloadConfig() {
      WorthGUI.ConfigCache.instance = null;
   }

   public static class WorthItem {
      private final Material material;
      private final ItemStack itemStack;
      private final String displayName;
      private final Double price;
      private final String category;

      public WorthItem(Material material, ItemStack itemStack, String displayName, Double price, String category) {
         this.material = material;
         this.itemStack = itemStack;
         this.displayName = displayName;
         this.price = price;
         this.category = category;
      }

      public Material getMaterial() {
         return this.material;
      }

      public ItemStack getItemStack() {
         return this.itemStack;
      }

      public String getDisplayName() {
         return this.displayName;
      }

      public Double getPrice() {
         return this.price;
      }

      public String getCategory() {
         return this.category;
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

   public static enum FilterType {
      ALL("all"),
      BLOCKS("blocks"),
      BOOKS("books"),
      CROPS("crops"),
      FISH("fish"),
      MOBS("mobs"),
      NATURAL("natural"),
      ORES("ores"),
      POTIONS("potions"),
      TOOLS("tools");

      private final String configName;

      private FilterType(String configName) {
         this.configName = configName;
      }

      public String getConfigName() {
         return this.configName;
      }

      // $FF: synthetic method
      private static FilterType[] $values() {
         return new FilterType[]{ALL, BLOCKS, BOOKS, CROPS, FISH, MOBS, NATURAL, ORES, POTIONS, TOOLS};
      }
   }

   private static class ConfigCache {
      static volatile ConfigCache instance;
      String guiTitle;
      int guiRows;
      List<Integer> worthItemSlots;
      int previousPageSlot;
      Material previousPageMaterial;
      String previousPageName;
      List<String> previousPageLore;
      int nextPageSlot;
      Material nextPageMaterial;
      String nextPageName;
      List<String> nextPageLore;
      int sortButtonSlot;
      Material sortButtonMaterial;
      String sortButtonName;
      List<String> sortButtonLoreInactive;
      List<String> sortButtonLoreActive;
      int refreshButtonSlot;
      Material refreshButtonMaterial;
      String refreshButtonName;
      List<String> refreshButtonLore;
      int filterButtonSlot;
      Material filterButtonMaterial;
      String filterButtonName;
      List<String> filterButtonLoreInactive;
      List<String> filterButtonLoreActive;
      int closeButtonSlot;
      Material closeButtonMaterial;
      String closeButtonName;
      List<String> closeButtonLore;
      String itemDisplayName;
      List<String> itemLore;

      static ConfigCache load() {
         ConfigCache c = new ConfigCache();
         File configFile = new File("plugins/DonutSell/gui/worth.yml");

         try {
            if (!configFile.exists()) {
               configFile.getParentFile().mkdirs();
               YamlConfiguration cfg = new YamlConfiguration();
               cfg.set("worth.title", "&8ᴡᴏʀᴛʜ");
               cfg.set("worth.rows", 6);
               cfg.set("worth.worth-items", "0-44");
               cfg.set("worth.navigation.previous-page-slot", 45);
               cfg.set("worth.navigation.previous-page.material", "ARROW");
               cfg.set("worth.navigation.previous-page.displayname", "&#04fc84ᴘʀᴇᴠɪᴏᴜꜱ");
               cfg.set("worth.navigation.previous-page.lore", Arrays.asList("&fClick to go to the previous page"));
               cfg.set("worth.navigation.next-page-slot", 53);
               cfg.set("worth.navigation.next-page.material", "ARROW");
               cfg.set("worth.navigation.next-page.displayname", "&#04fc84ɴᴇxᴛ");
               cfg.set("worth.navigation.next-page.lore", Arrays.asList("&fClick to go to the next page"));
               cfg.set("worth.navigation.sort-button.slot", 48);
               cfg.set("worth.navigation.sort-button.material", "CAULDRON");
               cfg.set("worth.navigation.sort-button.displayname", "&#04fc84ꜱᴏʀᴛɪɴɢ");
               cfg.set("worth.navigation.sort-button.lore-inactive", Arrays.asList("&f● ᴘʀɪᴄᴇ: ʜɪɡʜᴇsᴛ ғɪʀsᴛ", "&f● ᴘʀɪᴄᴇ: ʟᴏᴡᴇsᴛ ғɪʀsᴛ", "&f● ɴᴀᴍᴇ: ᴀ-ᴢ", "&f● ɴᴀᴍᴇ: ᴢ-ᴀ"));
               cfg.set("worth.navigation.sort-button.lore-active", Arrays.asList("&#04fc84● ᴘʀɪᴄᴇ: ʜɪɡʜᴇsᴛ ғɪʀsᴛ", "&#04fc84● ᴘʀɪᴄᴇ: ʟᴏᴡᴇsᴛ ғɪʀsᴛ", "&#04fc84● ɴᴀᴍᴇ: ᴀ-ᴢ", "&#04fc84● ɴᴀᴍᴇ: ᴢ-ᴀ"));
               cfg.set("worth.navigation.refresh.slot", 49);
               cfg.set("worth.navigation.refresh.material", "ANVIL");
               cfg.set("worth.navigation.refresh.displayname", "&#04fc84ɪᴛᴇᴍ ᴘʀɪᴄᴇꜱ");
               cfg.set("worth.navigation.refresh.lore", Arrays.asList("&fClick to refresh"));
               cfg.set("worth.navigation.filter.slot", 50);
               cfg.set("worth.navigation.filter.material", "HOPPER");
               cfg.set("worth.navigation.filter.displayname", "&#04fc84ꜰɪʟᴛᴇʀ");
               cfg.set("worth.navigation.filter.lore-inactive", Arrays.asList("&f● ᴀʟʟ", "&f● ʙʟᴏᴄᴋꜱ", "&f● ʙᴏᴏᴋꜱ", "&f● ᴄʀᴏᴘꜱ", "&f● ꜰɪꜱʜ", "&f● ᴍᴏʙꜱ", "&f● ɴᴀᴛᴜʀᴀʟ", "&f● ᴏʀᴇꜱ", "&f● ᴘᴏᴛɪᴏɴꜱ", "&f● ᴛᴏᴏʟꜱ"));
               cfg.set("worth.navigation.filter.lore-active", Arrays.asList("&#04fc84● ᴀʟʟ", "&#04fc84● ʙʟᴏᴄᴋꜱ", "&#04fc84● ʙᴏᴏᴋꜱ", "&#04fc84● ᴄʀᴏᴘꜱ", "&#04fc84● ꜰɪꜱʜ", "&#04fc84● ᴍᴏʙꜱ", "&#04fc84● ɴᴀᴛᴜʀᴀʟ", "&#04fc84● ᴏʀᴇꜱ", "&#04fc84● ᴘᴏᴛɪᴏɴꜱ", "&#04fc84● ᴛᴏᴏʟꜱ"));
               cfg.set("worth.navigation.close-button.slot", -1);
               cfg.set("worth.item.displayname", "&f%item-name%");
               cfg.set("worth.item.lore", Arrays.asList("&fPrice: &#0bf52b%price%"));
               cfg.save(configFile);
            }

            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
            c.guiTitle = cfg.getString("worth.title", "&8ᴡᴏʀᴛʜ");
            c.guiRows = cfg.getInt("worth.rows", 6);
            c.worthItemSlots = WorthGUI.parseSlotRangeStatic(cfg.getString("worth.worth-items", "0-44"));
            c.previousPageSlot = cfg.getInt("worth.navigation.previous-page-slot", 45);
            c.previousPageMaterial = Material.valueOf(cfg.getString("worth.navigation.previous-page.material", "ARROW"));
            c.previousPageName = cfg.getString("worth.navigation.previous-page.displayname", "&#04fc84ᴘʀᴇᴠɪᴏᴜꜱ");
            c.previousPageLore = translateList(cfg.getStringList("worth.navigation.previous-page.lore"));
            c.nextPageSlot = cfg.getInt("worth.navigation.next-page-slot", 53);
            c.nextPageMaterial = Material.valueOf(cfg.getString("worth.navigation.next-page.material", "ARROW"));
            c.nextPageName = cfg.getString("worth.navigation.next-page.displayname", "&#04fc84ɴᴇxᴛ");
            c.nextPageLore = translateList(cfg.getStringList("worth.navigation.next-page.lore"));
            c.sortButtonSlot = cfg.getInt("worth.navigation.sort-button.slot", 48);
            c.sortButtonMaterial = Material.valueOf(cfg.getString("worth.navigation.sort-button.material", "CAULDRON"));
            c.sortButtonName = cfg.getString("worth.navigation.sort-button.displayname", "&#04fc84ꜱᴏʀᴛɪɴɢ");
            c.sortButtonLoreInactive = translateList(cfg.getStringList("worth.navigation.sort-button.lore-inactive"));
            c.sortButtonLoreActive = translateList(cfg.getStringList("worth.navigation.sort-button.lore-active"));
            c.refreshButtonSlot = cfg.getInt("worth.navigation.refresh.slot", 49);
            c.refreshButtonMaterial = Material.valueOf(cfg.getString("worth.navigation.refresh.material", "ANVIL"));
            c.refreshButtonName = cfg.getString("worth.navigation.refresh.displayname", "&#04fc84ɪᴛᴇᴍ ᴘʀɪᴄᴇꜱ");
            c.refreshButtonLore = translateList(cfg.getStringList("worth.navigation.refresh.lore"));
            c.filterButtonSlot = cfg.getInt("worth.navigation.filter.slot", 50);
            c.filterButtonMaterial = Material.valueOf(cfg.getString("worth.navigation.filter.material", "HOPPER"));
            c.filterButtonName = cfg.getString("worth.navigation.filter.displayname", "&#04fc84ꜰɪʟᴛᴇʀ");
            c.filterButtonLoreInactive = translateList(cfg.getStringList("worth.navigation.filter.lore-inactive"));
            c.filterButtonLoreActive = translateList(cfg.getStringList("worth.navigation.filter.lore-active"));
            c.closeButtonSlot = cfg.getInt("worth.navigation.close-button.slot", -1);
            if (c.closeButtonSlot != -1) {
               c.closeButtonMaterial = Material.valueOf(cfg.getString("worth.navigation.close-button.material", "RED_DYE"));
               c.closeButtonName = cfg.getString("worth.navigation.close-button.displayname", "&#FC0000ᴄʟᴏꜱᴇ");
               c.closeButtonLore = translateList(cfg.getStringList("worth.navigation.close-button.lore"));
            }

            c.itemDisplayName = cfg.getString("worth.item.displayname", "&f%item-name%");
            c.itemLore = translateList(cfg.getStringList("worth.item.lore"));
         } catch (Exception var3) {
            c = defaultCache();
         }

         return c;
      }

      private static ConfigCache defaultCache() {
         ConfigCache c = new ConfigCache();
         c.guiTitle = "&8ᴡᴏʀᴛʜ";
         c.guiRows = 6;
         c.worthItemSlots = WorthGUI.parseSlotRangeStatic("0-44");
         c.previousPageSlot = 45;
         c.previousPageMaterial = Material.ARROW;
         c.previousPageName = "&#04fc84ᴘʀᴇᴠɪᴏᴜꜱ";
         c.previousPageLore = translateList(Arrays.asList("&fClick to go to the previous page"));
         c.nextPageSlot = 53;
         c.nextPageMaterial = Material.ARROW;
         c.nextPageName = "&#04fc84ɴᴇxᴛ";
         c.nextPageLore = translateList(Arrays.asList("&fClick to go to the next page"));
         c.sortButtonSlot = 48;
         c.sortButtonMaterial = Material.CAULDRON;
         c.sortButtonName = "&#04fc84ꜱᴏʀᴛɪɴɢ";
         c.sortButtonLoreInactive = translateList(Arrays.asList("&f● ᴘʀɪᴄᴇ: ʜɪɡʜᴇsᴛ ғɪʀsᴛ", "&f● ᴘʀɪᴄᴇ: ʟᴏᴡᴇsᴛ ғɪʀsᴛ", "&f● ɴᴀᴍᴇ: ᴀ-ᴢ", "&f● ɴᴀᴍᴇ: ᴢ-ᴀ"));
         c.sortButtonLoreActive = translateList(Arrays.asList("&#04fc84● ᴘʀɪᴄᴇ: ʜɪɡʜᴇsᴛ ғɪʀsᴛ", "&#04fc84● ᴘʀɪᴄᴇ: ʟᴏᴡᴇsᴛ ғɪʀsᴛ", "&#04fc84● ɴᴀᴍᴇ: ᴀ-ᴢ", "&#04fc84● ɴᴀᴍᴇ: ᴢ-ᴀ"));
         c.refreshButtonSlot = 49;
         c.refreshButtonMaterial = Material.ANVIL;
         c.refreshButtonName = "&#04fc84ɪᴛᴇᴍ ᴘʀɪᴄᴇꜱ";
         c.refreshButtonLore = translateList(Arrays.asList("&fClick to refresh"));
         c.filterButtonSlot = 50;
         c.filterButtonMaterial = Material.HOPPER;
         c.filterButtonName = "&#04fc84ꜰɪʟᴛᴇʀ";
         c.filterButtonLoreInactive = translateList(Arrays.asList("&f● ᴀʟʟ", "&f● ʙʟᴏᴄᴋꜱ", "&f● ʙᴏᴏᴋꜱ", "&f● ᴄʀᴏᴘꜱ", "&f● ꜰɪꜱʜ", "&f● ᴍᴏʙꜱ", "&f● ɴᴀᴛᴜʀᴀʟ", "&f● ᴏʀᴇꜱ", "&f● ᴘᴏᴛɪᴏɴꜱ", "&f● ᴛᴏᴏʟꜱ"));
         c.filterButtonLoreActive = translateList(Arrays.asList("&#04fc84● ᴀʟʟ", "&#04fc84● ʙʟᴏᴄᴋꜱ", "&#04fc84● ʙᴏᴏᴋꜱ", "&#04fc84● ᴄʀᴏᴘꜱ", "&#04fc84● ꜰɪꜱʜ", "&#04fc84● ᴍᴏʙꜱ", "&#04fc84● ɴᴀᴛᴜʀᴀʟ", "&#04fc84● ᴏʀᴇꜱ", "&#04fc84● ᴘᴏᴛɪᴏɴꜱ", "&#04fc84● ᴛᴏᴏʟꜱ"));
         c.closeButtonSlot = -1;
         c.itemDisplayName = "&f%item-name%";
         c.itemLore = translateList(Arrays.asList("&fPrice: &#0bf52b%price%"));
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
}
