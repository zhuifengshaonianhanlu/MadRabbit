package com.madrabbit.controller.challenge.deserialization;

import com.madrabbit.challenge.deser.VulnerableTask;
import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.*;

@RestController
@RequestMapping("/api/challenge/deser/native")
public class NativeDeserController {

    @Autowired
    private FlagService flagService;

    // Java serialization magic bytes (aced 0005) base64-encoded prefix
    private static final String JAVA_SERIAL_MAGIC = "rO0AB";

    /**
     * Get the vulnerable gadget class info - exposes class structure for students.
     * GET /api/challenge/deser/native/info
     */
    @GetMapping("/info")
    public Map<String, Object> getInfo() {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> appInfo = new LinkedHashMap<>();
        appInfo.put("app_name", "SessionRestoreService");
        appInfo.put("framework", "Spring Boot 2.7.x");
        appInfo.put("java_version", "1.8+");
        appInfo.put("deserialization", "ObjectInputStream.readObject() - NO FILTER");
        appInfo.put("classpath_note", "Application uses custom task scheduling classes");

        Map<String, Object> gadgetInfo = new LinkedHashMap<>();
        gadgetInfo.put("class", "com.madrabbit.challenge.deser.VulnerableTask");
        gadgetInfo.put("implements", "java.io.Serializable");
        gadgetInfo.put("serialVersionUID", 20250101L);
        gadgetInfo.put("fields", Collections.singletonMap("command", "java.lang.String"));
        gadgetInfo.put("behavior", "readObject() automatically executes the 'command' field during deserialization");

        String sourceCode = "public class VulnerableTask implements Serializable {\n"
                + "    private static final long serialVersionUID = 20250101L;\n"
                + "    private String command;\n"
                + "    // ...\n"
                + "    private void readObject(ObjectInputStream ois) {\n"
                + "        ois.defaultReadObject();\n"
                + "        if (command != null && !command.isEmpty()) {\n"
                + "            execute(command); // <-- triggered during deserialization!\n"
                + "        }\n"
                + "    }\n"
                + "}";
        gadgetInfo.put("source_snippet", sourceCode);

        result.put("success", true);
        result.put("appInfo", appInfo);
        result.put("gadgetClass", gadgetInfo);
        result.put("hint", "Craft a serialized VulnerableTask object with a non-empty 'command'. The server will call ObjectInputStream.readObject() on your data, triggering real code execution.");
        return result;
    }

    /**
     * Get current guest session token (a legitimate serialized VulnerableTask with empty command).
     * GET /api/challenge/deser/native/session
     */
    @GetMapping("/session")
    public Map<String, Object> getSession() {
        Map<String, Object> result = new HashMap<>();

        // Generate a real serialized VulnerableTask (with empty command = safe)
        String token = generateGuestToken();

        Map<String, Object> parsedInfo = new LinkedHashMap<>();
        parsedInfo.put("class", "com.madrabbit.challenge.deser.VulnerableTask");
        parsedInfo.put("command", "(empty)");
        parsedInfo.put("note", "This is a safe token - the command field is empty, so no code executes during deserialization");

        result.put("success", true);
        result.put("sessionToken", token);
        result.put("format", "Java Serialized Object (Base64)");
        result.put("magicBytes", "AC ED 00 05 (hex) = rO0AB (base64)");
        result.put("parsedSession", parsedInfo);
        result.put("hint", "This is a real Java serialized VulnerableTask. Decode and inspect it, then craft your own with a non-empty 'command' field.");
        return result;
    }

    /**
     * Restore session from serialized data - GENUINELY VULNERABLE to deserialization attack.
     * POST /api/challenge/deser/native/restore
     * Body: { "sessionData": "base64_string" }
     *
     * Uses ObjectInputStream.readObject() WITHOUT any filter.
     * When a VulnerableTask with non-empty command is deserialized,
     * readObject() fires and "executes" the command.
     */
    @PostMapping("/restore")
    public Map<String, Object> restoreSession(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();

        String sessionData = request.get("sessionData");

        if (sessionData == null || sessionData.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Please provide sessionData");
            return result;
        }

        // Update challenge status to in-progress
        try {
            Map<String, Object> status = flagService.getStatus("deserialization", "level1");
            if (status != null && "\u672a\u5f00\u59cb".equals(status.get("status"))) {
                flagService.updateStatus("deserialization", "level1", "\u8fdb\u884c\u4e2d");
            }
        } catch (Exception e) {
            // ignore
        }

        sessionData = sessionData.trim();

        // Check magic bytes
        if (!sessionData.startsWith(JAVA_SERIAL_MAGIC)) {
            result.put("success", false);
            result.put("message", "Invalid format: not a Java serialized object. Expected base64 data starting with 'rO0AB' (magic bytes: AC ED 00 05)");
            result.put("hint", "The data must be a valid Java serialized object. Use Java's ObjectOutputStream to create one.");
            return result;
        }

        // Decode base64
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(sessionData);
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", "Invalid Base64 encoding: " + e.getMessage());
            return result;
        }

        // Clear any previous execution result
        VulnerableTask.getAndClearResult();

