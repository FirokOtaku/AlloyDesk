# Alloy Desk

提供简易操作界面, 整合 Label Studio 和 MMDetection.  
帮助管理使用 MMDetection 框架进行 **实例分割** 任务时用到的数据集和生成的模型文件.

----

包含功能:

- [X]  数据集拉取和管理
- [x]  训练任务创建和管理
- [x]  模型管理
- [X]  模型测试
- [ ]  模型推理接口管理
- [ ]  简易用户管理和接口鉴权
- [ ]  系统日志

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

* 配置运行环境并启动
  * `java --enable-preview -jar alloy-desk.jar [...配置]`
* 在 Label Studio 中创建实例分割用数据集
* 在 Alloy Desk 中拉取数据集
* 上传初始模型
  * 第一次使用需要上传初始模型,  
    比如 MMDetection 框架提供的预训练模型
  * 后续训练可以使用前次任务生成的模型
* 指定训练计划任务并启动训练

> 底层实际在生成并执行各种 .py 配置文件和 .bat 脚本文件.  
> 除了为了方便配置初始环境而提供的上传 COCO 格式数据集接口所需的压缩包需要遵循指定结构,
> 其它不使用任何自定义数据格式.

依赖个人库:

* [Topaz](https://github.com/FirokOtaku/Topaz)
* [MVCIntrospector](https://github.com/FirokOtaku/MVCIntrospector)
* [Dubnium Sculptor](https://github.com/FirokOtaku/DubniumSculptor)
* [Alloy Wrench](https://github.com/FirokOtaku/AlloyWrench)
* [Label Studio Connector Java](https://github.com/FirokOtaku/LabelStudioConnectorJava)

> 如果你真的想自己编译打包这个项目,  
> 那得把这堆 repo 全部 clone 下来,  
> 然后 `mvn install` 到本地仓库.  
> 熟悉流程的话其实几分钟就行了 (吧)

----

> ![gpl3](https://www.gnu.org/graphics/gplv3-127x51.png)
