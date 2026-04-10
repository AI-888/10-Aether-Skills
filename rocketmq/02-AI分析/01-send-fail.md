## 🚀 RocketMQ 生产消息失败排查手册

---

### 1. 客户端问题 (Producer)

- **NameServer 地址配置错误**
  - 排查步骤：检查 Producer 代码或配置文件中的 `namesrvAddr` 值
    - 执行：查看应用配置文件或启动参数中 `namesrvAddr` 的值，确认格式为 `IP:PORT`（多个用分号分隔）
    - 检查点：地址格式正确、IP 和端口与实际 NameServer 一致、无多余空格或特殊字符
    - 检查人：用户

- **发送超时（`sendMsgTimeout` 设置过短）**
  - 排查步骤：确认超时配置并结合网络延迟判断是否合理
    - 执行：在代码中搜索 `sendMsgTimeout` 配置值（默认 3 秒），同时用 `ping <Broker_IP>` 测量网络延迟
    - 检查点：超时时间应大于网络往返延迟 + Broker 处理耗时；建议在高延迟环境下调整为 5~10 秒
    - 检查人：用户

- **消息体超过大小限制（默认 4MB）**
  - 排查步骤：检查发送消息的实际大小
    - 执行：排查send方法抛的异常日志
    - 检查点：消息体大小 < `maxMessageSize`（默认 4MB）；如需发送大消息，需同时调整客户端和 Broker 端的 `maxMessageSize` 配置
    - 检查人：用户

- **ACL 权限认证失败**
  - 排查步骤：核实 ACL 凭证配置
    - 执行：检查 Producer 配置中的 `accessKey` 和 `secretKey` 是否与 Broker 端 `plain_acl.yml` 中的配置一致
    - 检查点：凭证正确、该账号对目标 Topic 拥有 `PUB` 权限；客户端日志中无 `AclException` 异常
    - 检查人：用户

- **生产者状态异常（未启动或已关闭）**
  - 排查步骤：确认 Producer 实例的生命周期状态
    - 执行：在发送消息前调用 `producer.getServiceState()` 打印状态
    - 检查点：状态必须为 `RUNNING`；排查代码中是否存在提前调用 `shutdown()` 或 `start()` 失败的情况

- **客户端与服务端版本不兼容**
  - 排查步骤：确认客户端 SDK 与 Broker 的版本匹配关系
    - 执行：查看客户端 Maven/Gradle 依赖中 `rocketmq-client` 的版本号，以及 Broker 启动日志中打印的版本号
    - 检查点：5.x 客户端（gRPC 协议）不能直接连接 4.x Broker（Remoting 协议）；大版本号应保持一致或参考官方兼容性矩阵

---

### 2. 网络问题 (Network)

- **客户端无法访问 NameServer 或 Broker**
  - 排查步骤：逐层测试网络连通性
    - 执行：
      - `ping <NameServer_IP>`（测试基础网络可达性）
      - `telnet <NameServer_IP> 9876`（测试 NameServer 端口）
      - `telnet <Broker_IP> 10911`（测试 Broker 端口）
      - 检查对应组件的svc的状态，status是否包含error，exception
    - 检查点：三项测试均成功；如 ping 通但 telnet 失败，说明端口被防火墙或安全组拦截
    

- **网络延迟过高或抖动**
  - 排查步骤：量化网络延迟和丢包率
    - 执行：`ping -c 100 <Broker_IP>`（观察平均延迟和丢包率）；`mtr <Broker_IP>`（追踪链路中的瓶颈节点）
    - 检查点：平均延迟应在可接受范围内（通常 < 10ms 同机房，< 50ms 跨机房）；丢包率应为 0%

- **VIP 频道端口冲突（10909 端口未映射）**
  - 排查步骤：确认 VIP 通道配置
    - 执行：`telnet <Broker_IP> 10909`（测试 VIP 端口）；检查客户端是否设置了 `vipChannelEnabled=true`
    - 检查点：如 Broker 未开放 10909 端口，需在客户端设置 `vipChannelEnabled=false`

- **防火墙 / 安全组规则拦截**
  - 排查步骤：检查各节点的端口放行策略
    - 执行：
      - 客户端机器：`iptables -L -n | grep <PORT>` 或查看云安全组出站规则
      - Broker/NameServer 机器：`iptables -L -n | grep <PORT>` 或查看云安全组入站规则
    - 检查点：NameServer 的 9876 端口、Broker 的 10911（及 10909/10912）端口在所有相关节点的防火墙和安全组中均已放行

---

### 3. NameServer 问题 (NameServer)

- **NameServer 进程未运行或崩溃**
  - 排查步骤：检查进程状态和启动日志
    - 执行：
      - `ps -ef | grep NamesrvStartup`（确认进程是否存在）
      - `tail -200 $ROCKETMQ_HOME/logs/rocketmqlogs/namesrv.log`（查看最近日志）
    - 检查点：进程存在且日志中无 `OutOfMemoryError`、`Fatal` 等异常；如进程不存在则需重新启动

- **NameServer 负载过高导致响应超时**
  - 排查步骤：监控 NameServer 机器资源使用情况
    - 执行：
      - `top -p <NameServer_PID>`（观察 CPU 和内存占用）
      - `ss -tnlp | grep 9876`（确认端口监听正常）
    - 检查点：CPU 使用率 < 80%、内存充足、无大量 TIME_WAIT 连接堆积

