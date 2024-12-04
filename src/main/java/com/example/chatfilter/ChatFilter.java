package com.example.chatfilter;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ChatFilter extends JavaPlugin implements Listener {
    private boolean enabled;
    private List<String> bannedWords;
    private double capsThreshold;
    private int minLength;
    private String prefix;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfiguration();
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void loadConfiguration() {
        reloadConfig();
        config = getConfig();
        enabled = config.getBoolean("settings.enabled-by-default", false);
        bannedWords = config.getStringList("banned-words");
        capsThreshold = config.getDouble("settings.caps.threshold", 0.7);
        minLength = config.getInt("settings.caps.min-length", 5);
        prefix = ChatColor.translateAlternateColorCodes('&', config.getString("settings.prefix", "&8[&cFilter&8]"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("chatfilter")) {
            return false;
        }

        if (!sender.hasPermission("loungefilter.media")) {
            String noPermMsg = config.getString("settings.messages.no-permission", "&cУ вас нет прав!");
            sender.sendMessage(formatMessage(noPermMsg));
            return true;
        }

        if (args.length != 1 || (!args[0].equalsIgnoreCase("on") && !args[0].equalsIgnoreCase("off"))) {
            String usageMsg = config.getString("settings.messages.usage", "&cИспользование: /chatfilter <on|off>");
            sender.sendMessage(formatMessage(usageMsg));
            return true;
        }

        enabled = args[0].equalsIgnoreCase("on");
        String messageKey = enabled ? "settings.messages.filter-enabled" : "settings.messages.filter-disabled";
        String defaultMsg = enabled ? "&aФильтр чата включен!" : "&eФильтр чата выключен!";
        String message = config.getString(messageKey, defaultMsg);
        
        sender.sendMessage(formatMessage(message));
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!enabled) {
            return;
        }

        String message = event.getMessage();
        
        // Проверка на запрещенные слова
        for (String word : bannedWords) {
            if (checkBannedWord(message, word)) {
                event.setCancelled(true);
                String warningMsg = config.getString("settings.messages.contains-banned-word", 
                    "&cПожалуйста, следите за своей речью!");
                event.getPlayer().sendMessage(formatMessage(warningMsg));
                return;
            }
        }

        // Проверка на CAPS
        if (config.getBoolean("settings.caps.enabled", true) && message.length() >= minLength) {
            int upperCount = 0;
            int letterCount = 0;
            
            for (char c : message.toCharArray()) {
                if (Character.isLetter(c)) {
                    letterCount++;
                    if (Character.isUpperCase(c)) {
                        upperCount++;
                    }
                }
            }
            
            if (letterCount > 0) {
                double capsPercentage = (double) upperCount / letterCount;
                if (capsPercentage > capsThreshold) {
                    event.setMessage(message.toLowerCase());
                }
            }
        }
    }

    private boolean checkBannedWord(String message, String word) {
        String msgLower = message.toLowerCase();
        String wordLower = word.toLowerCase();
        
        // Проверяем точное совпадение слова
        String[] words = msgLower.split("\\s+");
        for (String w : words) {
            if (w.equals(wordLower)) {
                return true;
            }
        }
        
        // Проверяем вхождение слова с границами
        return msgLower.matches(".*\\b" + Pattern.quote(wordLower) + "\\b.*");
    }

    private String formatMessage(String message) {
        if (message == null) {
            return "&cОшибка в конфигурации сообщений";
        }
        return ChatColor.translateAlternateColorCodes('&', 
            message.replace("%prefix%", config.getString("settings.prefix", "&8[&cFilter&8]")));
    }
}
