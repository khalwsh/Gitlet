import java.io.File;
import java.io.IOException;
public class StagingArea {
    private final File Addition_Dir;
    private final File Removal_Dir;


    public File[] GetFilesForAddition(){
        File[] filesList = Addition_Dir.listFiles();
        return filesList;
    }
    public File[] GetFilesForRemoval(){
        File[] filesList = Removal_Dir.listFiles();
        return filesList;
    }
    public boolean IsEmpty(){
        return GetFilesForAddition().length == 0 && GetFilesForRemoval().length == 0;
    }
    public StagingArea(File Addition_Dir, File Removal_Dir) {
        this.Addition_Dir = Addition_Dir;
        this.Removal_Dir = Removal_Dir;

    }

    //this method check if file that exist in working directory is the same as in staging area
    public boolean checkBlobExistense(String targetedNameFile, String blobHash) {

        File file = Utils.join(this.Addition_Dir, targetedNameFile);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.out.println("Error creating file: " + e.getMessage());
            }
        }

        String fileContent = Utils.readContentsAsString(file);
        return fileContent.equals(blobHash);
    }

    //Addition_Dir=>fileName=>Sha-1
    public void stageForAddition(String fileName, String hash) {
        File file = Utils.join(this.Addition_Dir, fileName);
        Utils.writeContents(file, hash);
    }

    public boolean unstageForRemoval(String fileName) {
        File file = Utils.join(Removal_Dir, fileName);

        if (file == null) return false;
        return file.delete();
    }

    public void clear(){
        for(File file : GetFilesForAddition()){
            file.delete();
        }
        for(File file : GetFilesForRemoval()){
            file.delete();
        }
    }
}
