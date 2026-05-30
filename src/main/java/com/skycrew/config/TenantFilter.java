package com.skycrew.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Servlet filter that extracts the tenant ID from the X-Tenant-Id header
 * and sets it in the TenantContext for the duration of the request.
 */
@Component
@Order(1) // Run before other filters
public class TenantFilter implements Filter {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String tenantId = httpRequest.getHeader(TENANT_HEADER);

            if (tenantId != null && !tenantId.isBlank()) {
                TenantContext.setCurrentTenant(tenantId.trim().toUpperCase());
            } else {
                TenantContext.setCurrentTenant(TenantContext.DEFAULT_TENANT);
            }

            chain.doFilter(request, response);
        } finally {
            TenantContext.clear(); // Prevent thread-local leaks
        }
    }
}
