# Animania Base 玩法审计（2026-08-12）

## 结论

Base 目前不能按玩法完成项闭合。工程、注册、资源和一批功能方块路径已有可重放证据，但 1.12 基线与 1.20.1 公共动物实现之间存在 12 组明确的运行语义偏差，其中包括一个随机刷怪蛋的间歇性崩溃路径、一个动物成长后的状态丢失路径，以及永久产奶和计时倍率错误。

本轮只做诊断和证据审计，没有修改玩法实现，也没有写入迁移矩阵的 `closed` 状态。

## 后续修复状态（提交前验证）

2026-08-12 的后续修复已覆盖 B-GAME-001 至 B-GAME-012，并为原先缺失的异常路径增加真实 Forge GameTest：任意药水拒绝、睡眠交互、容器返还、未交互 care 门控、虚弱效果、随机蛋完整候选集合、成长状态复制、妊娠配置与随机量、产后生育/泌乳、羊毛与羽毛计时、跌落伤害、巢自动化和失败转换保留原实体。携带状态增加登录、StartTracking、维度、重生和 Clone 同步路径。

修复后四模块单元测试与 `clean build` 通过；全模块 Forge GameTest 为 117/117。矩阵状态仍须由中央闭合器依据新测试指纹重新计算，本文不直接宣称修改 `closed`。

## 审计范围与证据

- 基线：只读 `upstream/Animania-1.12`；1.18 仅用于 API 迁移参考。
- Base 矩阵：360 条（192 Java、168 资源）。当前矩阵文本为 `closed=99`、`implemented_unverified=259`、`unstarted=2`；这些数字不是本轮重新认可的结论。
- 新证据快照：96 条具备可关闭候选证据；134 条仍要求客户端证据；130 条不能闭合（其中 128 条缺少非客户端要求证据，2 条缺少实现映射）。
- Base 单元测试：在隔离构建镜像强制重跑，118/118 通过，0 failure、0 error、0 skipped。
- Forge GameTest：Java 17.0.17、Forge 47.4.22，实际专服执行，全模块 113/113 required tests 通过。
- Base-only 专服：只装 Base 的 `:base:runServer` 实际启动到 `Done (14.507s)`；重跑时日志无 ERROR、Exception、缺失注册表或 missing mapping。
- Base GameTest 源内有 17 个测试方法；`apiContractLoads` 和 `serverAuthoritySmoke` 只证明加载/运行侧，不能关闭具体玩法行为。

## 完全无法关闭：需要实际修复

### B-GAME-001 饥渴状态机缺失旧版虚弱效果，并绕过“交互后启用 AI”门控

- 1.12 `GenericBehavior`：缺食且缺水持续施加 Weakness II；只缺一项施加 Weakness I。
- 当前 `AnimaniaAnimalEntity.tick()` 没有任何 Weakness 效果。
- 当前标量 hunger/thirst 周期扣减不受 `REQUIRE_ANIMAL_INTERACTION_FOR_AI` 的 `careTimersActive` 门控，未与玩家交互的自然动物也会逐步变饿、变渴。
- 影响矩阵条目：`8be5537b36fe6dbbd79b1fd2`（旧 `GenericBehavior.java`）。

### B-GAME-002 饮水交互接受任意药水，且容器处理和睡眠门控错误

- 当前把所有 `Items.POTION` 当作饮水，治疗、剧毒等药水也会补水并被消耗；1.12 只接受实际装有 WATER 的流体容器。
- 水桶和 Animania 水瓶被缩减后没有可靠返还空桶/空瓶；普通药水分支只返还玻璃瓶。
- 1.12 在动物睡眠时拒绝喂食和饮水；当前喂食/饮水路径没有睡眠前置检查，仍会改变状态并消耗物品。
- 影响矩阵条目：`0c054eea6373428e57d64c3c`（旧 `AnimaniaHelper.java`）及 `8be5537b36fe6dbbd79b1fd2`。

### B-GAME-003 幼体替换为成年实体时丢失关键状态

- 当前 `growIntoAdultVariant()` 未复制自定义名称和 `interacted`。
- 1.12 明确复制名称和交互状态。
- 结果：命名动物长大后丢名；默认启用交互门控时，长大后的动物会重新进入“从未交互”状态。
- 当前实现还先 `discard()` 再 `addFreshEntity()`，创建失败时存在实体丢失窗口。
- 影响矩阵条目：`8be5537b36fe6dbbd79b1fd2`。

