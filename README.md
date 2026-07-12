# 线下凑单app

线下凑单app 是一个用于计算服装购物凑单方案和优惠券最优组合的 Android App。用户可以录入商品和优惠券规则，App 会根据满减门槛、单独结账分组、叠加规则、凑单小物等条件，计算更省钱的购买方案。

## 功能特性

- 商品管理：添加、编辑、删除商品，支持标记“必买”商品。
- 优惠券管理：支持满减券、折扣券、无门槛券。
- 优惠券规则：支持可叠加券、单独使用券、启用/停用和使用次数限制。
- 最优方案计算：自动组合商品分组，计算优惠后实付款。
- 凑单小物：可手动开启，开启后才允许小物或差价小物参与满减分组。
- 门槛提示：商品价格接近优惠券门槛时会标红或标黄，分组结果中同步展示颜色。
- 商品筛选：计算页可选择参与计算的商品，必买商品始终保留。
- 方案展示：按单独结账分组展示商品、凑单小物、优惠券和本组应付。
- 多用户凑单：商品归属用户，计算页按用户分框，每框并列「单独算」与「合并凑单分摊后」两口径；合并方案跨用户组合，优惠按各人原价占比分摊。

## 价格提示规则

- 距离最近满减门槛 `<= 20` 元：橙色提示（`AppAmber`）。
- 距离最近满减门槛 `<= 40` 元：绿色提示（`AppGreen`）。
- 其他商品：普通黑色文字。
- 必买商品（`isRequired`）在计算页名称/价格强制红色（`AppRed`），优先级最高，不受门槛色影响。

## 凑单小物规则

计算页右侧有一个浮动按钮：

- `衣`：只用衣服本身计算，不允许凑单小物参与。
- `凑`：允许凑单小物参与计算。

当开启凑单小物后，算法会比较：

- 衣服自身组合能否达到优惠券门槛；
- 单件或小组合衣服加差价小物后是否更划算；
- 优惠金额扣除凑单小物价格后的净收益。

只有凑单小物带来的净收益更高时，才会采用该方案。

## 多用户凑单

- **用户管理**：商品页左上角按钮打开侧拉抽屉，可添加/重命名/删除用户（默认用户「本人」不可删），勾选决定哪些用户参与计算，勾选状态长期留存。
- **按用户分框**：商品页与计算页均按勾选用户分框展示；每人商品可单独清空。
- **两口径对比**：计算页每用户框展示「单独算」实付，以及「合并凑单后」按金额比例分摊的真实实付。
- **跨用户合并**：合并方案允许不可叠加券跨用户组合，总优惠按各人原价占比分摊到每框。

## 数据模型（节选）

- `User(id, nickname, isDefault, isSelected)`：凑单参与者；`isDefault=true` 为「本人」，不可删；`isSelected` 决定参与计算。
- `Product(..., ownerId)`：新增 `ownerId` 字段，指向归属用户，默认 `1`（本人）。
- 完整模型见 `需求分析.md`。

## 技术栈

- Kotlin
- Android Gradle Plugin 8.5.0
- Jetpack Compose
- Navigation Compose
- Room
- Hilt
- KSP
- JUnit

## 项目结构

```text
app/src/main/kotlin/com/example/dealoptimizer/
├── data/               # Room 数据模型、DAO、Repository
├── di/                 # Hilt 依赖注入配置
├── domain/algorithm/   # 优惠券和凑单算法
├── presentation/ui/    # Compose 页面
└── presentation/viewmodel/
```

## 本地运行

1. 使用 Android Studio 打开项目根目录。
2. 等待 Gradle 同步完成。
3. 选择 `app` 运行配置。
4. 连接模拟器或真机运行。

命令行构建：

```powershell
.\gradlew.bat assembleDebug
```

运行单元测试：

```powershell
.\gradlew.bat testDebugUnitTest
```

如果命令行提示找不到 Java，可以临时使用 Android Studio 自带 JBR：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat testDebugUnitTest
```

## Git 使用

初始化仓库后，常用提交流程：

```powershell
git status
git add .
git commit -m "Initial commit"
```

如果要推送到 GitHub，先在 GitHub 创建一个空仓库，然后执行：

```powershell
git branch -M main
git remote add origin <your-repository-url>
git push -u origin main
```

## 测试状态

当前已通过：

```powershell
.\gradlew.bat testDebugUnitTest
```

## 备注

项目中的 `需求分析.md` 保留了原始需求说明，README 主要用于仓库首页展示、安装运行说明和功能概览。
