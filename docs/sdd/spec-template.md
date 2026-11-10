# Spec: <feature>

Status: draft | approved by <name>, <date>

## Intent
Three lines at most: who needs what, and why.

## Decisions
- <decision> (decided by <name or role>)

## Acceptance criteria
- AC-01 <rule>. Example: <input> → <expected result>. Boundaries: <exact limits, rounding, empty or invalid input>.

## Always state
- Units for every number (px, ms, degrees, ticks; deltaTime is 10 ms), and which point a position means (top-left or centre).
- Exact boundary values with their comparison (< or <=), and the float tolerance tests use.
- How time and randomness are controlled in tests (injected clock, fixed seed, or exact allowed range).
- The class and method the tests call, reachable without threads, Swing, assets or static screen state.
- Where comments and code disagree, which one is the intended behaviour (record it under Decisions).

## Existing behaviour this changes
- <existing test or behaviour that this spec overrides, and the new expectation> (or: none)

## Out of scope
- <what this feature deliberately does not do>

## Open questions
- <question> (owner: <name>)
