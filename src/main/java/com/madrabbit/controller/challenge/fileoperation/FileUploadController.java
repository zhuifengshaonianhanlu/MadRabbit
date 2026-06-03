package com.madrabbit.controller.challenge.fileoperation;

import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 文件上传漏洞关卡控制器
 * 场景：头像上传功能，三层防线（扩展名 + Content-Type + 文件头魔数）
 * 通关方式：构造图片马（合法扩展名 + 合法Content-Type + 图片魔数头 + 恶意代码内容）
 */
@RestController
@RequestMapping("/api/challenge/file-op/upload")
public class FileUploadController {

    @Autowired
    private FlagService flagService;

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif"
    ));

    private static final Set<String> ALLOWED_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/png", "image/gif"
    ));

    // WebShell 特征字符串
    private static final List<String> MALICIOUS_SIGNATURES = Arrays.asList(
            "<?php", "<%", "<jsp:", "<%@", "#!/bin/",
            "Runtime.exec", "ProcessBuilder", "eval(", "system("
    );

    /**
     * 获取当前头像信息
     */
    @GetMapping("/avatar")
    public Map<String, Object> getAvatarInfo() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("avatarUrl", "/uploads/avatars/default.png");
        return result;
    }

    /**
     * 上传头像 - 真实 multipart 文件上传
     * 三层校验：扩展名 → Content-Type → 文件头魔数
     * 若三层通过且含恶意代码特征，返回 flag
     */
    @PostMapping("/avatar")
    public Map<String, Object> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();

        if (file == null || file.isEmpty()) {
            result.put("success", false);
            result.put("message", "请选择要上传的文件");
            return result;
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            result.put("success", false);
            result.put("message", "文件名无效");
            return result;
        }

        // === 第一层：扩展名白名单校验 ===
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            result.put("success", false);
            result.put("blocked_by", "extension");
            result.put("message", "仅允许上传图片文件(.jpg, .png, .gif)");
            return result;
        }

        // === 第二层：Content-Type 校验 ===
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            result.put("success", false);
            result.put("blocked_by", "content_type");
            result.put("message", "Content-Type 必须为图片类型");
            return result;
        }

        // === 第三层：文件头魔数校验 ===
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "文件读取失败");
            return result;
        }

        if (!isValidImageMagicBytes(fileBytes)) {
            result.put("success", false);
            result.put("blocked_by", "magic_bytes");
            result.put("message", "文件头不是有效的图片格式");
            return result;
        }

        // === 三层全部通过，检测恶意内容 ===
        String fileContent = new String(fileBytes, StandardCharsets.ISO_8859_1);
        boolean hasMaliciousContent = false;
        for (String signature : MALICIOUS_SIGNATURES) {
            if (fileContent.contains(signature)) {
                hasMaliciousContent = true;
                break;
            }
        }

        // 保存文件
        String savedPath = saveFile(fileBytes, originalFilename);

        if (hasMaliciousContent) {
            // 绕过成功！返回 flag
            String flag = flagService.getFlag("file-operation", "level1");
            result.put("success", true);
            result.put("flag", flag);
            result.put("message", "恭喜！你成功绕过了所有文件类型校验！");
            result.put("uploadedPath", savedPath);
        } else {
            // 正常图片上传
            result.put("success", true);
            result.put("message", "头像上传成功");
            result.put("avatarUrl", savedPath);
        }

        return result;
    }

    /**
     * 获取文件扩展名（最后一个 . 后的部分）
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }

    /**
     * 检查文件头魔数是否为合法图片格式
     */
    private boolean isValidImageMagicBytes(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length < 4) {
            return false;
        }

        // JPEG: FF D8 FF
        if (fileBytes[0] == (byte) 0xFF && fileBytes[1] == (byte) 0xD8 && fileBytes[2] == (byte) 0xFF) {
            return true;
        }

        // PNG: 89 50 4E 47
        if (fileBytes[0] == (byte) 0x89 && fileBytes[1] == (byte) 0x50
                && fileBytes[2] == (byte) 0x4E && fileBytes[3] == (byte) 0x47) {
            return true;
        }

        // GIF: 47 49 46 38 (GIF8)
        if (fileBytes[0] == (byte) 0x47 && fileBytes[1] == (byte) 0x49
                && fileBytes[2] == (byte) 0x46 && fileBytes[3] == (byte) 0x38) {
            return true;
        }

        return false;
    }

    /**
     * 保存文件到 uploads/avatars/ 目录
     */
    private String saveFile(byte[] fileBytes, String originalFilename) {
        try {
            String uploadDir = System.getProperty("user.dir") + "/uploads/avatars/";
            Path dirPath = Paths.get(uploadDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String savedName = System.currentTimeMillis() + "_" + originalFilename;
            Path filePath = dirPath.resolve(savedName);
            Files.write(filePath, fileBytes);

            return "/uploads/avatars/" + savedName;
        } catch (IOException e) {
            return "/uploads/avatars/default.png";
        }
    }
}
