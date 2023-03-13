# 详细启动配置清单

启动命令为 `java --enable-preview -jar alloy-desk.jar [...配置项]`, 每个配置项的格式为 `--{键}={值}`, 如: `--server.port=8080`

推荐将配置后的启动命令保存为 `.bat` 脚本

## 一般配置

配置键 | 描述 | 示例值
--- | -- | ---
`server.port` | 调整运行 HTTP 端口 | `8080`
`logging.file.path` | 调整日志目录 | `./alloy-desk-log/`
`firok.spring.alloydesk.folder-dataset` | 调整数据集储存目录 | `./alloy-desk-dataset`
`firok.spring.alloydesk.folder-model` | 调整模型储存目录 | `./alloy-desk-model`
`firok.spring.alloydesk.folder-task` | 调整任务配置文件等目录 | `./alloy-desk-task`
`firok.spring.alloydesk.folder-test` | 调整模型测试临时目录 | `./alloy-desk-test`
`firok.spring.alloydesk.folder-mmdetection` | 调整 MMDetection 主目录 | `D:/mmdetection`
`firok.spring.alloydesk.mmdetection-pre-script` | 调整 MMDetection 环境预执行脚本 | `conda activate open-mmlab`
`firok.spring.alloydesk.mmdetection-base-model` | 调整 MMDetection 训练所用基础模型路径 | `D:/mmdetection/checkpoints/mask_rcnn_r50_caffe_fpn_1x_coco_bbox_mAP-0.38__segm_mAP-0.344_20200504_231812-0ebd1859.pth`
`firok.spring.alloydesk.test-device` | 调整模型测试时使用哪个设备 | `cuda:0`

## 更多配置

### 使用 MySQL 数据库

* `spring.datasource.driver-class-name` 设为 `com.mysql.cj.jdbc.Driver`
* `spring.datasource.url` 设为需要连接的数据库地址. 如: `jdbc:mysql://localhost:3306/alloy-desk?serverTimezone=GMT%2B8`
* `spring.datasource.username` 设为目标数据库用户名
* `spring.datasource.password` 设为目标数据库密码

### 使用 H2 数据库

* `spring.datasource.driver-class-name` 设为 `org.h2.Driver`
* `spring.datasource.url` 设为需要连接的数据库地址. 如: `jdbc:h2://~/alloy-desk.db;AUTO_SERVER=TRUE`

### 打印 SQL 操作日志

如需查看执行的 SQL 语句, 请将 `mybatis-plus.configuration.log-impl` 设为 `org.apache.ibatis.logging.stdout.StdOutImpl`

### 调整 H2 网页控制台

* 如果需要启用 H2 网页控制台,
  请将 `spring.h2.console.enabled` 设为 `true`, 然后访问 `http://{项目地址}/h2-console`
* 如果需要允许非本地地址访问网页控制台,
  请将 `spring.h2.console.settings.web-allow-others` 设为 `true`
* 调整网页控制台管理员密码,
  请调整 `spring.h2.console.settings.web-admin-password`

> ⚠ 为了调试和开发方便,  
> 项目里的 H2 控制台默认允许外部访问.  
> 这可能造成安全性问题,  
> 如果不需要的话最好关掉.
