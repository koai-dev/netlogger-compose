# iOS console crash in 1.5.1

## Confirmed crash evidence

The local simulator reports `test.kexe-2026-10-22-115307.ips`,
`test.kexe-2026-10-22-115501.ips`, `test.kexe-2026-10-22-115542.ips` and
`test.kexe-2026-10-22-115626.ips` all record the same failure while probing
`io.github.koai-dev:netlogger:1.5.1` on an arm64 iOS Simulator with Kotlin 2.4.20
and Ktor 3.6.0. These are existing consumer probe reports, not results of the
regression tests added in this repository. The original reports remain in the
machine's DiagnosticReports directory; they are not committed because they
include machine and application details.

All four reports contain:

```text
EXC_BAD_ACCESS (SIGSEGV)
KERN_INVALID_ADDRESS at 0x0067676f6c746566
Thread queue: com.apple.root.default-qos

objc_opt_respondsToSelector
_NSDescriptionWithStringProxyFunc
__CFSTRING_IS_CALLING_OUT_TO_AN_OBJECT_FORMAT_ARGUMENT_WITH_CONTEXT__
__CFStringAppendFormatCore
_CFStringCreateWithFormatAndArgumentsReturningMetadata
_CFStringCreateWithFormatAndArgumentsAux2
_CFLogvEx2Predicate
_CFLogvEx3
_NSLogv
NSLog
<Kotlin/Native C bridge; symbol omitted>
com.netlogger.lib.platform#writeConsole(Int;String;String)       Platform.ios.kt:34
com.netlogger.lib.platform.PlatformLog#println                 Platform.kt:19
NetloggerConsoleLogger$1.invoke                               NetloggerConsoleLogger.kt:18
```

The source of 1.5.1 passes a Kotlin `String` as a variadic argument to
`NSLog("%@", "$tag: $message")`. The `%@` formatter expects an Objective-C object;
the crash inside Foundation's object-description formatter confirms that this
call is unsafe. The invalid address has ASCII-like bytes, but the stack and the
call site are the evidence for the diagnosis; the address alone does not prove
the exact string layout.

## Reproduction and fix

1. On the main thread, initialize with
   `NetloggerConfig(maximumLogLevel = LogLevel.INFO, enableLogcatOutput = true)`.
2. Create a Ktor client with Darwin or MockEngine and call
   `Netlogger.configureClient(this)` in its configuration.
3. Send a request and wait for the asynchronous console consumer to run.

1.5.2 replaces the iOS console call with `kotlin.io.println("$tag: $message")`.
The output appears on standard output in the Xcode console. Format markers in
tags or messages are literal text. Android continues to use Logcat. Console
output is still opt-in and passes through the existing redaction and chunking.

`IosConsoleTest` exercises the real iOS writer on `Dispatchers.Default`, general
console logging at every severity, Unicode and literal format markers, chunking,
and Ktor MockEngine capture with console output enabled at INFO and ALL. The
network test waits for the repository save after console output, so closing the
client cannot skip the crash path. It also checks redaction and the INFO
header/body limits. It exercises the general console writer directly; it does
not claim end-to-end coverage of the `LogUtil` singleton queue.

```bash
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
./gradlew :netlogger:iosSimulatorArm64Test --tests '*IosConsoleTest*'
./scripts/verify-builds.sh
```

The native test suite is already part of both the build workflow and the
pre-publication workflow.

On October 22, 2026, running `IosConsoleTest` against the unmodified console
implementation reproduced signal 11 in
`nativeWriterHandlesLiteralFormatMarkersAndUnicode`. The new simulator report
`test.kexe-2026-10-22-160845.ips` records the same invalid address and the same
Foundation → `NSLog` → `writeConsole` stack, with the regression test as the
caller. This run used Xcode 27.1 (27A9275).

After applying the fix, the following verification completed successfully on the
same machine:

```bash
./gradlew :netlogger:iosSimulatorArm64Test \
  :netlogger:compileKotlinIosArm64 :netlogger:testAndroidHostTest
```

- iOS Simulator: 12 tests passed, including all 3 `IosConsoleTest` cases.
- Android host: 32 tests passed.
- iOS arm64 device KLIB: compiled successfully; no physical-device run was made.
- Both native main KLIBs: no current checkout path found as raw bytes or as its
  hexadecimal encoding. The linked simulator test binary's 124,257 symbols also
  contained no current checkout path in either representation.

This verification did not publish an artifact, run the external consumer's
`VNetloggerIosTest`, or build the sample applications with `verify-builds.sh`.

## Consumer workaround and remaining feedback

Until a fixed artifact is published, consumers of 1.5.1 should keep
`enableLogcatOutput = false` on iOS and use their own `println` adapter if needed.
After upgrading to the published fixed version, the consuming application's
`core-network-netlogger` adapter can set `libraryConsoleWorks = true` and rerun
`VNetloggerIosTest`. Those adapter files and tests are outside this repository.

The 1.5.2 source now also supports opt-in iOS shake-to-open through UIKit motion
events. See the README for window integration and UI test commands. The floating
shortcut and adjustable sensitivity remain Android-only.

The old crash's generated C-bridge symbol also contains the publisher's absolute
checkout path encoded as hexadecimal. Native compilations now use
`-Xklib-relative-path-base` with the repository root so Netlogger source paths
stored in newly compiled KLIBs are relative. This does not rewrite 1.5.1 or source
paths embedded in third-party dependencies. Verify newly built native artifacts
before publication; renaming a symbol in a crash report would not fix the binary.
