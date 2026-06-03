# Go2Go 🌍

[![Android Version](https://img.shields.io/badge/Android-7.0%2B-green.svg)](https://developer.android.com)
[![Version](https://img.shields.io/badge/Version-1.0.1-blue.svg)](https://github.com/gongjiantao/Go2Go/releases)
[![License](https://img.shields.io/badge/License-MIT-orange.svg)](LICENSE)

**Go2Go** 是一款基于百度地图 SDK 开发的 Android 位置模拟与足迹记录工具。它不仅支持实时位置模拟，还提供了自动路径规划、摇杆控制等高级功能，旨在为开发者调试、位置研究提供丝滑且高效的体验。

> **📦 项目来源**：本项目 fork 自 [ZCShou/GoGoGo](https://github.com/ZCShou/GoGoGo)，在其基础上进行了大量 UI 重构、功能修复及体验优化。感谢原作者的杰出贡献！

---

## ✨ 核心特性

- 📍 **精准位置模拟**：支持在地图上任意点选位置，并实时模拟设备 GPS。
- 🔄 **自动挡 (Auto Routing)**：点击地图添加多个途经点，应用将自动按顺序模拟行进路径。
- 🎮 **悬浮摇杆控制**：提供屏幕悬浮摇杆，随时随地精准控制模拟移动的方向和速度。
- 🔍 **智能搜索**：集成百度地图搜索，快速定位到街道、建筑或具体坐标。
- 📜 **足迹记录**：自动保存历史模拟位置，支持一键找回和快速跳转。
- 🎨 **现代化 UI 设计**：
  - 采用 **Material Design** 设计规范。
  - 拥有丝滑的入场动画与呼吸感背景。
  - 卡通化圆角风格，视觉体验高级且统一。
- ⚡ **高性能优化**：极简的布局嵌套，低内存占用，运行流畅不卡顿。

---

## 📸 界面预览

| 欢迎页 | 主界面 | 自动模式 |
| :---: | :---: | :---: |
| ![Welcome](https://via.placeholder.com/200x400?text=Welcome+Screen) | ![Main](https://via.placeholder.com/200x400?text=Main+Map) | ![Auto](https://via.placeholder.com/200x400?text=Auto+Mode) |

---

## 🚀 快速开始

### 环境要求
- Android 7.0 (API Level 24) 及以上。
- 开启系统的 **"开发者选项"** -> **"选择模拟位置信息应用"**，并选择 **"Go2Go"**。

### 安装与运行
1. **克隆仓库**：
   ```bash
   git clone https://github.com/gongjiantao/Go2Go.git
   ```
2. **配置百度地图 AK**：
   - 前往 [百度地图开放平台](https://lbsyun.baidu.com/) 申请 Android SDK 密钥。
   - 在 `AndroidManifest.xml` 中替换您的 AK。
3. **编译运行**：
   - 使用 Android Studio 打开项目。
   - 点击 `Run` 按钮部署到您的真机或模拟器。

---

## 🛠️ 技术架构

- **语言**：Java (JDK 11)
- **地图引擎**：Baidu Map SDK
- **核心组件**：
  - `CoordinatorLayout` + `AppBarLayout` 实现高级联动交互。
  - `Service` 后台运行模拟定位逻辑。
  - `SharedPreferences` 持久化用户配置。
  - `SQLite` 存储历史位置信息。
- **动画系统**：基于 `ViewPropertyAnimator` 实现的硬件加速动画。

---

## ⚠️ 免责声明

本软件仅供 **学习研究** 及 **开发调试** 使用，请勿用于任何非法用途！开发者不承担因用户违规使用而产生的任何直接或间接法律责任。使用本软件即表示您已阅读并同意 [服务条款](file:///d:/demo/Mo/gogogo/app/src/main/res/values/strings.xml)。

---

## 🤝 贡献与反馈

如果您发现了 Bug 或有更好的功能建议，欢迎提交 [Issue](https://github.com/gongjiantao/Go2Go/issues) 或 Pull Request。

- **联系作者**：微信搜索 `G46645426826` (点击应用内反馈可自动复制)
- **项目地址**：[https://github.com/gongjiantao/Go2Go](https://github.com/gongjiantao/Go2Go)
- **上游项目**：[ZCShou/GoGoGo](https://github.com/ZCShou/GoGoGo)

---

## 🔄 相比上游的主要改动

- 🎨 **全面 UI 重设计**：引入药丸按钮、圆角描边卡片、暖色系调色板、高级入场动画（Overshoot 弹性缩放 + 背景呼吸效果）及侧边栏现代化改造。
- 🐛 **问题修复**：修复暂停后恢复自动挡回到起点、部分设备 `bm.clear()` 导致地图跳至几内亚湾、首次启动模拟定位闪退等关键缺陷。
- 🧹 **代码与布局优化**：移除冗余代码、清理无用按钮、新增友好提示文案；合并布局层级以提升渲染性能。
- 📝 **完整文档**：重新编写 README，补充项目来源、免责声明与贡献说明。

---

⭐ 如果这个项目对你有帮助，请给它一个 Star！你的支持是开发者最大的动力。
