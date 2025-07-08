package cn.i7mc.commands;

import cn.i7mc.PlayerDeadManager;
import cn.i7mc.guis.TeleportGUI;
import cn.i7mc.managers.ConfigManager;
import cn.i7mc.managers.DataManager;
import cn.i7mc.managers.MessageManager;
import cn.i7mc.managers.TombstoneManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * PDM主命令处理器 - 统一处理所有插件命令
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public class PDMCommand implements CommandExecutor, TabCompleter {
    
    private final PlayerDeadManager plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final TombstoneManager tombstoneManager;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     */
    public PDMCommand(@NotNull PlayerDeadManager plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
        this.tombstoneManager = plugin.getTombstoneManager();
    }
    
    /**
     * 执行命令
     * 统一的命令执行方法
     * 
     * @param sender 命令发送者
     * @param command 命令
     * @param label 命令标签
     * @param args 命令参数
     * @return 是否成功处理命令
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                           @NotNull String label, @NotNull String[] args) {
        

        // 检查参数数量
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        // 根据子命令分发处理
        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        
        return switch (subCommand) {
            case "help", "?" -> {
                handleHelpCommand(sender, subArgs);
                yield true;
            }
            case "reload" -> {
                handleReloadCommand(sender, subArgs);
                yield true;
            }
            case "list", "ls" -> {
                handleListCommand(sender, subArgs);
                yield true;
            }
            case "gui", "menu" -> {
                handleGUICommand(sender, subArgs);
                yield true;
            }
            case "teleport", "tp" -> {
                handleTeleportCommand(sender, subArgs);
                yield true;
            }
            case "info", "version" -> {
                handleInfoCommand(sender, subArgs);
                yield true;
            }
            case "cleanup", "clean" -> {
                handleCleanupCommand(sender, subArgs);
                yield true;
            }
            default -> {
                sendUnknownCommandMessage(sender, subCommand);
                yield true;
            }
        };
    }
    
    /**
     * 处理帮助命令
     * 统一的帮助命令处理方法
     * 
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleHelpCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        sendHelpMessage(sender);
    }
    
    /**
     * 处理重载命令
     * 统一的重载命令处理方法
     * 
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleReloadCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        // 检查权限
        if (!sender.hasPermission("playerdeadmanager.admin.reload")) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            if (sender instanceof Player player) {
                messageManager.addPlayerPlaceholders(placeholders, player);
            }
            messageManager.sendMessage(sender, "permission.no-reload", placeholders);
            return;
        }
        
        // 执行重载
        plugin.reloadPlugin();
        
        // 发送成功消息
        Map<String, String> placeholders = messageManager.createPlaceholders();
        if (sender instanceof Player player) {
            messageManager.addPlayerPlaceholders(placeholders, player);
        }
        messageManager.sendMessage(sender, "commands.reload.success", placeholders);
    }
    
    /**
     * 处理列表命令
     * 统一的列表命令处理方法
     * 
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleListCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        // 只允许玩家使用
        if (!(sender instanceof Player player)) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.sendMessage(sender, "commands.player-only", placeholders);
            return;
        }
        
        // 检查权限
        if (!player.hasPermission("playerdeadmanager.list")) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            messageManager.sendMessage(player, "permission.no-list", placeholders);
            return;
        }
        
        // 获取玩家的墓碑列表
        List<DataManager.TombstoneData> tombstones = tombstoneManager.getPlayerTombstones(player.getUniqueId());
        
        if (tombstones.isEmpty()) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            messageManager.sendMessage(player, "commands.list.no-tombstones", placeholders);
            return;
        }
        
        // 发送墓碑列表
        Map<String, String> placeholders = messageManager.createPlaceholders();
        messageManager.addPlayerPlaceholders(placeholders, player);
        placeholders.put("count", String.valueOf(tombstones.size()));
        messageManager.sendMessage(player, "commands.list.header", placeholders);
        
        for (int i = 0; i < tombstones.size(); i++) {
            DataManager.TombstoneData tombstone = tombstones.get(i);
            Map<String, String> tombstonePlaceholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(tombstonePlaceholders, player);
            tombstonePlaceholders.put("index", String.valueOf(i + 1));
            tombstonePlaceholders.put("world", tombstone.worldName());
            tombstonePlaceholders.put("x", String.valueOf(tombstone.x()));
            tombstonePlaceholders.put("y", String.valueOf(tombstone.y()));
            tombstonePlaceholders.put("z", String.valueOf(tombstone.z()));
            tombstonePlaceholders.put("exp", String.valueOf(tombstone.experience()));
            messageManager.sendMessage(player, "commands.list.item", tombstonePlaceholders);
        }
    }
    
    /**
     * 处理GUI命令
     * 统一的GUI命令处理方法
     * 
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleGUICommand(@NotNull CommandSender sender, @NotNull String[] args) {
        // 只允许玩家使用
        if (!(sender instanceof Player player)) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.sendMessage(sender, "commands.player-only", placeholders);
            return;
        }
        
        // 检查权限
        if (!player.hasPermission("playerdeadmanager.gui")) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            messageManager.sendMessage(player, "permission.no-gui", placeholders);
            return;
        }
        
        // 打开传送GUI
        TeleportGUI teleportGUI = new TeleportGUI(
            plugin,
            player,
            configManager,
            messageManager,
            tombstoneManager
        );

        // 设置GUI管理器
        teleportGUI.setGUIManager(plugin.getGUIManager());

        teleportGUI.openGUI();
    }
    
    /**
     * 处理传送命令
     * 统一的传送命令处理方法
     * 
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleTeleportCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        // 只允许玩家使用
        if (!(sender instanceof Player player)) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.sendMessage(sender, "commands.player-only", placeholders);
            return;
        }
        
        // 检查权限
        if (!player.hasPermission("playerdeadmanager.teleport")) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            messageManager.sendMessage(player, "permission.no-teleport", placeholders);
            return;
        }
        
        // 检查参数
        if (args.length == 0) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            messageManager.sendMessage(player, "commands.teleport.usage", placeholders);
            return;
        }
        
        // 解析墓碑索引
        int tombstoneIndex;
        try {
            tombstoneIndex = Integer.parseInt(args[0]) - 1; // 转换为0基索引
        } catch (NumberFormatException e) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            messageManager.sendMessage(player, "commands.teleport.invalid-number", placeholders);
            return;
        }
        
        // 获取玩家的墓碑列表
        List<DataManager.TombstoneData> tombstones = tombstoneManager.getPlayerTombstones(player.getUniqueId());
        
        if (tombstoneIndex < 0 || tombstoneIndex >= tombstones.size()) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            placeholders.put("max", String.valueOf(tombstones.size()));
            messageManager.sendMessage(player, "commands.teleport.invalid-index", placeholders);
            return;
        }
        
        // 执行传送
        DataManager.TombstoneData tombstone = tombstones.get(tombstoneIndex);
        teleportToTombstone(player, tombstone);
    }
    
    /**
     * 处理信息命令
     * 统一的信息命令处理方法
     *
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleInfoCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        Map<String, String> placeholders = messageManager.createPlaceholders();
        if (sender instanceof Player player) {
            messageManager.addPlayerPlaceholders(placeholders, player);
        }
        placeholders.put("version", plugin.getDescription().getVersion());
        placeholders.put("author", plugin.getDescription().getAuthors().toString());
        messageManager.sendMessage(sender, "commands.info", placeholders);
    }

    /**
     * 处理清理命令
     * 统一的清理命令处理方法
     *
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleCleanupCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        // 检查权限
        if (!sender.hasPermission("playerdeadmanager.admin.cleanup")) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            if (sender instanceof Player player) {
                messageManager.addPlayerPlaceholders(placeholders, player);
            }
            messageManager.sendMessage(sender, "permission.no-cleanup", placeholders);
            return;
        }

        // 发送开始清理消息
        Map<String, String> placeholders = messageManager.createPlaceholders();
        if (sender instanceof Player player) {
            messageManager.addPlayerPlaceholders(placeholders, player);
        }
        messageManager.sendMessage(sender, "commands.cleanup.start", placeholders);

        // 在主线程执行清理操作，避免异步访问方块状态错误
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                // 获取EntityCleanupManager
                cn.i7mc.utils.EntityCleanupManager entityCleanupManager =
                    tombstoneManager.getEntityCleanupManager();

                // 执行清理
                entityCleanupManager.cleanupAllTombstoneEntities();

                // 发送完成消息
                Map<String, String> completePlaceholders = messageManager.createPlaceholders();
                if (sender instanceof Player player) {
                    messageManager.addPlayerPlaceholders(completePlaceholders, player);
                }
                messageManager.sendMessage(sender, "commands.cleanup.complete", completePlaceholders);

            } catch (Exception e) {
                plugin.getLogger().severe("清理残留实体时发生错误: " + e.getMessage());
                e.printStackTrace();

                // 发送错误消息
                Map<String, String> errorPlaceholders = messageManager.createPlaceholders();
                if (sender instanceof Player player) {
                    messageManager.addPlayerPlaceholders(errorPlaceholders, player);
                }
                messageManager.sendMessage(sender, "commands.cleanup.error", errorPlaceholders);
            }
        });
    }
    
    /**
     * 发送帮助消息
     * 统一的帮助消息发送方法
     * 
     * @param sender 命令发送者
     */
    private void sendHelpMessage(@NotNull CommandSender sender) {
        Map<String, String> placeholders = messageManager.createPlaceholders();
        if (sender instanceof Player player) {
            messageManager.addPlayerPlaceholders(placeholders, player);
        }
        
        messageManager.sendMessage(sender, "commands.help.header", placeholders);
        messageManager.sendMessage(sender, "commands.help.list", placeholders);
        messageManager.sendMessage(sender, "commands.help.gui", placeholders);
        messageManager.sendMessage(sender, "commands.help.teleport", placeholders);
        messageManager.sendMessage(sender, "commands.help.reload", placeholders);
        messageManager.sendMessage(sender, "commands.help.cleanup", placeholders);
        messageManager.sendMessage(sender, "commands.help.info", placeholders);
    }
    
    /**
     * 发送未知命令消息
     * 统一的未知命令消息发送方法
     * 
     * @param sender 命令发送者
     * @param subCommand 子命令
     */
    private void sendUnknownCommandMessage(@NotNull CommandSender sender, @NotNull String subCommand) {
        Map<String, String> placeholders = messageManager.createPlaceholders();
        if (sender instanceof Player player) {
            messageManager.addPlayerPlaceholders(placeholders, player);
        }
        placeholders.put("command", subCommand);
        messageManager.sendMessage(sender, "commands.unknown", placeholders);
    }
    
    /**
     * 传送到墓碑
     * 统一的传送方法
     * 
     * @param player 玩家
     * @param tombstone 墓碑数据
     */
    private void teleportToTombstone(@NotNull Player player, @NotNull DataManager.TombstoneData tombstone) {
        // 创建传送位置
        org.bukkit.Location teleportLocation = new org.bukkit.Location(
            plugin.getServer().getWorld(tombstone.worldName()),
            tombstone.x() + 0.5,
            tombstone.y() + 1,
            tombstone.z() + 0.5
        );
        
        // 检查世界是否存在
        if (teleportLocation.getWorld() == null) {
            Map<String, String> placeholders = messageManager.createPlaceholders();
            messageManager.addPlayerPlaceholders(placeholders, player);
            placeholders.put("world", tombstone.worldName());
            messageManager.sendMessage(player, "teleport.world-not-found", placeholders);
            return;
        }
        
        // 执行传送
        player.teleport(teleportLocation);
        
        // 发送成功消息
        Map<String, String> placeholders = messageManager.createPlaceholders();
        messageManager.addPlayerPlaceholders(placeholders, player);
        messageManager.addLocationPlaceholders(placeholders, 
            tombstone.worldName(), tombstone.x(), tombstone.y(), tombstone.z());
        messageManager.sendMessage(player, "teleport.success", placeholders);
    }
    
    /**
     * 提供命令补全
     * 统一的命令补全方法
     * 
     * @param sender 命令发送者
     * @param command 命令
     * @param alias 命令别名
     * @param args 命令参数
     * @return 补全建议列表
     */
    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, 
                                    @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // 第一级子命令补全
            List<String> subCommands = Arrays.asList("help", "reload", "list", "gui", "teleport", "info", "cleanup");
            String input = args[0].toLowerCase();

            for (String subCommand : subCommands) {
                String permission = subCommand.equals("cleanup") ? "playerdeadmanager.admin.cleanup" : "playerdeadmanager." + subCommand;
                if (subCommand.startsWith(input) && sender.hasPermission(permission)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("teleport")) {
            // 传送命令的墓碑索引补全
            if (sender instanceof Player player) {
                List<DataManager.TombstoneData> tombstones = tombstoneManager.getPlayerTombstones(player.getUniqueId());
                for (int i = 1; i <= tombstones.size(); i++) {
                    completions.add(String.valueOf(i));
                }
            }
        }
        
        return completions;
    }
}
