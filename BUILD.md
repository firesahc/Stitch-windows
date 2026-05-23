# Stitch - 构建指南

## 环境要求

- **JDK 17+**（需要包含 `jpackage` 工具）
- **Gradle**（使用项目自带的 `gradlew` 包装器）

## 构建产物

| 产物 | 文件 | 说明 |
|------|------|------|
| **便携版 ZIP** | `build/distributions/Stitch-1.0.0-portable.zip` | 解压后双击 `Stitch.exe` 即可运行，自带 JRE |
| **源码发行包** | `build/distributions/Stitch-1.0.0.zip` | Gradle application 插件标准包（`Stitch.bat` 启动） |

---

## 构建命令

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

### 3. 一键构建（全部命令）

```bash
# 1. 编译
.\gradlew clean installDist

# 2. jpackage 生成 app image
& "C:\Program Files\Java\jdk-17\bin\jpackage.exe" `
    --type app-image `
    --name Stitch `
    --input "build\install\Stitch\lib" `
    --main-jar Stitch-1.0.0.jar `
    --main-class soko.ekibun.stitch.AppKt `
    --dest "build\jpackage"

# 3. 打包 ZIP
New-Item -ItemType Directory -Path "build\distributions" -Force | Out-Null
Compress-Archive -Path "build\jpackage\Stitch\*" `
    -DestinationPath "build\distributions\Stitch-1.0.0-portable.zip" `
    -CompressionLevel Optimal -Force

Write-Output "完成！产物："
Get-ChildItem "build\distributions" | Select-Object Name, @{N='SizeMB';E={[math]::Round($_.Length/1MB,1)}}
```

---

## 产物使用说明

### 便携版 ZIP（推荐）
1. 解压 `Stitch-1.0.0-portable.zip`
2. 进入 `Stitch/` 目录
3. 双击 `Stitch.exe` 运行（无需安装 JRE）

---

## 注意事项

- `jpackage` 路径请根据实际 JDK 安装位置调整
- 如需生成 `--type exe` 安装包（真正的单个安装程序），需安装 [WiX Toolset](https://wixtoolset.org/) 或 [Inno Setup](https://jrsoftware.org/isinfo.php)
- 应用使用了 OpenCV 原生库（通过 OpenPnp），已包含在 jar 中
- 构建产物约 80MB（压缩后），解压后约 130MB（得益于自动剥离 OpenCV 非 Windows 原生库）
- **无控制台窗口**: EXE 启动时不会弹出命令行窗口（已移除 `--win-console`）
