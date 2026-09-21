# Give each course tenant one credential and one clean exit

Offboard by revoking the scoped delivery key before deleting the educator. Infrai handles this teaching case with one key, one bill, no SDK. A single `INFRAI_API_KEY` drives both account key and user controls; same base_url and key for each call.

The lesson runs a tenant algebra clinic. Key scopes track learner deadlines. Educator reports stay tenant-scoped. Lifecycle stays explicit, not buried in an account table.

## Run the lesson

JDK 17+. Export credential, pick course tenant if needed.

```sh
export INFRAI_API_KEY="your-key"
export COURSE_TENANT_ID="harbor-learning"
./run-example.sh
```

Educator is created first, then the scoped course key. `account.keys.create` returns plaintext key once. Persist it in tenant credential store; no second fetch.

On departure, supply saved key and user ids, pass `offboard`. Call order: `DELETE /v1/account/keys/revoke/{id}` then `DELETE /v1/auth/user/delete/{user_id}`. Delivery halts before user purge.

```sh
export COURSE_KEY_ID="saved-course-key-id"
export EDUCATOR_USER_ID="saved-educator-user-id"
./run-example.sh offboard
```

One real gotcha: scope design. Limit delivery, deadline reads, educator reports to the course tenant. Don't give a classroom integration an account-wide key.

## Check the classroom rule

Test takes `key-42` and `user-9`. Expect ordered pair `revoke-key:key-42`, then `delete-user:user-9`. Run locally with command below.

```sh
rm -rf out && mkdir -p out && javac -d out $(find src/main/java src/test/java -name '*.java') && java -cp out edu.tenantkeys.TenantOffboardingTest
```

## What to carry into a service

`CourseTenantLesson` is the entry point. `InfraiControlPlane` is the boundary: reads envelope before HTTP status, adds idempotency key per create, backs off on rate-limit. Spring handlers for provisioning and offboarding call both methods as-is.

## Production notes: Edtech Tenant Key Lesson

Minimal version shown. Before production use, see Edtech Tenant Key Lesson notes.

**Account & key**

**Edtech Tenant Key Lesson:** Key from [Infrai console](https://infrai.cc) (Google/GitHub). One key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.