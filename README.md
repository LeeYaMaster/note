# 笔记

一个扁平风格的 Android 笔记演示应用，用 Jetpack Compose 实现。视觉上参考小米笔记的信息结构（列表、瀑布流、编辑页），但不用 Material 3 的默认主题、投影和色调。

## 环境

- Android Studio（当前稳定版即可）
- JDK 17 或以上
- Android SDK 35

## 打开并运行

1. 启动 Android Studio，选择 **Open**，打开本仓库根目录（包含 `settings.gradle.kts` 的那一层）。
2. 等待 Gradle Sync 完成。如果提示缺少 SDK，按提示安装 Android 35 平台。
3. 连接手机（开启 USB 调试）或启动一个 API 26 以上的模拟器。
4. 选择运行配置 `app`，点击 Run。

命令行构建：

```bash
export ANDROID_HOME="$HOME/Android/Sdk"   # 按本机 SDK 路径修改
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

## 可以试什么

- 首页右上角 **更多** 菜单里切换 **列表视图** 和 **瀑布流视图**。选择会记在本机，下次打开仍然保留。
- 长按一条笔记进入多选，再点其他笔记可继续勾选，顶栏 **删除** 会一起删掉。
- 点搜索可按标题和正文过滤。
- 点一条笔记进入编辑；右下角黄色 **+** 新建笔记。空白笔记返回时会丢弃。
- **清单**：插入可勾选的一行。点左边的方框会打上绿色 ✅，再点一次取消。
- **图片**：从相册选一张图插进笔记。
- **涂鸦**：打开画板，可用笔、橡皮和撤销，完成后插进笔记。
- **格式**：对当前这一行设置左对齐、居中或右对齐，不是全局字号。
- 顶栏可以分享或删除当前笔记。

示例数据只存在内存里，进程结束后会恢复成初始笔记。

## 目录

```
app/src/main/java/com/flatnotes
├── MainActivity.kt          窗口与入口
├── data/                    笔记模型与示例数据
├── domain/                  日期分组、时间文案
└── ui/
    ├── theme/               扁平配色（无 MaterialTheme）
    ├── components/          分割线、菜单、悬浮按钮
    ├── icons/               线性图标
    ├── notes/               列表与瀑布流
    └── editor/              编辑页
```

配色：背景白色，强调色 `#FFC107`，分割线 `#EEEEEE`，圆角 8dp。卡片和按钮都没有投影。
