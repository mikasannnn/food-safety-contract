# 规划网络拓扑

1个orderer节点；组织Org1，org1下有两个peer节点：peer0和peer1；



![规划网络拓扑](./../../../../下载/imgs/Typora/规划网络拓扑.png)

资源使用规划：

| 节点       | 宿主机ip       |         hosts          | 端口 |
| ---------- | -------------- | :--------------------: | ---- |
| cli        | 192.168.10.106 |          N/A           | N/A  |
| orderer0   | 192.168.10.106 |  orderer0.example.com  | 7050 |
| org1-peer0 | 192.168.10.106 | peer0.org1.example.com | 7051 |
| org1-peer1 | 192.168.10.106 | peer1-org1.example.com | 8051 |
