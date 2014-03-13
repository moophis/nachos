package nachos.vm;

import nachos.machine.TranslationEntry;
import nachos.threads.Lock;

import java.util.HashMap;
import java.util.Iterator;

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
        virtualToEntry = new HashMap<VP, PIDEntry>();
        physicalToEntry = new HashMap<Integer, PIDEntry>();
    }

    /**
     * Get PIDEntry from virtual memory.
     *
     * @param vpn - virtual memory page number.
     */
    public PIDEntry getEntryFromVirtual(int vpn, int pid) {
        PIDEntry ret = null;

        pageLock.acquire();
        VP vp = new VP(vpn, pid);
        if (virtualToEntry.containsKey(vp)) {
            ret = virtualToEntry.get(vp);
        }
        pageLock.release();

        return ret;
    }

    /**
     * Associate PIDEntry with virtual memory.
     *
     * @param vpn - virtual memory page number.
     * @param entry - the Translation entry with process ID.
     */
    public void setVirtualToEntry(int vpn, int pid, PIDEntry entry) {
        pageLock.acquire();
        virtualToEntry.put(new VP(vpn, pid), entry);
        pageLock.release();
    }

    /**
     * Get PIDEntry from physical memory.
     *
     * @param ppn - physical memory page number.
     */
    public PIDEntry getEntryFromPhysical(int ppn) {
        PIDEntry ret = null;

        pageLock.acquire();
        if (physicalToEntry.containsKey(ppn)) {
            ret = physicalToEntry.get(ppn);
        }
        pageLock.release();

        return ret;
    }

    /**
     * Associate PIDEntry with physical memory.
     *
     * @param ppn - virtual memory page number.
     * @param entry - the Translation entry with process ID.
     */
    public void setPhysicalToEntry(int ppn, PIDEntry entry) {
        pageLock.acquire();
        physicalToEntry.put(ppn, entry);
        pageLock.release();
    }

    /**
     * Choose a victim page using clock algorithm.
     * @return the associated PIDEntry of the victim
     */
    public PIDEntry victimize() {
        // TODO: clock algorithm
        while (true) {
            Iterator<Integer> it = physicalToEntry.keySet().iterator();
            while (it.hasNext()) {
                int ppn = it.next();
                PIDEntry pe = physicalToEntry.get(ppn);
                TranslationEntry te = pe.getEntry();

                if (te.used) { // give you another chance
                    te.used = false;
                    pe.setEntry(te);
                    physicalToEntry.put(ppn, pe);
                } else { // now you are dead...
                    return pe;
                }
            }
        }
    }

    /** Inverted page table <<vpn, pid>, <pid, entry>> */
    private HashMap<VP, PIDEntry> virtualToEntry = null;

    /** Inverted core map <paddr, <pid, entry>> */
    private HashMap<Integer, PIDEntry> physicalToEntry = null;

    /** Memory lock */
    private Lock pageLock = new Lock();
}
