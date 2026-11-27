# Publishing Netlogger to Maven Central and JitPack

The library uses `com.vanniktech.maven.publish` and the Central Portal API.
Publishing runs on macOS so that Android and both iOS targets are included in one
deployment. It does not upload the sample apps or Xcode frameworks: consumers
receive Kotlin Multiplatform metadata, an Android AAR and native iOS KLIBs.

## Coordinates

Public coordinates are defined in the root `gradle.properties`:

```properties
GROUP=io.github.koai-dev
POM_ARTIFACT_ID=netlogger
VERSION_NAME=1.5.5
```

The four publications are `netlogger`, `netlogger-android`, `netlogger-iosarm64`
and `netlogger-iossimulatorarm64`. All include POM metadata, sources, javadoc
JARs and GPG signatures. The default javadoc JAR is empty; the sources contain
the Kotlin API documentation.

`GROUP` must be a namespace verified in your Central Portal account. Register
and verify the GitHub-based namespace `io.github.koai-dev` before the first
release. A GitHub repository URL alone does not verify a namespace. Keep the chosen coordinates
stable after publishing. A released version cannot be overwritten.

After release, consumers add `mavenCentral()` and:

```kotlin
// KMP commonMain
implementation("io.github.koai-dev:netlogger:1.5.5")

// Android-only debug host
debugImplementation("io.github.koai-dev:netlogger:1.5.5")
```

## Central account and signing key

