package com.bruce.jr.prometheus;

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * http请求打点
 *
 * @author 吕胜 lvheng1
 * @date 2023/7/21
 **/
public class PrometheusHttpFilter implements Filter {
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		Filter.super.init(filterConfig);
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		
		// 获取api url
		String path = ((HttpServletRequest) request).getRequestURI();
		// 获取请求方法
		String    method     = ((HttpServletRequest) request).getMethod();
		long      timeMillis = System.currentTimeMillis();
		LocalDate now        = LocalDate.now();
		
		List<Tag> tags = new ArrayList<>();
		tags.add(new ImmutableTag("path", path));
		tags.add(new ImmutableTag("method", method));
		tags.add(new ImmutableTag("day", now.toString()));
		
		
		try {
			chain.doFilter(request, response);
		} catch (Exception e) {
			// 请求失败次数加1
			Metrics.counter("http_request_error", tags).increment();
			throw e;
		} finally {
			long f = System.currentTimeMillis();
			long l = f - timeMillis;
			//记录请求响应时间
			Metrics.timer("http_request_time", tags).record(l, TimeUnit.MILLISECONDS);
			
			// 请求次数加1
			//自定义的指标名称：http_request_test_all，指标包含数据
			tags.add(new ImmutableTag("status", String.valueOf(((HttpServletResponse) response).getStatus())));
			Metrics.counter("http_request_total", tags).increment();
			
		}
	}
	
	@Override
	public void destroy() {
		Filter.super.destroy();
	}
}
