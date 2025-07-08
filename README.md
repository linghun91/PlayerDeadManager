# PlayerDeadManager (PDM) - 墓碑插件文档

## 📋 项目概述

PlayerDeadManager 是一个基于 Paper 1.20.1 的高级墓碑插件，在玩家死亡时自动创建墓碑保存物品和经验，支持保存图腾、GUI界面、传送功能、全息图显示、粒子效果等丰富功能。

- **API版本**: Paper 1.20.1
- **构建系统**: Gradle
- **指令简写**: `pdm`
- **数据库**: SQLite
- **多语言**: 支持中文/英文切换
- **bStats统计**: 插件ID 26074

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
│   │   ├── DataManager.java              # SQLite数据管理器
│   │   ├── MySQLDataManager.java         # MySQL数据管理器(预留)
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
│   ├── metrics/                          # 统计相关
│   │   └── Metrics.java                  # bStats统计
│   └── utils/                            # 工具类
│       ├── LocationUtil.java             # 位置工具
│       ├── TimeUtil.java                 # 时间工具
│       ├── HologramUtil.java             # 全息图工具
│       ├── ParticleUtil.java             # 粒子效果工具
│       └── EntityCleanupManager.java     # 实体清理管理器
└── src/main/resources/
    ├── plugin.yml                        # 插件描述文件
    ├── config.yml                        # 主配置文件
    ├── message.yml                       # 中文消息配置文件
    └── message_en.yml                    # 英文消息配置文件
