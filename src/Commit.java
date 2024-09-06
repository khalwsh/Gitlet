
import java.util.*;
import java.io.Serializable;
public class Commit  implements Serializable {
    private Date timeStamp = new Date();
    private String message;
    private String secondryParentHash;
    private String parentHash;
    private String currentHash;
    private Map<String, String> trackedFiles = new TreeMap();

    public Commit(Date timeStamp, String message, String secondryParentHash, String parentHash, Map<String, String> trackedFiles) {
        this.timeStamp = timeStamp;
        this.message = message;
        this.secondryParentHash = secondryParentHash;
        this.parentHash = parentHash;
        this.trackedFiles = trackedFiles;
        this.currentHash = generateHash();
    }

    public Commit(Date timeStamp, String message) {
        this.timeStamp = timeStamp;
        this.message = message;
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

    public String getCommitHash() {
        return currentHash;
    }

    public Map<String, String> trackedFiles() {
        return trackedFiles;
    }

}
