package com.gitlet;

import com.gitlet.util.FileUtils;
import com.gitlet.util.GitletExitException;

public final class Main {

    public static void main(String[] args) {
        try {
            run(args);
        } catch (GitletExitException ex) {
            System.out.println(ex.getMessage());
            System.exit(0);
        } catch (com.gitlet.util.GitletException ex) {
            System.err.println("Internal error: " + ex.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.err.println("Error: " + ex.getMessage());
            System.exit(1);
        }
    }

    public static void run(String[] args) {
        if (args.length == 0) {
            FileUtils.exitWithMessage("Please enter a command.");
        }

        Repository repository = new Repository(System.getProperty("user.dir"));
        String command = args[0];

        switch (command) {
            case "init":
                requireArgs(args, 1);
                repository.init();
                break;
            case "add":
                requireArgs(args, 2);
                repository.add(args[1]);
                break;
            case "commit":
                requireArgs(args, 2);
                repository.commit(args[1]);
                break;
            case "rm":
                requireArgs(args, 2);
                repository.rm(args[1]);
                break;
            case "log":
                requireArgs(args, 1);
                repository.log();
                break;
            case "global-log":
                requireArgs(args, 1);
                repository.globalLog();
                break;
            case "find":
                requireArgs(args, 2);
                repository.find(args[1]);
                break;
            case "status":
                requireArgs(args, 1);
                repository.status();
                break;
            case "checkout":
                handleCheckout(repository, args);
                break;
            case "branch":
                requireArgs(args, 2);
                repository.branch(args[1]);
                break;
            case "rm-branch":
                requireArgs(args, 2);
                repository.rmBranch(args[1]);
                break;
            case "reset":
                requireArgs(args, 2);
                repository.reset(args[1]);
                break;
            case "merge":
                requireArgs(args, 2);
                repository.merge(args[1], null);
                break;
            case "rebase":
                requireArgs(args, 2);
                repository.rebase(args[1]);
                break;
            case "add-remote":
                requireArgs(args, 3);
                repository.addRemote(args[1], args[2]);
                break;
            case "rm-remote":
                requireArgs(args, 2);
                repository.removeRemote(args[1]);
                break;
            case "push":
                requireArgs(args, 3);
                repository.push(args[1], args[2]);
                break;
            case "fetch":
                requireArgs(args, 3);
                repository.fetch(args[1], args[2]);
                break;
            case "pull":
                requireArgs(args, 3);
                repository.pull(args[1], args[2]);
                break;
            case "delete-repo":
                requireArgs(args, 1);
                repository.deleteRepo();
                break;
            default:
                FileUtils.exitWithMessage("No command with that name exists.");
        }
    }

    private static void handleCheckout(Repository repository, String[] args) {
        if (args.length == 3 && "--".equals(args[1])) {
            repository.checkoutFile(args[2]);
        } else if (args.length == 4 && "--".equals(args[2])) {
            repository.checkoutFileByCommit(args[1], args[3]);
        } else if (args.length == 2) {
            repository.checkoutBranch(args[1]);
        } else {
            FileUtils.exitWithMessage("Incorrect operands.");
        }
    }

    private static void requireArgs(String[] args, int expected) {
        if (args.length != expected) {
            FileUtils.exitWithMessage("Incorrect operands.");
        }
    }
}
