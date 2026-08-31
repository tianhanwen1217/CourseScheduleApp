# CourseScheduleApp

一款使用 Kotlin 和 Jetpack Compose 开发的 Android 课程表应用。应用可以通过内置网页打开教务系统，解析课表页面中的课程信息，并使用 Room 保存到本地。

## 功能

- 按周查看星期一至星期日的课程，支持左右滑动切换周次。
- 根据周次、单双周规则显示课程。
- 展示课程名称、上课地点、教师、节次和上课时间等信息。
- 在应用内登录教务系统并解析课表 HTML。
- 将导入结果保存到本地数据库，重新打开应用后仍可读取。
- 使用 Material 3 构建课程卡片、课程详情和导入页面。

## 技术栈

- Kotlin 2.0
- Jetpack Compose + Material 3
- Room 2.6
- Jsoup 1.18
- Android Gradle Plugin 8.7
- Min SDK 26 / Target SDK 35

## 运行项目

1. 使用 Android Studio 打开项目根目录。
2. 等待 Gradle 同步完成，并确认已安装 Android SDK 35。
3. 连接 Android 8.0 或更高版本的设备，或创建对应模拟器。
4. 运行 `app` 配置。

也可以在命令行构建 Debug APK：

```powershell
.\gradlew.bat assembleDebug
```

构建产物位于 `app/build/outputs/apk/debug/`。

## 导入课表

1. 点击首页右上角的导入按钮。
2. 在内置网页中登录教务系统并打开课表页面。
3. 等待应用识别页面中的课程表。
4. 解析成功后，课程会写入本地数据库并显示在课表中。

应用没有另外保存用户的登录账号或密码；登录过程由教务系统网页在 WebView 中完成。课表 HTML 在设备内解析。

## 项目结构

```text
app/src/main/java/com/hanwentian/courseschedule/
├── MainActivity.kt          # Compose 界面、课表导入和 HTML 解析
├── data/local/              # Room 实体、DAO、数据库和仓库
└── ui/theme/                # Compose 主题
```

## 当前限制

- 导入规则针对当前教务系统页面结构编写；页面改版后可能需要更新解析逻辑。
- 学期起始日期、课程节次和作息时间目前在代码中配置，尚未提供设置界面。
- 课程详情中的复制、删除和编辑目前只修改本次运行中的界面状态，尚未同步写入 Room。
- 仓库目前没有提供签名后的安装包，需要自行构建运行。

## 数据与隐私

- 应用仅在用户主动导入课表时访问教务系统网页。
- 解析后的课程数据保存在设备本地的 Room 数据库中。
- 请勿在 Issue、日志或截图中公开教务系统账号、Cookie 或个人课表信息。

## 许可证

本仓库暂未附加开源许可证。未经许可，请勿将代码用于再分发或商业用途。
