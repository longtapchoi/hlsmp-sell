package de.elivb.donutSell.manager;

import de.elivb.donutSell.models.PriceModel;
import java.io.File;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public class PriceManager {
   private final Map<String, PriceModel> priceCategories = new HashMap();
   private final Map<String, Double> enchantedBookPrices = new HashMap();
   private final Map<String, Double> potionPrices = new HashMap();
   private final File pricesFolder = new File("plugins/DonutSell/prices");

   public PriceManager() {
      this.loadAllPriceCategories();
   }

   public void loadAllPriceCategories() {
      if (!this.pricesFolder.exists()) {
         this.pricesFolder.mkdirs();
      }

      this.copyDefaultConfigsFromResources();
      File[] priceFiles = this.pricesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
      if (priceFiles != null && priceFiles.length > 0) {
         for(File priceFile : priceFiles) {
            String categoryName = priceFile.getName().replace(".yml", "");
            this.loadPriceCategory(categoryName, priceFile);
         }
      }

   }

   private void copyDefaultConfigsFromResources() {
      String[] defaultFiles = new String[]{"blocks.yml", "ores.yml", "tools.yml", "crops.yml", "books.yml", "fish.yml", "mobs.yml", "natural.yml", "potions.yml"};

      for(String fileName : defaultFiles) {
         File targetFile = new File(this.pricesFolder, fileName);
         if (!targetFile.exists()) {
            try {
               InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(fileName);
               if (inputStream == null) {
                  inputStream = this.getClass().getClassLoader().getResourceAsStream("prices/" + fileName);
               }

               if (inputStream != null) {
                  Files.copy(inputStream, targetFile.toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
               }
            } catch (Exception var8) {
            }
         }
      }

   }

   private void loadPriceCategory(String categoryName, File priceFile) {
      try {
         FileConfiguration config = YamlConfiguration.loadConfiguration(priceFile);
         PriceModel priceModel = new PriceModel(categoryName);
         if (config.contains("prices")) {
            int loaded = 0;
            int enchantedBooksLoaded = 0;
            int potionsLoaded = 0;

            for(String materialName : config.getConfigurationSection("prices").getKeys(false)) {
               try {
                  double price = config.getDouble("prices." + materialName);
                  if (materialName.startsWith("ENCHANTED_BOOK:")) {
                     String enchantmentKey = materialName.substring(15);
                     this.enchantedBookPrices.put(enchantmentKey, price);
                     ++enchantedBooksLoaded;
                  } else if (!materialName.startsWith("POTION:") && !materialName.startsWith("SPLASH_POTION:") && !materialName.startsWith("LINGERING_POTION:")) {
                     Material material = Material.getMaterial(materialName.toUpperCase());
                     if (material != null) {
                        priceModel.addPrice(material, price);
                        ++loaded;
                     }
                  } else {
                     this.potionPrices.put(materialName, price);
                     ++potionsLoaded;
                  }
               } catch (Exception var13) {
               }
            }
         }

         this.priceCategories.put(categoryName, priceModel);
      } catch (Exception var14) {
      }

   }

   public Double getPrice(Material material) {
      for(PriceModel priceModel : this.priceCategories.values()) {
         if (priceModel.hasPrice(material)) {
            return priceModel.getPrice(material);
         }
      }

      return null;
   }

   public Double getPriceWithDurability(ItemStack item) {
      if (item != null && item.getType() != Material.AIR) {
         Double basePrice = null;
         if (item.getType() == Material.ENCHANTED_BOOK) {
            basePrice = this.getEnchantedBookPrice(item);
         } else if (item.getType().toString().contains("POTION")) {
            basePrice = this.getPotionPrice(item);
         } else {
            basePrice = this.getPrice(item.getType());
         }

         if (basePrice != null && !(basePrice <= (double)0.0F)) {
            int maxDurability = item.getType().getMaxDurability();
            if (maxDurability > 0) {
               if (item.hasItemMeta() && item.getItemMeta().isUnbreakable()) {
                  return basePrice;
               } else {
                  int currentDurability = maxDurability - item.getDurability();
                  double durabilityPercent = (double)currentDurability / (double)maxDurability;
                  double multiplier = Math.max(0.05, durabilityPercent);
                  return basePrice * multiplier;
               }
            } else {
               return basePrice;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public Double getEnchantedBookPrice(ItemStack item) {
      if (item != null && item.getType() == Material.ENCHANTED_BOOK && item.hasItemMeta()) {
         EnchantmentStorageMeta meta = (EnchantmentStorageMeta)item.getItemMeta();
         if (!meta.hasStoredEnchants()) {
            return null;
         } else {
            Map<Enchantment, Integer> enchants = meta.getStoredEnchants();
            if (enchants.isEmpty()) {
               return null;
            } else {
               Map.Entry<Enchantment, Integer> enchant = (Map.Entry)enchants.entrySet().iterator().next();
               String enchantmentKey = this.getEnchantmentKey((Enchantment)enchant.getKey(), (Integer)enchant.getValue());
               return (Double)this.enchantedBookPrices.get(enchantmentKey);
            }
         }
      } else {
         return null;
      }
   }

   public Double getPotionPrice(ItemStack item) {
      if (item != null && item.getType().toString().contains("POTION") && item.hasItemMeta() && item.getItemMeta() instanceof PotionMeta) {
         PotionMeta meta = (PotionMeta)item.getItemMeta();
         String potionKey = null;
         if (!meta.getCustomEffects().isEmpty()) {
            PotionEffect primaryEffect = (PotionEffect)meta.getCustomEffects().get(0);
            PotionEffectType effectType = primaryEffect.getType();
            String var10000 = item.getType().name();
            potionKey = var10000 + ":" + effectType.getKey().getKey().toUpperCase();
         } else {
            try {
               if (meta.getBasePotionType() != null) {
                  PotionType baseType = meta.getBasePotionType();
                  String var8 = item.getType().name();
                  potionKey = var8 + ":" + baseType.name();
               }
            } catch (Exception var6) {
               potionKey = item.getType().name() + ":WATER";
            }
         }

         if (potionKey != null) {
            return (Double)this.potionPrices.get(potionKey);
         }
      }

      return null;
   }

   private String getEnchantmentKey(Enchantment enchantment, int level) {
      String enchantName = enchantment.getKey().getKey().toUpperCase();
      return enchantName + "_" + level;
   }

   private String getPotionKey(Material potionMaterial, PotionEffectType effectType) {
      String materialName = potionMaterial.name();
      String effectName = effectType.getKey().getKey().toUpperCase();
      return materialName + ":" + effectName;
   }

   private String getPotionKeyByType(Material potionMaterial, PotionType potionType) {
      String materialName = potionMaterial.name();
      String typeName = potionType.name();
      return materialName + ":" + typeName;
   }

   public boolean hasPrice(Material material) {
      for(PriceModel priceModel : this.priceCategories.values()) {
         if (priceModel.hasPrice(material)) {
            return true;
         }
      }

      return false;
   }

   public boolean hasEnchantedBookPrice(ItemStack item) {
      return this.getEnchantedBookPrice(item) != null;
   }

   public boolean hasPotionPrice(ItemStack item) {
      return this.getPotionPrice(item) != null;
   }

   public void reloadPrices() {
      this.priceCategories.clear();
      this.enchantedBookPrices.clear();
      this.potionPrices.clear();
      this.loadAllPriceCategories();
   }

   public Map<String, PriceModel> getPriceCategories() {
      return new HashMap(this.priceCategories);
   }

   public Map<String, Double> getEnchantedBookPrices() {
      return new HashMap(this.enchantedBookPrices);
   }

   public Map<String, Double> getPotionPrices() {
      return new HashMap(this.potionPrices);
   }
}
