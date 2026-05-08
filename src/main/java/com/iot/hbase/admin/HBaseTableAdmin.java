package com.iot.hbase.admin;

import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;

import java.io.IOException;

/**
 * HBase表管理员 - 创建和管理IoT数据表
 */
public class HBaseTableAdmin {

    private Admin admin;
    private static final String TABLE_NAME = "iot_device_data";
    private static final String[] COLUMN_FAMILIES = {
        "device_info",
        "sensor_data",
        "metadata"
    };

    public HBaseTableAdmin(Connection connection) throws IOException {
        this.admin = connection.getAdmin();
    }

    /**
     * 创建IoT数据表 (带预分片)
     * 
     * 预分片策略: 生成256个分片, 确保数据均衡分布
     * 
     * @throws IOException
     */
    public void createIotDataTable() throws IOException {
        TableName tableName = TableName.valueOf(TABLE_NAME);

        // 检查表是否存在
        if (admin.tableExists(tableName)) {
            System.out.println("表 " + TABLE_NAME + " 已存在");
            return;
        }

        // 创建表描述符
        HTableDescriptor tableDescriptor = new HTableDescriptor(tableName);

        // 添加列族
        for (String cf : COLUMN_FAMILIES) {
            HColumnDescriptor columnDescriptor = new HColumnDescriptor(cf);
            
            // 设置列族属性
            columnDescriptor.setMaxVersions(3);           // 保留3个版本
            columnDescriptor.setTimeToLive(30 * 24 * 3600); // 30天TTL
            columnDescriptor.setCompressionType(
                org.apache.hadoop.hbase.io.compress.Compression.Algorithm.SNAPPY);
            
            tableDescriptor.addFamily(columnDescriptor);
        }

        // 生成预分片 (256个)
        byte[][] splits = generateSplits(256);

        // 创建表
        admin.createTable(tableDescriptor, splits);

        System.out.println("表 " + TABLE_NAME + " 创建成功, 包含256个预分片");
    }

    /**
     * 生成预分片
     * 
     * @param numRegions 分片数量
     * @return 分片数组
     */
    private byte[][] generateSplits(int numRegions) {
        byte[][] splits = new byte[numRegions - 1][];

        for (int i = 1; i < numRegions; i++) {
            // 生成分片键: 0x00到0xFF
            splits[i - 1] = new byte[]{(byte) i};
        }

        return splits;
    }

    /**
     * 删除表
     */
    public void deleteTable() throws IOException {
        TableName tableName = TableName.valueOf(TABLE_NAME);

        if (admin.tableExists(tableName)) {
            admin.disableTable(tableName);
            admin.deleteTable(tableName);
            System.out.println("表 " + TABLE_NAME + " 已删除");
        }
    }

    /**
     * 关闭Admin
     */
    public void close() throws IOException {
        if (admin != null) {
            admin.close();
        }
    }
}
