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
