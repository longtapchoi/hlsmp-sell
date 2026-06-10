package de.elivb.donutSell;

import org.bukkit.plugin.java.JavaPlugin;

public class LicenseManager {
   private final JavaPlugin plugin;
   private boolean licenseValid = true;

   public LicenseManager(JavaPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean validateLicenseOnStartup() {
      this.licenseValid = true;
      this.plugin.getLogger().info("\u001b[32m[DonutSell] License bypassed - HLSMP\u001b[0m");
      return true;
   }

   public String getServerId() {
      return "HLSMP";
   }

   public String getLicenseKey() {
      return "HLSMP-KEY";
   }

   public boolean isLicenseValid() {
      return true;
   }

   public void setLicenseKey(String key) {
      this.licenseValid = true;
   }
}
