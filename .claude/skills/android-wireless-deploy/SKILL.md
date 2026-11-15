---
name: android-wireless-deploy
description: Install an Android build onto a phone over wireless adb, prove it actually works, then publish it — rotating mDNS ports and stale records, OEM install prompts that block adb with no output, the on-device measurements that report success while measuring nothing, and collecting a release APK to D:\GitHub\apk plus posting it to the Telegram channel, after scanning the DEX for the credentials an unminified build bakes in and confirming the signature with apksigner. Use when deploying an APK over wireless debugging, when `adb install` hangs or the app is installed but adb never returns, when a screenshot or gfxinfo reading looks like a pass and cannot be, when sending or sharing an APK with anyone, or after a release build that should reach testers.
---

# android-wireless-deploy

Getting a build onto a wireless-debugging phone, and — the harder half — believing what you then
measure on it. Four of the failures below **report success**: adb hangs after the install has
already completed, a screenshot returns a valid PNG of nothing, a frame-timing report shows a
perfect zero, and a tap arrives as a drag. Each reads as a code problem and is not.

## Connecting: every mDNS record is stale until proven otherwise

The advertised port **rotates every time wireless debugging is toggled**, and old records linger
in adb's cache. So `adb mdns services` routinely lists a dead entry alongside a live one:

```
adb-XXXX (2)  _adb-tls-connect._tcp  172.25.138.180:43235   <- live
adb-XXXX      _adb-tls-connect._tcp  172.25.138.180:40225   <- dead, cached
```

Connecting to the dead one gives `actively refused it. (10061)`, which looks like the phone has
wireless debugging off when it does not.

Collect every record, try newest first, and **verify the device reached `device` state** — don't
trust `adb connect`'s exit code:

```bash
adb connect "$addr"
adb devices | grep -E "$addr\s+device" || continue   # else it didn't really connect
```

If none connect, the records are all stale: toggle wireless debugging off and on. Pairing is
separate and only needed once per PC — and the *pairing* port rotates every time that screen
opens, so read it fresh.

Prefer an already-connected device over rediscovery. Check `adb devices` first.

## Installing: adb can hang long after the install succeeded

On MIUI/Xiaomi and several other skins, `adb install` blocks on an on-screen **"Install via
USB"** prompt. It emits nothing while blocked, so it is indistinguishable from a slow transfer.
Worse, the install can complete and adb still never return.

Check the package rather than waiting on the command:

```bash
adb -s "$dev" shell pm list packages | grep -q com.example.app && echo installed
```

Say so up front in any deploy script — "if this hangs, look at the phone" — because the person
running it is watching a terminal, not the device. And name the real failures:

| Symptom | Cause |
|---|---|
| `INSTALL_FAILED_USER_RESTRICTED` | MIUI restriction — **retry once before changing any setting** (see below); if it persists, *Developer options → Install via USB* is off |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, "signatures do not match" | debug vs release key — uninstall first |
| adb hangs, package present | OEM prompt; the install already worked |

`INSTALL_FAILED_USER_RESTRICTED` is not reliably a settings problem. On a Xiaomi 23124RN87I the
same APK failed with it and then installed on a bare retry seconds later, nothing changed in
between — an on-screen prompt had taken the first attempt. Retry once and look at the phone
before sending anyone into Developer options. What makes it expensive is *when* it strikes: the
first install after a debug→release key swap, i.e. straight after the uninstall, when the device
has no working copy of the app and the obvious reading is that the new build is broken.

Debug and release are signed with different keys, so switching between them needs an uninstall,
which takes the app's saved data with it. Detect it and warn rather than failing opaquely.

**Save that data first — the debug build is your only way in.** `run-as` works on the installed
debug build and will not work on the release build replacing it, so this is a one-way door:

```bash
# MSYS_NO_PATHCONV=1 or Git Bash rewrites /data/data/... into C:/Program Files/...,
# and the error ("ls: C:/Program: No such file") blames your command, not the rewrite.
MSYS_NO_PATHCONV=1 adb -s "$dev" shell \
  "run-as com.example.app cat /data/data/com.example.app/files/datastore/prefs.preferences_pb" \
  > prefs-backup.pb
```

