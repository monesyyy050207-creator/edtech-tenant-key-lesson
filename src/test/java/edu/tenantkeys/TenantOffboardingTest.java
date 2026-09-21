package edu.tenantkeys;

import java.util.List;

public final class TenantOffboardingTest {
    public static void main(String[] args) {
        List<String> expected = List.of("revoke-key:key-42", "delete-user:user-9");
        List<String> actual = TenantAccessPlan.offboardSteps("key-42", "user-9");
        if (!expected.equals(actual)) {
            throw new AssertionError("Offboarding must remove the scoped credential before the educator account");
        }
        System.out.println("PASS: key revocation precedes educator deletion");
    }
}
