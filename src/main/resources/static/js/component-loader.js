/**
 * 组件加载器 - 用于加载公共组件（头部、底部等）
 */

/**
 * 加载 HTML 组件
 * @param {string} url - 组件 URL
 * @param {string} targetSelector - 目标容器选择器
 * @returns {Promise<void>}
 */
async function loadComponent(url, targetSelector) {
    try {
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(`Failed to load component: ${url}`);
        }
        const html = await response.text();
        const targetElement = document.querySelector(targetSelector);
        if (targetElement) {
            targetElement.innerHTML = html;
        } else {
            console.error(`Target element not found: ${targetSelector}`);
        }
    } catch (error) {
        console.error(`Error loading component ${url}:`, error);
    }
}

/**
 * 加载所有公共组件
 */
async function loadCommonComponents() {
    // 加载头部导航
    await loadComponent('/components/head.html', '#navbar-container');
    
    // 加载底部装饰
    await loadComponent('/components/foot.html', '#footer-container');
    
    // 组件加载完成后立即初始化（不使用 setTimeout）
    await initCommonComponents();
    
    // 标记组件已加载完成
    window.__componentsLoaded = true;
    
    // 触发组件加载完成事件
    window.dispatchEvent(new CustomEvent('componentsLoaded'));
    
    // 调用页面初始化回调（如果存在）
    if (typeof window.onPageInit === 'function') {
        window.onPageInit();
    }
}

/**
 * 初始化公共组件
 */
async function initCommonComponents() {
    // 初始化用户信息
    initUserInfo();
    
    // 初始化语言选择器（等待翻译加载完成）
    await initLanguageSelector();
    
    // 绑定全局事件
    bindGlobalEvents();
}

/**
 * 初始化用户信息
 */
function initUserInfo() {
    const usernameElement = document.getElementById('username');
    const user = getCurrentUser();  // getCurrentUser() 已经返回解析后的对象
    
    if (user) {
        // 更新用户名显示：账号(角色)
        if (usernameElement) {
            const roleDisplay = user.role === 'ADMIN' ? 'Admin' : 'Learner';
            usernameElement.textContent = `${user.username || 'Guest'} (${roleDisplay})`;
        }
        
    } else {
        // 如果没有登录，重定向到登录页面
        if (window.location.pathname !== '/login.html') {
            window.location.href = '/login.html';
        }
        if (usernameElement) {
            usernameElement.textContent = 'Guest';
        }
    }
}

/**
 * 切换用户下拉菜单
 */
function toggleUserDropdown() {
    const dropdown = document.querySelector('.user-dropdown');
    if (dropdown) {
        dropdown.classList.toggle('active');
    }
}

/**
 * 登出
 */
function logout() {
    // 清除本地存储
    clearAuthInfo();
    
    // 调用后端登出接口（可选）
    fetch(getApiUrl('/auth/logout'), {
        method: 'POST',
        headers: {
            'Authorization': getToken() || ''
        }
    }).catch(err => console.error('登出接口调用失败:', err));
    
    // 重定向到登录页面
    window.location.href = '/login.html';
}

/**
 * 绑定全局事件
 */
function bindGlobalEvents() {
    // 点击外部关闭下拉菜单
    document.addEventListener('click', function(event) {
        // 关闭用户下拉菜单
        const dropdown = document.querySelector('.user-dropdown');
        if (dropdown && !dropdown.contains(event.target)) {
            dropdown.classList.remove('active');
        }
        
        // 关闭语言下拉菜单
        const langSelector = document.querySelector('.language-selector');
        if (langSelector && !langSelector.contains(event.target)) {
            langSelector.classList.remove('active');
        }
    });
}

/**
 * 初始化语言选择器
 */
async function initLanguageSelector() {
    // 获取当前语言
    let currentLang = 'en-US'; // 默认英文
    
    // 从 localStorage 获取语言设置
    const storedLang = localStorage.getItem('lang');
    if (storedLang && ['en-US', 'zh-CN'].includes(storedLang)) {
        currentLang = storedLang;
    }
    
    // 更新显示
    updateLanguageDisplay(currentLang);
    
    // 确保 i18nManager 已初始化翻译
    if (typeof i18nManager !== 'undefined') {
        // 如果翻译还没加载，等待加载完成
        if (!i18nManager.translationsLoaded) {
            await i18nManager.init();
        } else {
            // 已加载，直接刷新页面内容
            i18nManager.refreshPageContent();
        }
    }
}

/**
 * 更新语言显示
 */
function updateLanguageDisplay(lang) {
    // 更新按钮文字
    const currentLangText = document.getElementById('currentLangText');
    if (currentLangText) {
        currentLangText.textContent = lang === 'zh-CN' ? '中文' : 'English';
    }
    
    // 更新选中状态
    document.querySelectorAll('.lang-option').forEach(option => {
        if (option.getAttribute('data-lang') === lang) {
            option.classList.add('selected');
        } else {
            option.classList.remove('selected');
        }
    });
}

/**
 * 切换语言下拉菜单
 */
function toggleLanguageDropdown(event) {
    event.stopPropagation();
    const selector = document.querySelector('.language-selector');
    if (selector) {
        selector.classList.toggle('active');
    }
}

/**
 * 选择语言
 */
async function selectLanguage(lang) {
    // 关闭下拉菜单
    const selector = document.querySelector('.language-selector');
    if (selector) {
        selector.classList.remove('active');
    }
    
    // 更新显示
    updateLanguageDisplay(lang);
    
    // 调用 i18nManager 切换语言（会保存到 localStorage、加载翻译、刷新页面）
    if (typeof i18nManager !== 'undefined') {
        await i18nManager.setLanguage(lang);
    }
}

// 页面加载完成后自动加载组件
document.addEventListener('DOMContentLoaded', function() {
    loadCommonComponents();
});
