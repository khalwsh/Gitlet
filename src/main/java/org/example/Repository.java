package org.example;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
public class Repository {
    private final File CWD;
    private final File Gitlet_Dir;

    private final File Branches_Dir;
    private final File Blobs_Dir;
    private final File Commits_Dir;
    private final File Staged_Dir;
    private final File Addition_Dir;
    private final File Removal_Dir;
    private final File Remote_Dir;
    private final File Head_file;


    private final Head head;
    private final CommitStore commitStore;
    private final BranchStore branchStore;
    private final BlobStore blobStore;
    private final WorkingArea workingArea;
    private final StagingArea stagingArea;
    private final RemoteStore remoteStore;

    public Repository(String cwd) {
        CWD = new File(cwd);

        if (!CWD.exists()) throw new RuntimeException("Working directory not found.");

        Gitlet_Dir = Utils.join(CWD, ".gitlet");
        Branches_Dir = Utils.join(Gitlet_Dir, "branches");
        Blobs_Dir = Utils.join(Gitlet_Dir, "blobs");
        Commits_Dir = Utils.join(Gitlet_Dir, "commits");
        Staged_Dir = Utils.join(Gitlet_Dir, "staged");
        Addition_Dir = Utils.join(Staged_Dir, "addition");
        Removal_Dir = Utils.join(Staged_Dir, "removal");
        Remote_Dir = Utils.join(Gitlet_Dir, "remotes");
        Head_file = Utils.join(Gitlet_Dir, "head");

        commitStore = new CommitStore(Commits_Dir);
        branchStore = new BranchStore(Branches_Dir);
        blobStore = new BlobStore(Blobs_Dir);
        workingArea = new WorkingArea(CWD);
        stagingArea = new StagingArea(Addition_Dir, Removal_Dir);
        remoteStore = new RemoteStore(Remote_Dir);
        head = new Head(Head_file);
    }

    public void init() {
        if (Gitlet_Dir.exists()) Utils.exitWithMessage("Gitlet Repository already exists in current working directory");

        Gitlet_Dir.mkdir();
        Blobs_Dir.mkdir();
        Commits_Dir.mkdir();
        Staged_Dir.mkdir();
        Addition_Dir.mkdir();
        Removal_Dir.mkdir();
        Branches_Dir.mkdir();
        Remote_Dir.mkdir();
        try {
            Head_file.createNewFile();
        } catch (java.io.IOException ex) {
            Utils.exitWithMessage(ex.getMessage());
        }


        Commit commit = new Commit(new Date(0), "Initial Commit");   //Initialize the directory structure inside .gitlet

        // start with initial commit with message "initial commit" and timestamp=Unix epoch
        commitStore.saveCommit(commit);
        Branch master = new Branch("master", commit.getCommitHash());// create master branch
        branchStore.saveBranch(master);
        head.setHead(master.getName()); //set head to point to master
    }

    public void add(String fileName) {
        //check gitlet repo existence
        checkGitletExistense();
        //check file existense
        File currentFile = workingArea.checkFileExistense(fileName);
        if (currentFile == null) Utils.exitWithMessage("File doesn't exist");


        String committedFileHash = getCurrentCommit().trackedFiles().get(fileName);
        String workingFileHash = Utils.sha1(Utils.readContentsAsString(currentFile));
        if (!workingFileHash.equals(committedFileHash)) {
            stagingArea.stageForAddition(fileName, workingFileHash);
            blobStore.saveBlob(currentFile);
        } else {
            stagingArea.UnStageForAddittion(fileName);
        }

        stagingArea.unstageForRemoval(fileName);
    }

    public void rm(String fileName) {
        //check gitlet repo existence
        checkGitletExistense();

        boolean checkFileStagedForAddition = stagingArea.CheckFileStagedForAddition(fileName);


        //check if file tracked or not
        String fileHash = getCurrentCommit().trackedFiles().get(fileName);
        if (fileHash == null) //untracked
        {
            if (checkFileStagedForAddition) stagingArea.UnStageForAddittion(fileName);
            else Utils.exitWithMessage("No reason to remove the file.");
            //untracked and not staged for addition
        } else {
            if (checkFileStagedForAddition) stagingArea.UnStageForAddittion(fileName);
            ///add last commited file version to staged for removal
            stagingArea.StageForRemoval(fileName, fileHash);
            ///remove current file version form CWD
            workingArea.removeFromCWD(fileName);
        }

    }

