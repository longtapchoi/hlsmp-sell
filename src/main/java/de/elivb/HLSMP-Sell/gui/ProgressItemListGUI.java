package de.elivb.donutSell.gui;

import de.elivb.donutSell.HexColorCode;
import de.elivb.donutSell.Sell;
import de.elivb.donutSell.manager.PriceManager;
import de.elivb.donutSell.manager.SellManager;
import de.elivb.donutSell.models.PriceModel;
import de.elivb.donutSell.utils.CurrencyFormatter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

public class ProgressItemListGUI implements InventoryHolder {
   private Inventory inventory;
   private final Player player;
   private final Sell plugin;
   private final SellManager sellManager;
   private final PriceManager priceManager;
   private final CurrencyFormatter currencyFormatter;
   private final String category;
   private final String categoryKey;
   private final Material categoryMaterial;
   private static ConfigCache configCache;
   private int currentPage = 0;
   private List<WorthGUI.WorthItem> categoryItems = new ArrayList();
   private final Map<Integer, Material> slotToMaterialMap = new HashMap();

   public ProgressItemListGUI(Player player, Sell plugin, SellManager sellManager, String category, String categoryKey, Material categoryMaterial) {
      this.player = player;
      this.plugin = plugin;
      this.sellManager = sellManager;
      this.priceManager = sellManager.getPriceManager();
      this.currencyFormatter = plugin.getCurrencyFormatter();
      this.category = category;
      this.categoryKey = categoryKey;
      this.categoryMaterial = categoryMaterial;
      if (configCache == null) {
         configCache = ProgressItemListGUI.ConfigCache.load();
      }

      this.loadCategoryItems();
      this.createGUI();
      this.updateGUI();
   }

   private void loadCategoryItems() {
      this.categoryItems.clear();
      Map<String, PriceModel> priceCategories = this.priceManager.getPriceCategories();
      PriceModel targetModel = null;

      for(Map.Entry<String, PriceModel> entry : priceCategories.entrySet()) {
         String catName = ((String)entry.getKey()).toLowerCase();
         String normalizedCategory = this.categoryKey.toLowerCase();
         if (this.matchesCategory(catName, normalizedCategory)) {
            targetModel = (PriceModel)entry.getValue();
            break;
         }
      }

      if (targetModel != null) {
         for(Map.Entry<Material, Double> entry : targetModel.getAllPrices().entrySet()) {
            Material material = (Material)entry.getKey();
            Double price = (Double)entry.getValue();
            if (price != null && price > (double)0.0F && material.isItem() && !material.name().startsWith("POTTED_")) {
               try {
                  ItemStack itemStack = new ItemStack(material);
                  if (itemStack.getType() != Material.AIR) {
                     String displayName = this.formatMaterialName(material);
                     this.categoryItems.add(new WorthGUI.WorthItem(material, itemStack, displayName, price, this.category));
                  }
               } catch (IllegalArgumentException var16) {
               }
            }
         }
      }

      if (this.matchesCategory(this.categoryKey, "books") || this.matchesCategory(this.categoryKey, "enchanted-books")) {
         try {
            Map<String, Double> enchantedBookPrices = this.priceManager.getEnchantedBookPrices();
            if (enchantedBookPrices != null) {
               Iterator var25 = enchantedBookPrices.entrySet().iterator();

               label155:
               while(true) {
                  int lvl;
                  Double price;
                  String[] parts;
                  while(true) {
                     if (!var25.hasNext()) {
                        break label155;
                     }

                     Map.Entry<String, Double> entry = (Map.Entry)var25.next();
                     String key = (String)entry.getKey();
                     price = (Double)entry.getValue();
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
                        this.categoryItems.add(new WorthGUI.WorthItem(Material.ENCHANTED_BOOK, book, "Enchanted Book (" + disp + ")", price, this.category));
                     }
                  }
               }
            }
         } catch (Throwable var20) {
         }
      }

