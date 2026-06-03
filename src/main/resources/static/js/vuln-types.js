/**
 * MadRabbit 漏洞分类体系 - 交互逻辑
 */

// ====================================
// 页面加载初始化
// ====================================
document.addEventListener('DOMContentLoaded', function() {
    // 用户信息初始化由 component-loader.js 处理
    
    // 创建粒子效果
    createParticles();
    
    // 绑定卡片点击事件
    bindCardEvents();
    
    // 添加入场动画
    triggerEntranceAnimation();
});

// ====================================
// 创建粒子效果
// ====================================
function createParticles() {
    const particlesContainer = document.getElementById('particles');
    const particleCount = 30;
    
    for (let i = 0; i < particleCount; i++) {
        const particle = document.createElement('div');
        particle.className = 'particle';
        
        // 随机位置
        particle.style.left = Math.random() * 100 + '%';
        
        // 随机颜色
        const colors = ['#00f3ff', '#ff006e', '#bc13fe', '#00ff88', '#ff9500'];
        particle.style.background = colors[Math.floor(Math.random() * colors.length)];
        
        // 随机大小
        const size = Math.random() * 4 + 2;
        particle.style.width = size + 'px';
        particle.style.height = size + 'px';
        
        // 随机延迟和持续时间
        particle.style.animationDelay = Math.random() * 15 + 's';
        particle.style.animationDuration = (Math.random() * 10 + 10) + 's';
        
        particlesContainer.appendChild(particle);
    }
}

// ====================================
// 绑定卡片事件
// ====================================
function bindCardEvents() {
    const cards = document.querySelectorAll('.vuln-card');
    
    cards.forEach((card, index) => {
        // 鼠标悬停效果增强
        card.addEventListener('mouseenter', function(e) {
            const rect = card.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;
            
            // 更新发光位置
            const glow = card.querySelector('.card-glow');
            if (glow) {
                glow.style.background = `radial-gradient(circle at ${x}px ${y}px, rgba(255, 255, 255, 0.1), transparent 70%)`;
            }
        });
        
        // 点击事件
        card.addEventListener('click', function() {
            const vulnType = this.getAttribute('data-type');
            navigateToVulnType(vulnType);
        });
        
        // 按钮点击事件（阻止冒泡）
        const enterBtn = card.querySelector('.enter-btn');
        if (enterBtn) {
            enterBtn.addEventListener('click', function(e) {
                e.stopPropagation();
                const vulnType = card.getAttribute('data-type');
                navigateToVulnType(vulnType);
            });
        }
    });
}

// ====================================
// 导航到漏洞类型页面
// ====================================
function navigateToVulnType(vulnType) {
    // 添加点击动画效果
    const card = document.querySelector(`[data-type="${vulnType}"]`);
    if (card) {
        card.style.transform = 'scale(0.95)';
        setTimeout(() => {
            card.style.transform = '';
        }, 200);
    }
    
    // TODO: 实际项目中应该跳转到对应的漏洞类型详情页
    // window.location.href = `/levels/${vulnType}`;
    
    // 临时显示提示信息
    showNotification(`即将进入：${getVulnTypeName(vulnType)}`, 'info');
}

// ====================================
// 获取漏洞类型中文名称
// ====================================
function getVulnTypeName(vulnType) {
    const names = {
        'account-security': '账号安全',
        'xss': 'XSS 跨站脚本攻击',
        'sql-injection': 'SQL 注入',
        'csrf': 'CSRF 跨站请求伪造',
        'ssrf': 'SSRF 服务器端请求伪造',
        'command-execution': '命令执行'
    };
    return names[vulnType] || vulnType;
}

// ====================================
// 入场动画
// ====================================
function triggerEntranceAnimation() {
    const cards = document.querySelectorAll('.vuln-card');
    
    cards.forEach((card, index) => {
        // 初始状态
        card.style.opacity = '0';
        card.style.transform = 'translateY(50px)';
        
        // 依次显示
        setTimeout(() => {
            card.style.transition = 'all 0.6s cubic-bezier(0.175, 0.885, 0.32, 1.275)';
            card.style.opacity = '1';
            card.style.transform = 'translateY(0)';
        }, index * 150);
    });
}

// ====================================
// 通知提示
// ====================================
function showNotification(message, type = 'info') {
    // 创建通知元素
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.innerHTML = `
        <div class="notification-content">
            <i class="fas fa-${getNotificationIcon(type)}"></i>
            <span>${message}</span>
        </div>
    `;
    
    // 添加样式
    notification.style.cssText = `
        position: fixed;
        top: 100px;
        right: 20px;
        background: rgba(10, 14, 26, 0.95);
        border-left: 4px solid ${getNotificationColor(type)};
        padding: 1rem 1.5rem;
        border-radius: 4px;
        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
        z-index: 2000;
        animation: slideInRight 0.4s ease;
        backdrop-filter: blur(10px);
    `;
    
    document.body.appendChild(notification);
    
    // 自动移除
    setTimeout(() => {
        notification.style.animation = 'slideOutRight 0.4s ease';
        setTimeout(() => {
            notification.remove();
        }, 400);
    }, 3000);
}

function getNotificationIcon(type) {
    const icons = {
        'info': 'info-circle',
        'success': 'check-circle',
        'warning': 'exclamation-triangle',
        'error': 'times-circle'
    };
    return icons[type] || 'info-circle';
}

function getNotificationColor(type) {
    const colors = {
        'info': '#00f3ff',
        'success': '#00ff88',
        'warning': '#ff9500',
        'error': '#ff2a2a'
    };
    return colors[type] || '#00f3ff';
}

// ====================================
// 键盘快捷键支持
// ====================================
document.addEventListener('keydown', function(e) {
    // ESC 键登出
    if (e.key === 'Escape') {
        const confirmLogout = confirm('确定要退出系统吗？(按 ESC 确认)');
        if (confirmLogout) {
            logout();
        }
    }
    
    // 数字键 1-6 快速选择漏洞类型
    if (e.key >= '1' && e.key <= '6') {
        const cards = document.querySelectorAll('.vuln-card');
        const index = parseInt(e.key) - 1;
        if (cards[index]) {
            const vulnType = cards[index].getAttribute('data-type');
            navigateToVulnType(vulnType);
        }
    }
});

// ====================================
// 鼠标移动视差效果
// ====================================
document.addEventListener('mousemove', function(e) {
    const cards = document.querySelectorAll('.vuln-card');
    const mouseX = e.clientX / window.innerWidth;
    const mouseY = e.clientY / window.innerHeight;
    
    cards.forEach(card => {
        const moveX = (mouseX - 0.5) * 10;
        const moveY = (mouseY - 0.5) * 10;
        
        // 轻微移动卡片创造深度感
        card.style.transform = `translate(${moveX * 0.5}px, ${moveY * 0.5}px)`;
    });
});

// ====================================
// 性能优化 - 防抖函数
// ====================================
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// ====================================
// 响应式处理
// ====================================
const handleResize = debounce(() => {
    const isMobile = window.innerWidth <= 768;
    document.body.classList.toggle('mobile-view', isMobile);
}, 250);

window.addEventListener('resize', handleResize);
