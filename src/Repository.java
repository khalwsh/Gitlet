
import java.io.File;
import java.util.Date;

public class Repository {
    private final File CWD;
    private final File Gitlet_Dir;
    
    private final File Branches_Dir;
    private final File Blobs_Dir;
    private final File Commits_Dir;
    private final File Staged_Dir;
    private final File Addition_Dir;
    private final File Removal_Dir;
    private final File Head_file;

    private final Head head;
    private final CommitStore commitStore;
    private final BranchStore branchStore;
    private final BlobStore blobStore;
    private final WorkingArea workingArea;
    private final StagingArea stagingArea;
    public Repository(String cwd)
    {
           CWD=new File(cwd);
            
            if(!CWD.exists()) throw new RuntimeException("Working directory not found.");
            
            Gitlet_Dir=Utils.join(CWD,".gitlet");
            Branches_Dir=Utils.join(Gitlet_Dir,"branches");
            Blobs_Dir=Utils.join(Gitlet_Dir,"blobs");
            Commits_Dir=Utils.join(Gitlet_Dir, "commits");
            Staged_Dir=Utils.join(Gitlet_Dir, "staged");
            Addition_Dir=Utils.join(Staged_Dir, "addition");
            Removal_Dir=Utils.join(Staged_Dir, "removal");
            Head_file=Utils.join(Gitlet_Dir,"head");
             
            commitStore = new CommitStore(Commits_Dir);
            branchStore=new BranchStore(Branches_Dir);
            blobStore=new BlobStore(Blobs_Dir);
            workingArea=new WorkingArea(CWD);
            stagingArea=new StagingArea(Addition_Dir,Removal_Dir);
            head=new Head(Head_file);
    }
   
    public void init()
    {
        if(Gitlet_Dir.exists()) Utils.exitWithMessage("Gitlet Repository already exists in current working directory");

          Gitlet_Dir.mkdir();
          Blobs_Dir.mkdir();
          Commits_Dir.mkdir();
          Staged_Dir.mkdir();
          Addition_Dir.mkdir();
          Removal_Dir.mkdir();
          Branches_Dir.mkdir();

          try{
            Head_file.createNewFile();
          }
          catch(java.io.IOException ex)
          {
                   Utils.exitWithMessage(ex.getMessage());
          }
          Commit commit=new Commit(new Date(0), "Initial Commit");   //Initialize the directory structure inside .gitlet 
          // start with initial commit with message "initial commit" and timestamp=Unix epoch  
          commitStore.saveCommit(commit);   
          Branch master=new Branch("master", commit.getCommitHash());// create master branch
        branchStore.saveBranch(master);
          head.setHead(master); //set head to point to master
    }
  
///If the file in the working directory is different from the file in the staging area:
///Git will create a new blob object for the updated content.
//The staging area is updated to reference this new blob, meaning the new content will be included in the next commit.
//If the file in the working directory is the same as the file in the staging area:
//No new blob is created, and the file is not updated in the staging area. The staging area already has the correct content, so there is no need to re-stage the file.

    public void add(String fileName)
    {
                 //check gitlet repo existence
                checkGitletExistense();
              //check file existense 
            File currentFile=workingArea.checkFileExistense(fileName);
            if(currentFile==null) Utils.exitWithMessage("File doesn't exist");
              
             
             String curFileHash=Utils.sha1(Utils.readContentsAsString(currentFile));
             
                if(!stagingArea.checkBlobExistense(fileName, curFileHash))
                {
                     //there is modification and last version must be added to staging area
                     //so create a new blob
                     String fileHashCWD=blobStore.saveBlob(currentFile);
                     //then refer to that blob at staging area (adding new version to staging area)
                            stagingArea.stageForAddition(fileName, fileHashCWD);
                }
                else     System.out.println("File '" + fileName + "' is already up-to-date in the staging area.");
                
                 
                stagingArea.unstageForRemoval(fileName);
    }
    private void checkGitletExistense() {
      if (!Gitlet_Dir.exists()) {
          Utils.exitWithMessage("initialized Gitlet directory doesn't exist.");
      }
  }
//   //get active branch
//   private Branch getCurrentBranch() {
//     return branchStore.getBranch(head.getHead());
// }
// //get current commit refered to by active branch
// private Commit getCurrentCommit()
// {
//   String curCommitHash=getCurrentBranch().getReferredCommitHash();
// return commitStore.getCommit(curCommitHash);
// }


}
