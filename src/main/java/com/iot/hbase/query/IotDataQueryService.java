package com.iot.hbase.query;

import com.iot.hbase.model.IotData;
import com.iot.hbase.rowkey.RowKeyGenerator;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * IoT数据查询服务
 * 支持多种查询方式
 */
public class IotDataQueryService {

    private Connection connection;
    private Table table;
    private static final String TABLE_NAME = "iot_device_data";
    private static final String CF_DEVICE_INFO = "device_info";
    private static final String CF_SENSOR_DATA = "sensor_data";
    private static final String CF_METADATA = "metadata";

    public IotDataQueryService(Connection connection) throws IOException {
        this.connection = connection;
        this.table = connection.getTable(TableName.valueOf(TABLE_NAME));
    }

    /**
     * 查询指定设备最近N条记录
     * 
     * 这是最常见的查询场景
     * 
     * @param deviceId 设备ID
     * @param limit 返回记录数
     * @return 最近N条记录 (按时间从新到旧排序)
     * @throws IOException
     */
    public List<IotData> queryLatestRecords(String deviceId, int limit) throws IOException {
        List<IotData> results = new ArrayList<>();

        // 构建Scan对象
        Scan scan = new Scan();

        // 设置起始行: 最新时间对应的RowKey
        // 当前时间戳对应最小的反转时间戳
        byte[] startRow = RowKeyGenerator.generateRowKey(deviceId, System.currentTimeMillis());
        scan.withStartRow(startRow);

        // 设置终止行: 最旧时间对应的RowKey
        // 0时间戳对应最大的反转时间戳
        byte[] stopRow = RowKeyGenerator.generateRowKey(deviceId, 0);
        scan.withStopRow(stopRow, true);

        // 添加过滤器: 只查询指定设备
        Filter filter = new SingleColumnValueFilter(
            Bytes.toBytes(CF_DEVICE_INFO),
            Bytes.toBytes("device_id"),
            CompareFilter.CompareOp.EQUAL,
            Bytes.toBytes(deviceId)
        );
        scan.setFilter(filter);

        // 设置返回行数限制
        scan.setMaxResultSize(limit * 1000);
        scan.setCaching(limit);

        // 执行Scan
        try (ResultScanner scanner = table.getScanner(scan)) {
            int count = 0;
            for (Result result : scanner) {
                if (count >= limit) break;
                IotData data = parseResult(result);
                results.add(data);
                count++;
            }
        }

        return results;
    }

    /**
     * 查询时间范围内的数据
     * 
     * @param deviceId 设备ID
     * @param startTime 开始时间 (毫秒)
     * @param endTime 结束时间 (毫秒)
     * @param limit 限制返回行数
     * @return 符合条件的数据列表
     * @throws IOException
     */
    public List<IotData> queryByTimeRange(String deviceId, long startTime, 
                                          long endTime, int limit) throws IOException {
        List<IotData> results = new ArrayList<>();

        Scan scan = new Scan();

        // startTime和endTime都需要转换为反转时间戳
        // 由于反转，较晚的时间(endTime)对应较小的反转时间戳
        long reverseEndTime = Long.MAX_VALUE - endTime;
        long reverseStartTime = Long.MAX_VALUE - startTime;

        byte[] startRow = Bytes.add(
            new byte[]{(byte)(Math.abs(deviceId.hashCode()) % 256)},
            Bytes.toBytes(reverseEndTime),
            Bytes.toBytes(deviceId)
        );

        byte[] stopRow = Bytes.add(
            new byte[]{(byte)(Math.abs(deviceId.hashCode()) % 256)},
            Bytes.toBytes(reverseStartTime),
            Bytes.toBytes(deviceId)
        );

        scan.withStartRow(startRow);
        scan.withStopRow(stopRow, true);

        // 添加时间过滤器
        FilterList filters = new FilterList(
            FilterList.Operator.MUST_PASS_ALL
        );

        filters.addFilter(new SingleColumnValueFilter(
            Bytes.toBytes(CF_METADATA),
            Bytes.toBytes("report_time"),
            CompareFilter.CompareOp.GREATER_OR_EQUAL,
            Bytes.toBytes(startTime)
        ));

        filters.addFilter(new SingleColumnValueFilter(
            Bytes.toBytes(CF_METADATA),
            Bytes.toBytes("report_time"),
            CompareFilter.CompareOp.LESS_OR_EQUAL,
            Bytes.toBytes(endTime)
        ));

        scan.setFilter(filters);
        scan.setMaxResultSize(limit * 1000);

        try (ResultScanner scanner = table.getScanner(scan)) {
            int count = 0;
            for (Result result : scanner) {
                if (count >= limit) break;
                results.add(parseResult(result));
                count++;
            }
        }

        return results;
    }

