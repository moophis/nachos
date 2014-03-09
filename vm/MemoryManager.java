package nachos.vm;

import nachos.machine.TranslationEntry;
import nachos.threads.Lock;

import java.util.Hashtable;

/**
 * This class should handle almost every operations related
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
        virtualToEntry = new Hashtable<Integer, PIDEntry>();
        phyicalToEntry = new Hashtable<Integer, PIDEntry>();
    }

    /** Inverted page table <vaddr, <pid, entry>> */
    private Hashtable virtualToEntry = null;

    /** Inverted core map <paddr, <pid, entry>> */
    private Hashtable phyicalToEntry = null;

    /** Memory lock */
    private Lock memLock = new Lock();

    /** Page entry with PID information */
    private class PIDEntry {
        private int pid;
        private TranslationEntry entry;

        public PIDEntry(int pid, TranslationEntry entry) {
            this.pid = pid;
            this.entry = entry;
        }
    }

}
