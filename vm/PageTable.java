package nachos.vm;

import nachos.machine.TranslationEntry;
import nachos.threads.Lock;

import java.util.HashMap;

/**
 * This class should handle most operations related
 * to virtual memory manipulations.
 *
 * Created by liqiangw on 3/8/14.
 */
public class PageTable {
    private static PageTable ourInstance = new PageTable();

    public static PageTable getInstance() {
        return ourInstance;
    }

    private PageTable() {
        virtualToEntry = new HashMap<Integer, PIDEntry>();
        phyicalToEntry = new HashMap<Integer, PIDEntry>();
    }

    /**
     * Get PIDEntry from virtual memory.
     *
     * @param vpn - virtual memory page number.
     */
    public PIDEntry getEntryFromVirtual(int vpn) {
        PIDEntry ret = null;

        memLock.acquire();
        if (virtualToEntry.containsKey(vpn)) {
            ret = virtualToEntry.get(vpn);
        }
        memLock.release();

        return ret;
    }

    /**
     * Associate PIDEntry with virtual memory.
     *
     * @param vpn - virtual memory page number.
     * @param entry - the Translation entry with process ID.
     */
    public void setVirtualToEntry(int vpn, PIDEntry entry) {
        memLock.acquire();
        virtualToEntry.put(vpn, entry);
        memLock.release();
    }

    /**
     * Get PIDEntry from physical memory.
     *
     * @param ppn - physical memory page number.
     */
    public PIDEntry getEntryFromPhysical(int ppn) {
        PIDEntry ret = null;

        memLock.acquire();
        if (phyicalToEntry.containsKey(ppn)) {
            ret = phyicalToEntry.get(ppn);
        }
        memLock.release();

        return ret;
    }

    /**
     * Associate PIDEntry with physical memory.
     *
     * @param ppn - virtual memory page number.
     * @param entry - the Translation entry with process ID.
     */
    public void setPhysicalToEntry(int ppn, PIDEntry entry) {
        memLock.acquire();
        phyicalToEntry.put(ppn, entry);
        memLock.release();
    }


    /** Inverted page table <vaddr, <pid, entry>> */
    private HashMap<Integer, PIDEntry> virtualToEntry = null;

    /** Inverted core map <paddr, <pid, entry>> */
    private HashMap<Integer, PIDEntry> phyicalToEntry = null;

    /** Memory lock */
    private Lock memLock = new Lock();
}
