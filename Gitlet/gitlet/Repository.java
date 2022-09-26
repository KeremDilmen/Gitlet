package gitlet;

import java.io.File;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;

/** Class for all the functionality of the git repository for Gitlet.
 *  @author Kerem Dilmen
 */
public class Repository {

    @SuppressWarnings("unchecked")
    public Repository() {
        File head = Utils.join(_BRANCHES, "head");
        if (head.exists()) {
            _HEAD = Utils.readContentsAsString(head);
        }

        File stage = Utils.join(_STAGE, "area");
        if (stage.exists()) {
            _StageArea = Utils.readObject(stage, StagingArea.class);
        }

        File remotes = Utils.join(_REMOTES, "repos");
        if (remotes.exists()) {
            _remotes = Utils.readObject(remotes, TreeMap.class);
        }
    }

    /** Initialize a git repository. */
    public void init() {
        File init = new File(_GITLET);
        if (init.exists()) {
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
        } else {
            init.mkdirs();
            new File(_STAGE).mkdirs();
            new File(_COMMITS).mkdirs();
            new File(_BRANCHES).mkdirs();
            new File(_BLOBS).mkdirs();
            new File(_REMOTES).mkdirs();

            Commit initial = new Commit(new TreeMap<String, String>(),
                    "initial commit", null, 0, null);
            Utils.writeObject(Utils.join(_COMMITS, initial.getHash()), initial);
            Utils.writeContents(Utils.join(_BRANCHES, "head"), "master");
            Utils.writeContents(Utils.join(_BRANCHES, "master"),
                    initial.getHash());

            _StageArea = new StagingArea();
            Utils.writeObject(Utils.join(_STAGE, "area"), _StageArea);

            _remotes = new TreeMap<String, String>();
            Utils.writeObject(Utils.join(_REMOTES, "repos"), _remotes);

        }
    }

    /** Adds the copy of the file to the staging area.
     * @param file - Name of the file to be added. */
    public void add(String file) {
        File added = new File(file);
        if (!added.exists()) {
            System.out.println("File does not exist.");
        } else {
            String fileSHA = Utils.sha1(Utils.readContents(added));
            Commit curr = recentCommit();
            if (_StageArea.getRemoved().containsKey(file)) {
                _StageArea.getRemoved().remove(file);
                Utils.writeObject(Utils.join(_STAGE, "area"), _StageArea);
                return;
            }

            if (curr.getBlobs().containsKey(file)
                    && curr.getBlobs().get(file).equals(fileSHA)) {
                _StageArea.getAdded().remove(file);
                return;
            }

            Utils.writeContents(Utils.join(_BLOBS, fileSHA),
                    Utils.readContents(added));
            _StageArea.add(file, fileSHA);
            Utils.writeObject(Utils.join(_STAGE, "area"), _StageArea);
        }
    }

    /** Commits the files in the staging area with the given message.
     * @param message - Message that accompanies a commit. */
    @SuppressWarnings("unchecked")
    public void commit(String message) {
        Pattern p = Pattern.compile("\s*");
        Matcher match = p.matcher(message);
        if (_StageArea.getAdded().isEmpty()
                && _StageArea.getRemoved().isEmpty()) {
            System.out.println("No changes added to the commit.");
        } else if (match.matches()) {
            System.out.println("Please enter a commit message.");
        } else {
            Commit prev = recentCommit();
            TreeMap<String, String> newBlobs
                    = (TreeMap<String, String>) prev.getBlobs().clone();
            for (String file: _StageArea.getAdded().keySet()) {
                newBlobs.put(file, _StageArea.getAdded().get(file));
            }

            for (String file: _StageArea.getRemoved().keySet()) {
                newBlobs.remove(file);
            }

            Commit curr = new Commit(newBlobs, message,
                    prev.getHash(), getMaxNum() + 1, null);
            Utils.writeObject(Utils.join(_COMMITS, curr.getHash()), curr);
            Utils.writeContents(Utils.join(_BRANCHES, _HEAD), curr.getHash());
            _StageArea.clear();
            Utils.writeObject(Utils.join(_STAGE, "area"), _StageArea);
        }
    }

