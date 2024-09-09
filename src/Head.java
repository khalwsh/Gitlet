
import java.io.File;
public class Head {
    private final File head_Dir;

    public Head(File Head_Dir) {
        head_Dir = Head_Dir;
    }

    public void setHead(String branchName) // set branch name at head file that head refers to
    {
        Utils.writeContents(head_Dir, branchName);
    }

    public String getHead()//get branch name that head refers to
    {
        return Utils.readContentsAsString(head_Dir);
    }

}