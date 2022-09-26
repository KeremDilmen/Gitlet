package gitlet;
import java.io.File;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Kerem Dilmen
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
        } else if (!args[0].equals("init")
                && !(new File("./.gitlet").exists())) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else {
            Repository repo = new Repository();
            switch (args[0]) {
            case "init":
                if (checkArgs(args.length, 1)) {
                    repo.init();
                }
                break;
            case "add":
                if (checkArgs(args.length, 2)) {
                    repo.add(args[1]);
                }
                break;
            case "commit":
                if (checkArgs(args.length, 2)) {
                    repo.commit(args[1]);
                }
                break;
            case "rm":
                if (checkArgs(args.length, 2)) {
                    repo.remove(args[1]);
                }
                break;
            case "log":
                if (checkArgs(args.length, 1)) {
                    repo.log();
                }
                break;
            case "checkout":
                if (checkCheckout(args)) {
                    repo.checkout(args);
                }
                break;
            case "global-log":
                if (checkArgs(args.length, 1)) {
                    repo.globalLog();
                }
                break;
            case "find":
                if (checkArgs(args.length, 2)) {
                    repo.find(args[1]);
                }
                break;
            default:
                continued(args, repo);
            }
        }

        System.exit(0);
    }

    static void continued(String[] args, Repository repo) {
        switch (args[0]) {
        case "status":
            if (checkArgs(args.length, 1)) {
                repo.status();
            }
            break;
        case "branch":
            if (checkArgs(args.length, 2)) {
                repo.branch(args[1]);
            }
            break;
        case "rm-branch":
            if (checkArgs(args.length, 2)) {
                repo.removeBranch(args[1]);
            }
            break;
        case "reset":
            if (checkArgs(args.length, 2)) {
                repo.reset(args[1]);
            }
            break;
        case "merge":
            if (checkArgs(args.length, 2)) {
                repo.merge(args[1]);
            }
            break;
        case "add-remote":
            if (checkArgs(args.length, 3)) {
                repo.addRemote(args[1], args[2]);
            }
            break;
        case "rm-remote":
            if (checkArgs(args.length, 2)) {
                repo.rmRemote(args[1]);
            }
            break;
        case "fetch":
            if (checkArgs(args.length, 3)) {
                repo.fetch(args[1], args[2]);
            }
            break;
        case "push":
            if (checkArgs(args.length, 3)) {
                repo.push(args[1], args[2]);
            }
            break;
        case "pull":
            if (checkArgs(args.length, 3)) {
                repo.pull(args[1], args[2]);
            }
            break;
        default:
            System.out.println("No command with that name exists.");
        }
    }

    static boolean checkArgs(int actual, int expected) {
        if (expected == actual) {
            return true;
        } else {
            System.out.println("Incorrect operands.");
            return false;
        }
    }

    static boolean checkCheckout(String[] args) {
        if (args.length == 2
                || (args.length == 3 && args[1].equals("--"))
                || (args.length == 4 && args[2].equals("--"))) {
            return true;
        } else {
            System.out.println("Incorrect operands.");
            return false;
        }
    }

}
