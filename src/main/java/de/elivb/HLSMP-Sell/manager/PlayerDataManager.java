package de.elivb.donutSell.manager;

import de.elivb.donutSell.Sell;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class PlayerDataManager {
   private final Sell plugin;
   private final File playerDataFolder;
   private final Map<UUID, FileConfiguration> playerConfigs;
   private final Map<UUID, File> playerFiles;
   private final MySQLManager mysqlManager;
   private boolean useMySQL;

   public PlayerDataManager(Sell plugin) {
      this.plugin = plugin;
      this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
      this.playerConfigs = new HashMap();
      this.playerFiles = new HashMap();
      this.mysqlManager = new MySQLManager(plugin);
      this.useMySQL = this.mysqlManager.isEnabled();
      if (this.useMySQL) {
         this.mysqlManager.connect();
      } else {
         this.createFolder();
      }

   }

   public MySQLManager getMySQLManager() {
      return this.mysqlManager;
   }

   private void createFolder() {
      if (!this.playerDataFolder.exists()) {
         this.playerDataFolder.mkdirs();
      }

   }

   public void loadPlayerConfig(Player player) {
      if (this.useMySQL) {
         UUID uuid = player.getUniqueId();
         Map<String, Object> data = this.mysqlManager.loadPlayerData(uuid);
         FileConfiguration tempConfig = new YamlConfiguration();
         tempConfig.set("name", player.getName());
         tempConfig.set("uuid", uuid.toString());
         tempConfig.set("totalSold", data.getOrDefault("totalSold", (double)0.0F));
         tempConfig.set("totalItems", data.getOrDefault("totalItems", 0));
         if (data.containsKey("levelData")) {
            Map<String, Object> levelData = (Map)data.get("levelData");

            for(Map.Entry<String, Object> entry : levelData.entrySet()) {
               tempConfig.set("levels." + (String)entry.getKey(), entry.getValue());
            }
         }

         this.playerConfigs.put(uuid, tempConfig);
      } else {
         this.loadPlayerConfig(player.getUniqueId(), player.getName());
      }

   }

   public void loadPlayerConfig(UUID uuid, String playerName) {
      File playerFile = new File(this.playerDataFolder, playerName + ".yml");
      FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
      this.playerConfigs.put(uuid, playerConfig);
      this.playerFiles.put(uuid, playerFile);
      if (!playerConfig.contains("name")) {
         playerConfig.set("name", playerName);
         playerConfig.set("uuid", uuid.toString());
         playerConfig.set("totalSold", (double)0.0F);
         playerConfig.set("totalItems", 0);
         this.savePlayerConfig(uuid);
      }

   }

   public void savePlayerConfig(Player player) {
      this.savePlayerConfig(player.getUniqueId());
   }

   public void savePlayerConfig(UUID uuid) {
      if (!this.useMySQL) {
         if (this.playerConfigs.containsKey(uuid) && this.playerFiles.containsKey(uuid)) {
            try {
               ((FileConfiguration)this.playerConfigs.get(uuid)).save((File)this.playerFiles.get(uuid));
            } catch (IOException var11) {
            }
         }
      } else if (this.playerConfigs.containsKey(uuid)) {
         FileConfiguration config = (FileConfiguration)this.playerConfigs.get(uuid);
         String playerName = config.getString("name", "Unknown");
         double totalSold = config.getDouble("totalSold", (double)0.0F);
         int totalItems = config.getInt("totalItems", 0);
         Map<String, Object> levelData = new HashMap();
         if (config.contains("levels")) {
            for(String category : config.getConfigurationSection("levels").getKeys(false)) {
               Map<String, Object> catData = new HashMap();
               catData.put("totalEarned", config.getDouble("levels." + category + ".totalEarned", (double)0.0F));
               catData.put("level", config.getInt("levels." + category + ".level", 0));
               catData.put("multiplier", config.getDouble("levels." + category + ".multiplier", (double)1.0F));
               levelData.put(category, catData);
            }
         }

         this.mysqlManager.savePlayerData(uuid, playerName, totalSold, totalItems, levelData);
      }

   }

   public void addSellLog(Player player, int itemCount, double totalPrice, String itemsSold) {
      this.addSellLog(player.getUniqueId(), itemCount, totalPrice, itemsSold);
   }

   public void addSellLog(UUID uuid, int itemCount, double totalPrice, String itemsSold) {
      this.addSellLog(uuid, itemCount, totalPrice, itemsSold, (Map)null);
   }

   public void addSellLog(UUID uuid, int itemCount, double totalPrice, String itemsSold, Map<String, Integer> shulkerContents) {
      if (this.useMySQL) {
         this.mysqlManager.saveSellLog(uuid, itemCount, totalPrice, itemsSold, shulkerContents);
         if (this.playerConfigs.containsKey(uuid)) {
            FileConfiguration config = (FileConfiguration)this.playerConfigs.get(uuid);
            double currentTotal = config.getDouble("totalSold", (double)0.0F);
            config.set("totalSold", currentTotal + totalPrice);
            int currentItems = config.getInt("totalItems", 0);
            config.set("totalItems", currentItems + itemCount);
         }
      } else if (this.playerConfigs.containsKey(uuid)) {
         FileConfiguration config = (FileConfiguration)this.playerConfigs.get(uuid);
         String timestamp = (new SimpleDateFormat("dd.MM.yyyy HH:mm:ss")).format(new Date());
         String logKey = "logs." + System.currentTimeMillis();
         config.set(logKey + ".timestamp", timestamp);
         config.set(logKey + ".items", itemCount);
         config.set(logKey + ".price", totalPrice);
         config.set(logKey + ".itemsSold", itemsSold);
         if (shulkerContents != null && !shulkerContents.isEmpty()) {
            for(Map.Entry<String, Integer> entry : shulkerContents.entrySet()) {
               config.set(logKey + ".shulkerContents." + (String)entry.getKey(), entry.getValue());
            }
         }

         double currentTotal = config.getDouble("totalSold", (double)0.0F);
         config.set("totalSold", currentTotal + totalPrice);
         int currentItems = config.getInt("totalItems", 0);
         config.set("totalItems", currentItems + itemCount);
         this.limitLogSize(uuid);
         this.savePlayerConfig(uuid);
      }

   }

   public Map<String, Integer> getShulkerContents(UUID uuid, String logKey) {
      Map<String, Integer> contents = new HashMap();
      if (this.useMySQL) {
         return contents;
      } else {
         if (this.playerConfigs.containsKey(uuid)) {
            FileConfiguration config = (FileConfiguration)this.playerConfigs.get(uuid);
            String path = "logs." + logKey + ".shulkerContents";
            if (config.contains(path)) {
               ConfigurationSection section = config.getConfigurationSection(path);
               if (section != null) {
                  for(String key : section.getKeys(false)) {
                     contents.put(key, section.getInt(key));
                  }
               }
            }
         }

         return contents;
      }
   }

   private void limitLogSize(UUID uuid) {
      if (!this.useMySQL) {
         FileConfiguration config = (FileConfiguration)this.playerConfigs.get(uuid);
         if (config.contains("logs")) {
            Map<String, Object> logs = config.getConfigurationSection("logs").getValues(false);
            if (logs.size() > 1000) {
               logs.entrySet().stream().sorted((e1, e2) -> Long.compare(Long.parseLong((String)e1.getKey()), Long.parseLong((String)e2.getKey()))).limit((long)(logs.size() - 1000)).forEach((entry) -> config.set("logs." + (String)entry.getKey(), (Object)null));
            }
         }
      }

   }

   public FileConfiguration getPlayerConfig(Player player) {
      return this.getPlayerConfig(player.getUniqueId());
   }

   public FileConfiguration getPlayerConfig(UUID uuid) {
      if (!this.playerConfigs.containsKey(uuid)) {
         if (this.useMySQL) {
            FileConfiguration tempConfig = new YamlConfiguration();
            Map<String, Object> data = this.mysqlManager.loadPlayerData(uuid);
            tempConfig.set("name", "Unknown");
            tempConfig.set("uuid", uuid.toString());
            tempConfig.set("totalSold", data.getOrDefault("totalSold", (double)0.0F));
            tempConfig.set("totalItems", data.getOrDefault("totalItems", 0));
            if (data.containsKey("levelData")) {
               Map<String, Object> levelData = (Map)data.get("levelData");

               for(Map.Entry<String, Object> entry : levelData.entrySet()) {
                  tempConfig.set("levels." + (String)entry.getKey(), entry.getValue());
               }
            }

            this.playerConfigs.put(uuid, tempConfig);
         } else {
            this.loadPlayerConfig(uuid, "Unknown");
         }
      }

      return (FileConfiguration)this.playerConfigs.get(uuid);
   }

   public double getTotalSold(Player player) {
      return this.getTotalSold(player.getUniqueId());
   }

   public double getTotalSold(UUID uuid) {
      if (this.useMySQL && !this.playerConfigs.containsKey(uuid)) {
         Map<String, Object> data = this.mysqlManager.loadPlayerData(uuid);
         return (Double)data.getOrDefault("totalSold", (double)0.0F);
      } else {
         FileConfiguration config = this.getPlayerConfig(uuid);
         return config.getDouble("totalSold", (double)0.0F);
      }
   }

   public int getTotalSoldItems(Player player) {
      return this.getTotalSoldItems(player.getUniqueId());
   }

   public int getTotalSoldItems(UUID uuid) {
      if (this.useMySQL && !this.playerConfigs.containsKey(uuid)) {
         Map<String, Object> data = this.mysqlManager.loadPlayerData(uuid);
         return (Integer)data.getOrDefault("totalItems", 0);
      } else {
         FileConfiguration config = this.getPlayerConfig(uuid);
         return config.getInt("totalItems", 0);
      }
   }

   public void unloadPlayerConfig(Player player) {
      this.unloadPlayerConfig(player.getUniqueId());
   }

   public void unloadPlayerConfig(UUID uuid) {
      if (!this.useMySQL) {
         if (this.playerConfigs.containsKey(uuid)) {
            this.savePlayerConfig(uuid);
            this.playerConfigs.remove(uuid);
            this.playerFiles.remove(uuid);
         }
      } else if (this.playerConfigs.containsKey(uuid)) {
         FileConfiguration config = (FileConfiguration)this.playerConfigs.get(uuid);
         String playerName = config.getString("name", "Unknown");
         double totalSold = config.getDouble("totalSold", (double)0.0F);
         int totalItems = config.getInt("totalItems", 0);
         Map<String, Object> levelData = new HashMap();
         if (config.contains("levels")) {
            for(String category : config.getConfigurationSection("levels").getKeys(false)) {
               Map<String, Object> catData = new HashMap();
               catData.put("totalEarned", config.getDouble("levels." + category + ".totalEarned", (double)0.0F));
               catData.put("level", config.getInt("levels." + category + ".level", 0));
               catData.put("multiplier", config.getDouble("levels." + category + ".multiplier", (double)1.0F));
               levelData.put(category, catData);
            }
         }

         this.mysqlManager.savePlayerData(uuid, playerName, totalSold, totalItems, levelData);
         this.playerConfigs.remove(uuid);
      }

   }

   public Map<String, Object> getPlayerLogs(Player player) {
      return this.getPlayerLogs(player.getUniqueId());
   }

   public Map<String, Object> getPlayerLogs(UUID uuid) {
      if (!this.useMySQL) {
         FileConfiguration config = this.getPlayerConfig(uuid);
         return (Map<String, Object>)(config.contains("logs") ? config.getConfigurationSection("logs").getValues(false) : new HashMap());
      } else {
         Map<String, Object> logsMap = new HashMap();
         Map<Long, Map<String, Object>> mysqlLogs = this.mysqlManager.getPlayerLogs(uuid, 1000);

         for(Map.Entry<Long, Map<String, Object>> entry : mysqlLogs.entrySet()) {
            logsMap.put(String.valueOf(entry.getKey()), entry.getValue());
         }

         return logsMap;
      }
   }

   public boolean deletePlayerData(Player player) {
      return this.deletePlayerData(player.getUniqueId());
   }

   public boolean deletePlayerData(UUID uuid) {
      this.unloadPlayerConfig(uuid);
      if (this.useMySQL) {
         return this.mysqlManager.deletePlayerData(uuid);
      } else {
         File[] playerFiles = this.playerDataFolder.listFiles();
         if (playerFiles != null) {
            for(File file : playerFiles) {
               String fileName = file.getName().replace(".yml", "");
               String playerName = this.getPlayerConfig(uuid).getString("name");
               if (fileName.equalsIgnoreCase(playerName)) {
                  return file.delete();
               }
            }
         }

         return false;
      }
   }

   public File getPlayerFileByName(String playerName) {
      if (this.useMySQL) {
         return null;
      } else {
         File[] files = this.playerDataFolder.listFiles();
         if (files != null) {
            for(File file : files) {
               if (file.getName().equalsIgnoreCase(playerName + ".yml")) {
                  return file;
               }
            }
         }

         return null;
      }
   }

   public void resetPlayerData(Player player) {
      this.resetPlayerData(player.getUniqueId(), player.getName());
   }

   public void resetPlayerData(UUID uuid, String playerName) {
      if (this.useMySQL) {
         this.mysqlManager.resetPlayerData(uuid);
         if (this.playerConfigs.containsKey(uuid)) {
            FileConfiguration config = (FileConfiguration)this.playerConfigs.get(uuid);
            config.set("totalSold", (double)0.0F);
            config.set("totalItems", 0);
            config.set("logs", (Object)null);
            if (config.contains("levels")) {
               for(String category : config.getConfigurationSection("levels").getKeys(false)) {
                  config.set("levels." + category + ".totalEarned", (double)0.0F);
                  config.set("levels." + category + ".level", 0);
                  config.set("levels." + category + ".multiplier", (double)1.0F);
               }
            }

            this.savePlayerConfig(uuid);
         }
      } else {
         File playerFile = new File(this.playerDataFolder, playerName + ".yml");
         if (playerFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            config.set("totalSold", (double)0.0F);
            config.set("totalItems", 0);
            config.set("logs", (Object)null);
            if (config.contains("levels")) {
               for(String category : config.getConfigurationSection("levels").getKeys(false)) {
                  config.set("levels." + category + ".totalEarned", (double)0.0F);
                  config.set("levels." + category + ".level", 0);
                  config.set("levels." + category + ".multiplier", (double)1.0F);
               }
            }

            try {
               config.save(playerFile);
            } catch (IOException e) {
               e.printStackTrace();
            }
         }

         if (this.playerConfigs.containsKey(uuid)) {
            FileConfiguration config = (FileConfiguration)this.playerConfigs.get(uuid);
            config.set("totalSold", (double)0.0F);
            config.set("totalItems", 0);
            config.set("logs", (Object)null);
            if (config.contains("levels")) {
               for(String category : config.getConfigurationSection("levels").getKeys(false)) {
                  config.set("levels." + category + ".totalEarned", (double)0.0F);
                  config.set("levels." + category + ".level", 0);
                  config.set("levels." + category + ".multiplier", (double)1.0F);
               }
            }
         }
      }

   }

   public void resetOfflinePlayerData(String playerName) {
      if (this.useMySQL) {
         this.mysqlManager.resetPlayerDataByName(playerName);
      } else {
         File playerFile = new File(this.playerDataFolder, playerName + ".yml");
         if (playerFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            config.set("totalSold", (double)0.0F);
            config.set("totalItems", 0);
            config.set("logs", (Object)null);
            if (config.contains("levels")) {
               for(String category : config.getConfigurationSection("levels").getKeys(false)) {
                  config.set("levels." + category + ".totalEarned", (double)0.0F);
                  config.set("levels." + category + ".level", 0);
                  config.set("levels." + category + ".multiplier", (double)1.0F);
               }
            }

            try {
               config.save(playerFile);
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }

   }

   public boolean resetPlayerDataByName(String playerName) {
      if (this.useMySQL) {
         return this.mysqlManager.resetPlayerDataByName(playerName);
      } else {
         File playerFile = new File(this.playerDataFolder, playerName + ".yml");
         if (!playerFile.exists()) {
            return false;
         } else {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            config.set("totalSold", (double)0.0F);
            config.set("totalItems", 0);
            config.set("logs", (Object)null);
            if (config.contains("levels")) {
               for(String category : config.getConfigurationSection("levels").getKeys(false)) {
                  config.set("levels." + category + ".totalEarned", (double)0.0F);
                  config.set("levels." + category + ".level", 0);
                  config.set("levels." + category + ".multiplier", (double)1.0F);
               }
            }

            try {
               config.save(playerFile);
               return true;
            } catch (IOException e) {
               e.printStackTrace();
               return false;
            }
         }
      }
   }

   public double getOfflineTotalSold(String playerName) {
      if (this.useMySQL) {
         Map<String, Object> stats = this.mysqlManager.getOfflinePlayerStats(playerName);
         return (Double)stats.getOrDefault("totalSold", (double)0.0F);
      } else {
         try {
            File playerFile = new File(this.playerDataFolder, playerName + ".yml");
            if (playerFile.exists()) {
               FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
               return config.getDouble("totalSold", (double)0.0F);
            }
         } catch (Exception var4) {
         }

         return (double)0.0F;
      }
   }

   public int getOfflineTotalSoldItems(String playerName) {
      if (this.useMySQL) {
         Map<String, Object> stats = this.mysqlManager.getOfflinePlayerStats(playerName);
         return (Integer)stats.getOrDefault("totalItems", 0);
      } else {
         try {
            File playerFile = new File(this.playerDataFolder, playerName + ".yml");
            if (playerFile.exists()) {
               FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
               return config.getInt("totalItems", 0);
            }
         } catch (Exception var4) {
         }

         return 0;
      }
   }

   public List<String> getAllPlayerNames() {
      if (this.useMySQL) {
         return this.mysqlManager.getAllPlayerNames();
      } else {
         List<String> playerNames = new ArrayList();

         try {
            File[] files = this.playerDataFolder.listFiles();
            if (files != null) {
               for(File file : files) {
                  if (file.getName().endsWith(".yml")) {
                     String name = file.getName().replace(".yml", "");
                     playerNames.add(name);
                  }
               }
            }
         } catch (Exception var8) {
         }

         return playerNames;
      }
   }

   public FileConfiguration getOfflinePlayerConfig(String playerName) {
      if (this.useMySQL) {
         Map<String, Object> stats = this.mysqlManager.getOfflinePlayerStats(playerName);
         if (stats.isEmpty()) {
            return null;
         } else {
            FileConfiguration tempConfig = new YamlConfiguration();
            tempConfig.set("totalSold", stats.get("totalSold"));
            tempConfig.set("totalItems", stats.get("totalItems"));
            tempConfig.set("uuid", stats.get("uuid"));
            tempConfig.set("name", playerName);
            if (stats.containsKey("levelData")) {
               Map<String, Object> levelData = (Map)stats.get("levelData");

               for(Map.Entry<String, Object> entry : levelData.entrySet()) {
                  tempConfig.set("levels." + (String)entry.getKey(), entry.getValue());
               }
            }

            return tempConfig;
         }
      } else {
         try {
            File playerFile = new File(this.playerDataFolder, playerName + ".yml");
            if (playerFile.exists()) {
               return YamlConfiguration.loadConfiguration(playerFile);
            }
         } catch (Exception var7) {
         }

         return null;
      }
   }

   public boolean hasPlayerData(String playerName) {
      if (this.useMySQL) {
         return this.mysqlManager.hasPlayerData(playerName);
      } else {
         File playerFile = new File(this.playerDataFolder, playerName + ".yml");
         return playerFile.exists();
      }
   }

   public Map<String, Object> getOfflinePlayerStats(String playerName) {
      if (this.useMySQL) {
         return this.mysqlManager.getOfflinePlayerStats(playerName);
      } else {
         Map<String, Object> stats = new HashMap();

         try {
            File playerFile = new File(this.playerDataFolder, playerName + ".yml");
            if (playerFile.exists()) {
               FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
               stats.put("totalSold", config.getDouble("totalSold", (double)0.0F));
               stats.put("totalItems", config.getInt("totalItems", 0));
               stats.put("uuid", config.getString("uuid", ""));
               if (config.contains("levels")) {
                  Map<String, Object> levelData = new HashMap();

                  for(String category : config.getConfigurationSection("levels").getKeys(false)) {
                     Map<String, Object> catData = new HashMap();
                     catData.put("totalEarned", config.getDouble("levels." + category + ".totalEarned", (double)0.0F));
                     catData.put("level", config.getInt("levels." + category + ".level", 0));
                     catData.put("multiplier", config.getDouble("levels." + category + ".multiplier", (double)1.0F));
                     levelData.put(category, catData);
                  }

                  stats.put("levelData", levelData);
               }
            }
         } catch (Exception var9) {
         }

         return stats;
      }
   }
}
