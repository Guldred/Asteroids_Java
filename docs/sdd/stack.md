# Stack profile

- module: `.` (single IntelliJ module `Asteroids_Java.iml`)
- language: Java, language level 17 (`.idea/misc.xml`); default JDK on this machine is 26, so compile with `--release 17`
- build tool: none (plain IntelliJ project, no `pom.xml` / `build.gradle`); Maven 3.9.16 and Gradle 9.6.1 are installed locally

## Commands
- test: none (no test framework, no tests yet)
- test-one: none
- typecheck: `javac --release 17 -d out $(find src -name '*.java')` (verified: compiles, `out/` is gitignored)
- lint: none
- build: none (the IDE builds and runs `game.main.Main`)
- lock: `sh scripts/sdd/lock.sh`

## Folders
- source: `src/` (packages `game.component`, `game.object`, `game.object.projectiles`, `game.main`; assets in `src/game/resource/`)
- tests: none; proposed `test/` with `*Test.java` mirroring the `game.*` packages

## CI
- ci: none; Git remote host is github.com (`Guldred/Asteroids_Java`), so GitHub Actions fits

## Mutation
- mutation: PIT (proposed; needs a build tool and JUnit 5 plugin, see notes)
- mutation-min-score: 80

## Scripts
- scripts: POSIX shell (`.sh`)

## Notes
- The repository has no tests and no test runner. Step 3's baseline lock will list 0 files, and step 4 needs a test setup first: proposed a minimal Maven `pom.xml` (sourceDirectory `src`, testSourceDirectory `test`, JUnit 5, PIT with `pitest-junit5-plugin`), installed only after asking.
- Assets are loaded via relative `File` paths (`SoundManager`, `BackgroundManager`), so anything that runs the game needs the repository root as working directory; `Vector2` (pure math, no dependencies) is the natural first test target; `CollisionDetector` uses `java.awt.geom` and the game objects.
