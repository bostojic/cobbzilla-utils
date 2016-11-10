package org.cobbzilla.util.collection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MultiResult {

    public List<String> successes = new ArrayList<>();
    public Map<String, String> failures = new LinkedHashMap<>();

    public int successCount() { return successes.size(); }
    public int failCount() { return failures.size(); }

    public void success(String name) { successes.add(name); }
    public void fail(String name, String reason) { failures.put(name, reason); }

    public boolean hasFailures () { return !failures.isEmpty(); }

    public String getHeader() { return "TEST RESULTS"; }

    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append("\n\n" + getHeader() + "\n--------------------\n")
                .append(successCount()).append("\tsucceeded\n")
                .append(failCount()).append("\tfailed");
        if (!failures.isEmpty()) {
            b.append(":\n");
            for (Map.Entry<String, String> fail : failures.entrySet()) {
                b.append(fail.getKey()).append(":\t").append(fail.getValue()).append("\n");
            }
        }
        b.append("--------------------\n");
        return b.toString();
    }

}