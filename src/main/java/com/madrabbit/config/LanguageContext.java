package com.madrabbit.config;

/**
 * 语言上下文 - ThreadLocal 存储当前请求的语言
 */
public class LanguageContext {
    
    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();
    
    public static void set(String language) {
        CONTEXT.set(language);
    }
    
    public static String get() {
        return CONTEXT.get();
    }
    
    public static void clear() {
        CONTEXT.remove();
    }
}
