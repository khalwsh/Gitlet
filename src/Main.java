import java.util.*;
public class Main {
    public static void main(String[] args) {


        if (args.length == 0) {
            Utils.exitWithMessage("Please Enter command.");
        }
        String command = args[0], message, fileName, branchName;

        Repository repository = new Repository(System.getProperty("user.dir"));

<<<<<<< HEAD
        break;
        
        case "add":
        
        ValidateArgs(1, args.length-1);
        repository.add(args[1]);
        
        break;
        
        case "commit":
         ValidateArgs(1, args.length-1);
         repository.commit(args[1]);
         break;
        
        
        
        case "rm":
         
        ValidateArgs(1, args.length-1);
        repository.rm(args[1]);
        
        break;
        
        
        case "log":
        ValidateArgs(0, args.length-1);
         repository.log();
=======
>>>>>>> 963f9d64deea4867bac58f150fe2dc01e3cd2294

        switch (command) {
            case "init":
                ValidateArgs(0, args.length - 1);

                repository.init();

<<<<<<< HEAD
         break;
         
         case "checkout":
         
               if(args.length==3 &&args[1].equals("--"))
               {
                        //java gitlet.Main checkout -- [file name]
                        repository.CheckOutFile(args[2]);
                           break;
               }
               else if(args.length==4 &&args[2].equals("--"))
               {
                         //java gitlet.Main checkout [commit id] -- [file name]
                         repository.CheckOutFileByHash(args[1], args[3]);
                         break;
               }
               else if(args.length==2)
               {
                   //java gitlet.Main checkout [branch name]
                   repository.CheckOutBranch(args[1]);
                   break;
               }
               else  {Utils.exitWithMessage("Incorrect Operands");}
        case "branch":
             ValidateArgs(1, args.length-1);
              repository.branch(args[1]);
              break;

        case "rm-branch":
               ValidateArgs(1, args.length-1);
               repository.rmbranch(args[1]);
               break;
         default:
            break;
       }
           
=======
                break;

            case "add":

                ValidateArgs(1, args.length - 1);
                repository.add(args[1]);

                break;

            case "commit":
                ValidateArgs(1, args.length - 1);
                repository.commit(args[1]);
                break;


            case "rm":

                ValidateArgs(1, args.length - 1);
                repository.rm(args[1]);

                break;


            case "log":
                ValidateArgs(0, args.length - 1);
                repository.log();

                break;

            case "global-log":
                ValidateArgs(0, args.length - 1);
                repository.globallog();

                break;

            case "checkout":

                if (args.length == 3 && args[1] == "--") {
                    //java gitlet.Main checkout -- [file name]
                    repository.CheckOutFile(args[2]);
                    break;
                } else if (args.length == 4 && args[2] == "--") {
                    //java gitlet.Main checkout [commit id] -- [file name]
                    repository.CheckOutFileByHash(args[1], args[3]);
                    break;
                } else if (args.length == 2) {
                    //java gitlet.Main checkout [branch name]
                    repository.CheckOutBranch(args[1]);
                    break;
                } else Utils.exitWithMessage("Incorrect Operands");

                break;
            case "find":
                // it takes commit message and return the hashes of all commits has this message
                ValidateArgs(1, args.length - 1);
                repository.find(args[1]);
                break;
            case "status":
                ValidateArgs(0, args.length - 1);
                repository.status();
            default:
                break;
        }

>>>>>>> 963f9d64deea4867bac58f150fe2dc01e3cd2294
    }

    private static void ValidateArgs(int n, int argsLength) {
        if (argsLength != n) Utils.exitWithMessage("Incorrect Operands");
    }
}