1. Create a [Central Portal account](https://central.sonatype.com/).
2. [Register and verify your namespace](https://central.sonatype.org/register/namespace/).
3. Generate a publishing user token in the Portal. Use its generated username
   and password, rather than your account login.
4. Create or reuse a GPG signing key and
   [distribute its public key](https://central.sonatype.org/publish/requirements/gpg/#distributing-your-public-key).

For local publishing, put secrets in `~/.gradle/gradle.properties` (outside this
repository), or use the equivalent `ORG_GRADLE_PROJECT_...` environment variables:

If the private signing key is already in your local GPG keyring, use GPG directly
without exporting it. First list the available signing keys:

```bash
gpg --list-secret-keys --keyid-format LONG
```

Add these properties to `~/.gradle/gradle.properties`, using your signing key ID
or full fingerprint:

```properties
mavenCentralUsername=<Central token username>
mavenCentralPassword=<Central token password>
signing.useGpgCmd=true
signing.gnupg.keyName=<signing key ID or fingerprint>
```

GPG uses your local agent to unlock a passphrase-protected key. This mode requires
`gpg` on `PATH`; optionally set `signing.gnupg.executable` to its absolute path.
Central tokens and GPG signing are separate: configuring only the token causes
the `no configured signatory` error. Properties in `local.properties` are not
used for signing.

For an exported private key file, omit `signing.useGpgCmd=true` and use this
alternative configuration:

```properties
mavenCentralUsername=<Central token username>
mavenCentralPassword=<Central token password>
signing.keyId=<last 8 hex characters of signing key ID>
signing.password=<key passphrase>
signing.secretKeyRingFile=/absolute/path/to/private-signing-key.gpg
```

You can export a key to a private file without printing it:

```bash
umask 077
gpg --export-secret-keys YOUR_KEY_ID > /absolute/path/to/private-signing-key.gpg
```

The plugin also supports an ASCII-armored key through
`ORG_GRADLE_PROJECT_signingInMemoryKey` and its passphrase through
`ORG_GRADLE_PROJECT_signingInMemoryKeyPassword`. Never commit private keys or
publishing tokens. Signing is mandatory for release publications; normal builds
and tests do not need signing secrets.

## Local verification and upload

```bash
# Compile and test Android + iOS
./scripts/verify-builds.sh

# Build and sign all publications into build/maven-staging; no Central token needed
./gradlew --no-daemon :netlogger:verifyNetloggerSigningCredentials
./gradlew --no-daemon :netlogger:publishAllPublicationsToLocalStagingRepository

# Upload to Central Portal, then inspect the deployment and click Publish
./gradlew --no-daemon :netlogger:publishToMavenCentral

# Alternatively, upload and release directly
./gradlew --no-daemon :netlogger:publishAndReleaseToMavenCentral
```

For a different namespace or version, append `-PGROUP=io.github.koai-dev`
and/or `-PVERSION_NAME=1.5.5`. Publish the complete module; publishing only an
Android or iOS publication leaves the common metadata pointing to missing
artifacts.

`publishToMavenCentral` uses manual release mode. The upload is available in
[Central Portal deployments](https://central.sonatype.com/publishing/deployments)
for validation and final publishing. Versions ending in `-SNAPSHOT` use the
Portal snapshot repository, not the release repository.

## GitHub Actions

Configure these secrets in the repository or the `maven-central` environment:

| Secret | Value |
| --- | --- |
| `MAVEN_CENTRAL_USERNAME` | Portal user-token username |
| `MAVEN_CENTRAL_PASSWORD` | Portal user-token password |
| `SIGNING_IN_MEMORY_KEY` | Complete ASCII-armored private GPG key |
| `SIGNING_IN_MEMORY_KEY_PASSWORD` | Key passphrase; can be empty for an unencrypted key |

**Publish to Maven Central** runs when a release tag (`1.5.5`, or `1.6.0-rc1`) is
pushed, and can also be run manually from Actions with a chosen commit, version and
group ID. A tag run fails unless the tag equals `VERSION_NAME` in
`gradle.properties`, then uploads that version under `io.github.koai-dev`. The
workflow first checks both platforms, then uploads all signed publications in a
single deployment. Finish the release in the Central Portal after validation
succeeds.

## Release: one tag for Maven Central and JitPack

1. Set `VERSION_NAME` in `gradle.properties` (and the documented versions) to the
   new version, update `CHANGELOG.md`, and push the commit.
2. Tag that commit with exactly the version and push the tag:

   ```bash
   git tag 1.5.5
   git push origin 1.5.5
   ```

3. The tag starts the Maven Central workflow; publish its deployment in the Central
   Portal.
4. JitPack builds the tag on first request, or ahead of time from
   [jitpack.io](https://jitpack.io/#koai-dev/netlogger-compose) (**Look up**, then
   **Get it**). Its log is at
   `https://jitpack.io/com/github/koai-dev/netlogger-compose/<tag>/build.log`.

## JitPack

JitPack builds on Linux with the command in `jitpack.yml`. It passes
`GROUP=com.github.koai-dev.netlogger-compose` and `VERSION_NAME=<tag>`, because
JitPack serves a multi-artifact build under `com.github.<user>.<repository>`; the
Gradle module metadata then names the same coordinates that JitPack serves. With
`JITPACK=true` set by JitPack, the build skips GPG signing. Kotlin cross-compiles the
iOS KLIBs on Linux, so the four publications match Maven Central:

```kotlin
repositories { maven("https://jitpack.io") }

// KMP commonMain
implementation("com.github.koai-dev.netlogger-compose:netlogger:1.5.5")

// Android-only debug host
debugImplementation("com.github.koai-dev.netlogger-compose:netlogger-android:1.5.5")
```

Check the JitPack build locally without signing keys and without touching
`~/.m2`:

```bash
JITPACK=true GROUP=com.github.koai-dev ARTIFACT=netlogger-compose VERSION=1.5.5 \
  ./gradlew --no-daemon "-PGROUP=com.github.koai-dev.netlogger-compose" "-PVERSION_NAME=1.5.5" \
  -Dmaven.repo.local=/tmp/netlogger-jitpack-m2 :netlogger:publishToMavenLocal
```

A failed JitPack build of a tag is cached; delete it on the JitPack page (requires
push access to the repository) before building that tag again.

References: [plugin configuration](https://vanniktech.github.io/gradle-maven-publish-plugin/central/),
[KMP publication configuration](https://vanniktech.github.io/gradle-maven-publish-plugin/what/#kotlin-multiplatform-library),
[Central requirements](https://central.sonatype.org/publish/requirements/).
