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
