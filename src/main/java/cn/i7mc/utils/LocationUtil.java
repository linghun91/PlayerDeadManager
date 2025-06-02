package cn.i7mc.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 位置工具类 - 统一处理位置相关逻辑
 * 遵循统一方法原则，避免重复造轮子
 * 
 * @author saga
 * @version 1.0.0
 */
public class LocationUtil {
    
    /**
     * 检查位置是否安全放置墓碑
     * 统一的位置安全检查方法
     * 
     * @param location 要检查的位置
     * @return 是否安全
     */
    public static boolean isSafeLocation(@NotNull Location location) {
        if (location.getWorld() == null) {
            return false;
        }
        
        Block block = location.getBlock();
        Block above = block.getRelative(0, 1, 0);
        
        // 检查当前位置是否可替换
        if (!isReplaceable(block.getType())) {
            return false;
        }
        
        // 检查上方是否有足够空间
        if (!isReplaceable(above.getType())) {
            return false;
        }
        
        // 检查是否在世界边界内
        return isWithinWorldBounds(location);
    }
    
    /**
     * 检查方块类型是否可替换
     * 统一的方块替换检查方法
     * 
     * @param material 方块类型
     * @return 是否可替换
     */
    public static boolean isReplaceable(@NotNull Material material) {
        return material == Material.AIR || 
               material == Material.WATER || 
               material == Material.LAVA ||
               material == Material.TALL_GRASS ||
               material == Material.GRASS ||
               material == Material.FERN ||
               material == Material.DEAD_BUSH ||
               material == Material.SNOW;
    }
    
    /**
     * 检查位置是否在世界边界内
     * 统一的世界边界检查方法
     * 
     * @param location 要检查的位置
     * @return 是否在边界内
     */
    public static boolean isWithinWorldBounds(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        
        int y = location.getBlockY();
        return y >= world.getMinHeight() && y <= world.getMaxHeight() - 2;
    }
    
    /**
     * 寻找附近安全的位置
     * 统一的安全位置查找方法
     * 
     * @param center 中心位置
     * @param radius 搜索半径
     * @return 安全位置，找不到返回null
     */
    @Nullable
    public static Location findSafeNearbyLocation(@NotNull Location center, int radius) {
        World world = center.getWorld();
        if (world == null) {
            return null;
        }
        
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();
        
        // 螺旋搜索算法，从中心向外扩展
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    // 只检查当前半径边界上的点
                    if (Math.abs(dx) != r && Math.abs(dz) != r && r > 0) {
                        continue;
                    }
                    
                    for (int dy = -2; dy <= 2; dy++) {
                        Location testLocation = new Location(world, 
                            centerX + dx, centerY + dy, centerZ + dz);
                        
                        if (isSafeLocation(testLocation)) {
                            return testLocation;
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * 计算两个位置之间的距离
     * 统一的距离计算方法
     * 
     * @param loc1 位置1
     * @param loc2 位置2
     * @return 距离，如果世界不同返回-1
     */
    public static double getDistance(@NotNull Location loc1, @NotNull Location loc2) {
        if (!loc1.getWorld().equals(loc2.getWorld())) {
            return -1;
        }
        return loc1.distance(loc2);
    }
    
    /**
     * 格式化位置为字符串
     * 统一的位置格式化方法
     * 
     * @param location 位置
     * @return 格式化的位置字符串
     */
    @NotNull
    public static String formatLocation(@NotNull Location location) {
        return String.format("%s: %d, %d, %d", 
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ());
    }
    
    /**
     * 检查位置是否相等（忽略小数部分）
     * 统一的位置比较方法
     * 
     * @param loc1 位置1
     * @param loc2 位置2
     * @return 是否相等
     */
    public static boolean isSameBlockLocation(@NotNull Location loc1, @NotNull Location loc2) {
        return loc1.getWorld().equals(loc2.getWorld()) &&
               loc1.getBlockX() == loc2.getBlockX() &&
               loc1.getBlockY() == loc2.getBlockY() &&
               loc1.getBlockZ() == loc2.getBlockZ();
    }
}
