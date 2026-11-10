#!/bin/sh
# Mutation testing with PIT, limited to the given source files.
#
# Usage: sh scripts/sdd/mutate.sh [src/path/To/File.java ...]
#   No arguments: every .java file in the source folder (src/).
#   Arguments outside src/ or not ending in .java (tests, docs, deleted files) are skipped;
#   if none is left, there is nothing to mutate and the run passes.
#
# Each file is mapped to its class and its nested classes (a.b.C and a.b.C$*). The tests are
# compiled first and every test runs against the mutants (targetTests in pom.xml).
# The whole run is limited to 600 seconds. The score (killed / all mutants) is compared with
# mutation-min-score in docs/sdd/stack.md; PIT's own threshold stays unset on purpose.
# Prints the score and every surviving or uncovered mutant with file, line and change.
# Exit 0 at or above the minimum, 1 below it or when the run fails or times out.
#
# JDK: Maven runs on $SDD_JAVA_HOME if set; otherwise on macOS on JDK 21 (/usr/libexec/java_home),
# otherwise on whatever JAVA_HOME already is. The code is compiled for Java 17 (pom.xml).

set -u

root=$(cd "$(dirname "$0")/../.." && pwd) || { echo "mutate: cannot find repository root" >&2; exit 1; }
cd "$root" || exit 1

src_dir=src
time_limit=600
profile=docs/sdd/stack.md
report=target/pit-reports/mutations.xml
log=target/sdd-mutate.log

min_score=$(sed -n 's/^- mutation-min-score: *\([0-9][0-9]*\).*/\1/p' "$profile" | head -n 1)
if [ -z "$min_score" ]; then
  echo "mutate: no 'mutation-min-score: <number>' line in $profile" >&2
  exit 1
fi

if [ -n "${SDD_JAVA_HOME:-}" ]; then
  JAVA_HOME=$SDD_JAVA_HOME; export JAVA_HOME
elif [ -x /usr/libexec/java_home ] && jh=$(/usr/libexec/java_home -v 21 2>/dev/null); then
  JAVA_HOME=$jh; export JAVA_HOME
fi

if [ "$#" -eq 0 ]; then
  set -- $(find "$src_dir" -name '*.java' | sort)
fi

classes=""
skipped=""
for f in "$@"; do
  f=${f#./}
  case "$f" in
    "$src_dir"/*.java) ;;
    *) skipped="$skipped $f"; continue ;;
  esac
  case "$f" in
    */package-info.java|*/module-info.java) skipped="$skipped $f"; continue ;;
  esac
  if [ ! -f "$f" ]; then
    skipped="$skipped $f"; continue
  fi
  cls=$(printf '%s' "${f#"$src_dir"/}" | sed 's/\.java$//' | tr '/' '.')
  classes="${classes:+$classes,}$cls,$cls\$*"
done

[ -n "$skipped" ] && echo "skipped (not a source file under $src_dir/):$skipped"
if [ -z "$classes" ]; then
  echo "mutate: no source files given, no mutants. Score: n/a (passes)"
  exit 0
fi

mkdir -p target
# No stale report (a crashed run must not reuse the last score) and no stale classes
# (Maven keeps .class files of deleted tests, which would still run and kill mutants).
rm -rf target/pit-reports target/classes target/test-classes

echo "mutating: $classes"
perl -e 'alarm shift; exec @ARGV' "$time_limit" \
  mvn -B test-compile org.pitest:pitest-maven:mutationCoverage "-DtargetClasses=$classes" > "$log" 2>&1
status=$?
if [ "$status" -eq 142 ]; then
  echo "mutate FAILED: time limit of ${time_limit}s reached (log: $log)"
  exit 1
fi
if [ "$status" -ne 0 ]; then
  tail -n 30 "$log"
  echo "mutate FAILED: PIT exited with $status (log: $log)"
  exit 1
fi

if [ ! -f "$report" ]; then
  # failWhenNoMutations=false: PIT ends successfully but writes no report when the given
  # classes have no mutants (an interface, a constants class). That run passes.
  if grep -q -e "No mutations found" -e "Created 0 mutation test units" "$log"; then
    echo "mutate: no mutants in the given files. Score: n/a (passes)"
    exit 0
  fi
  if grep -q "Project has either no tests or no production code" "$log"; then
    echo "mutate FAILED: there are no tests, so no mutant can be killed (PIT skipped the run)"
    exit 1
  fi
  tail -n 30 "$log"
  echo "mutate FAILED: no report at $report (log: $log)"
  exit 1
fi

# One <mutation> element per record, whatever the line layout of the report.
awk -v q="'" -v min="$min_score" -v src="$src_dir" '
  function tag(rec, name,   s) {
    if (match(rec, "<" name ">[^<]*</" name ">")) {
      s = substr(rec, RSTART, RLENGTH); sub("^<" name ">", "", s); sub("</" name ">$", "", s)
      gsub(/&quot;/, "\"", s); gsub(/&lt;/, "<", s); gsub(/&gt;/, ">", s); gsub(/&amp;/, "\\&", s)
      return s
    }
    return ""
  }
  function attr(rec, name,   s) {
    if (match(rec, name "=" q "[^" q "]*" q)) {
      s = substr(rec, RSTART + length(name) + 2, RLENGTH - length(name) - 3); return s
    }
    return ""
  }
  { buf = buf $0 " " }
  END {
    n = split(buf, parts, "</mutation>")
    total = 0; killed = 0; out = ""
    for (i = 1; i <= n; i++) {
      rec = parts[i]
      if (index(rec, "<mutation ") == 0) continue
      total++
      if (attr(rec, "detected") == "true") { killed++; continue }
      cls = tag(rec, "mutatedClass"); sub(/\$.*/, "", cls); gsub(/\./, "/", cls)
      mut = tag(rec, "mutator"); sub(/.*\./, "", mut)
      out = out sprintf("  %-11s %s/%s.java:%s  %s.%s: %s (%s)\n", attr(rec, "status"), src, cls, \
        tag(rec, "lineNumber"), tag(rec, "mutatedClass"), tag(rec, "mutatedMethod"), tag(rec, "description"), mut)
    }
    if (total == 0) { print "mutate: no mutants in the given files. Score: n/a (passes)"; exit 0 }
    survived = total - killed
    printf "mutants: %d, killed: %d, survived: %d (incl. no coverage)\n", total, killed, survived
    if (survived > 0) printf "surviving mutants:\n%s", out
    printf "score: %.1f%% (minimum %d%%)\n", killed * 100 / total, min
    if (killed * 100 < min * total) { print "mutate FAILED: score below mutation-min-score"; exit 1 }
    print "mutate ok"
    exit 0
  }
' "$report"
