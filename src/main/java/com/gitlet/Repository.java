package com.gitlet;

import com.gitlet.model.Branch;
import com.gitlet.model.Commit;
import com.gitlet.storage.BlobStore;
import com.gitlet.storage.BranchStore;
import com.gitlet.storage.CommitStore;
import com.gitlet.storage.HeadRef;
import com.gitlet.storage.RemoteStore;
import com.gitlet.storage.StagingArea;
import com.gitlet.storage.WorkingArea;
import com.gitlet.util.CommitGraph;
import com.gitlet.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class Repository {

    private static final String DEFAULT_BRANCH = "master";
    private static final Date EPOCH = new Date(0);
    private static final String INITIAL_MESSAGE = "initial commit";

    private final File cwd;
    private final File gitletDir;
    private final File branchesDir;
    private final File blobsDir;
    private final File commitsDir;
    private final File stagedDir;
    private final File additionDir;
    private final File removalDir;
    private final File remoteDir;
    private final File headFile;

    private final HeadRef head;
    private final CommitStore commitStore;
    private final BranchStore branchStore;
    private final BlobStore blobStore;
    private final WorkingArea workingArea;
    private final StagingArea stagingArea;
    private final RemoteStore remoteStore;

    public Repository(String workingDirectory) {
        cwd = new File(workingDirectory);
        if (!cwd.exists()) {
            throw new IllegalStateException("Working directory not found: " + workingDirectory);
        }

        gitletDir = FileUtils.join(cwd, ".gitlet");
        branchesDir = FileUtils.join(gitletDir, "branches");
        blobsDir = FileUtils.join(gitletDir, "blobs");
        commitsDir = FileUtils.join(gitletDir, "commits");
        stagedDir = FileUtils.join(gitletDir, "staged");
        additionDir = FileUtils.join(stagedDir, "addition");
        removalDir = FileUtils.join(stagedDir, "removal");
        remoteDir = FileUtils.join(gitletDir, "remotes");
        headFile = FileUtils.join(gitletDir, "HEAD");

        commitStore = new CommitStore(commitsDir);
        branchStore = new BranchStore(branchesDir);
        blobStore = new BlobStore(blobsDir);
        workingArea = new WorkingArea(cwd);
        stagingArea = new StagingArea(additionDir, removalDir);
        remoteStore = new RemoteStore(remoteDir);
        head = new HeadRef(headFile);
    }

    public void init() {
        if (gitletDir.exists()) {
            FileUtils.exitWithMessage("A Gitlet version-control system already exists in the current directory.");
        }
        createDirectories(gitletDir, blobsDir, commitsDir, stagedDir,
                additionDir, removalDir, branchesDir, remoteDir);
        try {
            if (!headFile.createNewFile()) {
                FileUtils.exitWithMessage("Failed to initialize repository: HEAD already exists.");
            }
        } catch (IOException ex) {
            FileUtils.exitWithMessage("Failed to initialize repository: " + ex.getMessage());
        }

        Commit initial = new Commit(EPOCH, INITIAL_MESSAGE);
        commitStore.saveCommit(initial);
        Branch master = new Branch(DEFAULT_BRANCH, initial.getCommitHash());
        branchStore.saveBranch(master);
        head.set(master.getName());
    }

    private static void createDirectories(File... dirs) {
        for (File dir : dirs) {
            try {
                Files.createDirectories(dir.toPath());
            } catch (IOException ex) {
                FileUtils.exitWithMessage("Failed to create directory " + dir + ": " + ex.getMessage());
            }
        }
    }

    public void add(String fileName) {
        requireRepository();
        File workingFile = workingArea.getPlainFile(fileName);
        if (workingFile == null) {
            FileUtils.exitWithMessage("File does not exist.");
            return;
        }
        byte[] bytes = FileUtils.readContents(workingFile);
        String workingHash = FileUtils.sha1(bytes);
        String committedHash = currentCommit().trackedFiles().get(fileName);
        if (workingHash.equals(committedHash)) {
            stagingArea.unstageAddition(fileName);
        } else {
            stagingArea.stageForAddition(fileName, workingHash);
            blobStore.saveBlob(bytes);
        }
        stagingArea.unstageRemoval(fileName);
    }

    public void rm(String fileName) {
        requireRepository();
        boolean stagedAdd = stagingArea.isStagedForAddition(fileName);
        String trackedHash = currentCommit().trackedFiles().get(fileName);
        if (trackedHash == null) {
            if (stagedAdd) {
                stagingArea.unstageAddition(fileName);
            } else {
                FileUtils.exitWithMessage("No reason to remove the file.");
            }
            return;
        }
        if (stagedAdd) {
            stagingArea.unstageAddition(fileName);
        }
        stagingArea.stageForRemoval(fileName, trackedHash);
        workingArea.deleteFile(fileName);
    }

    public void commit(String message) {
        requireRepository();
        commitInternal(message, null, false);
    }

    private void commitInternal(String message, String secondParentHash, boolean allowEmpty) {
        if (message == null || message.isEmpty()) {
            FileUtils.exitWithMessage("Please enter a commit message.");
        }
        if (!allowEmpty && stagingArea.isEmpty()) {
            FileUtils.exitWithMessage("No changes added to the commit.");
        }
        Commit current = currentCommit();
        Map<String, String> trackedFiles = new TreeMap<>(current.trackedFiles());
        for (File f : stagingArea.filesStagedForAddition()) {
            String hash = FileUtils.readContentsAsString(f);
            trackedFiles.put(f.getName(), hash);
        }
        for (File f : stagingArea.filesStagedForRemoval()) {
            trackedFiles.remove(f.getName());
        }
        Commit newCommit = new Commit(new Date(), message, secondParentHash,
                current.getCommitHash(), trackedFiles);
        commitStore.saveCommit(newCommit);
        Branch currentBranch = currentBranch();
        currentBranch.setCommit(newCommit.getCommitHash());
        branchStore.saveBranch(currentBranch);
        stagingArea.clear();
    }

    public void log() {
        requireRepository();
        for (Commit commit : CommitGraph.firstParentChain(currentCommit(), commitStore)) {
            System.out.println("===");
            System.out.println(commit);
        }
    }

    public void globalLog() {
        requireRepository();
        for (Commit commit : commitStore.getAllCommits()) {
            System.out.println("===");
            System.out.println(commit);
        }
    }

    public void find(String message) {
        requireRepository();
        if (message == null || message.isEmpty()) {
            FileUtils.exitWithMessage("Please enter a commit message.");
        }
        boolean found = false;
        for (Commit commit : commitStore.getAllCommits()) {
            if (commit.getMessage().equals(message)) {
                System.out.println(commit.getCommitHash());
                found = true;
            }
        }
        if (!found) {
            FileUtils.exitWithMessage("Found no commit with that message.");
        }
    }

    public void status() {
        requireRepository();
        printBranches();
        printStagedAdditions();
        printStagedRemovals();
        printModifiedNotStaged();
        printUntracked();
    }

    private void printBranches() {
        System.out.println("=== Branches ===");
        String currentName = head.get();
        for (String branchName : branchStore.getAllBranchNames()) {
            if (currentName.equals(branchName)) {
                System.out.println("*" + branchName);
            } else {
                System.out.println(branchName);
            }
        }
        System.out.println();
    }

    private void printStagedAdditions() {
        System.out.println("=== Staged Files ===");
        for (String name : stagingArea.namesStagedForAddition()) {
            System.out.println(name);
        }
        System.out.println();
    }

    private void printStagedRemovals() {
        System.out.println("=== Removed Files ===");
        for (String name : stagingArea.namesStagedForRemoval()) {
            System.out.println(name);
        }
        System.out.println();
    }

    private void printModifiedNotStaged() {
        System.out.println("=== Modifications Not Staged For Commit ===");
        Map<String, String> tracked = currentCommit().trackedFiles();
        Set<String> workingNames = new HashSet<>(workingArea.plainFileNames());
        Set<String> stagedAdds = new HashSet<>(stagingArea.namesStagedForAddition());
        Set<String> stagedRems = new HashSet<>(stagingArea.namesStagedForRemoval());

        TreeSet<String> candidates = new TreeSet<>();
        candidates.addAll(tracked.keySet());
        candidates.addAll(stagedAdds);
        candidates.addAll(workingNames);

        for (String name : candidates) {
            boolean inWorking = workingNames.contains(name);
            boolean inTracked = tracked.containsKey(name);
            boolean stagedAdd = stagedAdds.contains(name);
            boolean stagedRem = stagedRems.contains(name);

            String workingHash = inWorking ? hashWorkingFile(name) : null;

            if (inTracked && inWorking && !stagedAdd
                    && !workingHash.equals(tracked.get(name))) {
                System.out.println(name + " (modified)");
            } else if (stagedAdd && inWorking
                    && !workingHash.equals(stagingArea.stagedAdditionHash(name))) {
                System.out.println(name + " (modified)");
            } else if (stagedAdd && !inWorking) {
                System.out.println(name + " (deleted)");
            } else if (inTracked && !stagedRem && !inWorking) {
                System.out.println(name + " (deleted)");
            }
        }
        System.out.println();
    }

    private void printUntracked() {
        System.out.println("=== Untracked Files ===");
        Set<String> tracked = currentCommit().trackedFiles().keySet();
        Set<String> stagedAdds = new HashSet<>(stagingArea.namesStagedForAddition());
        for (String name : workingArea.plainFileNames()) {
            if (!tracked.contains(name) && !stagedAdds.contains(name)) {
                System.out.println(name);
            }
        }
        System.out.println();
    }

    public void checkoutFile(String fileName) {
        requireRepository();
        String hash = currentCommit().trackedFiles().get(fileName);
        if (hash == null) {
            FileUtils.exitWithMessage("File does not exist in that commit.");
        }
        workingArea.saveBytes(blobStore.getBlobBytes(hash), fileName);
    }

    public void checkoutFileByCommit(String commitId, String fileName) {
        requireRepository();
        Commit target = commitStore.findByPrefix(commitId);
        if (target == null) {
            FileUtils.exitWithMessage("No commit with that id exists.");
            return;
        }
        String hash = target.trackedFiles().get(fileName);
        if (hash == null) {
            FileUtils.exitWithMessage("File does not exist in that commit.");
        }
        workingArea.saveBytes(blobStore.getBlobBytes(hash), fileName);
    }

    public void checkoutBranch(String branchName) {
        requireRepository();
        Branch target = branchStore.getBranch(branchName);
        if (target == null) {
            FileUtils.exitWithMessage("No such branch exists.");
            return;
        }
        if (head.get().equals(branchName)) {
            FileUtils.exitWithMessage("No need to checkout the current branch.");
        }
        Commit currentCommit = currentCommit();
        Commit targetCommit = commitStore.getCommit(target.getReferredCommitHash());
        if (hasUntrackedConflicts(currentCommit.trackedFiles(), targetCommit.trackedFiles())) {
            FileUtils.exitWithMessage("There is an untracked file in the way; delete it, or add and commit it first.");
        }
        applyCommitToWorkingDir(currentCommit.trackedFiles(), targetCommit.trackedFiles());
        head.set(branchName);
        stagingArea.clear();
    }

    private boolean hasUntrackedConflicts(Map<String, String> currentTracked,
                                          Map<String, String> targetTracked) {
        for (String name : workingArea.plainFileNames()) {
            if (currentTracked.containsKey(name)) {
                continue;
            }
            if (!targetTracked.containsKey(name)) {
                continue;
            }
            File f = workingArea.getPlainFile(name);
            if (f == null) {
                continue;
            }
            String workingHash = FileUtils.sha1(FileUtils.readContents(f));
            if (!workingHash.equals(targetTracked.get(name))) {
                return true;
            }
        }
        return false;
    }

    private String hashWorkingFile(String name) {
        File f = workingArea.getPlainFile(name);
        if (f == null) {
            return null;
        }
        return FileUtils.sha1(FileUtils.readContents(f));
    }

    private void applyCommitToWorkingDir(Map<String, String> from, Map<String, String> to) {
        for (Map.Entry<String, String> entry : from.entrySet()) {
            if (!to.containsKey(entry.getKey())) {
                workingArea.deleteFile(entry.getKey());
            }
        }
        for (Map.Entry<String, String> entry : to.entrySet()) {
            workingArea.saveBytes(blobStore.getBlobBytes(entry.getValue()), entry.getKey());
        }
    }

    public void branch(String branchName) {
        requireRepository();
        if (branchStore.exists(branchName)) {
            FileUtils.exitWithMessage("A branch with that name already exists.");
        }
        String headHash = currentCommit().getCommitHash();
        branchStore.saveBranch(new Branch(branchName, headHash));
    }

    public void rmBranch(String branchName) {
        requireRepository();
        if (branchName.equals(head.get())) {
            FileUtils.exitWithMessage("Cannot remove the current branch.");
        }
        if (!branchStore.exists(branchName)) {
            FileUtils.exitWithMessage("A branch with that name does not exist.");
        }
        branchStore.deleteBranch(branchName);
    }

    public void reset(String commitId) {
        requireRepository();
        Commit target = commitStore.findByPrefix(commitId);
        if (target == null) {
            FileUtils.exitWithMessage("No commit with that id exists.");
            return;
        }
        Commit current = currentCommit();
        if (hasUntrackedConflicts(current.trackedFiles(), target.trackedFiles())) {
            FileUtils.exitWithMessage("There is an untracked file in the way; delete it, or add and commit it first.");
        }
        applyCommitToWorkingDir(current.trackedFiles(), target.trackedFiles());
        Branch branch = currentBranch();
        branch.setCommit(target.getCommitHash());
        branchStore.saveBranch(branch);
        stagingArea.clear();
    }

    public void merge(String branchName, String remoteName) {
        requireRepository();
        Branch targetBranch = (remoteName != null)
                ? remoteStore.getRemoteBranchFromLocal(remoteName, branchName)
                : branchStore.getBranch(branchName);
        if (targetBranch == null) {
            FileUtils.exitWithMessage("A branch with that name does not exist.");
        }
        Branch currentBranch = currentBranch();
        if (remoteName == null && targetBranch.getName().equals(currentBranch.getName())) {
            FileUtils.exitWithMessage("Cannot merge a branch with itself.");
        }
        if (!stagingArea.isEmpty()) {
            FileUtils.exitWithMessage("You have uncommitted changes.");
        }

        Commit headCommit = commitStore.getCommit(currentBranch.getReferredCommitHash());
        Commit otherCommit = (remoteName != null)
                ? remoteStore.getRemoteCommit(targetBranch.getReferredCommitHash(), remoteName)
                : commitStore.getCommit(targetBranch.getReferredCommitHash());
        Commit splitCommit = CommitGraph.splitPoint(currentBranch, targetBranch, remoteName,
                commitStore, remoteStore);

        if (splitCommit == null) {
            FileUtils.exitWithMessage("No common ancestor found.");
        }
        if (splitCommit.equals(otherCommit)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }
        if (hasUntrackedConflicts(headCommit.trackedFiles(), otherCommit.trackedFiles())) {
            FileUtils.exitWithMessage("There is an untracked file in the way; delete it, or add and commit it first.");
        }
        if (splitCommit.equals(headCommit)) {
            applyCommitToWorkingDir(headCommit.trackedFiles(), otherCommit.trackedFiles());
            currentBranch.setCommit(otherCommit.getCommitHash());
            branchStore.saveBranch(currentBranch);
            stagingArea.clear();
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        boolean conflict = applyMerge(splitCommit, headCommit, otherCommit);
        String message = String.format("Merged %s into %s.",
                targetBranch.getName(), currentBranch.getName());
        commitInternal(message, otherCommit.getCommitHash(), true);
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    private boolean applyMerge(Commit splitCommit, Commit headCommit, Commit otherCommit) {
        Map<String, String> split = splitCommit.trackedFiles();
        Map<String, String> head = headCommit.trackedFiles();
        Map<String, String> other = otherCommit.trackedFiles();

        Set<String> allFiles = new TreeSet<>();
        allFiles.addAll(split.keySet());
        allFiles.addAll(head.keySet());
        allFiles.addAll(other.keySet());

        boolean conflict = false;
        for (String fileName : allFiles) {
            String s = split.get(fileName);
            String h = head.get(fileName);
            String o = other.get(fileName);

            boolean splitEqHead = Objects.equals(s, h);
            boolean splitEqOther = Objects.equals(s, o);
            boolean headEqOther = Objects.equals(h, o);

            if (headEqOther) {
                continue;
            }
            if (splitEqHead && !splitEqOther) {
                if (o == null) {
                    workingArea.deleteFile(fileName);
                    stagingArea.stageForRemoval(fileName, s);
                } else {
                    workingArea.saveBytes(blobStore.getBlobBytes(o), fileName);
                    stagingArea.stageForAddition(fileName, o);
                }
                continue;
            }
            if (splitEqOther && !splitEqHead) {
                continue;
            }
            String mergedContent = buildConflictContent(h, o);
            String mergedHash = blobStore.saveBlobContent(mergedContent);
            workingArea.saveFile(mergedContent, fileName);
            stagingArea.stageForAddition(fileName, mergedHash);
            conflict = true;
        }
        return conflict;
    }

    private String buildConflictContent(String headHash, String otherHash) {
        String headContent = (headHash == null) ? "" : blobStore.getBlobContent(headHash);
        String otherContent = (otherHash == null) ? "" : blobStore.getBlobContent(otherHash);
        return "<<<<<<< HEAD\n" + headContent
                + "=======\n" + otherContent
                + ">>>>>>>\n";
    }

    public void rebase(String branchName) {
        requireRepository();
        Branch currentBranch = currentBranch();
        Branch targetBranch = branchStore.getBranch(branchName);
        if (targetBranch == null) {
            FileUtils.exitWithMessage("A branch with that name does not exist.");
            return;
        }
        if (currentBranch.getName().equals(branchName)) {
            FileUtils.exitWithMessage("Cannot rebase a branch onto itself.");
        }
        if (!stagingArea.isEmpty()) {
            FileUtils.exitWithMessage("You have uncommitted changes.");
        }

        Commit currentCommit = currentCommit();
        Commit targetCommit = commitStore.getCommit(targetBranch.getReferredCommitHash());
        Commit splitCommit = CommitGraph.splitPoint(currentBranch, targetBranch, null,
                commitStore, remoteStore);
        if (splitCommit == null) {
            FileUtils.exitWithMessage("No common ancestor found.");
        }
        if (splitCommit.equals(targetCommit)) {
            System.out.println("Already up-to-date.");
            return;
        }
        if (hasUntrackedConflicts(currentCommit.trackedFiles(), targetCommit.trackedFiles())) {
            FileUtils.exitWithMessage("There is an untracked file in the way; delete it, or add and commit it first.");
        }
        if (splitCommit.equals(currentCommit)) {
            applyCommitToWorkingDir(currentCommit.trackedFiles(), targetCommit.trackedFiles());
            currentBranch.setCommit(targetCommit.getCommitHash());
            branchStore.saveBranch(currentBranch);
            stagingArea.clear();
            return;
        }

        List<Commit> toReplay = new ArrayList<>();
        Commit cursor = currentCommit;
        while (!cursor.equals(splitCommit)) {
            toReplay.add(0, cursor);
            cursor = commitStore.getCommit(cursor.getParentCommitHash());
            if (cursor == null) {
                FileUtils.exitWithMessage("Broken commit history during rebase.");
            }
        }

        Commit newParent = targetCommit;
        for (Commit replay : toReplay) {
            newParent = replayOnto(replay, newParent, splitCommit);
        }
        applyCommitToWorkingDir(currentCommit.trackedFiles(), newParent.trackedFiles());
        currentBranch.setCommit(newParent.getCommitHash());
        branchStore.saveBranch(currentBranch);
        stagingArea.clear();
    }

    private Commit replayOnto(Commit replay, Commit base, Commit split) {
        Map<String, String> baseFiles = base.trackedFiles();
        Map<String, String> replayFiles = replay.trackedFiles();
        Map<String, String> splitFiles = split.trackedFiles();

        Set<String> allFiles = new TreeSet<>();
        allFiles.addAll(baseFiles.keySet());
        allFiles.addAll(replayFiles.keySet());
        allFiles.addAll(splitFiles.keySet());

        Map<String, String> result = new TreeMap<>(baseFiles);
        for (String fileName : allFiles) {
            String s = splitFiles.get(fileName);
            String b = baseFiles.get(fileName);
            String r = replayFiles.get(fileName);

            if (Objects.equals(s, r)) {
                continue;
            }
            if (Objects.equals(s, b)) {
                if (r == null) {
                    result.remove(fileName);
                } else {
                    result.put(fileName, r);
                }
                continue;
            }
            if (Objects.equals(b, r)) {
                continue;
            }
            String merged = buildConflictContent(b, r);
            String mergedHash = blobStore.saveBlobContent(merged);
            result.put(fileName, mergedHash);
        }
        Commit replayed = new Commit(new Date(), replay.getMessage() + " (rebase)",
                null, base.getCommitHash(), result);
        commitStore.saveCommit(replayed);
        return replayed;
    }

    public void addRemote(String remoteName, String remotePath) {
        requireRepository();
        validateRemotePath(remotePath);
        remoteStore.addRemotePath(remoteName, remotePath);
    }

    public void removeRemote(String remoteName) {
        requireRepository();
        remoteStore.removeRemotePath(remoteName);
    }

    public void push(String remoteName, String remoteBranchName) {
        requireRepository();
        String remotePath = remoteStore.getRemotePath(remoteName);
        if (remotePath == null) {
            FileUtils.exitWithMessage("Remote directory not found.");
        }
        validateRemotePath(remotePath);

        Branch remoteBranch = remoteStore.getRemoteBranch(remoteName, remoteBranchName);
        Branch currentBranch = currentBranch();
        Commit headCommit = commitStore.getCommit(currentBranch.getReferredCommitHash());

        List<Commit> localHistory = CommitGraph.firstParentChain(headCommit, commitStore);
        List<Commit> toCopy;

        if (remoteBranch == null) {
            toCopy = new ArrayList<>(localHistory);
        } else {
            Commit remoteHead = remoteStore.getRemoteCommit(
                    remoteBranch.getReferredCommitHash(), remoteName);
            if (remoteHead == null) {
                FileUtils.exitWithMessage("Remote head commit not found.");
                return;
            }
            String remoteHeadHash = remoteHead.getCommitHash();
            boolean fastForward = false;
            toCopy = new ArrayList<>();
            for (Commit c : localHistory) {
                if (c.getCommitHash().equals(remoteHeadHash)) {
                    fastForward = true;
                    break;
                }
                toCopy.add(c);
            }
            if (!fastForward) {
                FileUtils.exitWithMessage("Please pull down remote changes before pushing.");
            }
            if (toCopy.isEmpty()) {
                System.out.println("Already up-to-date.");
                return;
            }
        }

        Path localCommits = Paths.get(gitletDir.getAbsolutePath(), "commits");
        Path localBlobs = Paths.get(gitletDir.getAbsolutePath(), "blobs");
        Path remoteCommits = Paths.get(remotePath, "commits");
        Path remoteBlobs = Paths.get(remotePath, "blobs");

        copyCommitsAndBlobs(localCommits, localBlobs, remoteCommits, remoteBlobs, toCopy);

        Branch updatedRemoteBranch = (remoteBranch != null)
                ? remoteBranch
                : new Branch(remoteBranchName, headCommit.getCommitHash());
        updatedRemoteBranch.setCommit(headCommit.getCommitHash());
        remoteStore.saveRemoteBranch(remoteName, updatedRemoteBranch);
    }

    public void fetch(String remoteName, String remoteBranchName) {
        requireRepository();
        String remotePath = remoteStore.getRemotePath(remoteName);
        if (remotePath == null) {
            FileUtils.exitWithMessage("Remote directory not found.");
        }
        validateRemotePath(remotePath);

        Branch remoteBranch = remoteStore.getRemoteBranch(remoteName, remoteBranchName);
        if (remoteBranch == null) {
            FileUtils.exitWithMessage("That remote does not have that branch.");
            return;
        }
        Commit remoteHead = remoteStore.getRemoteCommit(remoteBranch.getReferredCommitHash(), remoteName);
        if (remoteHead == null) {
            FileUtils.exitWithMessage("Remote head commit not found.");
            return;
        }

        List<Commit> remoteHistory = CommitGraph.firstParentChainRemote(remoteHead, remoteName, remoteStore);
        Set<String> localHashes = commitStore.getCommitHashes();
        List<Commit> missing = new ArrayList<>();
        for (Commit c : remoteHistory) {
            if (!localHashes.contains(c.getCommitHash())) {
                missing.add(c);
            }
        }

        Path localCommits = Paths.get(gitletDir.getAbsolutePath(), "commits");
        Path localBlobs = Paths.get(gitletDir.getAbsolutePath(), "blobs");
        Path remoteCommits = Paths.get(remotePath, "commits");
        Path remoteBlobs = Paths.get(remotePath, "blobs");

        copyCommitsAndBlobs(remoteCommits, remoteBlobs, localCommits, localBlobs, missing);
        remoteStore.saveRemoteBranchAtLocal(remoteName, remoteBranch);
    }

    public void pull(String remoteName, String remoteBranchName) {
        fetch(remoteName, remoteBranchName);
        merge(remoteBranchName, remoteName);
    }

    public void deleteRepo() {
        requireRepository();
        deleteRecursively(gitletDir);
        if (gitletDir.exists()) {
            FileUtils.exitWithMessage("Failed to fully delete the repository.");
        }
        System.out.println("Gitlet repository deleted.");
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    private void copyCommitsAndBlobs(Path commitSrc, Path blobSrc,
                                     Path commitDst, Path blobDst,
                                     List<Commit> commits) {
        try {
            Files.createDirectories(commitDst);
            Files.createDirectories(blobDst);
            for (Commit commit : commits) {
                Path src = commitSrc.resolve(commit.getCommitHash());
                Path dst = commitDst.resolve(commit.getCommitHash());
                if (Files.exists(src)) {
                    Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                }
                for (Map.Entry<String, String> entry : commit.trackedFiles().entrySet()) {
                    Path bSrc = blobSrc.resolve(entry.getValue());
                    Path bDst = blobDst.resolve(entry.getValue());
                    if (Files.exists(bSrc)) {
                        Files.copy(bSrc, bDst, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (IOException ex) {
            throw FileUtils.error("Failed to synchronize remote data: %s", ex.getMessage());
        }
    }

    private void validateRemotePath(String remotePath) {
        File dir = new File(remotePath);
        if (!remotePath.endsWith(".gitlet") || !dir.isDirectory()) {
            FileUtils.exitWithMessage("Remote directory not found.");
        }
    }

    private void requireRepository() {
        if (!gitletDir.exists()) {
            FileUtils.exitWithMessage("Not in an initialized Gitlet directory.");
        }
    }

    private Branch currentBranch() {
        return branchStore.getBranch(head.get());
    }

    private Commit currentCommit() {
        Branch branch = currentBranch();
        if (branch == null) {
            FileUtils.exitWithMessage("HEAD does not point to a valid branch.");
        }
        return commitStore.getCommit(branch.getReferredCommitHash());
    }
}
