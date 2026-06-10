package de.elivb.donutSell.models;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;

public class PriceModel {
   private final Map<Material, Double> prices;
   private final String categoryName;

   public PriceModel(String categoryName) {
      this.categoryName = categoryName;
      this.prices = new HashMap();
   }

   public void addPrice(Material material, double price) {
      this.prices.put(material, price);
   }

   public Double getPrice(Material material) {
      return (Double)this.prices.get(material);
   }

   public boolean hasPrice(Material material) {
      return this.prices.containsKey(material);
   }

   public Map<Material, Double> getAllPrices() {
      return new HashMap(this.prices);
   }

   public String getCategoryName() {
      return this.categoryName;
   }

   public void clearPrices() {
      this.prices.clear();
   }
}
