# Post-Merge Synchronization And Branch Cleanup Guide

Follow this after a Pull Request has been merged and the remote branch is no longer needed.

## 1. Confirm A Clean Workspace

Do not switch branches or delete branches while unrelated work is uncommitted.

```bash
git status --short --branch
```

If there are local changes, commit, stash, or intentionally leave them on the current branch before continuing.

## 2. Return To Main

```bash
git switch main
```

Use `git checkout main` only when `git switch` is unavailable.

## 3. Fast-Forward Main From Origin

```bash
git pull --ff-only
```

`--ff-only` prevents accidental merge commits during local synchronization.

## 4. Prune Deleted Remote Branches

```bash
git fetch --prune
```

## 5. Delete The Merged Local Branch

```bash
git branch -d <feature-branch-name>
```

Use `-d` by default because it refuses to delete branches Git does not consider merged. Use `-D` only when you explicitly intend to discard that local branch.

## 6. Optional Cleanup

Run this only when dependency, Gradle, database, or generated build state changed enough to justify a fresh local build.

```bash
./gradlew clean
```

On Windows PowerShell:

```powershell
.\gradlew.bat clean
```

## 7. Start The Next Branch

Create the next work branch from updated `main`:

```bash
git switch -c <new-branch-name>
```