      if (this.matchesCategory(this.categoryKey, "potions")) {
         try {
            Map<String, Double> potionPrices = this.priceManager.getPotionPrices();
            if (potionPrices != null) {
               for(Map.Entry<String, Double> entry : potionPrices.entrySet()) {
                  String key = (String)entry.getKey();
                  Double price = (Double)entry.getValue();
                  if (price != null && !(price <= (double)0.0F)) {
                     String[] parts = key.split(":");
                     if (parts.length >= 2) {
                        String matStr = parts[0];
                        String effectStr = parts[1];
                        Material mat = Material.getMaterial(matStr);
                        if (mat != null && (mat == Material.POTION || mat == Material.SPLASH_POTION || mat == Material.LINGERING_POTION)) {
                           ItemStack potion = new ItemStack(mat);
                           PotionMeta pm = (PotionMeta)potion.getItemMeta();
                           if (pm != null) {
                              try {
                                 PotionType pt = PotionType.valueOf(effectStr);
                                 pm.setBasePotionType(pt);
                              } catch (IllegalArgumentException var17) {
                                 PotionEffectType pet = PotionEffectType.getByName(effectStr);
                                 if (pet != null) {
                                    pm.addCustomEffect(new PotionEffect(pet, 3600, 0), true);
                                 } else {
                                    pm.setBasePotionType(PotionType.WATER);
                                 }
                              }

                              String pn = this.getPotionDisplayName(mat, effectStr);
                              pm.setDisplayName(HexColorCode.translateAllColorCodes("&f" + pn));
                              potion.setItemMeta(pm);
                              this.categoryItems.add(new WorthGUI.WorthItem(mat, potion, pn, price, this.category));
                           }
                        }
                     }
                  }
               }
            }
         } catch (Throwable var18) {
         }
      }

      this.categoryItems.sort((a, b) -> Double.compare(b.getPrice() != null ? b.getPrice() : (double)0.0F, a.getPrice() != null ? a.getPrice() : (double)0.0F));
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

   private boolean matchesCategory(String catName, String targetCategory) {
      if (catName.equalsIgnoreCase(targetCategory)) {
         return true;
      } else {
         switch (targetCategory) {
            case "crops" -> {
               return catName.equalsIgnoreCase("crops") || catName.equalsIgnoreCase("cropprices");
            }
            case "mobs" -> {
               return catName.equalsIgnoreCase("mobs") || catName.equalsIgnoreCase("mobdrops") || catName.equalsIgnoreCase("mob-drops");
            }
            case "ores" -> {
               return catName.equalsIgnoreCase("ores") || catName.equalsIgnoreCase("oreprices");
            }
            case "natural" -> {
               return catName.equalsIgnoreCase("natural") || catName.equalsIgnoreCase("naturalitems") || catName.equalsIgnoreCase("natural-items");
            }
            case "tools" -> {
               return catName.equalsIgnoreCase("tools") || catName.equalsIgnoreCase("armortools") || catName.equalsIgnoreCase("armor-tools");
            }
            case "fish" -> {
               return catName.equalsIgnoreCase("fish") || catName.equalsIgnoreCase("fishprices");
            }
            case "books" -> {
               return catName.equalsIgnoreCase("books") || catName.equalsIgnoreCase("enchantedbooks") || catName.equalsIgnoreCase("enchanted-books");
            }
            case "potions" -> {
               return catName.equalsIgnoreCase("potions") || catName.equalsIgnoreCase("potionprices");
            }
            case "blocks" -> {
               return catName.equalsIgnoreCase("blocks") || catName.equalsIgnoreCase("blockprices");
            }
            default -> {
               return false;
            }
         }
      }
   }

   private void createGUI() {
      String title = (String)configCache.titles.getOrDefault(this.categoryKey, "&8Items");
      title = HexColorCode.translateAllColorCodes(title);
      this.inventory = Bukkit.createInventory(this, configCache.guiRows * 9, title);
   }

   private void updateGUI() {
      this.inventory.clear();
      this.slotToMaterialMap.clear();
      this.updateItems();
      this.addNavigationButtons();
   }

