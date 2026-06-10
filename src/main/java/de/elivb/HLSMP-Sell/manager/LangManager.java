package de.elivb.donutSell.manager;

import de.elivb.donutSell.HexColorCode;
import de.elivb.donutSell.Sell;
import java.io.File;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class LangManager {
   private final Sell plugin;
   private FileConfiguration langConfig;
   private File langFile;
   private final Map<String, String> messages;
   private boolean prefixEnabled;
   private boolean sellSuccessChatEnabled;
   private List<String> sellSuccessChatMessages;
   private boolean sellSuccessActionbarEnabled;
   private List<String> sellSuccessActionbarMessages;
   private boolean sellSuccessTitleEnabled;
   private List<String> sellSuccessTitleMessages;
   private boolean sellSuccessSubtitleEnabled;
   private List<String> sellSuccessSubtitleMessages;

   public LangManager(Sell plugin) {
      this.plugin = plugin;
      this.messages = new HashMap();
      this.prefixEnabled = true;
      this.loadLangConfig();
   }

   public void loadLangConfig() {
      if (!this.plugin.getDataFolder().exists()) {
         this.plugin.getDataFolder().mkdirs();
      }

      this.langFile = new File(this.plugin.getDataFolder(), "lang.yml");
      if (!this.langFile.exists()) {
         try {
            InputStream inputStream = this.plugin.getResource("lang.yml");
            if (inputStream != null) {
               Files.copy(inputStream, this.langFile.toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
            } else {
               this.createDefaultLangFile();
            }
         } catch (Exception var2) {
            this.createDefaultLangFile();
         }
      }

      this.reloadLangConfig();
   }

   private void createDefaultLangFile() {
      try {
         this.langFile.createNewFile();
         YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(this.langFile);
         defaultConfig.set("prefix-enabled", false);
         defaultConfig.set("prefix", "&#E0E319&lSELL &7» ");
         defaultConfig.set("sell-success.module.chat.enabled", false);
         defaultConfig.set("sell-success.module.chat.message", Arrays.asList(""));
         defaultConfig.set("sell-success.module.actionbar.enabled", true);
         defaultConfig.set("sell-success.module.actionbar.message", Arrays.asList("&#00fc88+%price%"));
         defaultConfig.set("sell-success.module.title.enabled", true);
         defaultConfig.set("sell-success.module.title.message", Arrays.asList("&#00fc88+%price%"));
         defaultConfig.set("sell-success.module.subtitle.enabled", false);
         defaultConfig.set("sell-success.module.subtitle.message", Arrays.asList(""));
         defaultConfig.set("messages.no-permission", "&#FF0000You do not have permission for this!");
         defaultConfig.set("messages.plugin-reloaded", "&#0bf52bPlugin has been reloaded!");
         defaultConfig.set("messages.sellaxe-given", "&#0bf52bYou have received a Sell Axe!");
         defaultConfig.set("messages.sellaxe-give-to-player", "&#0bf52bSuccess!");
         defaultConfig.set("messages.sellaxe-broken", "&#FF0000Your Sell Axe has broken!");
         defaultConfig.set("messages.player-not-found", "&#FF0000Player not found!");
         defaultConfig.set("messages.player-only", "You must be a player to use this command!");
         defaultConfig.set("messages.sell-success", "&fYou sold &a%amount% items &ffor &#0bf52b%price% &f!");
         defaultConfig.set("messages.no-sellable-items", "&#FF0000You have no sellable items!");
         defaultConfig.set("messages.worth-toggle-enabled", "&#0bf52bWorth-Lore has been enabled!");
         defaultConfig.set("messages.worth-toggle-disabled", "&#FF0000Worth-Lore has been disabled!");
         defaultConfig.set("messages.material-not-exists", "&#FF0000The material does not exist!");
         defaultConfig.set("messages.available-categories", "&#0bf52bAvailable categories:");
         defaultConfig.set("messages.category-list", "&#0bf52b- &f%category%");
         defaultConfig.set("messages.material-created", "&#0bf52bMaterial %material% has been created in category %category% with price %price%!");
         defaultConfig.set("messages.material-updated", "&#0bf52bMaterial %material% has been updated in category %category% with price %price%!");
         defaultConfig.set("messages.enchantedbook-created", "&#0bf52bEnchanted Book with enchantment %enchantment% as been created in category %category% with price %price%!");
         defaultConfig.set("messages.enchantedbook-updated", "&#0bf52bEnchanted Book with enchantment %enchantment% as been updated in category %category% with price %price%!");
         defaultConfig.set("messages.potion-created", "&#0bf52bPotion %potion% has been created in category %category% with price %price%!");
         defaultConfig.set("messages.potion-updated", "&#0bf52bPotion %potion% has been updated in category %category% with price %price%!");
         defaultConfig.set("messages.no-item-in-hand", "&#FF0000You have no item in your hand!");
         defaultConfig.set("messages.invalid-price", "&#FF0000Invalid price!");
         defaultConfig.set("messages.player-reset", "&#0bf52bPlayer has been reset!");
         defaultConfig.set("action-bars.plugin-reloaded", "&#0bf52bPlugin has been reloaded!");
         defaultConfig.set("action-bars.no-sellable-items", "&#FF0000You have no sellable items!");
         defaultConfig.set("action-bars.item-worth", "&f%item% is &#0bf52b%price% &fworth");
         defaultConfig.set("action-bars.stack-worth", "&f%item% &7(x%amount%) is &#0bf52b%total% &fworth &7(%single% each)");
         defaultConfig.set("action-bars.shulker-worth", "&f%item% is &#0bf52b%price% &fworth &7(%count% items)");
         defaultConfig.set("action-bars.shulker-empty", "&f%item% is &#0bf52b%price% &fworth &7(empty)");
         defaultConfig.set("action-bars.item-not-sellable", "&#FF0000%item% this Item can´t sell");
         defaultConfig.set("action-bars.no-item-in-hand", "&#FF0000You have no Item in your hand");
         defaultConfig.set("action-bars.invalid-item", "&#FF0000Invalid item!");
         defaultConfig.set("action-bars.worth-toggle-on", "&#0bf52b✓ Worth-Lore is now visible");
         defaultConfig.set("action-bars.worth-toggle-off", "&#FF0000✗ Worth-Lore is now hidden");
         defaultConfig.set("command-usage", "&#E0E319&lSELL\n&#E0E319➤ &f/sell\n&#E0E319➤ &f/sell help - &#E0E319show this message\n&#E0E319➤ &f/worth - &#E0E319Worth GUI\n&#E0E319➤ &f/worth <item> - &#E0E319It shows you the price of the selected item\n&#E0E319➤ &f/worth hand - &#E0E319Shows you the price of the selected item\n&#E0E319➤ &f/worthtoggle - &#E0E319Toggle worth lore on/off\n&#E0E319➤ &f/sellhistory - &#E0E319Shows your sell history");
         defaultConfig.set("command-usage-admin", "&#FF0000&lSELL ADMIN\n&#FF0000➤ &f/sell reload\n&#FF0000➤ &f/sellaxe <player> - &#FF0000Gives a Sell Axe\n&#FF0000➤ &f/setworth <item> <price> - &#FF0000Sets the worth of an item\n&#FF0000➤ &f/setworth <item> <price> <category> - &#FF0000Sets the worth of an item with category\n&#FF0000➤ &f/setworth hand <price> - &#FF0000Sets the worth of the item in your hand\n&#FF0000➤ &f/setworth hand <price> <category> - &#FF0000Sets the worth of the item in your hand with category");
         defaultConfig.save(this.langFile);
      } catch (Exception var2) {
         var2.printStackTrace();
      }

   }

   public void reloadLangConfig() {
      this.langConfig = YamlConfiguration.loadConfiguration(this.langFile);
      this.messages.clear();

      for(String key : this.langConfig.getKeys(true)) {
         if (this.langConfig.isString(key) && !key.startsWith("#")) {
            this.messages.put(key, this.langConfig.getString(key));
         }
      }

      this.prefixEnabled = this.langConfig.getBoolean("prefix-enabled", false);
      this.loadSellSuccessModules();
   }

   private void loadSellSuccessModules() {
      String basePath = "sell-success.module.";
      this.sellSuccessChatEnabled = this.langConfig.getBoolean(basePath + "chat.enabled", false);
      this.sellSuccessChatMessages = this.langConfig.getStringList(basePath + "chat.message");
      this.sellSuccessActionbarEnabled = this.langConfig.getBoolean(basePath + "actionbar.enabled", true);
      this.sellSuccessActionbarMessages = this.langConfig.getStringList(basePath + "actionbar.message");
      this.sellSuccessTitleEnabled = this.langConfig.getBoolean(basePath + "title.enabled", true);
      this.sellSuccessTitleMessages = this.langConfig.getStringList(basePath + "title.message");
      this.sellSuccessSubtitleEnabled = this.langConfig.getBoolean(basePath + "subtitle.enabled", false);
      this.sellSuccessSubtitleMessages = this.langConfig.getStringList(basePath + "subtitle.message");
   }

   public String getMessage(String key) {
      return this.getMessage(key, (Map)null, true);
   }

   public String getMessage(String key, Map<String, String> placeholders) {
      return this.getMessage(key, placeholders, true);
   }

   public String getMessage(String key, Map<String, String> placeholders, boolean withPrefix) {
      String message = (String)this.messages.getOrDefault(key, "Message not found: " + key);
      if (withPrefix && this.prefixEnabled && !key.startsWith("title-") && !key.startsWith("subtitle-") && !key.startsWith("action-bars.") && !key.startsWith("command-usage") && !key.equals("command-usage") && !key.equals("command-usage-admin") && this.messages.containsKey("prefix")) {
         String var10000 = (String)this.messages.get("prefix");
         message = var10000 + message;
      }

      if (placeholders != null) {
         for(Map.Entry<String, String> entry : placeholders.entrySet()) {
            String placeholderName = (String)entry.getKey();
            String value = (String)entry.getValue();
            message = message.replace("{" + placeholderName + "}", value);
            message = message.replace("%" + placeholderName + "%", value);
         }
      }

      return HexColorCode.translateAllColorCodes(message);
   }

   public String getMessageWithoutPrefix(String key) {
      return this.getMessage(key, (Map)null, false);
   }

   public String getMessageWithoutPrefix(String key, Map<String, String> placeholders) {
      return this.getMessage(key, placeholders, false);
   }

   public List<String> getMessageList(String key) {
      return this.getMessageList(key, true);
   }

   public List<String> getMessageList(String key, boolean withPrefix) {
      if (this.langConfig.contains(key) && this.langConfig.isList(key)) {
         List<String> list = this.langConfig.getStringList(key);
         if (withPrefix && this.prefixEnabled && !key.startsWith("title-") && !key.startsWith("subtitle-") && !key.startsWith("action-bars.") && !key.startsWith("command-usage") && !key.equals("command-usage") && !key.equals("command-usage-admin") && this.messages.containsKey("prefix")) {
            String prefix = (String)this.messages.get("prefix");
            list.replaceAll((line) -> prefix + line);
         }

         list.replaceAll(HexColorCode::translateAllColorCodes);
         return list;
      } else {
         return List.of();
      }
   }

   public String getPrefix() {
      String prefix = (String)this.messages.getOrDefault("prefix", "&#E0E319&lSELL &7» ");
      return HexColorCode.translateAllColorCodes(prefix);
   }

   public boolean isPrefixEnabled() {
      return this.prefixEnabled;
   }

   public void setPrefixEnabled(boolean enabled) {
      this.prefixEnabled = enabled;
      this.langConfig.set("prefix-enabled", enabled);
      this.saveLangConfig();
   }

   public void sendTitle(Player player, String titleKey, String subtitleKey, Map<String, String> placeholders, int fadeIn, int stay, int fadeOut) {
      String title = "";
      String subtitle = "";
      if (titleKey != null) {
         title = this.getMessageWithoutPrefix(titleKey, placeholders);
      }

      if (subtitleKey != null) {
         subtitle = this.getMessageWithoutPrefix(subtitleKey, placeholders);
      }

      player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
   }

   public void sendTitle(Player player, String titleKey, String subtitleKey, Map<String, String> placeholders) {
      this.sendTitle(player, titleKey, subtitleKey, placeholders, 10, 70, 20);
   }

   public void sendTitle(Player player, String titleKey, Map<String, String> placeholders) {
      this.sendTitle(player, titleKey, (String)null, placeholders);
   }

   public void sendTitle(Player player, String titleKey, String subtitleKey) {
      this.sendTitle(player, titleKey, subtitleKey, (Map)null);
   }

   public void sendTitle(Player player, String titleKey) {
      this.sendTitle(player, titleKey, (String)null, (Map)null);
   }

   public void sendSellSuccess(Player player, int amount, String formattedPrice, Map<String, String> extraPlaceholders) {
      Map<String, String> placeholders = new HashMap();
      placeholders.put("amount", String.valueOf(amount));
      placeholders.put("price", formattedPrice);
      if (extraPlaceholders != null) {
         placeholders.putAll(extraPlaceholders);
      }

      if (this.sellSuccessChatEnabled && this.sellSuccessChatMessages != null && !this.sellSuccessChatMessages.isEmpty()) {
         for(String line : this.sellSuccessChatMessages) {
            String processed = line;

            for(Map.Entry<String, String> entry : placeholders.entrySet()) {
               processed = processed.replace("%" + (String)entry.getKey() + "%", (CharSequence)entry.getValue());
            }

            player.sendMessage(HexColorCode.translateAllColorCodes(processed));
         }
      }

      if (this.sellSuccessActionbarEnabled && this.sellSuccessActionbarMessages != null && !this.sellSuccessActionbarMessages.isEmpty()) {
         String actionbarMessage = (String)this.sellSuccessActionbarMessages.get(0);

         for(Map.Entry<String, String> entry : placeholders.entrySet()) {
            actionbarMessage = actionbarMessage.replace("%" + (String)entry.getKey() + "%", (CharSequence)entry.getValue());
         }

         player.sendActionBar(HexColorCode.translateAllColorCodes(actionbarMessage));
      }

      String titleMessage = null;
      String subtitleMessage = null;
      if (this.sellSuccessTitleEnabled && this.sellSuccessTitleMessages != null && !this.sellSuccessTitleMessages.isEmpty()) {
         titleMessage = (String)this.sellSuccessTitleMessages.get(0);

         for(Map.Entry<String, String> entry : placeholders.entrySet()) {
            titleMessage = titleMessage.replace("%" + (String)entry.getKey() + "%", (CharSequence)entry.getValue());
         }

         titleMessage = HexColorCode.translateAllColorCodes(titleMessage);
      }

      if (this.sellSuccessSubtitleEnabled && this.sellSuccessSubtitleMessages != null && !this.sellSuccessSubtitleMessages.isEmpty()) {
         subtitleMessage = (String)this.sellSuccessSubtitleMessages.get(0);

         for(Map.Entry<String, String> entry : placeholders.entrySet()) {
            subtitleMessage = subtitleMessage.replace("%" + (String)entry.getKey() + "%", (CharSequence)entry.getValue());
         }

         subtitleMessage = HexColorCode.translateAllColorCodes(subtitleMessage);
      }

      if (titleMessage != null || subtitleMessage != null) {
         player.sendTitle(titleMessage != null ? titleMessage : "", subtitleMessage != null ? subtitleMessage : "", 10, 70, 20);
      }

   }

   public void sendSellSuccess(Player player, int amount, double price) {
      this.sendSellSuccess(player, amount, this.plugin.getCurrencyFormatter().format(price), (Map)null);
   }

   public void saveLangConfig() {
      try {
         this.langConfig.save(this.langFile);
      } catch (Exception var2) {
         var2.printStackTrace();
      }

   }

   public FileConfiguration getLangConfig() {
      return this.langConfig;
   }

   public void setMessage(String key, String value) {
      this.langConfig.set(key, value);
      this.messages.put(key, value);
      this.saveLangConfig();
   }

   public void debugPlaceholders(String key, Map<String, String> placeholders) {
      if (placeholders != null) {
         for(Map.Entry var5 : placeholders.entrySet()) {
            ;
         }
      }

      this.getMessage(key, placeholders);
   }

   public boolean isSellSuccessChatEnabled() {
      return this.sellSuccessChatEnabled;
   }

   public List<String> getSellSuccessChatMessages() {
      return this.sellSuccessChatMessages;
   }

   public boolean isSellSuccessActionbarEnabled() {
      return this.sellSuccessActionbarEnabled;
   }

   public List<String> getSellSuccessActionbarMessages() {
      return this.sellSuccessActionbarMessages;
   }

   public boolean isSellSuccessTitleEnabled() {
      return this.sellSuccessTitleEnabled;
   }

   public List<String> getSellSuccessTitleMessages() {
      return this.sellSuccessTitleMessages;
   }

   public boolean isSellSuccessSubtitleEnabled() {
      return this.sellSuccessSubtitleEnabled;
   }

   public List<String> getSellSuccessSubtitleMessages() {
      return this.sellSuccessSubtitleMessages;
   }
}
