# Stitch - 构建指南

## 环境要求

- **JDK 17+**（需要包含 `jpackage` 工具）
- **Gradle**（使用项目自带的 `gradlew` 包装器）

## 构建产物

| 产物 | 文件 | 说明 |
|------|------|------|
| **便携版 ZIP** | `build/distributions/Stitch-1.0.0-portable.zip` | 解压后双击 `Stitch.exe` 即可运行，自带 JRE |
| **单文件安装包** | `build/distributions/Stitch-1.0.0.exe` | 双击安装（需 WiX 3.x 构建），自带 JRE |
| **源码发行包** | `build/distributions/Stitch-1.0.0.zip` | Gradle application 插件标准包（`Stitch.bat` 启动） |

---

## 一键构建（推荐）

```powershell
.\gradlew packagePortable   # 便携版 ZIP
.\gradlew packageInstaller  # 单文件安装包 .exe（需 WiX Toolset 3.x）
.\gradlew packageAll        # 两者一次构建
```

jpackage 路径优先级：`-PjpackageBin="..."` > 环境变量 `JPACKAGE_BIN` > 当前 JVM 自带。
所有打包任务超时均为 300 秒。

---

## 手动构建（备用）

> **注意**: `build.gradle.kts` 中包含一个 `stripOpenCvJar` 任务，会在 `installDist` 时自动剥离
> OpenCV jar 中非 Windows 的原生库（Linux、macOS、x86_32），大幅减小发行包体积。

### 1. 完整构建（Gradle 编译 + installDist）

```bash
.\gradlew clean installDist
```

### 2. 生成便携版 ZIP（含 EXE + 内置 JRE）

```bash
# 创建 app image
& "C:\Program Files\Java\jdk-17\bin\jpackage.exe" `
    --type app-image `
    --name Stitch `
    --input "build\install\Stitch\lib" `
    --main-jar Stitch-1.0.0.jar `
    --main-class soko.ekibun.stitch.AppKt `
    --dest "build\jpackage"

# 打包 ZIP
Compress-Archive -Path "build\jpackage\Stitch\*" `
    -DestinationPath "build\distributions\Stitch-1.0.0-portable.zip" `
    -CompressionLevel Optimal -Force
```

### 3. 生成单文件安装包（需 WiX Toolset 3.x）

```powershell
.\gradlew clean installDist

& "C:\Program Files\Java\jdk-17\bin\jpackage.exe" `
    --type exe `
    --name Stitch `
    --app-version 1.0.0 `
    --input "build\install\Stitch\lib" `
    --main-jar Stitch-1.0.0.jar `
    --main-class soko.ekibun.stitch.AppKt `
    --dest "build\distributions" `
    --win-dir-chooser --win-menu --win-shortcut
```

产物：`build\distributions\Stitch-1.0.0.exe`。

---

## 产物使用说明

### 便携版 ZIP（推荐）
1. 解压 `Stitch-1.0.0-portable.zip`
2. 进入 `Stitch/` 目录
3. 双击 `Stitch.exe` 运行（无需安装 JRE）

### 单文件安装包
1. 双击 `Stitch-1.0.0.exe` 按向导安装（可选安装目录）
2. 从开始菜单/桌面快捷方式启动（无需安装 JRE）

---

## 注意事项

- 一键任务会自动定位 `jpackage`（可用 `-PjpackageBin` / `JPACKAGE_BIN` 覆盖）；手动命令中的 `jpackage` 路径请按实际 JDK 安装位置调整
- `--type exe` 单文件安装包必须安装 [WiX Toolset 3.x](https://wixtoolset.org/releases/)（JDK 17 的 `jpackage` 只认 3.x，不认 Inno Setup）
- 应用使用了 OpenCV 原生库（通过 OpenPnp），已包含在 jar 中
- 构建产物约 80MB（压缩后），解压后约 130MB（得益于自动剥离 OpenCV 非 Windows 原生库）
- **无控制台窗口**: EXE 启动时不会弹出命令行窗口（已移除 `--win-console`）
