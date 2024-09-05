package gitlet;
import java.io.Serializable;
public class Branch implements Serializable{
    private String name;
    private String referredCommitHash;
     public Branch(String name,String referredCommitHash)
     {
        this.name=name;
        this.referredCommitHash=referredCommitHash;
     }
     public String  getName()
     {
         return this.name;
     }
     public String getReferredCommitHash()
     {
              return this.referredCommitHash;
     }
}
