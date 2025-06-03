package cn.i7mc.managers;

import cn.i7mc.PlayerDeadManager;
import cn.i7mc.abstracts.AbstractDataManager;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 数据管理器 - 实现SQLite数据库操作
 * 继承AbstractDataManager，遵循统一方法原则
 * 
 * @author saga
 * @version 1.0.0
 */
public class DataManager extends AbstractDataManager {
    
    private final PlayerDeadManager plugin;
    private final File databaseFile;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     */
    public DataManager(@NotNull PlayerDeadManager plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "tombstones.db");
    }
    
    /**
     * 初始化数据库连接
     * 统一的数据库初始化方法
     */
    @Override
    public void initializeDatabase() throws SQLException {
        try {
            // 确保数据文件夹存在
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            // 创建SQLite连接
            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            
            // 启用外键约束
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
            
            // 创建表
            createTables();
            
            plugin.getLogger().info("数据库初始化完成");
        } catch (SQLException e) {
            plugin.getLogger().severe("数据库初始化失败!");
            throw e;
        }
    }
    
    /**
     * 关闭数据库连接
     * 统一的数据库关闭方法
     */
    @Override
    public void closeDatabase() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("数据库连接已关闭");
            } catch (SQLException e) {
                plugin.getLogger().warning("关闭数据库连接时出错: " + e.getMessage());
            }
        }
    }
    
    /**
     * 创建数据库表
     * 统一的表创建方法
     */
    @Override
    protected void createTables() throws SQLException {
        // 创建墓碑数据表
        String createTombstonesTable = """
            CREATE TABLE IF NOT EXISTS tombstones (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                world_name TEXT NOT NULL,
                x INTEGER NOT NULL,
                y INTEGER NOT NULL,
                z INTEGER NOT NULL,
                death_time BIGINT NOT NULL,
                protection_expire BIGINT NOT NULL,
                experience INTEGER NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        // 创建物品数据表
        String createItemsTable = """
            CREATE TABLE IF NOT EXISTS tombstone_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tombstone_id INTEGER NOT NULL,
                slot_index INTEGER NOT NULL,
                item_data BLOB NOT NULL,
                FOREIGN KEY (tombstone_id) REFERENCES tombstones(id) ON DELETE CASCADE
            )
        """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTombstonesTable);
            stmt.execute(createItemsTable);
            plugin.getLogger().info("数据库表创建完成");
        }
    }
    
    /**
     * 保存墓碑数据
     * 统一的墓碑保存方法
     * 
     * @param playerId 玩家UUID
     * @param worldName 世界名称
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @param deathTime 死亡时间
     * @param protectionExpire 保护过期时间
     * @param experience 经验值
     * @param items 物品数组
     * @return 墓碑ID
     * @throws SQLException 数据库异常
     */
    public long saveTombstone(@NotNull UUID playerId, @NotNull String worldName, 
                             int x, int y, int z, long deathTime, long protectionExpire, 
                             int experience, @NotNull ItemStack[] items) throws SQLException {
        final long[] tombstoneId = new long[1];
        
        executeTransaction(connection -> {
            // 插入墓碑基本信息
            String insertTombstone = """
                INSERT INTO tombstones (player_uuid, world_name, x, y, z, death_time, protection_expire, experience)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(insertTombstone, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, worldName);
                stmt.setInt(3, x);
                stmt.setInt(4, y);
                stmt.setInt(5, z);
                stmt.setLong(6, deathTime);
                stmt.setLong(7, protectionExpire);
                stmt.setInt(8, experience);
                
                stmt.executeUpdate();
                
                // 获取生成的墓碑ID
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        tombstoneId[0] = rs.getLong(1);
                    }
                }
            }
            
            // 保存物品数据
            if (tombstoneId[0] > 0) {
                saveItems(connection, tombstoneId[0], items);
            }
        });
        
        return tombstoneId[0];
    }
    
    /**
     * 保存物品数据
     * 统一的物品保存方法
     * 
     * @param connection 数据库连接
     * @param tombstoneId 墓碑ID
     * @param items 物品数组
     * @throws SQLException 数据库异常
     */
    private void saveItems(@NotNull Connection connection, long tombstoneId, @NotNull ItemStack[] items) throws SQLException {
        String insertItem = "INSERT INTO tombstone_items (tombstone_id, slot_index, item_data) VALUES (?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(insertItem)) {
            for (int i = 0; i < items.length; i++) {
                ItemStack item = items[i];
                if (item != null && !item.getType().isAir()) {
                    stmt.setLong(1, tombstoneId);
                    stmt.setInt(2, i);
                    stmt.setBytes(3, serializeItemStack(item));
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
        }
    }
    
    /**
     * 加载墓碑物品
     * 统一的物品加载方法
     * 返回物品和索引的映射列表，用于简单的顺序展示
     *
     * @param tombstoneId 墓碑ID
     * @return 物品索引映射列表
     * @throws SQLException 数据库异常
     */
    @NotNull
    public List<TombstoneItemData> loadTombstoneItems(long tombstoneId) throws SQLException {
        List<TombstoneItemData> items = new ArrayList<>();
        String query = "SELECT slot_index, item_data FROM tombstone_items WHERE tombstone_id = ? ORDER BY slot_index";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setLong(1, tombstoneId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int slotIndex = rs.getInt("slot_index");
                    byte[] itemData = rs.getBytes("item_data");

                    if (itemData != null) {
                        ItemStack item = deserializeItemStack(itemData);
                        items.add(new TombstoneItemData(slotIndex, item));
                    }
                }
            }
        }

        return items;
    }

    /**
     * 墓碑物品数据记录类
     * 包含物品和其在PlayerInventory中的原始索引
     */
    public record TombstoneItemData(
        int originalSlotIndex,
        ItemStack item
    ) {}
    
    /**
     * 移除墓碑中的单个物品
     * 统一的单个物品移除方法
     *
     * @param tombstoneId 墓碑ID
     * @param slotIndex 物品槽位索引
     * @throws SQLException 数据库异常
     */
    public void removeTombstoneItem(long tombstoneId, int slotIndex) throws SQLException {
        String deleteItem = "DELETE FROM tombstone_items WHERE tombstone_id = ? AND slot_index = ?";

        try (PreparedStatement stmt = connection.prepareStatement(deleteItem)) {
            stmt.setLong(1, tombstoneId);
            stmt.setInt(2, slotIndex);
            stmt.executeUpdate();
        }
    }

    /**
     * 移除墓碑中的经验值
     * 统一的经验移除方法
     *
     * @param tombstoneId 墓碑ID
     * @throws SQLException 数据库异常
     */
    public void removeTombstoneExperience(long tombstoneId) throws SQLException {
        String updateExperience = "UPDATE tombstones SET experience = 0 WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(updateExperience)) {
            stmt.setLong(1, tombstoneId);
            stmt.executeUpdate();
        }
    }

    /**
     * 检查墓碑是否为空（无物品且无经验）
     * 统一的空墓碑检查方法
     *
     * @param tombstoneId 墓碑ID
     * @return 是否为空
     * @throws SQLException 数据库异常
     */
    public boolean isTombstoneEmpty(long tombstoneId) throws SQLException {
        // 检查是否有物品
        String checkItems = "SELECT COUNT(*) FROM tombstone_items WHERE tombstone_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(checkItems)) {
            stmt.setLong(1, tombstoneId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return false; // 有物品
                }
            }
        }

        // 检查是否有经验
        String checkExperience = "SELECT experience FROM tombstones WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(checkExperience)) {
            stmt.setLong(1, tombstoneId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt("experience") > 0) {
                    return false; // 有经验
                }
            }
        }

        return true; // 墓碑为空
    }

    /**
     * 删除墓碑数据
     * 统一的墓碑删除方法
     *
     * @param tombstoneId 墓碑ID
     * @throws SQLException 数据库异常
     */
    public void deleteTombstone(long tombstoneId) throws SQLException {
        executeTransaction(connection -> {
            // 删除物品数据（外键约束会自动删除）
            String deleteItems = "DELETE FROM tombstone_items WHERE tombstone_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteItems)) {
                stmt.setLong(1, tombstoneId);
                stmt.executeUpdate();
            }

            // 删除墓碑数据
            String deleteTombstone = "DELETE FROM tombstones WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteTombstone)) {
                stmt.setLong(1, tombstoneId);
                stmt.executeUpdate();
            }
        });
    }

    /**
     * 清理过期墓碑
     * 统一的过期墓碑清理方法
     *
     * @param currentTime 当前时间戳
     * @return 清理的墓碑数量
     * @throws SQLException 数据库异常
     */
    public int cleanupExpiredTombstones(long currentTime) throws SQLException {
        String query = """
            SELECT id, player_uuid, world_name, x, y, z, death_time, protection_expire, experience
            FROM tombstones
            WHERE death_time + ? < ?
        """;

        List<TombstoneData> expiredTombstones = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            // 计算24小时的毫秒数
            long despawnTime = plugin.getConfig().getLong("tombstone.despawn-time", 24) * 60 * 60 * 1000;
            stmt.setLong(1, despawnTime);
            stmt.setLong(2, currentTime);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    expiredTombstones.add(new TombstoneData(
                        rs.getLong("id"),
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("world_name"),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z"),
                        rs.getLong("death_time"),
                        rs.getLong("protection_expire"),
                        rs.getInt("experience")
                    ));
                }
            }
        }

        // 删除过期墓碑
        int deletedCount = 0;
        for (TombstoneData tombstone : expiredTombstones) {
            try {
                deleteTombstone(tombstone.id());
                deletedCount++;
            } catch (SQLException e) {
                plugin.getLogger().warning("删除过期墓碑失败 ID: " + tombstone.id() + " - " + e.getMessage());
            }
        }

        return deletedCount;
    }
    
    /**
     * 获取玩家的墓碑列表
     * 统一的墓碑查询方法
     * 
     * @param playerId 玩家UUID
     * @return 墓碑数据列表
     * @throws SQLException 数据库异常
     */
    @NotNull
    public List<TombstoneData> getPlayerTombstones(@NotNull UUID playerId) throws SQLException {
        List<TombstoneData> tombstones = new ArrayList<>();
        String query = """
            SELECT id, world_name, x, y, z, death_time, protection_expire, experience 
            FROM tombstones WHERE player_uuid = ? ORDER BY death_time DESC
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerId.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TombstoneData data = new TombstoneData(
                        rs.getLong("id"),
                        playerId,
                        rs.getString("world_name"),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z"),
                        rs.getLong("death_time"),
                        rs.getLong("protection_expire"),
                        rs.getInt("experience")
                    );
                    tombstones.add(data);
                }
            }
        }
        
        return tombstones;
    }
    
    /**
     * 获取所有墓碑数据
     * 统一的全部墓碑查询方法
     *
     * @return 所有墓碑数据列表
     * @throws SQLException 数据库异常
     */
    @NotNull
    public List<TombstoneData> getAllTombstones() throws SQLException {
        List<TombstoneData> tombstones = new ArrayList<>();
        String query = """
            SELECT id, player_uuid, world_name, x, y, z, death_time, protection_expire, experience
            FROM tombstones ORDER BY death_time DESC
        """;

        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                TombstoneData data = new TombstoneData(
                    rs.getLong("id"),
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("world_name"),
                    rs.getInt("x"),
                    rs.getInt("y"),
                    rs.getInt("z"),
                    rs.getLong("death_time"),
                    rs.getLong("protection_expire"),
                    rs.getInt("experience")
                );
                tombstones.add(data);
            }
        }

        return tombstones;
    }

    /**
     * 墓碑数据记录类
     * 统一的数据传输对象
     */
    public record TombstoneData(
        long id,
        UUID playerUuid,
        String worldName,
        int x,
        int y,
        int z,
        long deathTime,
        long protectionExpire,
        int experience
    ) {}
}