Preferences protobuf is mostly plain strings, so grepping printable runs out of the dump is
enough to tell the user what they are about to lose ("7 binned stations, 3 categories"). You
cannot write it back — but a list beats a shrug.

Also: `run-as` only works on a **debuggable** build, so a release APK's private files cannot be
inspected that way (`run-as: package not debuggable`). Don't build verification around it.

## Quoting: `adb shell` re-splits your arguments on the device

`adb shell` joins its arguments with spaces and hands the resulting **string** to the shell on the
phone, which splits it again. Anything that must survive as one word needs quoting for that
*second* parse — quoting it for Python, or for bash on the host, only gets it past the first.

Measured on a Xiaomi 23124RN87I (Android 15) over wireless adb. Passing the format as separate
argv elements:

```bash
adb -s "$dev" shell stat -c "%s %n" "$p1" "$p2"
```

```
stat: '%n': No such file or directory
4732539
124226339
```

`-c` took `%s` alone and `%n` became a **file operand**. The sizes are right and the names are
gone. Quote the whole command for the device's own shell instead:

```bash
adb -s "$dev" shell "stat -c '%s %n' '$p1' '$p2'"
```

```
4732539 /data/app/~~stze1bVoTfmA9t2ZQ6jM1A==/fm.isai.radio-kso2nsofsbMxpK7CCy0ohw==/base.apk
124226339 /data/app/~~cJIkYwQkTegs1uL_i8645g==/com.whatsapp-3IgdhcbFGIIrRfK3LMdJmg==/base.apk
```

The consequence is the point. The calling code mapped sizes back to apps **by path**; with no
paths in the output every lookup missed, so every app reported 0 bytes and the UI displayed
`0.0 MB` for any selection. Nothing errored — the adb call exited 0. It shipped, and came back
from a user as a UI bug, not as a command bug.

Build **one** command string and quote both the format and every path. A helper is worth having,
because a path with a space falls into the same trap:

```python
def shell_quote(text: str) -> str:
    return "'" + text.replace("'", "'\\''") + "'"

command = "stat -c '%s %n' " + " ".join(shell_quote(p) for p in paths)
```

Note the escaping idiom for an embedded single quote — close, escaped quote, reopen: `'\''`.

| Symptom | Cause |
|---|---|
| command exits 0, output is missing a field you asked for | the format string was split by the device shell |
| `stat: '%n': No such file or directory` | same thing — the format's second word became a file operand |
| a path containing a space reports "No such file or directory" | unquoted path split into two operands on the device |

And **never send binary through `adb shell`** — it rewrites LF to CRLF and corrupts images and
archives. Use `adb exec-out` (as with the `adb exec-out screencap -p` below), or have the device
write to `/data/local/tmp` and `adb pull` the file.

## Locating the SDK

Read `local.properties` first — it is what Gradle itself uses, so anything else risks installing
an APK built against a different SDK than the adb doing the install. Un-escape it: Gradle writes
`D\:\\Users\\...`, and `java.util.Properties` reads those escapes back.

Falling back to `%LOCALAPPDATA%\Android\Sdk` is a guess; the SDK is often on another drive. And
when *writing* `local.properties`, prefer forward slashes — a single-backslash path fails at
configuration time with the unhelpful `java.io.IOException: Invalid file path`.

## Verifying: the measurements that lie

This is the part that wastes the most time. Each of these looks like a pass.

### A screenshot of a locked screen is a valid PNG of nothing

`adb exec-out screencap -p` on a lock screen returns solid black — Android blocks capture there.
It is not an error and the file is well-formed. The tell is that repeated captures are
**byte-identical** (e.g. 7904 bytes every time) while a real frame varies.

Check state before trusting a capture:

