package nachos.vm;

import nachos.machine.OpenFile;
import nachos.machine.Processor;
import nachos.threads.Lock;
import nachos.threads.ThreadedKernel;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * The Swap file data structure and basic operations.
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
        indexMap = new HashMap<VP, Integer>();
        entryMap = new HashMap<VP, PIDEntry>();
        freeSlots = new LinkedList<Integer>();
        swapFile = ThreadedKernel.fileSystem.open(swapFileName, true);
    }

    /**
     * Close and delete the swap file. Should be called
     * on termination.
     */
    public void close() {
        // TODO
    }

    /**
     * Allocate one page in swap file.
     *
     * @return the index of the allocated page in pages.
     */
    public int allocPage() {
        // TODO
        return -1;
    }

    /**
     * Read ONE given page in the swap file.
     *
     * @param buf - the buffer to store the bytes in.
     * @param offset - the beginning position of buffer to store.
     * @param vpn - the number of virtual page intended to read.
     * @param pid - the current process ID.
     *
     * @return the size in byte it actually reads, -1 on error.
     */
    public int readPage(byte[] buf, int offset, int vpn, int pid) {
        // TODO
        return -1;
    }

    /**
     * Write ONE given page to the swap file.
     *
     * @param buf - the buffer to get the bytes from.
     * @param offset - the offset in the buffer to start getting.
     * @param vpn - the number of virtual page intended to write.
     * @param pid - the current process ID.
     *
     * @return the size in byte it actually writes, -1 on error.
     */
    public int writePage(byte[] buf, int offset, int vpn, int pid) {
        // TODO
        return -1;
    }

    /**
     * Find out whether the given page is in the swap file.
     *
     * @param vpn - the virtual page number.
     * @param pid - the associated process ID.
     *
     * @return <vpn, pid> pair if exists, null otherwise.
     */
    public VP find(int vpn, int pid) {
        // TODO
        return null;
    }

    /**
     * Get the size of the swap file in pages.
     */
    public int getSwapSize() {
        return size;
    }

    /**
     * Change the size of the swap file in pages.
     *
     * @param newSize - The new size in pages.
     */
    public void setSwapSize(int newSize) {
        size = newSize;
    }

    /**
     * Virtual page number - PID pair.
     */
    private class VP {
        private int pid;
        private int vpn;

        public VP(int pid, int vpn) {
            this.pid = pid;
            this.vpn = vpn;
        }
    }

    /**
     * Swapped page map from VP to the beginning index of
     * the swapped file in pages.
     */
    private HashMap<VP, Integer> indexMap = null;

    /**
     * Swapped page map from VP to PIDEntry.
     */
    private HashMap<VP, PIDEntry> entryMap = null;

    /** A list of free swap file spaces. */
    private LinkedList<Integer> freeSlots = null;

    /** The size of the swap file in pages */
    private int size = 0;

    /** The file name */
    private static final String swapFileName = ".swap";

    /** The file */
    private OpenFile swapFile = null;

    /** Swap file lock */
    private Lock swapLock = new Lock();

    private static final int pageSize = Processor.pageSize;
}
