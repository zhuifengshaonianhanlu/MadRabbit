package com.madrabbit.controller.challenge.deserialization;

import com.alibaba.fastjson.JSON;
import com.madrabbit.challenge.deser.FastjsonGadget;
import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/challenge/deser/fastjson")
public class FastjsonController {

    @Autowired
    private FlagService flagService;

    /**
     * Get system info - reveals Fastjson version.
     * GET /api/challenge/deser/fastjson/info
     */
    @GetMapping("/info")
    public Map<String, Object> getInfo() {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> systemInfo = new LinkedHashMap<>();
        systemInfo.put("app_name", "UserConfigService");
        systemInfo.put("version", "2.1.0");
        systemInfo.put("framework", "Spring Boot 2.3.4");
        systemInfo.put("json_parser", "Fastjson 1.2.24");
        systemInfo.put("java_version", "1.8.0_181");
        systemInfo.put("os", "Linux 4.15.0");

        result.put("success", true);
        result.put("systemInfo", systemInfo);
        result.put("note", "System dependencies exposed for debugging purposes");
        return result;
    }

    /**
     * Get gadget class info - exposes the custom gadget structure for students.
     * GET /api/challenge/deser/fastjson/gadget
     */
    @GetMapping("/gadget")
    public Map<String, Object> getGadgetInfo() {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> gadgetInfo = new LinkedHashMap<>();
        gadgetInfo.put("class", "com.madrabbit.challenge.deser.FastjsonGadget");
        gadgetInfo.put("pattern", "Mimics com.sun.rowset.JdbcRowSetImpl JNDI injection");

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("dataSourceName", "String - JNDI URL (e.g. ldap://attacker.com/Exploit)");
        fields.put("autoCommit", "boolean - trigger point");
        gadgetInfo.put("fields", fields);

        Map<String, String> setters = new LinkedHashMap<>();
        setters.put("setDataSourceName(String)", "Sets the JNDI data source URL");
        setters.put("setAutoCommit(boolean)", "When true and dataSourceName is set, triggers JNDI lookup -> RCE");
        gadgetInfo.put("setters", setters);

        gadgetInfo.put("trigger_condition", "autoCommit=true AND dataSourceName is non-empty");

        String sourceCode = "public class FastjsonGadget {\n"
                + "    private String dataSourceName;\n"
                + "\n"
                + "    public void setDataSourceName(String name) {\n"
                + "        this.dataSourceName = name;\n"
                + "    }\n"
                + "\n"
                + "    public void setAutoCommit(boolean auto) {\n"
                + "        if (auto && dataSourceName != null) {\n"
                + "            // JNDI lookup -> Remote class load -> RCE!\n"
                + "            jndiLookup(dataSourceName);\n"
                + "        }\n"
                + "    }\n"
                + "}";
        gadgetInfo.put("source_snippet", sourceCode);

        result.put("success", true);
        result.put("gadgetClass", gadgetInfo);
        result.put("hint", "Fastjson's @type field tells the parser which class to instantiate. "
                + "It will call the setters automatically. Craft a JSON with @type pointing to this gadget class.");
        return result;
    }

    /**
     * Update user config - vulnerable to Fastjson AutoType RCE.
     * Uses real Fastjson JSON.parseObject() to parse user input.
     * POST /api/challenge/deser/fastjson/update
     * Body: raw JSON string
     */
    @PostMapping("/update")
    public Map<String, Object> updateConfig(@RequestBody String rawBody) {
        Map<String, Object> result = new HashMap<>();

        if (rawBody == null || rawBody.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Please provide JSON configuration data");
            return result;
        }

        // Update challenge status to in-progress
        try {
            Map<String, Object> status = flagService.getStatus("deserialization", "level2");
            if (status != null && "\u672a\u5f00\u59cb".equals(status.get("status"))) {
                flagService.updateStatus("deserialization", "level2", "\u8fdb\u884c\u4e2d");
            }
        } catch (Exception e) {
            // ignore
        }

        // Clear any previous execution result
        FastjsonGadget.getAndClearResult();

        // ============================================================
        // THE VULNERABLE CODE: Fastjson JSON.parseObject() with AutoType
        // Fastjson 1.2.24 has AutoType enabled by default.
        // When @type is present, it instantiates the specified class
        // and calls its setters — triggering the gadget chain.
        // ============================================================
        try {
            Object parsed = JSON.parseObject(rawBody);

            // Check if the gadget fired during parsing
            String executionResult = FastjsonGadget.getAndClearResult();

            if (executionResult != null) {
                // Gadget was triggered — RCE achieved!
                String flag = flagService.getFlag("deserialization", "level2");
                result.put("success", true);
                result.put("rce_triggered", true);
                result.put("message", "CRITICAL: Fastjson AutoType RCE triggered! "
                        + "The @type field caused Fastjson to instantiate the gadget class "
                        + "and call its setters, triggering JNDI lookup -> RCE!");
                result.put("executionOutput", executionResult);
                result.put("vulnerability", "Fastjson 1.2.24 AutoType deserialization (CVE-2017-18349)");
                result.put("explanation", "JSON.parseObject() saw @type -> instantiated FastjsonGadget -> "
                        + "called setDataSourceName() and setAutoCommit(true) -> JNDI lookup triggered");
                result.put("flag", flag);
            } else {
                // Normal JSON parsed, no gadget triggered
                result.put("success", true);
                result.put("rce_triggered", false);
                result.put("message", "Configuration updated successfully");
                result.put("parsedData", parsed);
                result.put("hint", "Normal JSON processed by Fastjson. "
                        + "Fastjson's @type field allows specifying a class to deserialize into. "
                        + "Check GET /gadget for the available gadget class.");
            }
        } catch (com.alibaba.fastjson.JSONException e) {
            String msg = e.getMessage();
            // Detect JdbcRowSetImpl JNDI trigger — "set property error, autoCommit"
            // means Fastjson instantiated the class and called setAutoCommit(),
            // which triggered a real JNDI lookup that failed (no LDAP server).
            if (msg != null && msg.contains("autoCommit")) {
                String flag = flagService.getFlag("deserialization", "level2");
                result.put("success", true);
                result.put("rce_triggered", true);
                result.put("message", "CRITICAL: Fastjson AutoType RCE triggered! "
                        + "The class was instantiated and setAutoCommit() triggered a REAL JNDI lookup!");
                result.put("executionOutput", "JNDI lookup attempted via setAutoCommit() -> "
                        + "Connection failed (no LDAP/RMI server at target). "
                        + "With a running exploit server, this would achieve full RCE.");
                result.put("vulnerability", "Fastjson 1.2.24 AutoType deserialization (CVE-2017-18349)");
                result.put("detail", msg);
                result.put("flag", flag);
            } else {
                result.put("success", false);
                result.put("message", "Fastjson parse error: " + msg);
                result.put("hint", "Make sure your JSON is valid. If using @type, ensure the class exists on the classpath.");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        return result;
    }
}
