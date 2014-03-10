package nachos.vm;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * The Swap file data structure.
 *
 * Created by liqiangw on 3/9/14.
 * TODO: still under construction.
 */
public class SwapFile {
    private static SwapFile ourInstance = new SwapFile();

    public static SwapFile getInstance() {
        return ourInstance;
    }

    private SwapFile() {
        indexMap = new HashMap<PIDEntry, Integer>();
        freeSlots = new LinkedList<Integer>();
    }

    /**
     * Swapped page map from PIDEntry to beginning index of
     * the swapped file.
     */
    private HashMap<PIDEntry, Integer> indexMap = null;

    /** A list of free swap file spaces. */
    private LinkedList<Integer> freeSlots = null;

    /** The file name */
    private static final String swapFile = ".swap";
}
