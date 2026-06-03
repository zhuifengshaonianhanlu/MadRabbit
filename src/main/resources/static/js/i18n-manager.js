/**
 * 国际化管理器
 */
class I18nManager {
    constructor() {
        this.currentLang = this.loadLanguage();
        this.translations = {};
        this.translationsLoaded = false;
    }
    
    /**
     * 加载用户语言偏好
     * 注意：优先使用用户明确选择的语言，不自动检测浏览器语言
     */
    loadLanguage() {
        // 首先检查用户是否明确选择了语言
        const stored = localStorage.getItem(I18N_CONFIG.STORAGE_KEY);
        if (stored && I18N_CONFIG.SUPPORTED_LANGUAGES.includes(stored)) {
            return stored;
        }
        
        // 直接返回默认语言，不检测浏览器语言
        return I18N_CONFIG.DEFAULT_LANGUAGE;
    }
    
    /**
     * 加载翻译文件（返回 Promise）
     */
    async loadTranslations() {
        try {
            const response = await fetch(`/i18n/${this.currentLang}.json`);
            if (response.ok) {
                this.translations = await response.json();
                this.translationsLoaded = true;
            }
        } catch (error) {
            console.warn('Failed to load translations, using fallback:', error);
            this.translations = {};
        }
    }
    
    /**
     * 初始化（加载翻译并刷新页面）
     */
    async init() {
        await this.loadTranslations();
        this.refreshPageContent();
    }
    
    /**
     * 设置语言（异步方法）
     */
    async setLanguage(lang) {
        if (!I18N_CONFIG.SUPPORTED_LANGUAGES.includes(lang)) {
            console.warn(`Unsupported language: ${lang}, fallback to ${I18N_CONFIG.DEFAULT_LANGUAGE}`);
            lang = I18N_CONFIG.DEFAULT_LANGUAGE;
        }
        
        this.currentLang = lang;
        localStorage.setItem(I18N_CONFIG.STORAGE_KEY, lang);
        
        // 先加载翻译文件，等待完成
        await this.loadTranslations();
        
        // 翻译加载完成后再刷新页面内容
        this.refreshPageContent();
    }
    
    /**
     * 获取当前语言
     */
    getCurrentLanguage() {
        return this.currentLang;
    }
    
    /**
     * 翻译文本（使用嵌套 key）
     */
    t(key, params = {}) {
        const keys = key.split('.');
        let value = this.translations;
        
        for (const k of keys) {
            value = value?.[k];
            if (!value) {
                // 回退到英文或返回 key 本身
                return key;
            }
        }
        
        // 替换参数
        return this.interpolate(value, params);
    }
    
    /**
     * 插值替换
     */
    interpolate(template, params) {
        return template.replace(/\{(\w+)\}/g, (match, key) => {
            return params[key] !== undefined ? params[key] : match;
        });
    }
    
    /**
     * 刷新页面内容
     */
    refreshPageContent() {
        // 重新翻译所有带有 data-i18n 属性的元素
        document.querySelectorAll('[data-i18n]').forEach(el => {
            const key = el.getAttribute('data-i18n');
            const params = JSON.parse(el.getAttribute('data-i18n-params') || '{}');
            el.textContent = this.t(key, params);
        });
        
        // 翻译 placeholder
        document.querySelectorAll('[data-i18n-placeholder]').forEach(el => {
            const key = el.getAttribute('data-i18n-placeholder');
            el.placeholder = this.t(key);
        });
        
        // 翻译 title
        document.querySelectorAll('[data-i18n-title]').forEach(el => {
            const key = el.getAttribute('data-i18n-title');
            el.title = this.t(key);
        });
        
        // 触发自定义事件，让页面可以响应语言切换
        window.dispatchEvent(new CustomEvent('languageChanged', { 
            detail: { language: this.currentLang } 
        }));
    }
}

// 创建全局实例（初始化由 component-loader.js 统一管理）
const i18nManager = new I18nManager();

// ====================================
// 全局 i18n 辅助函数（立即定义，确保在任何地方可用）
// ====================================

/**
 * 获取翻译文本
 * @param {string} key - 翻译 key
 * @param {object} params - 替换参数
 * @returns {string} 翻译后的文本
 */
function t(key, params) {
    if (typeof i18nManager !== 'undefined') {
        return i18nManager.t(key, params);
    }
    return key;
}

/**
 * 获取当前语言
 * @returns {string} 当前语言代码
 */
function getCurrentLang() {
    if (typeof i18nManager !== 'undefined') {
        return i18nManager.currentLang;
    }
    return localStorage.getItem('lang') || 'en-US';
}

/**
 * 获取 API 请求的语言 Header
 * @returns {object} 包含 Accept-Language 的 header 对象
 */
function getLangHeaders() {
    return {
        'Accept-Language': getCurrentLang()
    };
}

/**
 * 格式化日期（根据当前语言）
 * @param {string} dateString - 日期字符串
 * @returns {string} 格式化后的日期
 */
function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString(getCurrentLang(), {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}
