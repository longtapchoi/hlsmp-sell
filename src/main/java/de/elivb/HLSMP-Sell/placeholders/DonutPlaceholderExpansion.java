package de.elivb.donutSell.placeholders;

import de.elivb.donutSell.Sell;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DonutPlaceholderExpansion extends PlaceholderExpansion {
   private final Sell plugin;

   public DonutPlaceholderExpansion(Sell plugin) {
      this.plugin = plugin;
   }

   public @NotNull String getIdentifier() {
      return "donutsell";
   }

   public @NotNull String getAuthor() {
      return "ᴇʟɪᴠʙ";
   }

   public @NotNull String getVersion() {
      return this.plugin.getDescription().getVersion();
   }

   public boolean persist() {
      return true;
   }

   public String onPlaceholderRequest(Player player, @NotNull String identifier) {
      if (this.plugin.getPlayerDataManager() == null) {
         return "0";
      } else {
         String identifierLower = identifier.toLowerCase();
         if (!identifierLower.equals("potions_money_me") && !identifierLower.equals("potion_money_me")) {
            if (!identifierLower.equals("potions_items_me") && !identifierLower.equals("potion_items_me")) {
               if (identifierLower.startsWith("sold_money_")) {
                  return this.getMoneyRank(identifierLower.substring(11), player);
               } else if (identifierLower.startsWith("sold_items_")) {
                  return this.getItemsRank(identifierLower.substring(11), player);
               } else {
                  switch (identifierLower) {
                     case "total_sold":
                        if (player == null) {
                           return "0";
                        }

                        double totalSold = this.plugin.getPlayerDataManager().getTotalSold(player);
                        return String.format("%.2f", totalSold);
                     case "total_items":
                        if (player == null) {
                           return "0";
                        }

                        int totalItems = this.plugin.getPlayerDataManager().getTotalSoldItems(player);
                        return String.valueOf(totalItems);
                     case "formatted_total":
                        if (player == null) {
                           return "0";
                        }

                        double totalAmount = this.plugin.getPlayerDataManager().getTotalSold(player);
                        return this.plugin.getCurrencyFormatter().format(totalAmount);
                     default:
                        return null;
                  }
               }
            } else {
               return this.getPlayerItemsRank(player);
            }
         } else {
            return this.getPlayerMoneyRank(player);
         }
      }
   }

   private String getPlayerMoneyRank(Player player) {
      if (player == null) {
         return "0";
      } else {
         try {
            List<PlayerMoneyData> allPlayers = this.getAllPlayersByMoney();

            for(int i = 0; i < allPlayers.size(); ++i) {
               PlayerMoneyData data = (PlayerMoneyData)allPlayers.get(i);
               if (data.getPlayerName().equalsIgnoreCase(player.getName())) {
                  return String.valueOf(i + 1);
               }
            }

            return String.valueOf(allPlayers.size() + 1);
         } catch (Exception var5) {
            return "0";
         }
      }
   }

   private String getPlayerItemsRank(Player player) {
      if (player == null) {
         return "0";
      } else {
         try {
            List<PlayerItemsData> allPlayers = this.getAllPlayersByItems();

            for(int i = 0; i < allPlayers.size(); ++i) {
               PlayerItemsData data = (PlayerItemsData)allPlayers.get(i);
               if (data.getPlayerName().equalsIgnoreCase(player.getName())) {
                  return String.valueOf(i + 1);
               }
            }

            return String.valueOf(allPlayers.size() + 1);
         } catch (Exception var5) {
            return "0";
         }
      }
   }

   private String getMoneyRank(String rank, Player currentPlayer) {
      try {
         int rankNumber = Integer.parseInt(rank);
         if (rankNumber < 1) {
            return "Invalid Rank";
         } else {
            List<PlayerMoneyData> topPlayers = this.getAllPlayersByMoney();
            if (rankNumber > topPlayers.size()) {
               return "---";
            } else {
               PlayerMoneyData data = (PlayerMoneyData)topPlayers.get(rankNumber - 1);
               String playerName = data.getPlayerName();
               double money = data.getMoney();
               if (currentPlayer != null && currentPlayer.getName().equalsIgnoreCase(playerName)) {
                  playerName = playerName + "§r";
               }

               String formattedMoney = this.plugin.getCurrencyFormatter().format(money);
               return playerName + " " + formattedMoney;
            }
         }
      } catch (NumberFormatException var10) {
         return "Invalid Rank";
      } catch (Exception var11) {
         return "Error";
      }
   }

   private String getItemsRank(String rank, Player currentPlayer) {
      try {
         int rankNumber = Integer.parseInt(rank);
         if (rankNumber < 1) {
            return "Invalid Rank";
         } else {
            List<PlayerItemsData> topPlayers = this.getAllPlayersByItems();
            if (rankNumber > topPlayers.size()) {
               return "---";
            } else {
               PlayerItemsData data = (PlayerItemsData)topPlayers.get(rankNumber - 1);
               String playerName = data.getPlayerName();
               int items = data.getItems();
               if (currentPlayer != null && currentPlayer.getName().equalsIgnoreCase(playerName)) {
                  playerName = playerName + "§r";
               }

               String formattedItems = this.formatNumber(items);
               return playerName + " " + formattedItems;
            }
         }
      } catch (NumberFormatException var9) {
         return "Invalid Rank";
      } catch (Exception var10) {
         return "Error";
      }
   }

   private List<PlayerMoneyData> getAllPlayersByMoney() {
      List<PlayerMoneyData> allPlayers = new ArrayList();

      for(Player player : Bukkit.getOnlinePlayers()) {
         double money = this.plugin.getPlayerDataManager().getTotalSold(player);
         allPlayers.add(new PlayerMoneyData(player.getName(), money, true));
      }

      for(String playerName : this.plugin.getPlayerDataManager().getAllPlayerNames()) {
         boolean isOnline = Bukkit.getPlayer(playerName) != null;
         if (!isOnline) {
            double money = this.plugin.getPlayerDataManager().getOfflineTotalSold(playerName);
            if (money > (double)0.0F) {
               allPlayers.add(new PlayerMoneyData(playerName, money, false));
            }
         }
      }

      return (List)((Map)allPlayers.stream().collect(Collectors.toMap(PlayerMoneyData::getPlayerName, (data) -> data, (existing, replacement) -> {
         if (existing.isOnline() && !replacement.isOnline()) {
            return existing;
         } else if (!existing.isOnline() && replacement.isOnline()) {
            return replacement;
         } else {
            return existing.getMoney() >= replacement.getMoney() ? existing : replacement;
         }
      }))).values().stream().sorted(Comparator.comparingDouble(PlayerMoneyData::getMoney).reversed()).collect(Collectors.toList());
   }

   private List<PlayerItemsData> getAllPlayersByItems() {
      List<PlayerItemsData> allPlayers = new ArrayList();

      for(Player player : Bukkit.getOnlinePlayers()) {
         int items = this.plugin.getPlayerDataManager().getTotalSoldItems(player);
         allPlayers.add(new PlayerItemsData(player.getName(), items, true));
      }

      for(String playerName : this.plugin.getPlayerDataManager().getAllPlayerNames()) {
         boolean isOnline = Bukkit.getPlayer(playerName) != null;
         if (!isOnline) {
            int items = this.plugin.getPlayerDataManager().getOfflineTotalSoldItems(playerName);
            if (items > 0) {
               allPlayers.add(new PlayerItemsData(playerName, items, false));
            }
         }
      }

      return (List)((Map)allPlayers.stream().collect(Collectors.toMap(PlayerItemsData::getPlayerName, (data) -> data, (existing, replacement) -> {
         if (existing.isOnline() && !replacement.isOnline()) {
            return existing;
         } else if (!existing.isOnline() && replacement.isOnline()) {
            return replacement;
         } else {
            return existing.getItems() >= replacement.getItems() ? existing : replacement;
         }
      }))).values().stream().sorted(Comparator.comparingInt(PlayerItemsData::getItems).reversed()).collect(Collectors.toList());
   }

   private String formatNumber(int number) {
      if (number >= 1000000) {
         return String.format("%.1fM", (double)number / (double)1000000.0F);
      } else {
         return number >= 1000 ? String.format("%.1fk", (double)number / (double)1000.0F) : String.valueOf(number);
      }
   }

   public boolean canRegister() {
      return true;
   }

   public boolean register() {
      if (!this.canRegister()) {
         return false;
      } else {
         try {
            return super.register();
         } catch (Exception var2) {
            return false;
         }
      }
   }

   private static class PlayerMoneyData {
      private final String playerName;
      private final double money;
      private final boolean online;

      public PlayerMoneyData(String playerName, double money, boolean online) {
         this.playerName = playerName;
         this.money = money;
         this.online = online;
      }

      public String getPlayerName() {
         return this.playerName;
      }

      public double getMoney() {
         return this.money;
      }

      public boolean isOnline() {
         return this.online;
      }
   }

   private static class PlayerItemsData {
      private final String playerName;
      private final int items;
      private final boolean online;

      public PlayerItemsData(String playerName, int items, boolean online) {
         this.playerName = playerName;
         this.items = items;
         this.online = online;
      }

      public String getPlayerName() {
         return this.playerName;
      }

      public int getItems() {
         return this.items;
      }

      public boolean isOnline() {
         return this.online;
      }
   }
}