    public void FastForwardMerge(String branchName, String remoteName) {

        Branch targetBranch;
        if (remoteName != null) targetBranch = remoteStore.getRemoteBranchFromLocal(remoteName, branchName);
        else targetBranch = branchStore.getBranch(branchName);

        if (targetBranch == null) {

            Utils.exitWithMessage("No such branch exists.");
        } else {
            String activeBranch = head.getHead();
            if (remoteName == null && activeBranch.equals(branchName)) {
                Utils.exitWithMessage("No need to checkout the current branch.");
            } else {
                //get list of all tracked files in both active and target branches
                Map<String, String> trackedInActive = getCurrentCommit().trackedFiles();

                String commitHashInTarget = targetBranch.getReferredCommitHash();
                Commit targetCommit = commitStore.getCommit(commitHashInTarget);

                Map<String, String> trackedInTarget = targetCommit.trackedFiles();
                //check for tracked in target
                for (Map.Entry<String, String> entry : trackedInTarget.entrySet()) {
                    String hashOfActive = trackedInActive.get(entry.getKey());
                    //tracked in target and not tracked in active
                    if (hashOfActive == null) {
                        //two cases
                        File existInCWD = workingArea.checkFileExistense(entry.getKey());
                        if (existInCWD == null) {// create new one at CWD with content from target
                            String blobContent = blobStore.getBlobContent(entry.getValue());
                            workingArea.addOrUpdateFileAtCWD(entry.getKey(), blobContent);
                        }
                        //can't take action since it is not tracked or removed
                        else
                            Utils.exitWithMessage("There is an untracked file in the way; delete it, or add and commit it first.");
                    }
                }
                ///must update fast forwarded branch pointer to point to same as the merged one head pointer still as it is
                String targetHash = targetBranch.getReferredCommitHash();
                Branch currentBranch = getCurrentBranch();
                currentBranch.SetCommit(targetHash);
                branchStore.saveBranch(currentBranch);
            }
        }
    }

    public void merge(String branchName, String remoteName) {
        Branch targetBranch;
        /////////////////////critical
        if (remoteName != null) {
            targetBranch = remoteStore.getRemoteBranchFromLocal(remoteName, branchName);
        } else targetBranch = branchStore.getBranch(branchName);

        if (targetBranch == null) {
            Utils.exitWithMessage("A branch with that name does not exist.");
        }

        Branch currentBranch = getCurrentBranch();
        if (targetBranch.getName().equals(currentBranch.getName())) {
            if (remoteName == null)
                Utils.exitWithMessage("Cannot merge a branch with itself.");
        }


        final Commit HEAD_COMMIT = commitStore.getCommit(currentBranch.getReferredCommitHash());
        final Commit OTHER_COMMIT = commitStore.getCommit(targetBranch.getReferredCommitHash());
        final Commit SPLIT_COMMIT = splitPoint(currentBranch, targetBranch, null);

        if (SPLIT_COMMIT == null) {
            Utils.exitWithMessage("There is No common LCA");
        }
        if (SPLIT_COMMIT.equals(OTHER_COMMIT)) {
            Utils.exitWithMessage("Given branch is an ancestor of the current branch.");
        }

        if (SPLIT_COMMIT.equals(HEAD_COMMIT)) {
            /////////////////////critical

            FastForwardMerge(targetBranch.getName(), remoteName);

            Utils.exitWithMessage("Current branch fast-forwarded.");
        }

        Set<String> ALL = new HashSet<>();
        ALL.addAll(SPLIT_COMMIT.trackedFiles().keySet());
        ALL.addAll(HEAD_COMMIT.trackedFiles().keySet());
        ALL.addAll(OTHER_COMMIT.trackedFiles().keySet());

        AtomicBoolean isConflict = new AtomicBoolean(false);

        ALL.forEach(fileName -> {
            final String SPLIT = SPLIT_COMMIT.trackedFiles().get(fileName);
            final String HEAD = HEAD_COMMIT.trackedFiles().get(fileName);
            final String OTHER = OTHER_COMMIT.trackedFiles().get(fileName);

            if (SPLIT != null && OTHER != null && !SPLIT.equals(OTHER) && SPLIT.equals(HEAD)) {
                // file in split commit and in the Other branch and the file changed but didn't change in the current branch
                String contents = blobStore.getBlobContent(OTHER);
                workingArea.saveFile(contents, fileName);

                stagingArea.stageForAddition(blobStore.getBlobContent(OTHER), fileName);
            }

            if (SPLIT != null && HEAD != null && !SPLIT.equals(HEAD) && SPLIT.equals(OTHER)) {
                // file in split commit and in the current branch and the file changed but didn't change in the other branch
                workingArea.saveFile(blobStore.getBlobContent(HEAD), fileName);
            }

            if (!Objects.equals(SPLIT, HEAD) && !Objects.equals(SPLIT, OTHER) && !Objects.equals(HEAD, OTHER)) {
                /* conflict */
                String headContents = (HEAD != null ? blobStore.getBlobContent(HEAD) : "");
                String otherContents = (OTHER != null ? blobStore.getBlobContent(OTHER) : "");
                String contents = "<<<<<<< HEAD\n" +
                        headContents +
                        "\n=======\n" +
                        otherContents +
                        "\n>>>>>>>\n";
                workingArea.saveFile(contents, fileName);

                blobStore.saveBlob(workingArea.getFile(fileName));

                stagingArea.stageForAddition(fileName, Utils.sha1(contents));
                isConflict.set(true);
            }

            if (SPLIT == null && OTHER == null && HEAD != null) {
                // exist only in the current branch
                workingArea.saveFile(blobStore.getBlobContent(HEAD), fileName);
            }

            if (SPLIT == null && HEAD == null && OTHER != null) {
                // exist only in the other branch
                String contents = blobStore.getBlobContent(OTHER);
                workingArea.saveFile(contents, fileName);
                stagingArea.stageForAddition(blobStore.getBlobContent(OTHER), fileName);
            }

            if (SPLIT != null && SPLIT.equals(HEAD) && OTHER == null) {
                // deleted from the other branch but not changed in the current branch
                File temp = workingArea.getFile(fileName);
                if (temp != null) {
                    stagingArea.StageForRemoval(fileName, Utils.sha1(temp));
                    workingArea.deleteFile(fileName);
                }
            }

            if (SPLIT != null && SPLIT.equals(OTHER) && HEAD == null) {
                /* leave the file removed */
            }
        });

        String commitMessage = String.format("Merged %s into %s.", targetBranch.getName(), currentBranch.getName());
        commit(commitMessage, OTHER_COMMIT.getCommitHash());
        if (isConflict.get()) {
            Utils.exitWithMessage("Encountered a merge conflict.");
        }
    }

