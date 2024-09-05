package gitlet;
import java.io.File;
public class BranchStore {
    private final File Branches_Dir;

    public BranchStore(File Branches_Dir)
    {
     this.Branches_Dir=Branches_Dir;
    }
       public void saveBranch(Branch branch)
       {
             File currentBranchFile=Utils.join(Branches_Dir, branch.getName());
            Utils.writeObject(currentBranchFile, branch);
       }
       public Branch getBranch(String branchName)
       {
        File branchFile = Utils.join(Branches_Dir, branchName);
        if (!branchFile.exists()) return null;
        return Utils.readObject(branchFile, Branch.class);
       }
}
