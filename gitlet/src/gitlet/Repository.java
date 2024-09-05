package gitlet;
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
    public Repository(String cwd)
    {
            CWD=new File(cwd);
            
            if(!CWD.exists()) throw new RuntimeException("Working directory not found.");
            
            Gitlet_Dir=Utils.join(CWD,".gitlet");
            Branches_Dir=Utils.join(Gitlet_Dir,"branches");
            Blobs_Dir=Utils.join(Gitlet_Dir,"blobs");
            Commits_Dir=Utils.join(Gitlet_Dir, "commits");
            Staged_Dir=Utils.join(Gitlet_Dir, "staged");
            Addition_Dir=Utils.join(Gitlet_Dir, "addition");
            Removal_Dir=Utils.join(Gitlet_Dir, "removal");
            Head_file=Utils.join(Gitlet_Dir,"head");

            commitStore = new CommitStore(Commits_Dir);
            branchStore=new BranchStore(Branches_Dir);
            head=new Head(Head_file);
    }
    /**
     * Initialize the directory structure inside .gitlet
     * Start with a single branch called "master"
     * Start with one commit with the message "initial commit" with a timestamp = Unix epoch
     * Set HEAD to point to the master branch
     */
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
          Commit commit=new Commit(new Date(0), "Initial Commit");      
          commitStore.saveCommit(commit);   
          Branch master=new Branch("master", commit.getCommitHash());
        branchStore.saveBranch(master);
          head.setHead(master);
    }
    public void add(String fileName)
    {
     
    }
}
