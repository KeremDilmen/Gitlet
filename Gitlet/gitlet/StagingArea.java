package gitlet;

import java.util.TreeMap;
import java.io.Serializable;

/** Class to track added and removed files for Gitlet.
 *  @author Kerem Dilmen
 */
public class StagingArea implements Serializable {
    public StagingArea() {
        clear();
    }

    /** Adds the file to the staging area along with its SHA-1 key.
     * @param file - Name of the file to be added.
     * @param sha - SHA-1 key of the file to be added. */
    public void add(String file, String sha) {
        _added.put(file, sha);
    }

    /** Adds the removed file to the staging area along with its SHA-1 key.
     * @param file - Name of the file to be removed.
     * @param sha - SHA-1 key of the file to be removed.*/
    public void remove(String file, String sha) {
        _removed.put(file, sha);
    }

    /** Returns the map of added files. */
    public TreeMap<String, String> getAdded() {
        return _added;
    }

    /** Returns the map of removed files. */
    public TreeMap<String, String> getRemoved() {
        return _removed;
    }

    /** Removes all added and removed files from the staging area. */
    public void clear() {
        _added = new TreeMap<String, String>();
        _removed = new TreeMap<String, String>();
    }

    /** A map for the added files and their SHA-1 keys. */
    private TreeMap<String, String> _added;

    /** A map to store the removed files. */
    private TreeMap<String, String> _removed;
}
