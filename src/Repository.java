import java.awt.image.AreaAveragingScaleFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.TreeSet;
import java.io.*;
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

    ///If the file in the working directory is different from the file in the staging area:
///Git will create a new blob object for the updated content.
//The staging area is updated to reference this new blob, meaning the new content will be included in the next commit.
//If the file in the working directory is the same as the file in the staging area:
//No new blob is created, and the file is not updated in the staging area. The staging area already has the correct content, so there is no need to re-stage the file.
    public void add(String fileName) {
        //check gitlet repo existence
        checkGitletExistense();
        //check file existense
        File currentFile = workingArea.checkFileExistense(fileName);
        if (currentFile == null) Utils.exitWithMessage("File doesn't exist");


        String curFileHash = Utils.sha1(Utils.readContentsAsString(currentFile));

        if (!stagingArea.checkBlobExistense(fileName, curFileHash)) {
            //there is modification and last version must be added to staging area
            //so create a new blob
            String fileHashCWD = blobStore.saveBlob(currentFile);
            //then refer to that blob at staging area (adding new version to staging area)
            stagingArea.stageForAddition(fileName, fileHashCWD);
        } else Utils.exitWithMessage("This file is already up-to-date in the staging area.");


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
            ; //untracked and not staged for addition
        } else {
            if (checkFileStagedForAddition) stagingArea.UnStageForAddittion(fileName);
            ///add last commited file version to staged for removal 
            stagingArea.StageForRemoval(fileName, fileHash);
            ///remove current file version form CWD
            workingArea.removeFromCWD(fileName);
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

    private void checkGitletExistense() {
        if (!Gitlet_Dir.exists()) {
            Utils.exitWithMessage("initialized Gitlet directory doesn't exist.");
        }
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
        checkGitletExistense(); // check repo is initialized
        //check branch existence and not the same as the current one
        if (!branchStore.CheckBranchExistence(branchName)) Utils.exitWithMessage("No such branch exists.");
        else {
            String activeBranch = head.getHead();
            if (activeBranch.equals(branchName)) Utils.exitWithMessage("No need to checkout the current branch.");
            else {
                //get list of all tracked files in both active and target branches
                Map<String, String> trackedInActive = getCurrentCommit().trackedFiles();

                String commitHashInTarget = branchStore.getBranch(branchName).getReferredCommitHash();
                Commit targetCommit = commitStore.getCommit(commitHashInTarget);

                Map<String, String> trackedInTarget = targetCommit.trackedFiles();

                //check for tracked in active
                for (Map.Entry<String, String> entry : trackedInActive.entrySet()) {

                    String hashOfTarget = trackedInTarget.get(entry.getKey());
                    if (hashOfTarget == null) {
                        //tracked in active and untracked in target=> remove it from CWD
                        workingArea.removeFromCWD(entry.getKey());
                    } else {//tracked in active and target
                        //replace version of target into CWD
                        String blobContent = blobStore.getBlobContent(hashOfTarget);
                        workingArea.addOrUpdateFileAtCWD(entry.getKey(), blobContent);
                    }
                }
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
                //change active branch to target and clear staging area
                head.setHead(branchName);
                stagingArea.clear();

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
            boolean branchExist = branchStore.CheckBranchExistence(branchName);
            if (branchExist == false) Utils.exitWithMessage("A branch with that name does not exist.");
            else {
                //branch exist and not active one then remove it
                branchStore.deleteBranch(branchName);
            }
        }
    }


    private String getCurrentBranchName() {
        return head.getHead();
    }

    public void status() {

        // printing the branch names
        checkGitletExistense();
        System.out.println("=== Branches ===");
        String CurrentBranchName = getCurrentBranchName();
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

    public void addRemote(String remoteName, String remotePath) {
        File remoteDir = new File(remotePath);
        if (!remotePath.endsWith(".gitlet") || !remoteDir.isDirectory())
            Utils.exitWithMessage("Invalid path for directory");

        remoteStore.addRemotePath(remoteName, remotePath);


    }

    public void removeRemote(String remoteName) {
        remoteStore.removeRemotePath(remoteName);
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
        workingArea.Clear();  // Ensure this method is correctly implemented
        stagingArea.clear();

        // Checkout files from the target commit
        targetCommit.trackedFiles().keySet().forEach(fileName ->
                CheckOutFileByHash(targetCommit.getCommitHash(), fileName));

        // Remove files that are tracked in the current commit but not in the target commit
        getCurrentCommit().trackedFiles().keySet().stream()
                .filter(fileName -> !targetCommit.trackedFiles().containsKey(fileName))
                .forEach(fileName -> workingArea.remove(fileName)); // Ensure remove() is implemented
    }

    public void reset(String commitHash) {
        checkGitletExistense();  // Ensure method name is correct

        // Retrieve target commit
        Commit targetCommit = commitStore.getCommit(commitHash);
        if (targetCommit == null) {
            Utils.exitWithMessage("No commit with that id exists.");
        }

        // Checkout the target commit
        checkoutCommit(targetCommit);

        // Update the current branch to point to the target commit
        Branch currentBranch = getCurrentBranch();
        currentBranch.SetCommit(targetCommit.getCommitHash());  // Ensure this method is correctly implemented
        branchStore.saveBranch(currentBranch);
    }



}
