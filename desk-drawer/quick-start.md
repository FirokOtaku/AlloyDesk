# 快速开始

所需环境:

* Windows
    * 目前底层生成调用的是 .bat 批处理文件,  
      后续可能提供 linux 环境支持
* Java 19+
* [Label Studio](https://labelstud.io/) 实例
* [MMDetection](https://mmdetection.readthedocs.io/) 环境
    * 可直接运行或基于 Anaconda 运行的均可
* MySQL 8+ / H2 DB
    * 在启动配置调整使用的驱动类和 JDBC URL

使用流程:

* 在 GitHub 下载 release 文件
* 配置运行环境并启动
    * `java --enable-preview -jar alloy-desk.jar [...配置]`
    * [详细启动配置清单](config.md)
* 访问 `http://localhost:29118`
* 在 Label Studio 中创建 **实例分割用** 数据集
* 在 Alloy Desk 中添加 Label Studio 数据源
* 在 Alloy Desk 中拉取数据集
* 上传初始模型
    * 第一次使用需要上传初始模型,  
      比如 MMDetection 框架提供的预训练模型
    * 后续训练可以使用先前任务生成的模型
* 指定训练计划任务并启动训练