    public void log() {
        //check gitlet repo existence
        checkGitletExistense();
        //get current commit
        Commit curCommit = getCurrentCommit();
        ArrayList<Commit> listOfCommits = branchStore.getBranchHistory(curCommit, commitStore);
        for (int i = 0; i < listOfCommits.size(); i++) {
            System.out.println("===");
            System.out.println(listOfCommits.get(i));
        }

    }

    public void logAll() {
        List<Commit> commits = getCommitTree(getCurrentCommit(), null);
        System.out.println(commits.size());
        for (int i = 0; i < commits.size(); i++) {
            System.out.println("===");
            System.out.println(commits.get(i));
        }
    }

    public void globallog() {
        checkGitletExistense(); // check repo is initialized
        ArrayList<Commit> listOfCommits = commitStore.getAllCommitsHistory();
        for (int i = 0; i < listOfCommits.size(); i++) {
            System.out.println("===");
            System.out.println(listOfCommits.get(i));
        }

    }

    public void find(String Message) {
        // this function search throw all commits and return the hashes of the commits that has this message
        checkGitletExistense(); // check repo is initialized
        if (Message.isEmpty()) {
            Utils.exitWithMessage("incorrect operands");
        }
        ArrayList<Commit> listOfCommits = commitStore.getAllCommitsHistory();
        for (Commit x : listOfCommits) {
            if (x.CommitMessage().equals(Message)) {
                System.out.println(x);
            }
        }
        if (!listOfCommits.isEmpty()) return;
        System.out.println("there is no commit with this message");
    }

    public void commit(String Message) {
        checkGitletExistense(); // check repo is initialized
        commit(Message, null);
    }

    public void commit(String Message, String SecondParentHash) {

        if (Message.isEmpty()) Utils.exitWithMessage("you have to enter a commit message");
        if (stagingArea.IsEmpty()) Utils.exitWithMessage("nothing to commit");


        String CurCommitHash = getCurrentCommit().getCommitHash();

        Map<String, String> trackedFiles = commitStore.getCommit(CurCommitHash).trackedFiles();

        for (File f : stagingArea.GetFilesForAddition()) {
            String BlobStored = Utils.readContentsAsString(f);
            trackedFiles.put(f.getName(), BlobStored);
        }
        for (File f : stagingArea.GetFilesForRemoval()) {
            trackedFiles.remove(f.getName());
        }

        Commit NewCommit = new Commit(new Date(), Message, SecondParentHash, CurCommitHash, trackedFiles);
        commitStore.saveCommit(NewCommit);

        Branch CurBranch = getCurrentBranch();
        CurBranch.SetCommit(NewCommit.getCommitHash());
        branchStore.saveBranch(CurBranch);
        stagingArea.clear();
    }

    public void CheckOutFile(String fileName) {
        checkGitletExistense(); // check repo is initialized
        String fileHashInHead = getCurrentCommit().trackedFiles().get(fileName);
        if (fileHashInHead == null) Utils.exitWithMessage("File does not exist in that commit.");
        else {
            String blobContent = blobStore.getBlobContent(fileHashInHead);
            workingArea.addOrUpdateFileAtCWD(fileName, blobContent);
        }
    }

