package com.iot.hbase.client;

import com.iot.hbase.model.IotData;
import com.iot.hbase.rowkey.RowKeyGenerator;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * HBase数据写入客户端
 * 支持单条和批量写入
 */
public class HBaseWriter {

    private Connection connection;
    private Table table;
    private static final String TABLE_NAME = "iot_device_data";
    private static final String CF_DEVICE_INFO = "device_info";
    private static final String CF_SENSOR_DATA = "sensor_data";
    private static final String CF_METADATA = "metadata";

    public HBaseWriter(Connection connection) throws IOException {
        this.connection = connection;
        this.table = connection.getTable(TableName.valueOf(TABLE_NAME));
    }

    /**
     * 单条写入数据
     * 
     * @param data IoT数据
     * @throws IOException
     */
    public void write(IotData data) throws IOException {
        byte[] rowKey = RowKeyGenerator.generateRowKey(data.getDeviceId(), data.getReportTime());
        Put put = new Put(rowKey);

        // device_info列族
        put.addColumn(Bytes.toBytes(CF_DEVICE_INFO), 
                     Bytes.toBytes("device_id"), 
                     Bytes.toBytes(data.getDeviceId()));
        put.addColumn(Bytes.toBytes(CF_DEVICE_INFO), 
                     Bytes.toBytes("device_type"), 
                     Bytes.toBytes(data.getDeviceType()));
        put.addColumn(Bytes.toBytes(CF_DEVICE_INFO), 
                     Bytes.toBytes("device_name"), 
                     Bytes.toBytes(data.getDeviceName() != null ? data.getDeviceName() : ""));
        put.addColumn(Bytes.toBytes(CF_DEVICE_INFO), 
                     Bytes.toBytes("location"), 
                     Bytes.toBytes(data.getLocation() != null ? data.getLocation() : ""));

        // sensor_data列族
        put.addColumn(Bytes.toBytes(CF_SENSOR_DATA), 
                     Bytes.toBytes("temperature"), 
                     Bytes.toBytes(data.getTemperature()));
        put.addColumn(Bytes.toBytes(CF_SENSOR_DATA), 
                     Bytes.toBytes("humidity"), 
                     Bytes.toBytes(data.getHumidity()));
        put.addColumn(Bytes.toBytes(CF_SENSOR_DATA), 
                     Bytes.toBytes("pressure"), 
                     Bytes.toBytes(data.getPressure()));
        put.addColumn(Bytes.toBytes(CF_SENSOR_DATA), 
                     Bytes.toBytes("signal_strength"), 
                     Bytes.toBytes(data.getSignalStrength()));

        // metadata列族
        put.addColumn(Bytes.toBytes(CF_METADATA), 
                     Bytes.toBytes("report_time"), 
                     Bytes.toBytes(data.getReportTime()));
        put.addColumn(Bytes.toBytes(CF_METADATA), 
                     Bytes.toBytes("status"), 
                     Bytes.toBytes(data.getStatus()));
        put.addColumn(Bytes.toBytes(CF_METADATA), 
                     Bytes.toBytes("error_code"), 
                     Bytes.toBytes(data.getErrorCode()));

        table.put(put);
    }

    /**
     * 批量写入数据 (性能更优)
     * 
     * @param dataList 数据列表
     * @throws IOException
     */
    public void batchWrite(List<IotData> dataList) throws IOException {
        List<Put> puts = new ArrayList<>();

        for (IotData data : dataList) {
            byte[] rowKey = RowKeyGenerator.generateRowKey(data.getDeviceId(), data.getReportTime());
            Put put = new Put(rowKey);

            // device_info列族
            put.addColumn(Bytes.toBytes(CF_DEVICE_INFO), 
                         Bytes.toBytes("device_id"), 
                         Bytes.toBytes(data.getDeviceId()));
            put.addColumn(Bytes.toBytes(CF_DEVICE_INFO), 
                         Bytes.toBytes("device_type"), 
                         Bytes.toBytes(data.getDeviceType()));

            // sensor_data列族
            put.addColumn(Bytes.toBytes(CF_SENSOR_DATA), 
                         Bytes.toBytes("temperature"), 
                         Bytes.toBytes(data.getTemperature()));
            put.addColumn(Bytes.toBytes(CF_SENSOR_DATA), 
                         Bytes.toBytes("humidity"), 
                         Bytes.toBytes(data.getHumidity()));
            put.addColumn(Bytes.toBytes(CF_SENSOR_DATA), 
                         Bytes.toBytes("pressure"), 
                         Bytes.toBytes(data.getPressure()));

            // metadata列族
            put.addColumn(Bytes.toBytes(CF_METADATA), 
                         Bytes.toBytes("report_time"), 
                         Bytes.toBytes(data.getReportTime()));
            put.addColumn(Bytes.toBytes(CF_METADATA), 
                         Bytes.toBytes("status"), 
                         Bytes.toBytes(data.getStatus()));

            puts.add(put);
        }

        // 批量提交
        table.put(puts);
        System.out.println("已批量写入 " + puts.size() + " 条数据");
    }

    /**
     * 关闭连接
     */
    public void close() throws IOException {
        if (table != null) {
            table.close();
        }
    }
}
