# Stack profile

- module: `.` (single IntelliJ module `Asteroids_Java.iml`)
- language: Java, language level 17 (`.idea/misc.xml`); default JDK on this machine is 26, so compile with `--release 17`
- build tool: Maven 3.9.16, minimal `pom.xml` added in step 4 for tests and mutation only (sources `src/`, tests `test/`, `maven.compiler.release` 17); the game is still built and run from IntelliJ
- JDK for Maven: 21 (`/usr/libexec/java_home -v 21`, Zulu 21); the default JDK 26 is not used to run Maven/PIT. `scripts/sdd/mutate.sh` picks it itself (override with `SDD_JAVA_HOME`)

## Commands
- test: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -q clean test` (JUnit 5.14.4; passes with "No tests to run" while there are none; `clean` so .class files of deleted tests cannot run)
- test-one: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -q clean test -Dtest=<TestClass>` (e.g. `-Dtest=Vector2Test`)
- typecheck: `javac --release 17 -d out $(find src -name '*.java')` (verified: compiles, `out/` is gitignored)
- lint: none
- build: none (the IDE builds and runs `game.main.Main`)
- lock: `sh scripts/sdd/lock.sh`

## Folders
- source: `src/` (packages `game.component`, `game.object`, `game.object.projectiles`, `game.main`; assets in `src/game/resource/`)
- tests: `test/` (Maven `testSourceDirectory`), `*Test.java` mirroring the `game.*` packages; none exist yet

## CI
- ci: none; Git remote host is github.com (`Guldred/Asteroids_Java`), so GitHub Actions fits

## Mutation
- mutation: PIT 1.30.2 with pitest-junit5-plugin 1.2.3 (`pom.xml`; reports in `target/pit-reports/`, gitignored)
- mutate: `sh scripts/sdd/mutate.sh <src/...java files>` (no arguments: all of `src/`; 600 s limit; score compared with mutation-min-score)
- mutation-min-score: 80

## Scripts
- scripts: POSIX shell (`.sh`)

## Notes
- The repository has no tests yet (baseline lock: 0 files). Step 4 added the minimal Maven `pom.xml` (JUnit 5 + PIT) after asking. Until tests exist, `mutate` on any source file fails with "there are no tests" (PIT skips the run before it counts mutants, so this includes files without mutants); once tests exist, files without mutants (for example an interface) pass, and so do runs with only non-source files.
- Assets are loaded via relative `File` paths (`SoundManager`, `BackgroundManager`), so anything that runs the game needs the repository root as working directory; `Vector2` (pure math, no dependencies) is the natural first test target; `CollisionDetector` uses `java.awt.geom` and the game objects.
