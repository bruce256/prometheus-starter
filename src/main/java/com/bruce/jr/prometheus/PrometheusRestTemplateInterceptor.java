package com.bruce.jr.prometheus;

import com.alibaba.dubbo.common.Constants;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * RestTemplate请求打点
 *
 * @author 吕胜 lvheng1
 * @date 2023/7/9
 **/
@Slf4j
public class PrometheusRestTemplateInterceptor implements ClientHttpRequestInterceptor {
	
	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		List<Tag> tags = new ArrayList<>();
		tags.add(new ImmutableTag("path", request.getURI().getPath()));
		tags.add(new ImmutableTag("role", Constants.CONSUMER));
		LocalDate now = LocalDate.now();
		tags.add(new ImmutableTag("day", now.toString()));
		long start = System.currentTimeMillis();
		
		ClientHttpResponse response = null;
		try {
			response = execution.execute(request, body);
		} catch (Throwable e) {
			Metrics.counter("http_request_outgoing_error_total", tags).increment();
			throw e;
		} finally {
			if (response != null) {
				tags.add(new ImmutableTag("status", String.valueOf(response.getRawStatusCode())));
			}
			long duration = System.currentTimeMillis() - start;
			Metrics.counter("http_request_outgoing_total", tags).increment();
			
			Metrics.timer("http_request_outgoing_time", tags).record(duration, TimeUnit.MILLISECONDS);
		}
		
		return response;
	}
}