    public void CheckOutFileByHash(String commitHash, String fileName) {
        checkGitletExistense(); // check repo is initialized
        Commit targetCommit = commitStore.getCommit(commitHash);
        if (targetCommit == null) Utils.exitWithMessage("No commit with that id exists.");
        else {
            String fileHash = targetCommit.trackedFiles().get(fileName);
            if (fileHash == null) Utils.exitWithMessage("File does not exist in that commit.");
            else {
                String blobContent = blobStore.getBlobContent(fileHash);
                workingArea.addOrUpdateFileAtCWD(fileName, blobContent);
            }
        }

    }

    public void CheckOutBranch(String branchName) {
        checkGitletExistense(); // Ensure the repository is initialized

        Branch targetBranch = branchStore.getBranch(branchName);
        if (targetBranch == null) {
            Utils.exitWithMessage("No such branch exists.");
        }

        String activeBranch = head.getHead();
        if (activeBranch.equals(branchName)) {
            Utils.exitWithMessage("No need to checkout the current branch.");
        }

        // Get the tracked files for the active and target branches
        Map<String, String> trackedInActive = getCurrentCommit().trackedFiles();
        Commit targetCommit = commitStore.getCommit(targetBranch.getReferredCommitHash());
        Map<String, String> trackedInTarget = targetCommit.trackedFiles();

        // Check for untracked file conflicts
        if (hasUntrackedFileConflicts(trackedInActive)) {
            Utils.exitWithMessage("There is an untracked file in the way; delete it, or add and commit it first.");
        }

        // Update the working tree
        updateWorkingDirectory(trackedInActive, trackedInTarget);

        // Update the active branch and clear the staging area
        head.setHead(branchName);
        stagingArea.clear();

    }

    private boolean hasUntrackedFileConflicts(Map<String, String> trackedInActive) {
        for (String fileName : workingArea.NameOfFilesInWorkingArea()) {
            if (!trackedInActive.containsKey(fileName)) {
                return true;
            }
            String fileHashInCommit = trackedInActive.get(fileName);
            String fileContent = Utils.readContentsAsString(new File(fileName));
            String fileHashInWorkingTree = Utils.sha1(fileContent);
            if (!fileHashInCommit.equals(fileHashInWorkingTree)) {
                return true;
            }
        }
        return false;
    }

    private void updateWorkingDirectory(Map<String, String> trackedInActive, Map<String, String> trackedInTarget) {
        // Remove or replace files from the working tree based on branch differences
        for (Map.Entry<String, String> entry : trackedInActive.entrySet()) {
            String targetHash = trackedInTarget.get(entry.getKey());
            if (targetHash == null) {
                workingArea.removeFromCWD(entry.getKey()); // Remove files not in the target branch
            } else {
                String content = blobStore.getBlobContent(targetHash);
                workingArea.addOrUpdateFileAtCWD(entry.getKey(), content); // Update to target version
            }
        }

        // Add new files from the target branch
        for (Map.Entry<String, String> entry : trackedInTarget.entrySet()) {
            if (!trackedInActive.containsKey(entry.getKey())) {
                String content = blobStore.getBlobContent(entry.getValue());
                workingArea.addOrUpdateFileAtCWD(entry.getKey(), content);
            }
        }
    }


    public void branch(String targetBranchName) {
        checkGitletExistense();
        String activeBranch = head.getHead();
        if (activeBranch == targetBranchName) Utils.exitWithMessage("No need to checkout the current branch.");
        else {
            String lastCommitHashInActive = getCurrentCommit().getCommitHash();
            branchStore.createNewBranch(targetBranchName, lastCommitHashInActive);
        }
    }

    public void rmbranch(String branchName) {
        String activeBranchName = head.getHead();

        if (branchName.equals(activeBranchName)) Utils.exitWithMessage("Cannot remove the current branch.");
        else {

            Branch targetBranch = branchStore.getBranch(branchName);
            if (targetBranch == null) Utils.exitWithMessage("A branch with that name does not exist.");
            else {
                //branch exist and not active one then remove it
                branchStore.deleteBranch(branchName);
            }
        }
    }

