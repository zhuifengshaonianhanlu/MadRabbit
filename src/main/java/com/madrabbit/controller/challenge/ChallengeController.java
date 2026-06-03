package com.madrabbit.controller.challenge;

import com.madrabbit.service.FlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 挑战关卡状态管理控制器
 * 提供关卡状态查询和 Flag 验证接口
 */
@RestController
@RequestMapping("/api/challenge")
@Tag(name = "挑战关卡管理", description = "关卡状态和 Flag 管理")
public class ChallengeController {

    @Autowired
    private FlagService flagService;

    /**
     * 获取关卡进度
     * GET /api/challenge/progress_get?vul_type={vul_type_name}
     */
    @GetMapping("/progress_get")
    @Operation(summary = "获取进度", description = "返回该类型整体的关卡的完成情况")
    public ResponseEntity<Map<String, Object>> getProgress(
            @RequestParam String vul_type) {
        
        Map<String, Object> result = flagService.getProgress(vul_type);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取所有漏洞类型的关卡进度（批量查询）
     * GET /api/challenge/progress_all
     */
    @GetMapping("/progress_all")
    @Operation(summary = "获取所有进度", description = "一次请求返回所有漏洞类型的关卡完成情况")
    public ResponseEntity<Map<String, Object>> getAllProgress() {
        Map<String, Object> result = flagService.getAllProgress();
        return ResponseEntity.ok(result);
    }

    /**
     * 获取关卡状态
     * GET /api/challenge/status_get?vul_type={vul_type_name}&vul_level={vul_level}
     */
    @GetMapping("/status_get")
    @Operation(summary = "获取关卡状态", description = "根据漏洞类型和关卡返回状态")
    public ResponseEntity<Map<String, Object>> getStatus(
            @RequestParam String vul_type,
            @RequestParam String vul_level) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> data = flagService.getStatus(vul_type, vul_level);
            
            if (data != null) {
                result.put("id", data.get("id"));
                result.put("vul_type", data.get("vul_type"));
                result.put("vul_level", data.get("vul_level"));
                result.put("status", data.get("status"));
            } else {
                result.put("success", false);
                result.put("message", "Challenge not found");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "System error, please try again later");
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 更新关卡状态
     * POST /api/challenge/status_update
     */
    @PostMapping("/status_update")
    @Operation(summary = "更新关卡状态", description = "对当前关卡状态进行更新")
    public ResponseEntity<Map<String, Object>> updateStatus(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String vulType = request.get("vul_type");
            String vulLevel = request.get("vul_level");
            String status = request.get("status");
            
            if (vulType == null || vulLevel == null || status == null) {
                result.put("success", false);
                result.put("message", "Missing required parameters");
                return ResponseEntity.badRequest().body(result);
            }
            
            // 更新关卡状态
            boolean success = flagService.updateStatus(vulType, vulLevel, status);
            
            if (success) {
                // 返回更新后的状态
                Map<String, Object> data = flagService.getStatus(vulType, vulLevel);
                result.put("id", data.get("id"));
                result.put("vul_type", data.get("vul_type"));
                result.put("vul_level", data.get("vul_level"));
                result.put("status", data.get("status"));
                result.put("success", true);
            } else {
                result.put("success", false);
                result.put("message", "Failed to update status");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "System error, please try again later");
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 验证 Flag
     * POST /api/challenge/flag_check
     */
    @PostMapping("/flag_check")
    @Operation(summary = "验证 Flag", description = "对当前关卡的flag进行验证")
    public ResponseEntity<Map<String, Object>> checkFlag(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String vulType = request.get("vul_type");
            String vulLevel = request.get("vul_level");
            String flag = request.get("flag");
            
            if (vulType == null || vulLevel == null || flag == null) {
                result.put("success", false);
                result.put("message", "Missing required parameters");
                return ResponseEntity.badRequest().body(result);
            }
            
            // 验证 Flag
            boolean isValid = flagService.checkFlag(vulType, vulLevel, flag);
            
            if (isValid) {
                result.put("success", true);
                result.put("message", "Flag is correct!");
                // 更新关卡状态为已完成
                flagService.updateStatus(vulType, vulLevel, "已完成");
            } else {
                result.put("success", false);
                result.put("message", "Flag is incorrect");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "System error, please try again later");
            return ResponseEntity.status(500).body(result);
        }
    }
}
