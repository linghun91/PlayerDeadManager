# PlayerDeadManager (PDM) - User Manual

## 📖 Plugin Overview

PlayerDeadManager is a powerful tombstone plugin that automatically creates a tombstone to protect your items and experience when you die in the game. You can retrieve your items through various methods including direct interaction, GUI interfaces, and teleportation features.

## 🎮 Core Features

### 🪦 Tombstone System
- **Auto Creation**: Automatically creates tombstones at death location
- **Item Protection**: Protects all dropped items and experience points
- **Time Protection**: Tombstones are accessible only by owner for a certain period
- **Auto Cleanup**: Automatically cleans unclaimed tombstones after 24 hours
- **Quantity Limit**: Each player can have maximum 3 tombstones simultaneously

### 🔮 Saving Totem
- **Special Item**: Use saving totems to protect items upon death
- **Consumption Mechanism**: Automatically consumes 1 saving totem when dying
- **Complete Protection**: Items and experience won't drop when using totems

### 🖥️ GUI Interface System
- **Teleport GUI**: View all tombstone locations and quick teleportation
- **Item Recovery GUI**: Directly retrieve items and experience from interface
- **Intuitive Operation**: Complete various operations with simple clicks

## 🎯 Basic Usage

### Post-Death Operation Flow

1. **Upon Death**: System automatically creates tombstone and displays location info
2. **Finding Tombstone**: 
   - Use `/pdm list` to view tombstone list
   - Use `/pdm gui` to open teleport interface
   - Follow particle effects and hologram guidance
3. **Item Recovery**:
   - Right-click tombstone to open item recovery interface
   - Click items in GUI to retrieve them directly
   - Click experience button to retrieve experience points

### Tombstone Interaction Methods

#### Method 1: Direct Interaction
- **Right-click Tombstone**: Opens item recovery GUI interface
- **Right-click Items in GUI**: Retrieves items to inventory
- **Click Experience Button**: Retrieves all experience points

#### Method 2: Command Operations
- **View List**: `/pdm list` - Shows all tombstone locations
- **Teleport to Tombstone**: `/pdm teleport <number>` - Teleports to specified tombstone
- **Open GUI**: `/pdm gui` - Opens teleport interface

## 📋 Command List

### Basic Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/pdm help` | Show help information | `playerdeadmanager.use` |
| `/pdm list` | View your tombstone list | `playerdeadmanager.list` |
| `/pdm gui` | Open teleport GUI interface | `playerdeadmanager.gui` |
| `/pdm teleport <number>` | Teleport to specified tombstone | `playerdeadmanager.teleport` |
| `/pdm info` | View plugin information | `playerdeadmanager.use` |

### Admin Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/pdm reload` | Reload configuration files | `playerdeadmanager.admin.reload` |

### Command Aliases
- `/pdm` = `/playerdeadmanager`
- All commands support full names

## 🔐 Permission System

### Player Permissions
- `playerdeadmanager.use` - Basic permission to use tombstone system (Default: All players)
- `playerdeadmanager.gui` - Permission to use GUI interfaces (Default: All players)
- `playerdeadmanager.teleport` - Permission to teleport to tombstones (Default: All players)
- `playerdeadmanager.list` - Permission to view tombstone list (Default: All players)

### Admin Permissions
- `playerdeadmanager.admin` - Administrator permission (Default: OP)
- `playerdeadmanager.admin.reload` - Permission to reload config (Default: OP)
- `playerdeadmanager.access.expired` - Permission to access expired tombstones (Default: OP)
- `playerdeadmanager.*` - All permissions

## 🎨 Interface Features

### Teleport GUI Interface
- **Interface Title**: "Death Location Teleport"
- **Tombstone Icons**: Shows tombstone location, death time, protection status
- **Click to Teleport**: Click tombstone icon to teleport to that location
- **Cost Display**: Shows required coins if teleport cost is enabled

### Item Recovery GUI Interface
- **Interface Title**: "Tombstone Item Recovery"
- **Item Display**: First 5 rows show all items in tombstone
- **Experience Button**: Last row shows experience retrieval button
- **Close Button**: Close interface button in last row
- **Right-click to Take**: Right-click items to retrieve them to inventory

## ✨ Special Features

### 🔍 Visual Guidance System
- **Hologram Display**: Shows owner and time information above tombstone
- **Particle Effects**: Generates particle effects around tombstone for easy finding
- **Distance Hints**: Shows discovery hints when approaching tombstones

### 🛡️ Protection Mechanism
- **Time Protection**: Only owner can access tombstone for 60 minutes after creation
- **Expired Access**: Other players can loot items after protection period
- **Looting Notification**: Receive notification when other players loot your tombstone

### 📊 Experience Handling
- **Complete Recovery**: Tombstone owner can fully retrieve experience
- **Partial Gain**: Other players only get 50% experience when looting
- **Level Protection**: Configurable whether to preserve experience levels

## 🌍 World Restrictions

### Enabled Worlds
- Tombstone feature enabled in all worlds by default
- Can customize enabled world list through config file

### Disabled Worlds
- Tombstone feature disabled in Nether and End by default
- Administrators can adjust disabled world list through config file

## 💰 Economy System

### Teleport Cost
- Configurable whether teleporting to tombstones requires cost
- Requires Vault plugin support
- Default cost: 100 coins (configurable)
- Cost system can be completely disabled

## 🔧 Compatibility

### Supported Plugins
- **Vault**: Economy system support
- **Oraxen**: Custom block support
- **WorldGuard**: Region protection support (optional)
- **GriefPrevention**: Land protection support (optional)

### Server Requirements
- **Server**: Paper 1.20.1 or higher
- **Java Version**: Java 17 or higher

## 📝 Usage Tips

### Quick Item Recovery
1. Immediately use `/pdm gui` to open teleport interface after death
2. Click the latest tombstone icon to teleport to death location
3. Right-click tombstone to open item recovery interface
4. Right-click needed items to quickly retrieve them

### Saving Totem Usage
1. Prepare saving totems (Totem of Undying) in advance
2. Keep totems in inventory
3. Totems automatically activate upon death, items won't drop

### Managing Multiple Tombstones
1. Use `/pdm list` to view all tombstones
2. Remember tombstone numbers, use `/pdm teleport <number>` for quick teleport
3. Clean up unnecessary tombstones timely to avoid reaching quantity limit

## ❓ Frequently Asked Questions

**Q: Why wasn't my tombstone created?**
A: Check if tombstone feature is enabled in current world and if you've reached the tombstone quantity limit.

**Q: Can other players take my items?**
A: Tombstones have a 60-minute protection period during which only you can access them. After expiration, other players can loot them.

**Q: When do tombstones disappear?**
A: Tombstones automatically clean up after 24 hours, or disappear immediately when all items are retrieved.

**Q: How do I get saving totems?**
A: Saving totems are regular Totems of Undying, obtainable through normal in-game methods.

**Q: Does teleportation require cost?**
A: No cost by default, administrators can enable teleport costs through config file.

## 📞 Support & Feedback

If you encounter issues or have suggestions during usage, please contact server administrators. The plugin supports complete custom configuration, and administrators can adjust various settings according to server needs.
