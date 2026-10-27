#!/usr/bin/env bash
set -euo pipefail

./gradlew -PabypassAbis=x86_64 assembleDebug assembleDebugAndroidTest --stacktrace
adb logcat -c
californium_ui_status=0
timeout --signal=INT --kill-after=30s 6m ./gradlew -PabypassAbis=x86_64 -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true connectedDebugAndroidTest --stacktrace || californium_ui_status=$?
adb logcat -d -v time -s TestRunner:I > ui-test-progress.txt || true
adb pull /sdcard/Android/data/app.abypass/files/screenshots ui-screenshots || true
if [ "$californium_ui_status" -ne 0 ]; then
    timeout 10s adb exec-out screencap -p > ui-timeout.png || true
fi
exit "$californium_ui_status"
