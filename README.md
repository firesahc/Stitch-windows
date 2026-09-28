# Stitch

Screenshot stitch helper

1. Tap Import/Screenshot to add images.
2. In the screenshot mode, tap the float button to add screenshots, long press the button to open the editor.
3. Tap the circle to select images to modify the stitching parameters, or drag the circle to move the images.
4. Automatic stitching is powered by OpenCV, which can be used to stitch maps. Please ensure there is enough overlapping areas.
5. Stitched data will be cleared after restart or tapping the quick button. Please export it as soon as you finish editing it.

截图拼接小工具

1. 点击导入/截图添加图片

2. 截图模式下，点击悬浮按钮添加屏幕截图，长按按钮打开编辑窗口

3. 点击圆圈选择图片修改拼接参数，或拖动圆圈移动位置

4. 自动拼接采用opencv计算，可以用于拼接地图，请保持足够的重合区域

5. 拼接数据将在软件重启或点击快捷按钮后清除，编辑完成后请尽快导出

Enjoy~

---

## 运行方式

| 分发形式 | 文件 | 用法 |
|------|------|------|
| 便携版 ZIP | `Stitch-1.0.0-portable.zip` | 解压后双击 `Stitch/Stitch.exe`，自带 JRE，无需安装 |
| 单文件安装包 | `Stitch-1.0.0.exe` | 双击安装（开始菜单/桌面快捷方式），适合分发单个文件 |

## 构建分发包（便携版 / 单文件安装包）

> 说明：`jpackage --type app-image` 只能产出**文件夹**（再打成 ZIP 分发），
> 原生单文件分发只有 `--type exe`（安装包）一条官方路径，且 Windows 下需要 WiX Toolset。
> 免安装的单文件 portable exe（双击直接运行、无需解压/安装）`jpackage` 原生不支持，
> 需用 7z SFX / NSIS 把 app-image 再包一层，体积大且易被杀软误报，一般不推荐。

前置要求：

- JDK 17+（自带 `jpackage.exe`；可用 `-PjpackageBin="..."` 或环境变量 `JPACKAGE_BIN` 指定路径）
- 单文件安装包额外需要 [WiX Toolset 3.x](https://wixtoolset.org/releases/)（`candle.exe` / `light.exe` 需在 `PATH` 中，JDK 17 的 `jpackage` 只认 3.x）

```powershell
.\gradlew packagePortable   # 便携版：build/distributions/Stitch-1.0.0-portable.zip（解压双击 Stitch.exe 即用）
.\gradlew packageInstaller  # 单文件安装包：build/distributions/Stitch-1.0.0.exe（需 WiX）
.\gradlew packageAll        # 两者一次构建
```

产物均自带 JRE，无需预装 Java。详细构建说明（含手动 `jpackage` 命令）见 [BUILD.md](BUILD.md)。