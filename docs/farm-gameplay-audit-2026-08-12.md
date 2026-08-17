# Animania Farm 玩法全面审计（2026-08-12）

## 结论

Farm 当前不能按 1.12 完整玩法基准闭合。注册、基础构造/存档、幼体成长、部分通用 AI、设施、流体、车辆和物品已经有可重放证据；但逐源码对照确认了 14 组必须先修复的玩法偏差，涉及全部 99 个 Farm 动物的属性与碰撞尺寸，以及战斗、猪骑乘、绝育、挤奶、Mooshroom 剪切、雷击转换、vanilla 替换、蜂巢、母鸡产蛋、啼鸣状态和奶酪模具。

本轮只执行审计与证据刷新，没有修改玩法代码，也没有写入迁移矩阵的 `closed` 状态。下述“可闭合”仅表示中央闭合器可以对**被具体测试断言覆盖的 requirement**进行复核，不能扩展为整类或整模块闭合。

## 固定基线、范围和实际执行

- 当前代码：`e15229c07d707676fd9c0c01fcf2af077332e8b3`。
- 1.12 只读基线：`32ae2b4c56cb84284e865dae0d3b78770992ba1d`。
- Farm 矩阵共 1,016 条：309 Java、707 resource；审计开始时为 `closed=341`、`implemented_unverified=669`、`unstarted=6`。这些旧状态没有在本轮被继承为新结论。
- 当前注册 102 个实体 ID：99 个动物、3 个车辆。
- `:farm:test --rerun-tasks`：16/16 单元测试通过，0 failure、0 error。
- `:farm:runGameTestServer`：Java 17、Forge 47.4.22 实际专服执行，60/60 required tests 通过；日志 SHA-256 为 `274a0f2282a9708a9a87ea0be8a59562f28db8c98f8e27d9dc2a183fe7635861`。
- 六个专用行为审计器在新日志上重新执行：幼体成长 6 results/6 rows、设施 25/10、流体 4/2、目标 10/5、特殊物品 15/7、车辆 16/7，均无 skipped/error。
- 旧静态审计器报告的 `provable row not closed` 只是“矩阵尚未被中央闭合器写为 closed”，不是功能失败；反过来，它们也不能单独证明功能正确。

## 完全无法关闭：必须实际修复

### F-GAME-001 全部 Farm 动物使用错误的统一属性

- 当前 `AnimaniaFarm.attributes()` 给 99 个动物全部安装 Base 通用的 10 生命、0.22 移速、1 攻击属性。
- 1.12 按家族和性别设置不同数值。例如鸡为 6 生命/0.29 移速，公鸡攻击 2；公牛为 24/0.20/4，母牛为 18/0.20/2；山羊、马、猪、羊及幼体也各有独立数值。
- 现有 60 个 GameTest 没有逐 ID 断言 max health、movement speed 和 attack damage。
- 修复门槛：建立 99 个动物 ID 的确定性属性表，对每个动物 ID 参数化断言旧默认值；3 个车辆 ID 单独验证车辆属性。

### F-GAME-002 碰撞尺寸被压平成两个通用尺寸

- 当前 `AnimaniaFarm.sizeFor()` 将所有成年动物设为 0.8×1.0、所有幼体设为 0.45×0.55。
- 1.12 家族/性别尺寸差异很大，例如公牛 1.6×1.8、母牛 1.4×1.8、马 1.8×2.2，并存在 Kiko、Angora、Nigerian Dwarf 等品种覆盖和成长阶段缩放。
- 该差异会影响碰撞、寻路、骑乘和牵引，不只是视觉问题。
- 修复门槛：逐 ID 尺寸表、成长前后 refreshDimensions、碰撞/骑乘/通道通过性测试。

### F-GAME-003 家畜反击与战斗目标不完整

- 当前通用 `registerGoals()` 不给普通 Farm 家畜安装旧版 hurt-by/melee 路径；只有伴侣动物、Extra 捕食者、Pepe/Killer 和仓鼠等少数分支获得战斗目标。
- 公牛旧版专用近战、击退、斗牛状态，母牛/母鸡/公鸡近战和受击反击没有等价实现。
- `configureRoosterCombat()` 只选择竞争目标，没有 leap/melee 执行目标；公鸡可能锁定对手却不能完成旧版攻击链。
- Farm 与 Extra 同装时，鸡捕食 frog/dartfrog/toad 的旧注入路径缺失。
- 山羊/羊顶头测试通过只证明该专用路径，不能覆盖上述缺口。

### F-GAME-004 鸡缺少侧向观察 AI

- 1.12 鸡使用 `EntityAIWatchClosestFromSide`。
- 当前 `AnimaniaWatchFromSideGoal` 只给孔雀类安装，鸡回退到正面观察目标。
- 修复门槛：鸡与孔雀分别验证侧向头部目标和目标结束复位，不能只检查 goal 类存在。

### F-GAME-005 猪骑乘玩法缺失

