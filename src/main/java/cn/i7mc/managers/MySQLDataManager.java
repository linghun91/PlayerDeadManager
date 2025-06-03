package cn.i7mc.managers;

import cn.i7mc.PlayerDeadManager;
import cn.i7mc.abstracts.AbstractDataManager;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * MySQL数据管理器 - 实现MySQL数据库操作
 * 继承AbstractDataManager，遵循统一方法原则
 * 
 * @author saga
 * @version 1.0.0
 */
public class MySQLDataManager extends AbstractDataManager {
    
    private final PlayerDeadManager plugin;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean useSSL;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     */
    public MySQLDataManager(@NotNull PlayerDeadManager plugin) {
        this.plugin = plugin;
        this.host = plugin.getConfig().getString("database.mysql.host", "localhost");
        this.port = plugin.getConfig().getInt("database.mysql.port", 3306);
        this.database = plugin.getConfig().getString("database.mysql.database", "minecraft");
        this.username = plugin.getConfig().getString("database.mysql.username", "root");
        this.password = plugin.getConfig().getString("database.mysql.password", "password");
        this.useSSL = plugin.getConfig().getBoolean("database.mysql.use-ssl", false);
        
        // 设置表前缀
        String prefix = plugin.getConfig().getString("database.mysql.table-prefix", "pdm_");
        setTablePrefix(prefix);
    }
    
    /**
     * 初始化数据库连接
     * 统一的数据库初始化方法
     */
    @Override
    public void initializeDatabase() throws SQLException {
        try {
            // 构建MySQL连接URL
            String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                    host, port, database, useSSL);
            
            // 创建MySQL连接
            connection = DriverManager.getConnection(url, username, password);
            
            // 创建表
            createTables();
            
            plugin.getLogger().info("MySQL数据库初始化完成");
        } catch (SQLException e) {
            plugin.getLogger().severe("MySQL数据库初始化失败: " + e.getMessage());
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
                plugin.getLogger().info("MySQL数据库连接已关闭");
            } catch (SQLException e) {
                plugin.getLogger().warning("关闭MySQL数据库连接时出错: " + e.getMessage());
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
        String createTombstonesTable = String.format("""
            CREATE TABLE IF NOT EXISTS %s (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                player_uuid VARCHAR(36) NOT NULL,
                world_name VARCHAR(255) NOT NULL,
                x INT NOT NULL,
                y INT NOT NULL,
                z INT NOT NULL,
                death_time BIGINT NOT NULL,
                protection_expire BIGINT NOT NULL,
                experience INT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_player_uuid (player_uuid),
                INDEX idx_death_time (death_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """, getTableName("tombstones"));
        
        // 创建物品数据表
        String createItemsTable = String.format("""
            CREATE TABLE IF NOT EXISTS %s (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                tombstone_id BIGINT NOT NULL,
                slot_index INT NOT NULL,
                item_data LONGBLOB NOT NULL,
                FOREIGN KEY (tombstone_id) REFERENCES %s(id) ON DELETE CASCADE,
                INDEX idx_tombstone_id (tombstone_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """, getTableName("tombstone_items"), getTableName("tombstones"));
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTombstonesTable);
            stmt.execute(createItemsTable);
            plugin.getLogger().info("MySQL数据库表创建完成");
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
    @Override
    public long saveTombstone(@NotNull UUID playerId, @NotNull String worldName,
                             int x, int y, int z, long deathTime, long protectionExpire,
                             int experience, @NotNull ItemStack[] items) throws SQLException {
        final long[] tombstoneId = new long[1];
        
        executeTransaction(connection -> {
            // 插入墓碑基本信息
            String insertTombstone = String.format("""
                INSERT INTO %s (player_uuid, world_name, x, y, z, death_time, protection_expire, experience)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """, getTableName("tombstones"));
            
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
            if (tombstoneId[0] > 0 && items.length > 0) {
                String insertItem = String.format("""
                    INSERT INTO %s (tombstone_id, slot_index, item_data) VALUES (?, ?, ?)
                """, getTableName("tombstone_items"));
                
                try (PreparedStatement stmt = connection.prepareStatement(insertItem)) {
                    for (int i = 0; i < items.length; i++) {
                        ItemStack item = items[i];
                        if (item != null && !item.getType().isAir()) {
                            stmt.setLong(1, tombstoneId[0]);
                            stmt.setInt(2, i);
                            stmt.setBytes(3, serializeItemStack(item));
                            stmt.addBatch();
                        }
                    }
                    stmt.executeBatch();
                }
            }
        });
        
        return tombstoneId[0];
    }
    
    /**
     * 删除墓碑数据
     * 统一的墓碑删除方法
     *
     * @param tombstoneId 墓碑ID
     * @throws SQLException 数据库异常
     */
    @Override
    public void deleteTombstone(long tombstoneId) throws SQLException {
        String deleteTombstone = String.format("DELETE FROM %s WHERE id = ?", getTableName("tombstones"));
        
        try (PreparedStatement stmt = connection.prepareStatement(deleteTombstone)) {
            stmt.setLong(1, tombstoneId);
            stmt.executeUpdate();
        }
    }
    
