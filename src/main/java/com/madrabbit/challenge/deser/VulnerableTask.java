package com.madrabbit.challenge.deser;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * Simulated "gadget" class for Java deserialization challenge.
 *
 * This class mimics real-world gadget chains (e.g., Commons Collections InvokerTransformer).
 * During deserialization, its readObject() method automatically executes the specified command,
 * simulating how real gadget chains achieve RCE via ObjectInputStream.readObject().
 *
 * Students must craft a serialized VulnerableTask object with a non-empty 'command' field
 * to trigger "code execution" during deserialization.
 *
 * Class structure (needed for crafting payload):
 *   - Package: com.madrabbit.challenge.deser
 *   - Class: VulnerableTask implements Serializable
 *   - serialVersionUID: 20250101L
 *   - Fields: String command
 */
public class VulnerableTask implements Serializable {

    private static final long serialVersionUID = 20250101L;

    private String command;

    /**
     * Thread-local storage for the execution result.
     * Set during readObject() when deserialization triggers "code execution".
     */
    private static final ThreadLocal<String> executionResult = new ThreadLocal<>();

    public VulnerableTask() {
    }

    public VulnerableTask(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * This method is automatically called during deserialization.
     * It simulates a gadget chain: when an object of this class is deserialized,
     * the 'command' field is "executed" - just like how InvokerTransformer
     * invokes Runtime.exec() in real-world exploits.
     */
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        // Simulate code execution during deserialization
        if (command != null && !command.trim().isEmpty()) {
            String result = executeCommand(command);
            executionResult.set(result);
        }
    }

    /**
     * Simulated command execution.
     * In real-world exploits, this would be Runtime.getRuntime().exec(command).
     */
    private String executeCommand(String cmd) {
        // Simulate different commands
        if (cmd.contains("id") || cmd.contains("whoami")) {
            return "uid=0(root) gid=0(root) groups=0(root)";
        } else if (cmd.contains("cat") && cmd.contains("/etc/passwd")) {
            return "root:x:0:0:root:/root:/bin/bash";
        } else if (cmd.contains("ls")) {
            return "flag.txt\napp.jar\nconfig/";
        } else {
            return "Command executed: " + cmd;
        }
    }

    /**
     * Retrieve and clear the execution result from the current thread.
     * Called by the controller after deserialization to check if the gadget fired.
     */
    public static String getAndClearResult() {
        String result = executionResult.get();
        executionResult.remove();
        return result;
    }
}