    public void status() {

        // printing the branch names
        checkGitletExistense();
        System.out.println("=== Branches ===");
        String CurrentBranchName = head.getHead();
        for (String BranchName : branchStore.GetAllBranchesName()) {
            if (CurrentBranchName.equals(BranchName)) System.out.print("*");
            System.out.println(BranchName);
        }
        System.out.println();
        // now printing staged Addition files
        System.out.println("=== Staged Files ===");
        for (String filename : stagingArea.GetNameOfFilesForAddition()) {
            System.out.println(filename);
        }
        System.out.println();
        // now printing staged for removal files
        System.out.println("=== Removed Files ===");
        for (String filename : stagingArea.GetNameOfFilesForRemoval()) {
            System.out.println(filename);
        }
        System.out.println();


        // now print the files that modified in working tree but not in the staging area
        System.out.println("=== Modifications Not Staged For Commit ===");

        // modified hashing changed from the one in Staging area
        File[] WorkingTreeFiles = workingArea.WorkingTreeFiles();
        Set<String> StagingAreaSet = (stagingArea.GetNameOfFilesForAddition().length == 0 ? null : new TreeSet<>(Arrays.asList(stagingArea.GetNameOfFilesForAddition())));

        for (File file : WorkingTreeFiles) {
            if (StagingAreaSet != null && StagingAreaSet.contains(file.getName())) {
                String WorkingTreeFileHash = Utils.sha1(Utils.readContentsAsString(file));
                String StagingAreaFileHash = Utils.readContentsAsString(new File(stagingArea.GetAdditionDir(), file.getName()));
                if (!WorkingTreeFileHash.equals(StagingAreaFileHash)) {
                    System.out.println(file.getName() + " (modified) ");
                }
            }
        }
        // deleted: exist in last commit and not staged for delete and not exist in working tree
        Commit LastCommit = getCurrentCommit();
        TreeSet<String> StagingAreaRem = (stagingArea.GetNameOfFilesForRemoval().length == 0 ? null : new TreeSet<>(Arrays.asList(stagingArea.GetNameOfFilesForRemoval())));
        TreeSet<String> WorkingTreeNames = new TreeSet<>(Arrays.asList(workingArea.NameOfFilesInWorkingArea()));
        for (Map.Entry<String, String> entry : LastCommit.trackedFiles().entrySet()) {
//            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
            String FileName = entry.getKey();
            if (!WorkingTreeNames.contains(FileName) && (StagingAreaRem == null || !StagingAreaRem.contains(FileName))) {
                System.out.println(FileName + " (deleted) ");
            }
        }
        System.out.println();


        // now the untracked files
        System.out.println("=== Untracked Files ===");
        File[] files = workingArea.WorkingTreeFiles();
        if (stagingArea.IsEmpty()) {
            for (File f : files) {
                if (f.isDirectory()) continue;
                System.out.println(f.getName());
            }
            System.out.println();
            return;
        }
        TreeSet<String> fileSet = new TreeSet<>(Arrays.asList(stagingArea.GetAllFilesNames()));
        for (File f : files) {
            if (fileSet.contains(f.getName())) continue;
            if (f.isDirectory()) continue;
            System.out.println(f.getName());
        }
        System.out.println();
    }

    public void reset(String commitHash) {
        checkGitletExistense();

        // Retrieve target commit
        Commit targetCommit = commitStore.getCommit(commitHash);
        if (targetCommit == null) {
            Utils.exitWithMessage("No commit with that id exists.");
        }

        // Checkout the target commit
        checkoutCommit(targetCommit);

        // Update the current branch to point to the target commit
        Branch currentBranch = getCurrentBranch();
        currentBranch.SetCommit(targetCommit.getCommitHash());
        branchStore.saveBranch(currentBranch);
    }

    public void addRemote(String remoteName, String remotePath) {
        //check if local and remote .gitlet folder exist
        checkGitletExistense();
        checkRemoteGitletExistenseAndPathValidity(remotePath);

        remoteStore.addRemotePath(remoteName, remotePath);
    }

    public void removeRemote(String remoteName) {
        checkGitletExistense();

        remoteStore.removeRemotePath(remoteName);
    }

