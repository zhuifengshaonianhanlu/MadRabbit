package com.madrabbit.challenge.deser;

import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * Custom gadget class for Fastjson AutoType challenge.
 * This is a REAL vulnerability — it performs an actual JNDI lookup.
 *
 * Exploit chain (identical to com.sun.rowset.JdbcRowSetImpl):
 * - Fastjson sees @type -> instantiates this class
 * - Fastjson calls setDataSourceName() and setAutoCommit()
 * - setAutoCommit(true) performs a REAL InitialContext.lookup(dataSourceName)
 * - If the student has an LDAP/RMI server returning a malicious class, it gets loaded and executed
 *
 * Structure:
 *   class: com.madrabbit.challenge.deser.FastjsonGadget
 *   fields/setters: dataSourceName (String), autoCommit (boolean)
 *   trigger: setAutoCommit(true) when dataSourceName is set
 */
public class FastjsonGadget {

    private static final ThreadLocal<String> executionResult = new ThreadLocal<>();

    private String dataSourceName;

    public FastjsonGadget() {
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String name) {
        this.dataSourceName = name;
    }

    public boolean isAutoCommit() {
        return false;
    }

    public void setAutoCommit(boolean auto) {
        if (auto && dataSourceName != null && !dataSourceName.isEmpty()) {
            // Record the trigger for flag awarding
            executionResult.set("JNDI lookup executed: " + dataSourceName);

            // Perform REAL JNDI lookup — this is the actual vulnerability
            try {
                Context ctx = new InitialContext();
                ctx.lookup(dataSourceName);
            } catch (Exception e) {
                // JNDI lookup attempted (connection refused, timeout, etc.)
                // The trigger is still recorded — flag is awarded regardless
                String current = executionResult.get();
                executionResult.set(current + " (lookup result: " + e.getClass().getSimpleName() + ")");
            }
        }
    }

    public static String getAndClearResult() {
        String r = executionResult.get();
        executionResult.remove();
        return r;
    }
}