   private void updateItems() {
      if (!this.categoryItems.isEmpty()) {
         int perPage = configCache.worthItemSlots.size();
         int start = this.currentPage * perPage;
         int end = Math.min(start + perPage, this.categoryItems.size());

         for(int i = start; i < end; ++i) {
            int idx = i - start;
            if (idx >= configCache.worthItemSlots.size()) {
               break;
            }

            int slot = (Integer)configCache.worthItemSlots.get(idx);
            WorthGUI.WorthItem wi = (WorthGUI.WorthItem)this.categoryItems.get(i);

            try {
               ItemStack item = this.createDisplayItem(wi);
               this.inventory.setItem(slot, item);
               this.slotToMaterialMap.put(slot, wi.getMaterial());
            } catch (Exception var9) {
            }
         }

      }
   }

   private ItemStack createDisplayItem(WorthGUI.WorthItem wi) {
      ItemStack item = wi.getItemStack().clone();
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         String name = wi.getDisplayName();
         meta.setDisplayName(HexColorCode.translateAllColorCodes(configCache.itemDisplayName.replace("%item-name%", name)));
         List<String> lore = new ArrayList();
         String formattedPrice = wi.getPrice() != null ? this.currencyFormatter.format(wi.getPrice()) : "0";

         for(String line : configCache.itemLore) {
            lore.add(HexColorCode.translateAllColorCodes(line.replace("%price%", formattedPrice).replace("%item-name%", name)));
         }

         meta.setLore(lore);
         item.setItemMeta(meta);
      }

