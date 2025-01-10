## Table of Contents
1. [Overview](#overview)
2. [Dependencies](#dependencies)
3. [How To Run](#how-to-run)
4. [Gitlet Functionality](#gitlet-functionality)
   - [init](#init)
   - [add](#add)
   - [commit](#commit)
   - [rm](#rm)
   - [log](#log)
   - [global-log](#global-log)
   - [find](#find)
   - [status](#status)
   - [checkout](#checkout)
   - [branch](#branch)
   - [rm-branch](#rm-branch)
   - [reset](#reset)
   - [merge](#merge)
   - [rebase](#rebase)
   - [delete-repo](#delete-repo)
   - [add-remote](#add-remote)
   - [rm-remote](#rm-remote)
   - [push](#push)
   - [fetch](#fetch)
   - [pull](#pull)

## Overview
A version-control system is essentially a backup system for related collections of files. The main functionality that Gitlet supports is:

- Saving the contents of entire directories of files. In Gitlet, this is called committing, and the saved contents themselves are called commits.

- Restoring a version of one or more files or entire commits. In Gitlet, this is called checking out those files or that commit.

- Viewing the history of your backups. In Gitlet, you view this history in something called the log.

- Maintaining related sequences of commits, called branches.

- Merging changes made in one branch into another.

In Gitlet, you don’t just commit individual files at a time. Instead, you can commit a coherent set of files at the same time. We like to think of each commit as a snapshot of your entire project at one point in time.

Maintaining a commit history of all versions of the project:
<p align="center">
  <img src="https://github.com/user-attachments/assets/d7d364c7-41ab-42ac-a4b3-49f0e7920a7d" alt="Commit History">
</p>

In Gitlet, we support branching and have different histories of commits derived from the same commit. We also support merging them into one new commit.
<p align="center">
  <img src="https://github.com/user-attachments/assets/eb11b532-fd33-48a7-a2a4-3b3417be10a0" alt="Branching and Merging">
</p>

## Dependencies
1. Install Java: [https://www.java.com/en/download/](https://www.java.com/en/download/)

## How To Run
1. Download the repo and unzip it.
2. Open `gitlet-master` then open the `gitlet` folder.
3. Open Git Bash and navigate to the path in step 3. Use `cd ..` to go one directory back, `cd folder_name` to enter the specified folder, and use `pwd` to know where you currently are. (NOTE: Use the previous commands without the braces.)
4. Compile the program by typing `javac *.java`.
5. Run the program by typing `java Main [THE COMMANDS GO HERE WITHOUT THE SQUARE BRACKETS EACH ARGUMENT SHOULD BE SEPARATED BY A SPACE OR CAPSULATE THE DESIRED ARGUMENT IN DOUBLE QUOTES]`.
6. Supported commands are listed below.

## Gitlet Functionality

### <b>init</b>
   - Creates a new Gitlet version-control system in the current directory. This system will automatically start with one commit: a commit that contains no files and has the commit message `initial commit` (just like that, with no punctuation). It will have a single branch: `master`, which initially points to this initial commit, and `master` will be the current branch. The timestamp for this initial commit will be `00:00:00 UTC, Thursday, 1 January 1970`. Since the initial commit in all repositories created by Gitlet will have exactly the same content, it follows that all repositories will automatically share this commit (they will all have the same UID) and all commits in all repositories will trace back to it.

### <b>add</b>
   - Adds a copy of the file as it currently exists to the staging area. If the current working version of the file is identical to the version in the current commit, Gitlet will not stage it to be added. This ensures that only changes are tracked, avoiding unnecessary duplication of files in the repository.

### <b>commit</b>
   - Saves a snapshot of tracked files in the current commit and staging area so they can be restored at a later time, creating a new commit. The commit is said to be tracking the saved files. By default, each commit’s snapshot of files will be exactly the same as its parent commit’s snapshot of files; it will keep versions of files exactly as they are, and not update them. A commit will only update the contents of files it is tracking that have been staged for addition at the time of commit, in which case the commit will now include the version of the file that was staged instead of the version it got from its parent. A commit will save and start tracking any files that were staged for addition but weren’t tracked by its parent. Finally, files tracked in the current commit may be untracked in the new commit as a result of being staged for removal. The staging area is cleared after a commit.

### <b>rm</b>
   - Unstage the file if it is currently staged for addition. If the file is tracked in the current commit, Gitlet stages it for removal and removes the file from the working directory if the user has not already done so. This ensures that the file is no longer tracked in the next commit.

### <b>log</b>
   - Starting at the current head commit, display information about each commit backwards along the commit tree until the initial commit, but only consider the first parent in case more than one parent. This provides a linear history of the project, showing the sequence of changes made over time.

### <b>global-log</b>
   - Like `log`, except displays information about all commits ever made. The order of the commits does not matter. This is useful for finding specific commits across all branches in the repository.

### <b>find</b>
   - Prints out the ids of all commits that have the given commit message, one per line. If there are multiple such commits, it prints the ids out on separate lines. This is helpful for locating specific changes based on commit messages.

### <b>status</b>
   - Displays some statistics about the current state of the repository, including the current branch, staged files, and untracked files. This provides a quick overview of the repository's status.

### <b>checkout</b>
   - Takes the version of the file as it exists in the head commit and puts it in the working directory, overwriting the version of the file that’s already there if there is one. The new version of the file is not staged. This command can also be used to switch branches or restore files from specific commits.

### <b>branch</b>
   - Creates a new branch with the given name, and points it at the current head commit. A branch is nothing more than a name for a reference (a SHA-1 identifier) to a commit node. This command does NOT immediately switch to the newly created branch (just as in real Git). Before you ever call `branch`, your code should be running with a default branch called “master”.

### <b>rm-branch</b>
   - Deletes the branch with the given name. This only means to delete the pointer associated with the branch; it does not mean to delete all commits that were created under the branch, or anything like that. This is useful for cleaning up unused branches.

### <b>reset</b>
   - Checks out all the files tracked by the given commit. Removes tracked files that are not present in that commit. Also moves the current branch’s head to that commit node. See the intro for an example of what happens to the head pointer after using `reset`. The `[commit id]` may be abbreviated as for `checkout`. The staging area is cleared. The command is essentially `checkout` of an arbitrary commit that also changes the current branch head.

### <b>merge</b>
   - Merges files from the given branch into the current branch. Any files that have been modified in the given branch since the split point, but not modified in the current branch since the split point should be changed to their versions in the given branch (checked out from the commit at the front of the given branch). These files should then all be automatically staged. To clarify, if a file is “modified in the given branch since the split point” this means the version of the file as it exists in the commit at the front of the given branch has different content from the version of the file at the split point. Remember: blobs are content addressable!

### <b>rebase</b>
   - Rebases the current branch onto the specified branch. This command replays all commits from the current branch onto the tip of the given branch, effectively creating a linear history. Identify the split point (the latest common commit) between the current branch and the target branch. Replay each commit from the current branch onto the target branch, creating new commits with the same changes but updated parent references. Move the current branch pointer to the last replayed commit. If conflicts occur during the rebase process, Gitlet will pause and allow you to resolve them. After resolving conflicts, you can continue the rebase. If the target branch is not in the history of the current branch, Gitlet will abort the operation and notify the user.

### <b>delete-repo</b>
  - Deletes the entire Gitlet repository, including all commits, branches, and staged files. This command is irreversible and removes all version control history associated with the project. Removes the `.gitlet` directory and all its contents from the current working directory. Clears any staged files, commit history, and branch information. If no Gitlet repository exists in the current directory, Gitlet will notify the user that there is nothing to delete. This command does not affect the working directory files (only the version control data).

### <b>add-remote</b>
   - Saves the given login information under the given remote name. Attempts to push or pull from the given remote name. This is useful for collaborating with remote repositories.

### <b>rm-remote</b>
   - Remove information associated with the given remote name. The idea here is that if you ever wanted to change a remote that you added, you would have to first remove it and then re-add it.

### <b>push</b>
   - Attempts to append the current branch’s commits to the end of the given branch at the given remote. This command only works if the remote branch’s head is in the history of the current local head, which means that the local branch contains some commits in the future of the remote branch. In this case, append the future commits to the remote branch. Then, the remote should reset to the front of the appended commits (so its head will be the same as the local head). This is called fast-forwarding. If the Gitlet system on the remote machine exists but does not have the input branch, then simply add the branch to the remote Gitlet.

### <b>fetch</b>
   - Brings down commits from the remote Gitlet repository into the local Gitlet repository. Basically, this copies all commits and blobs from the given branch in the remote repository (that are not already in the current repository) into a branch named `[remote name]/[remote branch name]` in the local `.gitlet` (just as in real Git), changing `[remote name]/[remote branch name]` to point to the head commit (thus copying the contents of the branch from the remote repository to the current one). This branch is created in the local repository if it did not previously exist.

### <b>pull</b>
   - Fetches branch `[remote name]/[remote branch name]` as for the `fetch` command, and then merges that fetch into the current branch. This is useful for updating your local branch with changes from the remote repository.
