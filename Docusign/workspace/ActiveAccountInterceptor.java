package com.example.Docusign.workspace;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

public class ActiveAccountInterceptor implements HandlerInterceptor {
    public static final String SESSION_KEY = "ACTIVE_ACCOUNT_ID";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        Object val = request.getSession(false) != null ? request.getSession(false).getAttribute(SESSION_KEY) : null;
        if (val instanceof Long l) {
            ActiveAccountContext.set(l);
        } else if (val instanceof String s) {
            try { ActiveAccountContext.set(Long.parseLong(s)); } catch (NumberFormatException ignored) {}
        } else {
            ActiveAccountContext.clear();
        }
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, @Nullable Exception ex) {
        ActiveAccountContext.clear();
    }
}
