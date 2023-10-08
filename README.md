# prometheus-starter

## 接入步骤
### 一 应用侧接入
1 maven依赖

```
<dependency>
    <groupId>com.midea.jr</groupId>
    <artifactId>prometheus-starter</artifactId>
    <version>1.0.0</version>
</dependency>

```
2 properties配置

```
## 应用名，sample会自动加上一个 application=portal的label对
management.metrics.tags.application=portal
## actuator暴露出prometheus
management.endpoints.web.exposure.include=prometheus
management.endpoint.health.show-details=always
```

3 dubbo配置

resources下新增META-INF/dubbo文件夹，再新增com.alibaba.dubbo.rpc.Filter文件，文件内添加如下内容

```
prometheusDubboFilter=com.midea.jr.prometheus.PrometheusDubboFilter
```

4 RestTemplate配置

```
List list = Lists.newArrayList(new PrometheusRestTemplateInterceptor());
restTemplate.setInterceptors(list);
```

5 使用@Counted， @Timed 注解，业务自由打点

```
@Timed(value = "applyIdentificationToken", description = "applyIdentificationToken", histogram = true)
@ApiOperation(value = "申请实名token")
@PostMapping("/applyIdentificationToken")
public ApplyTokenResult applyIdentificationToken(@RequestBody @Valid ApplyTokenParamVO applyTokenParamVO){
    ... ...
    return applyTokenResult;
}
```

### 二 Prometheus 配置
增加一个job

```
- job_name: portal
  metrics_path: /actuator/prometheus
  static_configs:
  - targets:
    - 127.0.0.1:8080
```

### 三 grafana 配置
按需配置grafana大盘
