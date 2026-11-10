# SDD workshop: build a trusted agentic process into your project

This file is your Claude's instruction set for the workshop. You type no terminal commands: Claude does every step, asks before anything it installs, and stops so you can look at what changed.

**Before the first step:** open Claude Code in the folder you want to work in (a project of your own, a customer project, or an empty folder): the Claude Desktop app (Code tab) is recommended, the `claude` CLI works too. Keep the permission mode on **Ask permissions**. You do not need to download anything yourself: the first sentence lets Claude fetch this file.

**Every step is one sentence to Claude, when the presenter shows it on a slide:**

> Execute step N of sdd-workshop.md.

Claude does the step and stops at each **STOP**, so you can look before it goes on. Say `continue` when you have looked. Behind? Answer your open STOP first (or say `skip`), then send the next sentence; every step starts by committing the previous one.

**Looking at your changes:** after each step, open the diff view of the Claude Desktop app, or ask Claude `Show me what this step changed.` Every step from 2 on first commits the previous step as `Step N: …`, so the diff shows exactly one step, and the history tells the story step by step.

The workshop has two parts.

- **Part 1, steps 1 to 7: install the process.** Claude adds a spec-driven development (SDD) process to your project, one piece per step, on a new branch `sdd-setup`. The presenter explains each piece on the slides while your Claude builds it, and every piece ends with a self-test.
- **Part 2, step 8: use it.** Claude creates a feature branch and builds one small test feature with the full process: a spec written with a team of agents, tests first, an implementation by a separate agent, mutation testing, an independent review, the gate, and a retrospective.

