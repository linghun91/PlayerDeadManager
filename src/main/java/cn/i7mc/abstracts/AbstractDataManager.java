package cn.i7mc.abstracts;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * 数据管理抽象类 - 统一处理数据存储相关逻辑
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public abstract class AbstractDataManager {
    
    protected Connection connection;
    
    /**
     * 初始化数据库连接
     * 统一的数据库初始化方法
     * 
     * @throws SQLException 数据库异常
     */
    public abstract void initializeDatabase() throws SQLException;
    
    /**
     * 关闭数据库连接
     * 统一的数据库关闭方法
     */
    public abstract void closeDatabase();
    
    /**
     * 创建数据库表
     * 统一的表创建方法
     * 
     * @throws SQLException 数据库异常
     */
    protected abstract void createTables() throws SQLException;
    
    /**
     * 序列化ItemStack为byte数组
     * 统一的物品序列化方法
     * 
     * @param itemStack 要序列化的物品
     * @return 序列化后的byte数组
     */
    @NotNull
    protected byte[] serializeItemStack(@NotNull ItemStack itemStack) {
        return itemStack.serializeAsBytes();
    }
    
    /**
     * 从byte数组反序列化ItemStack
     * 统一的物品反序列化方法
     * 
     * @param bytes 序列化的byte数组
     * @return 反序列化的ItemStack
     */
    @NotNull
    protected ItemStack deserializeItemStack(@NotNull byte[] bytes) {
        return ItemStack.deserializeBytes(bytes);
    }
    
    /**
     * 序列化ItemStack数组
     * 统一的物品数组序列化方法
     * 
     * @param items 物品数组
     * @return 序列化数据列表
     */
    @NotNull
    protected List<byte[]> serializeItemStacks(@NotNull ItemStack[] items) {
        return java.util.Arrays.stream(items)
                .filter(item -> item != null && !item.getType().isAir())
                .map(this::serializeItemStack)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 检查数据库连接是否有效
     * 统一的连接检查方法
     * 
     * @return 连接是否有效
     */
    protected boolean isConnectionValid() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * 重新连接数据库
     * 统一的重连方法
     * 
     * @throws SQLException 数据库异常
     */
    protected void reconnectDatabase() throws SQLException {
        if (!isConnectionValid()) {
            closeDatabase();
            initializeDatabase();
        }
    }
    
    /**
     * 执行数据库事务
     * 统一的事务处理方法
     * 
     * @param transaction 事务操作
     * @throws SQLException 数据库异常
     */
    protected void executeTransaction(@NotNull DatabaseTransaction transaction) throws SQLException {
        if (!isConnectionValid()) {
            reconnectDatabase();
        }
        
        boolean autoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            transaction.execute(connection);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }
    
    /**
     * 数据库事务接口
     */
    @FunctionalInterface
    protected interface DatabaseTransaction {
        void execute(@NotNull Connection connection) throws SQLException;
    }
    
    /**
     * 获取当前时间戳
     * 统一的时间戳获取方法
     * 
     * @return 当前时间戳
     */
    protected long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
    
    /**
     * 验证UUID格式
     * 统一的UUID验证方法
     * 
     * @param uuid UUID字符串
     * @return 是否为有效UUID
     */
    protected boolean isValidUUID(@Nullable String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return false;
        }
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * 安全关闭资源
     * 统一的资源关闭方法
     * 
     * @param autoCloseable 要关闭的资源
     */
    protected void safeClose(@Nullable AutoCloseable autoCloseable) {
        if (autoCloseable != null) {
            try {
                autoCloseable.close();
            } catch (Exception e) {
                // 静默关闭，避免在finally块中抛出异常
            }
        }
    }
}
