package de.elivb.donutSell.manager;

import de.elivb.donutSell.HexColorCode;
import de.elivb.donutSell.Sell;
import de.elivb.donutSell.utils.CurrencyFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;

public class WorthManager implements Listener {
   private final PriceManager priceManager;
   private final CurrencyFormatter currencyFormatter;
   private final SellManager sellManager;
   private final Sell plugin;
   private final boolean isFolia;
   private boolean enabled;
   private String loreFormat;
   private boolean shulkerSupportEnabled;
   private final ConcurrentHashMap<Material, Double> priceCache = new ConcurrentHashMap();
   private List<String> dontShowInGuis;
   private final ConcurrentHashMap<Player, Boolean> playerInBlockedGUI = new ConcurrentHashMap();
   private final ConcurrentHashMap<Player, Long> loreRemovalUntil = new ConcurrentHashMap();
   private final ConcurrentHashMap<Player, Long> lastUpdateTime = new ConcurrentHashMap();
   private final ConcurrentHashMap<Player, Boolean> playerWorthToggleStatus = new ConcurrentHashMap();
   private final ConcurrentHashMap<Player, Boolean> playerInventoryFullCache = new ConcurrentHashMap();
   private final ConcurrentHashMap<Player, Long> blockBreakCooldown = new ConcurrentHashMap();
   private final ConcurrentHashMap<Player, Boolean> playerIsBreakingBlock = new ConcurrentHashMap();
   private static final long LORE_REMOVAL_DURATION = 100L;
   private static final long UPDATE_INTERVAL = 100L;
   private static final long BLOCK_BREAK_COOLDOWN_TICKS = 50L;
   private static final long UPDATE_TIMER_TICKS = 20L;
   private String worthPrefix;
   private String worthSuffix;
   private String unitLoreFormat;
   private String unitWorthPrefix;
   private String unitWorthSuffix;
   private boolean debug = true;

   public WorthManager(PriceManager priceManager, CurrencyFormatter currencyFormatter, SellManager sellManager, boolean isFolia) {
      this.priceManager = priceManager;
      this.currencyFormatter = currencyFormatter;
      this.sellManager = sellManager;
      this.plugin = sellManager.getPlugin();
      this.isFolia = isFolia;
      this.loadConfig();
      this.warmupCache();
      this.startUpdateTimer();
      if (this.debug) {
      }

   }

   private void loadConfig() {
      this.enabled = this.plugin.getConfig().getBoolean("worth-lore.enabled", true);
      this.loreFormat = this.plugin.getConfig().getString("worth-lore.lore-format", "&7Tổng: &#0bf52b%price%");
      this.unitLoreFormat = this.plugin.getConfig().getString("worth-lore.unit-lore-format", "&7Giá mỗi item: &#0bf52b%price%");
      this.shulkerSupportEnabled = this.plugin.getConfig().getBoolean("shulker-support.enabled", true);
      this.dontShowInGuis = this.plugin.getConfig().getStringList("don-t-show-gui");
      if (this.dontShowInGuis == null) {
         this.dontShowInGuis = new ArrayList();
      }

      this.updateWorthPrefixSuffix();
      String[] unitPrefixSuffix = this.computePrefixSuffix(this.unitLoreFormat);
      if (unitPrefixSuffix != null) {
         this.unitWorthPrefix = unitPrefixSuffix[0];
         this.unitWorthSuffix = unitPrefixSuffix[1];
      } else {
         this.unitWorthPrefix = null;
         this.unitWorthSuffix = null;
      }
      if (this.debug) {
      }

   }