    public void push(String remoteName, String remoteBranchName) {
        //check existense of current gitlet =>existense of remote file=>existense of remote gitlet folder
        checkGitletExistense();
        String remotePath = remoteStore.getRemotePath(remoteName);
        if (remotePath == null) Utils.exitWithMessage("Remote file is not exist");
        checkRemoteGitletExistenseAndPathValidity(remotePath);

        Branch remoteBranch = remoteStore.getRemoteBranch(remoteName, remoteBranchName);
        Branch curBranch = getCurrentBranch();

        final Commit HEAD_COMMIT = commitStore.getCommit(curBranch.getReferredCommitHash());

        final Commit OTHER_COMMIT = remoteStore.getRemoteCommit(remoteBranch.getReferredCommitHash(), remoteName);

        final Commit SPLIT_COMMIT = splitPoint(curBranch, remoteBranch, remoteName);

        ///4 cases
        if (SPLIT_COMMIT == null) {
            Utils.exitWithMessage("There is No common LCA");
        }
        if (SPLIT_COMMIT.equals(HEAD_COMMIT) && !HEAD_COMMIT.equals(OTHER_COMMIT))
            Utils.exitWithMessage("local branch is behind remote branch you must pull before push");
        else if (HEAD_COMMIT.equals(SPLIT_COMMIT) && SPLIT_COMMIT.equals(OTHER_COMMIT) && OTHER_COMMIT.equals(HEAD_COMMIT))
            Utils.exitWithMessage("Already up to date");
        else {
            //divergent 2 cases 
            // check if local pulled from remote => valid push
            //otherwise => you must pull before push
            Set<String> localCommits = commitStore.GetCommitHashes();
            Set<String> remoteCommits = remoteStore.GetRemoteCommitHashes(remoteName);
            for (String hash : remoteCommits) {
                if (!localCommits.contains(hash))
                    Utils.exitWithMessage("Please pull down remote changes before pushing.");

            }
            ArrayList<Commit> copies = new ArrayList<>();
            for (String hash : localCommits) {
                if (!remoteCommits.contains(hash)) copies.add(commitStore.getCommit(hash));
            }
            String gitletPath = System.getProperty("user.dir");
            Path localCommitsPath = Paths.get(gitletPath, ".gitlet", "commits");
            Path localBlobsPath = Paths.get(gitletPath, ".gitlet", "blobs");

            Path remoteCommitsPath = Paths.get(remotePath, "commits");
            Path remoteBlobsPath = Paths.get(remotePath, "blobs");

            branchStore.CopyFromSrcToDist
                    (localCommitsPath.toString(), localBlobsPath.toString(), remoteCommitsPath.toString(), remoteBlobsPath.toString(), copies);

            //synchronize remote head pointer to current commit
            FastForward(remoteName, remoteBranch);
        }


    }

    void fetch(String remoteName, String remoteBranchName) {
        //check existense of current gitlet =>existense of remote file=>existense of remote gitlet folder
        checkGitletExistense();
        String remotePath = remoteStore.getRemotePath(remoteName);
        if (remotePath == null) Utils.exitWithMessage("Remote file is not exist");
        checkRemoteGitletExistenseAndPathValidity(remotePath);

        ///check remote branch
        Branch remoteBranch = remoteStore.getRemoteBranch(remoteName, remoteBranchName);
        if (remoteBranch == null) Utils.exitWithMessage("That remote does not have that branch.");
        ////get local active branch history
        ArrayList<Commit> listOfLocalCommits = branchStore.getBranchHistory(getCurrentCommit(), commitStore);
        Map<String, Boolean> listOfLocalStoredCommits = new TreeMap<>();
        for (Commit commit : listOfLocalCommits) {
            listOfLocalStoredCommits.put(commit.getCommitHash(), true);
        }
        Commit remoteHead = remoteStore.getRemoteCommit(remoteBranch.getReferredCommitHash(), remoteName);

        ArrayList<Commit> listOfRemoteCommits = branchStore.getRemoteBranchHistory(remoteHead, remoteName, remoteStore);
        ArrayList<Commit> copiedCommits = new ArrayList<>();
        for (Commit commit : listOfRemoteCommits) {
            String remoteCommitHash = commit.getCommitHash();
            Boolean difference = listOfLocalStoredCommits.get(remoteCommitHash);
            if (difference == null) copiedCommits.add(commit);
        }
        //copy commits and blobs from remote to local
        String gitletPath = System.getProperty("user.dir");
        Path localCommitsPath = Paths.get(gitletPath, ".gitlet", "commits");
        Path localBlobsPath = Paths.get(gitletPath, ".gitlet", "blobs");

        Path remoteCommitsPath = Paths.get(remotePath, "commits");
        Path remoteBlobsPath = Paths.get(remotePath, "blobs");

        branchStore.CopyFromSrcToDist
                (remoteCommitsPath.toString(), remoteBlobsPath.toString(), localCommitsPath.toString(), localBlobsPath.toString(), copiedCommits);
        ///create remote branch in remotes dir at local and save remote branch object
        remoteStore.saveRemoteBranchAtLocal(remoteName, remoteBranch);

    }

    public void pull(String remoteName, String branchName) {
        fetch(remoteName, branchName);
        merge(branchName, remoteName);
    }

