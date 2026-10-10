# 第三方组件与权利说明

以下说明区分仓库直接携带的文件与构建时下载的依赖，不将整个 APK 宣称为单一许可作品。

| 组件 | 使用方式 | 上游许可/条款 |
| --- | --- | --- |
| Gradle 9.5.0 Wrapper | 仓库包含启动脚本和 Wrapper JAR；完整 Gradle 不入库 | Apache-2.0 及其分发包附带声明，见 third_party/GRADLE-LICENSE.txt 与 GRADLE-NOTICE.txt |
| Android Gradle Plugin 9.3.0 | 构建时下载 | [Android 工具来源](https://developer.android.com/build)，以所下载组件许可为准 |
| JUnit 4.13.2 | 仅单元测试 | [JUnit 4 / EPL-1.0](https://github.com/junit-team/junit4/blob/main/LICENSE-junit.txt) |
| org.json:json 20240303 | 仅单元测试，验证实际 JSON 请求与响应解析；不打入 APK | [上游 LICENSE：Public Domain](https://github.com/stleary/JSON-java/blob/20240303/LICENSE) |
| 传递依赖 | Gradle 解析 | 各组件自身的许可与 NOTICE；发布二进制前必须再次核查 |

Gradle 随附的完整 LICENSE/NOTICE 作为上游声明保留，不表示列出的所有 Gradle 分发组件都被嵌入本仓库的 Wrapper JAR。

```sh
./gradlew :app:dependencies --configuration releaseRuntimeClasspath
```

上面命令可用于核对运行时依赖树；它本身不是完整许可审计或 SBOM。v3.2.16 已移除 ML Kit 及 OCR 模型，APK 不再包含这些组件。构建生成的依赖若被重新分发，发布者需遵守各自许可、署名和披露要求。本次仅托管源码项目，不上传 APK 或完整 SDK/JDK。

内置 `vocabulary.csv` 是通用入门示例，不代表完整课程题库；新增词表应说明来源并取得必要授权。个人导入、手动、模型和学习词库不随项目上传。Duolingo、Google、Android、OpenAI、DeepSeek、Qwen 等名称仅用于兼容性或来源说明，商标权归各权利人，本项目不主张背书关系。
