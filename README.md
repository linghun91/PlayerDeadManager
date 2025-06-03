# PlayerDeadManager (PDM) - 墓碑插件文档

## 📋 项目概述

PlayerDeadManager 是一个基于 Paper 1.20.1 的墓碑插件，在玩家死亡时创建墓碑，允许通过各种可配置的功能恢复物品和经验。

- **API版本**: Paper 1.20.1
- **构建系统**: Gradle
- **指令简写**: `pdm`

## 🔗 API参考文档

- **主要API文档**: [Paper Javadocs 1.20.1](https://jd.papermc.io/paper/1.20.1/)
- **事件系统**: [Bukkit Event API](https://jd.papermc.io/paper/1.20.1/org/bukkit/event/package-summary.html)
- **持久化数据**: [PersistentDataContainer](https://jd.papermc.io/paper/1.20.1/org/bukkit/persistence/PersistentDataContainer.html)

## 📁 项目结构

```
PlayerDeadManager/
├── build.gradle                           # Gradle构建配置
├── settings.gradle                        # 项目设置
├── gradle.properties                      # Gradle属性
├── README.md                              # 开发文档
├── src/main/java/cn/i7mc/
│   ├── PlayerDeadManager.java            # 主插件类
│   ├── abstracts/                        # 抽象类目录
│   │   ├── AbstractTombstone.java        # 墓碑抽象类
│   │   ├── AbstractGUI.java              # GUI抽象类
│   │   └── AbstractDataManager.java      # 数据管理抽象类
│   ├── managers/                          # 管理器类
│   │   ├── TombstoneManager.java         # 墓碑管理器
│   │   ├── DataManager.java              # 数据管理器
│   │   ├── ConfigManager.java            # 配置管理器
│   │   ├── MessageManager.java           # 消息管理器
│   │   └── GUIManager.java               # GUI管理器
│   ├── listeners/                         # 事件监听器
│   │   ├── PlayerDeathListener.java      # 玩家死亡监听
│   │   ├── PlayerInteractListener.java   # 玩家交互监听
│   │   ├── InventoryClickListener.java   # GUI点击监听
│   │   └── TombstoneProtectionListener.java # 墓碑保护监听
│   ├── guis/                             # GUI界面
│   │   ├── TeleportGUI.java              # 传送GUI
│   │   └── TombstoneItemsGUI.java        # 墓碑物品GUI
│   ├── commands/                         # 指令处理
│   │   └── PDMCommand.java               # 主指令处理
│   ├── tombstones/                       # 墓碑实现类
│   │   └── PlayerTombstone.java          # 玩家墓碑类
│   └── utils/                            # 工具类
│       ├── LocationUtil.java             # 位置工具
│       ├── TimeUtil.java                 # 时间工具
│       ├── HologramUtil.java             # 全息图工具
│       ├── ParticleUtil.java             # 粒子效果工具
│       └── EntityCleanupManager.java     # 实体清理管理器
└── src/main/resources/
    ├── plugin.yml                        # 插件描述文件
    ├── config.yml                        # 主配置文件
    └── message.yml                       # 消息配置文件
```

## 🎯 核心功能开发方法

### 1. 玩家死亡逻辑与墓碑创建
**API参考**:
- [PlayerDeathEvent](https://jd.papermc.io/paper/1.20.1/org/bukkit/event/entity/PlayerDeathEvent.html)
- [Player](https://jd.papermc.io/paper/1.20.1/org/bukkit/entity/Player.html)
- [Block](https://jd.papermc.io/paper/1.20.1/org/bukkit/block/Block.html)
- [ItemStack序列化](https://jd.papermc.io/paper/1.20.1/org/bukkit/inventory/ItemStack.html#serializeAsBytes--)
- [ItemStack反序列化](https://jd.papermc.io/paper/1.20.1/org/bukkit/inventory/ItemStack.html#deserializeBytes-byte:A-)

**实现方法**:
- 使用 `AbstractTombstone` 抽象类统一处理墓碑逻辑
- 通过 `TombstoneManager` 管理所有墓碑操作
- 墓碑方块使用 `PersistentDataContainer` 存储简洁key标记
- 物品和经验数据使用SQLite数据库存储
- 物品序列化: `ItemStack.serializeAsBytes()` 转换为byte[]存储
- 物品反序列化: `ItemStack.deserializeBytes(byte[])` 从byte[]恢复

### 2. 保存图腾功能
**API参考**:
- [ItemStack](https://jd.papermc.io/paper/1.20.1/org/bukkit/inventory/ItemStack.html)
- [ItemMeta](https://jd.papermc.io/paper/1.20.1/org/bukkit/inventory/meta/ItemMeta.html)

**实现方法**:
- 在 `PlayerDeathListener` 中检查图腾
- 使用统一的物品检测方法避免重复代码

### 3. 传送GUI系统
**API参考**:
- [Inventory](https://jd.papermc.io/paper/1.20.1/org/bukkit/inventory/Inventory.html)
- [InventoryClickEvent](https://jd.papermc.io/paper/1.20.1/org/bukkit/event/inventory/InventoryClickEvent.html)

**实现方法**:
- 使用 `AbstractGUI` 抽象类统一GUI处理
- 所有GUI文本通过 `message.yml` 配置

### 4. 实体清理管理系统
**API参考**:
- [PersistentDataContainer](https://jd.papermc.io/paper/1.20.1/org/bukkit/persistence/PersistentDataContainer.html)
- [NamespacedKey](https://jd.papermc.io/paper/1.20.1/org/bukkit/NamespacedKey.html)
- [ArmorStand](https://jd.papermc.io/paper/1.20.1/org/bukkit/entity/ArmorStand.html)
- [TileState](https://jd.papermc.io/paper/1.20.1/org/bukkit/block/TileState.html)

**实现方法**:
- 墓碑方块使用 `NamespacedKey("pdm", "tombstone_id")` 标记存储墓碑ID
- 全息图ArmorStand使用 `NamespacedKey("pdm", "pdm_hologram")` 标记存储墓碑ID
- 插件启动时自动清理所有带PDM标记的残留实体
- 清理完成后根据数据库重新生成有效的墓碑实体
- 统一的实体识别和清理方法，避免残留实体问题



## 📝 配置文件设计

### config.yml
- 墓碑方块类型配置
- 保护时间设置
- 传送费用配置
- 功能开关

### message.yml
- 所有用户可见消息
- GUI标题和描述
- 通知消息模板

## 🔧 开发工具

- **IDE**: 支持Gradle的Java IDE
- **JDK**: Java 17
- **测试服务器**: Paper 1.20.1