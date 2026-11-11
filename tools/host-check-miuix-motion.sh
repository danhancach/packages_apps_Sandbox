#!/usr/bin/env bash
# Host check: compile + run pure Kotlin motion sources (no Android/Gradle/Soong build).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KOTLINC="${KOTLINC:-/home/tuan/android/external/kotlinc/bin/kotlinc}"
OUT="$(mktemp -d "${TMPDIR:-/tmp}/sandbox-host-miuix-motion-check.XXXXXX")"
trap 'rm -rf -- "$OUT"' EXIT
mkdir -p "$OUT/classes"

SOURCES=(
  "$ROOT/src/com/android/axion/sandbox/ui/motion/MiuixTitleMotion.kt"
  "$ROOT/src/com/android/axion/sandbox/ui/motion/MiuixPageMotion.kt"
  "$ROOT/src/com/android/axion/sandbox/ui/motion/PagerChildFlingKc2Logic.kt"
  "$ROOT/src/com/android/axion/sandbox/ui/motion/PagerAxisOwnership.kt"
  "$ROOT/src/com/android/axion/sandbox/ui/motion/MiuixStretchMath.kt"
  "$ROOT/src/com/android/axion/sandbox/ui/motion/MiuixStretchReacquire.kt"
  "$ROOT/src/com/android/axion/sandbox/ui/motion/StretchBusyDeferGate.kt"
  "$ROOT/src/com/android/axion/sandbox/ui/motion/StretchBusyLifecycle.kt"
  "$ROOT/src/com/android/axion/sandbox/ui/SandboxBottomNavGeometry.kt"
  "$ROOT/tools/HostMiuixTitleMotionCheck.kt"
  "$ROOT/tools/HostMiuixPageMotionCheck.kt"
  "$ROOT/tools/HostPagerKc2Check.kt"
  "$ROOT/tools/HostPagerAxisOwnershipCheck.kt"
  "$ROOT/tools/HostSubmenuHeaderGeometryCheck.kt"
  "$ROOT/tools/HostBottomNavGeometryCheck.kt"
  "$ROOT/tools/HostStretchBusyCheck.kt"
  "$ROOT/tools/HostSandboxWiringCheck.kt"
)

KOTLIN_LIB="$(cd "$(dirname "$KOTLINC")/../lib" && pwd)"
STDLIB="$KOTLIN_LIB/kotlin-stdlib.jar"
"$KOTLINC" -cp "$STDLIB" -d "$OUT/classes" "${SOURCES[@]}"
java -cp "$OUT/classes:$STDLIB" com.android.axion.sandbox.tools.HostMiuixTitleMotionCheck
java -cp "$OUT/classes:$STDLIB" com.android.axion.sandbox.tools.HostMiuixPageMotionCheck
java -cp "$OUT/classes:$STDLIB" com.android.axion.sandbox.tools.HostPagerKc2Check
java -cp "$OUT/classes:$STDLIB" com.android.axion.sandbox.tools.HostPagerAxisOwnershipCheck
java -cp "$OUT/classes:$STDLIB" com.android.axion.sandbox.tools.HostSubmenuHeaderGeometryCheck
java -cp "$OUT/classes:$STDLIB" com.android.axion.sandbox.tools.HostBottomNavGeometryCheck
java -cp "$OUT/classes:$STDLIB" com.android.axion.sandbox.tools.HostStretchBusyCheck
java -cp "$OUT/classes:$STDLIB" com.android.axion.sandbox.tools.HostSandboxWiringCheck "$ROOT"
echo "ALL sandbox host miuix motion checks PASS"
