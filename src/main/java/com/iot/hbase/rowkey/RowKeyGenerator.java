package com.iot.hbase.rowkey;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * IoT数据RowKey生成器 - 支持3种设计方案
 * 
 * 方案A (推荐): salt + reverse_timestamp + device_id
 * - 优点: 最新数据排在前面, 盐值分散热点
 * - RowKey结构: [1字节盐值] + [8字节反转时间戳] + [N字节设备ID]
 */
public class RowKeyGenerator {

    /**
     * 方案A: 基于设备ID哈希的盐值 (推荐用于设备数量多的场景)
     * 
     * RowKey = salt(1字节) + reverse_timestamp(8字节) + device_id(N字节)
     * 
     * @param deviceId 设备ID
     * @param timestamp 时间戳(毫秒)
     * @return RowKey字节数组
     */
    public static byte[] generateRowKey(String deviceId, long timestamp) {
        // 生成盐值: 根据设备ID的哈希值取模
        int salt = Math.abs(deviceId.hashCode() % 256);

        // 反转时间戳: 确保最新时间的数据排在前面
        long reverseTimestamp = Long.MAX_VALUE - timestamp;

        // 构建RowKey
        ByteBuffer buffer = ByteBuffer.allocate(1 + 8 + deviceId.length());
        buffer.put((byte) salt);                    // 1字节盐值
        buffer.putLong(reverseTimestamp);           // 8字节反转时间戳
        buffer.put(deviceId.getBytes(StandardCharsets.UTF_8)); // N字节设备ID

        return buffer.array();
    }

    /**
     * 方案B: 时间感知的动态盐值
     * 
     * RowKey = salt(1字节) + reverse_timestamp(8字节) + device_id(N字节)
     * salt = (deviceId.hash + timestamp/1000) % 256
     * 
     * 优点: 相同设备在不同时间会有不同的盐值, 进一步分散热点
     * 
     * @param deviceId 设备ID
     * @param timestamp 时间戳(毫秒)
     * @return RowKey字节数组
     */
    public static byte[] generateRowKeyTimeAware(String deviceId, long timestamp) {
        // 时间感知盐值
        long timeSegment = timestamp / 1000; // 按秒分组
        int salt = (int) ((Math.abs(deviceId.hashCode()) + timeSegment) % 256);

        // 反转时间戳
        long reverseTimestamp = Long.MAX_VALUE - timestamp;

        // 构建RowKey
        ByteBuffer buffer = ByteBuffer.allocate(1 + 8 + deviceId.length());
        buffer.put((byte) salt);
        buffer.putLong(reverseTimestamp);
        buffer.put(deviceId.getBytes(StandardCharsets.UTF_8));

        return buffer.array();
    }

    /**
     * 方案C: 日期分片方案 (推荐用于数据归档场景)
     * 
     * RowKey = YYYYMMDD + "|" + hash_bucket(1字节) + "|" + device_id + "|" + timestamp
     * 
     * 优点: 便于按日期删除数据, 避免数据跨越多个Region
     * 
     * @param deviceId 设备ID
     * @param timestamp 时间戳(毫秒)
     * @return RowKey字符串
     */
    public static String generateRowKeyWithDateSharding(String deviceId, long timestamp) {
        // 生成日期前缀
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String datePrefix = sdf.format(new Date(timestamp));

        // 生成哈希桶 (0-255)
        int hashBucket = Math.abs(deviceId.hashCode() % 256);

        // 组合RowKey
        return String.format("%s|%03d|%s|%d", 
            datePrefix, 
            hashBucket, 
            deviceId, 
            timestamp);
    }

    /**
     * 解析RowKey信息
     * 
     * @param rowKey RowKey字节数组
     * @return RowKeyInfo对象
     */
    public static RowKeyInfo parseRowKey(byte[] rowKey) {
        ByteBuffer buffer = ByteBuffer.wrap(rowKey);

        // 读取盐值
        byte salt = buffer.get();

        // 读取反转时间戳
        long reverseTimestamp = buffer.getLong();
        long timestamp = Long.MAX_VALUE - reverseTimestamp;

        // 读取设备ID
        byte[] deviceIdBytes = new byte[buffer.remaining()];
        buffer.get(deviceIdBytes);
        String deviceId = new String(deviceIdBytes, StandardCharsets.UTF_8);

        return new RowKeyInfo(salt, timestamp, deviceId);
    }

    /**
     * RowKey信息类
     */
    public static class RowKeyInfo {
        public byte salt;
        public long timestamp;
        public String deviceId;

        public RowKeyInfo(byte salt, long timestamp, String deviceId) {
            this.salt = salt;
            this.timestamp = timestamp;
            this.deviceId = deviceId;
        }

        @Override
        public String toString() {
            return String.format("RowKeyInfo{salt=%d, timestamp=%d, deviceId='%s'}", 
                salt, timestamp, deviceId);
        }
    }
}
