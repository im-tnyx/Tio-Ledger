# Commit attribution guard

Tracked by [#66](https://github.com/im-tnyx/Tio-Ledger/issues/66). `AGENTS.md` remains the normative policy.

## What it enforces

- A `pull_request_target` runner executes trusted `main` code at the PR base SHA; the PR head is fetched as Git objects only, never checked out or executed.
- `scripts/check_commit_attribution.sh` inspects every commit in `base..head`, parses Git trailers, and checks common free-form AI-credit footers.
- It rejects provider/model `Co-Authored-By` and similar AI signatures or self-credit. Ordinary human co-authors, sign-offs and tool discussion are allowed. New unknown AI identities cannot be detected perfectly from arbitrary names.
- Exit status: `0` clean or exact owner-approved PR exception, `1` prohibited credit, `2` technical failure. Technical failures are never exempted.
- The Actions job `Attribution guard runner` publishes a different required Check Run named `Commit attribution guard` using a dedicated GitHub App. The check must be pinned to that App ID.

## Required owner/admin activation

1. Install the existing `Tio Attribution Guard` GitHub App on `im-tnyx/Tio-Ledger`, or register a new dedicated Checks-only App. Grant `Checks: Read and write` plus `Metadata: Read`, without unrelated permissions. Keep the installation scoped to this repository.
2. Create Actions environment `attribution-guard-trusted` and restrict deployment branches to `main` only. Save `ATTRIBUTION_GUARD_APP_PRIVATE_KEY` as an environment secret (the PEM private key) and `ATTRIBUTION_GUARD_APP_CLIENT_ID` as an environment variable. Never store the key in Git.
3. Land the reviewed PR only once the environment/App are ready. Validate a normal PR publishes a successful App-owned Check Run on its exact head SHA.
4. Enable PR requirements and status checks on protected `main`; require the App-owned `Commit attribution guard` check with its exact source App ID, not the Actions job and not a generic `github-actions` check of the same name. Enforce administrators, if that is the approved policy.
5. Verify clean, bad (AI co-author), and remediated disposable PRs; make sure the bad PR is blocked. Record exact run links and App identity in #66 before closing it. Never merge a deliberately bad validation PR.

**Not yet active:** Until the GitHub App, trusted environment and App-source-pinned branch protection have been configured and live-tested, the PR gate is not an enforceable repository protection. Current GitHub integration cannot administer these settings (branch-protection endpoint returned HTTP 403).

## Developer remediation

Run `bash scripts/check_commit_attribution_test.sh` and inspect the checker log to find the offending feature-branch commit. Remove any prohibited trailer/footer from that feature branch only. Published feature-branch rewrites need explicit owner approval and `git push --force-with-lease`; never rewrite `main`. Supply explicit clean squash-merge subject/body.

An exceptional owner-approved PR may be authorized through the repository Actions variable `AI_ATTRIBUTION_EXCEPTION_PR` set to its exact numeric PR ID. Clear it after that PR. An exception never suppresses technical failures.

Historical cleanup is outside this work and remains in [#83](https://github.com/im-tnyx/Tio-Ledger/issues/83). No Kotlin, KMP, SQLDelight, finance, ledger or UI logic changes are required.
