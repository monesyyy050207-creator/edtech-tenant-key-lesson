package edu.tenantkeys;

import java.util.List;

/** The domain decision for an educator leaving a course tenant. */
public final class TenantAccessPlan {
    private TenantAccessPlan() {
    }

    public static List<String> offboardSteps(String keyId, String userId) {
        if (keyId == null || keyId.isBlank() || userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("A course tenant needs both a key id and a user id");
        }
        return List.of("revoke-key:" + keyId, "delete-user:" + userId);
    }
}
