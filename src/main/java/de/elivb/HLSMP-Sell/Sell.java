package de.elivb.donutSell;

import de.elivb.donutSell.gui.MainGUI;
import de.elivb.donutSell.gui.SellHistoryGUI;
import de.elivb.donutSell.manager.LangManager;
import de.elivb.donutSell.manager.LevelManager;
import de.elivb.donutSell.manager.PlayerDataManager;
import de.elivb.donutSell.manager.SellAxeManager;
import de.elivb.donutSell.manager.SellManager;
import de.elivb.donutSell.manager.SetWorthManager;
import de.elivb.donutSell.manager.WorthItemManager;
import de.elivb.donutSell.manager.WorthManager;
import de.elivb.donutSell.placeholders.DonutPlaceholderExpansion;
import de.elivb.donutSell.utils.CurrencyFormatter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Sell extends JavaPlugin implements TabCompleter, Listener {
   private Economy economy;
   private CurrencyFormatter currencyFormatter;
   private LangManager langManager;
   private SellManager sellManager;
   private SellAxeManager sellAxeManager;
   private WorthManager worthManager;
   private WorthItemManager worthItemManager;
   private PlayerDataManager playerDataManager;
   private SetWorthManager setWorthManager;
   private LevelManager levelManager;
   private boolean isEnabled = false;
   private boolean economyEnabled = false;
   private boolean shulkerSupportEnabled = true;
   private boolean isFolia = false;
   private LicenseManager licenseManager;

   public void onEnable() {
      this.licenseManager = new LicenseManager(this);
      if (this.licenseManager.validateLicenseOnStartup()) {
         this.saveDefaultConfig();
         this.langManager = new LangManager(this);
         this.currencyFormatter = new CurrencyFormatter(this.getConfig());
         this.playerDataManager = new PlayerDataManager(this);
         MainGUI.loadConfig();
         this.registerCommands();
         this.getServer().getPluginManager().registerEvents(this, this);

         try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            this.isFolia = true;
         } catch (ClassNotFoundException var3) {
            this.isFolia = false;
         }

         this.delayedEnable();

         try {
            this.ensureManagersInitialized();
            if (this.worthManager != null) {
            }
         } catch (Throwable var2) {
         }

      }
   }

   private void registerCommands() {
      try {
         Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
         commandMapField.setAccessible(true);
         CommandMap commandMap = (CommandMap)commandMapField.get(Bukkit.getServer());
         PluginCommand sellCommand = this.getCommand("sell");
         if (sellCommand != null) {
            List<String> aliases = this.getConfig().getStringList("command-aliases");
            sellCommand.setAliases(aliases);
            sellCommand.setExecutor(this);
            sellCommand.setTabCompleter(this);

            for(String alias : aliases) {
               if (!alias.equals("sell")) {
                  PluginCommand aliasCommand = this.createCommand(alias);
                  if (aliasCommand != null) {
                     aliasCommand.setExecutor(this);
                     aliasCommand.setTabCompleter(this);
                     aliasCommand.setPermission("sell.use");
                     aliasCommand.setUsage("/" + alias);
                     aliasCommand.setLabel(alias);
                     aliasCommand.setAliases(new ArrayList());
                     commandMap.register("", aliasCommand);
                  }
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   private PluginCommand createCommand(String name) {
      try {
         Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
         constructor.setAccessible(true);
         return (PluginCommand)constructor.newInstance(name, this);
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }

   public LicenseManager getLicenseManager() {
      return this.licenseManager;
   }

   private void delayedEnable() {
      this.runTask(() -> {
         try {
            this.economyEnabled = this.setupEconomy();
            if (!this.economyEnabled) {
            }

            this.shulkerSupportEnabled = this.getConfig().getBoolean("shulker-support.enabled", true);
            this.sellManager = new SellManager(this, this.economy, this.currencyFormatter, this.langManager, this.playerDataManager, this.isFolia);
            if (this.sellManager != null) {
               try {
                  this.worthManager = new WorthManager(this.sellManager.getPriceManager(), this.currencyFormatter, this.sellManager, this.isFolia);
                  this.getServer().getPluginManager().registerEvents(this.worthManager, this);
                  this.worthItemManager = new WorthItemManager(this.sellManager.getPriceManager(), this.currencyFormatter, this.langManager, this.sellManager);
                  this.sellAxeManager = new SellAxeManager(this, this.sellManager.getPriceManager(), this.currencyFormatter, this.langManager, this.playerDataManager, this.isFolia);
                  this.setWorthManager = new SetWorthManager(this, this.sellManager.getPriceManager(), this.currencyFormatter, this.langManager);
                  this.levelManager = new LevelManager(this, this.currencyFormatter, this.sellManager.getPriceManager());
               } catch (Throwable t) {
                  t.printStackTrace();
               }
            }

            this.setupPlaceholderAPI();
            if (this.playerDataManager.getMySQLManager().isEnabled() && this.playerDataManager.getMySQLManager().isDatabaseEmpty()) {
               this.playerDataManager.getMySQLManager().migrateFromFiles(this.playerDataManager);
            }

            for(Player player : Bukkit.getOnlinePlayers()) {
               try {
                  this.playerDataManager.loadPlayerConfig(player);
               } catch (Throwable var4) {
               }
            }

            this.isEnabled = true;
         } catch (Throwable t) {
            this.isEnabled = false;
            t.printStackTrace();
         }

      });
   }

   public void onDisable() {
      if (this.isEnabled) {
         for(Player player : Bukkit.getOnlinePlayers()) {
            this.playerDataManager.unloadPlayerConfig(player);
         }
      }

   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      if (this.isEnabled) {
         Player player = event.getPlayer();
         this.playerDataManager.loadPlayerConfig(player);
         if (this.sellAxeManager != null) {
            this.runTaskLater(() -> this.sellAxeManager.checkPlayerInventory(player), 20L);
         }

         if (!this.economyEnabled && player.hasPermission("sell.admin")) {
            this.runTaskLater(() -> {
               if (player.isOnline()) {
               }

            }, 40L);
         }
      }

   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      if (this.isEnabled) {
         Player player = event.getPlayer();
         this.playerDataManager.unloadPlayerConfig(player);
      }

   }

   @EventHandler
   public void onInventoryOpen(InventoryOpenEvent event) {
      if (this.isEnabled && this.sellAxeManager != null && event.getPlayer() instanceof Player) {
         Player player = (Player)event.getPlayer();
         this.runTask(() -> this.sellAxeManager.checkPlayerInventory(player));
      }

   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      if (this.isEnabled && this.sellAxeManager != null && event.getPlayer() instanceof Player) {
         Player player = (Player)event.getPlayer();
         this.runTask(() -> this.sellAxeManager.checkPlayerInventory(player));
      }

   }

   @EventHandler
   public void onItemPickup(EntityPickupItemEvent event) {
      if (this.isEnabled && this.sellAxeManager != null && event.getEntity() instanceof Player) {
         Player player = (Player)event.getEntity();
         this.runTaskLater(() -> this.sellAxeManager.checkPlayerInventory(player), 2L);
      }

   }

   @EventHandler
   public void onItemDrop(PlayerDropItemEvent event) {
      if (this.isEnabled && this.sellAxeManager != null) {
         Player player = event.getPlayer();
         this.runTask(() -> this.sellAxeManager.checkPlayerInventory(player));
      }

   }

   private boolean setupEconomy() {
      if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
         this.getLogger().warning("\u001b[93mVault not found!\u001b[0m");
         return false;
      } else {
         RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
         if (rsp == null) {
            this.getLogger().warning("\u001b[93mNo economy provider found!\u001b[0m");
            return false;
         } else {
            this.economy = (Economy)rsp.getProvider();
            if (this.economy == null) {
               return false;
            } else {
               this.getLogger().info("\u001b[96mEconomy found: " + this.economy.getName() + "\u001b[0m");
               return true;
            }
         }
      }
   }

   private void setupPlaceholderAPI() {
      if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
         (new DonutPlaceholderExpansion(this)).register();
      }

   }

   private void ensureManagersInitialized() {
      if (this.sellManager == null) {
         try {
            this.sellManager = new SellManager(this, this.economy, this.currencyFormatter, this.langManager, this.playerDataManager, this.isFolia);
         } catch (Throwable t) {
            t.printStackTrace();
            this.initManagersSync();
            if (this.sellManager == null) {
               return;
            }
         }
      }

      if (this.worthManager == null) {
         try {
            this.worthManager = new WorthManager(this.sellManager.getPriceManager(), this.currencyFormatter, this.sellManager, this.isFolia);
            this.getServer().getPluginManager().registerEvents(this.worthManager, this);
         } catch (Throwable t) {
            t.printStackTrace();
            this.initManagersSync();
         }
      }

      if (this.worthItemManager == null) {
         try {
            this.worthItemManager = new WorthItemManager(this.sellManager.getPriceManager(), this.currencyFormatter, this.langManager, this.sellManager);
         } catch (Throwable t) {
            t.printStackTrace();
            this.initManagersSync();
         }
      }

      if (this.sellAxeManager == null) {
         try {
            this.sellAxeManager = new SellAxeManager(this, this.sellManager.getPriceManager(), this.currencyFormatter, this.langManager, this.playerDataManager, this.isFolia);
         } catch (Throwable t) {
            t.printStackTrace();
            this.initManagersSync();
         }
      }

      if (this.setWorthManager == null) {
         try {
            this.setWorthManager = new SetWorthManager(this, this.sellManager.getPriceManager(), this.currencyFormatter, this.langManager);
         } catch (Throwable t) {
            t.printStackTrace();
            this.initManagersSync();
         }
      }

      if (this.levelManager == null && this.sellManager != null && this.sellManager.getPriceManager() != null) {
         try {
            this.levelManager = new LevelManager(this, this.currencyFormatter, this.sellManager.getPriceManager());
         } catch (Throwable t) {
            t.printStackTrace();
         }
      }

   }

   private void initManagersSync() {
      try {
         this.economyEnabled = this.setupEconomy();
         this.shulkerSupportEnabled = this.getConfig().getBoolean("shulker-support.enabled", true);
         if (this.sellManager == null) {
            this.sellManager = new SellManager(this, this.economy, this.currencyFormatter, this.langManager, this.playerDataManager, this.isFolia);
         }

         if (this.worthManager == null && this.sellManager != null) {
            this.worthManager = new WorthManager(this.sellManager.getPriceManager(), this.currencyFormatter, this.sellManager, this.isFolia);
            this.getServer().getPluginManager().registerEvents(this.worthManager, this);
         }

         if (this.worthItemManager == null && this.sellManager != null) {
            this.worthItemManager = new WorthItemManager(this.sellManager.getPriceManager(), this.currencyFormatter, this.langManager, this.sellManager);
         }

         if (this.sellAxeManager == null && this.sellManager != null) {
            this.sellAxeManager = new SellAxeManager(this, this.sellManager.getPriceManager(), this.currencyFormatter, this.langManager, this.playerDataManager, this.isFolia);
         }

         if (this.setWorthManager == null && this.sellManager != null) {
            this.setWorthManager = new SetWorthManager(this, this.sellManager.getPriceManager(), this.currencyFormatter, this.langManager);
         }

         if (this.levelManager == null && this.sellManager != null && this.sellManager.getPriceManager() != null) {
            this.levelManager = new LevelManager(this, this.currencyFormatter, this.sellManager.getPriceManager());
         }

         this.setupPlaceholderAPI();

         for(Player p : Bukkit.getOnlinePlayers()) {
            try {
               this.playerDataManager.loadPlayerConfig(p);
            } catch (Throwable var4) {
            }
         }

         this.isEnabled = true;
      } catch (Throwable t) {
         t.printStackTrace();
      }

   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      this.ensureManagersInitialized();
      List<String> aliases = this.getConfig().getStringList("command-aliases");
      boolean isSellCommand = command.getName().equalsIgnoreCase("sell") || aliases.contains(command.getName().toLowerCase());
      if (isSellCommand) {
         if (args.length >= 1 && args[0].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("sell.reset")) {
               sender.sendMessage(this.langManager.getMessage("messages.no-permission"));
               return true;
            }

            if (args.length < 2) {
               return true;
            }

            String targetName = args[1];
            Player targetPlayer = Bukkit.getPlayer(targetName);
            if (targetPlayer != null && targetPlayer.isOnline()) {
               this.playerDataManager.resetPlayerData(targetPlayer);
               sender.sendMessage(this.langManager.getMessage("messages.player-reset"));
            } else if (this.playerDataManager.hasPlayerData(targetName)) {
               this.playerDataManager.resetOfflinePlayerData(targetName);
               sender.sendMessage(this.langManager.getMessage("messages.player-reset"));
            } else {
               sender.sendMessage(this.langManager.getMessage("messages.player-not-found"));
            }

            return true;
         }

         if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("sell.reload")) {
               sender.sendMessage(this.langManager.getMessage("messages.no-permission"));
               return true;
            }

            this.reloadConfig();
            this.currencyFormatter = new CurrencyFormatter(this.getConfig());
            this.langManager.reloadLangConfig();
            MainGUI.reloadConfig();
            if (this.sellManager != null) {
               this.sellManager.reloadConfig();
            }

            if (this.sellAxeManager != null) {
               this.sellAxeManager.reloadConfig();
            }

            if (this.worthManager != null) {
               this.worthManager.reloadConfig();
            }

            if (this.setWorthManager != null) {
               this.setWorthManager.reloadSounds();
            }

            if (this.levelManager != null) {
               this.levelManager.reloadConfig();
            }

            SellHistoryGUI.reloadConfig();
            this.shulkerSupportEnabled = this.getConfig().getBoolean("shulker-support.enabled", true);
            this.getLogger().info("\u001b[96mReloading configuration...\u001b[0m");
            this.getLogger().info("\u001b[91m╠ Reloaded config.yml!\u001b[0m");
            this.getLogger().info("\u001b[91m╠ Reloaded lang.yml!\u001b[0m");
            this.getLogger().info("\u001b[91m╠ Reloaded level.yml!\u001b[0m");
            this.getLogger().info("\u001b[91m╠ Reloaded mysql.yml!\u001b[0m");
            this.getLogger().info("\u001b[91m╠ Reloaded sellaxe.yml!\u001b[0m");
            this.getLogger().info("\u001b[91m╠ Reloaded GUI.yml!\u001b[0m");
            this.getLogger().info("\u001b[96m╚ Successful reload!\u001b[0m");
            sender.sendMessage(this.langManager.getMessage("messages.plugin-reloaded"));
            if (sender instanceof Player) {
               Player player = (Player)sender;
               player.sendActionBar(this.langManager.getMessage("action-bars.plugin-reloaded"));
               if (this.sellManager != null) {
                  this.sellManager.playReloadSound(player);
               }
            }

            return true;
         }

         if (this.isEnabled) {
            if (!(sender instanceof Player)) {
               sender.sendMessage(this.langManager.getMessage("messages.player-only"));
               return true;
            }

            if (args.length > 0) {
               if (!args[0].equalsIgnoreCase("help") && !args[0].equalsIgnoreCase("?")) {
                  sender.sendMessage(this.langManager.getMessageWithoutPrefix("command-usage"));
                  return true;
               }

               this.sendHelp(sender);
               return true;
            }

            Player player = (Player)sender;
            if (this.sellManager != null) {
               this.sellManager.openSellGUI(player);
            }

            return true;
         }

         this.ensureManagersInitialized();
      } else {
         if (command.getName().equalsIgnoreCase("worth")) {
            if (!(sender instanceof Player)) {
               return true;
            }

            Player player = (Player)sender;
            if (!player.hasPermission("sell.use")) {
               player.sendMessage(this.langManager.getMessage("messages.no-permission"));
               return true;
            }

            if (args.length == 0) {
               if (this.sellManager != null) {
                  this.sellManager.openWorthGUI(player);
               }

               return true;
            }

            if (args.length == 1) {
               if (args[0].equalsIgnoreCase("hand")) {
                  if (this.worthItemManager != null) {
                     this.worthItemManager.showHeldItemWorth(player);
                  }

                  return true;
               }

               String itemName = args[0].toUpperCase();

               try {
                  Material material = Material.valueOf(itemName);
                  if (this.worthItemManager != null) {
                     this.worthItemManager.showItemWorth(player, material);
                  }
               } catch (IllegalArgumentException var15) {
               }

               return true;
            }

            return true;
         }

         if (command.getName().equalsIgnoreCase("sellaxe")) {
            if (!sender.hasPermission("sell.sellaxe")) {
               sender.sendMessage(this.langManager.getMessage("messages.no-permission"));
               return true;
            }

            if (args.length == 0) {
               sender.sendMessage(this.langManager.getMessageWithoutPrefix("command-usage-admin"));
               return true;
            }

            if (args.length == 1) {
               Player target = Bukkit.getPlayer(args[0]);
               if (target == null) {
                  sender.sendMessage(this.langManager.getMessage("messages.player-not-found"));
                  return true;
               }

               ItemStack sellAxe = this.sellAxeManager.createSellAxe();
               target.getInventory().addItem(new ItemStack[]{sellAxe});
               sender.sendMessage(this.langManager.getMessage("messages.sellaxe-give-to-player"));
               target.sendMessage(this.langManager.getMessage("messages.sellaxe-given"));
               return true;
            }
         } else {
            if (command.getName().equalsIgnoreCase("worthtoggle")) {
               if (!(sender instanceof Player)) {
                  sender.sendMessage(this.langManager.getMessage("messages.player-only"));
                  return true;
               }

               Player player = (Player)sender;
               if (!player.hasPermission("sell.use")) {
                  player.sendMessage(this.langManager.getMessage("messages.no-permission"));
                  return true;
               }

               if (this.worthManager == null) {
                  try {
                     this.ensureManagersInitialized();
                  } catch (Throwable var17) {
                  }

                  if (this.worthManager == null) {
                     try {
                        if (this.sellManager != null && this.sellManager.getPriceManager() != null) {
                           this.worthManager = new WorthManager(this.sellManager.getPriceManager(), this.currencyFormatter, this.sellManager, this.isFolia);
                           this.getServer().getPluginManager().registerEvents(this.worthManager, this);
                        }
                     } catch (Throwable t) {
                        t.printStackTrace();
                     }
                  }
               }

               if (this.worthManager != null) {
                  this.worthManager.togglePlayerWorthLore(player);
               }

               return true;
            }

            if (command.getName().equalsIgnoreCase("sellhistory")) {
               if (args.length >= 1) {
                  if (!sender.hasPermission("sell.history.other")) {
                     sender.sendMessage(this.langManager.getMessage("messages.no-permission"));
                     return true;
                  }

                  String targetName = args[0];
                  Player targetPlayer = Bukkit.getPlayer(targetName);
                  if (targetPlayer != null && targetPlayer.isOnline()) {
                     if (!(sender instanceof Player)) {
                        sender.sendMessage(this.langManager.getMessage("messages.player-only"));
                        return true;
                     }

                     Player viewer = (Player)sender;
                     if (this.sellManager != null) {
                        this.sellManager.openSellHistoryForPlayer(viewer, targetPlayer);
                     }
                  } else {
                     if (!this.playerDataManager.hasPlayerData(targetName)) {
                        sender.sendMessage(this.langManager.getMessage("messages.player-not-found"));
                        return true;
                     }

                     if (sender instanceof Player) {
                        Player viewer = (Player)sender;
                        FileConfiguration offlineConfig = this.playerDataManager.getOfflinePlayerConfig(targetName);
                        Map<String, Object> offlineStats = this.playerDataManager.getOfflinePlayerStats(targetName);
                        if (this.sellManager != null) {
                           this.sellManager.openOfflineSellHistoryGUI(viewer, targetName, offlineConfig, offlineStats);
                        }
                     } else {
                        this.playerDataManager.getOfflineTotalSold(targetName);
                        this.playerDataManager.getOfflineTotalSoldItems(targetName);
                     }
                  }

                  return true;
               }

               if (!(sender instanceof Player)) {
                  sender.sendMessage(this.langManager.getMessage("messages.player-only"));
                  return true;
               }

               Player player = (Player)sender;
               if (!player.hasPermission("sell.use")) {
                  player.sendMessage(this.langManager.getMessage("messages.no-permission"));
                  return true;
               }

               if (this.sellManager != null) {
                  this.sellManager.openSellHistoryGUI(player);
               }

               return true;
            }

            if (command.getName().equalsIgnoreCase("setworth")) {
               if (!sender.hasPermission("sell.setworth")) {
                  sender.sendMessage(this.langManager.getMessage("messages.no-permission"));
                  return true;
               }

               if (args.length < 2) {
                  new HashMap();
                  sender.sendMessage(this.langManager.getMessageWithoutPrefix("command-usage-admin"));
                  return true;
               }

               this.ensureManagersInitialized();
               if (this.setWorthManager == null) {
                  return true;
               }

               String subCommand = args[0].toLowerCase();
               if (subCommand.equals("hand")) {
                  if (!(sender instanceof Player)) {
                     sender.sendMessage(this.langManager.getMessage("messages.player-only"));
                     return true;
                  }

                  Player player = (Player)sender;
                  ItemStack item = player.getInventory().getItemInMainHand();
                  if (item != null && item.getType() != Material.AIR) {
                     double price = this.currencyFormatter.parseAbbreviatedAmount(args[1]);
                     if (price <= (double)0.0F) {
                        Map<String, String> placeholders = new HashMap();
                        placeholders.put("price", args[1]);
                        player.sendMessage(this.langManager.getMessage("messages.invalid-price", placeholders));
                        return true;
                     }

                     String targetCategory = args.length >= 3 ? args[2] : null;
                     if (item.getType() == Material.ENCHANTED_BOOK) {
                        String enchantKey = this.setWorthManager.getEnchantedBookKeyFromItem(item);
                        if (enchantKey != null) {
                           this.setWorthManager.setEnchantedBookPrice(player, enchantKey, price, targetCategory);
                        } else {
                           player.sendMessage(this.langManager.getMessage("messages.material-not-exists"));
                        }
                     } else if (item.getType().toString().contains("POTION") && item.hasItemMeta() && item.getItemMeta() instanceof PotionMeta) {
                        String potionKey = this.setWorthManager.getPotionKeyFromItem(item);
                        if (potionKey != null) {
                           this.setWorthManager.setPotionPrice(player, potionKey, price, targetCategory);
                        } else {
                           player.sendMessage(this.langManager.getMessage("messages.material-not-exists"));
                        }
                     } else {
                        this.setWorthManager.setMaterialPrice(player, item.getType(), price, targetCategory);
                     }

                     return true;
                  }

                  Map<String, String> placeholders = new HashMap();
                  player.sendMessage(this.langManager.getMessage("messages.no-item-in-hand", placeholders));
                  return true;
               }

               if (args.length >= 2) {
                  String materialName = args[0].toUpperCase();
                  double price = this.currencyFormatter.parseAbbreviatedAmount(args[1]);
                  if (price <= (double)0.0F) {
                     Map<String, String> placeholders = new HashMap();
                     placeholders.put("price", args[1]);
                     sender.sendMessage(this.langManager.getMessage("messages.invalid-price", placeholders));
                     return true;
                  }

                  String targetCategory = args.length >= 3 ? args[2] : null;
                  if (!(sender instanceof Player)) {
                     sender.sendMessage(this.langManager.getMessage("messages.player-only"));
                     return true;
                  }

                  Player player = (Player)sender;
                  if (materialName.startsWith("ENCHANTED_BOOK:")) {
                     String enchantKey = materialName.substring(15);
                     this.setWorthManager.setEnchantedBookPrice(player, enchantKey, price, targetCategory);
                  } else if (materialName.contains("POTION:")) {
                     this.setWorthManager.setPotionPrice(player, materialName, price, targetCategory);
                  } else {
                     try {
                        Material material = Material.valueOf(materialName);
                        this.setWorthManager.setMaterialPrice(player, material, price, targetCategory);
                     } catch (IllegalArgumentException var18) {
                        Map<String, String> placeholders = new HashMap();
                        placeholders.put("material", args[0]);
                        player.sendMessage(this.langManager.getMessage("messages.material-not-exists", placeholders));
                     }
                  }

                  return true;
               }
            }
         }
      }

      return false;
   }

   private void sendHelp(CommandSender sender) {
      sender.sendMessage(this.langManager.getMessageWithoutPrefix("command-usage"));
      if (sender.hasPermission("sell.admin")) {
         sender.sendMessage(this.langManager.getMessageWithoutPrefix("command-usage-admin"));
      }

   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList();
      if (command.getName().equalsIgnoreCase("sell") && args.length == 1) {
         String input = args[0].toLowerCase();
         if ("help".startsWith(input)) {
            completions.add("help");
         }

         if ("reload".startsWith(input) && sender.hasPermission("sell.reload")) {
            completions.add("reload");
         }

         if ("reset".startsWith(input) && sender.hasPermission("sell.reset")) {
            completions.add("reset");
         }
      } else if (command.getName().equalsIgnoreCase("sell") && args.length == 2 && args[0].equalsIgnoreCase("reset")) {
         String input = args[1].toLowerCase();
         if (sender.hasPermission("sell.reset")) {
            for(Player player : Bukkit.getOnlinePlayers()) {
               if (player.getName().toLowerCase().startsWith(input)) {
                  completions.add(player.getName());
               }
            }

            for(String name : this.playerDataManager.getAllPlayerNames()) {
               if (name.toLowerCase().startsWith(input) && !completions.contains(name)) {
                  completions.add(name);
               }
            }
         }
      } else if (command.getName().equalsIgnoreCase("worth")) {
         if (args.length == 1) {
            String input = args[0].toLowerCase();
            if ("hand".startsWith(input)) {
               completions.add("hand");
            }

            if (this.worthItemManager != null) {
               for(Material material : Material.values()) {
                  if (material.name().toLowerCase().startsWith(input) && this.worthItemManager.getItemPrice(material) != null) {
                     completions.add(material.name().toLowerCase());
                  }
               }
            }
         }
      } else if (command.getName().equalsIgnoreCase("sellaxe")) {
         if (args.length == 1) {
            String input = args[0].toLowerCase();

            for(Player player : Bukkit.getOnlinePlayers()) {
               if (player.getName().toLowerCase().startsWith(input)) {
                  completions.add(player.getName());
               }
            }
         }
      } else {
         if (command.getName().equalsIgnoreCase("worthtoggle")) {
            return completions;
         }

         if (command.getName().equalsIgnoreCase("sellhistory")) {
            if (args.length == 1 && sender.hasPermission("sell.history.other")) {
               String input = args[0].toLowerCase();

               for(Player player : Bukkit.getOnlinePlayers()) {
                  if (player.getName().toLowerCase().startsWith(input)) {
                     completions.add(player.getName());
                  }
               }

               for(String name : this.playerDataManager.getAllPlayerNames()) {
                  if (name.toLowerCase().startsWith(input) && !completions.contains(name)) {
                     completions.add(name);
                  }
               }
            }
         } else if (command.getName().equalsIgnoreCase("setworth")) {
            if (args.length == 1) {
               String input = args[0].toLowerCase();
               if ("hand".startsWith(input)) {
                  completions.add("hand");
               }

               for(Material material : Material.values()) {
                  if (material.name().toLowerCase().startsWith(input)) {
                     completions.add(material.name().toLowerCase());
                  }
               }
            } else if (args.length == 3 && this.setWorthManager != null) {
               for(String category : this.setWorthManager.getCategoryFiles().keySet()) {
                  if (category.toLowerCase().startsWith(args[2].toLowerCase())) {
                     completions.add(category);
                  }
               }
            }
         }
      }

      return completions;
   }

   public boolean isShulkerBox(ItemStack item) {
      if (this.shulkerSupportEnabled && item != null && item.hasItemMeta()) {
         Material type = item.getType();
         return type == Material.SHULKER_BOX || type == Material.WHITE_SHULKER_BOX || type == Material.ORANGE_SHULKER_BOX || type == Material.MAGENTA_SHULKER_BOX || type == Material.LIGHT_BLUE_SHULKER_BOX || type == Material.YELLOW_SHULKER_BOX || type == Material.LIME_SHULKER_BOX || type == Material.PINK_SHULKER_BOX || type == Material.GRAY_SHULKER_BOX || type == Material.LIGHT_GRAY_SHULKER_BOX || type == Material.CYAN_SHULKER_BOX || type == Material.PURPLE_SHULKER_BOX || type == Material.BLUE_SHULKER_BOX || type == Material.BROWN_SHULKER_BOX || type == Material.GREEN_SHULKER_BOX || type == Material.RED_SHULKER_BOX || type == Material.BLACK_SHULKER_BOX;
      } else {
         return false;
      }
   }

   public double calculateShulkerBoxValue(ItemStack shulkerBox) {
      if (this.shulkerSupportEnabled && this.isShulkerBox(shulkerBox)) {
         double totalValue = (double)0.0F;

         try {
            BlockStateMeta meta = (BlockStateMeta)shulkerBox.getItemMeta();
            if (meta == null) {
               return totalValue;
            }

            BlockState state = meta.getBlockState();
            if (!(state instanceof ShulkerBox)) {
               return totalValue;
            }

            ShulkerBox shulker = (ShulkerBox)state;
            Inventory shulkerInventory = shulker.getInventory();

            for(ItemStack item : shulkerInventory.getContents()) {
               if (item != null && item.getType() != Material.AIR) {
                  Double price = this.worthItemManager.getItemPrice(item);
                  if (price != null && price > (double)0.0F) {
                     totalValue += price * (double)item.getAmount();
                  }
               }
            }
         } catch (Exception var13) {
         }

         return totalValue;
      } else {
         return (double)0.0F;
      }
   }

   public int countItemsInShulkerBox(ItemStack shulkerBox) {
      if (this.shulkerSupportEnabled && this.isShulkerBox(shulkerBox)) {
         int itemCount = 0;

         try {
            BlockStateMeta meta = (BlockStateMeta)shulkerBox.getItemMeta();
            if (meta == null) {
               return itemCount;
            }

            BlockState state = meta.getBlockState();
            if (!(state instanceof ShulkerBox)) {
               return itemCount;
            }

            ShulkerBox shulker = (ShulkerBox)state;
            Inventory shulkerInventory = shulker.getInventory();

            for(ItemStack item : shulkerInventory.getContents()) {
               if (item != null && item.getType() != Material.AIR) {
                  itemCount += item.getAmount();
               }
            }
         } catch (Exception var11) {
         }

         return itemCount;
      } else {
         return 0;
      }
   }

   public double sellShulkerBoxContents(Player player, ItemStack shulkerBox) {
      if (this.shulkerSupportEnabled && this.isShulkerBox(shulkerBox)) {
         double totalMoney = (double)0.0F;

         try {
            BlockStateMeta meta = (BlockStateMeta)shulkerBox.getItemMeta();
            if (meta == null) {
               return totalMoney;
            }

            BlockState state = meta.getBlockState();
            if (!(state instanceof ShulkerBox)) {
               return totalMoney;
            }

            ShulkerBox shulker = (ShulkerBox)state;
            Inventory shulkerInventory = shulker.getInventory();

            for(ItemStack item : shulkerInventory.getContents()) {
               if (item != null && item.getType() != Material.AIR) {
                  Double price = this.worthItemManager.getItemPrice(item);
                  if (price != null && price > (double)0.0F) {
                     totalMoney += price * (double)item.getAmount();
                     shulkerInventory.removeItem(new ItemStack[]{item});
                  }
               }
            }

            meta.setBlockState(shulker);
            shulkerBox.setItemMeta(meta);
            if (totalMoney > (double)0.0F) {
               this.economy.depositPlayer(player, totalMoney);
            }
         } catch (Exception var14) {
         }

         return totalMoney;
      } else {
         return (double)0.0F;
      }
   }

   public void runTask(Runnable task) {
      if (this.isFolia) {
         Bukkit.getGlobalRegionScheduler().execute(this, task);
      } else {
         Bukkit.getScheduler().runTask(this, task);
      }

   }

   public void runTaskLater(Runnable task, long delayTicks) {
      if (this.isFolia) {
         Bukkit.getGlobalRegionScheduler().runDelayed(this, (t) -> task.run(), delayTicks);
      } else {
         Bukkit.getScheduler().runTaskLater(this, task, delayTicks);
      }

   }

   public void runTaskTimer(Runnable task, long delayTicks, long periodTicks) {
      if (this.isFolia) {
         Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, (t) -> task.run(), delayTicks, periodTicks);
      } else {
         Bukkit.getScheduler().runTaskTimer(this, task, delayTicks, periodTicks);
      }

   }

   public void runAsync(Runnable task) {
      if (this.isFolia) {
         Bukkit.getAsyncScheduler().runNow(this, (t) -> task.run());
      } else {
         Bukkit.getScheduler().runTaskAsynchronously(this, task);
      }

   }

   public void runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
      Bukkit.getScheduler().runTaskTimerAsynchronously(this, task, delayTicks, periodTicks);
   }

   public void runTaskForPlayer(Player player, Runnable task) {
      if (this.isFolia) {
         player.getScheduler().run(this, (t) -> task.run(), (Runnable)null);
      } else {
         Bukkit.getScheduler().runTask(this, task);
      }

   }

   public CurrencyFormatter getCurrencyFormatter() {
      return this.currencyFormatter;
   }

   public SellManager getSellManager() {
      return this.sellManager;
   }

   public SellAxeManager getSellAxeManager() {
      return this.sellAxeManager;
   }

   public Economy getEconomy() {
      return this.economy;
   }

   public PlayerDataManager getPlayerDataManager() {
      return this.playerDataManager;
   }

   public boolean isPluginEnabled() {
      return this.isEnabled;
   }

   public boolean isEconomyEnabled() {
      return this.economyEnabled;
   }

   public WorthManager getWorthManager() {
      return this.worthManager;
   }

   public WorthItemManager getWorthItemManager() {
      return this.worthItemManager;
   }

   public SetWorthManager getSetWorthManager() {
      return this.setWorthManager;
   }

   public LevelManager getLevelManager() {
      return this.levelManager;
   }

   public boolean isShulkerSupportEnabled() {
      return this.shulkerSupportEnabled;
   }

   public boolean isFolia() {
      return this.isFolia;
   }
}
