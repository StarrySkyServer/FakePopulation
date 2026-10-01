# FakePopulation

Nukkit-MOT 服务器在线人数伪造插件。通过表单或命令维护一份"假玩家"名单，抬高服务器对外显示的在线人数，并把假玩家名同步到游戏内玩家列表（TAB），同时支持让 Tips、StarrySkyLink 等第三方插件也显示假人数。

## 功能特性

- 表单管理：图形化界面增删假玩家、启停、翻页查看名单
- 命令管理：`/fakepop` 全套子命令，控制台可用
- 双通道显示
  - 服务器查询（RakNet 广告串）：游戏内服务器列表的在线人数
  - 游戏内玩家列表（TAB / 暂停菜单）：广播假玩家名
- 第三方集成（均可开关，默认关闭）
  - Tips：覆盖 `{online}` / `{maxplayer}`
  - StarrySkyLink：QQ「在线」查询回复注入假玩家名
- 假玩家不占用真实玩家位：显示人数抬高，但玩家容量不变，正常玩家照常进服
- 命名校验遵循 Xbox 玩家代号规则，避免与真实玩家混淆

## 环境要求

- Nukkit-MOT 服务端
- Java 17+

## 安装

1. 构建（或下载）`FakePopulation-1.0.0.jar`
2. 放入服务端 `plugins/` 目录
3. 重启服务端，自动生成 `plugins/FakePopulation/config.yml`

## 快速开始

```
/fakepop ui                     # 玩家：打开管理表单
/fakepop add "Major Nelson"     # 添加假玩家（含空格请加引号）
/fakepop list                   # 查看名单
/fakepop on                     # 开启总开关
```

## 命令

命令：`/fakepop`，别名 `/fp`

| 子命令 | 说明 | 权限 |
| --- | --- | --- |
| （无参数） | 玩家打开表单，控制台等同 `status` | `fakepop.use` |
| `ui` | 打开管理表单 | `fakepop.admin` |
| `status` | 查看当前状态 | `fakepop.use` |
| `list` | 列出全部假玩家 | `fakepop.use` |
| `add <名字>` | 添加假玩家（含空格请加引号） | `fakepop.admin` |
| `remove <名字>` | 移除假玩家 | `fakepop.admin` |
| `enable <名字>` | 启用某个假玩家 | `fakepop.admin` |
| `disable <名字>` | 禁用某个假玩家 | `fakepop.admin` |
| `clear` | 清空名单 | `fakepop.admin` |
| `on` / `off` | 开启 / 关闭总开关 | `fakepop.admin` |
| `reload` | 重载配置 | `fakepop.admin` |

## 权限

| 权限 | 默认 | 说明 |
| --- | --- | --- |
| `fakepop.use` | true | 查看假人数状态 |
| `fakepop.admin` | op | 打开管理表单、增删改假玩家名单、重载配置 |

## 配置文件

路径：`plugins/FakePopulation/config.yml`

```yaml
enabled: true                   # 总开关，false 时完全回落到真实人数（名单保留不删）
max-fake-players: 200           # 假玩家数量上限
refresh-interval: 20            # 刷新广告串的间隔（tick，20 = 1 秒）
show-in-tab: true               # 把假玩家名广播到游戏内玩家列表（TAB）
reserved-names: []              # 保留名黑名单，禁止用作假玩家（忽略大小写）
fake-players: []                # 假玩家名单，元素为 {name, enabled}
starryskylink-integration: false
tips-integration: false
```

显示人数 = 真实在线人数 + 启用中的假玩家数量。

## 命名规则

假玩家名遵循 Xbox 现代玩家代号规则：

- 长度 3–15 个字符
- 允许：空格、撇号（`'`）、数字、大小写字母、拉丁补充字符、拉丁扩展 A、CJK（中日韩汉字）
- 不允许：下划线等保留字符
- 首尾不能有空格，也不能有连续空格
- 不能与在线玩家或已注册玩家重名

命令中输入带空格的名字请加引号，例如 `/fakepop add "Major Nelson"`；表单输入直接写即可。

## 第三方插件集成

两个集成都通过 `softdepend` 软依赖，**不会强制加载**对应插件：插件不在场或开关关闭时自动跳过，不影响服务端启动。

### Tips

1. 在 `plugins/FakePopulation/config.yml` 设置 `tips-integration: true`
2. **无需改动 Tips 配置**——插件会直接覆盖 Tips 内置的 `{online}` 和 `{maxplayer}`

效果：真实 3 人 + 3 个假玩家时，`{online}` 显示 6，`{maxplayer}` 显示「真实上限 + 3」（两边各加假人数）。

### StarrySkyLink

1. 在 `plugins/FakePopulation/config.yml` 设置 `starryskylink-integration: true`
2. 在 `plugins/StarrySkyLink/config.yml` 的 `QueryServerMessage` 里加入令牌：

```yaml
# 真人前、假人后
QueryServerMessage: '[星空服] 当前在线玩家数：%playerNum% %players% %fakeplayers%'
# 想反过来（假人前、真人后），只需调换令牌位置
```

| 令牌 | 内容 |
| --- | --- |
| `%fakeplayers%` | 假玩家名列表 |
| `%fakepopnum%` | 含假玩家的人数（真实 + 假人） |

`%players%` / `%playerNum%` 是 StarrySkyLink 内置的真实数据，保持不变。

> 原理：StarrySkyLink 没有占位符注册接口，无法"主动调用"外部变量，因此由本插件读取它的 `QueryServerMessage` 模板、替换令牌后写回其公开静态字段，每次刷新重算。假玩家名只能前 / 后追加，无法插入 `%players%` 中间。

## 工作原理

- **服务器查询人数**：监听 `QueryRegenerateEvent`，把 `playerCount` 改为「真实 + 假人」，`maxPlayerCount` 保持服务端真实容量（假玩家不占玩家位）。
- **游戏内玩家列表**：通过 `PlayerListPacket` 向客户端广播假玩家条目，使其出现在 TAB / 暂停菜单。未进服时看到的服务器列表条目协议本身不含玩家名，只能显示人数。
- **第三方插件**：按各插件可用的接口逐个适配——Tips 走公开变量 API，StarrySkyLink 走公开静态字段注入。**不采用伪造 `Player` 对象注入服务端的方式**，避免遍历、广播、关服存档等路径崩溃。

## 构建

```bash
mvn clean package
```

产物：`target/FakePopulation-1.0.0.jar`