
import java.io.File;

public class RemoteStore {
    private final File Remote_Dir;

    public RemoteStore(File Remote_Dir) {
        this.Remote_Dir = Remote_Dir;
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

     File remotBranchFile=Utils.join(remotePath,"branches",remoteBranchName);
     if(!remotBranchFile.exists()) return null;
  
        Branch remoteBranch=Utils.readObject(remotBranchFile, Branch.class);
        return remoteBranch;
          
    }
    public void saveRemoteBranchFile(String remoteName,Branch remoteBranch)
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
         if(currentCommitFile.exists())
         return Utils.readObject(currentCommitFile,Commit.class);
         
         return null;
     
    }
}