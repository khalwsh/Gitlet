import java.io.File;
import java.io.IOException;
public class StagingArea {
    private final File Addition_Dir;
    private final File Removal_Dir;


    public File[] GetFilesForAddition() {
        return Addition_Dir.listFiles(file -> file.isFile());
    }
    public String[] GetNameOfFilesForAddition(){
        return Addition_Dir.list();
    }
    public File[] GetFilesForRemoval() {
        return Removal_Dir.listFiles();

    }
    public String[] GetNameOfFilesForRemoval() {
       return Removal_Dir.list();
    }
    public String[] GetAllFilesNames(){
        // Get file names from both directories
        String[] additionFiles = GetNameOfFilesForAddition();
        String[] removalFiles = GetNameOfFilesForRemoval();
        if(additionFiles.length == 0 && removalFiles.length == 0){
            return null;
        }
        // Handle null cases (in case directories are empty or invalid)
        if (additionFiles == null) {
            additionFiles = new String[0];
        }
        if (removalFiles == null) {
            removalFiles = new String[0];
        }

        // Create an array to hold all file names from both directories
        String[] allFiles = new String[additionFiles.length + removalFiles.length];

        // Copy files from the first directory
        System.arraycopy(additionFiles, 0, allFiles, 0, additionFiles.length);

        // Copy files from the second directory
        System.arraycopy(removalFiles, 0, allFiles, additionFiles.length, removalFiles.length);

        return allFiles;
    }
    public boolean IsEmpty() {
        return GetFilesForAddition().length == 0 && GetFilesForRemoval().length == 0;
    }

    public StagingArea(File Addition_Dir, File Removal_Dir) {
        this.Addition_Dir = Addition_Dir;
        this.Removal_Dir = Removal_Dir;

    }

   
    //Addition_Dir=>fileName=>Sha-1
    public void stageForAddition(String fileName, String hash) {
        File file = Utils.join(this.Addition_Dir, fileName);
        Utils.writeContents(file, hash);
    }

    public void UnStageForAddittion(String fileName) {
        File file = Utils.join(Addition_Dir, fileName);
        file.delete();
    }

    /////add version of file from last commit to Staged/removal dir
    public void StageForRemoval(String fileName, String hash) {
        File file = Utils.join(Removal_Dir, fileName);
        Utils.writeContents(file, hash);
    }

    public boolean unstageForRemoval(String fileName) {
        File file = Utils.join(Removal_Dir, fileName);

        if (file == null) return false;
        return file.delete();

    }


    public boolean CheckFileStagedForAddition(String fileName) {
        File file = Utils.join(Addition_Dir, fileName);
        return file.exists();
    }
    public File GetAdditionDir(){
        return this.Addition_Dir;
    }

    public void clear() {
        for (File file : GetFilesForAddition()) {
            file.delete();
        }
        for (File file : GetFilesForRemoval()) {
            file.delete();
        }
    }
}