The process adapts to your stack: step 1 detects your language, build tool, test runner, CI and a fitting mutation tool, and every later step reads that profile. The [BMAD Method](https://github.com/bmad-code-org/BMAD-METHOD) is the base: its role agents and Party Mode discuss specs and retrospectives in one conversation.

## What you end up with

| Piece | Files in your repository | Step |
| --- | --- | --- |
| Stack profile: the exact commands for this repository | `docs/sdd/stack.md` | 1 |
| Spec template and the rules of the process | `docs/sdd/spec-template.md`, a section in `CLAUDE.md` | 2 |
| Test lock: approved and existing tests cannot change unnoticed | `scripts/sdd/lock*`, `docs/sdd/baseline.sha256` (each feature's `docs/sdd/features/<feature>/acceptance.sha256` follows in part 2) | 3 |
| Mutation testing, scoped to changed files | the tool's config, `scripts/sdd/mutate*` | 4 |
| Independent agents: test writer, implementer, reviewer | `.claude/agents/sdd-*.md` | 5 |
| Gate, locally and in your CI pipeline | `scripts/sdd/gate*`, one job in your CI config | 6 |
| Protection: locked tests, fingerprints, gate and CI change only with a human | `CODEOWNERS`, `.claude/settings.json` | 6 |
| The feature workflow that chains all of it | `.claude/skills/sdd-feature/SKILL.md` | 7 |

## Ground rules, for you and for your agent

- **Part 1** works on the branch `sdd-setup`, **part 2** on a branch `feature/<name>` created from it. Commit only on these branches and only as the steps say. Never push, open a pull request or delete a branch. The branches are yours to keep or throw away.
- **Part 1 does not change production code or existing tests.** It adds files, and it changes only `.gitignore`, `CLAUDE.md` (append only), the build file where a tool must be registered (for example `package.json`, `pom.xml`, `build.gradle`, `composer.json`, `pyproject.toml`) and its dependency lock file, your CI config, `CODEOWNERS` and `.claude/settings.json`. A self-test may change a test for a moment; it restores the file at once with `git restore`.
- **Part 2** changes production code, but only through the workflow, and approved acceptance tests only after you agree.
- No secrets, customer data or customer code in the Teams chat. Post outcomes only, such as “Step 4: 14 mutants, 3 survived”.
- You never need a terminal. If you want to see something, ask Claude.
- If a step does not fit your stack, say so in the chat and skip it.

Agent: these rules apply to every step below. The user types no commands: run every command yourself, and when the user wants to see something (a diff, the history, a file), show it. **Ask before every install** and before any command that deletes or changes something these rules do not cover. When you need a tool's command line or config, follow that tool's current documentation instead of guessing flags. If the repository has several modules or apps, ask once which one this workshop uses and record it in the stack profile; every later step works only there.

## Part 1 — Install the system

## Step 1 — Setup and stack profile (at the start of the session, 5 to 8 minutes)

Send, as the first thing you say in Claude:

> Download https://raw.githubusercontent.com/Guldred/Putting-AI-Code-to-the-Test/main/sdd-workshop.md into this folder, then execute step 1 of sdd-workshop.md.

(If you already saved this file as `sdd-workshop.md` in the folder, `Execute step 1 of sdd-workshop.md.` is enough.)

Claude asks before every install (the BMAD skills, `uv` if missing, a test runner if it creates a project); approve each with a click. In **Auto** mode Claude's safety check can refuse the installs, and Claude then stops and tells you so. The skills installer prints “Review skills before use; they run with full agent permissions”; that is expected. If the step ends with “Step 1 blocked”, do what the line says if you can; otherwise post it in the chat and watch; nobody debugs your machine live. The step takes 5 to 8 minutes and runs while the talk goes on.

**Agent:** do the following in order and say what you are doing in one line each. If something from this step already exists (the branch, the workshop file, the skills, `_bmad/`, the profile), check it, say so in one line and go on. If a permission check refuses a command, do not work around it: **STOP** and print exactly `Step 1 blocked: Claude was not allowed to run <command>. Switch the permission mode to Ask permissions, then say continue.` On `continue`, run that command again.

1. **Check the tools.** Run `git --version`, `node --version`, `npx --version` and `uv --version`.
   - Git missing: **STOP** and print exactly `Step 1 blocked: install Git from https://git-scm.com, then say continue.`
   - Node.js missing or older than 22: **STOP** and print exactly `Step 1 blocked: install Node.js 22 or newer from https://nodejs.org, then say continue.` (BMAD's installer needs Node, whatever language your project uses.)
   - `uv` missing: ask once, then install it (macOS and Linux: `curl -LsSf https://astral.sh/uv/install.sh | sh`; Windows PowerShell: `powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"`). Use the path the installer prints if the shell does not find `uv` yet, and verify with `uv --version`.
   - No internet or a blocked download: **STOP** with `Step 1 blocked: no internet access for installs.`
2. **Pick the repository.** If the current folder is a Git repository: use it, and create and switch to the branch `sdd-setup` (or switch to it if it exists). If it has uncommitted changes, ask once: commit them on the new branch as `Before the workshop`, or **STOP** so the user can deal with them first. Later steps commit everything, so nothing of the user's may stay uncommitted.
   If it is not a repository or is empty: ask once whether to create a minimal project here. Then `git init -b main`, set up TypeScript 5 (`typescript@5`; mutation tools do not all support TypeScript 7 yet) with Vitest, a `.gitignore` with `node_modules/` and the usual build and report folders, and write one function `discountedTotal(subtotalCents, code)` in `src/checkout.ts`: the code SAVE10 gives 10 % off, orders under €100 pay €5 shipping. Add three happy-path tests in `tests/checkout.test.ts`, the way a quick vibe-coding session would, with exactly these inputs: €50 without a code, €100 with SAVE10, €200 with SAVE10; their expected values are whatever your function returns. Any reasonable reading of the rule is fine; nobody has decided the details yet, and that is the point. Commit this on `main` as `Initial project`, then create and switch to the branch `sdd-setup`. From here on the new repository is treated like an existing one.
3. **Keep this file** as `sdd-workshop.md` in the repository root, unchanged; every later step reads it from there. If it is missing, download it from https://raw.githubusercontent.com/Guldred/Putting-AI-Code-to-the-Test/main/sdd-workshop.md; if it is somewhere else, move it there and say so.
4. **Install the BMAD skills:**
   ```sh
   npx skills add bmad-code-org/BMAD-METHOD -a claude-code -y --copy -s bmad -s bmod-core-tools -s bmod-method -s bmad-party-mode -s bmad-facilitate -s bmad-agent-analyst -s bmad-agent-pm -s bmad-agent-po -s bmad-agent-architect -s bmad-agent-ux-designer -s bmad-agent-dev -s bmad-code-review -s bmad-review
   ```
   `skills-lock.json` stays tracked on purpose: it records which skills are installed, so teammates can reinstall them. Then make sure `.gitignore` contains these five lines, adding the missing ones:
   ```
   .claude/skills/bmad*/
   .claude/skills/bmod*/
   _bmad/
   _bmad-output/
   .bmad-help-setup-modules.toml
   ```
5. **Set up BMAD.** Use the `bmad` skill you just installed and run its setup for this project with every default, passing only this project's `.claude/skills` folder as the skills root (never scan or change global skills); it needs no answers from the user. It creates `_bmad/`. Check that `_bmad/scripts/resolve_config.py` exists, then run `uv run _bmad/scripts/resolve_config.py --project-root . --key core.output_folder`; it prints JSON containing that key. If the setup fails, read the error once, fix what is yours to fix, try once more, and otherwise **STOP** with `Step 1 blocked: BMAD setup failed: <one line>`.
6. **Write the stack profile** `docs/sdd/stack.md`. Detect, do not install anything here:
   - **Module:** the folder this workshop works in (`.` for the whole repository).
   - **Language and build tool**, from the build files.
   - **Commands**, exactly as they run in this repository, each one verified by running it once where that is quick: `test` (the whole suite), `test-one` (one test file or class, with a placeholder such as `<file>` or `<Class>`), `typecheck` (for compiled languages: compiling the code and the tests) and `lint` (or `none`), `build` (or `none`).
   - **Source and test folders**, and the naming pattern of test files.
   - **CI:** the existing pipeline file, from this list or what you find: `.github/workflows/*.yml` (GitHub Actions), `.gitlab-ci.yml` (GitLab CI), `azure-pipelines.yml` (Azure Pipelines), `bitbucket-pipelines.yml` (Bitbucket Pipelines), `Jenkinsfile` (Jenkins), `.circleci/config.yml` (CircleCI). If there is none, write `none` and the Git remote's host, if any.
   - **Mutation tool:** the one already in the project, or the usual one for the stack: StrykerJS (JavaScript, TypeScript), PIT (Java, Kotlin; Maven or Gradle plugin), Infection (PHP; needs a coverage driver such as PCOV or Xdebug), mutmut (Python), Stryker.NET (C#), Stryker4s (Scala), cargo-mutants (Rust), Gremlins (Go), mutant (Ruby). Write `installed` or `proposed`; step 4 installs it.
   - **Scripts:** the language for the SDD scripts: POSIX shell (`.sh`) by default, PowerShell (`.ps1`) if the project is Windows-only, or the project's own task runner (npm scripts, Make, Composer scripts, Maven or Gradle tasks) only if the project already runs several of its checks through it.
   - **Mutation minimum score:** `mutation-min-score: 80` unless the project already has a threshold; the gate uses it in step 6.
   - **Notes:** anything a later step must know, such as a test suite that is already red or slower than two minutes.

   Keep it under 40 lines.
7. **Report, then STOP.** Commit nothing: whatever this step changed stays uncommitted for the user to look at, and step 2 commits it. Report: the repository and branch; the Node and `uv` versions; the BMAD module version (from the setup output or `.claude/skills/bmod-method/bmod.toml`) or `not installed`; the stack profile in five lines (language, test command, CI, mutation tool, script language); and the sentence for the chat, `Step 1 done: <language>, <mutation tool>, <CI>` or `Step 1 blocked: <reason>`. Ask the user to correct the profile if anything is wrong. **STOP.**

**You should see:** a profile of your repository you agree with. **Chat:** “Step 1 done: TypeScript, StrykerJS, GitHub Actions.”

## Step 2 — Say what correct means: the spec stage

**Agent:**

0. **Commit step 1 first.** Check that the current branch is `sdd-setup`; if not, **STOP** and say so. Run `git add -A` and `git commit -m "Step 1: setup"`; if there is nothing to commit, say so. Show the commit in one line (`git log --oneline -1`). From here on commit nothing: this step's changes stay uncommitted, so the diff shows exactly this step. *(Every later step starts the same way, with its own number; it is written out once here.)*
1. **Write `docs/sdd/spec-template.md`:**
   ```markdown
   # Spec: <feature>

   Status: draft | approved by <name>, <date>

   ## Intent
   Three lines at most: who needs what, and why.

   ## Decisions
   - <decision> (decided by <name or role>)

   ## Acceptance criteria
   - AC-01 <rule>. Example: <input> → <expected result>. Boundaries: <exact limits, rounding, empty or invalid input>.

   ## Existing behaviour this changes
   - <existing test or behaviour that this spec overrides, and the new expectation> (or: none)

   ## Out of scope
   - <what this feature deliberately does not do>

   ## Open questions
   - <question> (owner: <name>)
   ```
2. **Ask the team what every spec in this repository must pin down.** Party Mode needs the BMAD setup: if `_bmad/scripts/resolve_config.py` is missing, run step 1, item 5 again first, or use the three-role fallback below. Start Party Mode (the `bmad-party-mode` skill) on this topic:
   > This repository (read docs/sdd/stack.md and skim the code). We are adding a spec template that every feature must fill in before any code is written: docs/sdd/spec-template.md. What must a spec here always state so that tests can be written from it without asking? Which boundaries, units, error cases or compatibility questions keep coming back in this kind of code? Two rounds, concrete, disagree where you do.

   If Party Mode asks about an initiative or a project to attach the discussion to, answer that this is a loose discussion without one. Let the room run two rounds; the user may steer. Party Mode keeps going until told to stop, so after two rounds ask it to wrap up. Then ask the room to wrap up with at most five additions to the template.
   **STOP.** Show the proposed additions. The user picks which ones go in.
3. Add the chosen ones to the template, as extra lines under *Acceptance criteria* or as a short section *Always state*. Keep the template under 50 lines.
4. **Append this section to `CLAUDE.md`** (create the file if it does not exist; never change what is already there):
   ```markdown
   ## Spec-driven development (SDD)

   New features and behaviour changes in this repository follow docs/sdd/. Start them with the `sdd-feature` skill.

   1. No production code without an approved spec: docs/sdd/features/<feature>/spec.md, from docs/sdd/spec-template.md, with `Status: approved by <name>`. Ambiguities are decided by a human and written down under Decisions, never guessed. Existing tests the change contradicts are named in the spec under Existing behaviour this changes; only those may get a new expected value.
   2. Tests first: the sdd-test-writer agent writes one test per acceptance criterion from the spec alone. Every new rule has at least one test that fails for the right reason (a value mismatch, not a syntax, compile or import error) before any implementation exists.
   3. Approved tests are locked in docs/sdd/features/<feature>/acceptance.sha256, existing tests in docs/sdd/baseline.sha256. Only a human changes them and refreshes their fingerprints; Claude asks before editing them (.claude/settings.json), the lock check catches every other change, and CODEOWNERS makes the repository require a human review.
   4. The sdd-implementer agent makes the tests pass without touching locked tests.
   5. Mutation testing runs on the changed files. A surviving mutant is a question for a human: add a test, or accept it with a reason in docs/sdd/features/<feature>/mutation.md. The gate fails below the minimum score in docs/sdd/stack.md.
   6. The sdd-reviewer agent reviews the change in a fresh context, against the spec, and changes nothing.
   7. The gate (see docs/sdd/stack.md) runs every check; nothing ships when it fails, and nothing skips a check.

   Human attention goes to: the spec, changes to locked tests, surviving mutants, review findings, and the final decision. Code is read where that evidence points.
   ```
   **STOP.** Show the template and the new `CLAUDE.md` section.

If the `bmad-party-mode` skill is not found although step 1 installed it, this session has not loaded it yet: **STOP** and print `Type /reload-skills, then say: Execute step 2 of sdd-workshop.md. If that does not help, open a new Claude session in this folder and say it there.` If Party Mode is not available at all, play three roles yourself (analyst, product owner, developer) and let them argue for two rounds.

**You should see:** a template that asks for what your code actually gets wrong. **Chat:** “Step 2: template plus N additions from Party Mode.”

## Step 3 — A test must fail first: the test lock

**Agent:**

0. **Commit step 2 first** (as in step 2, item 0, with `Step 2: spec template and SDD rules`).
1. **Write the lock script** `scripts/sdd/lock` in the script language from the profile (`lock.sh`, `lock.ps1`, or a task in the project's runner; record the exact command in the profile under `lock`). It:
   - reads `docs/sdd/baseline.sha256` and every `docs/sdd/features/*/acceptance.sha256`, or only the fingerprint files given as arguments; paths inside them are relative to the repository root;
   - recomputes the SHA-256 of every listed test file (`shasum -a 256` on macOS, `sha256sum` on Linux, `Get-FileHash` on Windows; the files use the usual `<hash>  <path>` lines);
   - prints `lock ok: N files` and exits 0, or names every changed or missing file and exits 1;
   - has no option that skips a file.
2. **Self-test it**, without touching the repository's tests: create a temporary folder with a small text file and a fingerprint file for it, run the lock with that fingerprint file as argument (it passes), change one character in the text file, run it again (it fails and names the file), then delete the temporary folder. A fingerprint file outside the repository may use absolute paths.
3. **Lock the existing tests as a baseline:** write the fingerprints of every existing test file to `docs/sdd/baseline.sha256`, so that a change to an old test is noticed too, not only to new acceptance tests. Run the lock without arguments: `lock ok: N files`.
4. **Add the line `lock: <command>` to the profile.**
   **STOP.** Show the script and the three runs.

**You should see:** a check that notices a one-character change in an approved test. **Chat:** “Step 3: lock passes, catches an edit, N existing test files locked.”

## Step 4 — Test the tests: mutation testing

**Agent:**

0. **Commit step 3 first** (`Step 3: test lock`).
1. **Install the mutation tool** from the profile, after asking. Follow its current documentation for this build tool and test runner. Configure it so that:
   - it runs the project's normal test command or runner;
   - it mutates only the files it is given (the tool's own option for limiting files or classes), with the profile's source folder as the default;
   - the whole run has a time limit (about ten minutes), so a large codebase cannot block the session; if the tool only limits single mutants, the wrapper enforces it. macOS has no `timeout` command; a pattern that works on macOS and Linux is `perl -e 'alarm shift; exec @ARGV' 600 <tool command>`. Never leave a background `sleep` watchdog running: it keeps the gate's output open after the tool has finished;
   - its reports go to a folder that `.gitignore` excludes.
   - a run that finds no mutants (for example only test files changed) does not fail;
   - the wrapper compares the report's score with `mutation-min-score` from the profile and fails below it; leave the tool's own break threshold unset, so the profile stays the only place for that number.
   Known pitfalls: **PIT** needs the JUnit 5 plugin for JUnit 5 tests, takes class names instead of files (map each file to its class and include nested classes, `Class$*`), needs the tests compiled first, and limits time only per mutant, so the wrapper needs its own time limit (macOS has no `timeout` command). **Infection** needs a coverage driver. **StrykerJS** needs the runner plugin for the project's test runner and may not support TypeScript 7 yet; if it crashes on the TypeScript version, say so and use the manual procedure instead of inventing a workaround. **mutmut** works on paths and can be slow on large suites.
   If the tool needs something the user must install system-wide (for example a PHP coverage driver), **STOP** and say exactly what. If there is no usable tool for this stack, ask the user for a one-line reason, write `mutation: none (<reason>)` in the profile and use the manual procedure in item 4.
2. **Write the wrapper** `scripts/sdd/mutate` in the profile's script language: it takes source files as arguments, runs the tool on exactly those, and prints the score plus every surviving mutant with file, line and the change. Record `mutate: <command>` in the profile.
3. **Self-test it** on one small source file that already has tests (in a project created in step 1: `src/checkout.ts`). Report the number of mutants, killed and survived, and for each survivor the input that would tell the original and the mutant apart. Do not fix anything: survivors in existing code are information, not today's job.
4. **Manual procedure, only if there is no tool:** change one comparison or constant in that file (`<` to `<=`, a limit by one), run the tests, report killed or survived, and restore the file with `git restore`.
   **STOP.** Show the config diff and the result.

If the suite is slower than two minutes, run the self-test on a single test file (`test-one` in the profile) and say so.

**You should see:** whether your existing tests notice a broken rule, often for the first time. **Chat:** “Step 4: N mutants, N survived.”

## Step 5 — Independent agents: author, implementer, reviewer

This is where Claude's subagents come in: each one starts with an empty context and only the brief it is given, so the reviewer has not seen the author's reasoning, and the test writer has not seen the implementation. You see them as separate, expandable runs in the conversation.

**Agent:**

0. **Commit step 4 first** (`Step 4: mutation testing`).
1. **Write three agent files** in `.claude/agents/`, exactly as below; the agents read the commands from the profile when they run.

   `.claude/agents/sdd-test-writer.md`:
   ```markdown
   ---
   name: sdd-test-writer
   description: Writes acceptance tests from an approved SDD spec, before any implementation exists. Use in the tests stage of the sdd-feature workflow.
   tools: Read, Grep, Glob, Write, Edit, Bash
   ---
   You write tests from a specification, not from an implementation.

   Input: the path of an approved spec, docs/sdd/features/<feature>/spec.md.
   1. Read the spec, docs/sdd/stack.md and the existing tests' style. Do not read or change production code beyond the public signatures the tests must call. If a signature the tests need does not exist yet, you may add a stub of it only (no logic: a dummy return value), so the red is a value mismatch and not a compile or import error; say so.
   2. Write one test per acceptance criterion, named after it (AC-01 …), with the spec's example and boundaries, in a new test file for this feature.
   3. Run them with the profile's test-one command. Every new rule needs at least one test that fails with a value mismatch. Tests that pin behaviour the spec keeps unchanged may pass; mark them as such. A syntax, compile, import or setup error is not a valid red: fix the test.
   4. Run the whole suite and list every existing test that fails or that contradicts the spec. Each one must be named in the spec under Existing behaviour this changes; if it is not, report it as a spec gap.
   5. Write the fingerprints of your new test files to docs/sdd/features/<feature>/acceptance.sha256.
   Report: the test files, one line per test with its criterion and red or green, the red output, existing tests that contradict the spec, and any criterion you could not express as a test.
   ```

   `.claude/agents/sdd-implementer.md`:
   ```markdown
   ---
   name: sdd-implementer
   description: Implements an SDD feature until its locked acceptance tests pass, without changing them. Use in the implementation stage of the sdd-feature workflow.
   tools: Read, Grep, Glob, Write, Edit, Bash
   ---
   You make locked acceptance tests pass with the smallest sensible change.

   Input: the path of an approved spec and its acceptance.sha256.
   1. Read the spec and the locked tests. Never change a file listed in an acceptance.sha256. Never change an expected value to match your code; the only exception is an existing test the spec names under Existing behaviour this changes, which you update to exactly the expectation the spec gives; the lock will then fail on it, and a human approves the new fingerprint. If any other test contradicts the spec, stop and report it; do not change it.
   2. Implement the smallest change that makes the tests pass, then refactor for readability. Run the whole suite after each.
   3. Run the lock command from docs/sdd/stack.md. If it fails on a file you were not allowed to change, undo your change to that file.
   Report: the diff of every file you changed, the commands and their actual results, and what the tests do not prove.
   ```

   `.claude/agents/sdd-reviewer.md`:
   ```markdown
   ---
   name: sdd-reviewer
   description: Reviews an SDD change in a fresh context against its spec. Read-only. Use in the review stage of the sdd-feature workflow.
   tools: Read, Grep, Glob, Bash
   ---
   You are an independent reviewer. You have not seen how this change was made, and you change nothing.

   Input: the path of a spec and a Git range (base..head).
   1. Read the spec. Review `git diff <range>`. Run the test command from docs/sdd/stack.md if it needs no install. Use Bash only to read, diff and run checks; never edit, commit or install.
   2. For each finding: severity, file and line, the violated acceptance criterion, a reproducing input, expected versus observed. Separate defects from product questions. Ground every finding in the code or in a run, and say what you could not verify.
   3. End with one recommendation: accept, request changes, or need more evidence. No findings is not proof of correctness.
   ```
2. **Say in one line** why the test writer and the implementer are separate: the tests come from the spec, not from the code that will satisfy them. And say what enforces the rules: the agents' instructions do not stop an edit to a test file; the lock (new and existing tests) and the gate do.
3. **Self-test the reviewer** on the last two commits: run the `sdd-reviewer` agent on the range `HEAD~2..HEAD` (the lock script and the mutation wrapper) with steps 3 and 4 of `sdd-workshop.md` as its spec, ignoring dependency lock files such as `package-lock.json`, and show its recommendation. Claude Code loads a new agent file within seconds, except the first one in a new `.claude/agents/` folder, which needs a new session. So if the `sdd-reviewer` agent is not available yet in this session, start a general-purpose subagent instead and give it the text of `.claude/agents/sdd-reviewer.md` below the frontmatter as its instructions; say that you did so.
   **STOP.** Show the three files and the reviewer's report.

Stronger independence, optional: in the Claude Desktop app, ask Claude to start a **separate session** in this folder with the reviewer brief. It appears as its own row in the sidebar; open it in split view and watch it beside the author.

**You should see:** a reviewer run as its own visible subagent, with its own findings. **Chat:** “Step 5: three agents, reviewer says: …”

## Step 6 — No check is optional: the gate, locally and in CI

**Agent:**

0. **Commit step 5 first** (`Step 5: independent agents`).
1. **Write the gate** `scripts/sdd/gate` in the profile's script language. It runs, in this order, and stops at the first failure with a nonzero exit code and a message naming the failed check:
   1. `typecheck` and `lint` from the profile, if they exist;
   2. the whole test suite;
   3. the lock;
   4. mutation testing on the source files changed against the base branch (`git diff --name-only <base>...HEAD` plus uncommitted and untracked files, filtered to the source folder): it fails below `mutation-min-score` from the profile and lists the survivors. If the profile says `mutation: none`, it prints `mutation: not configured (<reason from the profile>)` and passes this check only if the user wrote that reason; otherwise it fails. The base is `sdd-setup` by default and can be passed as the only argument. No changed source files: it says so and passes this check.
   It has no flag, variable or argument that skips a check. Record `gate: <command>` in the profile.
2. **Add the gate to CI.** In the pipeline file from the profile, add one job or stage that runs the gate on every pull request or merge request, following that platform's current documentation and the file's existing style; reuse the pipeline's existing setup (runtime, caches) where it has one. Change nothing else in the file. If adding the trigger would change when existing jobs run (in GitHub Actions, `on:` applies to the whole file), put the job in a new pipeline file instead, for example `.github/workflows/sdd-gate.yml`. In CI, `sdd-setup` does not exist and checkouts are often shallow: check out the full history and pass the target branch as the gate's base (GitHub Actions: `fetch-depth: 0` and `origin/${{ github.base_ref }}`; GitLab: `origin/$CI_MERGE_REQUEST_TARGET_BRANCH_NAME`). If the profile says `none`, ask once which platform to use (propose the one matching the Git remote) and create a minimal pipeline that only runs the gate. If a linter for the file is installed (for example `actionlint`), run it; otherwise say `CI config unverified: it runs on the first push`.
3. **Protect the protection.** The lock notices a changed test, but whoever can change a test can also change its fingerprint, the gate or the pipeline. Two layers close that gap:
   - **A required human review in the repository.** Write a `CODEOWNERS` file in the place your platform reads it (GitHub: `.github/CODEOWNERS`; GitLab: `CODEOWNERS` or `.gitlab/CODEOWNERS`). It assigns the test folders from the profile, `docs/sdd/` (specs and fingerprints), `scripts/sdd/` and the CI file to an owner. Ask the user once for the owner: a user or team handle, never invented. If the platform has no `CODEOWNERS` (for example Azure DevOps or Bitbucket Server), write the same list to `docs/sdd/protected-paths.md` and say that the platform's required-reviewer policy must point at it. You cannot switch the review on: tell the user to enable it after merging (GitHub: branch protection with “Require review from Code Owners”; GitLab: protected branch with “Code owner approval”).
   - **Claude asks before touching them.** Create or extend `.claude/settings.json` (keep every existing entry) with `permissions.ask` rules, following the current Claude Code permissions documentation: one `Edit(/<path>)` rule per file listed in `docs/sdd/baseline.sha256`, plus `Edit(/docs/sdd/**/*.sha256)`, `Edit(/scripts/sdd/**)` and `Edit(/<CI file>)`. Paths start with `/`, relative to the project root. `Edit` rules cover every built-in tool that edits files, so Claude must ask the user before it changes any of them, and the user decides at that moment. Use `ask`, not `deny`: an approved change (a spec that names an existing test) must still be possible.
   Say the limits in one line each: a shell command can still write a file without asking, which is why the lock, the gate and the required review stay in place; and the review protects only once it is switched on in the platform.
4. **Self-test the gate:** run it (it passes, or fails on something that already failed before this workshop: report that, do not fix it). Then change one expected value in an existing test with your edit tool: Claude Code must now ask the user first (the new `ask` rules); the user approves it for this self-test. If no question appears, the new settings are not loaded in this session yet: say so; part 2 starts a new session, which loads them. Run the gate again (it blocks and names the tests), and restore the file with `git restore <file>`. Run it once more. On `sdd-setup` no source file has changed yet, so the mutation check says so; step 4 tested the wrapper, and part 2 exercises the check.
   **STOP.** Show the gate, the CI diff, `CODEOWNERS`, the new rules in `.claude/settings.json`, the three gate runs, and whether Claude Code asked before the test edit. Tell the user which review setting to switch on in their platform.

**You should see:** one command that refuses, the same command in your pipeline, and every way around it (editing a locked test, a fingerprint, the gate or the pipeline) needing a human. **Chat:** “Step 6: gate passes, blocks, passes; CI job and protection added.”

## Step 7 — The workflow: one entry point for every feature

**Agent:**

0. **Commit step 6 first** (`Step 6: gate and CI`).
1. **Write `.claude/skills/sdd-feature/SKILL.md`:**
   ````markdown
   ---
   name: sdd-feature
   description: Build a feature or behaviour change in this repository with the spec-driven process in docs/sdd/. Use when the user asks for a new feature or a change in behaviour.
   argument-hint: <feature in one sentence>
   ---
   Build this feature with the SDD process: $ARGUMENTS

   Read CLAUDE.md (section Spec-driven development) and docs/sdd/stack.md first. Choose a short kebab-case <feature> name and work in docs/sdd/features/<feature>/.
   Each stage ends with a STOP: show what the stage produced and wait. When the user says continue, first commit the stage's changes as "<feature>: <stage>" (if a stage changed no files, say so and go on), then go on. Never skip a stage; if one cannot run, say why and ask.

   1. **Spec.** Start Party Mode (the bmad-party-mode skill) on the feature: what is unclear, what would each role decide differently, which concrete inputs and boundaries must the spec state? Two rounds, then decisions and open questions. STOP: the user answers the open questions. Write spec.md from docs/sdd/spec-template.md with the user's decisions. Search the existing tests, and the code the feature touches, for behaviour it changes, including behaviour no test pins (a rounding step, the order of operations), and list it under Existing behaviour this changes, so the user decides it now and not in stage 3. STOP: the user approves; set `Status: approved by <name>, <date>`.
   2. **Tests first.** Run the sdd-test-writer agent with the spec path. Show its red output and acceptance.sha256. Add one `Edit(/<path>)` rule per new test file to `permissions.ask` in .claude/settings.json, so they are protected like the baseline. Claude Code asks the user before writing acceptance.sha256 and settings.json: that question is the human approving the lock. STOP.
   3. **Implementation.** Run the sdd-implementer agent with the spec and fingerprint paths. Then run the lock and the whole suite yourself. Show green, the lock result and the diff stat. If the lock fails only on existing tests the spec names under Existing behaviour this changes, show their diff: the user approves, then refresh exactly those lines in docs/sdd/baseline.sha256. STOP.
   4. **Mutation.** Run the mutate command on the production files changed in stage 3. For each survivor name the input that would kill it. STOP: for each survivor the user decides: add a test (then add it in the feature's test file, update acceptance.sha256 and say that a human approved the change), or accept it with a reason written to docs/sdd/features/<feature>/mutation.md. If accepted survivors keep the score below `mutation-min-score`, only the user may lower that value in the profile, with the reason in mutation.md.
   5. **Review.** Run the sdd-reviewer agent with the spec path and the range from the branch point to HEAD (`git merge-base sdd-setup HEAD`..HEAD). Pass only that brief, never your own summary of the work. Save its report unedited as review.md. STOP: the user decides which findings to fix; fixes go back through stage 3 and the lock.
   6. **Gate.** Run the gate. Show its result. STOP.
   7. **Retrospective.** Start Party Mode: what went well and what did not in this feature's spec, tests, review and gate, and what should change in docs/sdd/ next time? Two rounds, then action items. STOP: the user picks the actions; write retro.md with Actions agreed (owner each), What went well, What did not, Discussed but not agreed, and commit it as "<feature>: retrospective".

   If a named agent is not available in this session, start a general-purpose subagent with the text of its file in .claude/agents/ as instructions, and say so.
   ````
2. **Check the whole system once:** list every file from the table at the top of this workshop file with `present` or `missing` (feature files under `docs/sdd/features/` come in part 2), and run the gate.
   **STOP.** Report the list and the gate result. The chat line is `Step 7: system complete, gate green` or what is missing.

**You should see:** the complete system, committed step by step on `sdd-setup`. **Chat:** “Step 7: system complete.”

## Part 2 — Use it on a real feature

## Step 8 — A feature, built by the process (20 to 30 minutes)

Start a **new Claude session in the same folder**, so the agents and the skill from steps 5 and 7 are loaded and nothing of part 1's conversation carries over. Then send:

> Execute step 8 of sdd-workshop.md.

**Agent:**

0. **Commit step 7 first.** Check that the current branch is `sdd-setup`; if not, **STOP** and say so. Then commit as in step 2, item 0, with `Step 7: sdd-feature workflow`.
1. **Check the system.** Run step 7's system check again (every file from the table at the top present, gate green). If a piece is missing, name it and **STOP**: the user finishes that step first, or decides to go on without it.
2. **Pick the feature.** Ask the user for a small feature or behaviour change, one that a short spec can describe (a validation rule, a limit, a calculation, a filter, an error message). If the user has none, propose three from the code, each in one sentence, smallest first; in a project created in step 1, propose first: “SAVE10 gives at most €15 discount per order.”
3. **Create the feature branch** from `sdd-setup`: `git switch -c feature/<feature>`. Show `git log --oneline -3`.
4. **Run the workflow:** use the `sdd-feature` skill with the feature sentence and follow it stage by stage, with all its STOPs. If the skill is not available in this session, read `.claude/skills/sdd-feature/SKILL.md` and follow it as written; say that you did so.

**You should see, stage by stage:** the team's questions before any code; tests that fail for the right reason; an implementation by an agent that never wrote the tests; mutants that tell you whether the tests are strong enough; a reviewer that never saw the work; a gate that decides; and a retrospective that improves `docs/sdd/` for the next feature. **Chat:** post the stage you reached and one surprise, for example “Stage 4: 2 survivors, one was a real gap.”

## After the session

- Ask Claude `Show me the history of sdd-setup and of my feature branch.`: one commit per step of part 1, one per stage of part 2.
- To keep the process: review `sdd-setup` like any other change and merge it the way your team works. The first push runs the new CI job.
- To throw it away: ask Claude `Switch back to my main branch and delete the branches sdd-setup and feature/<feature>.` The untracked `_bmad/` folder and the `.claude/skills/bmad*` copies can be deleted at any time.