### B-GAME-004 妊娠配置和随机区间没有真正生效

- 当前优先返回 `SpeciesDefinition.gestationTicks`；各 addon 注册时普遍硬编码 20000，使 Base 的 `GESTATION_TICKS` 配置对这些动物无效。
- 1.12 普通动物为配置值加 `0..199` 随机量，马有更大的初始化随机范围；当前固定时长导致所有个体同步生产。
- 影响矩阵条目：`100bf2c27a3c927958a073b0`（旧 `IImpregnable.java`）及 `8be5537b36fe6dbbd79b1fd2`。

### B-GAME-005 产后生育冷却/干乳状态被简化成不等价的 vanilla 年龄冷却

- 当前生产后只执行 `setAge(6000)`，没有旧版 Fertile、DryTimer、HasKids 状态机。
- 1.12 产后设为不育，按 `gestationTimer / 9 + random(50)` 恢复；这些状态也用于挤奶和信息探针。
- 当前行为的数值、可见状态和再次繁殖条件均不等价。
- 影响矩阵条目：`100bf2c27a3c927958a073b0`、`8be5537b36fe6dbbd79b1fd2`。

### B-GAME-006 产奶状态不会自然结束

- 当前出生时 `setMilkReady(true)`，挤奶不会清除，生产代码中没有对应的自然 `setMilkReady(false)` 路径。
- 1.12 在对应幼体长成后定位母体并清除 `HasKids`，从而结束产奶期。
- 结果：母体可永久重复产奶，属于玩法和资源复制语义错误。

### B-GAME-007 羊毛再生计时慢 20 倍且丢失随机量

- 当前 `woolRegrowthTicks` 每 20 tick 才减 1，但初值仍是旧版以 tick 为单位的 8000。
- 默认实际等待约 160000 tick；1.12 是 8000..8499 tick，并且每 tick 递减。
- 影响所有可剪毛羊和安哥拉山羊。

### B-GAME-008 羽毛掉落计时与物种规则不等价

- 当前用实体 `tickCount % FEATHER_TIMER`，所有同龄动物同步掉落，且计时不持久化、没有鸡的 `random(1000)` 分散。
- 当前同时允许 `peacock_` 和 `peahen_` 掉孔雀羽毛；1.12 的该掉落逻辑只在公孔雀基类。
- 这是 Base 公共生产逻辑错误，实际影响 Farm 鸡和 Extra 孔雀。

### B-GAME-009 随机实体蛋包含非动物实体，存在间歇性 ClassCastException

- `AnimaniaItems.allAnimalTypes()` 只排除了车辆和 `item_` 前缀，然后无检查强制转换为 `EntityType<? extends AnimaniaAnimalEntity>`。
- Farm 的 `brown_egg_projectile` 会进入候选列表；随机选中后创建结果不是 `AnimaniaAnimalEntity`，强转会崩溃。
- 现有 GameTest 只随机执行一次，因此通过不代表候选集合安全。

### B-GAME-010 跌落伤害减免作用范围和公式错误

- 1.12 只在动物被拴绳时将最终 fall damage 乘配置值。
- 当前对所有 Animania 动物无条件缩放 `fallDistance`；未拴绳动物也获减伤，且缩放阈值前距离与缩放最终伤害在数学上不等价。
- 影响矩阵条目：`4ac673e13616e508f653b4eb`（旧 `EntityEventHandler.java`）。

### B-GAME-011 巢的自动化语义偏离且可能丢失鸟种信息

- 1.12 `ItemHandlerNest.insertItem()` 明确拒绝一切自动化插入。
- 当前 capability 接受 vanilla egg 或注册路径包含 `egg` 的任意物品，既可能误收无关物品，也不会通过 `insertEgg()` 写入 `birdVariant`。
- 现有测试只验证 DIRT 被拒绝和直接 `insertEgg()` 保存，未验证 capability 对蛋的旧版拒绝语义及鸟种一致性。
- 影响矩阵条目：`eccc4ba4e33a9c3055866a06`。

### B-GAME-012 `/animania tovanilla` 有实体丢失窗口

