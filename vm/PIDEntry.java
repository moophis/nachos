package nachos.vm;

import nachos.machine.TranslationEntry;

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

    public String toString() {
        return "pid = " + " vpn = " + entry.vpn
                + " ppn = " + entry.ppn + " readOnly = " + entry.readOnly
                + " used = " + entry.used + " dirty = " + entry.dirty;
    }
}
