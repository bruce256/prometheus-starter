package com.bruce.jr.prometheus.config;

import com.bruce.jr.prometheus.PrometheusHttpFilter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 需要的配置bean
 *
 * @author 吕胜 lvheng1
 * @date 2023/7/1
 **/
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
																  .sla(Duration.ofMillis(50).toNanos(),
																	   Duration.ofMillis(100).toNanos(),
																	   Duration.ofMillis(500).toNanos(),
																	   Duration.ofSeconds(1).toNanos(),
																	   Duration.ofSeconds(5).toNanos())
																  .minimumExpectedValue(Duration.ofMillis(1).toNanos())
																  // 时间段且访问量小的情况下，可能算不出来
																  .expiry(Duration.ofMinutes(5))
																  .build()
																  .merge(config);
							} else {
								return config;
							}
						}
					});
		};
	}
	
	@Bean
	public FilterRegistrationBean<PrometheusHttpFilter> prometheusHttpFilter() {
		FilterRegistrationBean<PrometheusHttpFilter> singleSignOutFilterBean = new FilterRegistrationBean<>();
		singleSignOutFilterBean.setFilter(new PrometheusHttpFilter());
		singleSignOutFilterBean.setOrder(-1);
		singleSignOutFilterBean.addUrlPatterns(this.urlPattern);
		return singleSignOutFilterBean;
	}
}
