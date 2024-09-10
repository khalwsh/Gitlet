
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
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

    public ArrayList<Commit> getBranchHistory(Commit currCommit, CommitStore commitStore) {
        ArrayList<Commit> listOfCommits = new ArrayList<Commit>();

        listOfCommits.add(currCommit);
        while (currCommit.getParentCommitHash() != null) {

                   Commit prevCommit=getPreviousCommit(currCommit, commitStore);
                   if(prevCommit==null) break;
                    listOfCommits.add(prevCommit);
                    currCommit=prevCommit;
                  }
                  
                return listOfCommits;
       }
       public ArrayList<Commit>getRemoteBranchHistory(Commit curCommit,String remoteName,RemoteStore remoteStore)
       {

        ArrayList<Commit>listOfCommits=new ArrayList<Commit>();
                
        listOfCommits.add(curCommit);
        
      while (curCommit.getParentCommitHash()!=null) {

        String prevCommitHash=curCommit.getParentCommitHash();
        Commit prevCommit=remoteStore.getRemoteCommit(prevCommitHash,remoteName);
       if(prevCommit==null) break;
        listOfCommits.add(prevCommit);
        curCommit=prevCommit;
      }
      
    return listOfCommits;
      
       }
           Commit getPreviousCommit(Commit curCommit,CommitStore commitStore)
           {
            String prevCommitHash=curCommit.getParentCommitHash();
                    Commit prevCommit=commitStore.getCommit(prevCommitHash);
                    return prevCommit;
           }
           
       public Map.Entry<Boolean, ArrayList<Commit>> checkIsAncestor(String remoteName,Branch remoteBranch,Commit curCommit,CommitStore commitStore,RemoteStore remoteStore)
       {
               String remoteHeadHash=remoteBranch.getReferredCommitHash();
              

                ArrayList<Commit>listOfCommits=new ArrayList<Commit>();
               boolean Found=false;
               while(true)
               {
                if(!remoteHeadHash.equals(curCommit.getCommitHash()))
                {
                  listOfCommits.add(curCommit);

                  Commit prevCommit=getPreviousCommit(curCommit, commitStore);

                  if(prevCommit==null) break;
                  curCommit=prevCommit;
                }
                else {Found=true;break;}
                      
               }
               //possibility for shared history
               if(Found)
               {
                      Commit remoteCommit=remoteStore.getRemoteCommit(remoteHeadHash,remoteName);
                      while (curCommit!=null && remoteCommit!=null ) {

                        if(!remoteCommit.getCommitHash().equals(curCommit.getCommitHash())) 
                        {
                          //history is not identical
                          Found=false;
                          break;
                        }
                         Commit prevLocalCommit=getPreviousCommit(curCommit, commitStore);
                         curCommit=prevLocalCommit;
                         Commit prevRemoteCommit=getPreviousCommit(remoteCommit, commitStore);
                         remoteCommit=prevRemoteCommit;
                      }
               }
           
               return new AbstractMap.SimpleEntry<>(Found,listOfCommits );
             
       }
       public void CopyFromSrcToDist
       (String firstSrc,String secondSrc,String firstDist,String secondDist,ArrayList<Commit>listOfCommits)
       {
        //copy commits and blobs
             for(int i=0;i<listOfCommits.size();i++)
             {

              Commit curCommit=listOfCommits.get(i);
              //copy commits
              String hash=curCommit.getCommitHash();
              Path commitSourcePath = Paths.get(firstSrc, hash);
              Path commitDestPath = Paths.get(firstDist, hash);
              copyFile(commitSourcePath, commitDestPath);
              //copy blobs
              Map<String,String>trackedFiles;
              trackedFiles=curCommit.trackedFiles();

              for (Map.Entry<String, String> entry : trackedFiles.entrySet()) {
               String blob=entry.getValue();
               Path blobSourcePath = Paths.get(secondSrc, blob);
               Path blobDestPath = Paths.get(secondDist, blob);
               copyFile(blobSourcePath, blobDestPath);
            }

             }
        
       }
      
       public static void copyFile(Path sourcePath, Path destinationPath) {
        try {
          // Ensure the parent directory exists
          Files.createDirectories(destinationPath.getParent());

          // Copy the file from source to destination
          Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);

      } catch (IOException e) {
          System.out.println("An error occurred while copying the file.");
          e.printStackTrace();
      }
    }

   
       public void createNewBranch(String branchName ,String commitHash)
       {
       
              Branch newBranch=new Branch(branchName, commitHash);
              saveBranch(newBranch);
       }
       public String[]GetAllBranchesName() {
        return Branches_Dir.list();
    }

}
