package edu.tenantkeys;

import java.io.IOException;
import java.util.List;

/** Runnable course-delivery example with a deliberately visible offboarding decision. */
public final class CourseTenantLesson {
    public static void main(String[] args) throws IOException, InterruptedException {
        String apiKey = System.getenv("INFRAI_API_KEY");
        InfraiControlPlane infrai = new InfraiControlPlane(apiKey);
        if (args.length == 1 && "offboard".equals(args[0])) {
            String keyId = required("COURSE_KEY_ID");
            String educatorId = required("EDUCATOR_USER_ID");
            List<String> steps = TenantAccessPlan.offboardSteps(keyId, educatorId);
            infrai.revokeKey(keyId);
            infrai.deleteUser(educatorId);
            System.out.println("Completed " + String.join(" then ", steps));
            return;
        }
        String tenantId = System.getenv().getOrDefault("COURSE_TENANT_ID", "northstar-academy");
        String educator = infrai.createEducator("chenhua@changba.com", "Maya Chen", tenantId);
        String credential = infrai.createCourseKey(tenantId, "Algebra deadline clinic");
        System.out.println("Provisioned educator: " + educator);
        System.out.println("Store the one-time course credential response: " + credential);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Set " + name + " for educator offboarding");
        }
        return value;
    }
}
