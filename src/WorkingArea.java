import java.io.File;
public class WorkingArea {
    private final File Working_Dir;
    public WorkingArea(File Working_Dir)
    {
  this.Working_Dir=Working_Dir;
    }
    public File checkFileExistense(String fileName)
    {
        File targetFile=Utils.join(Working_Dir,fileName);
        return targetFile.exists()? targetFile:null;
    }
    public void removeFromCWD(String fileName)
    {
      File file=Utils.join(Working_Dir, fileName);
      if(file.exists()) file.delete();
    }
}
