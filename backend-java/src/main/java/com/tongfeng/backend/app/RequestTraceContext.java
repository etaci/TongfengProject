package com.tongfeng.backend.app;

public final class RequestTraceContext {

	private static final ThreadLocal<String> TRACE_ID_HOLDER = new ThreadLocal<>();
	private static final ThreadLocal<String> PATH_HOLDER = new ThreadLocal<>();

	private RequestTraceContext() {
	}

	public static void set(String traceId, String path) {
		TRACE_ID_HOLDER.set(traceId);
		PATH_HOLDER.set(path);
	}

	public static String traceId() {
		return TRACE_ID_HOLDER.get();
	}

	public static String path() {
		return PATH_HOLDER.get();
	}

	public static void clear() {
		TRACE_ID_HOLDER.remove();
		PATH_HOLDER.remove();
	}
}
