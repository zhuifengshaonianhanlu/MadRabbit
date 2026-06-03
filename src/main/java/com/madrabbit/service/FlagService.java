package com.madrabbit.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flag 服务
 * 管理关卡状态和 Flag 验证
 */
@Service
public class FlagService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 应用启动时自动补齐缺失的 flag 数据行。
     * 使用 INSERT IGNORE 确保：已有行保持原状（不会重置进度），仅插入缺失的行。
     */
    @PostConstruct
    public void initMissingFlags() {
        String[][] allFlags = {
            // ssrf
            {"ssrf", "level0", null},
            {"ssrf", "level1", "flag{SSRF_B4s1c_Expl01t}"},
            {"ssrf", "level2", "flag{SSRF_Pr0t0c0l_Abus3}"},
            {"ssrf", "level3", "flag{SSRF_F1lt3r_Byp4ss}"},
            {"ssrf", "level4", "flag{SSRF_R3d1r3ct_Byp4ss}"},
            {"ssrf", "level5", "flag{SSRF_D3f3ns3_Pr0}"},
            // injection
            {"injection", "level0", null},
            {"injection", "level1", "flag{SQL_Inj3ct10n_L0g1n}"},
            {"injection", "level2", "flag{SQL_Un10n_D4t4_L34k}"},
            {"injection", "level3", "flag{MyB4t1s_Ag3_Inj3ct}"},
            {"injection", "level4", "flag{MyB4t1s_N4m3_Inj3ct}"},
            {"injection", "level5", "flag{MyB4t1s_L1k3_Inj3ct}"},
            {"injection", "level6", "flag{MyB4t1s_T4bl3_Inj3ct}"},
            {"injection", "level7", "flag{MyB4t1s_0rd3r_Inj3ct}"},
            {"injection", "level8", "flag{MyB4t1s_1N_Inj3ct}"},
            {"injection", "level9", "flag{Bl1nd_SQL_Inj3ct}"},
            {"injection", "level10", null},
            // csrf
            {"csrf", "level0", null},
            {"csrf", "level1", "flag{G3T_CSRF_Expl01t}"},
            {"csrf", "level2", "flag{P0ST_CSRF_Byp4ss}"},
            {"csrf", "level3", "flag{CSRF_T0k3n_N0t_B0und}"},
            {"csrf", "level4", "flag{CSRF_Pr0t3ct10n_Pr0}"},
            // rce
            {"rce", "level0", null},
            {"rce", "level1", "flag{Cmd_Inj3ct10n_Pwn3d}"},
            {"rce", "level2", "flag{SpEL_C0d3_Inj3ct}"},
            {"rce", "level3", "flag{RC3_F1lt3r_Byp4ss}"},
            {"rce", "level4", "flag{RC3_D3f3ns3_Pr0}"},
            // access-control
            {"access-control", "level0", null},
            {"access-control", "level1", "flag{H0r1z0nt4l_Pr1v_Esc}"},
            {"access-control", "level2", "flag{V3rt1c4l_Pr1v_Esc}"},
            {"access-control", "level3", "flag{IDOR_D1r3ct_Acc3ss}"},
            {"access-control", "level4", "flag{Acc3ss_C0ntr0l_Pr0}"},
            // file-operation
            {"file-operation", "level0", null},
            {"file-operation", "level1", "flag{F1l3_Upl04d_Byp4ss}"},
            {"file-operation", "level2", "flag{P4th_Tr4v3rs4l_Pwn}"},
            {"file-operation", "level3", "flag{F1l3_1nclus10n_Exp}"},
            {"file-operation", "level4", "flag{F1l3_0p_D3f3ns3_Pr0}"},
            // security-config
            {"security-config", "level0", null},
            {"security-config", "level1", "flag{Actu4t0r_3nv_L34k}"},
            {"security-config", "level2", "flag{Sw4gg3r_AP1_D0cs_Exp0s3d}"},
            {"security-config", "level3", null},
            // info-leak
            {"info-leak", "level0", null},
            {"info-leak", "level1", "flag{3rr0r_Msg_L34k_Inf0}"},
            {"info-leak", "level2", "flag{H4rdc0d3d_S3cr3t_Exp0s3d}"},
            {"info-leak", "level3", "flag{G1t_H1st0ry_S3cr3t_Exp}"},
            {"info-leak", "level4", null},
            // business-logic
            {"business-logic", "level0", null},
            {"business-logic", "level1", "flag{Pr1c3_T4mp3r_Pwn}"},
            {"business-logic", "level2", "flag{C0up0n_4bus3_Exp}"},
            {"business-logic", "level3", "flag{Pr0c3ss_Sk1p_Byp4ss}"},
            {"business-logic", "level4", "flag{B1z_L0g1c_D3f3ns3_Pr0}"},
            // deserialization
            {"deserialization", "level0", null},
            {"deserialization", "level1", "flag{J4va_D3s3r_Gadg3t_RCE}"},
            {"deserialization", "level2", "flag{F4stjs0n_Aut0Typ3_RCE}"},
            {"deserialization", "level3", "flag{L0g4Sh3ll_JNDI_Inj3ct}"},
            {"deserialization", "level4", null},
            // xxe
            {"xxe", "level0", null},
            {"xxe", "level1", "flag{XX3_F1l3_R34d_Pwn3d}"},
            {"xxe", "level2", "flag{XX3_00B_Ext4ct10n}"},
            {"xxe", "level3", "flag{XX3_C0nt3nt_Typ3_Sw1tch}"},
            {"xxe", "level4", null},
            // xss
            {"xss", "level0", null},
            {"xss", "level1", "flag{R3fl3ct3d_XSS_M4st3r}"},
            {"xss", "level2", "flag{St0r3d_XSS_Hunt3r}"},
            {"xss", "level3", "flag{D0M_XSS_Expl0r3r}"},
            {"xss", "level4", "flag{XSS_Pr0t3ct10n_Pr0}"},
        };

        String sql = "INSERT IGNORE INTO flags (vul_type, vul_level, flag, status) VALUES (?, ?, ?, '未开始')";
        int inserted = 0;
        for (String[] row : allFlags) {
            try {
                int affected = jdbcTemplate.update(sql, row[0], row[1], row[2]);
                if (affected > 0) inserted++;
            } catch (Exception e) {
                // 忽略单行插入异常，继续处理后续行
            }
        }
        if (inserted > 0) {
            System.out.println("[FlagService] Auto-initialized " + inserted + " missing flag rows.");
        }
    }

    /**
     * 获取关卡状态
     */
    public Map<String, Object> getStatus(String vulType, String vulLevel) {
        try {
            String sql = "SELECT id, vul_type, vul_level, status FROM flags WHERE vul_type = ? AND vul_level = ?";
            List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, vulType, vulLevel);
            
            if (!records.isEmpty()) {
                return records.get(0);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 更新关卡状态
     */
    @Transactional
    public boolean updateStatus(String vulType, String vulLevel, String status) {
        try {
            String sql = "UPDATE flags SET status = ? WHERE vul_type = ? AND vul_level = ?";
            int rows = jdbcTemplate.update(sql, status, vulType, vulLevel);
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 验证 Flag
     */
    public boolean checkFlag(String vulType, String vulLevel, String submittedFlag) {
        try {
            String sql = "SELECT flag FROM flags WHERE vul_type = ? AND vul_level = ?";
            List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, vulType, vulLevel);
            
            if (!records.isEmpty()) {
                String correctFlag = (String) records.get(0).get("flag");
                return correctFlag != null && correctFlag.equals(submittedFlag);
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取关卡 Flag（用于登录成功后返回）
     */
    public String getFlag(String vulType, String vulLevel) {
        try {
            String sql = "SELECT flag FROM flags WHERE vul_type = ? AND vul_level = ?";
            List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, vulType, vulLevel);
            
            if (!records.isEmpty()) {
                return (String) records.get(0).get("flag");
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取某漏洞类型的所有关卡进度
     * GET /api/challenge/progress_get?vul_type={vul_type_name}
     */
    public Map<String, Object> getProgress(String vulType) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String sql = "SELECT id, vul_type, vul_level, status FROM flags WHERE vul_type = ? ORDER BY id";
            List<Map<String, Object>> challenges = jdbcTemplate.queryForList(sql, vulType);
            
            int completedCount = 0;
            for (Map<String, Object> challenge : challenges) {
                if ("已完成".equals(challenge.get("status"))) {
                    completedCount++;
                }
            }
            
            result.put("success", true);
            result.put("challenges", challenges);
            result.put("completedCount", completedCount);
            result.put("totalCount", challenges.size());
            result.put("progressPercent", challenges.size() > 0 ? (completedCount * 100 / challenges.size()) : 0);
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Failed to get progress: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取所有漏洞类型的关卡进度概览（简化版）
     */
    public Map<String, Object> getAllProgress() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String sql = "SELECT vul_type, COUNT(*) as total_count, " +
                         "SUM(CASE WHEN status = '已完成' THEN 1 ELSE 0 END) as completed_count " +
                         "FROM flags GROUP BY vul_type ORDER BY vul_type";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            
            List<Map<String, Object>> data = new java.util.ArrayList<>();
            for (Map<String, Object> row : rows) {
                Map<String, Object> item = new HashMap<>();
                int totalCount = ((Number) row.get("total_count")).intValue();
                int completedCount = ((Number) row.get("completed_count")).intValue();
                
                item.put("vulType", row.get("vul_type"));
                item.put("totalCount", totalCount);
                item.put("completedCount", completedCount);
                item.put("progressPercent", totalCount > 0 ? (completedCount * 100 / totalCount) : 0);
                data.add(item);
            }
            
            result.put("success", true);
            result.put("data", data);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Failed to get all progress: " + e.getMessage());
        }
        
        return result;
    }
}
