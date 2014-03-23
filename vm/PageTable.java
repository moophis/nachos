package nachos.vm;

import nachos.machine.Interrupt;
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

    public void iterateVirtualTable() {
        Lib.debug(dbgVM, "#In iterateVirtualTable(): ");
        for (VP vp : virtualToEntry.keySet()) {
            Lib.debug(dbgVM, vp + "->" + virtualToEntry.get(vp).toString());
        }
    }

    public void iteratePhysicalTable() {
//        Lib.debug(dbgVM, "#In iteratePhysicalTable(): ");
//        for (int ppn : physicalToEntry.keySet()) {
//            Lib.debug(dbgVM, ppn + "->" + physicalToEntry.get(ppn).toString());
//        }
    }

    /**
     * Get PIDEntry from virtual memory.
     *
     * @param vpn - virtual memory page number.
     * @param pid - the associated process ID.
     */
    public PIDEntry getEntryFromVirtual(int vpn, int pid) {
        Lib.debug(dbgPT, "#In getEntryFromVirtual(): vpn = " + vpn
                         + " pid = " + pid);
        PIDEntry ret = null;

//        pageLock.acquire();
        VP vp = new VP(vpn, pid);

        if (virtualToEntry.containsKey(vp)) {
            Lib.debug(dbgPT, "\t#Find key " + vp);
            ret = virtualToEntry.get(vp);
        } else {
            Lib.debug(dbgPT, "\t#Cannot find such entry");
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
        Lib.debug(dbgPT, "#In setEntryToVirtual(): vpn = " + vpn
                + " pid = " + pid);
//        pageLock.acquire();
        if (entry != null) {
            virtualToEntry.put(new VP(vpn, pid), entry);
        }
//        pageLock.release();
    }

    /**
     * Delete PIDEntry associated with the virtual page.
     * Note: this is used when the virtual page is evicted
     * from the physical memory.
     *
     * @param vpn - virtual memory page number.
     * @param pid - the associated process ID.
     *
     * @return removed PIDEntry.
     */
    public PIDEntry unsetVirtualToEntry(int vpn, int pid) {
        Lib.debug(dbgPT, "#In VirtualToEntry(): vpn = " + vpn
                + " pid = " + pid);
//        pageLock.acquire();
        VP t = new VP(vpn, pid);
        if (virtualToEntry.containsKey(t)) {
            Lib.debug(dbgPT, "\tContains key...");
            return virtualToEntry.remove(t);
        }
//        pageLock.release();
        return null;
    }

    /**
     * Get PIDEntry from physical memory.
     *
     * @param ppn - physical memory page number.
     */
    public PIDEntry getEntryFromPhysical(int ppn) {
        Lib.debug(dbgPT, "#In getEntryFromPhysical(): ppn = " + ppn);
        PIDEntry ret = null;

//        pageLock.acquire();
        if (physicalToEntry.containsKey(ppn)) {
            ret = physicalToEntry.get(ppn);
        }
//        pageLock.release();

        return ret;
    }

    /**
     * Associate PIDEntry with physical memory.
     *
     * @param ppn - virtual memory page number.
     * @param entry - the Translation entry with process ID.
     */
    public void setPhysicalToEntry(int ppn, PIDEntry entry) {
        Lib.debug(dbgPT, "#In setPhysicalToEntry(): ppn = " + ppn);
//        pageLock.acquire();
        physicalToEntry.put(ppn, entry);
//        pageLock.release();
    }

    /**
     * Delete PIDEntry associated with the physical page.
     * Note: this is used when the associated virtual page
     * is evicted from the physical memory.
     *
     * @param ppn - physical memory page number.
     * @param pid - the associated process ID.
     *
     * @return removed PIDEntry.
     */
    public PIDEntry unsetPhysicalToEntry(int ppn, int pid) {
        Lib.debug(dbgPT, "#In unsetPhysicalToEntry(): ppn = " + ppn
                + " pid = " + pid);
//        pageLock.acquire();
        if (physicalToEntry.containsKey(ppn)) {
            Lib.debug(dbgPT, "\tContains key...");
            return physicalToEntry.remove(ppn);
        }
//        pageLock.release();
        return null;
    }

    /**
     * Set an entry into both virtual table and physical table.
     *
     * @param vpn - virtual memory page number.
     * @param pid - the associated process ID.
     * @param te  - the associated translation entry.
     */
    public void set(int vpn, int pid, TranslationEntry te) {
        if (vpn < 0 || pid < 0 || te == null)
            return;
        Lib.debug(dbgPT, "**In set(): vpn = " + vpn + ", pid = " + pid
                    + ", ppn = " + te.ppn);
        Lib.assertTrue(vpn == te.vpn);

        PIDEntry pe = new PIDEntry(pid, te);
        setVirtualToEntry(vpn, pid, pe);
        setPhysicalToEntry(te.ppn, pe);
    }

    /**
     * Remove an entry from both virtual table and physical table.
     *
     * @param vpn - virtual memory page number.
     * @param pid - the associated process ID.
     */
    public void remove(int vpn, int pid) {
        if (vpn < 0 || pid < 0)
            return;
        Lib.debug(dbgPT, "**In remove(): vpn = " + vpn + ", pid = " + pid);

        PIDEntry pe = unsetVirtualToEntry(vpn, pid);
        if (pe != null && pe.getEntry() != null
                && pe.getEntry().ppn >= 0) {
            Lib.assertTrue(pe.getEntry().vpn == vpn);
            Lib.assertTrue(pe.getPID() == pid);

            unsetPhysicalToEntry(pe.getEntry().ppn, pid);
        }
    }

    /**
     * Choose a victim page using clock algorithm.
     * @return the associated PIDEntry of the victim
     */
    public PIDEntry victimize() {
        Lib.debug(dbgPT, "#In victimize()");
        // TODO: clock algorithm
        while (true) {
            Iterator<Integer> it = physicalToEntry.keySet().iterator();

            if (it == null || !it.hasNext()) {
                return null;
            }

//            iterateVirtualTable();
//            iteratePhysicalTable();

            while (it.hasNext()) {
                int ppn = it.next();
                PIDEntry pe = physicalToEntry.get(ppn);
                int vpn = pe.getEntry().vpn;
                int pid = pe.getPID();
                TranslationEntry te = pe.getEntry();

                Lib.assertTrue(ppn == pe.getEntry().ppn);
                if (te.used) { // give you another chance
                    te.used = false;
                    pe.setEntry(te);

                    set(vpn, pid, te);
//                    Lib.debug(dbgVM, "\t#(vic)Physical: " + getEntryFromPhysical(ppn).toString());
//                    Lib.debug(dbgVM, "\t#(vic)Virtual: " + getEntryFromVirtual(vpn, pid).toString());
//                    iterateVirtualTable();
//                    Lib.assertTrue(!getEntryFromPhysical(ppn).getEntry().used);
//                    Lib.assertTrue(!getEntryFromVirtual(vpn, pid).getEntry().used);
                } else { // now you are dead...
                    Lib.debug(dbgVM, "\t#(vic)Choose victim " + pe);
                    return pe;
                }
            }
        }
    }

    /**
     * Choose a victim page using random algorithm.
     * @return the associated PIDEntry of the victim
     */
    public PIDEntry randVictimize() {
        int len = physicalToEntry.size();
        PIDEntry ret = null;

        do {
            int index = Lib.random(len);

            if (physicalToEntry.containsKey(index)) {
                ret = physicalToEntry.get(index);
            }
        } while (ret == null || !ret.getEntry().valid);

        return ret;
    }

    /** Inverted page table <<vpn, pid>, <pid, entry>> */
    private HashMap<VP, PIDEntry> virtualToEntry = null;

    /** Inverted core map <paddr, <pid, entry>> */
    private HashMap<Integer, PIDEntry> physicalToEntry = null;

    /** Memory lock */
    private Lock pageLock = new Lock();

    private static final char dbgPT = 'T';

    private static final char dbgVM = 'v';
}
