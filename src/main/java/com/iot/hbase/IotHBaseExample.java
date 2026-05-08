package com.iot.hbase;

import com.iot.hbase.admin.HBaseTableAdmin;
import com.iot.hbase.client.HBaseWriter;
import com.iot.hbase.model.IotData;
import com.iot.hbase.query.IotDataQueryService;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * IoT HBase 存储完整演示程序
 * 
 * 演示流程:
 * 1. 创建表
 * 2. 写入数据 (单条和批量)
 * 3. 查询数据 (最近N条、时间范围、条件查询)
 */
public class IotHBaseExample {

    public static void main(String[] args) {
        try {
            // 1. 初始化HBase连接
            Configuration conf = HBaseConfiguration.create();
            conf.set("hbase.zookeeper.quorum", "localhost");
            conf.set("hbase.zookeeper.property.clientPort", "2181");

            Connection connection = ConnectionFactory.createConnection(conf);
            System.out.println("✓ HBase连接成功\n");

            // 2. 创建表
            System.out.println("========== 1. 创建表 ==========");
            HBaseTableAdmin admin = new HBaseTableAdmin(connection);
            admin.createIotDataTable();
            admin.close();

            // 3. 写入数据
            System.out.println("\n========== 2. 写入数据 ==========");
            HBaseWriter writer = new HBaseWriter(connection);

            // 生成测试数据
            List<IotData> dataList = generateTestData(10);

            // 单条写入示例
            System.out.println("单条写入示例:");
            if (!dataList.isEmpty()) {
                writer.write(dataList.get(0));
                System.out.println("✓ 已写入: " + dataList.get(0));
            }

            // 批量写入
            System.out.println("\n批量写入示例:");
            writer.batchWrite(dataList);
            writer.close();

            // 4. 查询数据
            System.out.println("\n========== 3. 查询数据 ==========");
            IotDataQueryService queryService = new IotDataQueryService(connection);

            // 查询最近N条记录
            System.out.println("查询设备device_001最近5条记录:");
            List<IotData> latestRecords = queryService.queryLatestRecords("device_001", 5);
            for (int i = 0; i < latestRecords.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + latestRecords.get(i));
            }

            // 查询时间范围内的数据
            System.out.println("\n查询时间范围内的数据:");
            long now = System.currentTimeMillis();
            long oneHourAgo = now - 3600000;
            List<IotData> rangeData = queryService.queryByTimeRange("device_001", oneHourAgo, now, 5);
            System.out.println("  找到 " + rangeData.size() + " 条记录");

            queryService.close();

            // 5. 关闭连接
            connection.close();
            System.out.println("\n✓ 演示完成");

        } catch (IOException e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 生成测试数据
     */
    private static List<IotData> generateTestData(int count) {
        List<IotData> dataList = new ArrayList<>();
        Random random = new Random();

        String[] deviceIds = {"device_001", "device_002", "device_003"};
        String[] deviceTypes = {"温度传感器", "湿度传感器", "综合传感器"};

        long currentTime = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            IotData data = new IotData();
            data.setDeviceId(deviceIds[i % deviceIds.length]);
            data.setDeviceType(deviceTypes[i % deviceTypes.length]);
            data.setDeviceName("IoT设备" + (i + 1));
            data.setLocation("机房A");
            data.setTemperature(15 + random.nextDouble() * 20);  // 15-35℃
            data.setHumidity(30 + random.nextDouble() * 40);      // 30-70%
            data.setPressure(1013 + random.nextDouble() * 10);    // 1013-1023hPa
            data.setSignalStrength(50 + random.nextInt(50));      // 50-100
            data.setReportTime(currentTime - (long) i * 60000);   // 每条数据相隔1分钟
            data.setStatus("NORMAL");
            data.setErrorCode(0);

            dataList.add(data);
        }

        return dataList;
    }
}
