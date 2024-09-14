import java.util.*;
import java.io.Serializable;
public class Commit  implements Serializable {
    private static final long serialVersionUID = 1L;
    private Date timeStamp = new Date();
    private String message;
    private String secondryParentHash;
    private String parentHash;
    private String currentHash;
    private Map<String, String> trackedFiles=new TreeMap<>() ;
   
    public Commit(Date timeStamp, String message, String secondryParentHash, String parentHash, Map<String, String> trackedFiles) {

        this.timeStamp = timeStamp;
        this.message = message;
        this.secondryParentHash = secondryParentHash;
        this.parentHash = parentHash;
        this.trackedFiles = trackedFiles;
        this.currentHash = generateHash();
    }

    public Commit(Date timeStamp, String message,Map<String, String> trackedFiles) {
        this.timeStamp = timeStamp;
        this.message = message;
        this.trackedFiles=trackedFiles;
        this.currentHash = generateHash();
    }

    private String generateHash() {
        List<Object> itemList = new ArrayList<>(
                Arrays.asList(message, timeStamp.toString(), (secondryParentHash == null) ? "" : secondryParentHash, (parentHash == null) ? "" : parentHash)
        );
        for (Map.Entry<String, String> entry : trackedFiles.entrySet()) {
            itemList.add(entry.toString());
        }
        return Utils.sha1(itemList);

    }
 public Date GetTime(){
        return this.timeStamp;
    }
   
    public String getSecondryParent(){
        return secondryParentHash;
    }
    public String getCommitHash() {
        return currentHash;
    }

    public String getParentCommitHash() {
        return parentHash;
    }
    public TreeSet<String> getTrackedFilesSet() {
        return new TreeSet<>(trackedFiles.keySet());
    }
    public Map<String, String> trackedFiles() {
        return trackedFiles;
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("commit ").append(currentHash).append("\n");
        sb.append("Date: ").append(timeStamp.toString()).append("\n");
        sb.append(message).append("\n");

        // Add an empty line after the commit message
        sb.append("\n");

        return sb.toString();
    }
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Commit) {
            return ((Commit) obj).getCommitHash().equals(this.getCommitHash());
        }
        return false;
    }
    public String CommitMessage() {
        return this.message;
    }
}
