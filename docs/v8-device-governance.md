# V8 设备接入治理接口说明

本版本新增两类能力：

1. 设备目录能力
2. 设备治理能力

核心目标：

- 前端可先拉取可接入设备目录，再引导用户绑定
- 后端按设备类型、厂商、型号进行白名单校验
- 后端按设备能力限制可同步的指标类型
- 前端可通过设备总览页直接展示每台设备的同步状态

## 1. 设备目录

接口：

`GET /api/v1/devices/catalog`

返回字段：

- `profileCode`
- `deviceType`
- `deviceTypeName`
- `vendorName`
- `deviceModel`
- `supportedMetricTypes`
- `bindingHint`

当前内置目录：

- `URIC_ACID_METER / Tongfeng / UA-1`：支持 `URIC_ACID`
- `SMART_WATER_CUP / Tongfeng / CUP-1`：支持 `HYDRATION`
- `WEIGHT_SCALE / Tongfeng / SCALE-1`：支持 `WEIGHT`
- `BLOOD_PRESSURE_MONITOR / Tongfeng / BP-1`：支持 `BLOOD_PRESSURE`
- `HEALTH_COMBO_STATION / Tongfeng / STATION-1`：支持 `URIC_ACID / HYDRATION / WEIGHT / BLOOD_PRESSURE`

## 2. 设备绑定

接口：

`POST /api/v1/devices`

请求示例：

```json
{
  "deviceType": "HEALTH_COMBO_STATION",
  "vendorName": "Tongfeng",
  "deviceModel": "STATION-1",
  "serialNumber": "STATION-SN-001",
  "aliasName": "family-health-station"
}
```

返回新增字段：

- `vendorProfileCode`
- `supportedMetricTypes`

失败场景：

- `DEVICE_TYPE_UNSUPPORTED`
- `DEVICE_VENDOR_UNSUPPORTED`
- `DEVICE_MODEL_UNSUPPORTED`
- `DEVICE_EXISTS`

## 3. 设备列表

接口：

`GET /api/v1/devices`

前端可直接使用：

- `vendorProfileCode` 渲染设备档案标签
- `supportedMetricTypes` 渲染该设备可同步的指标标签

## 4. 设备总览

接口：

`GET /api/v1/devices/overview`

返回字段：

- `totalDevices`
- `activeDevices`
- `recentlySyncedDevices`
- `attentionDevices`
- `devices`

单台设备字段：

- `deviceCode`
- `aliasName`
- `deviceType`
- `deviceTypeName`
- `vendorName`
- `deviceModel`
- `status`
- `supportedMetricTypes`
- `totalSyncCount`
- `latestMetricType`
- `latestSummary`
- `syncHealthStatus`
- `lastSyncedAt`

`syncHealthStatus` 当前取值：

- `ACTIVE`
- `NEVER_SYNCED`
- `STALE`
- `UNBOUND`

## 5. 设备同步白名单

接口：

`POST /api/v1/devices/{deviceCode}/sync`

规则：

- 后端会先校验 `metricType` 是否属于平台支持范围
- 再校验当前设备是否允许同步该指标
- 不允许时返回 `DEVICE_METRIC_NOT_ALLOWED`

例如：

- `URIC_ACID_METER` 只能同步 `URIC_ACID`
- `BLOOD_PRESSURE_MONITOR` 只能同步 `BLOOD_PRESSURE`
- `HEALTH_COMBO_STATION` 可同步全部四类指标

## 6. 组合设备同步示例

```json
{
  "items": [
    {
      "metricType": "URIC_ACID",
      "externalEventId": "evt-ua-1",
      "measuredAt": "2026-04-29T08:00:00Z",
      "value": 502,
      "unit": "umol/L",
      "note": "device sync"
    },
    {
      "metricType": "HYDRATION",
      "externalEventId": "evt-hyd-1",
      "measuredAt": "2026-04-29T09:00:00Z",
      "waterIntakeMl": 900,
      "urineColorLevel": 4,
      "note": "smart cup sync"
    },
    {
      "metricType": "WEIGHT",
      "externalEventId": "evt-weight-1",
      "measuredAt": "2026-04-29T10:00:00Z",
      "value": 71.8,
      "unit": "kg",
      "note": "smart scale sync"
    },
    {
      "metricType": "BLOOD_PRESSURE",
      "externalEventId": "evt-bp-1",
      "measuredAt": "2026-04-29T10:05:00Z",
      "systolicPressure": 162,
      "diastolicPressure": 101,
      "pulseRate": 88,
      "unit": "mmHg",
      "note": "bp monitor sync"
    }
  ]
}
```

## 7. 非法同步示例

如果把血压数据同步到尿酸仪：

```json
{
  "items": [
    {
      "metricType": "BLOOD_PRESSURE",
      "externalEventId": "evt-bp-blocked-1",
      "measuredAt": "2026-04-29T10:05:00Z",
      "systolicPressure": 142,
      "diastolicPressure": 91,
      "pulseRate": 80,
      "unit": "mmHg"
    }
  ]
}
```

后端会返回：

- `code = DEVICE_METRIC_NOT_ALLOWED`
