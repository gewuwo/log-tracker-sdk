# log-tracker-sdk
日志追踪sdk


## 配置参数说明

| 参数 | 描述 |
|---|---|
| enabled | 是否启用，默认为false |
| project | 项目名称，为空则使用tracker_UUID |
| senderClient | 发送客户端（不可为空），当前可选值为：<br/>tracker-server（tracker服务端）feishu（飞书） |
| sendUrl | 发送的接口地址（不可为空） |
| retries | 重试次数，默认为 10  |
| totalSizeInBytes | 单个 producer 实例能缓存的日志大小上限，默认为 100MB |
| maxBlockMs | 如果 producer 可用空间不足，调用者在 send 方法上的最大阻塞时间，默认为 60 秒 |
| ioThreadCount | 执行日志发送任务的线程池大小，默认为可用处理器个数 |
| batchSizeThresholdInBytes | 当一个 ProducerBatch 中缓存的日志大小大于等于 batchSizeThresholdInBytes 时，该 batch 将被发送，默认为 512 KB，最大可设置成 5MB |
| batchCountThreshold | 当一个 ProducerBatch 中缓存的日志条数大于等于 batchCountThreshold 时，该 batch 将被发送，默认为 4096，最大可设置成 40960 |
| lingerMs | 一个 ProducerBatch 从创建到可发送的逗留时间，默认为 2 秒，最小可设置成 100 毫秒 |
| baseRetryBackoffMs | 首次重试的退避时间，默认为 100 毫秒。<br/>Producer 采样指数退避算法，第 N 次重试的计划等待时间为 baseRetryBackoffMs * 2^(N-1) |
| maxRetryBackoffMs | 重试的最大退避时间，默认为 50 秒 |
| basePackageName | 最大错误消息行数拦截的包名，配合 {maxErrorMessageLine} 字段一起使用，例如拦截com.gewuwo.xx可以配置：gewuwo |
| maxErrorMessageLine | 最大错误消息行数，默认为10<br/>检测到某个关键字之后再取 {maxErrorMessageLine} 行 |




## 实现参考
* [aliyun-log-java-producer](https://github.com/aliyun/aliyun-log-java-producer)
* [aliyun-log-java-sdk](https://github.com/aliyun/aliyun-log-java-sdk)
* [aliyun-log-log4j2-appender](https://github.com/aliyun/aliyun-log-log4j2-appender)