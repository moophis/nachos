package nachos.vm;

import nachos.machine.*;
import nachos.machine.Processor;
import nachos.threads.*;
import nachos.userprog.*;


/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public VMProcess() {
        super();
        // TODO
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
        super.saveState();
        // TODO
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
        super.restoreState();
        // TODO
    }

    /**
     * Find whether TLB contains the query translation.
     *
     * @param vpn - virtual page number.
     * @return the index of TLB, if not found return -1.
     */
    private int findEntryFromTLB(int vpn) {
        int size = Machine.processor().getTLBSize();

        for (int i = 0; i < size; i++) {
            TranslationEntry te = Machine.processor().readTLBEntry(i);

            // find the entry in TLB
            if (te != null && te.valid && te.vpn == vpn) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Transfer data from memory to buffering array.
     *
     * @see nachos.userprog.UserProcess#readVirtualMemory(int, byte[], int, int).
     */
    @Override
    public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
        Lib.assertTrue(offset >= 0 && length >= 0
                && offset + length <= data.length);
        Lib.debug(dbgProcess, "In readVirtualMemory(VM): vaddr=" + vaddr + ", byte len="
                + data.length + ", beginning offset=" + offset + ", length=" + length
                + " current pid = " + getPID());

        byte[] physicalMemory = Machine.processor().getMemory();
        int amount = 0;

        if (vaddr < 0)
            return 0;

        // virtual memory: [from, to]
        int fromPage = Processor.pageFromAddress(vaddr);
        int fromOffset = Processor.offsetFromAddress(vaddr);
        int toPage = Processor.pageFromAddress(vaddr + length - 1);
        int toOffset = Processor.offsetFromAddress(vaddr + length - 1);
        Lib.debug(dbgProcess, "\tVirtualMem Addr from (page " + fromPage + " offset "
                + fromOffset + ") to (page " + toPage + " offset " + toOffset + ")");

        for (int i = fromPage; i <= toPage; i++) {
            int tlbIndex = findEntryFromTLB(i);

            if (tlbIndex == -1) { // TLB miss
                tlbIndex = handleTLBMiss(i);

                if (tlbIndex == -1) {  // Page fault
                    // TODO: handle page fault
                    // expect to get a TLB index
                }
            }
            if (tlbIndex == -1) {
                return amount;
            }

            TranslationEntry te = Machine.processor().readTLBEntry(tlbIndex);
            if (te == null || te.valid) {
                return amount;
            }

            int ppn = te.ppn;
            int count, off;
            if (i == fromPage) {
                count = Math.min(Processor.pageSize - fromOffset, length);
                off = fromOffset;
            } else if (i == toPage) {
                count = toOffset + 1; // read [0, toOffset] from the last page
                off = 0;
            } else {
                count = Processor.pageSize;
                off = 0;
            }

            int srcPos = Processor.makeAddress(ppn, off);

            Lib.debug(dbgProcess, "\t *PhyMem Addr=" + srcPos + " data index=" + (offset + amount)
                    + " count=" + count);
            System.arraycopy(physicalMemory, srcPos, data, offset + amount, count);

            te.used = true;
            Machine.processor().writeTLBEntry(tlbIndex, te);

            amount += count;
        }

        return amount;
    }

    /**
     * Transfer data from buffering array to physical memory.
     *
     * @see nachos.userprog.UserProcess#writeVirtualMemory(int, byte[], int, int).
     */
    @Override
    public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
        Lib.assertTrue(offset >= 0 && length >= 0
                && offset + length <= data.length);
//		Lib.debug(dbgProcess, "In writeVirtualMemory: vaddr=" + vaddr + ", byte len="
//				+ data.length + ", beginning offset=" + offset + ", length=" + length
//				+ " current pid = " + getPID());

        byte[] physicalMemory = Machine.processor().getMemory();
        int amount = 0;

        if (vaddr < 0)
            return 0;

        // virtual memory: [from, to)
        int fromPage = Processor.pageFromAddress(vaddr);
        int fromOffset = Processor.offsetFromAddress(vaddr);
        int toPage = Processor.pageFromAddress(vaddr + length - 1);
        int toOffset = Processor.offsetFromAddress(vaddr + length - 1);

        for (int i = fromPage; i <= toPage; i++) {
            int tlbIndex = findEntryFromTLB(i);

            if (tlbIndex == -1) { // TLB miss
                tlbIndex = handleTLBMiss(i);

                if (tlbIndex == -1) {  // Page fault
                    // TODO: handle page fault
                    // expect to get a TLB index
                }
            }
            if (tlbIndex == -1) {
                return amount;
            }

            TranslationEntry te = Machine.processor().readTLBEntry(tlbIndex);
            if (te == null || te.valid) {
                return amount;
            }

            int ppn = te.ppn;
            int count, off;
            if (i == fromPage) {
                count = Math.min(Processor.pageSize - fromOffset, length);
                off = fromOffset;
            } else if (i == toPage) {
                count = toOffset + 1;
                off = 0;
            } else {
                count = Processor.pageSize;
                off = 0;
            }

            int dstPos = Processor.makeAddress(ppn, off);
            System.arraycopy(data, offset + amount, physicalMemory, dstPos, count);

            te.dirty = true;  // set it dirty
            te.used = true;  // set it used
            Machine.processor().writeTLBEntry(tlbIndex, te);

            amount += count;
        }

        return amount;
    }

    /**
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     *
     * @return <tt>true</tt> if successful.
     */
    protected boolean loadSections() {
        // TODO: for task 3
        return super.loadSections();
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        super.unloadSections();
    }

    /**
     * Handle TLB miss.
     *
     * @param vaddr - the virtual memory address.
     * @return - index in TLB, if TLB is handled;
     *           -1, if the miss cannot be handled, might be a page fault.
     */
    private int handleTLBMiss(int vaddr) {
        // TODO: use randomlyVictimizeTLB() to replace
        int vpn = Processor.pageFromAddress(vaddr);
        PIDEntry pe = PageTable.getInstance().getEntryFromVirtual(vpn);

        if (pe == null || pe.getEntry() == null
                || !pe.getEntry().valid)
            return -1;  // page fault

        return -1;
    }

    /**
     * Randomly choose a victim TLB entry to be replaced.
     */
    private void randomlyVictimizeTLB(TranslationEntry te) {
        int index = Lib.random(Machine.processor().getTLBSize());

        Lib.assertTrue(te != null && te.valid);
        Machine.processor().writeTLBEntry(index, te);
    }

    /**
     * Handle page fault. Note that this function can only be
     * called when <tt>handleTLBMiss()</tt> fails, not directly
     * by <tt>handleException()</tt>.
     *
     * @param vaddr - the virtual memory address.
     * @return index in TLB on success, -1 on failure.
     */
    private int handlePageFault(int vaddr) {
        // TODO
        return -1;
    }

    /**
     * Swap page from disk to physical memory.
     *
     * @param vpn - virtual memory page number.
     * @return true on success, false otherwise.
     */
    private boolean swapIn(int vpn) {
        // TODO
        return false;
    }

    /**
     * Swap page from physical memory to disk.
     *
     * @param vpn - virtual memory page number.
     * @return true on success, false otherwise.
     */
    private boolean swapOut(int vpn) {
        // TODO
        return false;
    }


    /**
     * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
     * . The <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param cause the user exception that occurred.
     */
    public void handleException(int cause) {
        Processor processor = Machine.processor();

        switch (cause) {
        case Processor.exceptionTLBMiss:
            regLock.acquire();
            int badVAddr = processor.readRegister(Processor.regBadVAddr);
            regLock.release();
            if (handleTLBMiss(badVAddr) == -1) {
                // TODO: handle page fault
            }
            break;
        default:
            super.handleException(cause);
            break;
        }
    }

    private static final int pageSize = Processor.pageSize;

    private static final char dbgProcess = 'a';

    private static final char dbgVM = 'v';

    private static Lock regLock = new Lock();
}