        // ============================================================
        // THE VULNERABLE CODE: ObjectInputStream with NO ObjectInputFilter
        // This is the real vulnerability. readObject() is called on
        // attacker-controlled data, triggering VulnerableTask.readObject()
        // which auto-executes the 'command' field.
        // ============================================================
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decoded))) {
            Object obj = ois.readObject();

            // Check if the gadget fired during deserialization
            String executionResult = VulnerableTask.getAndClearResult();

            if (executionResult != null) {
                // Code was executed during deserialization — RCE achieved!
                String flag = flagService.getFlag("deserialization", "level1");
                result.put("success", true);
                result.put("rce_triggered", true);
                result.put("message", "CRITICAL: Remote Code Execution achieved! "
                        + "VulnerableTask.readObject() executed your command during deserialization!");
                result.put("executionOutput", executionResult);
                result.put("deserializedClass", obj.getClass().getName());
                result.put("explanation", "ObjectInputStream.readObject() invoked VulnerableTask.readObject() "
                        + "which auto-executed the 'command' field. This is exactly how real-world "
                        + "gadget chains (CommonsCollections InvokerTransformer, etc.) achieve RCE.");
                result.put("flag", flag);
            } else {
                // Object deserialized successfully but no code execution
                result.put("success", true);
                result.put("rce_triggered", false);
                result.put("message", "Object deserialized successfully, but no code execution was triggered.");
                result.put("deserializedClass", obj.getClass().getName());
                result.put("deserializedObject", obj.toString());
                result.put("hint", "The VulnerableTask gadget only fires when its 'command' field is non-empty. "
                        + "Serialize a VulnerableTask with command='id' or 'whoami' to trigger RCE.");
            }
        } catch (ClassNotFoundException e) {
            result.put("success", false);
            result.put("message", "ClassNotFoundException: " + e.getMessage());
            result.put("hint", "The serialized object references a class not on the server classpath. "
                    + "Use com.madrabbit.challenge.deser.VulnerableTask (serialVersionUID=20250101L).");
        } catch (InvalidClassException e) {
            result.put("success", false);
            result.put("message", "InvalidClassException: " + e.getMessage());
            result.put("hint", "serialVersionUID mismatch. Use serialVersionUID = 20250101L");
        } catch (StreamCorruptedException e) {
            result.put("success", false);
            result.put("message", "StreamCorruptedException: invalid stream header");
            result.put("hint", "The data is not a valid Java serialization stream. "
                    + "Use ObjectOutputStream to serialize a VulnerableTask object.");
        } catch (java.io.EOFException e) {
            result.put("success", false);
            result.put("message", "EOFException: serialized data is incomplete or truncated");
            result.put("hint", "Your data starts with the correct magic bytes (rO0AB) but the serialization stream "
                    + "is incomplete. You must use ObjectOutputStream.writeObject() to produce a complete serialized object. "
                    + "Manual byte editing won't work — use the Java serialization API.");
            result.put("expectedFlow", "1) Create VulnerableTask.java matching server class  "
                    + "2) new ObjectOutputStream(baos).writeObject(new VulnerableTask(\"id\"))  "
                    + "3) Base64.encode the byte array  "
                    + "4) Submit the full base64 string");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Deserialization error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        return result;
    }

    /**
     * Helper endpoint for educational purposes.
     * POST /api/challenge/deser/native/generate
     * Body: { "command": "" }
     *
     * Only generates safe (empty command) payloads.
     * Students must craft the exploit themselves.
     */
    @PostMapping("/generate")
    public Map<String, Object> generatePayload(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();

        String command = request.get("command");

        if (command != null && !command.trim().isEmpty()) {
            // Don't generate the exploit for them
            result.put("success", false);
            result.put("message", "This helper only generates safe (empty command) payloads. You must craft the exploit yourself!");
            result.put("hint_java", ""
                    + "// Create a Java file with the VulnerableTask class (same package, same serialVersionUID),\n"
                    + "// then serialize it:\n"
                    + "ByteArrayOutputStream baos = new ByteArrayOutputStream();\n"
                    + "ObjectOutputStream oos = new ObjectOutputStream(baos);\n"
                    + "VulnerableTask task = new VulnerableTask(\"" + command + "\");\n"
                    + "oos.writeObject(task);\n"
                    + "String payload = Base64.getEncoder().encodeToString(baos.toByteArray());\n"
                    + "System.out.println(payload);");
            result.put("hint_class", ""
                    + "// Your VulnerableTask.java must match:\n"
                    + "package com.madrabbit.challenge.deser;\n"
                    + "import java.io.Serializable;\n"
                    + "public class VulnerableTask implements Serializable {\n"
                    + "    private static final long serialVersionUID = 20250101L;\n"
                    + "    private String command;\n"
                    + "    public VulnerableTask(String cmd) { this.command = cmd; }\n"
                    + "}");
            return result;
        }

        // Generate a safe payload (empty command)
        String token = generateGuestToken();
        result.put("success", true);
        result.put("payload", token);
        result.put("note", "This is a VulnerableTask with empty command (safe). "
                + "Submit it to /restore and observe: no RCE is triggered. "
                + "Now craft one WITH a command to trigger the gadget!");
        return result;
    }

    /**
     * Generate base64-encoded serialized VulnerableTask with empty command.
     */
    private String generateGuestToken() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            VulnerableTask guestTask = new VulnerableTask(); // empty command
            oos.writeObject(guestTask);
            oos.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            return "ERROR_GENERATING_TOKEN";
        }
    }
}
