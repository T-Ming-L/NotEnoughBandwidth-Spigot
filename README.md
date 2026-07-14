# NotEnoughBandwidth-Spigot

> **AI 辅助开发** · 将 NeoForge Mod "[NotEnoughBandwidth](https://github.com/USS-Shenzhou/NotEnoughBandwidth)" 移植到 Spigot/Paper 的插件版本。

## ⚠️ 测试说明

由于原 Mod 的客户端和服务端功能高度耦合，移植后**没有对应的客户端 Mod 可用于联机测试**，因此本插件目前仅进行了以下验证：

- ✅ **服务端加载** — 插件在 Purpur 26.2 上可正常加载、启用/禁用
- ✅ **配置文件生成** — 首次运行自动生成带中文注释的 `config.json`
- ✅ **命令注册** — `/neb` `/neb stat` `/neb reload` 命令可正常执行
- ✅ **构建验证** — 使用 Gradle + paperweight 构建成功，生成 fat jar
- ❌ **实际联机数据包聚合测试** — 因缺少客户端 Mod 配合，**未经实际游戏流量验证**

欢迎有兴趣的贡献者协助完善和测试。

## 功能

- **数据包聚合** — 将多个小数据包合并为一个大数据包，减少网络开销
- **Zstd 压缩** — 使用 Zstandard 算法压缩聚合后的数据包
- **区块跟踪缓存** — 扩展玩家视距，减少区块加载/卸载抖动
- **数据包大小限制** — 可配置的最大数据包大小
- **带宽统计** — 实时显示带宽使用情况和压缩率

## 命令

| 命令          | 说明         |
| ------------- | ------------ |
| `/neb`        | 显示帮助信息 |
| `/neb stat`   | 显示带宽统计 |
| `/neb reload` | 重载配置文件 |

## 权限

- `neb.admin` — 允许使用所有 NEB 命令（默认: op）

## 配置

配置文件位于 `plugins/NotEnoughBandwidth-Spigot/config.json`，首次运行自动生成，包含中文注释说明。

主要配置项：

| 配置项                   | 默认值         | 说明                   |
| ------------------------ | -------------- | ---------------------- |
| `compatibleMode`         | `false`        | 兼容模式，保守压缩策略 |
| `contextLevel`           | `23`           | Zstd 压缩级别（21~25） |
| `dccSizeLimit`           | `60`           | DCC 触发大小（KB）     |
| `dccDistance`            | `5`            | DCC 字典窗口距离       |
| `dccTimeout`             | `60`           | DCC 字典缓存超时（秒） |
| `maxPacketSize`          | `"4MB"`        | 聚合包最大体积         |
| `debugLog`               | `false`        | 调试日志开关           |
| `blackList`              | _(见配置文件)_ | 不参与聚合的数据包类型 |
| `playersDoNotUseContext` | _(见配置文件)_ | 免 DCC 压缩的玩家 UUID |

## 构建

**前置要求**: Java 21+、Gradle

```bash
git clone https://github.com/T-Ming-L/NotEnoughBandwidth-Spigot.git
cd NotEnoughBandwidth-Spigot
./gradlew build
```

构建产物位于 `build/libs/NotEnoughBandwidth-Spigot-26.2-1.0.jar`

## 开源许可

本项目基于 **GNU General Public License v3.0 (GPL-3.0)** 开源。

```
Copyright © 2026 T-Ming-L & USS_Shenzhou

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.
```
