package com.iot.hbase.model;

/**
 * IoT设备数据模型
 */
public class IotData {
    private String deviceId;
    private String deviceType;
    private String deviceName;
    private String location;
    private double temperature;
    private double humidity;
    private double pressure;
    private int signalStrength;
    private long reportTime;
    private String status;
    private int errorCode;

    // 构造函数
    public IotData() {}

    public IotData(String deviceId, String deviceType, double temperature, 
                   double humidity, long reportTime) {
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.temperature = temperature;
        this.humidity = humidity;
        this.reportTime = reportTime;
        this.status = "NORMAL";
        this.errorCode = 0;
    }

    // Getter/Setter
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }

    public double getPressure() { return pressure; }
    public void setPressure(double pressure) { this.pressure = pressure; }

    public int getSignalStrength() { return signalStrength; }
    public void setSignalStrength(int signalStrength) { this.signalStrength = signalStrength; }

    public long getReportTime() { return reportTime; }
    public void setReportTime(long reportTime) { this.reportTime = reportTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getErrorCode() { return errorCode; }
    public void setErrorCode(int errorCode) { this.errorCode = errorCode; }

    @Override
    public String toString() {
        return String.format("IotData{deviceId='%s', temp=%.2f, humidity=%.2f, time=%d}", 
            deviceId, temperature, humidity, reportTime);
    }
}
