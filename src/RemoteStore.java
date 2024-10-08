
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class RemoteStore {
    private final File Remote_Dir;

    public RemoteStore(File Remote_Dir) {
        this.Remote_Dir = Remote_Dir;
    }
public Set<String> GetRemoteCommitHashes(String remoteName) {

    String remotePath=getRemotePath(remoteName);
    if(remotePath ==null) Utils.exitWithMessage("Remote file is not exist");

  File remoteCommitFile=Utils.join(remotePath,"commits");
  if(!remoteCommitFile.exists()) return null;

        File[] filesList = remoteCommitFile.listFiles();
          Set<String> set = new HashSet<>();
        for (File file : filesList) {
            set.add(file.getName());
        }
        return set;
    
    }
    //just create new file with remote name and path to it
   public void addRemotePath(String remoteName, String remotePath) {
    // Create the directory for the remote
    File remoteDir = Utils.join(Remote_Dir, remoteName);
    
    // Check if the remote directory already exists
    if (remoteDir.exists()) {
        Utils.exitWithMessage("A remote with that name already exists.");
    }

    // Create the remote directory and subdirectories for blobs and commits
    if (!remoteDir.mkdirs()) {
        Utils.exitWithMessage("Failed to create the remote directory structure.");
    }
    
    // Create the file to store the remote path
    File remotePathFile = Utils.join(remoteDir, remoteName);
        Utils.writeContents(remotePathFile, remotePath);

}
    
    public void removeRemotePath(String remoteName)
    {
        File reomteFile=Utils.join(Remote_Dir, remoteName,remoteName);
        if(!reomteFile.exists()) Utils.exitWithMessage( "A remote with that name does not exist.");

        reomteFile.delete();
    }
    public String getRemotePath(String remoteName)
    {
        File reomteFile=Utils.join(Remote_Dir,remoteName ,remoteName);
        if(reomteFile.exists())
        return Utils.readContentsAsString(reomteFile);
        return null;
    }
    public Branch getRemoteBranch(String remoteName,String remoteBranchName)
    {
       String remotePath=getRemotePath(remoteName);
       if(remotePath ==null) Utils.exitWithMessage("Remote file is not exist");

     File remoteBranchFile=Utils.join(remotePath,"branches",remoteBranchName);
     if(!remoteBranchFile.exists()) return null;
  
        Branch remoteBranch=Utils.readObject(remoteBranchFile, Branch.class);
        return remoteBranch;
          
    }
    public Branch getRemoteBranchFromLocal(String remoteName,String remoteBranch)
    {
        File remoteBranchFile=Utils.join(Remote_Dir, remoteName,remoteBranch);
        if(!remoteBranchFile.exists()) return null;
        Branch branch= Utils.readObject(remoteBranchFile,Branch.class);
        return branch;
    }

    public void saveRemoteBranchAtLocal(String remoteName,Branch remoteBranch)
    {
        File remoteBranchFile=Utils.join(Remote_Dir, remoteName,remoteBranch.getName());
          Utils.writeObject(remoteBranchFile,remoteBranch);
    }
    public void saveRemoteBranch(String remoteName,Branch remoteBranch)
    {
        String remotePath=getRemotePath(remoteName);
        if(remotePath ==null) Utils.exitWithMessage("Remote file is not exist");
 
      File remoteBranchFile=Utils.join(remotePath,"branches",remoteBranch.getName());
        
      Utils.writeObject(remoteBranchFile, remoteBranch);
      
    }
    public Commit getRemoteCommit(String commitHash,String remoteName)
    {
     if(commitHash ==null) return null;

     String remotePath=getRemotePath(remoteName);
     if(remotePath ==null) Utils.exitWithMessage("Remote file is not exist");

     File currentCommitFile=Utils.join(remotePath,"commits", commitHash);
     
    
    //////java.lang.IllegalArgumentException here
         
         return Utils.readObject(currentCommitFile,Commit.class);
         
      
     
    }
}