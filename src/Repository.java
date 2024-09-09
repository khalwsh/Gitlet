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
import java.sql.Time;
import java.util.*;
import java.time.Instant;

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


        String committedFileHash = getCurrentCommit().trackedFiles().get(fileName);
        String workingFileHash = Utils.sha1(Utils.readContentsAsString(currentFile));
        if (!workingFileHash.equals(committedFileHash)) {
            stagingArea.stageForAddition(fileName,workingFileHash);
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


private String getCurrentBranchName(){
    return head.getHead();
}
public void status() {

    // printing the branch names
    checkGitletExistense();
    System.out.println("=== Branches ===");
    String CurrentBranchName =head.getHead();
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

        Branch targetBranch = branchStore.getBranch(branchName);
        if (targetBranch == null) {
            Utils.exitWithMessage("A branch with that name does not exist.");
        }

        Branch currentBranch = getCurrentBranch();
        if (targetBranch.getName().equals(currentBranch.getName())) {
            Utils.exitWithMessage("Cannot merge a branch with itself.");
        }

        if (workingArea.allFiles().stream().anyMatch(file -> !getCurrentCommit().trackedFiles().containsKey(file.getName()))) {
            Utils.exitWithMessage("There is an untracked file in the way; delete it, or add and commit it first.");
        }

        final Commit HEAD_COMMIT = commitStore.getCommit(currentBranch.getReferredCommitHash());
        final Commit OTHER_COMMIT = commitStore.getCommit(targetBranch.getReferredCommitHash());
        final Commit SPLIT_COMMIT = splitPoint(currentBranch, targetBranch);

        if (SPLIT_COMMIT.equals(OTHER_COMMIT)) {
            Utils.exitWithMessage("Given branch is an ancestor of the current branch.");
        }

        if (SPLIT_COMMIT.equals(HEAD_COMMIT)) {
            CheckOutBranch(targetBranch.getName());
            Utils.exitWithMessage("Current branch fast-forwarded.");
        }

        Set<String> ALL = new HashSet<>();
        ALL.addAll(SPLIT_COMMIT.trackedFiles().keySet());
        ALL.addAll(HEAD_COMMIT.trackedFiles().keySet());
        ALL.addAll(OTHER_COMMIT.trackedFiles().keySet());

        AtomicBoolean isConflict = new AtomicBoolean(false);

        ALL.forEach(fileName -> {
            final String SPLIT = SPLIT_COMMIT.trackedFiles().get(fileName);
            final String HEAD  = HEAD_COMMIT.trackedFiles().get(fileName);
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
                        "=======\n" +
                        otherContents +
                        ">>>>>>>\n";
                workingArea.saveFile(contents, fileName);
                stagingArea.stageForAddition(contents, fileName);
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
                if(temp != null) {
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
    private Commit splitPoint(Branch a, Branch b) {
        Commit A = commitStore.getCommit(a.getReferredCommitHash());
        Commit B = commitStore.getCommit(b.getReferredCommitHash());
        Set<String> seta = getCommitTree(A).stream().map(Commit::getCommitHash).collect(Collectors.toSet());
        Set<String> setb = getCommitTree(B).stream().map(Commit::getCommitHash).collect(Collectors.toSet());
        Date LcaDate = new Date(0);
        Commit Point = null;
        for(String sa : seta){
            if(setb.contains(sa)){
                Commit x = commitStore.getCommit(sa);
                if(x.GetTime().after(LcaDate)){
                    LcaDate = x.GetTime();
                    Point = x;
                }
            }
        }
        return Point;
    }
    private List<Commit> getCommitTree(Commit rootCommit) {
        List<Commit> result = new ArrayList<>();
        DFS(rootCommit, new HashSet<>(), result);
        return result;
    }

    private void DFS(Commit node, Set<String> visited, List<Commit> list) {
        list.add(node);
        visited.add(node.getCommitHash());

        String primaryParent = node.GetParent();
        String secondaryParent = node.getSecondryParent();

        if (primaryParent != null && !visited.contains(primaryParent)) {
            DFS(commitStore.getCommit(primaryParent), visited, list);
        }

        if (secondaryParent != null && !visited.contains(secondaryParent)) {
            DFS(commitStore.getCommit(secondaryParent), visited, list);
        }
    }
public void addRemote(String remoteName,String remotePath)
{
    //check if local and remote .gitlet folder exist
checkGitletExistense();
checkRemoteGitletExistenseAndPathValidity(remotePath);
       
  remoteStore.addRemotePath(remoteName, remotePath);
}
public void removeRemote(String remoteName)
{
    checkGitletExistense();

 remoteStore.removeRemotePath(remoteName);       
}
/////////////////
public void push(String remoteName,String remoteBranchName)
{
    //check existense of current gitlet =>existense of remote file=>existense of remote gitlet folder
    checkGitletExistense();
       String remotePath= remoteStore.getRemotePath(remoteName);
        if(remotePath==null) Utils.exitWithMessage("Remote file is not exist");
    checkRemoteGitletExistenseAndPathValidity(remotePath);
         ///get remote  branch into memory
        Branch remoteBranch=remoteStore.getRemoteBranch(remoteName, remoteBranchName);
        ///check if remote head is an ancestor of local branch history
        ///if yes then push to remote 
        /// otherwise you must pull before push
        Map.Entry<Boolean, ArrayList<Commit>>
        checkAncestor=branchStore.checkIsAncestor(remoteName,remoteBranch, getCurrentCommit(), commitStore,remoteStore);
        if(!checkAncestor.getKey()) Utils.exitWithMessage("Please pull down remote changes before pushing.");
        else
        {//copy commits and blobs from local to remote
            String gitletPath = System.getProperty("user.dir");
            Path localCommitsPath = Paths.get(gitletPath, ".gitlet", "commits");
            Path localBlobsPath = Paths.get(gitletPath, ".gitlet", "blobs");
          
            Path remoteCommitsPath = Paths.get(remotePath, "commits");
            Path remoteBlobsPath = Paths.get(remotePath, "blobs");

            branchStore.CopyFromSrcToDist
            (localCommitsPath.toString(), localBlobsPath.toString(),remoteCommitsPath.toString() , remoteBlobsPath.toString(),checkAncestor.getValue()); 
       
       //now set remote head to current commit
         FastForward(remoteName,remoteBranch);
        }
}

void fetch(String remoteName,String remoteBranchName)
{
    //check existense of current gitlet =>existense of remote file=>existense of remote gitlet folder
    checkGitletExistense();
       String remotePath= remoteStore.getRemotePath(remoteName);
        if(remotePath==null) Utils.exitWithMessage("Remote file is not exist");
    checkRemoteGitletExistenseAndPathValidity(remotePath);

    ///check remote branch 
    Branch remoteBranch=remoteStore.getRemoteBranch(remoteName, remoteBranchName);
    if(remoteBranch==null) Utils.exitWithMessage("That remote does not have that branch.");
    ////get local active branch history
    ArrayList<Commit>listOfLocalCommits=branchStore.getBranchHistory(getCurrentCommit(), commitStore);
         Map<String,Boolean>listOfLocalStoredCommits=new TreeMap<>();
         for (Commit commit : listOfLocalCommits) {
            listOfLocalStoredCommits.put(commit.getCommitHash(), true);
         }
         Commit remoteHead=remoteStore.getRemoteCommit(remoteBranch.getReferredCommitHash(),remoteName);

         ArrayList<Commit>listOfRemoteCommits=branchStore.getRemoteBranchHistory(remoteHead,remoteName,remoteStore);
         ArrayList<Commit>copiedCommits=new ArrayList<>();
           for (Commit commit : listOfRemoteCommits) {
            String remoteCommitHash=commit.getCommitHash();
            Boolean difference=listOfLocalStoredCommits.get(remoteCommitHash);
              if(difference==null) copiedCommits.add(commit);
           }
           //copy commits and blobs from remote to local
           String gitletPath = System.getProperty("user.dir");
           Path localCommitsPath = Paths.get(gitletPath, ".gitlet", "commits");
           Path localBlobsPath = Paths.get(gitletPath, ".gitlet", "blobs");
         
           Path remoteCommitsPath = Paths.get(remotePath, "commits");
           Path remoteBlobsPath = Paths.get(remotePath, "blobs");

           branchStore.CopyFromSrcToDist
           (remoteCommitsPath.toString(), remoteBlobsPath.toString(),localCommitsPath.toString() , localBlobsPath.toString(),copiedCommits); 
            ///create remote branch in remotes dir at local and save remote branch object
           remoteStore.saveRemoteBranchFile(remoteName, remoteBranch);

}





  //get active branch
  private Branch getCurrentBranch() 
  { 
    return branchStore.getBranch(head.getHead());
  }
//get current commit refered to by active branch
private Commit getCurrentCommit()
{
  String curCommitHash=getCurrentBranch().getReferredCommitHash();
return commitStore.getCommit(curCommitHash);
}

private void checkGitletExistense()
 {
    if (!Gitlet_Dir.exists()) 
    {
        Utils.exitWithMessage("initialized Gitlet directory doesn't exist.");
    }
}
private void checkRemoteGitletExistenseAndPathValidity(String remotePath)
{
    File remoteDir = new File(remotePath);
    if (!remotePath.endsWith(".gitlet")|| !remoteDir.isDirectory())  Utils.exitWithMessage("Remote directory not found.");
}
private void FastForward(String remoteName,Branch remoteBranch)
{
    String curCommitHash=getCurrentCommit().getCommitHash();
    remoteBranch.SetCommit(curCommitHash);
    remoteStore.saveRemoteBranch(remoteName, remoteBranch);
}
}