```

## 🎯 核心功能详解

### 1. 墓碑系统
**功能特性**:
- **自动创建墓碑**: 玩家死亡时自动在死亡位置创建墓碑，保存所有物品和经验
- **墓碑保护**: 墓碑有保护期（默认60分钟），期间只有所有者和管理员可访问
- **自动清理**: 墓碑24小时后自动消失，定时清理过期墓碑
- **数量限制**: 每个玩家最多3个墓碑，超出时自动移除最旧的
- **智能位置**: 死亡位置不可用时自动搜索附近可用位置
- **世界过滤**: 支持启用/禁用特定世界

**API参考**:
- [PlayerDeathEvent](https://jd.papermc.io/paper/1.20.1/org/bukkit/event/entity/PlayerDeathEvent.html)
- [Player](https://jd.papermc.io/paper/1.20.1/org/bukkit/entity/Player.html)
- [Block](https://jd.papermc.io/paper/1.20.1/org/bukkit/block/Block.html)
- [ItemStack序列化](https://jd.papermc.io/paper/1.20.1/org/bukkit/inventory/ItemStack.html#serializeAsBytes--)
- [ItemStack反序列化](https://jd.papermc.io/paper/1.20.1/org/bukkit/inventory/ItemStack.html#deserializeBytes-byte:A-)

**实现方法**:
- 使用 `AbstractTombstone` 抽象类统一处理墓碑逻辑
- 通过 `TombstoneManager` 管理所有墓碑操作
- 墓碑方块使用 `PersistentDataContainer` 存储墓碑ID标记
- 物品和经验数据使用SQLite数据库存储
- 物品序列化: `ItemStack.serializeAsBytes()` 转换为byte[]存储
- 物品反序列化: `ItemStack.deserializeBytes(byte[])` 从byte[]恢复

### 2. 保存图腾功能
**功能特性**:
- **图腾激活**: 死亡时如果背包有保存图腾，消耗1个图腾保护物品和经验
- **完全保护**: 激活图腾后保持背包和经验不掉落
- **自定义图腾**: 支持自定义图腾物品类型、显示名称、模型数据

**API参考**:
- [ItemStack](https://jd.papermc.io/paper/1.20.1/org/bukkit/inventory/ItemStack.html)
- [ItemMeta](https://jd.papermc.io/paper/1.20.1/org/bukkit/inventory/meta/ItemMeta.html)

**实现方法**:
- 在 `PlayerDeathListener` 中检查图腾
- 使用统一的物品检测方法避免重复代码
- 支持自定义模型数据和显示名称验证

### 3. GUI界面系统
**功能特性**:
- **传送GUI**: 显示所有墓碑列表，点击可传送到墓碑位置
- **物品回收GUI**: 右键墓碑打开，可逐个取出物品和经验
- **智能布局**: 支持边框装饰、分页显示、功能按钮
- **权限控制**: 细粒度的GUI访问权限控制

**API参考**:
- [Inventory](https://jd.papermc.io/paper/1.20.1/org/bukkit/inventory/Inventory.html)
- [InventoryClickEvent](https://jd.papermc.io/paper/1.20.1/org/bukkit/event/inventory/InventoryClickEvent.html)

**实现方法**:
- 使用 `AbstractGUI` 抽象类统一GUI处理
- 所有GUI文本通过 `message.yml` 配置
- `GUIManager` 统一管理活跃的GUI实例
- 支持右键点击取出物品，防止误操作

### 4. 视觉效果系统
**功能特性**:
- **全息图显示**: 墓碑上方显示所有者、死亡时间、保护状态
- **粒子效果**: 墓碑周围的灵魂粒子效果
- **自定义方块**: 支持PLAYER_HEAD等自定义墓碑方块

**API参考**:
- [ArmorStand](https://jd.papermc.io/paper/1.20.1/org/bukkit/entity/ArmorStand.html)
- [Particle](https://jd.papermc.io/paper/1.20.1/org/bukkit/Particle.html)

**实现方法**:
- `HologramUtil` 管理全息图创建和更新
- `ParticleUtil` 管理粒子效果
- 使用 `PersistentDataContainer` 标记全息图实体

### 5. 实体清理管理系统
**API参考**:
- [PersistentDataContainer](https://jd.papermc.io/paper/1.20.1/org/bukkit/persistence/PersistentDataContainer.html)
- [NamespacedKey](https://jd.papermc.io/paper/1.20.1/org/bukkit/NamespacedKey.html)
- [ArmorStand](https://jd.papermc.io/paper/1.20.1/org/bukkit/entity/ArmorStand.html)
- [TileState](https://jd.papermc.io/paper/1.20.1/org/bukkit/block/TileState.html)

**实现方法**:
- 墓碑方块使用 `NamespacedKey("playerdeadmanager", "tombstone_id")` 标记存储墓碑ID
- 全息图ArmorStand使用 `NamespacedKey("playerdeadmanager", "pdm_hologram")` 标记存储墓碑ID
- 插件启动时自动清理所有带PDM标记的残留实体
- 清理完成后根据数据库重新生成有效的墓碑实体
- 统一的实体识别和清理方法，避免残留实体问题



## 🎮 指令系统

### 主要指令
- `/pdm list` - 查看你的墓碑列表
- `/pdm gui` - 打开传送GUI界面
- `/pdm teleport <编号>` - 传送到指定墓碑
- `/pdm reload` - 重新加载配置文件（管理员）
- `/pdm info` - 查看插件信息
- `/pdm help` - 显示帮助信息

### 权限节点
- `playerdeadmanager.*` - 所有权限
- `playerdeadmanager.use` - 使用墓碑系统的基本权限（默认：true）
- `playerdeadmanager.admin` - 管理员权限（默认：op）
- `playerdeadmanager.gui` - 使用GUI界面的权限（默认：true）
- `playerdeadmanager.teleport` - 传送到墓碑的权限（默认：true）
- `playerdeadmanager.list` - 查看墓碑列表的权限（默认：true）
- `playerdeadmanager.access.expired` - 访问过期墓碑的权限（默认：op）

## 📝 配置文件设计

### config.yml 主要配置项
- **语言设置**: 支持中文(zh)/英文(en)切换
- **墓碑设置**: 方块类型、保护时间、消失时间、数量限制
- **保存图腾**: 图腾物品类型、显示名称、自定义模型数据
- **传送GUI**: 标题、大小、传送费用设置
- **全息图**: 启用状态、高度偏移、内容格式、更新间隔
- **粒子效果**: 粒子类型、数量、范围、生成间隔
- **数据库**: 文件名、连接池、超时设置
- **世界设置**: 启用/禁用世界列表
- **权限设置**: 权限节点配置
- **兼容性**: Vault、WorldGuard等插件支持

### message.yml / message_en.yml
- **多语言支持**: 中文和英文消息配置
- **占位符系统**: 支持{player}、{time}、{location}等动态占位符
- **颜色代码**: 完整的Minecraft颜色代码支持
- **GUI消息**: 所有GUI标题、按钮、提示信息
- **指令消息**: 帮助信息、错误提示、成功消息
- **时间格式**: 时间显示格式和相对时间描述

## 🗄️ 数据库设计

### SQLite表结构
**tombstones表**:
- `id` - 墓碑唯一ID（主键）
- `player_uuid` - 玩家UUID
- `world_name` - 世界名称
- `x, y, z` - 墓碑坐标
- `death_time` - 死亡时间戳
- `protection_expire` - 保护过期时间戳
- `experience` - 存储的经验值
- `created_at` - 创建时间

**tombstone_items表**:
- `id` - 物品记录ID（主键）
- `tombstone_id` - 关联的墓碑ID（外键）
- `slot_index` - 物品在背包中的原始槽位索引
- `item_data` - 序列化的物品数据（BLOB）

## 🔧 开发工具

- **IDE**: 支持Gradle的Java IDE
- **JDK**: Java 17
- **测试服务器**: Paper 1.20.1
- **构建工具**: Gradle 8.x
- **数据库**: SQLite（内置）

## 📊 统计功能

- **bStats集成**: 插件ID 26074
- **统计数据**: 服务器类型、功能启用状态、配置使用情况
- **自定义图表**: 保护时间分布、最大墓碑数量等

## 🛡️ 保护机制

### 墓碑保护
- **时间保护**: 墓碑在保护期内只有所有者和管理员可访问
- **爆炸保护**: 墓碑永远不会被爆炸破坏
- **方块保护**: 防止其他玩家破坏或在墓碑位置放置方块
- **权限保护**: 基于权限系统的细粒度访问控制

### 数据保护
- **事务处理**: 完整的数据库事务支持，确保数据一致性
- **异常处理**: 完善的异常处理机制，防止数据丢失
- **连接管理**: 自动重连和连接池管理
- **数据验证**: UUID格式验证、物品数据完整性检查

## 🔄 性能优化

### 异步处理
- **数据库操作**: 所有数据库操作异步执行，不阻塞主线程
- **定时任务**: 墓碑清理和更新任务异步运行
- **文件操作**: 配置文件读写异步处理

### 内存管理
- **活跃墓碑缓存**: 只在内存中保存活跃的墓碑实例
- **自动清理**: 过期墓碑自动从内存和数据库清理
- **连接池**: 数据库连接池管理，避免连接泄漏

### 智能加载
- **按需加载**: 墓碑物品数据按需从数据库加载
- **批量操作**: 使用批量插入和删除提高性能
- **索引优化**: 数据库查询使用适当的索引

## 🎨 自定义功能

### 外观自定义
- **墓碑方块**: 支持任意方块类型，包括玩家头颅
- **全息图**: 完全可自定义的全息图内容和格式
- **粒子效果**: 可配置的粒子类型、数量和效果
- **GUI界面**: 可自定义的GUI标题、按钮和布局

### 行为自定义
- **时间设置**: 保护时间、消失时间、清理间隔完全可配置
- **数量限制**: 可设置每个玩家的最大墓碑数量
- **世界控制**: 可指定启用或禁用的世界列表
- **权限控制**: 细粒度的权限节点配置

## 🔌 扩展性设计

### 抽象类架构
- **AbstractTombstone**: 墓碑功能的抽象基类，便于扩展不同类型的墓碑
- **AbstractGUI**: GUI界面的抽象基类，统一GUI处理逻辑
- **AbstractDataManager**: 数据管理的抽象基类，支持不同数据库类型

### 管理器模式
- **模块化设计**: 每个功能模块独立管理，便于维护和扩展
- **统一接口**: 所有管理器提供统一的接口，便于调用和测试
- **依赖注入**: 管理器之间通过构造函数注入依赖，降低耦合

### 插件兼容
- **Vault支持**: 预留经济插件支持接口
- **WorldGuard支持**: 预留区域保护插件支持
- **Oraxen支持**: 支持自定义方块和物品

## 💡 使用说明

### 基本使用流程
1. **玩家死亡**: 自动创建墓碑，保存所有物品和经验
2. **查看墓碑**: 使用 `/pdm list` 查看墓碑列表
3. **传送到墓碑**: 使用 `/pdm gui` 打开传送界面，或 `/pdm teleport <编号>` 直接传送
4. **回收物品**: 右键点击墓碑打开物品回收界面，逐个取出物品和经验
5. **自动清理**: 墓碑24小时后自动消失

### 保存图腾使用
1. **获得图腾**: 管理员可通过配置设置图腾物品类型
2. **携带图腾**: 将保存图腾放在背包中
3. **死亡保护**: 死亡时自动消耗1个图腾，保护所有物品和经验
4. **无墓碑**: 使用图腾后不会创建墓碑，物品直接保留在背包中

### 管理员功能
- **重载配置**: `/pdm reload` 重新加载所有配置文件
- **访问所有墓碑**: 管理员可以访问任何玩家的墓碑
- **强制清理**: 可以手动破坏墓碑进行清理
- **权限管理**: 通过权限插件控制玩家功能访问

## 🚀 安装与配置

### 安装步骤
1. 下载插件jar文件
2. 将jar文件放入服务器的 `plugins` 文件夹
3. 重启服务器或使用 `/reload` 命令
4. 插件会自动生成配置文件和数据库

### 基础配置
1. **语言设置**: 在 `config.yml` 中设置 `language.code` 为 `zh` 或 `en`
2. **墓碑方块**: 设置 `tombstone.block-type` 为想要的方块类型
3. **保护时间**: 调整 `tombstone.protection-time` 设置保护时长（分钟）
4. **世界控制**: 在 `worlds` 部分配置启用/禁用的世界

### 高级配置
1. **自定义消息**: 编辑 `message.yml` 自定义所有消息内容
2. **全息图**: 在 `hologram` 部分配置全息图显示内容和格式
3. **粒子效果**: 在 `particles` 部分配置粒子类型和效果
4. **权限设置**: 配置细粒度的权限控制

## 🐛 故障排除

### 常见问题
1. **墓碑不显示**: 检查世界是否在禁用列表中
2. **无法取出物品**: 检查玩家是否有相应权限
3. **全息图不显示**: 确认 `hologram.enabled` 为 true
4. **数据库错误**: 检查插件文件夹权限和磁盘空间

### 调试方法
1. **查看日志**: 检查服务器控制台的错误信息
2. **权限检查**: 确认玩家拥有必要的权限节点
3. **配置验证**: 使用 YAML 验证器检查配置文件格式
4. **版本兼容**: 确认使用 Paper 1.20.1 或兼容版本

## 📈 更新日志

### 版本 1.0.0
- 初始版本发布
- 完整的墓碑系统实现
- 保存图腾功能
- GUI界面系统
- 多语言支持
- 全息图和粒子效果
- 完善的权限系统
- bStats统计集成