   /**
    * Tách 1 format lore (ví dụ "&7Tổng: &#0bf52b%price%") thành [prefix, suffix]
    * (sau khi bỏ màu), dùng chung cho cả dòng "Tổng" và "Giá mỗi item".
    */
   private String[] computePrefixSuffix(String format) {
      if (format == null || format.isEmpty()) {
         return null;
      }

      String withColors = HexColorCode.translateAllColorCodes(format);
      String plainFormat = ChatColor.stripColor(withColors);
      if (plainFormat == null || plainFormat.isEmpty()) {
         return null;
      }

      int priceIndex = plainFormat.indexOf("%price%");
      if (priceIndex >= 0) {
         return new String[]{plainFormat.substring(0, priceIndex), plainFormat.substring(priceIndex + 7)};
      } else {
         return new String[]{plainFormat, ""};
      }
   }

   private void updateWorthPrefixSuffix() {
      String[] prefixSuffix = this.computePrefixSuffix(this.loreFormat);
      if (prefixSuffix != null) {
         this.worthPrefix = prefixSuffix[0];
         this.worthSuffix = prefixSuffix[1];
      } else {
         this.worthPrefix = null;
         this.worthSuffix = null;
      }

      if (this.debug) {
      }

   }

   private boolean isWorthLoreLine(String line) {
      return this.matchesPriceLoreFormat(line, this.worthPrefix, this.worthSuffix);
   }

   private boolean isUnitWorthLoreLine(String line) {
      return this.matchesPriceLoreFormat(line, this.unitWorthPrefix, this.unitWorthSuffix);
   }

   /**
    * Kiểm tra 1 dòng lore có khớp format "prefix + giá + suffix" không
    * (dùng chung cho cả dòng "Tổng" và "Giá mỗi item").
    */
   private boolean matchesPriceLoreFormat(String line, String prefix, String suffix) {
      if (prefix == null || suffix == null) {
         return false;
      } else if (line == null) {
         return false;
      } else {
         String strippedLine = ChatColor.stripColor(HexColorCode.translateAllColorCodes(line));
         if (!strippedLine.startsWith(prefix)) {
            return false;
         } else if (!strippedLine.endsWith(suffix)) {
            return false;
         } else {
            int start = prefix.length();
            int end = strippedLine.length() - suffix.length();
            if (end < start) {
               return false;
            }

            String pricePart = strippedLine.substring(start, end);
            String cleanPrice = pricePart.replaceAll("^[\\$€¥£]|[\\$€¥£]$", "");
            boolean isValid = false;
            if (cleanPrice.matches("\\d+(?:[\\.,]\\d+)?[KMB]")) {
               isValid = true;
            } else if (cleanPrice.matches("\\d+(?:[\\.,]\\d+)?")) {
               isValid = true;
            }

            return isValid;
         }
      }
   }

   private void warmupCache() {
      this.plugin.runAsync(() -> {
         for(Material material : Material.values()) {
            Double price = this.priceManager.getPrice(material);
            if (price != null) {
               this.priceCache.put(material, price);
            }
         }

         if (this.debug) {
         }

      });
   }

   private void startUpdateTimer() {
      long initialDelay = this.isFolia ? 10L : 0L;
      this.plugin.runTaskTimer(() -> {
         if (this.enabled) {
            long currentTime = System.currentTimeMillis();

            for(Player player : Bukkit.getOnlinePlayers()) {
               if (!this.isInBlockBreakCooldown(player)) {
                  Long lastUpdate = (Long)this.lastUpdateTime.get(player);
                  if (lastUpdate == null || currentTime - lastUpdate >= 100L) {
                     if (this.debug) {
                     }

                     this.updatePlayerInventory(player);
                     this.lastUpdateTime.put(player, currentTime);
                  }
               }
            }

         }
      }, initialDelay, UPDATE_TIMER_TICKS);
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      if (this.enabled) {
         Player player = event.getPlayer();
         this.loadPlayerToggleStatus(player);
         if (this.debug) {
         }

         this.plugin.runTaskLater(() -> {
            if (player.isOnline()) {
               this.updatePlayerInventory(player);
            }

         }, 5L);
      }
   }