- **Broker 心跳超时被 NameServer 剔除（路由信息丢失）**
  - 排查步骤：验证 Topic 路由信息是否完整
    - 执行：`sh mqadmin topicRoute -n <NameServer_IP>:9876 -t <TopicName>`（查看路由中的 Broker 列表）
    - 检查点：路由结果中包含预期的所有 Broker 地址；如缺少某个 Broker，检查该 Broker 是否正常运行并与 NameServer 保持心跳（默认 30s 超时剔除）

- **多 NameServer 节点间数据不一致**
  - 排查步骤：分别查询每个 NameServer 节点的路由信息并对比
    - 执行：对每个 NameServer 节点分别执行 `sh mqadmin topicRoute -n <NameServer_IP_X>:9876 -t <TopicName>`
    - 检查点：所有节点返回的路由信息一致；如不一致，检查 Broker 配置的 `namesrvAddr` 是否包含了所有 NameServer 地址

---

### 4. Broker 问题 (Broker)

- **Broker 进程未运行或异常退出**
  - 排查步骤：检查进程和启动日志
    - 执行：
      - `ps aux | grep BrokerStartup`（确认进程是否存在）
      - `tail -200 $ROCKETMQ_HOME/logs/rocketmqlogs/broker.log`（查看最近日志）
    - 检查点：进程存在且日志中无 `Fatal`、`Shutdown` 等异常信息

- **磁盘空间不足（超过阈值拒绝写入）**
  - 排查步骤：检查存储目录磁盘使用率
    - 执行：
      - `df -h`（查看各分区使用率）
      - `sh mqadmin getBrokerConfig -b <Broker_IP>:10911 -n <NameServer_IP>:9876 | grep diskMaxUsedSpaceRatio`（查看阈值配置，默认 85%）
    - 检查点：存储目录所在分区使用率 < `diskMaxUsedSpaceRatio`；如超过阈值需清理过期消息或扩容磁盘

- **Broker 过载 / 繁忙（触发流控）**
  - 排查步骤：分析 Broker 日志中的流控关键字
    - 执行：
      - `grep -E "broker busy|TIMEOUT_CLEAN_QUEUE|PAGE_WRITE_BUSY" $ROCKETMQ_HOME/logs/rocketmqlogs/broker.log | tail -50`
    - 检查点：如频繁出现上述关键字，说明 Broker 处理能力达到瓶颈；需排查是否存在突发流量、线程池满或 PageCache 锁竞争

- **磁盘 I/O 性能差导致写入超时**
  - 排查步骤：监控磁盘 I/O 指标
    - 执行：`iostat -x 1 5`（重点观察存储盘的 `%util`、`await`、`w/s` 指标）
    - 检查点：`%util` < 80%、`await` < 10ms 为健康状态；如 I/O 持续饱和，考虑更换 SSD 或优化刷盘策略（`flushDiskType` 从 `SYNC_FLUSH` 改为 `ASYNC_FLUSH`）

- **Broker 权限被设置为只读**
  - 排查步骤：检查 Broker 的读写权限配置
    - 执行：`sh mqadmin clusterList -n <NameServer_IP>:9876`（查看各 Broker 的 `perm` 值）
    - 检查点：`perm` 值为 6（可读写）表示正常；如为 4（只读）则需通过 `sh mqadmin updateBrokerConfig` 修改

---

### 5. Topic 问题 (Topic)

- **Topic 不存在且未开启自动创建**
  - 排查步骤：确认 Topic 是否存在以及自动创建开关状态
    - 执行：
      - `sh mqadmin topicList -n <NameServer_IP>:9876 | grep <TopicName>`（搜索目标 Topic）
      - `sh mqadmin getBrokerConfig -b <Broker_IP>:10911 -n <NameServer_IP>:9876 | grep autoCreateTopicEnable`（查看自动创建开关）
    - 检查点：Topic 存在于列表中；如不存在且 `autoCreateTopicEnable=false`，需手动创建：`sh mqadmin updateTopic -c <ClusterName> -t <TopicName> -n <NameServer_IP>:9876`

- **Topic 路由信息错误或包含不可达地址**
  - 排查步骤：查看路由详情并验证每个地址的可达性
    - 执行：
      - `sh mqadmin topicRoute -t <TopicName> -n <NameServer_IP>:9876`（获取路由中所有 Broker 地址）
      - 对路由中的每个 `brokerAddr` 执行 `telnet <IP> <PORT>`
    - 检查点：路由中所有 Broker 地址均可达；如存在不可达地址，需检查对应 Broker 是否已下线或 IP 变更

- **Topic 写入权限被禁用（perm 为只读）**
  - 排查步骤：检查 Topic 的权限字段
    - 执行：`sh mqadmin topicRoute -t <TopicName> -n <NameServer_IP>:9876`（查看输出中每个 Queue 的 `perm` 字段）
    - 检查点：`perm=6` 表示可读写（正常）；`perm=4` 表示只读（无法写入）；如需修改：`sh mqadmin updateTopic -c <ClusterName> -t <TopicName> -n <NameServer_IP>:9876 -p 6`

- **所有 Message Queue 不可用（队列所在 Broker 全部宕机）**
  - 排查步骤：查看 Topic 的队列分布和 Broker 存活状态
    - 执行：
      - `sh mqadmin topicStatus -n <NameServer_IP>:9876 -t <TopicName>`（查看各队列状态）
      - `sh mqadmin clusterList -n <NameServer_IP>:9876`（查看集群中各 Broker 存活状态）
    - 检查点：至少有一个 Broker 处于存活状态且其上有可写的 Message Queue；如所有 Queue 所在 Broker 均不可用，需优先恢复 Broker
