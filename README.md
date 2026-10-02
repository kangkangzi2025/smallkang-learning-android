# anki-rsi

一个小而美的 Android 间隔重复（Anki 式）记忆卡片应用。Jetpack Compose 编写，FSRS-6 排程，release 包约 1.4 MB。

## 功能

- 卡组、卡片的增删改；正面写 `{{答案}}` 即成挖空卡（也认 Anki 的 `{{c1::答案::提示}}`）
- 学习：先看问题，点屏幕或「显示答案」，再选 重来 / 困难 / 良好 / 简单，按钮上写着下次复习的时间
- FSRS-6 默认参数，目标记忆保持率 90%；每天最多 20 张新卡；凌晨 4 点换日（同 Anki）
- 导入 TSV / CSV（UTF-8），包括 Anki「导出 → 纯文本笔记」生成的文件：文件名就是卡组名，第一列是正面，第二列是背面；同名卡组再导入只会补上新增的行
- 备份 / 恢复：「备份」导出一个 JSON 文件，「导入文件」选它即可恢复
- 导出复习记录（`card_id,review_time,review_rating`），可直接交给 [FSRS 优化器](https://github.com/open-spaced-repetition/fsrs-optimizer) 拟合个人参数
- 浅色 / 深色；平板（如 OPPO Pad Mini）上内容排在居中的阅读栏里，横竖屏都可以

## 构建与安装

需要 JDK 17+ 和 Android SDK（compileSdk 36）。

```bash
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

release 包用调试密钥签名，方便装到自己的手机上。换一台电脑构建时调试密钥不同，覆盖安装会失败，先在应用里「备份」再卸载重装。

`./gradlew test` 会把 FSRS 的实现和 py-fsrs 的结果逐值对比。

## 代码

| 文件 | 内容 |
| --- | --- |
| `Model.kt` | 卡片、卡组，今天的学习队列 |
| `Fsrs.kt` | FSRS-6 排程 |
| `Library.kt` | 内存状态、JSON 存储、导入导出 |
| `Theme.kt` | 颜色、字体、圆角 |
| `Ui.kt` | 共用的小组件 |
| `DecksScreen.kt` · `DeckScreen.kt` · `StudyScreen.kt` | 三个页面 |

`examples/` 里有一个从论文做出的示例卡组。
