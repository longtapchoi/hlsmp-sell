package de.elivb.donutSell.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.configuration.file.FileConfiguration;

public class CurrencyFormatter {
   private final DecimalFormat currencyFormat;
   private final boolean abbreviationsEnabled;
   private final String currencySymbol;
   private static final Pattern ABBREVIATION_PATTERN = Pattern.compile("([0-9.]+)([kmbt])", 2);

   public CurrencyFormatter(FileConfiguration config) {
      String format = config.getString("economy.currency-format", "#,##0.##");
      this.currencyFormat = new DecimalFormat(format, new DecimalFormatSymbols(Locale.GERMAN));
      this.abbreviationsEnabled = config.getBoolean("economy.abbreviations.enabled", true);
      this.currencySymbol = "$";
   }

   public String format(double amount) {
      if (!this.abbreviationsEnabled) {
         String var10000 = this.currencySymbol;
         return var10000 + this.currencyFormat.format(amount);
      } else {
         return this.formatWithAbbreviations(amount);
      }
   }

   private String formatWithAbbreviations(double amount) {
      if (this.abbreviationsEnabled && !(amount < (double)1000.0F)) {
         String[] suffixes = new String[]{"", "K", "M", "B", "T"};
         int suffixIndex = 0;

         double formattedAmount;
         for(formattedAmount = amount; formattedAmount >= (double)1000.0F && suffixIndex < suffixes.length - 1; ++suffixIndex) {
            formattedAmount /= (double)1000.0F;
         }

         DecimalFormat abbrevFormat = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.GERMAN));
         String var10000 = this.currencySymbol;
         return var10000 + abbrevFormat.format(formattedAmount) + suffixes[suffixIndex];
      } else {
         String var10000 = this.currencySymbol;
         return var10000 + this.currencyFormat.format(amount);
      }
   }

   public double parseAbbreviatedAmount(String input) {
      if (this.abbreviationsEnabled && input != null && !input.isEmpty()) {
         try {
            String cleaned = input.replace(this.currencySymbol, "").trim();
            Matcher matcher = ABBREVIATION_PATTERN.matcher(cleaned);
            if (matcher.matches()) {
               double number = Double.parseDouble(matcher.group(1));
               String suffix = matcher.group(2).toLowerCase();
               byte var8 = -1;
               switch (suffix.hashCode()) {
                  case 98:
                     if (suffix.equals("B")) {
                        var8 = 2;
                     }
                     break;
                  case 107:
                     if (suffix.equals("K")) {
                        var8 = 0;
                     }
                     break;
                  case 109:
                     if (suffix.equals("M")) {
                        var8 = 1;
                     }
                     break;
                  case 116:
                     if (suffix.equals("T")) {
                        var8 = 3;
                     }
               }

               switch (var8) {
                  case 0 -> {
                     return number * (double)1000.0F;
                  }
                  case 1 -> {
                     return number * (double)1000000.0F;
                  }
                  case 2 -> {
                     return number * (double)1.0E9F;
                  }
                  case 3 -> {
                     return number * 1.0E12;
                  }
                  default -> {
                     return number;
                  }
               }
            } else {
               return Double.parseDouble(cleaned);
            }
         } catch (NumberFormatException var81) {
            return (double)0.0F;
         }
      } else {
         return (double)0.0F;
      }
   }

   public boolean isAbbreviationsEnabled() {
      return this.abbreviationsEnabled;
   }

   public String getCurrencySymbol() {
      return this.currencySymbol;
   }
}
