package nachos.vm;

import nachos.machine.Lib;
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

    private void iterateVirtualTable() {
//        Lib.debug(dbgVM, "#In iterateVirtualTable(): ");
//        for (VP vp : virtualToEntry.keySet()) {
//            Lib.debug(dbgVM, vp + "->" + virtualToEntry.get(vp).toString());
//        }
    }

    /**
     * Get PIDEntry from virtual memory.
     *
     * @param vpn - virtual memory page number.
     * @param pid - the associated process ID.
     */
    public PIDEntry getEntryFromVirtual(int vpn, int pid) {
        Lib.debug(dbgVM, "#In getEntryFromVirtual(): vpn = " + vpn
                         + " pid = " + pid);
        PIDEntry ret = null;

        iterateVirtualTable();
//        pageLock.acquire();
        VP vp = new VP(vpn, pid);

        if (virtualToEntry.containsKey(vp)) {
            Lib.debug(dbgVM, "\t#Find key " + vp);
            ret = virtualToEntry.get(vp);
        } else {
            Lib.debug(dbgVM, "\t#Cannot find such entry");
        }
//        pageLock.release();

        return ret;
    }

    /**
     * Associate PIDEntry with virtual memory.
     *
     * @param vpn - virtual memory page number.
     * @param pid - the associated process ID.
     * @param entry - the Translation entry with process ID.
     */
    public void setVirtualToEntry(int vpn, int pid, PIDEntry entry) {
        Lib.debug(dbgVM, "#In setEntryToVirtual(): vpn = " + vpn
                + " pid = " + pid);
//        pageLock.acquire();
        if (entry != null) {
            virtualToEntry.put(new VP(vpn, pid), entry);
        }
        iterateVirtualTable();
//        pageLock.release();
    }

    /**
     * Delete PIDEntry associated with the virtual page.
     * Note: this is used when the virtual page is evicted
     * from the physical memory.
     *
     * @param vpn - virtual memory page number.
     * @param pid - the associated process ID.
     */
    public void unsetVirtualToEntry(int vpn, int pid) {
        Lib.debug(dbgVM, "#In unsetEntryFromVirtual(): vpn = " + vpn
                + " pid = " + pid);
//        pageLock.acquire();
        VP t = new VP(vpn, pid);
        if (virtualToEntry.containsKey(t)) {
            Lib.debug(dbgVM, "\tContains key...");
            virtualToEntry.remove(t);
        }
//        pageLock.release();
    }

    /**
     * Get PIDEntry from physical memory.
     *
     * @param ppn - physical memory page number.
     */
    public PIDEntry getEntryFromPhysical(int ppn) {
        Lib.debug(dbgVM, "#In getEntryFromPhysical(): ppn = " + ppn);
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
        Lib.debug(dbgVM, "#In setPhysicalToEntry(): ppn = " + ppn);
        pageLock.acquire();
        physicalToEntry.put(ppn, entry);
        pageLock.release();
    }

    /**
     * Choose a victim page using clock algorithm.
     * @return the associated PIDEntry of the victim
     */
    public PIDEntry victimize() {
        Lib.debug(dbgVM, "#In victimize()");
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

    private static final char dbgVM = 'v';
}
