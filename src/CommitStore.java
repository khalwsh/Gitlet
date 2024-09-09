
import java.io.File;
import java.util.ArrayList;
public class CommitStore {
    private final File Commits_Dir;

    public CommitStore(File Commits_Dir) {
        this.Commits_Dir = Commits_Dir;
    }

    public File[] GetCommitFiles() {
        File[] filesList = Commits_Dir.listFiles();
        return filesList;
    }

    public void saveCommit(Commit commit) {
        File currentCommitFile = Utils.join(Commits_Dir, commit.getCommitHash());
        Utils.writeObject(currentCommitFile, commit);
    }

    public Commit getCommit(String commitHash) {
        if (commitHash == null) return null;
        File currentCommitFile = Utils.join(Commits_Dir, commitHash);

        //////java.lang.IllegalArgumentException here
        if (currentCommitFile.exists())
            return Utils.readObject(currentCommitFile, Commit.class);

        return null;

    }

    public ArrayList<Commit> getAllCommitsHistory() {
        ArrayList<Commit> listOfCommits = new ArrayList<>();
        for (File f : GetCommitFiles()) {
            listOfCommits.add(Utils.readObject(f, Commit.class));
        }
        return listOfCommits;

    }

}