    public void deleteRepo() {
        // validate Gitlet repository existence
        checkGitletExistense();

        // create a safety confirmation mechanism
        System.out.println("WARNING: You are about to permanently delete the entire Gitlet repository.");
        System.out.println("This action cannot be undone and will remove ALL version history, branches, and staged files.");
        System.out.print("Are you sure you want to proceed? (Type 'YES' to confirm): ");

        // Use scanner to get user input
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        String confirmation = scanner.nextLine().trim();

        if (!confirmation.equals("YES")) {
            scanner.close();
            System.out.println("Repository deletion cancelled.");
            return;
        }

        // additional confirmation with repository path
        System.out.printf("Please confirm the repository path: %s\n", CWD.getAbsolutePath());
        System.out.print("Enter the full path to proceed with deletion: ");

        String pathConfirmation = scanner.nextLine().trim();
        scanner.close();
        if (!pathConfirmation.equals(CWD.getAbsolutePath())) {
            System.out.println("Path mismatch. Repository deletion cancelled.");
            return;
        }

        // perform a comprehensive cleanup of Gitlet directories
        File[] subdirectories = {
                Gitlet_Dir,
                Branches_Dir,
                Blobs_Dir,
                Commits_Dir,
                Staged_Dir,
                Addition_Dir,
                Removal_Dir,
                Remote_Dir
        };

        final int[] deletedFiles = {0};
        final int[] deletedDirectories = {0};

        // recursive deletion helper method
        java.util.function.Consumer<File> recursiveDelete = new java.util.function.Consumer<File>() {
            public void accept(File file) {
                if (file.isDirectory()) {
                    File[] contents = file.listFiles();
                    if (contents != null) {
                        for (File f : contents) {
                            accept(f);
                        }
                    }
                    if (file.delete()) {
                        deletedDirectories[0]++;
                    }
                } else if (file.isFile()) {
                    if (file.delete()) {
                        deletedFiles[0]++;
                    }
                }
            }
        };

        // delete Gitlet repository contents
        for (File dir : subdirectories) {
            if (dir.exists()) {
                recursiveDelete.accept(dir);
            }
        }

        // delete head file separately
        if (Head_file.exists()) {
            Head_file.delete();
        }

        boolean fullyDeleted = Gitlet_Dir.exists() == false;

        // provide detailed deletion report
        if (fullyDeleted) {
            System.out.println("Gitlet Repository Successfully Deleted:");
            System.out.printf("Total Files Deleted: %d\n", deletedFiles[0]);
            System.out.printf("Total Directories Deleted: %d\n", deletedDirectories[0]);
            System.out.println("All version control data has been permanently removed.");
        } else {
            System.err.println("WARNING: Complete repository deletion was not successful.");
            System.err.println("Some files or directories might remain. Manual cleanup might be required.");
        }

    }

    public void rebase(String branchName) {
        // Check Gitlet repository existence
        checkGitletExistense();

        // Get current branch and target branch
        Branch currentBranch = getCurrentBranch();
        Branch targetBranch = branchStore.getBranch(branchName);

        if (targetBranch == null) {
            Utils.exitWithMessage("A branch with that name does not exist.");
        }

        // Get commits for current and target branches
        Commit currentCommit = getCurrentCommit();
        Commit targetCommit = commitStore.getCommit(targetBranch.getReferredCommitHash());

        // Find the split point
        Commit splitCommit = splitPoint(currentBranch, targetBranch, null);

        if (splitCommit == null) {
            Utils.exitWithMessage("No common ancestor found.");
        }

        // Fast-forward case: if current branch is ancestor of target branch
        if (splitCommit.equals(currentCommit)) {
            CheckOutBranch(branchName);
            return;
        }

        // Check if target branch is ancestor of current branch
        List<Commit> currentCommitHistory = getCommitTree(currentCommit, null);
        if (currentCommitHistory.contains(targetCommit)) {
            Utils.exitWithMessage("Already up-to-date");
        }

        // Collect commits to replay
        List<Commit> commitsToReplay = new ArrayList<>();
        Commit temp = targetCommit;
        while (!temp.getCommitHash().equals(splitCommit.getCommitHash())) {
            commitsToReplay.add(0, temp);  // Add at the beginning to maintain order
            temp = commitStore.getCommit(temp.getParentCommitHash());
        }

        // Temporarily checkout target branch
        String originalBranchName = head.getHead();
        head.setHead(branchName);

        // Replay commits
        Commit newParent = targetCommit;
        for (Commit commitToReplay : commitsToReplay) {
            // Prepare for merge conflict detection
            Map<String, String> newTrackedFiles = new TreeMap<>(targetCommit.trackedFiles());

            // Handle conflicts and file changes
            for (Map.Entry<String, String> entry : commitToReplay.trackedFiles().entrySet()) {
                String fileName = entry.getKey();
                String commitFileHash = entry.getValue();
                String splitFileHash = splitCommit.trackedFiles().get(fileName);
                String targetFileHash = targetCommit.trackedFiles().get(fileName);

                // Conflict detection logic
                if (!Objects.equals(splitFileHash, commitFileHash) && !Objects.equals(splitFileHash, targetFileHash)) {
                    // Conflict
                    String headContents = blobStore.getBlobContent(commitFileHash);
                    String otherContents = blobStore.getBlobContent(targetFileHash);
                    String contents = "<<<<<<< HEAD\n" +
                            headContents +
                            "\n=======\n" +
                            otherContents +
                            "\n>>>>>>>\n";

                    workingArea.saveFile(contents, fileName);
                    blobStore.saveBlob(workingArea.getFile(fileName));
                    stagingArea.stageForAddition(fileName, Utils.sha1(contents));

                    // Modify tracked files to include conflict file
                    newTrackedFiles.put(fileName, Utils.sha1(contents));
                } else {
                    // No conflict, add file
                    newTrackedFiles.put(fileName, commitFileHash);
                }
//                System.out.println(commitFileHash + " " + fileName);
                workingArea.saveFile(blobStore.getBlobContent(commitFileHash), fileName);
            }

            // Create new commit
            String commitMessage = commitToReplay.CommitMessage() + " (rebase)";
            Commit newCommit = new Commit(
                    new Date(),
                    commitMessage,
                    null,
                    newParent.getCommitHash(),
                    newTrackedFiles
            );

            commitStore.saveCommit(newCommit);
            newParent = newCommit;
        }

        // Update current branch to point to the last replayed commit
        currentBranch.SetCommit(newParent.getCommitHash());
        branchStore.saveBranch(currentBranch);

        // Restore original head
        head.setHead(originalBranchName);

        // Clear staging area
        stagingArea.clear();

    }