- 当前先 `entity.discard()`，后 `level.addFreshEntity(replacement)`，并且无添加失败检查。
- 若目标实体因事件、UUID 冲突或其他原因添加失败，原实体已经永久删除，命令仍计数成功。
- 影响矩阵条目：`9fc27a2b1830216d46579861`。

## 需客户端或联机验证

### B-NET-001 携带动物的迟加入、维度切换和重生同步

- 目前只在携带状态变化及玩家登录时广播该玩家自己的状态。
- 未发现 `StartTracking`、`PlayerChangedDimensionEvent` 或 `PlayerEvent.Clone` 的补发/复制路径。
- 新加入客户端不能保证收到已在线玩家的携带动物状态；维度切换、死亡重生和断线重连必须用双客户端验证。
- 该项在补齐同步事件前既是静态集成缺口，也保留实际双客户端验证要求。

### B-CLIENT-AGGREGATE

- 134 个 Base 矩阵条目仍声明 `client` 要求。用户当前未发现新的模型/贴图问题不等于这些要求自动通过。
- 后续应按具体 ID 运行方块 atlas、物品模型、手册、携带层、蛋着色和设施 BER 的客户端诊断；不能用同一张启动截图批量关闭。

### Extra 旁注：仓鼠飞轮方向

- 1.12 CraftStudio 轮轴关键帧是负 Z 方向；当前轮子也是每 tick 约 `-4.5°`，仓鼠 Y 轴旋转也沿用了旧版 `-90°`。
- 因此仅从符号对照，当前方向与旧源码一致，不能直接判定为移植时反号。
- 但当前轮内仓鼠是静态实体模型，旧动画关系是否产生“仓鼠朝向与轮滚方向相反”的视觉问题，需要 Extra 阶段逐帧客户端验证；本轮不计入 Base 闭合。

## 可关闭候选（仅限已绑定要求）

以下能力已有较强的真实 GameTest 或单元测试证据，可以在中央闭合器确认哈希和 requirement 绑定后关闭对应要求，而不是关闭整个公共类：

- 槽：双方块控制器/清理、3 份食物容量、1000 mB 流体容量、食物/流体互斥、比较器、雨水收集、item/fluid capability 持久化。
- 盐舔块：治疗条件、使用次数和耐久/破坏规则。
- 泥：形状、摩擦、声音和移动阻尼。
- 种子/稻草堆：支撑和形状规则、配置控制的发射器放置。
- Base 注册：旧声音 ID、旧 ore dictionary 到现代 tags 的映射、手册和旧拼写物品的注册类型。
- 配方/配置：slop 配方的配置和桶语义、trough food 的现代注册表匹配、默认配置值的静态一致性。
- 存档/API 的纯数据合同：已有单元测试覆盖的 `AnimalSnapshot`、枚举、tag/API 查找和明确 NBT 字段。
- 独立加载：Base-only 专服启动已通过；这只能关闭“Base 独立加载”集成要求，不能关闭上述动物玩法。

## 无效或不足以闭合的现有证据

- `apiContractLoads` 只能证明类可加载，不能证明 API 的状态语义。
- `serverAuthoritySmoke` 只断言 GameTest 位于服务端世界，不能证明交互和世界修改均为服务端权威。
- 一次随机蛋成功不能证明候选集合全部是动物。
- `farmLactationAndEggLayStatePersists` 手工调用 `setMilkReady(false)` 只证明字段可保存，不能证明玩法会自然结束泌乳。
- `legacyCareTimersResetExpireAndSurviveReload` 不覆盖 Weakness、未交互标量扣减、任意药水或睡眠交互。
- `childGrowsIntoAdultRegistryType` 不覆盖名称、`interacted` 和替换失败时的数据安全。
- 汇总的 113/113 只表示所执行选择器通过，不代表未断言的 1.12 行为已迁移。

## 下一步修复顺序

1. 修复 B-GAME-009 随机蛋崩溃和 B-GAME-003/B-GAME-012 实体丢失路径。
2. 修复饮水/睡眠/容器和 care 状态机；补 Weakness 与交互门控测试。
3. 恢复妊娠配置、产后生育/泌乳状态机及完整 NBT/API 状态。
4. 修复羊毛、羽毛计时与物种限制。
5. 修复跌落伤害和巢自动化语义。
6. 增加双客户端携带同步测试，再对通过项运行中央闭合器。
