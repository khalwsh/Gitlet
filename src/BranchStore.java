
import java.io.File;
import java.util.ArrayList;
public class BranchStore {
    private final File Branches_Dir;

    public BranchStore(File Branches_Dir) {
        this.Branches_Dir = Branches_Dir;
    }
       public void saveBranch(Branch branch)
       {
             File currentBranchFile=Utils.join(Branches_Dir, branch.getName());
            Utils.writeObject(currentBranchFile, branch);
       }
       public void deleteBranch(String branchName)
       {
             File branchFile=Utils.join(Branches_Dir, branchName);
             branchFile.delete();
       }
       public Branch getBranch(String branchName)
       {
        File branchFile = Utils.join(Branches_Dir, branchName);
        if (!branchFile.exists()) return null;
        return Utils.readObject(branchFile, Branch.class);
       }

       public ArrayList<Commit> getBranchHistory(Commit currCommit, CommitStore commitStore)
       {
                ArrayList<Commit>listOfCommits=new ArrayList<Commit>();
                
                    listOfCommits.add(currCommit);
                  while (currCommit.getParentCommitHash()!=null) {

                    String prevCommitHash=currCommit.getParentCommitHash();
                    Commit prevCommit=commitStore.getCommit(prevCommitHash);
                    listOfCommits.add(prevCommit);
                    currCommit=prevCommit;
                  }
                  
                return listOfCommits;
       }
       public boolean CheckBranchExistence(String branchName)
       {
               File branchFile=Utils.join(Branches_Dir, branchName);
               return branchFile.exists();
       }
       public void createNewBranch(String branchName ,String commitHash)
       {
       
              Branch newBranch=new Branch(branchName, commitHash);
              saveBranch(newBranch);
       }
}