    private Commit splitPoint(Branch a, Branch b, String remoteName) {
        Commit A = commitStore.getCommit(a.getReferredCommitHash());
        Commit B;
        if (remoteName != null) {
            B = remoteStore.getRemoteCommit(b.getReferredCommitHash(), remoteName);
        } else B = commitStore.getCommit(b.getReferredCommitHash());

        Set<String> seta = getCommitTree(A, null).stream().map(Commit::getCommitHash).collect(Collectors.toSet());
        Set<String> setb = getCommitTree(B, remoteName).stream().map(Commit::getCommitHash).collect(Collectors.toSet());
        Date LcaDate = new Date(0);
        Commit Point = null;
        for (String sa : seta) {
            if (setb.contains(sa)) {
                Commit x = commitStore.getCommit(sa);
                if (!x.GetTime().before(LcaDate)) {
                    LcaDate = x.GetTime();
                    Point = x;
                }
            }
        }
        return Point;
    }

    private List<Commit> getCommitTree(Commit rootCommit, String remoteName) {
        List<Commit> result = new ArrayList<>();
        DFS(rootCommit, new HashSet<>(), result, remoteName);
        return result;
    }

    private void DFS(Commit node, Set<String> visited, List<Commit> list, String remoteName) {
        list.add(node);
        visited.add(node.getCommitHash());

        String primaryParent = node.getParentCommitHash();
        String secondaryParent = node.getSecondryParent();

        if (primaryParent != null && !visited.contains(primaryParent)) {
            if (remoteName == null) DFS(commitStore.getCommit(primaryParent), visited, list, null);
            else DFS(remoteStore.getRemoteCommit(primaryParent, remoteName), visited, list, remoteName);
        }

        if (secondaryParent != null && !visited.contains(secondaryParent)) {
            if (remoteName == null) DFS(commitStore.getCommit(secondaryParent), visited, list, null);
            else DFS(remoteStore.getRemoteCommit(secondaryParent, remoteName), visited, list, remoteName);
        }
    }

    private void checkGitletExistense() {
        if (!Gitlet_Dir.exists()) {
            Utils.exitWithMessage("initialized Gitlet directory doesn't exist.");
        }
    }

    private void checkRemoteGitletExistenseAndPathValidity(String remotePath) {
        File remoteDir = new File(remotePath);
        if (!remotePath.endsWith(".gitlet") || !remoteDir.isDirectory())
            Utils.exitWithMessage("Remote directory not found.");
    }

    //get active branch
    private Branch getCurrentBranch() {
        return branchStore.getBranch(head.getHead());
    }

    //get current commit refered to by active branch
    private Commit getCurrentCommit() {
        String curCommitHash = getCurrentBranch().getReferredCommitHash();
        return commitStore.getCommit(curCommitHash);
    }

    private void FastForward(String remoteName, Branch remoteBranch) {
        String curCommitHash = getCurrentCommit().getCommitHash();
        remoteBranch.SetCommit(curCommitHash);
        remoteStore.saveRemoteBranch(remoteName, remoteBranch);
    }

    private void checkoutCommit(Commit targetCommit) {
        // Check for untracked files that could be overwritten
        boolean hasUntrackedFiles = workingArea.allFiles().stream()
                .map(File::getName)
                .filter(fileName -> !getCurrentCommit().trackedFiles().containsKey(fileName))
                .anyMatch(fileName -> targetCommit.trackedFiles().containsKey(fileName));

        if (hasUntrackedFiles) {
            Utils.exitWithMessage("There is an untracked file in the way; delete it, or add and commit it first.");
        }

        // Clear working and staging areas
        workingArea.Clear();
        stagingArea.clear();

        // Checkout files from the target commit
        targetCommit.trackedFiles().keySet().forEach(fileName -> CheckOutFileByHash(targetCommit.getCommitHash(), fileName));
    }

}



