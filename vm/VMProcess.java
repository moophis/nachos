package nachos.vm;

import nachos.machine.*;
import nachos.machine.Processor;
import nachos.threads.*;
import nachos.userprog.*;

import java.util.HashMap;


/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public VMProcess() {
        super();

        secMap = new HashMap<Integer, SecInfo>();

        int tlbSize = Machine.processor().getTLBSize();
        tlbBackUp = new TranslationEntry[tlbSize];

        for (int i = 0; i < tlbSize; i++) {
            tlbBackUp[i] = new TranslationEntry(-1, -1, false, false, false, false);
        }
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
        if (getPID() != 0) {
            Lib.debug(dbgVM, "In saveState(): pid = " + getPID());
            iterateTLB();
        }

        // save TLB
        Processor proc = Machine.processor();
        PageTable pt = PageTable.getInstance();
        for (int i = 0; i < proc.getTLBSize(); i++) {
            tlbBackUp[i] = proc.readTLBEntry(i);
            if (tlbBackUp[i] != null && tlbBackUp[i].valid) {
                // write back to page table
                TranslationEntry tmpEntry = tlbBackUp[i];
//                pt.setVirtualToEntry(tmpEntry.vpn,
//                        getPID(), new PIDEntry(getPID(), tmpEntry));
//                pt.setPhysicalToEntry(tmpEntry.ppn,
//                        new PIDEntry(getPID(), tmpEntry));
                Lib.debug(dbgVM, "\tsaveState(): set()-> vpn = " + tmpEntry.vpn
                        + ", pid = " + getPID());
                pt.set(tmpEntry.vpn, getPID(), tmpEntry);
            }
        }
//        Lib.debug(dbgVM, "Leaving saveState(): pid = " + getPID());
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
        if (getPID() != 0) {
            Lib.debug(dbgVM, "In restoreState(): pid = " + getPID());
            iterateTLB();
        }

        // restore TLB if possible
        Processor proc = Machine.processor();
        PageTable pt = PageTable.getInstance();
        for (int i = 0; i < proc.getTLBSize(); i++) {
            PIDEntry pe = null;
            if (tlbBackUp[i].valid) {
                pe = pt.getEntryFromVirtual(tlbBackUp[i].vpn, getPID());
            }
            if (pe == null) {
                proc.writeTLBEntry(i, new TranslationEntry(-1, -1, false, false, false, false));
            } else {
                Lib.assertTrue(pe.getEntry() != null);
                proc.writeTLBEntry(i, pe.getEntry());
            }
        }
    }

    /**
     * Find whether TLB contains the query translation.
     *
     * @param vpn - virtual page number.
     * @return the index of TLB, if not found return -1.
     */
    private int findEntryFromTLB(int vpn) {
//        Lib.debug(dbgVM, "In findEntryFromTLB: vpn = " + vpn);
        int size = Machine.processor().getTLBSize();

        for (int i = 0; i < size; i++) {
            TranslationEntry te = Machine.processor().readTLBEntry(i);

            // find the entry in TLB
            if (te != null && te.valid && te.vpn == vpn) {
//                Lib.debug(dbgVM, "\tFind index:  " + i);
                return i;
            }
        }

        Lib.debug(dbgVM, "\tCannot find entry slot");
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

        vmLock.acquire();

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
            int iAddr = Processor.makeAddress(i, 0);
            int tlbIndex = findEntryFromTLB(i);

            if (tlbIndex == -1) { // TLB miss
                tlbIndex = handleTLBMiss(iAddr);

                if (tlbIndex == -1) {
                    // abort
                    Lib.debug(dbgProcess, "\tCannot handle page fault!");
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

            // update entry tables
            Machine.processor().writeTLBEntry(tlbIndex, te);

            amount += count;
        }

        vmLock.release();

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
//		Lib.debug(dbgProcess, "In writeVirtualMemory(vm): vaddr=" + vaddr + ", byte len="
//				+ data.length + ", beginning offset=" + offset + ", length=" + length
//				+ " current pid = " + getPID());

        vmLock.acquire();

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
            int iAddr = Processor.makeAddress(i, 0);
            int tlbIndex = findEntryFromTLB(i);

            if (tlbIndex == -1) { // TLB miss
                tlbIndex = handleTLBMiss(iAddr);

                if (tlbIndex == -1) {
                    // abort
                    Lib.debug(dbgProcess, "\tCannot handle page fault!");
                    handleExit(Processor.exceptionBusError);
                }
            }

            TranslationEntry te = Machine.processor().readTLBEntry(tlbIndex);
            Lib.assertTrue((te != null) && te.valid);

            // check if the read-only page is to be written.
            if (te.readOnly) {
                // abort
                Lib.debug(dbgProcess, "\tTry to write readOnly page " + i);
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
            Machine.processor().writeTLBEntry(tlbIndex, te); // write-back mechanism

            amount += count;
        }

        vmLock.release();

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
        Lib.debug(dbgVM, "(vm)In loadSections():");
        vmLock.acquire();
        int pagesCount = 0;
        int vpn = -1;

        // register sections
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection cs = coff.getSection(s);
            boolean ro = cs.isReadOnly();
            Lib.debug(dbgVM, "\tSection " + s + ": " + cs.getName()
                        + ", readOnly: " + cs.isReadOnly()
                        + ", initialized: " + cs.isInitialzed()
                        + ", firstVPN: " + cs.getFirstVPN()
                        + ", secLength: " + cs.getLength());

            for (int i = 0; i < cs.getLength(); i++) {
                vpn = cs.getFirstVPN() + i;

                /*
                 * Here we only register the pages without
                 * actually loading them into memory.
                 */
                secMap.put(vpn, new SecInfo(s, i, ro, false));
                pagesCount++;
            }
        }

        // register stack pages and the parameter page
        // XXX: do we really need this?
        int residue = numPages - pagesCount;
        Lib.assertTrue(residue == stackPages + 1); // 8 stack pages + 1 parameter page
        for (int i = 0; i < residue - 1; i++) {
            secMap.put(++vpn, new SecInfo(-1, i, false, false));
        }
        secMap.put(++vpn, new SecInfo(-2, 0, false, false));

        vmLock.release();
        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        //super.unloadSections();
        // TODO: unload page table entries belonging to current process.
        Lib.debug(dbgVM, "(vm)In unloadSections():");
//        vmLock.acquire();

        // Invalidate current TLB entries in order not to influence
        // the following processes (On context switch, OS may write
        // TLB back to page table).
        invalidateAllTLB();

        PageTable pt = PageTable.getInstance();
        int pid = getPID();

        Lib.debug(dbgVM, "\tPageTables before unloadSections:");
        pt.iterateVirtualTable();
        pt.iteratePhysicalTable();

        for (Integer v : secMap.keySet()) {
            int ppn = -1;
            if (pt.getEntryFromVirtual(v, pid) != null) {
                ppn = pt.getEntryFromVirtual(v, pid).getEntry().ppn;
            }

            if (ppn != -1) {
//                pt.unsetVirtualToEntry(v, pid);
//                pt.unsetPhysicalToEntry(ppn, pid);
                Lib.debug(dbgVM, "\tunloadSections(): remove()-> vpn = " + v
                        + ", pid = " + pid);
                pt.remove(v, pid);

                // reclaim the physical page
                Lib.debug(dbgVM, "\tReclaim page pid = " + pid +
                        " vpn = " + v + " ppn = " + ppn);
                pt.iterateVirtualTable();
                pt.iteratePhysicalTable();

                UserKernel.fpLock.acquire();
                UserKernel.freePages.add(ppn);
                UserKernel.fpLock.release();
            }
        }

        Lib.debug(dbgVM, "\tPageTables after unloadSections:");
        pt.iterateVirtualTable();
        pt.iteratePhysicalTable();

        coff.close();

//        vmLock.release();
    }

    /**
     * Invalidate all TLB entry.
     * Only used on termination.
     */
    private void invalidateAllTLB() {
        Lib.debug(dbgVM, "In invalidateAllTLB(), pid = " + getPID());
        int tlbSize = Machine.processor().getTLBSize();
        Processor proc = Machine.processor();

        for (int i = 0; i < tlbSize; i++) {
            Lib.debug(dbgVM, "\tOld TLB(" + i + "): vpn = " +
                    proc.readTLBEntry(i).vpn + ", ppn = " +
                    proc.readTLBEntry(i).ppn);
            proc.writeTLBEntry(i, new TranslationEntry(-1, -1, false, false, false, false));
        }
    }

    private void iterateTLB() {
        Lib.debug(dbgVM, "In iterateTLB(), pid = " + getPID());
        int tlbSize = Machine.processor().getTLBSize();
        Processor proc = Machine.processor();

        for (int i = 0; i < tlbSize; i++) {
            Lib.debug(dbgVM, "\tOld TLB(" + i + "): vpn = " +
                    proc.readTLBEntry(i).vpn + ", ppn = " +
                    proc.readTLBEntry(i).ppn + ", valid = " +
                    proc.readTLBEntry(i).valid);
        }
    }

    /**
     * Handle TLB miss.
     *
     * @param vaddr - the virtual memory address.
     * @return - index in TLB, if TLB is handled;
     *           -1, if the miss cannot be handled, might be an illegal access.
     */
    private int handleTLBMiss(int vaddr) {
        Lib.debug(dbgVM, "--- In handleTLBMiss(): vaddr = " + vaddr + "vpn = "
                   + Processor.pageFromAddress(vaddr) + ", pid = " + getPID());
        int vpn = Processor.pageFromAddress(vaddr);
        int sizeTLB = Machine.processor().getTLBSize();
        int invalidIndex = -1;
        PIDEntry pe = PageTable.getInstance().getEntryFromVirtual(vpn, getPID());

        if (pe == null) {
            // handle page fault
            if (!handlePageFault(vaddr))
                return -1;
            // reload entry from page table
            pe = PageTable.getInstance().getEntryFromVirtual(vpn, getPID());
        }
        Lib.assertTrue(pe != null && pe.getEntry().valid);

        // find an invalid entry to victimize if possible
        for (int i = 0; i < sizeTLB; i++)
        {
            // get entry in TLB
            TranslationEntry te = Machine.processor().readTLBEntry(i);

            // check if entry is invalid
            if (te == null || !te.valid)
            {
                invalidIndex = i;
                break;
            }
        }
        Lib.debug(dbgVM, "\tChoose TLB index: " + invalidIndex);

        // all entries in TLB were valid, choose randomly to replace
        // note that we do no need to write back the invalid entry to page table
        if (invalidIndex == -1)
        {
            invalidIndex = randomlyVictimizeTLB();
            Lib.debug(dbgVM, "\tVictimize TLB index: " + invalidIndex);

            // write the victim entry back to page table before replacement
            TranslationEntry tmpEntry = Machine.processor().readTLBEntry(invalidIndex);

//            PageTable.getInstance().setVirtualToEntry(tmpEntry.vpn,
//                    getPID(), new PIDEntry(getPID(), tmpEntry));
//            PageTable.getInstance().setPhysicalToEntry(tmpEntry.ppn,
//                    new PIDEntry(getPID(), tmpEntry));
            Lib.debug(dbgVM, "\thandleTLBMiss(): set()-> vpn = " + tmpEntry.vpn
                            + ", pid = " + getPID());
            PageTable.getInstance().set(tmpEntry.vpn, getPID(), tmpEntry);
        }

        // write the new TLB entry
        Machine.processor().writeTLBEntry(invalidIndex, pe.getEntry());

        Lib.debug(dbgVM, "--- Leaving handleTLBMiss(): vaddr = " + vaddr
                + ", pid = " + getPID() + ", TLB index = " + invalidIndex);
        return invalidIndex;
    }

    /**
     * Randomly choose a victim TLB entry to be replaced.
     */
    private int randomlyVictimizeTLB() {
        return Lib.random(Machine.processor().getTLBSize());
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
        Lib.debug(dbgVM, "In handlePageFault(): vaddr = " + vaddr);
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
        Lib.debug(dbgVM, "In swapIn(): vpn = " + vpn + ", pid = " + pid);
        PageTable pt = PageTable.getInstance();
        SwapFile sf = SwapFile.getInstance();
        Lib.assertTrue(pt != null && sf != null);

        // find free slot in main memory
        int ppn = -1;
        if (UserKernel.freePages.size() > 0) {
            ppn = UserKernel.freePages.poll();
            Lib.debug(dbgVM, "\tswapIn(): find free ppn = " + ppn);
        }
        if (ppn == -1) { // no free main memory
            Lib.debug(dbgVM, "\tswapIn(): cannot find free physical memory");
            if ((ppn = swapOut(nextVictimPage())) == -1) {
                Lib.debug(dbgVM, "\tswapOut() failed: no free page!");
                return false;
            }
        }
        Lib.debug(dbgVM, "\tswapIn(): find new page: ppn = " + ppn);

        // load coff section into physical memory
        PIDEntry pe;
        TranslationEntry te;
        if (secMap.containsKey(vpn) && !secMap.get(vpn).loaded) {
            SecInfo si = secMap.get(vpn);
            si.loaded = true;
            secMap.put(vpn, si);  // update the map
            Lib.assertTrue(secMap.get(vpn).loaded);

            Lib.debug(dbgVM, "\tload Coff section: sec " + si.spn
                    + " subpage " + si.ipn);
            if (si.spn >= 0) {
                // load regular coff sections
                Lib.debug(dbgVM, "\tloading regular pages: spn = "
                        + si.spn + ", ipn = " + si.ipn);
                coff.getSection(si.spn).loadPage(si.ipn, ppn);
            } else {
                // stack pages or the parameter page
                Lib.debug(dbgVM, "\tloading stack pages or parameters: spn = "
                        + si.spn + ", ipn = " + si.ipn);
            }

            // update page table
            te = new TranslationEntry(vpn, ppn, true, si.readOnly, true, true);
//            pe = new PIDEntry(getPID(), te);
        } else {
            int paddr = Processor.makeAddress(ppn, 0);
            byte[] physicalMemory = Machine.processor().getMemory();
            byte[] buf = new byte[pageSize];
            if (sf.readPage(buf, 0, vpn, pid) != pageSize) {
                Lib.debug(dbgVM, "\tReading page from swap failed!");
                return false;
            }

            System.arraycopy(buf, 0, physicalMemory, paddr, pageSize);

            pe = sf.findEntryInSwap(vpn, pid);
            Lib.assertTrue(pe != null);
            Lib.debug(dbgVM, "\tLoading from swap file: " + pe);

            // keep readOnly bit invariant
            te = pe.getEntry();
            Lib.assertTrue(te != null);
            te.ppn = ppn;
            te.vpn = vpn;
            te.valid = true;
            te.used = true;
            te.dirty = false;
        }

        Lib.debug(dbgVM, "\tNow set the page table entries...");
//        pe.setEntry(te);
//        pt.setVirtualToEntry(vpn, pid, pe);  // update the <VP, PIDEntry>
//        pt.setPhysicalToEntry(ppn, pe);
        Lib.debug(dbgVM, "\tswapIn(): set()-> vpn = " + vpn
                + ", pid = " + pid);
        pt.set(vpn, pid, te);
        pt.iterateVirtualTable();
        pt.iteratePhysicalTable();
        Lib.debug(dbgVM, "\tswapIn(): Exit handling...");

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
        Lib.debug(dbgVM, "In swapOut(): " + outEntry);
        TranslationEntry entry = outEntry.getEntry();
        Lib.assertTrue(entry != null && entry.valid);
        int vpn = entry.vpn;
        int ppn = entry.ppn;
        int pid = outEntry.getPID();

        if (entry.dirty) {
            Lib.debug(dbgVM, "\tswapOut(): need write back");
            int paddr = Processor.makeAddress(ppn, 0);

            byte[] buf = new byte[pageSize];
            byte[] physicalMemory = Machine.processor().getMemory();

            System.arraycopy(physicalMemory, paddr, buf, 0, pageSize);

            if (SwapFile.getInstance().writePage(buf, 0, vpn, pid) != pageSize) {
                Lib.debug(dbgVM, "\tswapOut(): copy physical page failed");
                return -1;
            }
        }

        // invalidate the entry buffered in TLB if exists
        entry.valid = false;
        int index = findEntryFromTLB(vpn);
        if (index != -1) {
            Machine.processor().writeTLBEntry(index, entry);
        }

        /*
         * Remove entry in the page table as page table should
         * only contain entries of virtual page actually residing
         * in the physical memory.
         */
        Lib.debug(dbgVM, "\tswapOut(): remove()-> vpn = " + vpn
                        + ", pid = " + pid);
        PageTable.getInstance().remove(vpn, pid);
//        PageTable.getInstance().unsetVirtualToEntry(vpn, pid);
//        PageTable.getInstance().unsetPhysicalToEntry(ppn, pid);

        return entry.ppn;
    }

    private PIDEntry nextVictimPage() {
        Lib.debug(dbgVM, "In nextVictimPage(): ");
        /*
         * Prepare for the victimizing algorithm by writing updated
         * entries in TLB back to page tables.
         */
        int size = Machine.processor().getTLBSize();
        PageTable pt = PageTable.getInstance();
        for (int i = 0; i < size; i++) {
            TranslationEntry te = Machine.processor().readTLBEntry(i);

            if (te != null && te.valid) {
//                PIDEntry pe = new PIDEntry(getPID(), te);
//                pt.setVirtualToEntry(te.vpn, getPID(), pe);
//                pt.setPhysicalToEntry(te.ppn, p
                Lib.debug(dbgVM, "\tnextVictimPage(): set()-> vpn = " + te.vpn
                        + ", pid = " + getPID());
                pt.set(te.vpn, getPID(), te);
            }
        }

//        PIDEntry victim = PageTable.getInstance().victimize();
        PIDEntry victim = PageTable.getInstance().randVictimize();
        Lib.debug(dbgVM, "\tvictim: " + victim);
        return victim;
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
            Lib.debug(dbgVM, "After handling TLB miss exception...");
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

    /** Backup TLB entries. */
    private TranslationEntry[] tlbBackUp = null;

    /** Section map: <section page vpn, SecInfo>. */
    private HashMap<Integer, SecInfo> secMap = null;

    /** Coff section information. */
    private class SecInfo {
        public int spn;  // section number
        public int ipn;  // page number within a section
        public boolean readOnly;
        public boolean loaded = false;

        public SecInfo(int spn, int ipn, boolean readOnly, boolean loaded) {
            this.spn = spn;
            this.ipn = ipn;
            this.readOnly = readOnly;
            this.loaded = loaded;
        }
    }
}
