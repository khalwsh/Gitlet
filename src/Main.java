import java.util.*;
public class Main {
    public static void main(String[] args) {
     
    
        if(args.length==0)
        {
            Utils.exitWithMessage("Please Enter command.");
        }
        String command="log",message,fileName,branchName;
        
        Repository repository=new Repository(System.getProperty("user.dir"));
        

       switch (command) {
        case "init":
            ValidateArgs(0, args.length-1);
            
            repository.init();

        break;
        
        case "add":
        
        ValidateArgs(1, args.length-1);
        repository.add("file.txt");
        
        break;
        
        
        
        
        case "rm":
         
        ValidateArgs(1, args.length-1);
        repository.rm("file.txt");
        
        break;
        
        
        case "log":
        //ValidateArgs(0, args.length-1);
         repository.log();

         break;
        case"commit":
         ValidateArgs(1, args.length-1);
         repository.commit(args[1]);
         break;
         default:
            break;
       }
           
    }
    private static void ValidateArgs(int n,int argsLength)
    {
        if(argsLength!=n)  Utils.exitWithMessage("Incorrect Operands");
    }
}