    /**
     * 加载墓碑物品
     * 统一的物品加载方法
     *
     * @param tombstoneId 墓碑ID
     * @return 物品数据列表
     * @throws SQLException 数据库异常
     */
    @NotNull
    @Override
    public List<TombstoneItemData> loadTombstoneItems(long tombstoneId) throws SQLException {
        List<TombstoneItemData> items = new ArrayList<>();
        String query = String.format("""
            SELECT slot_index, item_data FROM %s
            WHERE tombstone_id = ? ORDER BY slot_index
        """, getTableName("tombstone_items"));

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setLong(1, tombstoneId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int slotIndex = rs.getInt("slot_index");
                    byte[] itemData = rs.getBytes("item_data");

                    if (itemData != null) {
                        try {
                            ItemStack item = deserializeItemStack(itemData);
                            items.add(new TombstoneItemData(slotIndex, item));
                        } catch (Exception e) {
                            plugin.getLogger().warning("反序列化物品失败: " + e.getMessage());
                        }
                    }
                }
            }
        }

        return items;
    }
    
    /**
     * 移除墓碑物品
     * 统一的物品移除方法
     *
     * @param tombstoneId 墓碑ID
     * @param slotIndex 槽位索引
     * @throws SQLException 数据库异常
     */
    @Override
    public void removeTombstoneItem(long tombstoneId, int slotIndex) throws SQLException {
        String deleteItem = String.format("""
            DELETE FROM %s WHERE tombstone_id = ? AND slot_index = ?
        """, getTableName("tombstone_items"));
        
        try (PreparedStatement stmt = connection.prepareStatement(deleteItem)) {
            stmt.setLong(1, tombstoneId);
            stmt.setInt(2, slotIndex);
            stmt.executeUpdate();
        }
    }
    
    /**
     * 检查墓碑是否为空
     * 统一的空墓碑检查方法
     * 
     * @param tombstoneId 墓碑ID
     * @return 是否为空
     * @throws SQLException 数据库异常
     */
    public boolean isTombstoneEmpty(long tombstoneId) throws SQLException {
        String query = String.format("SELECT COUNT(*) FROM %s WHERE tombstone_id = ?", getTableName("tombstone_items"));
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setLong(1, tombstoneId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) == 0;
                }
            }
        }
        
        return true;
    }

    /**
     * 移除墓碑经验
     * 统一的经验移除方法
     *
     * @param tombstoneId 墓碑ID
     * @throws SQLException 数据库异常
     */
    @Override
    public void removeTombstoneExperience(long tombstoneId) throws SQLException {
        String updateExperience = String.format("UPDATE %s SET experience = 0 WHERE id = ?",
                                               getTableName("tombstones"));

        try (PreparedStatement stmt = connection.prepareStatement(updateExperience)) {
            stmt.setLong(1, tombstoneId);
            stmt.executeUpdate();
        }
    }

    /**
     * 清理过期墓碑
     * 统一的过期墓碑清理方法
     *
     * @param currentTime 当前时间戳
     * @return 清理的墓碑数量
     * @throws SQLException 数据库异常
     */
    @Override
    public int cleanupExpiredTombstones(long currentTime) throws SQLException {

        String query = String.format("""
            SELECT id, player_uuid, world_name, x, y, z, death_time, protection_expire, experience
            FROM %s WHERE death_time < (? - ?)
        """, getTableName("tombstones"));

        List<AbstractDataManager.TombstoneData> expiredTombstones = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            // 计算24小时的毫秒数
            long despawnTime = plugin.getConfig().getLong("tombstone.despawn-time", 24) * 60 * 60 * 1000;
            stmt.setLong(1, currentTime);
            stmt.setLong(2, despawnTime);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    expiredTombstones.add(new AbstractDataManager.TombstoneData(
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
        for (AbstractDataManager.TombstoneData tombstone : expiredTombstones) {
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
    @Override
    public List<AbstractDataManager.TombstoneData> getPlayerTombstones(@NotNull UUID playerId) throws SQLException {
        List<AbstractDataManager.TombstoneData> tombstones = new ArrayList<>();
        String query = String.format("""
            SELECT id, world_name, x, y, z, death_time, protection_expire, experience
            FROM %s WHERE player_uuid = ? ORDER BY death_time DESC
        """, getTableName("tombstones"));

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerId.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AbstractDataManager.TombstoneData data = new AbstractDataManager.TombstoneData(
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
    @Override
    public List<AbstractDataManager.TombstoneData> getAllTombstones() throws SQLException {
        List<AbstractDataManager.TombstoneData> tombstones = new ArrayList<>();
        String query = String.format("""
            SELECT id, player_uuid, world_name, x, y, z, death_time, protection_expire, experience
            FROM %s ORDER BY death_time DESC
        """, getTableName("tombstones"));

        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                AbstractDataManager.TombstoneData data = new AbstractDataManager.TombstoneData(
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

}
