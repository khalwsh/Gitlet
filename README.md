# Gitlet

A minimal, self-contained version-control system written in Java. Gitlet is a
faithful clone of the core Git workflow — commits, branches, merges, remotes —
implemented from scratch over the filesystem. Inspired by UC Berkeley's CS61B
[Project 2](https://sp21.datastructur.es/materials/proj/proj2/proj2).

---

## Table of Contents

1. [Features](#features)
2. [Project Layout](#project-layout)
3. [Build](#build)
4. [Tests](#tests)
5. [Run](#run)
6. [Commands](#commands)
7. [Storage Model](#storage-model)
8. [Architecture Overview](#architecture-overview)

---

## Features

- Single-file `add`/`rm` staging with content-addressed blobs.
- Snapshot commits with deterministic SHA-1 identifiers (timezone-independent).
- Linear `log`, repository-wide `global-log`, and `find` by message.
- Branches, `checkout` (file, file@commit, or branch), and `reset`.
- Three-way `merge` with conflict markers and automatic merge commits.
- Linear `rebase` of the current branch onto a target branch.
- Remotes: `add-remote`, `rm-remote`, `fetch`, `push`, `pull`.
- Abbreviated commit IDs (`reset abc123`, `checkout abc123 -- file`).
- Detailed `status` covering staged, removed, modified-but-not-staged, and
  untracked files.
- Binary-safe blob storage — non-UTF-8 files (PNG, JAR, etc.) round-trip
  exactly through add/commit/checkout.

---

## Project Layout

```
Gitlet/
├── pom.xml
├── README.md
├── approach.md
└── src/
    ├── main/java/com/gitlet/
    │   ├── Main.java
    │   ├── Repository.java
    │   ├── model/{Branch,Commit}.java
    │   ├── storage/{BlobStore,BranchStore,CommitStore,HeadRef,
    │   │            RemoteStore,StagingArea,WorkingArea}.java
    │   └── util/{CommitGraph,FileUtils,GitletException,
    │              GitletExitException}.java
    └── test/java/com/gitlet/
        ├── support/TestSupport.java
        ├── model/{BranchTest,CommitTest}.java
        ├── storage/*Test.java
        ├── util/{FileUtilsTest,CommitGraphTest}.java
        └── integration/{InitCommitTest,BranchCheckoutTest,MergeTest,
                         RebaseTest,ResetTest,RemoteTest,StatusTest,
                         DeleteRepoTest,RequireRepositoryTest,
                         MainCliTest,BinaryBlobTest}.java
```

---

## Build

Gitlet targets Java 17 and uses Maven.

```bash
mvn package
```

This produces `target/gitlet.jar` with `com.gitlet.Main` as the entry point.

To compile without Maven:

```bash
javac -d target/classes $(find src/main/java -name '*.java')
```

## Tests

164 unit and integration tests cover every package, including binary-file
round-tripping and the CLI dispatch in `Main`. Run them with Maven:

```bash
mvn test
```

In IntelliJ (or any IDE with Maven support), reload the project and right-click
any test class, package, or `src/test/java` to run.

---

## Run

Drop into any working directory and invoke the CLI:

```bash
java -cp target/classes com.gitlet.Main <command> [args...]
# or, after `mvn package`:
java -jar target/gitlet.jar <command> [args...]
```

For convenience you can wrap this in a shell function:

```bash
gitlet() { java -jar /absolute/path/to/gitlet.jar "$@"; }
```

---

## Commands

| Command | Synopsis | Description |
| --- | --- | --- |
| `init` | `gitlet init` | Create `.gitlet/` and seed the `master` branch with the initial commit. |
| `add` | `gitlet add <file>` | Stage `<file>` for the next commit. Unstages it from removal if pending. |
| `rm` | `gitlet rm <file>` | Stage `<file>` for removal and delete it from the working directory if tracked. |
| `commit` | `gitlet commit "<message>"` | Snapshot staged changes onto a new commit on the current branch. |
| `log` | `gitlet log` | Print the first-parent history starting from `HEAD`. |
| `global-log` | `gitlet global-log` | Print every commit ever recorded, in no particular order. |
| `find` | `gitlet find "<message>"` | Print the IDs of all commits whose message exactly matches `<message>`. |
| `status` | `gitlet status` | Show branches, staged/removed files, unstaged changes, and untracked files. |
| `checkout` | `gitlet checkout -- <file>` | Restore `<file>` from the head commit. |
| | `gitlet checkout <commitId> -- <file>` | Restore `<file>` from the given commit (abbreviated IDs allowed). |
| | `gitlet checkout <branch>` | Switch to `<branch>`. |
| `branch` | `gitlet branch <name>` | Create a new branch pointing at the current commit (does not switch). |
| `rm-branch` | `gitlet rm-branch <name>` | Delete a branch pointer. |
| `reset` | `gitlet reset <commitId>` | Move the current branch and working tree to `<commitId>`. |
| `merge` | `gitlet merge <branch>` | Three-way merge `<branch>` into the current branch. |
| `rebase` | `gitlet rebase <branch>` | Replay current-branch commits onto `<branch>`. |
| `add-remote` | `gitlet add-remote <name> <path>` | Register a remote whose `.gitlet` lives at `<path>`. |
| `rm-remote` | `gitlet rm-remote <name>` | Forget a registered remote. |
| `push` | `gitlet push <remote> <branch>` | Fast-forward push commits to `<remote>`. |
| `fetch` | `gitlet fetch <remote> <branch>` | Pull `<remote>/<branch>` commits and blobs into the local repository. |
| `pull` | `gitlet pull <remote> <branch>` | `fetch` followed by `merge`. |
| `delete-repo` | `gitlet delete-repo` | Permanently delete the `.gitlet` directory in the current repository. |

---

## Storage Model

```
.gitlet/
├── HEAD                       # name of the current branch
├── branches/<name>            # serialized Branch objects
├── commits/<sha1>             # serialized Commit objects
├── blobs/<sha1>               # raw file content keyed by SHA-1
├── staged/
│   ├── addition/<filename>    # body = SHA-1 of the staged content
│   └── removal/<filename>     # body = SHA-1 of the removed-from-HEAD content
└── remotes/<remote>/
    ├── <remote>               # text file containing the remote's .gitlet path
    └── <branch>               # cached remote branch ref (from fetch)
```

- **Commits** are content-addressed by a SHA-1 over `message`, the parent and
  optional second-parent hashes, the timestamp (in milliseconds since the
  epoch), and the sorted `(filename, blobHash)` pairs. The initial commit's
  identifier is therefore deterministic across machines.
- **Blobs** are deduplicated by content hash — identical file contents share a
  single blob across all commits and branches.
- **Branches** are tiny serialized `(name, commitHash)` references.
- The **staging area** is just two directories on disk; an empty `addition/`
  and `removal/` mean there is nothing to commit.

---

## Architecture Overview

| Package | Responsibility |
| --- | --- |
| `com.gitlet` | `Main` (CLI dispatch) and `Repository` (orchestrator). |
| `com.gitlet.model` | Domain objects `Commit` and `Branch`. |
| `com.gitlet.storage` | Filesystem-backed stores: `CommitStore`, `BranchStore`, `BlobStore`, `RemoteStore`, `StagingArea`, `WorkingArea`, `HeadRef`. |
| `com.gitlet.util` | `FileUtils` (SHA-1, file I/O, serialization), `CommitGraph` (BFS LCA, ancestor walks), `GitletException`, `GitletExitException` (thrown by `exitWithMessage`, caught at the `Main` boundary). |

`Repository` is the only class that mutates multiple stores in a single
operation; everything else is single-purpose and pure with respect to its
backing directory. The CLI in `Main` does nothing more than parse arguments
and delegate, wrapping the dispatch in a single `try`/`catch` that converts
`GitletExitException` into stdout + a clean `System.exit`.
