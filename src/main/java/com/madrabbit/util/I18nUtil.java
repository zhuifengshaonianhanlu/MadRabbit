package com.madrabbit.util;

import com.madrabbit.config.LanguageContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 国际化工具类
 */
@Component
public class I18nUtil {
    
    @Autowired
    private MessageSource messageSource;
    
    /**
     * 获取当前语言的 Locale
     */
    public Locale getCurrentLocale() {
        String language = LanguageContext.get();
        if (language == null) {
            return Locale.US;
        }
        
        String[] parts = language.split("-");
        return new Locale(parts[0], parts.length > 1 ? parts[1] : "");
    }
    
    /**
     * 获取国际化消息
     */
    public String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, getCurrentLocale());
    }
    
    /**
     * 获取国际化消息（带默认值）
     */
    public String getMessage(String code, String defaultMessage, Object... args) {
        return messageSource.getMessage(code, args, defaultMessage, getCurrentLocale());
    }
}
