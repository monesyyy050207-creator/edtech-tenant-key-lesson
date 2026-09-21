#!/usr/bin/env sh
set -eu
rm -rf out
mkdir -p out
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out edu.tenantkeys.TenantOffboardingTest
java -cp out edu.tenantkeys.CourseTenantLesson "$@"
