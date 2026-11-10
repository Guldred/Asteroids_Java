#!/bin/sh
# SDD gate: runs every check in order and stops at the first failure.
#
# Usage: sh scripts/sdd/gate.sh [base]
#   base: the branch or commit the change is compared with for mutation testing
#         (default: sdd-setup; in CI: origin/<target branch>).
#
# Checks, in this order:
#   1. typecheck (javac --release 17 over src/) and lint (none in the profile)
#   2. the whole test suite (mvn clean test)
#   3. the test lock (scripts/sdd/lock.sh, every fingerprint file)
#   4. mutation testing (scripts/sdd/mutate.sh) on the .java files under src/ changed
#      against <base>: committed (<base>...HEAD), staged, unstaged and untracked.
#      Fails below mutation-min-score in docs/sdd/stack.md. No changed source files: passes.
# Exit 0 when every check passes, 1 when one fails (the message names the check).
# There is deliberately no flag, variable or argument that skips a check.
#
# JDK: $SDD_JAVA_HOME if set; otherwise on macOS JDK 21 via /usr/libexec/java_home;
# otherwise the JAVA_HOME / java already set up (for example by actions/setup-java in CI).

set -u

root=$(cd "$(dirname "$0")/../.." && pwd) || { echo "gate: cannot find repository root" >&2; exit 1; }
cd "$root" || exit 1

src_dir=src
profile=docs/sdd/stack.md

if [ "$#" -gt 1 ]; then
  echo "gate: at most one argument, the base (the gate has no options)" >&2
  exit 1
fi
case "${1:-}" in
  -*) echo "gate: unknown option '$1' (the gate has no options; the only argument is the base)" >&2; exit 1 ;;
esac
base=${1:-sdd-setup}

fail() {
  echo "gate FAILED: $1"
  exit 1
}

step() {
  echo
  echo "== gate: $1"
}

# One JDK for every check.
if [ -n "${SDD_JAVA_HOME:-}" ]; then
  JAVA_HOME=$SDD_JAVA_HOME; export JAVA_HOME
elif [ -x /usr/libexec/java_home ] && jh=$(/usr/libexec/java_home -v 21 2>/dev/null); then
  JAVA_HOME=$jh; export JAVA_HOME
fi
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
  javac_cmd=$JAVA_HOME/bin/javac
else
  javac_cmd=javac
fi
echo "gate: base $base, JDK ${JAVA_HOME:-from PATH}"

# 1. typecheck and lint
step "typecheck"
src_files=$(find "$src_dir" -name '*.java' | sort)
[ -n "$src_files" ] || fail "typecheck (no .java files under $src_dir/)"
# shellcheck disable=SC2086 # file names under src/ contain no spaces
"$javac_cmd" --release 17 -d out $src_files || fail "typecheck (javac --release 17)"
echo "typecheck ok"

step "lint"
echo "lint: none (profile)"

# 2. whole test suite
step "tests"
mvn -B -q clean test || fail "tests (mvn clean test)"
echo "tests ok"

# 3. test lock
step "lock"
sh scripts/sdd/lock.sh || fail "lock (a locked test or fingerprint changed)"

# 4. mutation testing on changed source files
step "mutation"
mutation_line=$(sed -n 's/^- mutation: *//p' "$profile" | head -n 1)
case "$mutation_line" in
  none*)
    reason=$(printf '%s' "$mutation_line" | sed -n 's/^none *(\(..*\)).*/\1/p')
    [ -n "$reason" ] || fail "mutation (profile says 'mutation: none' without a reason)"
    echo "mutation: not configured ($reason)"
    echo
    echo "gate ok"
    exit 0
    ;;
esac

git rev-parse --verify --quiet "$base^{commit}" > /dev/null \
  || fail "mutation (base '$base' is not a known branch or commit; in CI fetch the full history)"
committed=$(git diff --name-only "$base...HEAD") || fail "mutation (git diff $base...HEAD)"
unstaged=$(git diff --name-only) || fail "mutation (git diff)"
staged=$(git diff --name-only --cached) || fail "mutation (git diff --cached)"
untracked=$(git ls-files --others --exclude-standard) || fail "mutation (git ls-files)"

changed=$(printf '%s\n%s\n%s\n%s\n' "$committed" "$unstaged" "$staged" "$untracked" \
  | grep "^$src_dir/.*\.java\$" | sort -u)
set --
for f in $changed; do
  [ -f "$f" ] && set -- "$@" "$f"
done
if [ "$#" -eq 0 ]; then
  echo "mutation: no changed source files under $src_dir/ against $base, nothing to mutate (passes)"
else
  echo "changed source files: $*"
  sh scripts/sdd/mutate.sh "$@" || fail "mutation (score below mutation-min-score, or the run failed)"
fi

echo
echo "gate ok"
exit 0