    /** Checks out a file, iff it exists,
     *  as it exists in HEAD, as it exists in a certain commit,
     *  or checks out a branch iff it exists.
     *  @param args - Arguments that accompany the checkout command.*/
    public void checkout(String[] args) {
        if (args.length == 3) {
            checkoutHelper(args[2], recentCommit());
        } else if (args.length == 4) {
            String commitID = args[1];
            String file = args[3]; boolean found = false;
            for (String currID: Utils.plainFilenamesIn(_COMMITS)) {
                if (currID.contains(commitID)) {
                    commitID = currID;
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.out.println("No commit with that id exists.");
            } else {
                Commit curr = Utils.readObject(Utils.join(_COMMITS, commitID),
                        Commit.class);
                checkoutHelper(file, curr);
            }
        } else {
            File givenBranch = Utils.join(_BRANCHES, args[1]);
            if (!givenBranch.exists()) {
                System.out.println("No such branch exists.");
                return;
            }
            String currBranchName = Utils.readContentsAsString(
                    Utils.join(_BRANCHES, "head"));
            if (currBranchName.equals(args[1])) {
                System.out.println("No need to checkout the current branch.");
                return;
            }
            String givenCommitSHA = Utils.readContentsAsString(givenBranch);
            Commit givenCommit = Utils.readObject(Utils.join(_COMMITS,
                    givenCommitSHA), Commit.class);
            Commit currCommit = recentCommit();
            TreeMap<String, String> givenBlobs = givenCommit.getBlobs();
            TreeMap<String, String> currBlobs = currCommit.getBlobs();
            TreeSet<String> untracked = getUntracked(currCommit);
            for (String file: untracked) {
                if (givenBlobs.containsKey(file)) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    return;
                }
            }
            for (String file: Utils.plainFilenamesIn(_CWD)) {
                if (!givenBlobs.containsKey(file)
                        && currBlobs.containsKey(file)) {
                    Utils.restrictedDelete(file);
                }
            }
            for (String file: givenBlobs.keySet()) {
                checkoutHelper(file, givenCommit);
            }
            _StageArea.clear();
            Utils.writeObject(Utils.join(_STAGE, "area"), _StageArea);
            Utils.writeContents(Utils.join(_BRANCHES, "head"), args[1]);
        }
    }

    /** Deletes the current version of the file if it exists
     * and restores the version in the given commit.
     * @param file - Name of the file to be checked out.
     * @param commit - Commit object that will be used for checkout. */
    private void checkoutHelper(String file, Commit commit) {
        TreeMap<String, String> blobs = commit.getBlobs();

        if (!blobs.containsKey(file)) {
            System.out.println("File does not exist in that commit.");
            return;
        }

        if (Utils.join(_CWD, file).exists()) {
            Utils.restrictedDelete(file);
        }

        byte[] contents
                = Utils.readContents(Utils.join(_BLOBS, blobs.get(file)));
        Utils.writeContents(Utils.join(_CWD, file), contents);
    }

    /** Prints information about all of the commits in the commit tree. */
    public void log() {
        Commit curr = recentCommit();
        while (true) {
            System.out.println("===");
            System.out.println("commit " + curr.getHash());
            System.out.println("Date: " + curr.getTime() + " -0800");
            System.out.println(curr.getMessage());
            System.out.println();

            if (curr.getParent() == null) {
                break;
            } else {
                curr = Utils.readObject(Utils.join(_COMMITS, curr.getParent()),
                        Commit.class);
            }
        }
    }


    public void globalLog() {
        TreeMap<Integer, String> sorter = new TreeMap<Integer, String>();

        for (String commit: Utils.plainFilenamesIn(_COMMITS)) {
            Commit curr = Utils.readObject(Utils.join(_COMMITS, commit),
                    Commit.class);
            sorter.put(curr.getNum(), curr.getHash());
        }

        for (String sha: sorter.values()) {
            Commit curr = Utils.readObject(
                    Utils.join(_COMMITS, sha), Commit.class);
            System.out.println("===");
            System.out.println("commit " + curr.getHash());
            System.out.println("Date: " + curr.getTime() + " -0800");
            System.out.println(curr.getMessage());
            System.out.println();
        }
    }


