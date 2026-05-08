package com.iot.hbase.rowkey;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * RowKey生成器单元测试
 */
public class RowKeyGeneratorTest {

    @Test
    public void testGenerateRowKey() {
        String deviceId = "device_001";
        long timestamp = System.currentTimeMillis();

        byte[] rowKey = RowKeyGenerator.generateRowKey(deviceId, timestamp);

        assertNotNull(rowKey);
        assertTrue(rowKey.length > 0);
        System.out.println("✓ RowKey生成成功，长度: " + rowKey.length);
    }

    @Test
    public void testParseRowKey() {
        String deviceId = "device_001";
        long timestamp = 1704067200000L;

        byte[] rowKey = RowKeyGenerator.generateRowKey(deviceId, timestamp);
        RowKeyGenerator.RowKeyInfo info = RowKeyGenerator.parseRowKey(rowKey);

        assertEquals(deviceId, info.deviceId);
        assertEquals(timestamp, info.timestamp);
        System.out.println("✓ RowKey解析成功: " + info);
    }

    @Test
    public void testSaltDistribution() {
        int[] saltCount = new int[256];

        for (int i = 0; i < 1000; i++) {
            String deviceId = "device_" + i;
            byte[] rowKey = RowKeyGenerator.generateRowKey(deviceId, System.currentTimeMillis());
            int salt = rowKey[0] & 0xFF;
            saltCount[salt]++;
        }

        int nonZeroCount = 0;
        for (int count : saltCount) {
            if (count > 0) nonZeroCount++;
        }

        System.out.println("✓ 1000个设备的盐值分布: " + nonZeroCount + "/256");
        assertTrue(nonZeroCount > 200);
    }

    @Test
    public void testRowKeySorting() {
        String deviceId = "device_001";
        long time1 = 1704067200000L;
        long time2 = 1704070800000L;

        byte[] rowKey1 = RowKeyGenerator.generateRowKey(deviceId, time1);
        byte[] rowKey2 = RowKeyGenerator.generateRowKey(deviceId, time2);

        int cmp = compareBytes(rowKey1, rowKey2);
        System.out.println("✓ RowKey排序测试: 较晚的时间应排在前面");
        assertTrue(cmp > 0);
    }

    @Test
    public void testTimeAwareRowKey() {
        String deviceId = "device_001";
        long timestamp = System.currentTimeMillis();

        byte[] rowKey1 = RowKeyGenerator.generateRowKey(deviceId, timestamp);
        byte[] rowKey2 = RowKeyGenerator.generateRowKeyTimeAware(deviceId, timestamp);

        System.out.println("✓ 时间感知RowKey生成成功");
        assertNotNull(rowKey1);
        assertNotNull(rowKey2);
    }

    @Test
    public void testDateShardinRowKey() {
        String deviceId = "device_001";
        long timestamp = 1704067200000L;

        String rowKey = RowKeyGenerator.generateRowKeyWithDateSharding(deviceId, timestamp);
        System.out.println("✓ 日期分片RowKey: " + rowKey);

        assertTrue(rowKey.contains("20240101"));
        assertTrue(rowKey.contains("device_001"));
    }

    private int compareBytes(byte[] a, byte[] b) {
        int minLen = Math.min(a.length, b.length);
        for (int i = 0; i < minLen; i++) {
            if (a[i] != b[i]) {
                return (a[i] & 0xFF) - (b[i] & 0xFF);
            }
        }
        return a.length - b.length;
    }
}
