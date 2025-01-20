package com.bruce.jr.prometheus.config;

import com.bruce.jr.prometheus.PrometheusHttpFilter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * 需要的配置bean
 *
 * @author 吕胜 lvheng1
 * @date 2023/7/1
 **/
@Slf4j
@Configuration
public class PrometheusConfig {

    @Value("#{'${prometheus.http.filter.url.pattern:/*}'.split(',')}")
    private String[] urlPattern;

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            registry.config().meterFilter(
                new MeterFilter() {
                    @Override
                    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                        if (id.getType() == Meter.Type.TIMER) {
                            return DistributionStatisticConfig.builder()
                                .percentilesHistogram(true)
                                .percentiles(0.5, 0.90, 0.95, 0.99)
                                // 加了这个会导致quantile为0，why？
                                /*.sla(Duration.ofMillis(50).toNanos(),
                                     Duration.ofMillis(100).toNanos(),
                                     Duration.ofMillis(500).toNanos(),
                                     Duration.ofSeconds(1).toNanos(),
                                     Duration.ofSeconds(5).toNanos())
                                .minimumExpectedValue(Duration.ofMillis(1).toNanos())*/
                                // 时间段且访问量小的情况下，可能算不出来
                                .expiry(Duration.ofMinutes(5))
                                .build()
                                .merge(config);
                        } else {
                            return config;
                        }
                    }
                });
            try {
                String hostAddress = getIpAddress();
                log.info("current ip is:{}", hostAddress);
                registry.config().commonTags("instance", hostAddress);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        };
    }

    public static String getIpAddress() {
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress                   ip               = null;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface)allNetInterfaces.nextElement();
                if (netInterface.isLoopback() || netInterface.isVirtual() || !netInterface.isUp()) {
                    continue;
                } else {
                    Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        ip = addresses.nextElement();
                        if (ip != null && ip instanceof Inet4Address) {
                            return ip.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return "ip not found";
    }

/*    @Bean
    public FilterRegistrationBean<PrometheusHttpFilter> prometheusHttpFilter() {
        FilterRegistrationBean<PrometheusHttpFilter> singleSignOutFilterBean = new FilterRegistrationBean<>();
        singleSignOutFilterBean.setFilter(new PrometheusHttpFilter());
        singleSignOutFilterBean.setOrder(-1);
        singleSignOutFilterBean.addUrlPatterns(this.urlPattern);
        return singleSignOutFilterBean;
    }*/
}
