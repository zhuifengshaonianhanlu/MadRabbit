/**
 * 全局配置文件
 * 
 * 说明：
 * 1. 本配置文件用于统一管理后端 API 地址等全局变量
 * 2. BASE_URL 默认使用当前页面所在的主机地址（window.location.origin），
 *    这样无论通过 localhost、127.0.0.1 还是真实 IP 访问，API 请求都会自动指向当前服务器
 * 3. 如需指定其他后端地址（如前后端分离部署），可手动修改 BASE_URL
 * 4. 所有前端页面都应使用这些全局变量，而不是硬编码地址
 */

// ====================================
// 后端 API 基础 URL 配置
// ====================================
const API_CONFIG = {
    // 后端服务的基础 URL（默认使用当前页面所在主机地址，适配 localhost / 127.0.0.1 / 真实IP）
    // 如果前后端分离部署，可改为具体地址，如 'http://192.168.1.100:8080'
    BASE_URL: window.location.origin,
    
    // API 路径前缀
    API_PREFIX: '/api',
    
    // 完整的 API 基础 URL
    get API_BASE_URL() {
        return this.BASE_URL + this.API_PREFIX;
    }
};

// ====================================
// 应用配置
// ====================================
const APP_CONFIG = {
    // 应用名称
    APP_NAME: 'MadRabbit web-VulnHub Platform',
    
    // 应用版本
    VERSION: '1.0.0',
    
    // Token 存储键名
    TOKEN_KEY: 'token',
    
    // 用户信息存储键名
    USER_INFO_KEY: 'currentUser'
};

// ====================================
// 工具函数
// ====================================

/**
 * 获取完整的 API URL
 * @param {string} path - API 路径（不需要包含 /api 前缀）
 * @returns {string} 完整的 API URL
 */
function getApiUrl(path) {
    // 如果 path 已经包含了 /api 前缀，直接拼接 BASE_URL
    if (path.startsWith('/api')) {
        return API_CONFIG.BASE_URL + path;
    }
    // 否则添加 /api 前缀
    return API_CONFIG.API_BASE_URL + path;
}

/**
 * 获取当前 Token
 * @returns {string|null} Token 字符串
 */
function getToken() {
    return localStorage.getItem(APP_CONFIG.TOKEN_KEY);
}

/**
 * 获取当前用户信息
 * @returns {Object|null} 用户信息对象
 */
function getCurrentUser() {
    const userInfo = localStorage.getItem(APP_CONFIG.USER_INFO_KEY);
    if (!userInfo) {
        return null;
    }
    try {
        return JSON.parse(userInfo);
    } catch (e) {
        console.error('解析用户信息失败:', e);
        return null;
    }
}

/**
 * 检查是否已登录
 * @returns {boolean} 是否已登录
 */
function isLoggedIn() {
    return !!getToken() && !!getCurrentUser();
}

/**
 * 登出并清除本地存储
 */
function clearAuthInfo() {
    localStorage.removeItem(APP_CONFIG.TOKEN_KEY);
    localStorage.removeItem(APP_CONFIG.USER_INFO_KEY);
}

// ====================================
// 全局 fetch 拦截器
// ====================================

/**
 * 全局 fetch 拦截器
 * 1. 自动为 /api/challenge/ 请求注入 Authorization header（JWT Token）
 * 2. 检测 Token 过期（HTTP 401）时自动清除认证信息并跳转登录页
 */
(function() {
    const originalFetch = window.fetch;
    
    window.fetch = function(...args) {
        // ---- 自动注入 JWT Token ----
        // 对 /api/challenge/ 开头的请求，如果未手动设置 Authorization，则自动补充
        if (args.length >= 1 && typeof args[0] === 'string') {
            const url = args[0];
            if (url.includes('/api/challenge/')) {
                const token = getToken();
                if (token) {
                    // 合并或创建 headers
                    let options = args[1] || {};
                    let headers = options.headers || {};
                    
                    // 如果 headers 是 Headers 对象，转为普通对象
                    if (headers instanceof Headers) {
                        const plainHeaders = {};
                        headers.forEach((value, key) => { plainHeaders[key] = value; });
                        headers = plainHeaders;
                    }
                    
                    // 仅当未手动设置 Authorization 时才自动注入
                    const hasAuth = Object.keys(headers).some(k => k.toLowerCase() === 'authorization');
                    if (!hasAuth) {
                        headers['Authorization'] = 'Bearer ' + token;
                        options.headers = headers;
                        args[1] = options;
                    }
                }
            }
        }
        
        return originalFetch.apply(this, args).then(response => {
            // 检测 401 状态码（Token 过期或未授权）
            if (response.status === 401) {
                // 如果当前已在登录页，不处理
                if (window.location.pathname === '/login.html') {
                    return response;
                }
                
                // 克隆响应以便读取body后仍可返回原始响应
                const clonedResponse = response.clone();
                
                clonedResponse.json().then(data => {
                    if (data && data.success === false && 
                        (data.message === 'Token 无效或已过期' || data.message === '未授权访问，请先登录')) {
                        // 清除本地存储的认证信息
                        localStorage.removeItem(APP_CONFIG.TOKEN_KEY);
                        localStorage.removeItem(APP_CONFIG.USER_INFO_KEY);
                        
                        // 如果是 iframe 内的页面，通知父页面跳转
                        if (window.parent !== window) {
                            window.parent.location.href = '/login.html';
                        } else {
                            window.location.href = '/login.html';
                        }
                    }
                }).catch(() => {
                    // JSON 解析失败时，仅根据 401 状态码处理
                    localStorage.removeItem(APP_CONFIG.TOKEN_KEY);
                    localStorage.removeItem(APP_CONFIG.USER_INFO_KEY);
                    if (window.parent !== window) {
                        window.parent.location.href = '/login.html';
                    } else {
                        window.location.href = '/login.html';
                    }
                });
            }
            
            return response;
        });
    };
})();
