package com.mw.ai.agi.openapi.client;

public final class AgiOpenApiRequestContextHolder {
    private static final ThreadLocal<AgiOpenApiRequestContext> CONTEXT = new ThreadLocal<>();

    private AgiOpenApiRequestContextHolder() {
    }

    public static AgiOpenApiRequestContext get() {
        return CONTEXT.get();
    }

    public static void set(AgiOpenApiRequestContext context) {
        CONTEXT.set(context);
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static void runWith(AgiOpenApiRequestContext context, Runnable action) {
        set(context);
        try {
            action.run();
        } finally {
            clear();
        }
    }

    public static <T> T callWith(AgiOpenApiRequestContext context, java.util.concurrent.Callable<T> action) {
        set(context);
        try {
            return action.call();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        } finally {
            clear();
        }
    }
}
