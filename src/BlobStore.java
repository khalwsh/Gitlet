import java.io.File;

public class BlobStore {
      private final File Blobs_Dir;
      public BlobStore(File Blobs_Dir)
      {
        this.Blobs_Dir=Blobs_Dir;
      }
      public String saveBlob(File curFile)
      {
        //get content of file(comin from working area) =>calculate hash=> serialize content=>store into blob
           
        String fileContent=Utils.readContentsAsString(curFile);

        String curFileHash=Utils.sha1(fileContent);
          
          File blobFile=Utils.join(Blobs_Dir, curFileHash);
         
          Utils.writeObject(blobFile,fileContent);
          
          return curFileHash;

      }
    //   public boolean checkBlobHash(String hash)
    //   {
    //     File targetBlob=Utils.join(Blobs_Dir, hash);
    //     return targetBlob.exists();
    //   }

}
