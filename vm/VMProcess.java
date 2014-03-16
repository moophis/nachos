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

                if (tlbIndex == -1) {
                    // abort
                    handleExit(Processor.exceptionBusError);
                }
            }

            TranslationEntry te = Machine.processor().readTLBEntry(tlbIndex);
            Lib.assertTrue((te != null) && te.valid);

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

            te.used = true; // make it used
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

        // virtual memory: [from, to]
        int fromPage = Processor.pageFromAddress(vaddr);
        int fromOffset = Processor.offsetFromAddress(vaddr);
        int toPage = Processor.pageFromAddress(vaddr + length - 1);
        int toOffset = Processor.offsetFromAddress(vaddr + length - 1);

        for (int i = fromPage; i <= toPage; i++) {
            int tlbIndex = findEntryFromTLB(i);

            if (tlbIndex == -1) { // TLB miss
                tlbIndex = handleTLBMiss(i);

                if (tlbIndex == -1) {
                    // abort
                    handleExit(Processor.exceptionBusError);
                }
            }

            TranslationEntry te = Machine.processor().readTLBEntry(tlbIndex);
            Lib.assertTrue((te != null) && te.valid);

            // check if the read-only page is to be written.
            if (te.readOnly) {
                // abort
                handleExit(Processor.exceptionReadOnly);
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
        // TODO: unload page table entries belonging to current process.
    }

    /**
     * Handle TLB miss.
     *
     * @param vaddr - the virtual memory address.
     * @return - index in TLB, if TLB is handled;
     *           -1, if the miss cannot be handled, might be an illegal access.
     */
    private int handleTLBMiss(int vaddr) {
        int vpn = Processor.pageFromAddress(vaddr);
        int sizeTLB = Machine.processor().getTLBSize();
        int invalidIndex = -1;
        PIDEntry pe = PageTable.getInstance().getEntryFromVirtual(vpn, getPID());

        if (pe == null || pe.getEntry() == null) {
            // error case: invalid address
            return -1;
        }

        if (!pe.getEntry().valid) {
            // handle page fault
            if (!handlePageFault(vaddr))
                return -1;
        }

        for(int i = 0; i < sizeTLB; i++)
        {
            //get entry in TLB
            TranslationEntry te = Machine.processor().readTLBEntry(i);

            //check if entry is invalid
            if (te == null || !te.valid)
            {
                invalidIndex = i;
                break;
            }
        }

        //all entries in TLB were valid, choose randomly to replace
        if(invalidIndex == -1)
        {
            invalidIndex = randomlyVictimizeTLB();
        }

        // write the victim entry back to page table before replacement
        TranslationEntry tmpEntry = Machine.processor().readTLBEntry(invalidIndex);
        PageTable.getInstance().setVirtualToEntry(tmpEntry.vpn,
                    getPID(), new PIDEntry(getPID(), tmpEntry));

        Machine.processor().writeTLBEntry(invalidIndex, pe.getEntry());

        return invalidIndex;
    }

    /**
     * Randomly choose a victim TLB entry to be replaced.
     */
    private int randomlyVictimizeTLB() {
        int index = Lib.random(Machine.processor().getTLBSize());

        return index;
    }

    /**
     * Handle page fault. Note that this function can only be
     * called when <tt>handleTLBMiss()</tt> fails, not directly
     * by <tt>handleException()</tt>.
     *
     * @param vaddr - the virtual memory address.
     * @return true on success, false on failure.
     */
    private boolean handlePageFault(int vaddr) {
        int vpn = Processor.pageFromAddress(vaddr);

        return swapIn(vpn, getPID());
    }

    /**
     * Swap page from disk to physical memory.
     *
     * @param vpn - virtual memory page number.
     * @param pid - the associated process ID.
     * @return true on success, false otherwise.
     */
    private boolean swapIn(int vpn, int pid) {
        // TODO: need a check
        PageTable pt = PageTable.getInstance();
        SwapFile sf = SwapFile.getInstance();
        Lib.assertTrue(pt != null && sf != null);

        byte[] buf = new byte[pageSize];
        if (sf.readPage(buf, 0, vpn, pid) != pageSize) {
            Lib.debug(dbgVM, "\tReading page from swap failed!");
            return false;
        }

        // find free slot in main memory
        int ppn = -1;
        if (UserKernel.freePages.size() > 0) {
            ppn = UserKernel.freePages.poll();
        }
        if (ppn == -1) { // no free main memory
            if ((ppn = swapOut(nextVictimPage())) == -1) {
                Lib.debug(dbgVM, "\tswapOut() failed: no free page!");
                return false;
            }
        }

        // form a new PIDEntry
        PIDEntry pe;
        TranslationEntry te;
        if ((pe = pt.getEntryFromVirtual(vpn, pid)) == null) {
            Lib.debug(dbgVM, "\tgetting entry failed: no such entry!");
            return false;
        }

        te = pe.getEntry();
        te.ppn = ppn;
        te.vpn = vpn;
        te.valid = true;
        te.used = false;
        te.dirty = false;
        pe.setEntry(te);
        pt.setVirtualToEntry(vpn, pid, pe);  // update the <VP, PIDEntry>
        pt.setPhysicalToEntry(ppn, pe);

        int paddr = Processor.makeAddress(ppn, 0);
        byte[] physicalMemory = Machine.processor().getMemory();

        System.arraycopy(buf, 0, physicalMemory, paddr, pageSize);

        return true;
    }

    /**
     * Swap page from physical memory to disk.
     *
     * @param outEntry - the entry associated with the victim page.
     * @return page number of freed physical memory on success,
     *         -1 otherwise.
     */
    private int swapOut(PIDEntry outEntry) {
        TranslationEntry entry = outEntry.getEntry();
        int vpn = entry.vpn;
        int ppn = entry.ppn;
        int pid = outEntry.getPID();

        if (entry.dirty) {
            int paddr = Processor.makeAddress(ppn, 0);

            byte[] buf = new byte[pageSize];
            byte[] physicalMemory = Machine.processor().getMemory();

            System.arraycopy(physicalMemory, paddr, buf, 0, pageSize);

            Lib.assertTrue(SwapFile.getInstance().writePage(buf, 0, vpn, pid) == pageSize);
        }

        entry.valid = false;
        outEntry.setEntry(entry);
        PageTable.getInstance().setVirtualToEntry(vpn, pid, outEntry);

        return entry.ppn;
    }

    private PIDEntry nextVictimPage() {
        return PageTable.getInstance().victimize();
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
        case Processor.exceptionTLBMiss:  // XXX: still has lock problem
            vmLock.acquire();
            int badVAddr = processor.readRegister(Processor.regBadVAddr);

            if (handleTLBMiss(badVAddr) == -1) {
                // abort process
                vmLock.release();
                handleExit(Processor.exceptionBusError);
            }
            vmLock.release();
            break;
        default:
            super.handleException(cause);
            break;
        }
    }

    private static final int pageSize = Processor.pageSize;

    private static final char dbgProcess = 'a';

    private static final char dbgVM = 'v';

    private static Lock vmLock = new Lock();
}
