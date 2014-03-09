package nachos.vm;

import nachos.machine.*;
import nachos.machine.Processor;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import javax.annotation.processing.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
	/**
	 * Allocate a new process.
	 */
	public VMProcess() {
		super();
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
     * Transfer data from memory to buffering array.
     */
    @Override
    public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
        // TODO
        return 0;
    }

    /**
     * Transfer data from buffering array to physical memory.
     */
    @Override
    public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
        // TODO
        return 0;
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

    private int handleTLBMiss(int vaddr) {
        // TODO
        return -1;
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
            // TODO
            handleTLBMiss(processor.readRegister(Processor.regBadVAddr));
            break;
		default:
			super.handleException(cause);
			break;
		}
	}

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}
