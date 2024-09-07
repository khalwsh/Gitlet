import java.io.File;
public class WorkingArea {
    private final File Working_Dir;

    public WorkingArea(File Working_Dir) {
        this.Working_Dir = Working_Dir;
    }

    public File checkFileExistense(String fileName) {
        File targetFile = Utils.join(Working_Dir, fileName);
        return targetFile.exists() ? targetFile : null;
    }

    public void removeFromCWD(String fileName) {
        File file = Utils.join(Working_Dir, fileName);
        if (file.exists()) file.delete();
    }

    public void addOrUpdateFileAtCWD(String fileName, String fileContent) {
        File file = Utils.join(Working_Dir, fileName);

        Utils.writeContents(file, fileContent);

    }
    public File[] WorkingTreeFiles(){
        return Working_Dir.listFiles(File::isFile);
      }
    public File getWorkingDir() {
        return Working_Dir;
    }
    public String[] NameOfFilesInWorkingArea(){
//        System.out.println(System.getProperty("user.dir"));
//        System.out.println(Working_Dir.list());
//        for (String fileName : Working_Dir.list()) {
//            System.out.println(fileName);
//        }
        return Working_Dir.list();
    }
}