- 1.12 成年公猪/母猪可装鞍、骑乘、用胡萝卜钓竿控制并加速，鞍状态可保存。
- 当前装鞍、乘客控制、`setSaddled()` 和 `boost()` 都仅允许 horse，猪只把胡萝卜钓竿当诱惑食物。
- 现有测试没有覆盖猪装鞍、上乘、转向、加速、下乘与 NBT。

### F-GAME-006 雕刻刀没有绝育交互

- 1.12 交互事件中，雕刻刀作用于 `ISterilizable`，播放反馈、损耗耐久并调用 `sterilize()`。
- 当前 `FarmCarvingKnifeItem` 只有耐久和合成剩余物语义，没有实体交互或等价事件入口。
- 现有特殊物品测试只断言物品类型/耐久/配方，不证明绝育。

### F-GAME-007 挤奶状态机与产物不等价并可重复取奶

- 1.12 牛、母山羊和母羊必须成年、已喂食、已饮水且处于 `HasKids` 泌乳期；挤奶后会清除 watered，必须再次饮水。
- 当前 `isMilkable()` 不检查 fed/watered，取奶后不消耗饮水/泌乳状态，可连续用空桶重复获取。
- 当前错误地允许 mare 产 vanilla milk；1.12 母马没有该挤奶交互。
- 1.12 Mooshroom 母牛用碗取得 mushroom stew 并清除 watered；当前没有该分支。
- 1.12 名为 `purp` 的特定牛产熔岩，并有对应抗火/着火表现；当前没有 Purp 状态与产物逻辑。
- `cowsMilkableAtSpawn` 当前还在每个服务端 tick 强制重设 milk-ready，旧版仅在构造时初始化 `HasKids`。

### F-GAME-008 Mooshroom 剪切转换缺失

- 1.12 成年 Mooshroom 公/母牛被剪切后分别转换为同性别 Friesian，保留生命、名称和朝向，掉落 5 个红蘑菇并损耗剪刀。
- 当前没有 mooshroom/shears 交互路径。
- 现有所谓 transactional conversion 测试覆盖的是命令 UUID 失败路径，不是 Mooshroom 剪切。

### F-GAME-009 猪被雷击后的转换缺失

- 1.12 猪被雷击后转换为 zombie pigman，并保留幼体、名称和 NoAI 状态。
- 当前 Farm 猪没有 `thunderHit` 等价实现。
- 现代目标应为 zombified piglin，并要求新实体成功加入后才移除原实体。

### F-GAME-010 vanilla 家畜替换不按旧生成语义且非事务性

- 当前在 `EntityJoinLevelEvent` 替换，随机从全部品种选取，不按旧配置的 biome type；命名实体和非自然加入路径也可能被替换。
- 1.12 在 CheckSpawn 自然生成路径按生物群系和配置选品种，并保留不应被替换的命名 vanilla 实体。
- 当前不检查 `addFreshEntity(replacement)` 返回值便取消原事件，插入失败时有丢实体风险。
- 现有测试只覆盖一次成功的 cow 替换，没覆盖 biome、命名、非自然生成和失败保留。

### F-GAME-011 蜂巢产蜜、蜇伤与世界生成规则偏离

- 1.12 仅在 `hiveValidBiomeTypes` 中产蜜；每次 25 mB，周期重置为配置值加 0..99 随机量。
- 当前蜂巢忽略有效生物群系，使用固定周期且没有随机量。
- 旧 wild hive 的蜇伤检查是持续 tick 概率；当前只在产蜜周期触发后再做 1/40 判定，实际频率大幅降低。
- 当前 wild hive 世界放置只看频率和表面，不按 `hiveValidBiomeTypes` 过滤。
- 现有测试只证明流体量、能力、保存和 `sting()` 的单次伤害，不证明真实调度或世界生成。

### F-GAME-012 母鸡进巢和产蛋门控不完整

- 1.12 寻巢/产蛋要求白天、清醒、已喂食、已饮水，并检查巢容量和蛋/品种兼容；初始计时为配置的一半加随机量，后续才重置完整计时。
- 当前 `tryLayFarmEgg()` 只检查成年雌性与计时器，能在饥饿、口渴、睡眠或夜间直接向附近巢插蛋；初始计时也是完整延迟。
- 配置关闭世界散落蛋但仍允许向巢产蛋这一点与旧版一致，不列为缺陷。

### F-GAME-013 公鸡啼鸣只剩声音，缺少同步动画状态

- 当前保存 `CrowTime` 并播放声音，但没有旧版 `CrowDuration=50` 的同步/保存状态。
- 旧模型依赖该状态驱动啼鸣姿态，因此它既是玩法状态缺失，也是后续客户端回归的前置缺陷。

### F-GAME-014 奶酪模具加入了错误的瓶装奶捷径并改变交互

- 1.12 模具只接收五种 Animania 奶流体或水，成熟后输出对应完整 cheese wheel 或配置数量的盐。
- 当前接受普通 `milk_bottle`，直接加工成 Friesian cheese wedge；这是旧版不存在的配方和错误产物。
- 1.12 非潜行右键直接取成品，潜行右键显示进度；当前回退到通用容器界面，交互语义不同。
- 现有 GameTest 反而把“milk bottle 变 Friesian wedge”断言为正确行为，该断言必须更正，不能作为闭合证据。

