package nachos.vm;

import nachos.machine.Lib;
import nachos.machine.OpenFile;
import nachos.machine.Processor;
import nachos.machine.TranslationEntry;
import nachos.threads.Lock;
import nachos.threads.ThreadedKernel;
import nachos.vm.*;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * The Swap file data structure and basic operations.
 *
 * Created by liqiangw on 3/9/14.
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
        swapFile.close();
        ThreadedKernel.fileSystem.remove(swapFileName);
    }

    /**
     * Allocate one page in swap file.
     *
     * @return the index of the allocated page in pages.
     */
    private int allocPage() {
        //Check if there are any free slots
        if (freeSlots.size() != 0)
        {
            //Remove the first free slot and return index
            return freeSlots.removeFirst();
        }
        //No free slots found, so append one
        int swapSize = getSwapSize();
        setSwapSize(swapSize + 1);

        return swapSize;
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
        if (buf == null || offset < 0 || vpn < 0 || pid < 0) {
            return -1;
        }

        VP targetVP = new VP(vpn, pid);

        swapLock.acquire();
        if (indexMap.containsKey(targetVP))
        {
            int pageIndex = indexMap.get(targetVP);

            swapLock.release();
            return swapFile.read(pageIndex, buf, offset, pageSize);
        }

        swapLock.release();
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
     * @return the size in byte it actually writes, -1 on error (0 when
     *         the page is clean).
     */
    public int writePage(byte[] buf, int offset, int vpn, int pid) {
        if (buf == null || offset < 0 || vpn < 0 || pid < 0) {
            return -1;
        }

        VP targetVP = new VP(vpn, pid);

        swapLock.acquire();
        if (indexMap.containsKey(targetVP)) {
            Lib.assertTrue(entryMap.containsKey(targetVP));
            int pageIndex = indexMap.get(targetVP);
            int byteWritten = swapFile.write(pageIndex, buf, offset, pageSize);

            swapLock.release();
            return byteWritten;
        } else {
            // first time that the page is swapped out
            int pageIndex = allocPage();
            Lib.assertTrue(pageIndex >= 0);

            indexMap.put(targetVP, pageIndex);
            entryMap.put(targetVP, PageTable.getInstance().getEntryFromVirtual(vpn, pid));

            int byteWritten = swapFile.write(pageIndex, buf, offset, pageSize);

            swapLock.release();
            return byteWritten;
        }
    }

    /**
     * Find out whether the given page is in the swap file.
     *
     * @param vpn - the virtual page number.
     * @param pid - the associated process ID.
     *
     * @return PIDEntry if exists, null otherwise.
     */
    public PIDEntry findEntryInSwap(int vpn, int pid) {
        //Create an object for input pair
        VP targetVP = new VP(vpn, pid);

        //If the pair exists, return it
        swapLock.acquire();
        if (indexMap.containsKey(targetVP)) {
            PIDEntry pe = entryMap.get(targetVP);
            swapLock.release();
            return pe;
        }

        swapLock.release();
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