```bash
adb shell dumpsys window | grep mCurrentFocus     # NotificationShade == locked
adb shell dumpsys power  | grep mWakefulness=     # Asleep == screen off
```

`mFocusedApp` can name your activity while `mCurrentFocus` is the lock screen — the app is
foreground but not visible. Capture needs `mCurrentFocus` to be your window.

### Frame-timing counters report a perfect score for an invisible app

`dumpsys gfxinfo <pkg>` on a backgrounded or screen-off app returns:

```
Total frames rendered: 0
Janky frames: 0 (0.00%)
```

Zero jank, because zero frames. This is the single most misleading output in the toolkit: it is
exactly what a perfectly optimised app would print, and it means the app was never on screen.

Always gate a measurement on focus **before and after** the run, and treat a run that lost focus
as void rather than as data:

```bash
focused() { adb shell dumpsys window | grep -q "mCurrentFocus.*$PKG"; }
focused || { echo "not focused - void"; exit 1; }
adb shell dumpsys gfxinfo "$PKG" reset
sleep 60
focused || { echo "lost focus - void"; exit 1; }
adb shell dumpsys gfxinfo "$PKG" | grep -E 'Total frames|Janky|90th|99th'
```

Reset the counters **after** warm-up, or the numbers are dominated by process start, JIT and
`ProfileInstaller` rather than steady state. And read the absolute count alongside the
percentage: a run with few frames can show a *worse* percentage while doing strictly less work,
because there are no cheap frames diluting the ratio.

Also note `p99` over a small sample is one or two frames — not a trend.

### Keeping the screen on

`svc power stayon true` only applies **while charging**, so it does nothing over wireless adb
where there is no power source. Raise the timeout instead, and restore it afterwards:

```bash
adb shell settings get system screen_off_timeout        # save it
adb shell settings put system screen_off_timeout 1800000
```

Tell the user what you changed on their device, and how to undo it.

### Synthetic input is not a reliable click

`adb shell input tap` is delivered as a short drag; the log shows `moveCount:10`, and Compose may
treat it as a scroll rather than a click. A blind tap that "did nothing" may have missed, hit the
row instead of the button, or scrolled — you cannot tell from the absence of an effect.

If the user is also touching the phone, the input stream mixes and the run is not attributable
(the log shows more `ACTION_DOWN` events than you sent). For anything that must be certain,
prefer a deterministic check over driving the UI: read the artifact, the mapping file, the
persisted file, or ask the user to perform the gesture.

### Synthetic back cannot exercise predictive back

`adb shell input keyevent KEYCODE_BACK` and `adb shell input swipe` from a screen edge do **not**
go through the predictive-back path — the one that is enabled by default for apps targeting
API 35. So a back-navigation bug that only reproduces under a real finger will pass a synthetic
back test, repeatedly and convincingly.

Observed on a Compose app whose back gesture exited to the launcher instead of going up one
screen: `input swipe 5 900 400 900 100` followed by `dumpsys activity activities` reported the
*correct* `topResumedActivity` afterwards, so the broken build was called verified — twice.
The bug was reproducible only by hand.

The same run also demonstrated the failure mode in the section above, in its purest form. Three
consecutive readings all returned:

```
topResumedActivity=ActivityRecord{… com.miui.home/.launcher.Launcher t4390}
```

The device was sitting on the launcher; the taps had gone nowhere and the app under test was
never on screen. Every reading was internally consistent and the whole run was worthless — and
it was reported as a pass, because nothing in the output says "this is not your app".

So, for any back/up/navigation behaviour:

- **Gate on focus first**, exactly as for `gfxinfo`. A `dumpsys` reading that names a package
  other than yours is void, not data — check the package name in the output, do not just grep for
  the field.
- **Confirm the app is actually installed before drawing conclusions from a quiet log.** An
  `install` that failed (see `INSTALL_FAILED_USER_RESTRICTED` above) leaves the *previous* build
  running; an empty `logcat` filter then reads like "no errors" when it means "your code never
  ran". `adb shell pm list packages | grep <pkg>` after every install.