    public void remove(String file) {
        Commit curr = recentCommit();
        boolean tracked = curr.getBlobs().containsKey(file);
        boolean staged = _StageArea.getAdded().containsKey(file);

        if (!staged && !tracked) {
            System.out.println("No reason to remove the file.");
            return;
        }

        if (tracked) {
            _StageArea.remove(file, curr.getBlobs().get(file));
            Utils.restrictedDelete(file);
        }

        if (staged) {
            _StageArea.getAdded().remove(file);
        }

        Utils.writeObject(Utils.join(_STAGE, "area"), _StageArea);
    }

    public void find(String message) {
        boolean found = false;
        TreeMap<Integer, String> sorter = new TreeMap<Integer, String>();
        for (String commit: Utils.plainFilenamesIn(_COMMITS)) {
            Commit curr = Utils.readObject(Utils.join(_COMMITS,
                    commit), Commit.class);
            if (curr.getMessage().equals(message)) {
                sorter.put(curr.getNum(), curr.getHash());
                found = true;
            }
        }

        if (!found) {
            System.out.println("Found no commit with that message.");
            return;
        }

        for (String sha: sorter.values()) {
            System.out.println(sha);
        }
    }

    public void status() {
        System.out.println("=== Branches ===");
        for (String branch: Utils.plainFilenamesIn(_BRANCHES)) {
            if (branch.equals(_HEAD)) {
                System.out.println("*" + branch);
            } else if (!branch.equals("head")) {
                System.out.println(branch);
            }
        }
        System.out.println();
        Commit curr = recentCommit();
        System.out.println("=== Staged Files ===");
        for (String added: _StageArea.getAdded().keySet()) {
            System.out.println(added);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String removed: _StageArea.getRemoved().keySet()) {
            System.out.println(removed);
        }
        System.out.println();

        TreeMap<String, String> mod = new TreeMap<String, String>();
        for (String file: Utils.plainFilenamesIn(_CWD)) {
            String sha = Utils.sha1(Utils.readContents(Utils.join(_CWD, file)));
            if ((curr.getBlobs().containsKey(file)
                    && !curr.getBlobs().get(file).equals(sha)
                    && !_StageArea.getAdded().containsKey(file))
                    || (_StageArea.getAdded().containsKey(file)
                    && !_StageArea.getAdded().get(file).equals(sha))) {
                mod.put(file, " (modified)");
            }
        }
        for (String file: _StageArea.getAdded().keySet()) {
            if (!Utils.join(_CWD, file).exists()) {
                mod.put(file, " (deleted)");
            }
        }
        for (String file: curr.getBlobs().keySet()) {
            if (!_StageArea.getRemoved().containsKey(file)
                    && !Utils.join(_CWD, file).exists()) {
                mod.put(file, " (deleted)");
            }
        }
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String file: mod.keySet()) {
            System.out.println(file + mod.get(file));
        }
        System.out.println();

