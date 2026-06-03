-- ============================================================
-- MadRabbit Web 漏洞靶场系统 - 一键部署数据库初始化脚本
-- 数据库：madrabbit
-- 生成时间：2026-05-26
-- 说明：本脚本包含系统运行所需的全部表结构与初始数据
--       已排除废弃表（roles, permissions, role_permissions,
--       vulnerability_types, levels, user_progress, challenge_progress）
-- 使用方式：mysql -u root -p < install_init.sql
-- ============================================================

-- ============================================================
-- 第一部分：创建数据库
-- ============================================================

CREATE DATABASE IF NOT EXISTS madrabbit CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE madrabbit;

-- ============================================================
-- 第二部分：创建表结构
-- ============================================================

-- -----------------------------------------------------------
-- 2.1 用户表
-- 用途：系统用户账户，被登录认证、越权关卡、注入关卡等广泛使用
-- 关联关卡：auth-session/level1~3, access-control, injection/level1
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（明文存储，用于教学演示）',
    password_md5 VARCHAR(32) COMMENT '密码MD5值（用于暴力破解关卡验证）',
    email VARCHAR(100) COMMENT '邮箱地址',
    role VARCHAR(20) DEFAULT 'LEARNER' COMMENT '用户角色：ADMIN/LEARNER',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '用户状态：ACTIVE/INACTIVE',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_role (role),
    INDEX idx_status (status)
) COMMENT '用户表';

-- -----------------------------------------------------------
-- 2.2 关卡 Flag 与状态管理表
-- 用途：管理所有漏洞类型的关卡进度和 Flag 验证
-- 替代了已废弃的 challenge_progress 表
-- 关联服务：FlagService（核心状态查询与Flag验证）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS flags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键 ID',
    vul_type VARCHAR(100) NOT NULL COMMENT '漏洞大类名称（如 injection, xss, csrf 等）',
    vul_level VARCHAR(50) NOT NULL COMMENT '关卡名称（如 level0, level1 等）',
    flag VARCHAR(100) COMMENT 'Flag: flag{16位字母数字组合}，level0和终结关为NULL',
    status VARCHAR(20) DEFAULT '未开始' COMMENT '关卡状态：未开始、进行中、已完成',
    UNIQUE KEY uk_vul_type_level (vul_type, vul_level)
) COMMENT '关卡 Flag 和状态管理表';

-- -----------------------------------------------------------
-- 2.3 SQL 注入关卡数据表
-- 用途：MyBatis 注入关卡（level3~level8）的查询目标数据
-- 关联关卡：injection/level3(Age), level4(Name), level5(Like),
--           level6(Table), level7(Order), level8(IN), level9(Blind)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS challenge_users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '用户名',
    age INT NOT NULL COMMENT '年龄',
    email VARCHAR(100) COMMENT '邮箱',
    role VARCHAR(20) DEFAULT 'user' COMMENT '角色',
    department VARCHAR(50) COMMENT '部门'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'SQL注入关卡-用户数据表';

