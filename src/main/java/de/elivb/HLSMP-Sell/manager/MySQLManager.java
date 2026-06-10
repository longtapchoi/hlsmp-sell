package de.elivb.donutSell.manager;

import de.elivb.donutSell.Sell;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class MySQLManager {
   private final Sell plugin;
   private Connection connection;
   private boolean mysqlEnabled;
   private String host;
   private String database;
   private String user;
   private String password;
   private int port;

   public MySQLManager(Sell plugin) {
      this.plugin = plugin;
      this.loadMySQLConfig();
   }

   private String generateRandomPassword() {
      Random random = new Random();
      String digits = "";

      for(int i = 0; i < 3; ++i) {
         digits = digits + random.nextInt(10);
      }

      String letters = "";
      String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

      for(int i = 0; i < 3; ++i) {
         letters = letters + alphabet.charAt(random.nextInt(alphabet.length()));
      }

      String combined = digits + letters;
      char[] chars = combined.toCharArray();

      for(int i = chars.length - 1; i > 0; --i) {
         int j = random.nextInt(i + 1);
         char temp = chars[i];
         chars[i] = chars[j];
         chars[j] = temp;
      }

      return new String(chars);
   }

   private void loadMySQLConfig() {
      File mysqlFile = new File(this.plugin.getDataFolder(), "mysql.yml");

      try {
         if (!mysqlFile.exists()) {
            this.plugin.saveResource("mysql.yml", false);
            FileConfiguration mysqlConfig = YamlConfiguration.loadConfiguration(mysqlFile);
            String randomPassword = this.generateRandomPassword();
            mysqlConfig.set("password", randomPassword);
            mysqlConfig.save(mysqlFile);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      FileConfiguration mysqlConfig = YamlConfiguration.loadConfiguration(mysqlFile);
      this.mysqlEnabled = mysqlConfig.getBoolean("enable", false);
      this.host = mysqlConfig.getString("ip", "127.0.0.1");
      this.port = mysqlConfig.getInt("port", 3306);
      this.user = mysqlConfig.getString("user", "root");
      this.password = mysqlConfig.getString("password", "");
      this.database = mysqlConfig.getString("name", "DonutSell");
      if (this.mysqlEnabled && (this.password == null || this.password.isEmpty())) {
         String randomPassword = this.generateRandomPassword();
         mysqlConfig.set("password", randomPassword);

         try {
            mysqlConfig.save(mysqlFile);
            this.password = randomPassword;
         } catch (IOException e) {
            e.printStackTrace();
         }
      }

   }

   public boolean isEnabled() {
      return this.mysqlEnabled;
   }

   public void connect() {
      if (this.mysqlEnabled) {
         try {
            if (this.connection != null && !this.connection.isClosed()) {
               return;
            }

            Class.forName("com.mysql.jdbc.Driver");
            this.connection = DriverManager.getConnection("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?useSSL=false&autoReconnect=true", this.user, this.password);
            this.createTables();
         } catch (ClassNotFoundException | SQLException e) {
            this.mysqlEnabled = false;
            ((Exception)e).printStackTrace();
         }

      }
   }

   public void disconnect() {
      if (this.connection != null) {
         try {
            this.connection.close();
         } catch (SQLException e) {
            e.printStackTrace();
         }
      }

   }

   private void createTables() throws SQLException {
      String playerTable = "CREATE TABLE IF NOT EXISTS player_data (uuid VARCHAR(36) PRIMARY KEY,player_name VARCHAR(16) NOT NULL,total_sold DOUBLE DEFAULT 0,total_items INT DEFAULT 0,level_data TEXT,last_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);";
      String logsTable = "CREATE TABLE IF NOT EXISTS sell_logs (id INT AUTO_INCREMENT PRIMARY KEY,uuid VARCHAR(36) NOT NULL,timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,item_count INT NOT NULL,total_price DOUBLE NOT NULL,items_sold TEXT,INDEX idx_uuid (uuid));";
      String shulkerTable = "CREATE TABLE IF NOT EXISTS shulker_contents (id INT AUTO_INCREMENT PRIMARY KEY,log_id INT NOT NULL,item_type VARCHAR(64) NOT NULL,amount INT NOT NULL,FOREIGN KEY (log_id) REFERENCES sell_logs(id) ON DELETE CASCADE);";
      Statement stmt = this.connection.createStatement();

      try {
         stmt.execute(playerTable);
         stmt.execute(logsTable);
         stmt.execute(shulkerTable);
      } catch (Throwable var8) {
         if (stmt != null) {
            try {
               stmt.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (stmt != null) {
         stmt.close();
      }

   }

   public void savePlayerData(UUID uuid, String playerName, double totalSold, int totalItems, Map<String, Object> levelData) {
      if (this.mysqlEnabled && this.connection != null) {
         String query = "INSERT INTO player_data (uuid, player_name, total_sold, total_items, level_data) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), total_sold = VALUES(total_sold), total_items = VALUES(total_items), level_data = VALUES(level_data)";

         try {
            PreparedStatement ps = this.connection.prepareStatement(query);

            try {
               ps.setString(1, uuid.toString());
               ps.setString(2, playerName);
               ps.setDouble(3, totalSold);
               ps.setInt(4, totalItems);
               ps.setString(5, this.convertLevelDataToJson(levelData));
               ps.executeUpdate();
            } catch (Throwable var12) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var11) {
                     var12.addSuppressed(var11);
                  }
               }

               throw var12;
            }

            if (ps != null) {
               ps.close();
            }
         } catch (SQLException e) {
            e.printStackTrace();
         }

      }
   }

   public void savePlayerData(UUID uuid, String playerName, double totalSold, int totalItems) {
      this.savePlayerData(uuid, playerName, totalSold, totalItems, new HashMap());
   }

   public Map<String, Object> loadPlayerData(UUID uuid) {
      Map<String, Object> data = new HashMap();
      if (this.mysqlEnabled && this.connection != null) {
         String query = "SELECT * FROM player_data WHERE uuid = ?";

         try {
            PreparedStatement ps = this.connection.prepareStatement(query);

            try {
               ps.setString(1, uuid.toString());
               ResultSet rs = ps.executeQuery();
               if (rs.next()) {
                  data.put("totalSold", rs.getDouble("total_sold"));
                  data.put("totalItems", rs.getInt("total_items"));
                  String levelDataJson = rs.getString("level_data");
                  if (levelDataJson != null && !levelDataJson.isEmpty()) {
                     Map<String, Object> levelData = this.parseLevelDataFromJson(levelDataJson);
                     if (!levelData.isEmpty()) {
                        data.put("levelData", levelData);
                     }
                  }
               } else {
                  data.put("totalSold", (double)0.0F);
                  data.put("totalItems", 0);
               }
            } catch (Throwable var9) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (ps != null) {
               ps.close();
            }
         } catch (SQLException e) {
            e.printStackTrace();
         }

         return data;
      } else {
         return data;
      }
   }

   public int saveSellLog(UUID uuid, int itemCount, double totalPrice, String itemsSold, Map<String, Integer> shulkerContents) {
      if (this.mysqlEnabled && this.connection != null) {
         try {
            String logQuery = "INSERT INTO sell_logs (uuid, item_count, total_price, items_sold) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = this.connection.prepareStatement(logQuery, 1);
            ps.setString(1, uuid.toString());
            ps.setInt(2, itemCount);
            ps.setDouble(3, totalPrice);
            ps.setString(4, itemsSold);
            ps.executeUpdate();
            ResultSet generatedKeys = ps.getGeneratedKeys();
            int logId = -1;
            if (generatedKeys.next()) {
               logId = generatedKeys.getInt(1);
            }

            if (shulkerContents != null && !shulkerContents.isEmpty() && logId != -1) {
               String shulkerQuery = "INSERT INTO shulker_contents (log_id, item_type, amount) VALUES (?, ?, ?)";
               PreparedStatement shulkerPs = this.connection.prepareStatement(shulkerQuery);

               for(Map.Entry<String, Integer> entry : shulkerContents.entrySet()) {
                  shulkerPs.setInt(1, logId);
                  shulkerPs.setString(2, (String)entry.getKey());
                  shulkerPs.setInt(3, (Integer)entry.getValue());
                  shulkerPs.addBatch();
               }

               shulkerPs.executeBatch();
               shulkerPs.close();
            }

            ps.close();
            return logId;
         } catch (SQLException e) {
            e.printStackTrace();
            return -1;
         }
      } else {
         return -1;
      }
   }

   public Map<Long, Map<String, Object>> getPlayerLogs(UUID uuid, int limit) {
      Map<Long, Map<String, Object>> logs = new LinkedHashMap();
      if (this.mysqlEnabled && this.connection != null) {
         String query = "SELECT * FROM sell_logs WHERE uuid = ? ORDER BY timestamp DESC LIMIT ?";

         try {
            PreparedStatement ps = this.connection.prepareStatement(query);

            try {
               ps.setString(1, uuid.toString());
               ps.setInt(2, limit);

               Map<String, Object> logData;
               long timestamp;
               for(ResultSet rs = ps.executeQuery(); rs.next(); logs.put(timestamp, logData)) {
                  logData = new HashMap();
                  timestamp = rs.getTimestamp("timestamp").getTime();
                  int logId = rs.getInt("id");
                  logData.put("items", rs.getInt("item_count"));
                  logData.put("price", rs.getDouble("total_price"));
                  logData.put("itemsSold", rs.getString("items_sold"));
                  logData.put("timestamp", (new SimpleDateFormat("dd.MM.yyyy HH:mm:ss")).format(rs.getTimestamp("timestamp")));
                  Map<String, Integer> shulkerContents = this.getShulkerContents(logId);
                  if (!shulkerContents.isEmpty()) {
                     logData.put("shulkerContents", shulkerContents);
                  }
               }
            } catch (Throwable var13) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var12) {
                     var13.addSuppressed(var12);
                  }
               }

               throw var13;
            }

            if (ps != null) {
               ps.close();
            }
         } catch (SQLException e) {
            e.printStackTrace();
         }

         return logs;
      } else {
         return logs;
      }
   }

   private Map<String, Integer> getShulkerContents(int logId) {
      Map<String, Integer> contents = new HashMap();
      if (this.mysqlEnabled && this.connection != null) {
         String query = "SELECT * FROM shulker_contents WHERE log_id = ?";

         try {
            PreparedStatement ps = this.connection.prepareStatement(query);

            try {
               ps.setInt(1, logId);
               ResultSet rs = ps.executeQuery();

               while(rs.next()) {
                  contents.put(rs.getString("item_type"), rs.getInt("amount"));
               }
            } catch (Throwable var8) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var7) {
                     var8.addSuppressed(var7);
                  }
               }

               throw var8;
            }

            if (ps != null) {
               ps.close();
            }
         } catch (SQLException e) {
            e.printStackTrace();
         }

         return contents;
      } else {
         return contents;
      }
   }

   public Map<String, Object> getOfflinePlayerStats(String playerName) {
      Map<String, Object> stats = new HashMap();
      if (this.mysqlEnabled && this.connection != null) {
         String query = "SELECT * FROM player_data WHERE player_name = ?";

         try {
            PreparedStatement ps = this.connection.prepareStatement(query);

            try {
               ps.setString(1, playerName);
               ResultSet rs = ps.executeQuery();
               if (rs.next()) {
                  stats.put("totalSold", rs.getDouble("total_sold"));
                  stats.put("totalItems", rs.getInt("total_items"));
                  stats.put("uuid", rs.getString("uuid"));
                  String levelDataJson = rs.getString("level_data");
                  if (levelDataJson != null && !levelDataJson.isEmpty()) {
                     Map<String, Object> levelData = this.parseLevelDataFromJson(levelDataJson);
                     if (!levelData.isEmpty()) {
                        stats.put("levelData", levelData);
                     }
                  }
               }
            } catch (Throwable var9) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (ps != null) {
               ps.close();
            }
         } catch (SQLException e) {
            e.printStackTrace();
         }

         return stats;
      } else {
         return stats;
      }
   }

   public List<String> getAllPlayerNames() {
      List<String> playerNames = new ArrayList();
      if (this.mysqlEnabled && this.connection != null) {
         String query = "SELECT player_name FROM player_data ORDER BY player_name";

         try {
            Statement stmt = this.connection.createStatement();

            try {
               ResultSet rs = stmt.executeQuery(query);

               try {
                  while(rs.next()) {
                     playerNames.add(rs.getString("player_name"));
                  }
               } catch (Throwable var9) {
                  if (rs != null) {
                     try {
                        rs.close();
                     } catch (Throwable var8) {
                        var9.addSuppressed(var8);
                     }
                  }

                  throw var9;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var10) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var7) {
                     var10.addSuppressed(var7);
                  }
               }

               throw var10;
            }

            if (stmt != null) {
               stmt.close();
            }
         } catch (SQLException e) {
            e.printStackTrace();
         }

         return playerNames;
      } else {
         return playerNames;
      }
   }

   public boolean hasPlayerData(String playerName) {
      if (this.mysqlEnabled && this.connection != null) {
         String query = "SELECT COUNT(*) FROM player_data WHERE player_name = ?";

         try {
            PreparedStatement ps = this.connection.prepareStatement(query);

            boolean var5;
            label69: {
               try {
                  ps.setString(1, playerName);
                  ResultSet rs = ps.executeQuery();
                  if (rs.next()) {
                     var5 = rs.getInt(1) > 0;
                     break label69;
                  }
               } catch (Throwable var7) {
                  if (ps != null) {
                     try {
                        ps.close();
                     } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                     }
                  }

                  throw var7;
               }

               if (ps != null) {
                  ps.close();
               }

               return false;
            }

            if (ps != null) {
               ps.close();
            }

            return var5;
         } catch (SQLException e) {
            e.printStackTrace();
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean deletePlayerData(UUID uuid) {
      if (this.mysqlEnabled && this.connection != null) {
         String query = "DELETE FROM player_data WHERE uuid = ?";

         try {
            PreparedStatement ps = this.connection.prepareStatement(query);

            boolean var4;
            try {
               ps.setString(1, uuid.toString());
               var4 = ps.executeUpdate() > 0;
            } catch (Throwable var7) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (ps != null) {
               ps.close();
            }

            return var4;
         } catch (SQLException e) {
            e.printStackTrace();
            return false;
         }
      } else {
         return false;
      }
   }

   public void resetPlayerData(UUID uuid) {
      if (this.mysqlEnabled && this.connection != null) {
         try {
            String updateQuery = "UPDATE player_data SET total_sold = 0, total_items = 0, level_data = '{}' WHERE uuid = ?";
            PreparedStatement ps = this.connection.prepareStatement(updateQuery);

            try {
               ps.setString(1, uuid.toString());
               ps.executeUpdate();
            } catch (Throwable var10) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var8) {
                     var10.addSuppressed(var8);
                  }
               }

               throw var10;
            }

            if (ps != null) {
               ps.close();
            }

            String deleteLogsQuery = "DELETE FROM sell_logs WHERE uuid = ?";
            PreparedStatement ps2 = this.connection.prepareStatement(deleteLogsQuery);

            try {
               ps2.setString(1, uuid.toString());
               ps2.executeUpdate();
            } catch (Throwable var9) {
               if (ps2 != null) {
                  try {
                     ps2.close();
                  } catch (Throwable var7) {
                     var9.addSuppressed(var7);
                  }
               }

               throw var9;
            }

            if (ps2 != null) {
               ps2.close();
            }
         } catch (SQLException e) {
            e.printStackTrace();
         }

      }
   }

   public boolean resetPlayerDataByName(String playerName) {
      if (this.mysqlEnabled && this.connection != null) {
         String uuidQuery = "SELECT uuid FROM player_data WHERE player_name = ?";

         try {
            PreparedStatement ps = this.connection.prepareStatement(uuidQuery);

            boolean var6;
            label60: {
               try {
                  ps.setString(1, playerName);
                  ResultSet rs = ps.executeQuery();
                  if (rs.next()) {
                     UUID uuid = UUID.fromString(rs.getString("uuid"));
                     this.resetPlayerData(uuid);
                     var6 = true;
                     break label60;
                  }
               } catch (Throwable var8) {
                  if (ps != null) {
                     try {
                        ps.close();
                     } catch (Throwable var7) {
                        var8.addSuppressed(var7);
                     }
                  }

                  throw var8;
               }

               if (ps != null) {
                  ps.close();
               }

               return false;
            }

            if (ps != null) {
               ps.close();
            }

            return var6;
         } catch (SQLException e) {
            e.printStackTrace();
            return false;
         }
      } else {
         return false;
      }
   }

   public void resetPlayerData(Player player) {
      this.resetPlayerData(player.getUniqueId());
   }

   private String convertLevelDataToJson(Map<String, Object> levelData) {
      if (levelData != null && !levelData.isEmpty()) {
         StringBuilder json = new StringBuilder("{");
         boolean first = true;

         for(Map.Entry<String, Object> entry : levelData.entrySet()) {
            if (!first) {
               json.append(",");
            }

            first = false;
            json.append("\"").append((String)entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof Map) {
               json.append(this.convertCategoryDataToJson((Map)value));
            } else {
               json.append(value);
            }
         }

         json.append("}");
         return json.toString();
      } else {
         return "{}";
      }
   }

   private String convertCategoryDataToJson(Map<String, Object> catData) {
      StringBuilder json = new StringBuilder("{");
      boolean first = true;

      for(Map.Entry<String, Object> entry : catData.entrySet()) {
         if (!first) {
            json.append(",");
         }

         first = false;
         json.append("\"").append((String)entry.getKey()).append("\":").append(entry.getValue());
      }

      json.append("}");
      return json.toString();
   }

   private Map<String, Object> parseLevelDataFromJson(String json) {
      Map<String, Object> levelData = new HashMap();
      if (json != null && !json.isEmpty() && !json.equals("{}")) {
         try {
            json = json.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
               json = json.substring(1, json.length() - 1);
               int depth = 0;
               StringBuilder currentCategory = new StringBuilder();
               StringBuilder currentValue = new StringBuilder();
               boolean inKey = true;
               boolean inString = false;

               for(int i = 0; i < json.length(); ++i) {
                  char c = json.charAt(i);
                  if (c != '"' || i != 0 && json.charAt(i - 1) == '\\') {
                     if (!inString) {
                        if (c == '{') {
                           ++depth;
                        } else if (c == '}') {
                           --depth;
                        } else {
                           if (c == ':' && depth == 1 && inKey) {
                              inKey = false;
                              continue;
                           }

                           if (c == ',' && depth == 1) {
                              String category = currentCategory.toString().replace("\"", "");
                              String valueStr = currentValue.toString().trim();
                              Map<String, Object> catData = this.parseCategoryData(valueStr);
                              if (!catData.isEmpty()) {
                                 levelData.put(category, catData);
                              }

                              currentCategory = new StringBuilder();
                              currentValue = new StringBuilder();
                              inKey = true;
                              continue;
                           }
                        }
                     }

                     if (inKey) {
                        currentCategory.append(c);
                     } else {
                        currentValue.append(c);
                     }
                  } else {
                     inString = !inString;
                     if (inKey) {
                        currentCategory.append(c);
                     } else {
                        currentValue.append(c);
                     }
                  }
               }

               if (currentCategory.length() > 0 && currentValue.length() > 0) {
                  String category = currentCategory.toString().replace("\"", "");
                  String valueStr = currentValue.toString().trim();
                  Map<String, Object> catData = this.parseCategoryData(valueStr);
                  if (!catData.isEmpty()) {
                     levelData.put(category, catData);
                  }
               }
            }
         } catch (Exception var13) {
         }

         return levelData;
      } else {
         return levelData;
      }
   }

   private Map<String, Object> parseCategoryData(String json) {
      Map<String, Object> catData = new HashMap();
      if (json != null && !json.isEmpty()) {
         try {
            json = json.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
               json = json.substring(1, json.length() - 1);
               String[] parts = json.split(",");

               for(String part : parts) {
                  String[] kv = part.split(":", 2);
                  if (kv.length == 2) {
                     String key = kv[0].trim().replace("\"", "");
                     String value = kv[1].trim();

                     try {
                        if (value.contains(".")) {
                           catData.put(key, Double.parseDouble(value));
                        } else {
                           catData.put(key, Integer.parseInt(value));
                        }
                     } catch (NumberFormatException var12) {
                        catData.put(key, value);
                     }
                  }
               }
            }
         } catch (Exception var13) {
         }

         return catData;
      } else {
         return catData;
      }
   }

   public void migrateFromFiles(PlayerDataManager playerDataManager) {
      if (this.mysqlEnabled && this.connection != null) {
         try {
            List<String> mysqlPlayers = this.getAllPlayerNames();
            if (!mysqlPlayers.isEmpty()) {
               return;
            }

            List<String> ymlPlayers = playerDataManager.getAllPlayerNames();
            if (ymlPlayers.isEmpty()) {
               return;
            }

            this.plugin.getLogger().info("YML-Data: " + ymlPlayers.size());
            this.plugin.getLogger().info("Migration started...");
            int success = 0;
            int failed = 0;

            for(String playerName : ymlPlayers) {
               try {
                  FileConfiguration config = playerDataManager.getOfflinePlayerConfig(playerName);
                  if (config == null) {
                     ++failed;
                  } else {
                     String uuidStr = config.getString("uuid");
                     if (uuidStr == null) {
                        ++failed;
                     } else {
                        UUID uuid = UUID.fromString(uuidStr);
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

                        this.savePlayerData(uuid, playerName, totalSold, totalItems, levelData);
                        if (config.contains("logs")) {
                           ConfigurationSection logsSection = config.getConfigurationSection("logs");
                           if (logsSection != null) {
                              for(String logKey : logsSection.getKeys(false)) {
                                 String path = "logs." + logKey;
                                 int items = config.getInt(path + ".items", 0);
                                 double price = config.getDouble(path + ".price", (double)0.0F);
                                 String itemsSold = config.getString(path + ".itemsSold", "");
                                 Map<String, Integer> shulkerContents = new HashMap();
                                 if (config.contains(path + ".shulkerContents")) {
                                    ConfigurationSection shulkerSection = config.getConfigurationSection(path + ".shulkerContents");
                                    if (shulkerSection != null) {
                                       for(String itemType : shulkerSection.getKeys(false)) {
                                          shulkerContents.put(itemType, shulkerSection.getInt(itemType));
                                       }
                                    }
                                 }

                                 this.saveSellLog(uuid, items, price, itemsSold, shulkerContents);
                              }
                           }
                        }

                        ++success;
                        if (success % 10 == 0) {
                           this.plugin.getLogger().info("Migriert: " + success + "/" + ymlPlayers.size() + " Player...");
                        }
                     }
                  }
               } catch (Exception e) {
                  e.printStackTrace();
                  ++failed;
               }
            }

            this.plugin.getLogger().info("Migration completed!");
            this.plugin.getLogger().info("Successfully migrated: " + success);
            if (failed > 0) {
               this.plugin.getLogger().info("Errors: " + failed);
            }

            this.createYmlBackup(playerDataManager);
         } catch (Exception e) {
            e.printStackTrace();
         }

      }
   }

   private void createYmlBackup(PlayerDataManager playerDataManager) {
      try {
         File playerDataFolder = new File(this.plugin.getDataFolder(), "playerdata");
         if (!playerDataFolder.exists()) {
            return;
         }

         File backupFolder = new File(this.plugin.getDataFolder(), "playerdata_backup_" + System.currentTimeMillis());
         this.copyFolder(playerDataFolder, backupFolder);
      } catch (Exception var4) {
      }

   }

   private void copyFolder(File source, File target) throws IOException {
      if (source.isDirectory()) {
         if (!target.exists()) {
            target.mkdirs();
         }

         String[] children = source.list();
         if (children != null) {
            for(String child : children) {
               this.copyFolder(new File(source, child), new File(target, child));
            }
         }
      } else {
         InputStream in = new FileInputStream(source);

         try {
            OutputStream out = new FileOutputStream(target);

            try {
               byte[] buf = new byte[1024];

               int length;
               while((length = in.read(buf)) > 0) {
                  out.write(buf, 0, length);
               }
            } catch (Throwable var10) {
               try {
                  out.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }

               throw var10;
            }

            out.close();
         } catch (Throwable var11) {
            try {
               in.close();
            } catch (Throwable var8) {
               var11.addSuppressed(var8);
            }

            throw var11;
         }

         in.close();
      }

   }

   public boolean isDatabaseEmpty() {
      if (this.mysqlEnabled && this.connection != null) {
         String query = "SELECT COUNT(*) FROM player_data";

         try {
            Statement stmt = this.connection.createStatement();

            boolean var4;
            label98: {
               try {
                  ResultSet rs = stmt.executeQuery(query);

                  label89: {
                     try {
                        if (!rs.next()) {
                           break label89;
                        }

                        var4 = rs.getInt(1) == 0;
                     } catch (Throwable var8) {
                        if (rs != null) {
                           try {
                              rs.close();
                           } catch (Throwable var7) {
                              var8.addSuppressed(var7);
                           }
                        }

                        throw var8;
                     }

                     if (rs != null) {
                        rs.close();
                     }
                     break label98;
                  }

                  if (rs != null) {
                     rs.close();
                  }
               } catch (Throwable var9) {
                  if (stmt != null) {
                     try {
                        stmt.close();
                     } catch (Throwable var6) {
                        var9.addSuppressed(var6);
                     }
                  }

                  throw var9;
               }

               if (stmt != null) {
                  stmt.close();
               }

               return true;
            }

            if (stmt != null) {
               stmt.close();
            }

            return var4;
         } catch (SQLException e) {
            e.printStackTrace();
            return true;
         }
      } else {
         return true;
      }
   }

   public boolean isConnected() {
      try {
         return this.connection != null && !this.connection.isClosed();
      } catch (SQLException var2) {
         return false;
      }
   }
}
