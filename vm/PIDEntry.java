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

    @Override
    public String toString() {
        return "pid = " + pid + " vpn = " + entry.vpn
                + " ppn = " + entry.ppn + " readOnly = " + entry.readOnly
                + " used = " + entry.used + " dirty = " + entry.dirty
                + " valid = " + entry.valid;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof PIDEntry)) {
            return false;
        }
        PIDEntry that = (PIDEntry) obj;

        return this.pid == that.pid &&
               this.getEntry().vpn == that.getEntry().vpn &&
               this.getEntry().ppn == that.getEntry().ppn &&
               this.getEntry().valid == that.getEntry().valid &&
               this.getEntry().dirty == that.getEntry().dirty &&
               this.getEntry().used == that.getEntry().used &&
               this.getEntry().readOnly == that.getEntry().readOnly;
    }
}