-- -----------------------------------------------------------
-- 2.4 日志表（logs_access）
-- 用途：MyBatis 动态表名注入关卡（injection/level6）
-- 攻击者通过构造表名参数读取不同的日志表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS logs_access (
    id INT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '时间戳',
    user_id INT COMMENT '用户ID',
    action VARCHAR(100) COMMENT '操作类型',
    ip_address VARCHAR(45) COMMENT 'IP地址',
    status VARCHAR(20) COMMENT '状态'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '访问日志表（表名注入关卡）';

-- -----------------------------------------------------------
-- 2.5 日志表（logs_error）
-- 用途：与 logs_access 配合，供 injection/level6 动态表名注入使用
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS logs_error (
    id INT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '时间戳',
    error_code VARCHAR(10) COMMENT '错误码',
    message VARCHAR(255) COMMENT '错误信息',
    source VARCHAR(100) COMMENT '来源',
    severity VARCHAR(20) COMMENT '严重程度'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '错误日志表（表名注入关卡）';

-- -----------------------------------------------------------
-- 2.6 商家订单表
-- 用途：越权关卡（access-control level1~level3）的订单数据
-- 关联关卡：level1(水平越权), level2(垂直越权), level3(IDOR)
-- 每个用户4条订单数据，merchant_id 关联 users.id
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS merchant_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(20) NOT NULL UNIQUE COMMENT '订单编号(YYYYMMDD+5位随机)',
    merchant_id BIGINT NOT NULL COMMENT '商家ID(关联users.id)',
    order_time DATETIME NOT NULL COMMENT '订单时间',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    recipient_name VARCHAR(50) NOT NULL COMMENT '收件人姓名',
    recipient_phone VARCHAR(20) NOT NULL COMMENT '收件人手机号',
    recipient_address VARCHAR(200) NOT NULL COMMENT '收件人地址',
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_order_no (order_no)
) COMMENT '商家订单表（越权关卡）';

-- ============================================================
-- 第三部分：插入初始数据
-- ============================================================

-- -----------------------------------------------------------
-- 3.1 用户数据
-- 密码统一为 123456，MD5值为 e10adc3949ba59abbe56e057f20f883e
-- tom 的密码为 123456（暴力破解关卡目标）
-- lucy 的密码为 123456（密码重置关卡目标）
-- -----------------------------------------------------------
INSERT IGNORE INTO users (username, password, password_md5, email, role, status) VALUES
('admin',      '123456', 'e10adc3949ba59abbe56e057f20f883e', 'admin@madrabbit.com',      'ADMIN',   'ACTIVE'),
('jack',       '123456', 'e10adc3949ba59abbe56e057f20f883e', 'jack@madrabbit.com',       'LEARNER', 'ACTIVE'),
('lucy',       '123456', 'e10adc3949ba59abbe56e057f20f883e', 'lucy@madrabbit.com',       'LEARNER', 'ACTIVE'),
('tom',        '123456', 'e10adc3949ba59abbe56e057f20f883e', 'tom@madrabbit.com',        'LEARNER', 'ACTIVE'),
('lili',       '123456', 'e10adc3949ba59abbe56e057f20f883e', 'lili@madrabbit.com',       'LEARNER', 'ACTIVE'),
('hanmeimei',  '123456', 'e10adc3949ba59abbe56e057f20f883e', 'hanmeimei@madrabbit.com',  'LEARNER', 'ACTIVE');

-- -----------------------------------------------------------
-- 3.2 关卡 Flag 数据
-- 说明：Flag 值以 FlagService.java 中的 initMissingFlags() 为准
--       level0 为学习关（无 Flag），终结关也无 Flag
--       应用启动时 FlagService 会通过 INSERT IGNORE 自动补齐缺失行
-- -----------------------------------------------------------

-- === 认证与会话安全 (auth-session) ===
-- 关卡：level0-获取秘籍, level1-弱密码破解, level2-任意密码重置,
--       level3-JWT令牌安全, level4-终结
INSERT IGNORE INTO flags (vul_type, vul_level, flag, status) VALUES
('auth-session', 'level0', NULL, '未开始'),
('auth-session', 'level1', 'flag{Brut3F0rc3M4st3r}', '未开始'),
('auth-session', 'level2', 'flag{AuthByp4ssK3y}', '未开始'),
('auth-session', 'level3', 'flag{JWT_W34k_K3y_BrUt3}', '未开始'),
('auth-session', 'level4', NULL, '未开始');

-- === SQL 注入 (injection) ===
-- 关卡：level0-获取秘籍, level1-登录注入, level2-联合查询,
--       level3-Age注入, level4-Name注入, level5-Like注入,
--       level6-表名注入, level7-Order注入, level8-IN注入,
--       level9-盲注, level10-终结
INSERT IGNORE INTO flags (vul_type, vul_level, flag, status) VALUES
('injection', 'level0',  NULL, '未开始'),
('injection', 'level1',  'flag{SQL_Inj3ct10n_L0g1n}', '未开始'),
('injection', 'level2',  'flag{SQL_Un10n_D4t4_L34k}', '未开始'),
('injection', 'level3',  'flag{MyB4t1s_Ag3_Inj3ct}', '未开始'),
('injection', 'level4',  'flag{MyB4t1s_N4m3_Inj3ct}', '未开始'),
('injection', 'level5',  'flag{MyB4t1s_L1k3_Inj3ct}', '未开始'),
('injection', 'level6',  'flag{MyB4t1s_T4bl3_Inj3ct}', '未开始'),
('injection', 'level7',  'flag{MyB4t1s_0rd3r_Inj3ct}', '未开始'),
('injection', 'level8',  'flag{MyB4t1s_1N_Inj3ct}', '未开始'),
('injection', 'level9',  'flag{Bl1nd_SQL_Inj3ct}', '未开始'),
('injection', 'level10', NULL, '未开始');

-- === CSRF 跨站请求伪造 (csrf) ===
-- 关卡：level0-获取秘籍, level1-GET型CSRF, level2-POST型CSRF,
--       level3-Token无效绑定, level4-终结
INSERT IGNORE INTO flags (vul_type, vul_level, flag, status) VALUES
('csrf', 'level0', NULL, '未开始'),
('csrf', 'level1', 'flag{G3T_CSRF_Expl01t}', '未开始'),
('csrf', 'level2', 'flag{P0ST_CSRF_Byp4ss}', '未开始'),
('csrf', 'level3', 'flag{CSRF_T0k3n_N0t_B0und}', '未开始'),
('csrf', 'level4', 'flag{CSRF_Pr0t3ct10n_Pr0}', '未开始');

-- === SSRF 服务器端请求伪造 (ssrf) ===
-- 关卡：level0-获取秘籍, level1-基础SSRF, level2-协议利用,
--       level3-过滤绕过, level4-重定向绕过, level5-终结
INSERT IGNORE INTO flags (vul_type, vul_level, flag, status) VALUES
('ssrf', 'level0', NULL, '未开始'),
('ssrf', 'level1', 'flag{SSRF_B4s1c_Expl01t}', '未开始'),
('ssrf', 'level2', 'flag{SSRF_Pr0t0c0l_Abus3}', '未开始'),
('ssrf', 'level3', 'flag{SSRF_F1lt3r_Byp4ss}', '未开始'),
('ssrf', 'level4', 'flag{SSRF_R3d1r3ct_Byp4ss}', '未开始'),
('ssrf', 'level5', 'flag{SSRF_D3f3ns3_Pr0}', '未开始');

-- === 命令执行 (rce) ===
-- 关卡：level0-获取秘籍, level1-命令注入, level2-SpEL注入,
--       level3-过滤绕过, level4-终结
INSERT IGNORE INTO flags (vul_type, vul_level, flag, status) VALUES
('rce', 'level0', NULL, '未开始'),
('rce', 'level1', 'flag{Cmd_Inj3ct10n_Pwn3d}', '未开始'),
('rce', 'level2', 'flag{SpEL_C0d3_Inj3ct}', '未开始'),
('rce', 'level3', 'flag{RC3_F1lt3r_Byp4ss}', '未开始'),
('rce', 'level4', 'flag{RC3_D3f3ns3_Pr0}', '未开始');

-- === 访问控制 (access-control) ===
-- 关卡：level0-获取秘籍, level1-水平越权, level2-垂直越权,
--       level3-IDOR, level4-终结
INSERT IGNORE INTO flags (vul_type, vul_level, flag, status) VALUES
('access-control', 'level0', NULL, '未开始'),
('access-control', 'level1', 'flag{H0r1z0nt4l_Pr1v_Esc}', '未开始'),
('access-control', 'level2', 'flag{V3rt1c4l_Pr1v_Esc}', '未开始'),
('access-control', 'level3', 'flag{IDOR_D1r3ct_Acc3ss}', '未开始'),
('access-control', 'level4', 'flag{Acc3ss_C0ntr0l_Pr0}', '未开始');

-- === 文件操作 (file-operation) ===
-- 关卡：level0-获取秘籍, level1-文件上传, level2-路径穿越,
--       level3-文件包含, level4-终结
INSERT IGNORE INTO flags (vul_type, vul_level, flag, status) VALUES
('file-operation', 'level0', NULL, '未开始'),
('file-operation', 'level1', 'flag{F1l3_Upl04d_Byp4ss}', '未开始'),
('file-operation', 'level2', 'flag{P4th_Tr4v3rs4l_Pwn}', '未开始'),
('file-operation', 'level3', 'flag{F1l3_1nclus10n_Exp}', '未开始'),
('file-operation', 'level4', 'flag{F1l3_0p_D3f3ns3_Pr0}', '未开始');

-- === 安全配置 (security-config) ===
-- 关卡：level0-获取秘籍, level1-Actuator端点暴露, level2-Swagger文档泄露,
--       level3-终结
INSERT IGNORE INTO flags (vul_type, vul_level, flag, status) VALUES
('security-config', 'level0', NULL, '未开始'),
('security-config', 'level1', 'flag{Actu4t0r_3nv_L34k}', '未开始'),
('security-config', 'level2', 'flag{Sw4gg3r_AP1_D0cs_Exp0s3d}', '未开始'),
('security-config', 'level3', NULL, '未开始');

-- === 敏感信息泄露 (info-leak) ===
-- 关卡：level0-获取秘籍, level1-错误信息泄露, level2-硬编码密钥,
--       level3-Git历史泄露, level4-终结
INSERT IGNORE INTO flags (vul_type, vul_level, flag, status) VALUES
('info-leak', 'level0', NULL, '未开始'),
('info-leak', 'level1', 'flag{3rr0r_Msg_L34k_Inf0}', '未开始'),
('info-leak', 'level2', 'flag{H4rdc0d3d_S3cr3t_Exp0s3d}', '未开始'),
('info-leak', 'level3', 'flag{G1t_H1st0ry_S3cr3t_Exp}', '未开始'),
('info-leak', 'level4', NULL, '未开始');

-- === 业务逻辑 (business-logic) ===
-- 关卡：level0-获取秘籍, level1-价格篡改, level2-优惠券滥用,
--       level3-流程跳过, level4-终结
INSERT IGNORE INTO flags (vul_type, vul_level, flag, status) VALUES
('business-logic', 'level0', NULL, '未开始'),
('business-logic', 'level1', 'flag{Pr1c3_T4mp3r_Pwn}', '未开始'),
('business-logic', 'level2', 'flag{C0up0n_4bus3_Exp}', '未开始'),
('business-logic', 'level3', 'flag{Pr0c3ss_Sk1p_Byp4ss}', '未开始'),
('business-logic', 'level4', 'flag{B1z_L0g1c_D3f3ns3_Pr0}', '未开始');

-- === 反序列化 (deserialization) ===
-- 关卡：level0-获取秘籍, level1-Java原生反序列化, level2-Fastjson,
--       level3-Log4Shell, level4-终结
INSERT IGNORE INTO flags (vul_type, vul_level, flag, status) VALUES
('deserialization', 'level0', NULL, '未开始'),
('deserialization', 'level1', 'flag{J4va_D3s3r_Gadg3t_RCE}', '未开始'),
('deserialization', 'level2', 'flag{F4stjs0n_Aut0Typ3_RCE}', '未开始'),
('deserialization', 'level3', 'flag{L0g4Sh3ll_JNDI_Inj3ct}', '未开始'),
('deserialization', 'level4', NULL, '未开始');

-- === XXE (xxe) ===
-- 关卡：level0-获取秘籍, level1-文件读取, level2-OOB外带,
--       level3-Content-Type切换, level4-终结
INSERT IGNORE INTO flags (vul_type, vul_level, flag, status) VALUES
('xxe', 'level0', NULL, '未开始'),
('xxe', 'level1', 'flag{XX3_F1l3_R34d_Pwn3d}', '未开始'),
('xxe', 'level2', 'flag{XX3_00B_Ext4ct10n}', '未开始'),
('xxe', 'level3', 'flag{XX3_C0nt3nt_Typ3_Sw1tch}', '未开始'),
('xxe', 'level4', NULL, '未开始');

-- === XSS 跨站脚本 (xss) ===
-- 关卡：level0-获取秘籍, level1-反射型XSS, level2-存储型XSS,
--       level3-DOM型XSS, level4-终结
INSERT IGNORE INTO flags (vul_type, vul_level, flag, status) VALUES
('xss', 'level0', NULL, '未开始'),
('xss', 'level1', 'flag{R3fl3ct3d_XSS_M4st3r}', '未开始'),
('xss', 'level2', 'flag{St0r3d_XSS_Hunt3r}', '未开始'),
('xss', 'level3', 'flag{D0M_XSS_Expl0r3r}', '未开始'),
('xss', 'level4', 'flag{XSS_Pr0t3ct10n_Pr0}', '未开始');

-- -----------------------------------------------------------
-- 3.3 SQL 注入关卡用户数据
-- 包含隐藏的 superadmin 用户 Eve，用于教学演示
-- -----------------------------------------------------------
INSERT IGNORE INTO challenge_users (id, name, age, email, role, department) VALUES
(1, 'Alice',   25, 'alice@example.com',   'user',       'engineering'),
(2, 'Bob',     30, 'bob@example.com',     'user',       'marketing'),
(3, 'Charlie', 35, 'charlie@example.com',  'admin',      'engineering'),
(4, 'Diana',   28, 'diana@example.com',   'user',       'hr'),
(5, 'Eve',     22, 'eve_secret@internal.com', 'superadmin', 'security'),
(6, 'Frank',   32, 'frank@example.com',   'user',       'engineering'),
(7, 'Grace',   27, 'grace@example.com',   'moderator',  'support'),
(8, 'Henry',   40, 'henry@example.com',   'user',       'finance');

-- -----------------------------------------------------------
-- 3.4 访问日志数据（injection/level6 表名注入）
-- -----------------------------------------------------------
INSERT IGNORE INTO logs_access (user_id, action, ip_address, status) VALUES
(1, 'LOGIN',        '192.168.1.100', 'SUCCESS'),
(2, 'VIEW_PROFILE', '192.168.1.101', 'SUCCESS'),
(3, 'ADMIN_ACCESS', '10.0.0.1',      'SUCCESS'),
(1, 'LOGOUT',       '192.168.1.100', 'SUCCESS'),
(5, 'ADMIN_ACCESS', '172.16.0.50',   'DENIED');

-- -----------------------------------------------------------
-- 3.5 错误日志数据（injection/level6 表名注入）
-- -----------------------------------------------------------
INSERT IGNORE INTO logs_error (error_code, message, source, severity) VALUES
('E001', 'Database connection timeout',  'db-pool',      'HIGH'),
('E002', 'Invalid authentication token',  'auth-service', 'MEDIUM'),
('E003', 'File not found: config.yml',    'config-loader','LOW'),
('E004', 'Memory limit exceeded',         'worker-3',     'CRITICAL'),
('E005', 'SSL certificate expired',       'nginx-proxy',  'HIGH');

-- -----------------------------------------------------------
-- 3.6 商家订单数据（access-control 越权关卡）
-- 每个用户 4 条订单，merchant_id 对应 users.id
-- -----------------------------------------------------------

-- admin (merchant_id=1) 的订单
INSERT IGNORE INTO merchant_orders (order_no, merchant_id, order_time, product_name, price, recipient_name, recipient_phone, recipient_address) VALUES
('2026050112345', 1, '2026-05-01 09:15:00', '企业级路由器',   2899.00, '王建国', '13900001111', '北京市朝阳区望京SOHO T1'),
('2026050356789', 1, '2026-05-03 14:30:00', '服务器硬盘4TB',  1599.00, '李明辉', '13900002222', '北京市海淀区中关村软件园'),
('2026050623456', 1, '2026-05-06 10:45:00', '网络交换机',     3299.00, '赵德华', '13900003333', '北京市西城区金融街甲9号'),
('2026050978123', 1, '2026-05-09 16:20:00', '机柜42U',        4500.00, '孙志强', '13900004444', '北京市大兴区亦庄经济开发区');

-- jack (merchant_id=2) 的订单
INSERT IGNORE INTO merchant_orders (order_no, merchant_id, order_time, product_name, price, recipient_name, recipient_phone, recipient_address) VALUES
('2026050234561', 2, '2026-05-02 11:20:00', '蓝牙耳机Pro',    599.00,  '张小龙', '13811112222', '上海市浦东新区张江高科技园'),
('2026050467892', 2, '2026-05-04 15:40:00', '机械键盘红轴',   459.00,  '陈思远', '13811113333', '上海市徐汇区漕河泾开发区'),
('2026050712345', 2, '2026-05-07 09:00:00', '电竞显示器27寸', 2199.00, '黄嘉豪', '13811114444', '上海市静安区南京西路1601号'),
('2026051045678', 2, '2026-05-10 13:25:00', '无线充电板',     129.00,  '周文婷', '13811115555', '上海市黄浦区淮海中路333号');

-- lucy (merchant_id=3) 的订单
INSERT IGNORE INTO merchant_orders (order_no, merchant_id, order_time, product_name, price, recipient_name, recipient_phone, recipient_address) VALUES
('2026050345672', 3, '2026-05-03 08:30:00', '瑜伽垫加厚款',   168.00,  '林小美', '13722221111', '广州市天河区珠江新城花城大道'),
('2026050578901', 3, '2026-05-05 12:15:00', '运动水壶1L',     89.00,   '吴丽华', '13722222222', '广州市越秀区北京路步行街'),
('2026050812346', 3, '2026-05-08 17:50:00', '跑步手表GPS',    1399.00, '王晓芳', '13722223333', '广州市番禺区万博CBD'),
('2026051134567', 3, '2026-05-11 10:10:00', '健身弹力带套装', 59.00,   '郑美玲', '13722224444', '广州市白云区白云大道北');

-- tom (merchant_id=4) 的订单
INSERT IGNORE INTO merchant_orders (order_no, merchant_id, order_time, product_name, price, recipient_name, recipient_phone, recipient_address) VALUES
('2026050456783', 4, '2026-05-04 10:00:00', '编程书籍套装',   299.00,  '刘学文', '13633331111', '深圳市南山区科技园南路'),
('2026050689012', 4, '2026-05-06 14:30:00', '人体工学椅',     1899.00, '杨志豪', '13633332222', '深圳市福田区华强北路'),
('2026050923457', 4, '2026-05-09 11:45:00', 'USB扩展坞',      259.00,  '马瑞霖', '13633333333', '深圳市宝安区前海自贸区'),
('2026051256780', 4, '2026-05-12 16:00:00', '降噪耳机头戴式', 899.00,  '徐浩然', '13633334444', '深圳市龙岗区坂田街道');

-- lili (merchant_id=5) 的订单
INSERT IGNORE INTO merchant_orders (order_no, merchant_id, order_time, product_name, price, recipient_name, recipient_phone, recipient_address) VALUES
('2026050567894', 5, '2026-05-05 09:30:00', '手工皂礼盒',     128.00,  '陈小玲', '13544441111', '杭州市西湖区文三路'),
('2026050789013', 5, '2026-05-07 13:00:00', '香薰蜡烛套装',   199.00,  '赵雅琪', '13544442222', '杭州市余杭区未来科技城'),
('2026051012348', 5, '2026-05-10 08:20:00', '真丝眼罩',       79.00,   '孙梦瑶', '13544443333', '杭州市滨江区网商路'),
('2026051345679', 5, '2026-05-13 15:40:00', '桌面加湿器',     149.00,  '王思雨', '13544444444', '杭州市拱墅区大关路');

-- hanmeimei (merchant_id=6) 的订单
INSERT IGNORE INTO merchant_orders (order_no, merchant_id, order_time, product_name, price, recipient_name, recipient_phone, recipient_address) VALUES
('2026050678905', 6, '2026-05-06 11:00:00', '绘画颜料套装',   239.00,  '胡艺文', '13455551111', '成都市高新区天府软件园'),
('2026050890124', 6, '2026-05-08 14:15:00', '数位板手写板',   699.00,  '郭小芳', '13455552222', '成都市武侯区科华北路'),
('2026051123459', 6, '2026-05-11 09:50:00', '素描本A3',       45.00,   '钱伟杰', '13455553333', '成都市锦江区春熙路'),
('2026051456782', 6, '2026-05-14 17:30:00', '马克笔60色',     189.00,  '冯晓燕', '13455554444', '成都市青羊区宽窄巷子');

-- ============================================================
-- 第四部分：初始化结果验证
-- ============================================================

SELECT '=========================================' AS '';
SELECT 'Database initialization completed!' AS 'Status';
SELECT '=========================================' AS '';
SELECT COUNT(*) AS 'Total Users' FROM users;
SELECT COUNT(*) AS 'Total Flags' FROM flags;
SELECT vul_type, COUNT(*) AS level_count FROM flags GROUP BY vul_type ORDER BY vul_type;
SELECT COUNT(*) AS 'Total Challenge Users' FROM challenge_users;
SELECT COUNT(*) AS 'Total Access Logs' FROM logs_access;
SELECT COUNT(*) AS 'Total Error Logs' FROM logs_error;
SELECT COUNT(*) AS 'Total Merchant Orders' FROM merchant_orders;
