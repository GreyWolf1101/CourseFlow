# 课序 CourseFlow

“课序”是一款原生 Android 周课程表应用，使用 Kotlin 与 Jetpack Compose 编写。

## 已实现

- 周一至周日七列同屏显示，首页以紧凑信息栏提示当前周与当天日期
- 点击首页周次可选择任意周，在课表区域通过跟手分页动画切换相邻周
- 星期标题下显示完整月/日日期，例如 `9/21`
- 单周、双周、每周课程规则，以及第 1—30 周学期配置
- 课程详情：教室、任课教师、时间、周数、备注
- 手动新增与编辑课程；可调整课程名称、连续节数，并从 12 种协调配色中自由选择
- 第一周日期使用日历选择，并自动对齐到所选日期所在周的周一
- 默认提供 13 节课程时段至 23:00；每一节的开始时间和时长仍可单独修改
- 导入课表、手动添加、学期信息与上课时间统一集中在设置页
- 相同课程的相邻节次自动合并为连续卡片
- 导入 DOCX、XLSX、PDF、CSV、TXT；PDF 使用内置中文 ML Kit 模型离线 OCR
- 导入预览、覆盖导入与追加导入、本地持久化
- 设置页检查 GitHub Release 更新，支持应用内下载、SHA-256 校验和系统安装器升级

旧版二进制 `.doc` / `.xls` 需要先在 Office 或 WPS 中另存为 `.docx` / `.xlsx`。不同学校的课表模板差异很大，导入器采用“表头 + 节次 + 元数据”的容错解析；无法确定的内容不会静默写入，而会在预览页显示提醒。

## 构建

使用 Android Studio（JDK 17、Android SDK 35）打开项目，或运行：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

调试 APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。

## 正式发布与更新

正式版使用专用证书签名。证书不进入 Git 仓库，本机默认从
`%USERPROFILE%/.courseflow-signing/signing.properties` 读取。

GitHub Actions 已配置为在推送 `v*` 标签时自动测试、签名并创建 Release。仓库需要设置：

- `KEXU_KEYSTORE_BASE64`
- `KEXU_STORE_PASSWORD`
- `KEXU_KEY_ALIAS`
- `KEXU_KEY_PASSWORD`

发布新版本前同时增加 `versionCode` 与 `versionName`，提交代码后推送对应标签，例如 `v1.1.0`。旧版本随后可在设置页直接发现并下载安装新版本。
