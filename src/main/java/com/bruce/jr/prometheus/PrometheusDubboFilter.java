package com.bruce.jr.prometheus;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.support.RpcUtils;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * dubbo请求打点
 *
 * @author 吕胜 lvheng1
 * @date 2023/7/8
 **/
@Slf4j
@Activate(group = {Constants.PROVIDER, Constants.CONSUMER})
public class PrometheusDubboFilter implements Filter {
	
	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
		RpcContext context     = RpcContext.getContext();
		boolean    isProvider  = context.isProviderSide();
		String     serviceName = invoker.getInterface().getName();
		String     methodName  = RpcUtils.getMethodName(invocation);
		long       start       = System.currentTimeMillis();
		
		List<Tag> tags = new ArrayList<>();
		tags.add(new ImmutableTag("serviceName", serviceName));
		tags.add(new ImmutableTag("method", methodName));
		
		LocalDate now = LocalDate.now();
		tags.add(new ImmutableTag("day", now.toString()));
		if (isProvider) {
			tags.add(new ImmutableTag("role", Constants.PROVIDER));
		} else {
			tags.add(new ImmutableTag("role", Constants.CONSUMER));
			
		}
		
		try {
			// proceed invocation chain
			Result result = invoker.invoke(invocation);
			String status = "success";
			if (result.getException() != null) {
				status = result.getException().getClass().getSimpleName();
			}
			tags.add(new ImmutableTag("status", status));
			Metrics.counter("dubbo_request_total", tags).increment();
			return result;
		} catch (RpcException e) {
			String result = "error";
			if (e.isTimeout()) {
				result = "timeoutError";
			}
			if (e.isBiz()) {
				result = "bizError";
			}
			if (e.isNetwork()) {
				result = "networkError";
			}
			if (e.isSerialization()) {
				result = "serializationError";
			}
			tags.add(new ImmutableTag("status", result));
			Metrics.counter("dubbo_request_error", tags).increment();
			throw e;
		} finally {
			long duration = System.currentTimeMillis() - start;
			Metrics.timer("dubbo_request_time", tags).record(duration, TimeUnit.MILLISECONDS);
		}
	}
}
