import java.io.IOException;
import java.io.File;
public class CommitStore {
    private final File Commits_Dir;

    public CommitStore(File Commits_Dir)
    {
     this.Commits_Dir=Commits_Dir;
    }
       public void saveCommit(Commit commit)
       {
             File currentCommitFile=Utils.join(Commits_Dir, commit.getCommitHash());
            Utils.writeObject(currentCommitFile, commit);
       }
       public Commit getCommit(String commitHash)
       {
        if(commitHash ==null) return null;
        File currentCommitFile=Utils.join(Commits_Dir, commitHash);
        
       //////java.lang.IllegalArgumentException here
            if(currentCommitFile.exists())
            return Utils.readObject(currentCommitFile, Commit.class);
            
            return null;
        
       }

}