- **Ask the user to perform the gesture.** For predictive back specifically there is no synthetic
  substitute, so a green automated result here is not evidence and should not be reported as one.

## Publishing the build: collect it, then post it

A release build that only exists in `app/build/outputs/` is one `clean` away from gone, and a
tester cannot install what they cannot reach. Two steps, in this order, after every **release**
build:

1. Copy it to `D:\GitHub\apk\<applicationId>.apk`, overwriting. That folder holds the *latest*
   build of each app, not a history, which is why the name is stable and the applicationId (not
   the repo or module name) disambiguates it. Leave the Gradle output in place; this is a copy.
2. Post it to Telegram with [tgpost](https://github.com/pandiyarajk/tgpost).
   `D:\github\tgpost\scripts\send_webservice_data.py` posts to the **AI work updates** channel (formerly "Sharing")
   (`-1004406329437`) as `@global_tgpost_bot` by default; both the APK and the
   too-large-to-attach notice below go there. Point a project at its own channel with
   `--chat-id`, or `TGPOST_CHAT_ID` in the build environment, rather than editing the script. Pass `--file` as an ABSOLUTE path: the script runs from its own folder, so a relative path fails with "no such file".

```powershell
$meta = Get-Content app\build\outputs\apk\release\output-metadata.json | ConvertFrom-Json
$el   = $meta.elements[0]
$apk  = "app\build\outputs\apk\release\$($el.outputFile)"
$size = (Get-Item $apk).Length
Copy-Item $apk "D:\GitHub\apk\$($meta.applicationId).apk" -Force

$caption = "{0} {1} ({2}) release, {3:N1} MB, {4}" -f `
    $meta.applicationId, $el.versionName, $el.versionCode, ($size / 1MB), (git rev-parse --short HEAD)

if ($size -le 50MB) {
    python D:\github\tgpost\scripts\send_webservice_data.py --file $apk --text $caption
} else {
    python D:\github\tgpost\scripts\send_webservice_data.py --text `
        "$caption - too large to attach (Bot API limit is 50 MB). Collected at D:\GitHub\apk\$($meta.applicationId).apk"
}
```

**Branch on the size yourself; do not let the sender decide.** Over the limit, tgpost skips the
file, prints a warning to stderr and exits 1 — correct behaviour for a batch, wrong here, because
the run then posts *nothing* and the channel shows no build at all. The `else` arm above is the
point of the whole section: an announcement naming the version, size and collection path is worth
far more than silence, and a release APK crosses 50 MB routinely. (If you want the file itself,
a [local Bot API server](https://github.com/tdlib/telegram-bot-api) raises the limit to 2000 MB;
point tgpost at it with `--base-url`.)

**Read `applicationId` and the version from `output-metadata.json`, not from the filename or the
Gradle DSL.** AGP writes it next to the APK on every build, so it already accounts for flavours,
`applicationIdSuffix` and whatever the version was resolved to. Parsing `app-release.apk` out of a
path tells you nothing, and re-reading `build.gradle.kts` re-derives a value the build has already
computed — which is how a flavour ends up published under the wrong id.

**Send the Gradle output, not the collected copy.** Telegram preserves the uploaded file name, so
uploading `app-release.apk` or `myapp-1.4.2-release.apk` keeps the version visible in the channel,
while the `D:\GitHub\apk\<applicationId>.apk` copy stays stable for local use. Uploading the copy
makes every post in the channel look identical.

**Scan the DEX for credentials before posting, and treat the scan as the gate.** A Telegram
upload is irreversible: every member can see it and it cannot reliably be unsent. An app that
seeds development credentials through `BuildConfig` ships them *readable in the DEX* whenever
`isMinifyEnabled = false`, which is the default for a release build that has not set up R8. The
APK looks completely ordinary.

```powershell
python - <<'PY'
import zipfile, re, sys
p = r"app\build\outputs\apk\release\app-release.apk"
blob = b"".join(zipfile.ZipFile(p).read(n)
                for n in zipfile.ZipFile(p).namelist() if n.endswith(".dex"))
pats = {
    "Atlassian token": rb"ATATT[A-Za-z0-9_\-]{20,}",
    "JWT":             rb"eyJ[A-Za-z0-9_\-]{20,}",
    "GitHub PAT":      rb"gh[pousr]_[A-Za-z0-9]{20,}",
    "AWS key":         rb"AKIA[0-9A-Z]{16}",
    "Slack token":     rb"xox[baprs]-[A-Za-z0-9-]{10,}",
    "Google API key":  rb"AIza[0-9A-Za-z_\-]{35}",
    "private key":     rb"-----BEGIN [A-Z ]*PRIVATE KEY-----",
    "email address":   rb"[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}",
}
bad = False
for label, pat in pats.items():
    hits = re.findall(pat, blob, re.I)
    if "email" in label:
        hits = [h for h in hits if not h.lower().endswith((b".png", b".xml", b".dtd"))]
    if hits:
        bad = True
        print(f"  {label}: {len(hits)} found, prefix {hits[0][:10]!r}")
print("NOT SAFE - do not publish" if bad else "clean")
sys.exit(1 if bad else 0)
PY
```

Print **prefixes only**, never a whole match: the output goes into a terminal, a transcript and
possibly a chat log, and a secret pasted there is disclosed just as surely as one uploaded.

To produce a clean build, empty the seed sources and rebuild with `--rerun-tasks` (a cached
`BuildConfig` is the trap), then **re-scan the artefact rather than trusting the procedure**.

**Emptying the properties file is not enough on its own.** A seed helper that falls back to an
environment variable — `localProps.getProperty(k).ifEmpty { System.getenv(env) }`, a common and
sensible pattern so a token already exported for `gh` need not be duplicated — will happily bake
the ambient value in after `local.properties` has been stripped. Measured on devdash-android: the
Jira, Tempo and Zephyr tokens went away and a GitHub PAT stayed, because `GITHUB_TOKEN` was
exported in the shell. Blank every fallback variable in the build shell too, and let the re-scan,
not the checklist, decide whether it worked.

Restore the credentials immediately after the build, and verify the restore (`(Get-Content
local.properties).Count`). A half-restored `local.properties` is a confusing failure an hour
later, and a backup copy left lying beside it is a second copy of the secrets.

**Publish only signed release builds.** An unsigned release APK installs nowhere
(see [release-build](../../rules/android-app/release-build.mdc)), and a debug APK handed to a
tester collides with the release key later. Both look fine as attachments. Verify before posting:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\<ver>\apksigner.bat" verify --print-certs $apk
```

**Use `apksigner`, not a `META-INF` listing, to decide whether an APK is signed.** Checking for
`META-INF/*.RSA` or `*.SF` answers a v1 (JAR signing) question, and a modern AGP release is signed
with **v2 only** — the signature lives in a block before the central directory, and `META-INF`
holds nothing. A correctly signed, installable APK therefore reads as unsigned to that test, which
sends you rebuilding something that was never broken.

**The token is read from the environment at process start.** `TGPOST_BOT_TOKEN` must be set in the
shell running the build; a terminal (or VS Code) opened before the variable was set or rotated
keeps serving the old value and the post fails with `401 Unauthorized` while the build itself looks
clean. Refresh in place with
`$env:TGPOST_BOT_TOKEN = [Environment]::GetEnvironmentVariable('TGPOST_BOT_TOKEN','User')`.

The bot must be an **administrator with "post messages"** in the channel; a bot that is not a
member gets `Bad Request: chat not found`, which reads like a wrong chat id.

## Reporting honestly

Say which measurements were void and why. "0 frames rendered, but the phone was locked, so this
proves nothing" is worth more than a clean-looking table, and a percentage quoted from an
unfocused run will be believed and then acted on.

Note the variant too: a **debug** build's Compose performance is materially worse than release,
so debug numbers are a floor, not a verdict.