        System.out.println("=== Untracked Files ===");
        TreeSet<String> untracked = getUntracked(curr);
        for (String file: untracked) {
            System.out.println(file);
        }
        System.out.println();
    }

    private TreeSet<String> getUntracked(Commit curr) {
        TreeSet<String> untracked = new TreeSet<String>();
        for (String file: Utils.plainFilenamesIn(_CWD)) {
            if (!curr.getBlobs().containsKey(file)
                    && !_StageArea.getAdded().containsKey(file)
                    && !_StageArea.getRemoved().containsKey(file)) {
                untracked.add(file);
            }
        }
        return untracked;
    }

    public void branch(String name) {
        File newBranch = Utils.join(_BRANCHES, name);

        if (newBranch.exists()) {
            System.out.println("A branch with that name already exists.");
        } else {
            Utils.writeContents(newBranch, recentCommit().getHash());
        }
    }

    public void removeBranch(String name) {
        File removed = Utils.join(_BRANCHES, name);

        if (!removed.exists()) {
            System.out.println("A branch with that name does not exist.");
        } else if (_HEAD.equals(name)) {
            System.out.println("Cannot remove the current branch.");
        } else {
            removed.delete();
        }
    }

    public void reset(String commitID) {
        if (!Utils.plainFilenamesIn(_COMMITS).contains(commitID)) {
            System.out.println("No commit with that id exists.");
            return;
        }

        Commit resetCommit = Utils.readObject(Utils.join(_COMMITS,
                commitID), Commit.class);

        Commit curr = recentCommit();

        List<String> files = Utils.plainFilenamesIn(_CWD);

        for (String file: files) {
            if (resetCommit.getBlobs().containsKey(file)
                    && !curr.getBlobs().containsKey(file)) {
                System.out.println("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
                return;
            }
        }

        for (String file: files) {
            if (!resetCommit.getBlobs().containsKey(file)
                    && curr.getBlobs().containsKey(file)) {
                Utils.restrictedDelete(file);
            }
        }

        for (String file: resetCommit.getBlobs().keySet()) {
            checkoutHelper(file, resetCommit);
        }

        _StageArea.clear();

        Utils.writeObject(Utils.join(_STAGE, "area"), _StageArea);
        Utils.writeContents(Utils.join(_BRANCHES, _HEAD), commitID);

    }

    public void merge(String branch) {
        if (!checkInput(branch)) {
            return;
        }
        String brCommitSHA = Utils.readContentsAsString(
                Utils.join(_BRANCHES, branch));
        Commit brCommit = Utils.readObject(Utils.join(_COMMITS,
                brCommitSHA), Commit.class);
        Commit currCommit = recentCommit();

        for (String file: Utils.plainFilenamesIn(_CWD)) {
            if (!currCommit.getBlobs().containsKey(file)
                && brCommit.getBlobs().containsKey(file)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }

        Commit split = findSplit(currCommit, brCommit);
        if (!checkSplit(split, brCommit, currCommit, branch)) {
            return;
        }

        TreeMap<String, String> brBlobs = brCommit.getBlobs();
        TreeMap<String, String> currBlobs = currCommit.getBlobs();
        TreeMap<String, String> splitBlobs = split.getBlobs();
        boolean conflict = false;

        for (String file: currCommit.getBlobs().keySet()) {
            String brSHA = brBlobs.get(file);
            String currSHA = currBlobs.get(file);
            String splitSHA = splitBlobs.get(file);

            if (splitSHA != null && brSHA != null) {
                if (splitSHA.equals(currSHA) && !splitSHA.equals(brSHA)) {
                    checkout(new String[] {"checkout",
                            brCommit.getHash(), "--", file});
                    add(file);
                } else if (!splitSHA.equals(currSHA) && !splitSHA.equals(brSHA)
                        && !brSHA.equals(currSHA)) {
                    conflict = true;
                    conflictHelper(file, currSHA, brSHA);
                }
            } else if ((brSHA == null && splitSHA != null
                    && !splitSHA.equals(currSHA))
                    || (splitSHA == null && brSHA != null
                    && !brSHA.equals(currSHA))) {
                conflict = true;
                conflictHelper(file, currSHA, brSHA);
            }
        }

        mergeHelper(brBlobs, splitBlobs, currBlobs, brCommit);
        mergeCommit("Merged " + branch + " into " + _HEAD + ".",
                currCommit.getHash(), brCommit.getHash());
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    private boolean checkSplit(Commit split, Commit brCommit,
                               Commit currCommit, String branch) {
        if (split.getHash().equals(brCommit.getHash())) {
            System.out.println("Given branch is an "
                    + "ancestor of the current branch.");
            return false;
        } else if (split.getHash().equals(currCommit.getHash())) {
            checkout(new String[] {"checkout", branch});
            System.out.println("Current branch fast-forwarded.");
            return false;
        }

        return true;
    }

    private void mergeHelper(TreeMap<String, String> brBlobs,
                             TreeMap<String, String> splitBlobs,
                             TreeMap<String, String> currBlobs,
                             Commit brCommit) {
        for (String brFile: brBlobs.keySet()) {
            if (!splitBlobs.containsKey(brFile)
                    && !currBlobs.containsKey(brFile)) {
                checkout(new String[] {"checkout",
                        brCommit.getHash(), "--", brFile});
                add(brFile);
            }
        }

        for (String spFile: splitBlobs.keySet()) {
            if (currBlobs.containsKey(spFile)
                    && currBlobs.get(spFile).equals(splitBlobs.get(spFile))
                    && !brBlobs.containsKey(spFile)) {
                remove(spFile);
            }
        }
    }

    private void conflictHelper(String file, String currSHA, String brSHA) {
        if (brSHA == null) {
            Utils.writeContents(Utils.join(_CWD, file), "<<<<<<< HEAD\n"
                    + Utils.readContentsAsString(Utils.join(_BLOBS, currSHA))
                    + "=======\n" + ">>>>>>>\n");
            add(file);
            return;
        }

        Utils.writeContents(Utils.join(_CWD, file), "<<<<<<< HEAD\n"
                + Utils.readContentsAsString(Utils.join(_BLOBS, currSHA))
                + "=======\n"
                + Utils.readContentsAsString(Utils.join(_BLOBS, brSHA))
                + ">>>>>>>\n");
        add(file);
    }

    @SuppressWarnings("unchecked")
    private void mergeCommit(String message, String currSHA, String brSHA) {
        Commit prev = recentCommit();
        TreeMap<String, String> newBlobs
                = (TreeMap<String, String>) prev.getBlobs().clone();
        for (String file: _StageArea.getAdded().keySet()) {
            newBlobs.put(file, _StageArea.getAdded().get(file));
        }

        for (String file: _StageArea.getRemoved().keySet()) {
            newBlobs.remove(file);
        }



        Commit curr = new Commit(newBlobs, message,
                currSHA, getMaxNum() + 1, brSHA);
        Utils.writeObject(Utils.join(_COMMITS, curr.getHash()), curr);
        Utils.writeContents(Utils.join(_BRANCHES, _HEAD), curr.getHash());
        _StageArea.clear();
        Utils.writeObject(Utils.join(_STAGE, "area"), _StageArea);
    }

    private Commit findSplit(Commit currCommit, Commit brCommit) {
        Commit split = null;
        TreeMap<String, Commit> commitTree = new TreeMap<String, Commit>();

        Commit tempC1 = currCommit;
        int distance1 = 0;
        while (true) {
            commitTree.put(tempC1.getHash(), tempC1);
            if (tempC1.getParent() == null) {
                break;
            } else {
                tempC1 = Utils.readObject(Utils.join(_COMMITS,
                        tempC1.getParent()), Commit.class);
            }
        }


        Commit tempBr = brCommit;
        while (true) {
            String branchSHA = tempBr.getHash();
            if (commitTree.containsKey(branchSHA)) {
                split = commitTree.get(branchSHA);
                break;
            } else if (tempBr.getParent() == null) {
                break;
            } else if (tempBr.getParent2() != null) {
                tempBr = Utils.readObject(Utils.join(_COMMITS,
                        tempBr.getParent2()), Commit.class);
            } else {
                tempBr = Utils.readObject(Utils.join(_COMMITS,
                        tempBr.getParent()), Commit.class);
            }
        }

        Commit tempC2 = currCommit;
        while (true) {
            commitTree.put(tempC1.getHash(), tempC2);
            if (tempC2.getHash().equals(split.getHash())) {
                break;
            } else {
                distance1++;
                tempC2 = Utils.readObject(Utils.join(_COMMITS,
                        tempC2.getParent()), Commit.class);
            }
        }

        Commit possibleSplit = splitContinued(distance1, brCommit, currCommit);
        if (possibleSplit != null) {
            split = possibleSplit;
        }
        return split;
    }

    private Commit splitContinued(int distance1,
                                  Commit brCommit, Commit currCommit) {
        TreeMap<String, Commit> commitTree = new TreeMap<String, Commit>();
        int distance2 = 0;
        Commit split = null;

        Commit tempBr2 = brCommit;
        while (true) {
            commitTree.put(tempBr2.getHash(), tempBr2);
            if (tempBr2.getParent() == null) {
                break;
            } else {
                tempBr2 = Utils.readObject(Utils.join(_COMMITS,
                        tempBr2.getParent()), Commit.class);
            }
        }

        Commit tempC3 = currCommit;
        while (true) {
            String cSHA = tempC3.getHash();
            if (commitTree.containsKey(cSHA) && distance2 < distance1) {
                split = commitTree.get(cSHA);
                break;
            } else if (tempC3.getParent() == null) {
                break;
            } else if (tempC3.getParent2() != null) {
                tempC3 = Utils.readObject(Utils.join(_COMMITS,
                        tempC3.getParent2()), Commit.class);
                distance2++;
            } else {
                tempC3 = Utils.readObject(Utils.join(_COMMITS,
                        tempC3.getParent()), Commit.class);
                distance2++;
            }
        }

        return split;
    }

    private boolean checkInput(String branch) {
        if (!_StageArea.getAdded().isEmpty()
                || !_StageArea.getRemoved().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return false;
        } else if (!Utils.join(_BRANCHES, branch).exists()) {
            System.out.println("A branch with that name does not exist.");
            return false;
        } else if (_HEAD.equals(branch)) {
            System.out.println("Cannot merge a branch with itself.");
            return false;
        }

        return true;
    }

    /** Returns the commit that HEAD points to. */
    public Commit recentCommit() {
        String commitHash
                = Utils.readContentsAsString(Utils.join(_BRANCHES, _HEAD));
        return Utils.readObject(Utils.join(_COMMITS, commitHash), Commit.class);
    }

    public int getMaxNum() {
        int max = 0;

        for (String file: Utils.plainFilenamesIn(_BRANCHES)) {
            if (!file.equals("head")) {
                String sha = Utils.readContentsAsString(
                        Utils.join(_BRANCHES, file));
                Commit curr = Utils.readObject(Utils.join(_COMMITS, sha),
                        Commit.class);
                int currNum = curr.getNum();
                if (currNum > max) {
                    max = currNum;
                }
            }
        }

        return max;
    }


    /** Path to current working directory. */
    private String _CWD = "./";

    /** Path to .gitlet directory. */
    private String _GITLET = "./.gitlet/";

    /** Path to stage directory. */
    private String _STAGE = "./.gitlet/stage/";

    /** Path to commits directory. */
    private String _COMMITS = "./.gitlet/commits/";

    /** Path to blobs directory. */
    private String _BLOBS = "./.gitlet/blobs/";

    /** Path to branches directory. */
    private String _BRANCHES = "./.gitlet/branches/";

    /** HEAD pointer that keep track of the latest commit
     * on the current branch. */
    private String _HEAD = "master";

    /** Staging area of this repository. */
    private StagingArea _StageArea;


    public void addRemote(String name, String path) {
        if (_remotes.containsKey(name)) {
            System.out.println("A remote with that name already exists.");
            return;
        }

        _remotes.put(name, path);
        Utils.writeObject(Utils.join(_REMOTES, "repos"), _remotes);
        Utils.join(_BRANCHES, name).mkdir();
    }

    public void rmRemote(String name) {
        if (!_remotes.containsKey(name)) {
            System.out.println("A remote with that name does not exist.");
            return;
        }

        _remotes.remove(name);
        Utils.writeObject(Utils.join(_REMOTES, "repos"), _remotes);
        Utils.join(_BRANCHES, name).delete();
    }

    public void fetch(String rName, String bName) {
        File newBranch = Utils.join(_BRANCHES, rName + "/" + bName);
        String rPath = _remotes.get(rName);
        File rBranch = Utils.join(rPath, "branches/" + bName);
        if (!(new File(rPath).exists())) {
            System.out.println("Remote directory not found.");
            return;
        }

        if (!rBranch.exists()) {
            System.out.println("That remote does not have that branch.");
            return;
        }

        String rCommitHash = Utils.readContentsAsString(Utils.join(rBranch));
        Utils.writeContents(newBranch, rCommitHash);

        TreeSet<String> currBlobs = new TreeSet<String>();
        currBlobs.addAll(Utils.plainFilenamesIn(_BLOBS));

        for (String rFile: Utils.plainFilenamesIn(Utils.join(rPath, "blobs"))) {
            if (!currBlobs.contains(rFile)) {
                Utils.writeContents(Utils.join(_BLOBS, rFile),
                        Utils.readContents(
                                Utils.join(rPath, "blobs/" + rFile)));
            }
        }

        TreeSet<String> currCommits = new TreeSet<String>();
        currCommits.addAll(Utils.plainFilenamesIn(_COMMITS));

        for (String rCommit: Utils.plainFilenamesIn(
                Utils.join(rPath, "commits"))) {
            if (!currCommits.contains(rCommit)) {
                Utils.writeObject(Utils.join(_COMMITS, rCommit),
                        Utils.readObject(Utils.join(rPath, "commits/"
                                + rCommit), Commit.class));
            }
        }
    }

    public void push(String rName, String bName) {
        String rPath = _remotes.get(rName);
        if (!(new File(rPath).exists())) {
            System.out.println("Remote directory not found.");
            return;
        }
        File rBranch = Utils.join(rPath, "branches/" + bName);
        String rCommitHash = Utils.readContentsAsString(Utils.join(rBranch));
        Commit rCommit = Utils.readObject(
                Utils.join(rPath, "commits/" + rCommitHash),
                Commit.class);
        Commit curr = recentCommit();
        boolean found = false;
        TreeMap<Integer, Commit> history = new TreeMap<Integer, Commit>();

        int i = 0;
        Commit temp = curr;
        while (true) {
            history.put(i, temp);
            if (temp.getParent() == null) {
                break;
            } else if (temp.getParent().equals(rCommitHash)) {
                found = true;
                break;
            } else {
                temp = Utils.readObject(Utils.join(
                        _COMMITS, temp.getParent()), Commit.class);
                i++;
            }
        }

        if (!found) {
            System.out.println("Please pull down "
                    + "remote changes before pushing.");
            return;
        }

        String bHash = Utils.readContentsAsString(
                Utils.join(rPath, "branches/" + bName));
        Commit parent = Utils.readObject(Utils.join(rPath,
                "commits/" + bHash), Commit.class);
        for (int k = i; k >= 0; k--) {
            temp = history.get(k);
            temp.setParent(parent.getHash());
            Utils.writeObject(Utils.join(
                    rPath, "commits/" + temp.getHash()), temp);
            parent = temp;
            if (k == 0) {
                Utils.writeContents(Utils.join(
                        rPath, "branches/" + bName), temp.getHash());
            }

        }
    }

    private int getRemoteMax(String rPath) {
        int max = 0;

        for (String file: Utils.plainFilenamesIn(
                Utils.join(rPath, "branches"))) {
            if (!file.equals("head")) {
                String sha = Utils.readContentsAsString(
                        Utils.join(rPath, "branches/" + file));
                Commit curr = Utils.readObject(
                        Utils.join(rPath, "commits/" + sha),
                        Commit.class);
                int currNum = curr.getNum();
                if (currNum > max) {
                    max = currNum;
                }
            }
        }

        return max;
    }

    public void pull(String rName, String bName) {
        fetch(rName, bName);
        merge(rName + "/" + bName);
    }









    /** TreeMap that maps remote names to their paths. */
    private TreeMap<String, String> _remotes;

    /** Path to remotes directory. */
    private String _REMOTES = "./.gitlet/remotes/";
}