## 可闭合候选：仅限已绑定的具体 requirement

以下能力已有真实 GameTest 或单元测试。中央闭合器仍需检查旧文件哈希、目标/测试指纹和逐 requirement 所有权；任何一项都不能用于关闭同一类的其他行为。

- 99 个动物 ID 可注册、可构造，并能保存/恢复当前通用字段；这不证明旧属性、尺寸、AI、交互或掉落等价。
- 6 个幼体家族的父母到成年 ID 映射，以及全部已注册幼体的实际成长转换；这不证明成长碰撞尺寸和全部保留字段。
- 通用找食、找水、跟随父母、睡眠、交配、吃草、漫游/避水，以及盐舔目标的已断言路径。
- 山羊/羊竞争顶头、马匹昼夜/骑手/牵引门控、猪泥地休息/拱食/诱惑的专用目标。
- 槽、巢、蜂巢、奶酪模具、羊毛和奶酪块在测试明确断言的容量、状态、能力、持久化、掉落和形状路径；蜂巢调度与奶酪模具错误交互除外。
- 五种奶流体和蜂蜜的注册、旧流动阻力/再生规则、奶桶转换配方、slop 接受 addon 奶桶。
- cart、chest cart、wagon、tiller 的注册、生成、库存/菜单、保存、牵引/脱离、掉落规则，以及 tiller 三行耕作和种子消耗。
- milk bottle 的饮用/玻璃瓶/清效果、honey jar/bottle 的流体能力、brown egg 投掷、cheese wheel 放置、truffle soup，以及 riding crop 对当前马/车辆的已断言路径；雕刻刀绝育与猪骑乘除外。
- 旧声音 ID、七种羊毛状态和已逐 ID 执行的资源/标签确定性检查。

## 仍需客户端或真实集成验证

这些项目当前没有足够证据，不能因用户暂时没看到新视觉问题或 GameTest 绿灯而关闭：

- 99 个动物逐品种的公/母/幼体模型、纹理、阴影和尺寸；睡眠、进食、泥污、剪毛、怀孕、啼鸣、骑乘和牵引状态。
- 公鸡 `CrowDuration` 修复后的动画触发与双客户端同步；Mooshroom 剪切、Purp 牛和泌乳状态的客户端刷新。
- cart/chest cart/wagon/tiller 的乘客位置、牵引线、旋转、碰撞和区块卸载/重载。
- 蜂巢、奶酪模具、巢、羊毛、奶酪和全部物品在方块 atlas、手持、掉落物和 GUI 中的模型/贴图。
- Farm-only、Base+Farm、全 addon 组合的客户端和专服启动；缺 Base 的明确依赖错误。
- JEI、Jade、TOP 固定版本分别安装及同时安装时的真实回调。当前 GameTest 只验证共享 probe 文本，不能代替兼容模组加载。
- 双客户端挤奶竞争、产蛋/巢写入竞争、骑猪/骑马、牵引车辆、容器同步、断线、维度切换和区块卸载。
- `hiveValidBiomeTypes` 修复后的真实世界生成统计，以及持续运行中的 wild hive 蜇伤概率。

## 现有绿灯不能证明的内容

- `all_legacy_animals_construct_persist` 只证明当前类型可创建且当前字段可保存，不证明 1.12 属性、尺寸、AI、交互和掉落。
- `farmLactationAndEggLayStatePersists` 只证明手工设置的字段能保存，不证明真实挤奶消耗、泌乳结束或产蛋门控。
- `farmSpecialItemsRetainLegacyUseSemantics` 没有对雕刻刀执行实体绝育，也没有测试猪骑乘。
- `wildHiveStingUsesLegacyDamageTypeAndAmount` 直接调用一次伤害函数，不覆盖服务器 tick 概率、生物群系或世界生成。
- `cheeseMoldAcceptsModernMilkFluid` 能证明现代流体能力路径，但同一测试组里把旧版不存在的 milk-bottle 捷径当成正确行为；错误断言必须删除或反转。
- 60/60 只表示已执行的 60 个选择器通过，不表示未写断言的 1.12 行为已经迁移。

## 修复与补测顺序

1. 先建立逐 ID 属性/尺寸表并参数化测试，避免继续用通用值掩盖 99 个实体的系统性错误。
2. 修复挤奶复制风险、vanilla 替换事务性、Mooshroom 剪切和猪雷击转换。
3. 恢复猪骑乘、雕刻刀绝育、家畜战斗/鸡侧向观察和跨 addon 捕食。
4. 修复产蛋门控、公鸡啼鸣状态、蜂巢周期/生物群系/蜇伤和奶酪模具交互。
5. 对每项新增专用或逐 ID GameTest；随后执行 Farm-only/组合启动、客户端截图和双客户端测试。
6. 只让中央闭合器按具体 requirement 应用新证据，不允许按 `AnimaniaAnimalEntity`、整个 Farm 模块或“60/60”批量闭合。
