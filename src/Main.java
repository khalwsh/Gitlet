
public class Main {
    public static void main(String[] args) {


        if (args.length == 0) {
            Utils.exitWithMessage("Please Enter command.");
        }
        String command =args[0];

        Repository repository = new Repository(System.getProperty("user.dir"));


        switch (command) {
            case "init":
                ValidateArgs(0, args.length - 1);

                repository.init();

                break;
            case "add":
                ValidateArgs(1, args.length - 1);
                repository.add(args[1]);
                break;


            case "commit":
                ValidateArgs(1, args.length - 1);
                repository.commit(args[1]);
                break;


            case "log":
                ValidateArgs(0, args.length - 1);
                repository.log();
                break;
            case "log-all":
                  ValidateArgs(0, args.length-1);
                  repository.logAll();
                  break;
            case "global-log":
                ValidateArgs(0, args.length - 1);
                repository.globallog();
                break;

            case "find":
                ValidateArgs(1, args.length - 1);
                repository.find(args[1]);
                break;
            case "rm":
                ValidateArgs(1, args.length - 1);
                repository.rm(args[1]);
                break;

            case "status":
                ValidateArgs(0, args.length - 1);
                repository.status();
                break;

            case "checkout":

                if (args.length == 3 && args[1].equals("--")) {
                    //java gitlet.Main checkout -- [file name]
                    repository.CheckOutFile(args[2]);
                    break;
                } else if (args.length == 4 && args[2].equals("--")) {
                    //java gitlet.Main checkout [commit id] -- [file name]
                    repository.CheckOutFileByHash(args[1], args[3]);
                    break;
                } else if (args.length == 2) {
                    //java gitlet.Main checkout [branch name]
                    repository.CheckOutBranch(args[1]);
                    break;
                } else {
                    Utils.exitWithMessage("Incorrect Operands");
                }
            case "branch":
                ValidateArgs(1, args.length - 1);
                repository.branch(args[1]);
                break;

            case "rm-branch":
                ValidateArgs(1, args.length - 1);
                repository.rmbranch(args[1]);
                break;
            case "merge":
                ValidateArgs(1, args.length - 1);
                repository.merge(args[1],null);
                break;
            case "reset":
                ValidateArgs(1, args.length - 1);
                repository.reset(args[1]);
                break;
            case "add-remote":

                ValidateArgs(2, args.length - 1);
                repository.addRemote(args[1], args[2]);
                break;
            case "rm-remote":
                ValidateArgs(1, args.length - 1);
                repository.removeRemote(args[1]);
                break;
            case "push":
                ValidateArgs(2, args.length - 1);
                repository.push(args[1], args[2]);
                break;
            case "fetch":
                ValidateArgs(2, args.length - 1);
                repository.fetch(args[1],args[2]);
                break;
            case "pull":
                ValidateArgs(2, args.length-1);
                repository.pull(args[1],args[2]);
           
            default:
                break;
        }

    }

    private static void ValidateArgs(int n, int argsLength) {
        if (argsLength != n) Utils.exitWithMessage("Incorrect Operands");
    }

}