    /**
     * 查询温度在指定范围内的数据
     * 
     * @param deviceId 设备ID
     * @param minTemp 最小温度
     * @param maxTemp 最大温度
     * @param limit 限制返回行数
     * @return 符合条件的数据列表
     * @throws IOException
     */
    public List<IotData> queryByTemperatureRange(String deviceId, double minTemp, 
                                                  double maxTemp, int limit) throws IOException {
        List<IotData> results = new ArrayList<>();

        Scan scan = new Scan();

        // 构建RowKey范围
        byte[] startRow = RowKeyGenerator.generateRowKey(deviceId, System.currentTimeMillis());
        byte[] stopRow = RowKeyGenerator.generateRowKey(deviceId, 0);

        scan.withStartRow(startRow);
        scan.withStopRow(stopRow, true);

        // 添加温度过滤器
        FilterList filters = new FilterList(
            FilterList.Operator.MUST_PASS_ALL
        );

        filters.addFilter(new SingleColumnValueFilter(
            Bytes.toBytes(CF_SENSOR_DATA),
            Bytes.toBytes("temperature"),
            CompareFilter.CompareOp.GREATER_OR_EQUAL,
            Bytes.toBytes(minTemp)
        ));

        filters.addFilter(new SingleColumnValueFilter(
            Bytes.toBytes(CF_SENSOR_DATA),
            Bytes.toBytes("temperature"),
            CompareFilter.CompareOp.LESS_OR_EQUAL,
            Bytes.toBytes(maxTemp)
        ));

        scan.setFilter(filters);
        scan.setMaxResultSize(limit * 1000);

        try (ResultScanner scanner = table.getScanner(scan)) {
            int count = 0;
            for (Result result : scanner) {
                if (count >= limit) break;
                results.add(parseResult(result));
                count++;
            }
        }

        return results;
    }

    /**
     * 解析Result对象为IotData
     */
    private IotData parseResult(Result result) {
        IotData data = new IotData();

        // 解析RowKey获取deviceId和timestamp
        RowKeyGenerator.RowKeyInfo rowKeyInfo = RowKeyGenerator.parseRowKey(result.getRow());
        data.setDeviceId(rowKeyInfo.deviceId);
        data.setReportTime(rowKeyInfo.timestamp);

        // 从device_info列族读取
        byte[] deviceId = result.getValue(Bytes.toBytes(CF_DEVICE_INFO), 
                                         Bytes.toBytes("device_id"));
        if (deviceId != null) {
            data.setDeviceId(Bytes.toString(deviceId));
        }

        byte[] deviceType = result.getValue(Bytes.toBytes(CF_DEVICE_INFO), 
                                           Bytes.toBytes("device_type"));
        if (deviceType != null) {
            data.setDeviceType(Bytes.toString(deviceType));
        }

        // 从sensor_data列族读取
        byte[] temperature = result.getValue(Bytes.toBytes(CF_SENSOR_DATA), 
                                            Bytes.toBytes("temperature"));
        if (temperature != null) {
            data.setTemperature(Bytes.toDouble(temperature));
        }

        byte[] humidity = result.getValue(Bytes.toBytes(CF_SENSOR_DATA), 
                                         Bytes.toBytes("humidity"));
        if (humidity != null) {
            data.setHumidity(Bytes.toDouble(humidity));
        }

        byte[] pressure = result.getValue(Bytes.toBytes(CF_SENSOR_DATA), 
                                         Bytes.toBytes("pressure"));
        if (pressure != null) {
            data.setPressure(Bytes.toDouble(pressure));
        }

        // 从metadata列族读取
        byte[] status = result.getValue(Bytes.toBytes(CF_METADATA), 
                                       Bytes.toBytes("status"));
        if (status != null) {
            data.setStatus(Bytes.toString(status));
        }

        return data;
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
