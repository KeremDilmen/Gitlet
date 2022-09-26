package gitlet;

import java.io.Serializable;
import java.util.TreeMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Class to store the properties of a given commit for Gitlet.
 *  @author Kerem Dilmen
 */
public class Commit implements Serializable {
    public Commit(TreeMap<String, String> blobs,
                  String message, String parent, int num, String parent2) {
        _blobs = blobs;
        _message = message;
        _parent = parent;
        _time = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("E MMM dd HH:mm:ss yyyy"));
        _hash = findHash();
        _num = num;
        _parent2 = parent2;
    }

    private String findHash() {
        return Utils.sha1((Object) Utils.serialize(this));
    }

    public String getHash() {
        return _hash;
    }

    public TreeMap<String, String> getBlobs() {
        return _blobs;
    }

    public String getMessage() {
        return _message;
    }

    public String getParent() {
        return _parent;
    }

    public String getTime() {
        return _time;
    }

    public int getNum() {
        return _num;
    }

    public String getParent2() {
        return _parent2;
    }

    public void setParent(String parent) {
        _parent = parent;
    }


    /** A map that stores a mapping of each
     * file to its corresponding SHA-1 key. */
    private TreeMap<String, String> _blobs;

    /** The message of this commit. */
    private String _message;

    /** SHA-1 key of this commit's parent. */
    private String _parent;

    /** SHA-1 key of this commit's optional second parent. */
    private String _parent2;

    /** Date and time of this commit. */
    private String _time;

    /** SHA-1 key of this commit. */
    private String _hash;

    /** Number to sort commits. */
    private int _num;
}