   @EventHandler
   public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
      if (this.enabled) {
         Player player = event.getPlayer();
         if (this.debug) {
         }

         this.plugin.runTaskLater(() -> {
            if (player.isOnline()) {
               this.removeAllWorthLoresFromInventory(player);
               this.updatePlayerInventory(player);
            }

         }, 1L);
      }
   }

   @EventHandler
   public void onEntityPickupItem(EntityPickupItemEvent event) {
      if (this.enabled && event.getEntity() instanceof Player) {
         Player player = (Player)event.getEntity();
         ItemStack item = event.getItem().getItemStack();
         if (this.debug) {
         }

         this.removeAllPriceLoreFast(item);
         this.activateLoreRemoval(player);
      }
   }

   @EventHandler
   public void onBlockBreak(BlockBreakEvent event) {
      if (this.enabled) {
         Player player = event.getPlayer();
         if (this.debug) {
         }

         this.playerIsBreakingBlock.put(player, true);
         this.removeAllWorthLoresFromInventory(player);
         this.setBlockBreakCooldown(player);
         this.plugin.runTaskLater(() -> this.playerIsBreakingBlock.remove(player), 5L);
      }
   }

   private void setBlockBreakCooldown(Player player) {
      this.blockBreakCooldown.put(player, System.currentTimeMillis());
      if (this.debug) {
      }

      this.plugin.runTaskLater(() -> {
         this.blockBreakCooldown.remove(player);
         if (this.debug) {
         }

         if (player.isOnline()) {
            this.updatePlayerInventory(player);
         }

      }, 50L);
   }

   private boolean isInBlockBreakCooldown(Player player) {
      boolean inCooldown = this.blockBreakCooldown.containsKey(player);
      if (this.debug && inCooldown) {
      }

      return inCooldown;
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (this.enabled && event.getWhoClicked() instanceof Player) {
         Player player = (Player)event.getWhoClicked();
         if (this.debug) {
         }

         ItemStack clickedItem = event.getCurrentItem();
         if (clickedItem != null && clickedItem.getType() != Material.AIR) {
            this.removeAllPriceLoreFast(clickedItem);
         }

         ItemStack cursorItem = event.getCursor();
         if (cursorItem != null && cursorItem.getType() != Material.AIR) {
            this.removeAllPriceLoreFast(cursorItem);
         }

         this.activateLoreRemoval(player);
      }
   }

   @EventHandler
   public void onInventoryDrag(InventoryDragEvent event) {
      if (this.enabled && event.getWhoClicked() instanceof Player) {
         Player player = (Player)event.getWhoClicked();
         if (this.debug) {
         }

         for(ItemStack item : event.getNewItems().values()) {
            if (item != null && item.getType() != Material.AIR) {
               this.removeAllPriceLoreFast(item);
            }
         }

         this.activateLoreRemoval(player);
      }
   }

   @EventHandler
   public void onInventoryOpen(InventoryOpenEvent event) {
      if (this.enabled && event.getPlayer() instanceof Player) {
         Player player = (Player)event.getPlayer();
         String title = event.getView().getTitle();
         boolean isBlocked = false;

         for(String blockedTitle : this.dontShowInGuis) {
            String translatedBlocked = HexColorCode.translateAllColorCodes(blockedTitle);
            if (title.contains(translatedBlocked)) {
               isBlocked = true;
               break;
            }
         }

         this.playerInBlockedGUI.put(player, isBlocked);
         if (this.debug) {
         }

         InventoryType type = event.getInventory().getType();
         if (this.isContainer(type)) {
            for(ItemStack item : event.getInventory().getContents()) {
               if (item != null && item.getType() != Material.AIR) {
                  this.removeAllPriceLoreFast(item);
               }
            }
         }

         this.updatePlayerInventory(player);
      }
   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      if (this.enabled && event.getPlayer() instanceof Player) {
         Player player = (Player)event.getPlayer();
         if (this.debug) {
         }

         this.playerInBlockedGUI.remove(player);
         this.updatePlayerInventory(player);
      }
   }

   private void loadPlayerToggleStatus(Player player) {
      boolean toggleStatus = this.plugin.getPlayerDataManager().getPlayerConfig(player).getBoolean("worth-toggle", true);
      this.playerWorthToggleStatus.put(player, toggleStatus);
   }

   private void savePlayerToggleStatus(Player player, boolean status) {
      this.plugin.getPlayerDataManager().getPlayerConfig(player).set("worth-toggle", status);
      this.plugin.getPlayerDataManager().savePlayerConfig(player);
      this.playerWorthToggleStatus.put(player, status);
   }

   public void togglePlayerWorthLore(Player player) {
      boolean currentStatus = (Boolean)this.playerWorthToggleStatus.getOrDefault(player, true);
      boolean newStatus = !currentStatus;
      this.removeAllWorthLoresFromInventory(player);
      this.savePlayerToggleStatus(player, newStatus);
      if (this.debug) {
      }

      String statusMessage = newStatus ? this.sellManager.getLangManager().getMessage("messages.worth-toggle-enabled") : this.sellManager.getLangManager().getMessage("messages.worth-toggle-disabled");
      player.sendMessage(statusMessage);
      String actionBarMessage = newStatus ? this.sellManager.getLangManager().getMessage("action-bars.worth-toggle-on") : this.sellManager.getLangManager().getMessage("action-bars.worth-toggle-off");
      player.sendActionBar(actionBarMessage);
      if (newStatus) {
         player.playSound(player.getLocation(), this.sellManager.getSellSuccessSound(), 1.0F, 1.0F);
      } else {
         player.playSound(player.getLocation(), this.sellManager.getNoSellableItemsSound(), 1.0F, 1.0F);
      }

   }

   private void removeAllWorthLoresFromInventory(Player player) {
      if (player != null) {
         if (this.debug) {
         }

         for(ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
               this.removeAllPriceLoreFast(item);
            }
         }

         ItemStack offHand = player.getInventory().getItemInOffHand();
         if (offHand != null && offHand.getType() != Material.AIR) {
            this.removeAllPriceLoreFast(offHand);
         }

      }
   }

   public boolean getPlayerWorthToggleStatus(Player player) {
      return (Boolean)this.playerWorthToggleStatus.getOrDefault(player, true);
   }

   public void setPlayerWorthToggleStatus(Player player, boolean status) {
      this.savePlayerToggleStatus(player, status);
      this.updatePlayerInventory(player);
   }

   private void activateLoreRemoval(Player player) {
      if (this.isBedrockPlayer(player)) {
         // Bedrock: chỉ hiện "Giá mỗi item" (đơn giá, không nhân số lượng) -> không
         // bao giờ đổi theo amount -> không ảnh hưởng isSimilar(), không cần ẩn/hiện.
         // Chỉ cần đảm bảo item mới/di chuyển có lore ngay.
         this.updatePlayerInventory(player);
         return;
      }

      // Java: dòng "Tổng" (đơn giá × số lượng) vẫn phụ thuộc amount -> vẫn cần ẩn
      // 0.1s để Bukkit xử lý merge/gather/drop đúng (tránh MẤT ĐỒ / DUPE) như cũ.
      this.loreRemovalUntil.put(player, System.currentTimeMillis() + LORE_REMOVAL_DURATION);

      if (this.debug) {
      }

      this.updatePlayerInventory(player);

      // Thêm lại lore đúng ~100ms (2 tick) sau, ngay khi cửa sổ ẩn kết thúc — không
      // chờ timer chu kỳ ~1s (lệch pha gây delay 1-2s không nhất quán).
      this.plugin.runTaskLater(() -> {
         if (player.isOnline() && !this.isLoreRemovalActive(player)) {
            this.updatePlayerInventory(player);
         }
      }, 2L);
   }

   /**
    * Kiểm tra người chơi có đang vào server qua Floodgate (Bedrock) hay không.
    * Dùng reflection để không cần thêm dependency Floodgate vào pom.xml —
    * nếu Floodgate không được cài, luôn trả về false (coi như người chơi Java).
    */
   private boolean isBedrockPlayer(Player player) {
      try {
         Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
         Object api = apiClass.getMethod("getInstance").invoke((Object)null);
         Object result = apiClass.getMethod("isFloodgatePlayer", java.util.UUID.class).invoke(api, player.getUniqueId());
         return Boolean.TRUE.equals(result);
      } catch (Throwable ignored) {
         return false;
      }
   }

   private boolean isInventoryNearlyFull(Player player) {
      if (player == null) {
         return false;
      } else {
         Inventory inv = player.getInventory();
         int freeSlots = 0;

         for(int i = 0; i <= 35; ++i) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) {
               ++freeSlots;
            }
         }

         ItemStack offHand = inv.getItem(40);
         if (offHand == null || offHand.getType() == Material.AIR) {
            ++freeSlots;
         }

         return freeSlots <= 1;
      }
   }

   private void updateInventoryFullStatus(Player player) {
      boolean isNearlyFull = this.isInventoryNearlyFull(player);
      Boolean cachedStatus = (Boolean)this.playerInventoryFullCache.get(player);
      if (cachedStatus == null || cachedStatus != isNearlyFull) {
         if (this.debug) {
         }

         this.playerInventoryFullCache.put(player, isNearlyFull);
      }

   }

   private boolean shouldDisableDueToFullInventory(Player player) {
      Boolean isNearlyFull = (Boolean)this.playerInventoryFullCache.get(player);
      if (isNearlyFull == null) {
         isNearlyFull = this.isInventoryNearlyFull(player);
         this.playerInventoryFullCache.put(player, isNearlyFull);
      }

      return isNearlyFull;
   }

   private void updatePlayerInventory(Player player) {
      if (player.isOnline()) {
         if (!this.isInBlockBreakCooldown(player)) {
            if (!this.playerIsBreakingBlock.containsKey(player)) {
               try {
                  this.updateInventoryFullStatus(player);
                  boolean showWorth = !(Boolean)this.playerInBlockedGUI.getOrDefault(player, false);
                  boolean loreRemovalActive = this.isLoreRemovalActive(player);
                  boolean worthToggleEnabled = this.getPlayerWorthToggleStatus(player);
                  boolean inventoryFull = this.shouldDisableDueToFullInventory(player);
                  boolean isCreative = player.getGameMode() == GameMode.CREATIVE;
                  boolean shouldAddLore = showWorth && !loreRemovalActive && worthToggleEnabled && !inventoryFull && !isCreative;
                  boolean isBedrock = this.isBedrockPlayer(player);
                  if (this.debug && shouldAddLore) {
                  }

                  ItemStack[] contents = player.getInventory().getContents();

                  for(ItemStack item : contents) {
                     if (item != null && item.getType() != Material.AIR) {
                        this.updateItem(item, shouldAddLore, isBedrock);
                     }
                  }

                  ItemStack offHand = player.getInventory().getItemInOffHand();
                  if (offHand != null && offHand.getType() != Material.AIR) {
                     this.updateItem(offHand, shouldAddLore, isBedrock);
                  }

                  ItemStack cursor = player.getItemOnCursor();
                  if (cursor != null && cursor.getType() != Material.AIR) {
                     this.updateItem(cursor, shouldAddLore, isBedrock);
                  }
               } catch (Exception var13) {
                  if (this.debug) {
                  }
               }

            }
         }
      }
   }

   /**
    * Cập nhật lore "Tổng" + "Giá mỗi item" cho 1 item.
    *
    * - "Giá mỗi item" (đơn giá, KHÔNG nhân số lượng): hiện cho CẢ Java và Bedrock,
    *   KHÔNG toggle theo loreRemovalUntil — vì giá trị này không đổi theo amount
    *   nên không ảnh hưởng ItemStack.isSimilar() (an toàn cho merge/gather/drop),
    *   và không cần Bedrock "load lại" liên tục -> không còn flicker.
    * - "Tổng" (đơn giá × số lượng): CHỈ Java, CHỈ khi amount > 1 (amount=1 thì
    *   trùng "Giá mỗi item", khỏi lặp). Vẫn toggle theo addLore (loreRemovalUntil)
    *   như cũ để bảo vệ merge/gather/drop trên Java.
    */
   private void updateItem(ItemStack item, boolean addLore, boolean isBedrock) {
      if (item != null && item.getType() != Material.AIR) {
         try {
            if (this.shulkerSupportEnabled && this.isShulkerBox(item)) {
               this.removeWorthLoreFast(item);
               this.removeUnitWorthLoreFast(item);
               if (addLore) {
                  this.updateShulkerLoreFast(item);
               }

               return;
            }

            Double unitValue = this.calculateUnitItemValue(item);
            boolean hasUnitLore = this.hasUnitWorthLore(item);
            boolean shouldHaveUnitLore = addLore && unitValue != null;
            if (hasUnitLore != shouldHaveUnitLore) {
               this.removeUnitWorthLoreFast(item);
               if (shouldHaveUnitLore) {
                  if (this.debug) {
                  }

                  this.addUnitWorthLoreFast(item, unitValue);
               }
            }

            boolean hasTotalLore = this.hasWorthLore(item);
            boolean shouldHaveTotalLore = !isBedrock && addLore && item.getAmount() > 1 && this.calculateTotalItemValue(item) != null;
            if (hasTotalLore != shouldHaveTotalLore) {
               this.removeWorthLoreFast(item);
               if (shouldHaveTotalLore) {
                  Double totalPrice = this.calculateTotalItemValue(item);
                  if (totalPrice != null && totalPrice > (double)0.0F) {
                     if (this.debug) {
                     }

                     this.addWorthLoreFast(item, totalPrice);
                  }
               }
            }
         } catch (Exception exception) {
            if (this.debug) {
               exception.printStackTrace();
            }
         }

      }
   }

   private boolean hasWorthLore(ItemStack item) {
      return this.hasLoreLineMatching(item, this::isWorthLoreLine);
   }

   private boolean hasUnitWorthLore(ItemStack item) {
      return this.hasLoreLineMatching(item, this::isUnitWorthLoreLine);
   }

   private boolean hasLoreLineMatching(ItemStack item, java.util.function.Predicate<String> matcher) {
      if (!item.hasItemMeta()) {
         return false;
      } else {
         ItemMeta meta = item.getItemMeta();
         if (meta != null && meta.hasLore()) {
            for(String line : meta.getLore()) {
               if (matcher.test(line)) {
                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      }
   }

   private boolean isLoreRemovalActive(Player player) {
      Long removalEnd = (Long)this.loreRemovalUntil.get(player);
      if (removalEnd == null) {
         return false;
      } else if (System.currentTimeMillis() > removalEnd) {
         this.loreRemovalUntil.remove(player);
         return false;
      } else {
         return true;
      }
   }

   private boolean isContainer(InventoryType type) {
      return type == InventoryType.CHEST || type == InventoryType.ENDER_CHEST || type == InventoryType.BARREL || type == InventoryType.SHULKER_BOX || type == InventoryType.HOPPER || type == InventoryType.DROPPER || type == InventoryType.DISPENSER;
   }

   private void removeWorthLoreFast(ItemStack item) {
      this.removeLoreLineFast(item, this::isWorthLoreLine);
   }

   private void removeUnitWorthLoreFast(ItemStack item) {
      this.removeLoreLineFast(item, this::isUnitWorthLoreLine);
   }

   /**
    * Xoá CẢ 2 dòng "Tổng" và "Giá mỗi item" — dùng trong các event handler để
    * đảm bảo "sạch" trước khi Bukkit xử lý click/drag/drop, hoặc khi item rời
    * khỏi inventory người chơi (vào container/đổi gamemode/tắt hiển thị).
    */
   private void removeAllPriceLoreFast(ItemStack item) {
      this.removeWorthLoreFast(item);
      this.removeUnitWorthLoreFast(item);
   }

   private void removeLoreLineFast(ItemStack item, java.util.function.Predicate<String> matcher) {
      if (item != null && item.getType() != Material.AIR && item.hasItemMeta()) {
         try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasLore()) {
               return;
            }

            List<String> lore = meta.getLore();
            if (lore == null || lore.isEmpty()) {
               return;
            }

            List<String> newLore = new ArrayList();
            boolean loreRemoved = false;

            for(String line : lore) {
               if (!matcher.test(line)) {
                  newLore.add(line);
               } else {
                  loreRemoved = true;
                  if (this.debug) {
                  }
               }
            }

            if (loreRemoved) {
               if (newLore.isEmpty()) {
                  meta.setLore((List)null);
               } else {
                  meta.setLore(newLore);
               }

               item.setItemMeta(meta);
            }
         } catch (Exception var8) {
         }

      }
   }

   private void addWorthLoreFast(ItemStack item, double totalPrice) {
      this.addLoreLineFast(item, this.formatWorthLine(totalPrice), this::isWorthLoreLine);
   }

   private void addUnitWorthLoreFast(ItemStack item, double unitPrice) {
      this.addLoreLineFast(item, this.formatUnitWorthLine(unitPrice), this::isUnitWorthLoreLine);
   }

   private void addLoreLineFast(ItemStack item, String formattedLine, java.util.function.Predicate<String> matcher) {
      try {
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return;
         }

         List<String> lore = meta.getLore();
         if (lore == null) {
            lore = new ArrayList(1);
         }

         boolean alreadyExists = false;

         for(String line : lore) {
            if (matcher.test(line)) {
               alreadyExists = true;
               break;
            }
         }

         if (!alreadyExists) {
            if (this.debug) {
            }

            lore.add(formattedLine);
            meta.setLore(lore);
            item.setItemMeta(meta);
         }
      } catch (Exception var10) {
      }

   }

   private void updateShulkerLoreFast(ItemStack shulkerBox) {
      if (this.shulkerSupportEnabled && this.isShulkerBox(shulkerBox)) {
         try {
            BlockStateMeta meta = (BlockStateMeta)shulkerBox.getItemMeta();
            if (meta == null) {
               return;
            }

            BlockState state = meta.getBlockState();
            if (!(state instanceof ShulkerBox)) {
               return;
            }

            ShulkerBox shulker = (ShulkerBox)state;
            Inventory shulkerInventory = shulker.getInventory();
            if (shulkerInventory == null) {
               return;
            }

            double totalValue = (double)0.0F;
            int itemCount = 0;
            Double shulkerBoxValue = this.getSingleItemPrice(shulkerBox);
            if (shulkerBoxValue != null && shulkerBoxValue > (double)0.0F) {
               totalValue += shulkerBoxValue;
            }

            boolean hasContent = false;

            for(ItemStack item : shulkerInventory.getContents()) {
               if (item != null && item.getType() != Material.AIR) {
                  hasContent = true;
                  Double price = this.getSingleItemPrice(item);
                  if (price != null && price > (double)0.0F) {
                     totalValue += price * (double)item.getAmount();
                     itemCount += item.getAmount();
                  }
               }
            }

            List<String> lore = meta.getLore();
            if (lore == null) {
               lore = new ArrayList();
            } else {
               List<String> newLore = new ArrayList();

               for(String line : lore) {
                  if (!this.isWorthLoreLine(line)) {
                     newLore.add(line);
                  }
               }

               lore = newLore;
            }

            if (totalValue > (double)0.0F) {
               String worthLine = this.formatShulkerWorthLine(totalValue, itemCount);
               lore.add(worthLine);
            }

            meta.setLore(lore.isEmpty() ? null : lore);
            shulkerBox.setItemMeta(meta);
         } catch (Exception exception) {
            if (this.debug) {
               exception.printStackTrace();
            }
         }

      }
   }

   private Double calculateTotalItemValue(ItemStack item) {
      if (item != null && item.getType() != Material.AIR) {
         Double singlePrice = this.getSingleItemPrice(item);
         return singlePrice != null && !(singlePrice <= (double)0.0F) ? singlePrice * (double)item.getAmount() : null;
      } else {
         return null;
      }
   }

   /**
    * "Giá mỗi item" — đơn giá, KHÔNG nhân số lượng. Giá trị này không đổi theo
    * amount, nên không cần ẩn/hiện khi click/drag/drop (không ảnh hưởng isSimilar()).
    */
   private Double calculateUnitItemValue(ItemStack item) {
      if (item != null && item.getType() != Material.AIR) {
         Double singlePrice = this.getSingleItemPrice(item);
         return singlePrice != null && !(singlePrice <= (double)0.0F) ? singlePrice : null;
      } else {
         return null;
      }
   }

   private Double getSingleItemPrice(ItemStack item) {
      return item != null && item.getType() != Material.AIR ? this.priceManager.getPriceWithDurability(item) : null;
   }

   private String formatShulkerWorthLine(double totalValue, int itemCount) {
      String formattedValue = this.currencyFormatter.format(totalValue);
      String line = this.loreFormat.replace("%price%", formattedValue);
      return HexColorCode.translateAllColorCodes(line);
   }

   private String formatWorthLine(double totalPrice) {
      String formattedPrice = this.currencyFormatter.format(totalPrice);
      String line = this.loreFormat.replace("%price%", formattedPrice);
      return HexColorCode.translateAllColorCodes(line);
   }

   private String formatUnitWorthLine(double unitPrice) {
      String formattedPrice = this.currencyFormatter.format(unitPrice);
      String line = this.unitLoreFormat.replace("%price%", formattedPrice);
      return HexColorCode.translateAllColorCodes(line);
   }

   private boolean isShulkerBox(ItemStack item) {
      if (item == null) {
         return false;
      } else {
         Material type = item.getType();
         return type == Material.SHULKER_BOX || type == Material.WHITE_SHULKER_BOX || type == Material.ORANGE_SHULKER_BOX || type == Material.MAGENTA_SHULKER_BOX || type == Material.LIGHT_BLUE_SHULKER_BOX || type == Material.YELLOW_SHULKER_BOX || type == Material.LIME_SHULKER_BOX || type == Material.PINK_SHULKER_BOX || type == Material.GRAY_SHULKER_BOX || type == Material.LIGHT_GRAY_SHULKER_BOX || type == Material.CYAN_SHULKER_BOX || type == Material.PURPLE_SHULKER_BOX || type == Material.BLUE_SHULKER_BOX || type == Material.BROWN_SHULKER_BOX || type == Material.GREEN_SHULKER_BOX || type == Material.RED_SHULKER_BOX || type == Material.BLACK_SHULKER_BOX;
      }
   }

   private boolean isPotion(ItemStack item) {
      return item != null && (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION) && item.hasItemMeta() && item.getItemMeta() instanceof PotionMeta;
   }

   public ItemStack processItemInstant(ItemStack item) {
      if (this.enabled && item != null && item.getType() != Material.AIR) {
         this.updateItem(item, true, false);
      }

      return item;
   }

   public List<ItemStack> processItemsInstant(List<ItemStack> items) {
      if (!this.enabled) {
         return items;
      } else {
         for(ItemStack item : items) {
            this.processItemInstant(item);
         }

         return items;
      }
   }

   public void forceUpdatePlayer(Player player) {
      this.updatePlayerInventory(player);
   }

   public void onInventoryChanged(Player player) {
      this.activateLoreRemoval(player);
   }

   public void reloadConfig() {
      this.loadConfig();
      this.priceCache.clear();
      this.warmupCache();
      this.playerInBlockedGUI.clear();
      this.loreRemovalUntil.clear();
      this.playerInventoryFullCache.clear();
      this.blockBreakCooldown.clear();
      if (this.debug) {
      }

   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public String getLoreFormat() {
      return this.loreFormat;
   }

   public boolean isShulkerSupportEnabled() {
      return this.shulkerSupportEnabled;
   }
}
