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
public class MemoryManager {
    private static MemoryManager ourInstance = new MemoryManager();

    public static MemoryManager getInstance() {
        return ourInstance;
    }

    private MemoryManager() {
        virtualToEntry = new HashMap<Integer, PIDEntry>();
        phyicalToEntry = new HashMap<Integer, PIDEntry>();
    }

    /**
     * Get PIDEntry from virtual memory.
     *
     * @param vpage - virtual memory page number.
     */
    public PIDEntry getEntryFromVirtual(int vpage) {
        PIDEntry ret = null;

        memLock.acquire();
        if (virtualToEntry.containsKey(vpage)) {
            ret = (PIDEntry) virtualToEntry.get(vpage);
        }
        memLock.release();

        return ret;
    }

    /**
     * Associate PIDEntry with virtual memory.
     *
     * @param vpage - virtual memory page number.
     * @param entry - the Translation entry with process ID.
     */
    public void setVirtualToEntry(int vpage, PIDEntry entry) {
        memLock.acquire();
        virtualToEntry.put(vpage, entry);
        memLock.release();
    }

    /**
     * Get PIDEntry from physical memory.
     *
     * @param ppage - physical memory page number.
     */
    public PIDEntry getEntryFromPhysical(int ppage) {
        PIDEntry ret = null;

        memLock.acquire();
        if (phyicalToEntry.containsKey(ppage)) {
            ret = (PIDEntry) phyicalToEntry.get(ppage);
        }
        memLock.release();

        return ret;
    }

    /**
     * Associate PIDEntry with physical memory.
     *
     * @param ppage - virtual memory page number.
     * @param entry - the Translation entry with process ID.
     */
    public void setPhysicalToEntry(int ppage, PIDEntry entry) {
        memLock.acquire();
        phyicalToEntry.put(ppage, entry);
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
