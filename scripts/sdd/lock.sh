#!/bin/sh
# Test lock: checks that every test file listed in a fingerprint file is unchanged.
#
# Usage: sh scripts/sdd/lock.sh [fingerprint-file ...]
#   No arguments: reads docs/sdd/baseline.sha256 and every
#   docs/sdd/features/*/acceptance.sha256.
#   Fingerprint lines are "<sha256>  <path>" (sha256sum / shasum -a 256 format).
#   Relative paths are relative to the repository root; absolute paths are used as they are.
#
# Prints "lock ok: N files" and exits 0, or names every changed or missing file and exits 1.
# There is deliberately no option to skip a file.

set -u

root=$(cd "$(dirname "$0")/../.." && pwd) || { echo "lock: cannot find repository root" >&2; exit 1; }
cd "$root" || exit 1

if command -v sha256sum >/dev/null 2>&1; then
  hash_file() { sha256sum < "$1" | cut -d' ' -f1; }
elif command -v shasum >/dev/null 2>&1; then
  hash_file() { shasum -a 256 < "$1" | cut -d' ' -f1; }
else
  echo "lock: neither sha256sum nor shasum found" >&2
  exit 1
fi

if [ "$#" -eq 0 ]; then
  set -- docs/sdd/baseline.sha256
  for f in docs/sdd/features/*/acceptance.sha256; do
    [ -f "$f" ] && set -- "$@" "$f"
  done
fi

checked=0
failed=0

for list in "$@"; do
  case "$list" in
    -*) echo "lock: unknown option '$list' (the lock has no options)" >&2; exit 1 ;;
  esac
  if [ ! -f "$list" ]; then
    echo "missing fingerprint file: $list"
    failed=$((failed + 1))
    continue
  fi
  n=0
  while IFS= read -r line || [ -n "$line" ]; do
    n=$((n + 1))
    line=$(printf '%s' "$line" | tr -d '\r')
    [ -z "$line" ] && continue
    expected=$(printf '%s' "$line" | cut -c1-64 | tr 'A-F' 'a-f')
    sep=$(printf '%s' "$line" | cut -c65-66)
    path=$(printf '%s' "$line" | cut -c67-)
    case "$expected" in
      *[!0-9a-f]*) expected="" ;;
    esac
    if [ "${#expected}" -ne 64 ] || { [ "$sep" != "  " ] && [ "$sep" != " *" ]; } || [ -z "$path" ]; then
      echo "malformed line $n in $list"
      failed=$((failed + 1))
      continue
    fi
    checked=$((checked + 1))
    if [ ! -f "$path" ]; then
      echo "missing: $path ($list)"
      failed=$((failed + 1))
    elif [ "$(hash_file "$path")" != "$expected" ]; then
      echo "changed: $path ($list)"
      failed=$((failed + 1))
    fi
  done < "$list"
done

if [ "$failed" -gt 0 ]; then
  echo "lock FAILED: $failed problem(s), $checked files checked"
  exit 1
fi
echo "lock ok: $checked files"
exit 0
