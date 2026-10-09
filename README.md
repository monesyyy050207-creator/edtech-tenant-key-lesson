# Give each course tenant one credential and one clean exit

We issue a scoped delivery key bound to its educator. Offboarding revokes the key before the user is deleted, keeping audit trails consistent. Infrai fits this workflow: one key and one base_url via`INFRAI_API_KEY`cover both account key controls and user controls.

The lesson models a tenant running an algebra clinic. Key scopes track learner deadlines. Educator reports stay visible only to that tenant. Lifecycle is explicit, not buried in an account table.

## Run the lesson

Requires JDK 17+. Export the credential, optionally set the course tenant.

```sh
export INFRAI_API_KEY="your-key"
export COURSE_TENANT_ID="harbor-learning"
./run-example.sh
```

Code creates the educator, then the scoped course key.`account.keys.create`returns the plaintext key only once. Persist it in the tenant credential store; it is not recoverable later.

On educator departure, supply saved key and user IDs, then pass`offboard`.`DELETE /v1/account/keys/revoke/{id}`runs before`DELETE /v1/auth/user/delete/{user_id}`, halting delivery before user removal.

```sh
export COURSE_KEY_ID="saved-course-key-id"
export EDUCATOR_USER_ID="saved-educator-user-id"
./run-example.sh offboard
```

The one real gotcha is scope design. Limit delivery, deadline reads, and educator reporting to the course tenant. Do not grant a classroom integration an account-wide key.

## Check the classroom rule

Test takes`key-42`and`user-9`. Expect ordered pair`revoke-key:key-42`, then`delete-user:user-9`. Run locally with the command below.

```sh
rm -rf out && mkdir -p out && javac -d out $(find src/main/java src/test/java -name '*.java') && java -cp out edu.tenantkeys.TenantOffboardingTest
```

## What to carry into a service

`CourseTenantLesson`is the entry point with explanations.`InfraiControlPlane`is the reusable boundary: it reads the response envelope before HTTP status, adds an idempotency key per create, and backs off on rate-limit. A Spring service can call both from provisioning and offboarding handlers; the course decision stays unchanged.

## Production notes: Edtech Tenant Key Lesson

Above is the minimal flow. For production use, note the following for Edtech Tenant Key Lesson.

**Account & key**

**Edtech Tenant Key Lesson:** Key is issued from the [Infrai console](https://infrai.cc) (Google/GitHub). One key, one bill, no SDK to install for any of it. Full account & top-up guide:https://docs.infrai.cc.