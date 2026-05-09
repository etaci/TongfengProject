package com.tongfeng.backend.app;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

	public static final String TRACE_ID_HEADER = "X-Trace-Id";
	private static final String MDC_KEY = "traceId";

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String incomingTraceId = request.getHeader(TRACE_ID_HEADER);
		String traceId = incomingTraceId == null || incomingTraceId.isBlank()
				? UUID.randomUUID().toString()
				: incomingTraceId.trim();
		String path = request.getRequestURI();

		MDC.put(MDC_KEY, traceId);
		RequestTraceContext.set(traceId, path);
		response.setHeader(TRACE_ID_HEADER, traceId);
		try {
			filterChain.doFilter(request, response);
		} finally {
			RequestTraceContext.clear();
			MDC.remove(MDC_KEY);
		}
	}
}