      return item;
   }

   private void addNavigationButtons() {
      int total = this.categoryItems.size();
      int perPage = configCache.worthItemSlots.size();
      int pages = (int)Math.ceil((double)total / (double)perPage);
      if (this.currentPage > 0) {
         this.inventory.setItem(configCache.previousPageSlot, this.createNavigationItem(configCache.previousPageMaterial, configCache.previousPageName, configCache.previousPageLore));
      }

      if (this.currentPage < pages - 1) {
         this.inventory.setItem(configCache.nextPageSlot, this.createNavigationItem(configCache.nextPageMaterial, configCache.nextPageName, configCache.nextPageLore));
      }

      if (configCache.closeButtonSlot != -1) {
         this.inventory.setItem(configCache.closeButtonSlot, this.createNavigationItem(configCache.closeButtonMaterial, configCache.closeButtonName, configCache.closeButtonLore));
      }

   }

   private ItemStack createNavigationItem(Material material, String name, List<String> lore) {
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(HexColorCode.translateAllColorCodes(name));
         if (lore != null && !lore.isEmpty()) {
            List<String> translatedLore = new ArrayList();

            for(String line : lore) {
               translatedLore.add(HexColorCode.translateAllColorCodes(line));
            }

            meta.setLore(translatedLore);
         }

         item.setItemMeta(meta);
      }

      return item;
   }

   public void nextPage() {
      int perPage = configCache.worthItemSlots.size();
      int pages = (int)Math.ceil((double)this.categoryItems.size() / (double)perPage);
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

   public void close() {
      this.sellManager.unregisterProgressItemListGUI(this.player);
      ProgressGUI progressGUI = new ProgressGUI(this.player, this.plugin, this.sellManager, this.category, this.categoryKey, this.categoryMaterial);
      this.sellManager.registerProgressGUI(this.player, progressGUI);
      if (this.sellManager.isFolia()) {
         this.player.getScheduler().run(this.plugin, (scheduledTask) -> this.player.openInventory(progressGUI.getInventory()), (Runnable)null);
      } else {
         this.player.openInventory(progressGUI.getInventory());
      }

   }

   public boolean isPreviousPageSlot(int slot) {
      return slot == configCache.previousPageSlot;
   }

   public boolean isNextPageSlot(int slot) {
      return slot == configCache.nextPageSlot;
   }

   public boolean isCloseButtonSlot(int slot) {
      return configCache.closeButtonSlot != -1 && slot == configCache.closeButtonSlot;
   }

   public boolean isNavigationSlot(int slot) {
      return slot == configCache.previousPageSlot || slot == configCache.nextPageSlot || configCache.closeButtonSlot != -1 && slot == configCache.closeButtonSlot;
   }

   public Material getMaterialInSlot(int slot) {
      return (Material)this.slotToMaterialMap.get(slot);
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

   public static void reloadConfig() {
      configCache = ProgressItemListGUI.ConfigCache.load();
   }

   public @NotNull Inventory getInventory() {
      return this.inventory;
   }

   private static class ConfigCache {
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
      int closeButtonSlot;
      Material closeButtonMaterial;
      String closeButtonName;
      List<String> closeButtonLore;
      String itemDisplayName;
      List<String> itemLore;
      Map<String, String> titles = new HashMap();

      static ConfigCache load() {
         ConfigCache c = new ConfigCache();
         File configFile = new File("plugins/DonutSell/gui/progress.item.list.yml");

         try {
            if (!configFile.exists()) {
               configFile.getParentFile().mkdirs();
               YamlConfiguration cfg = new YamlConfiguration();
               cfg.set("category-item-worth-list.rows", 6);
               cfg.set("category-item-worth-list.worth-items", "0-44");
               cfg.set("category-item-worth-list.titles.crops", "&8ꜰᴀʀᴍ ɪᴛᴇᴍѕ");
               cfg.set("category-item-worth-list.titles.mobs", "&8ᴍᴏʙ ᴅʀᴏᴘѕ");
               cfg.set("category-item-worth-list.titles.ores", "&8ᴏʀᴇѕ");
               cfg.set("category-item-worth-list.titles.natural", "&8ɴᴀᴛᴜʀᴀʟ ɪᴛᴇᴍѕ");
               cfg.set("category-item-worth-list.titles.tools", "&8ᴀʀᴍᴏʀ ᴀɴᴅ ᴛᴏᴏʟѕ");
               cfg.set("category-item-worth-list.titles.fish", "&8ғɪsʜ");
               cfg.set("category-item-worth-list.titles.books", "&8ʙᴏᴏᴋѕ");
               cfg.set("category-item-worth-list.titles.potions", "&8ᴘᴏᴛɪᴏɴѕ");
               cfg.set("category-item-worth-list.titles.blocks", "&8ʙʟᴏᴄᴋѕ");
               cfg.set("category-item-worth-list.navigation.previous-page-slot", 48);
               cfg.set("category-item-worth-list.navigation.previous-page.material", "ARROW");
               cfg.set("category-item-worth-list.navigation.previous-page.displayname", "&#04fc84ᴘʀᴇᴠɪᴏᴜꜱ");
               cfg.set("category-item-worth-list.navigation.previous-page.lore", Arrays.asList("&fClick to go to the previous page"));
               cfg.set("category-item-worth-list.navigation.next-page-slot", 50);
               cfg.set("category-item-worth-list.navigation.next-page.material", "ARROW");
               cfg.set("category-item-worth-list.navigation.next-page.displayname", "&#04fc84ɴᴇxᴛ");
               cfg.set("category-item-worth-list.navigation.next-page.lore", Arrays.asList("&fClick to go to the next page"));
               cfg.set("category-item-worth-list.navigation.close-button.slot", 45);
               cfg.set("category-item-worth-list.navigation.close-button.material", "RED_STAINED_GLASS_PANE");
               cfg.set("category-item-worth-list.navigation.close-button.displayname", "&#FC0000ᴄʟᴏꜱᴇ");
               cfg.set("category-item-worth-list.navigation.close-button.lore", Arrays.asList("&fClick to close the menu"));
               cfg.set("category-item-worth-list.item.displayname", "&f%item-name%");
               cfg.set("category-item-worth-list.item.lore", Arrays.asList("&fPrice: &#0bf52b%price%"));
               cfg.save(configFile);
            }

            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
            String basePath = "category-item-worth-list.";
            c.guiRows = cfg.getInt(basePath + "rows", 6);
            c.worthItemSlots = parseSlotRange(cfg.getString(basePath + "worth-items", "0-44"));
            if (cfg.contains(basePath + "titles")) {
               for(String key : cfg.getConfigurationSection(basePath + "titles").getKeys(false)) {
                  c.titles.put(key, cfg.getString(basePath + "titles." + key, "&8Items"));
               }
            }

            c.previousPageSlot = cfg.getInt(basePath + "navigation.previous-page-slot", 48);
            c.previousPageMaterial = Material.valueOf(cfg.getString(basePath + "navigation.previous-page.material", "ARROW"));
            c.previousPageName = cfg.getString(basePath + "navigation.previous-page.displayname", "&#04fc84ᴘʀᴇᴠɪᴏᴜꜱ");
            c.previousPageLore = translateList(cfg.getStringList(basePath + "navigation.previous-page.lore"));
            c.nextPageSlot = cfg.getInt(basePath + "navigation.next-page-slot", 50);
            c.nextPageMaterial = Material.valueOf(cfg.getString(basePath + "navigation.next-page.material", "ARROW"));
            c.nextPageName = cfg.getString(basePath + "navigation.next-page.displayname", "&#04fc84ɴᴇxᴛ");
            c.nextPageLore = translateList(cfg.getStringList(basePath + "navigation.next-page.lore"));
            c.closeButtonSlot = cfg.getInt(basePath + "navigation.close-button.slot", 45);
            if (c.closeButtonSlot != -1) {
               c.closeButtonMaterial = Material.valueOf(cfg.getString(basePath + "navigation.close-button.material", "RED_STAINED_GLASS_PANE"));
               c.closeButtonName = cfg.getString(basePath + "navigation.close-button.displayname", "&#FC0000ᴄʟᴏꜱᴇ");
               c.closeButtonLore = translateList(cfg.getStringList(basePath + "navigation.close-button.lore"));
            }

            c.itemDisplayName = cfg.getString(basePath + "item.displayname", "&f%item-name%");
            c.itemLore = translateList(cfg.getStringList(basePath + "item.lore"));
         } catch (Exception e) {
            e.printStackTrace();
            c = getDefaultCache();
         }

         return c;
      }

      private static ConfigCache getDefaultCache() {
         ConfigCache c = new ConfigCache();
         c.guiRows = 6;
         c.worthItemSlots = parseSlotRange("0-44");
         c.previousPageSlot = 48;
         c.previousPageMaterial = Material.ARROW;
         c.previousPageName = "&#04fc84ᴘʀᴇᴠɪᴏᴜꜱ";
         c.previousPageLore = translateList(Arrays.asList("&fClick to go to the previous page"));
         c.nextPageSlot = 50;
         c.nextPageMaterial = Material.ARROW;
         c.nextPageName = "&#04fc84ɴᴇxᴛ";
         c.nextPageLore = translateList(Arrays.asList("&fClick to go to the next page"));
         c.closeButtonSlot = 45;
         c.closeButtonMaterial = Material.RED_STAINED_GLASS_PANE;
         c.closeButtonName = "&#FC0000ᴄʟᴏꜱᴇ";
         c.closeButtonLore = translateList(Arrays.asList("&fClick to close the menu"));
         c.itemDisplayName = "&f%item-name%";
         c.itemLore = translateList(Arrays.asList("&fPrice: &#0bf52b%price%"));
         c.titles.put("crops", "&8ꜰᴀʀᴍ ɪᴛᴇᴍѕ");
         c.titles.put("mobs", "&8ᴍᴏʙ ᴅʀᴏᴘѕ");
         c.titles.put("ores", "&8ᴏʀᴇѕ");
         c.titles.put("natural", "&8ɴᴀᴛᴜʀᴀʟ ɪᴛᴇᴍѕ");
         c.titles.put("tools", "&8ᴀʀᴍᴏʀ ᴀɴᴅ ᴛᴏᴏʟѕ");
         c.titles.put("fish", "&8ғɪsʜ");
         c.titles.put("books", "&8ʙᴏᴏᴋѕ");
         c.titles.put("potions", "&8ᴘᴏᴛɪᴏɴѕ");
         c.titles.put("blocks", "&8ʙʟᴏᴄᴋѕ");
         return c;
      }

      private static List<Integer> parseSlotRange(String range) {
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
