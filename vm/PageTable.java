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
            ret = (PIDEntry) virtualToEntry.get(vpn);
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
            ret = (PIDEntry) phyicalToEntry.get(ppn);
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
    private HashMap virtualToEntry = null;

    /** Inverted core map <paddr, <pid, entry>> */
    private HashMap phyicalToEntry = null;

    /** Memory lock */
    private Lock memLock = new Lock();

    /** Page entry with PID information */
    public class PIDEntry {
        private int pid;
        private TranslationEntry entry;

        public PIDEntry(int pid, TranslationEntry entry) {
            this.pid = pid;
            this.entry = entry;
        }

        public void setPID(int pid) {
            this.pid = pid;
        }

        public int getPID() {
            return this.pid;
        }

        public void setEntry(TranslationEntry entry) {
            this.entry = entry;
        }

        public TranslationEntry getEntry() {
            return this.entry;
        }
    }

}
