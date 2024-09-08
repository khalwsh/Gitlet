
import java.io.File;
public class RemoteStore {
    private final File Remote_Dir;

    public RemoteStore(File Remote_Dir) {
        this.Remote_Dir = Remote_Dir;
    }

    //just create new file with remote name and path to it
    public void addRemotePath(String remoteName, String remotePath) {
        File reomteFile = Utils.join(Remote_Dir, remoteName);
        if (reomteFile.exists()) Utils.exitWithMessage("A remote with that name already exists.");


        Utils.writeContents(reomteFile, remotePath);
        File test = Utils.join(remotePath, "testfromlocal");
        Utils.writeContents(test, "hello from local");

    }

    public void removeRemotePath(String remoteName) {
        File reomteFile = Utils.join(Remote_Dir, remoteName);
        if (!reomteFile.exists()) Utils.exitWithMessage("A remote with that name does not exist.");
        reomteFile.delete();
    }

    public String getRemotePath(String remoteName) {
        File reomteFile = Utils.join(Remote_Dir, remoteName);
        return Utils.readContentsAsString(reomteFile);
    }
}