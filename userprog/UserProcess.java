package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 * 
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 * 
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		/* We postpone the memory allocation until a binary is loaded. */
		pidLock.acquire();
//		for (int i = 0; ; i++) {
//			if (!UserKernel.pidPoll.contains(i)) {
//				this.pid = i;
//				UserKernel.pidPoll.add(i);
//				break;
//			}
//		}
		this.pid = pidAccumulated++;
		
		Lib.assertTrue(totalProcess >= 0);
		totalProcess++;
		pidLock.release();
		
		// Initialize open files
		openFileLock.acquire();
		// Since process 0 is init process, other process should
		// inherit the stdin and stdout console file which are created
		// by the init process.
		if (pid == 0) {
			stdin = UserKernel.console.openForReading();
			stdout = UserKernel.console.openForWriting();
			openedFiles[0] = stdin;
			openedFiles[1] = stdout;
		} 
		openFileLock.release();
		
		// Initialize children structure
		children = new HashMap<Integer, UserProcess>();
		endedChildren = new HashMap<Integer, Integer>();
		
		virtualToTransEntry = new HashMap<Integer, TranslationEntry>();
		
		Lib.debug(dbgProcess, "*** A process has been created, pid = " + pid);
	}
	
	/**
	 * Set the parent of the current process and inherent shared
	 * resources.
	 */
	public void setParent(UserProcess parent) {
		this.parent = parent;
		
		parent.children.put(this.getPID(), this);
		Lib.debug(dbgProcess, "parent (pid = " + parent.getPID() 
				+ ") gets a child (pid = " + parent.children.get(this.getPID())+ ")");
		
		// set shared opened files
		openFileLock.acquire();
		this.stdin = parent.stdin;
		this.stdout = parent.stdout;
		openedFiles[0] = stdin;
		openedFiles[1] = stdout;
		openFileLock.release();
	}
	
	/**
	 * Get the Process ID of current process. 
	 */
	public int getPID() {
		return pid;
	}
	
	/**
	 * Get the UThread corresponding to the current process.
	 */
	public UThread getThread() {
		return thread;
	}
	
	@Override
	public String toString() {
		return "Process: pid = " + getPID();
	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 * 
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
		return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;

		thread = new UThread(this);
		thread.setName(name).fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 * 
	 * @param vaddr the starting virtual address of the null-terminated string.
	 * @param maxLength the maximum number of characters in the string, not
	 * including the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 * found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @param offset the first byte to write in the array.
	 * @param length the number of bytes to transfer from virtual memory to the
	 * array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);
		Lib.debug(dbgProcess, "In readVirtualMemory: vaddr=" + vaddr + ", byte len="
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
		
//		// for now, just assume that virtual addresses equal physical addresses
//		if (vaddr < 0 || vaddr >= physicalMemory.length)
//			return 0;
//
//		int amount = Math.min(length, physicalMemory.length - vaddr);
//		System.arraycopy(physicalMemory, vaddr, data, offset, amount);
		
		for (int i = fromPage; i <= toPage; i++) {
			Lib.debug(dbgProcess, "\t** In page " + i);
			if (!virtualToTransEntry.containsKey(i)
					|| !virtualToTransEntry.get(i).valid) {
				// the current query page is invalid to access
				Lib.debug(dbgProcess, "\t Page invalid or not exist for this process");
				break;
			}

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
			
			int srcPos = Processor.makeAddress(virtualToTransEntry.get(i).ppn, off);
			
			Lib.debug(dbgProcess, "\t *PhyMem Addr=" + srcPos + " data index=" + (offset + amount) 
					+ " count=" + count);
			System.arraycopy(physicalMemory, srcPos, data, offset + amount, count);
			
			amount += count;
		}
//		Lib.debug(dbgProcess, "\t**Memory Read: " + new String(data, 0, amount));

		return amount;
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @param offset the first byte to transfer from the array.
	 * @param length the number of bytes to transfer from the array to virtual
	 * memory.
	 * @return the number of bytes successfully transferred.
	 */
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

//		// for now, just assume that virtual addresses equal physical addresses
//		if (vaddr < 0 || vaddr >= memory.length)
//			return 0;
//		int amount = Math.min(length, memory.length - vaddr);
//		System.arraycopy(data, offset, memory, vaddr, amount);
		
		for (int i = fromPage; i <= toPage; i++) {
			if (!virtualToTransEntry.containsKey(i)
					|| !virtualToTransEntry.get(i).valid
					|| virtualToTransEntry.get(i).readOnly) {
				// the current query page is invalid to access
				break;
			}

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
			
			int dstPos = Processor.makeAddress(virtualToTransEntry.get(i).ppn, off);
			System.arraycopy(data, offset + amount, physicalMemory, dstPos, count);
			
			amount += count;
		}

		
		return amount;
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		}
		catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		Lib.debug(dbgProcess, "\tBegin parsing each section...");
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}
		Lib.debug(dbgProcess, "\t--->complete! Section pages: " + numPages);

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();
		Lib.debug(dbgProcess, "\tentry point: " + initialPC);

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages * pageSize;
		Lib.debug(dbgProcess, "\tstack initial point: " + initialSP);

		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		Lib.debug(dbgProcess, "entryOffset = " + entryOffset);
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\"): complete!");
		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		Lib.debug(dbgProcess, "UserProcess.loadSections");
		Lib.debug(dbgProcess, "\tNeed " + numPages + " pages, free pages #: " 
					+ UserKernel.freePages.size());
		UserKernel.fpLock.acquire();
		
		// check whether there are enough free pages
		if (numPages > UserKernel.freePages.size()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			UserKernel.fpLock.release();
			return false;
		}
		
		// allocate the page table now
		int pagesCount = 0;
		pageTable = new TranslationEntry[numPages];

		// load sections
		int vpn = -1, ppn = -1;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			boolean readOnly = section.isReadOnly();

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				vpn = section.getFirstVPN() + i;

				// now find a free physical page
				Lib.assertTrue(!UserKernel.freePages.isEmpty());
				ppn = UserKernel.freePages.pollFirst();
				section.loadPage(i, ppn);
				
				// register this page
				pageTable[pagesCount++] = 
						new TranslationEntry(vpn, ppn, true, readOnly, false, false);
			}
		}
		
		// register remaining pages for stack and arguments (XXX: not sure)
		Lib.assertTrue(vpn >= 0 && ppn >= 0);
		Lib.debug(dbgProcess, "\tStill has " + (numPages - pagesCount) + " pages");
		while (pagesCount < numPages) {
			Lib.assertTrue(!UserKernel.freePages.isEmpty());
			ppn = UserKernel.freePages.pollFirst();
			pageTable[pagesCount++] =
					new TranslationEntry(++vpn, ppn, true, false, false, false);
		}
		
		// fill up the virtual -> translation entry map
		for (int i = 0; i < pageTable.length; i++) {
			virtualToTransEntry.put(pageTable[i].vpn, pageTable[i]);
		}
		
		UserKernel.fpLock.release();

		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		UserKernel.fpLock.acquire();
		
		// Put back the using physical pages to free list again
		for (TranslationEntry entry : virtualToTransEntry.values()) {
			UserKernel.freePages.add(entry.ppn);
		}
		
		UserKernel.fpLock.release();
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {

		Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}
	
	/**
	 * Attempt to open the file located at the virtual address.
	 * Create the file if it does not exist and return the file
	 * descriptor that can be used to accesss the file.  This can
	 * only be used to create files on disk; can never return a 
	 * file descriptor referencing a file stream.
	 * 
	 * @param vaddr = The virtual address of the file name
	 * @return the new file descriptor, or -1 if there was an error
	 */
	private int handleCreate(int vaddr) {
		//Get the file name.
		String fileName = readVirtualMemoryString(vaddr, MAX_FILENAME_LEN);
		if(fileName != null)
		{
			//File name is valid so "create".
			OpenFile newFile = ThreadedKernel.fileSystem.open(fileName, true);
			if(newFile != null)
			{
				//Find first empty slot in openedFiles, and place file there.
				int openSlot = 0;
				while((openedFiles[openSlot] != null) && (openSlot < MAX_FILES))
				{
					openSlot++;
				}
				if(openSlot != MAX_FILES) 
				{
					openedFiles[openSlot] = newFile;
					return openSlot;
				}
			}
		}
		return -1;
	}
	
	/**
	 * Open the file referenced by the virtual address and 
	 * return the fileDescriptor on success.  This will only
	 * open files on the disk; it won't open any file streams.
	 * 
	 * @param vaddr = The virtual address of the file name
	 * @return fileDescriptor on success, or -1 if there was an error
	 */
	private int handleOpen(int vaddr) {
		//Get the file name.
		String fileName = readVirtualMemoryString(vaddr, MAX_FILENAME_LEN);
		if(fileName != null)
		{
			//File name is valid so "open".
			OpenFile newFile = ThreadedKernel.fileSystem.open(fileName, false);
			if(newFile != null)
			{
				//Find first empty slot in openedFiles, and place file there.
				int openSlot = 0;
				while((openedFiles[openSlot] != null) && (openSlot < MAX_FILES))
				{
					openSlot++;
				}
				if(openSlot != MAX_FILES)
				{
					openedFiles[openSlot] = newFile;
					return openSlot;
				}
			}
		}
		return -1;
	}
	
	/**
	 * Attempt to read up to 'count' number of bytes into the
	 * buffer from a file or stream which is referred to by
	 * the fileDescriptor.  On success, the number of bytes read
	 * is returned.  If the fileDescriptor referes to a file on 
	 * disk, file position is advanced by this number.  There is
	 * not necessarily an error if the th number returned is less
	 * than the count; if the fileDescriptor is referring to a 
	 * file on the disk this indicates the read has reached EOF.
	 * This method never waits for a stream to obtain more data,
	 * it will read whatever is currently available upon request
	 * which may also cause the return to be less than count if
	 * not enough data is ready.
	 * 
	 * @param fileDescriptor = reference to file
	 * @param baddr = the address to the read buffer (read to)
	 * @param count = number of bytes to (attempt to) read
	 * @return number of bytes read on success, or -1 if there was an error
	 */
	private int handleRead(int fileDescriptor, int baddr, int count) {
//		System.out.println("In handleRead(): fd = " + fileDescriptor + 
//				 " read buf @" + baddr + " count = " + count);
		//Verify valid fileDescriptor and valid count parameters
		if((fileDescriptor > MAX_FILES - 1) || (fileDescriptor < 0) || count < 0)
		{
			return -1;
		}
		else
		{
			//File is valid so open
			OpenFile readFile = openedFiles[fileDescriptor];
			if(readFile == null)
			{
				return -1;
			}
			else
			{
				//This buffer is needed to be read during the OpenFile.read() call.
				byte[] tempReadBuffer = new byte[count];
				
				//read the file
				int bytesRead =
					readFile.read(tempReadBuffer, 0, count);
				if(bytesRead != -1)
				{
					return writeVirtualMemory(baddr, tempReadBuffer, 0, bytesRead);
				}
				else
				{
					//error when trying to read into byte buffer
					return -1;
				}
			}
		}
	}

	/**
	 * Attempt to write up to 'count' bytes from the buffer to
	 * the file/stream referred to by fileDescriptor.  Write() can
	 * return before bytes are actually flushed to file or stream.
	 * A write to a stream can block, if kernel queues are full.
	 * On success, the number of bytes written is returned and file
	 * position is advanced by this number.  It is an error if this
	 * number is smaller than 'count;.  For disk files, this indicates
	 * that the disk is full.  For streams, this indicates that the
	 * stream was terminated by some remote host.
	 * 
	 * @param fileDescriptor = reference to file
	 * @param baddr = the address to the buffer (write from)
	 * @param count = number of bytes to (attempt to) write
	 * @return number of bytes written on success, or -1 if there was an error
	 */
	private int handleWrite(int fileDescriptor, int baddr, int count) {
//		Lib.debug(dbgProcess, "In handleWrite(): fd = " + fileDescriptor 
//				+ " buf addr = " + baddr + " count = " + count);
		//Verify valid fileDescriptor and valid count parameters
		if((fileDescriptor > MAX_FILES - 1) || (fileDescriptor < 0) || count < 0)
		{
			return -1;
		}
		else
		{
			//File is valid so open
			OpenFile writeFile = openedFiles[fileDescriptor];
			if(writeFile == null)
			{
				return -1;
			}
			else
			{
				byte[] tempWriteBuffer = new byte[count];
				int bytesFromVirtual = readVirtualMemory(baddr, tempWriteBuffer);
			
				if(bytesFromVirtual == count)
				{
					int bytesWritten = writeFile.write(tempWriteBuffer, 0, bytesFromVirtual);
				
					if(bytesWritten != bytesFromVirtual)
					{
						return -1;
					}
					return bytesWritten;
				}
				else
				{
					return -1;
				}	
			}
		}
	}
	
	/**
	 * Close the fileDescriptor so that it no longer refers
	 * to any file or stream and may be reused.  If the
	 * fileDescriptor refers to a file, all data written to
	 * it by write() will be flushed to the disk before close()
	 * returns.  If it refers to a stream, all data written to it
	 * by write() will eventually be flushed but not necessarily
	 * before close() returns.  Resources associated with the
	 * fileDescriptor are released.  If the fileDescriptor is the
	 * last reference to disk file whic has been removed using
	 * handleUnlink() then file is deleted.
	 * 
	 * @param fileDescriptor = reference to a file
	 * @return 0 on success, or -1 if there was an error
	 */
	private int handleClose(int fileDescriptor) {
		//Verify valid fileDescriptor
		if((fileDescriptor > MAX_FILES - 1) || (fileDescriptor < 0))
		{
			//fileDescriptor is invalid. Error occured.
			return -1;
		}
		else
		{
			//Get the file referenced by fileDescriptor
			OpenFile openFile = openedFiles[fileDescriptor];
			
			//Check if there is actually a file referenced by this location
			if(openFile == null)
			{
				return -1;
			}
			else
			{
				openFile.close();
				openedFiles[fileDescriptor] = null;
			}
		}
		return 0;
	}
	
	/**
	 * If no processes currently have the file open, delete
	 * it immediately and free the space it was using.
	 * If any process has the file open currently, the file
	 * wil remain in existance until the last file descriptor
	 * referring to it is closed.  Create() and Open() will 
	 * not be able to return new file descriptors for the file
	 * it is deleted.
	 * 
	 * @param vaddr = The virtual address of the file name
	 * @return 0 on success, or -1 if there was an error
	 */
	private int handleUnlink(int vaddr) {
		//Get the string file name located at the virtual address
		String fileName = readVirtualMemoryString(vaddr, MAX_FILENAME_LEN);
		
		//Call the remove method in the FileSystem stub
		//It will check to see if the file name is valid
		//as a part of its processing. Returns false on error.
		if(!(ThreadedKernel.fileSystem.remove(fileName)))
		{
			return -1;
		}
		else
		{
			return 0;
		}
	}
	
	/**    
	 * int exec(char *file, int argc, char *argv[]);
	 * Execute the program stored in the specified file, with the specified
	 * arguments, in a new child process. The child process has a new unique
	 * process ID, and starts with stdin opened as file descriptor 0, and stdout
	 * opened as file descriptor 1.
	 *
	 * file is a null-terminated string that specifies the name of the file
	 * containing the executable. Note that this string must include the ".coff"
	 * extension.
	 *
	 * argc specifies the number of arguments to pass to the child process. This
	 * number must be non-negative.
	 *
	 * argv is an array of pointers to null-terminated strings that represent the
	 * arguments to pass to the child process. argv[0] points to the first
	 * argument, and argv[argc-1] points to the last argument.
	 *
	 * exec() returns the child process's process ID, which can be passed to
	 * join(). On error, returns -1.
	 * 
	 */
	private int handleExec(int vaddr1, int argnum, int vaddrc) {
		Lib.debug(dbgProcess, "## In handleExec(): argc = " + argnum + " argv addr: " + vaddrc);
		String stringFile = readVirtualMemoryString(vaddr1, VtoSmaxLength);
		if (stringFile == null || !stringFile.endsWith(".coff")) {
			return -1;
		}
		Lib.debug(dbgProcess, "\tProgram name: " + stringFile);
		
		String[] args = new String[argnum];
        for (int i = 0; i < argnum; i++) {
        	byte[] readbyte = new byte[4];
        	int readcount = 0;
        	int argAddr;
        	readcount = readVirtualMemory(vaddrc + (i * 4), readbyte);
        	if (readcount == 0) {
        		return -1;
        	}
        	argAddr = Lib.bytesToInt(readbyte, 0);
        	args[i] = readVirtualMemoryString(argAddr, VtoSmaxLength);
        	
        	Lib.debug(dbgProcess, "\targs[" + i + "] = " + args[i]);
        }
        
		UserProcess child = UserProcess.newUserProcess();
		child.setParent(this);  // set it parent (new function)
		this.children.put(child.getPID(), child);
		boolean successexec = child.execute(stringFile, args);
		if (successexec) {
			return child.getPID();
		} else {
			return -1;
		}
	}
	
	/**    
	 * int join(int processID, int *status);
	 * Suspend execution of the current process until the child process specified
	 * by the processID argument has exited. If the child has already exited by the
	 * time of the call, returns immediately. When the current process resumes, it
	 * disowns the child process, so that join() cannot be used on that process
	 * again.
	 *
	 * processID is the process ID of the child process, returned by exec().
	 *
	 * status points to an integer where the exit status of the child process will
	 * be stored. This is the value the child passed to exit(). If the child exited
	 * because of an unhandled exception, the value stored is not defined.
	 *
	 * If the child exited normally, returns 1. If the child exited as a result of
	 * an unhandled exception, returns 0. If processID does not refer to a child
	 * process of the current process, returns -1.
	 */
	private int handleJoin(int joinpid, int statusPtr) {
		Lib.debug(dbgProcess, "## In handleJoin (current pid = " +
				getPID() + ", joinpid = " + joinpid + " status p = "
				+ statusPtr);
		UserProcess child = null;
		
		// should not wait itself!
		if (joinpid == getPID()) {
			Lib.debug(dbgProcess, "\t(handleJoin(curPID = " + this.getPID()
					+ ")) You cannot wait yourself!");
			return -1;
		}
		
		if (children == null || children.size() == 0) {
			Lib.debug(dbgProcess, "\t(handleJoin(curPID = " + this.getPID()
					+ ")) No children to wait");
			return -1;
		}
		
		if (!children.containsKey(joinpid)) {
			Lib.debug(dbgProcess, "\t(handleJoin(curPID = " + this.getPID()
					+ ")) joinpid = " + joinpid + ": child not matched");
			return -1;
		} else {	// process to be waited is a valid child
			child = children.get(joinpid);
			joinLock.acquire();
			if (endedChildren.containsKey(joinpid)) {
				joinLock.release();
				children.remove(joinpid);
			} else {
				joinLock.release();
				child.getThread().join();	// wait here!
			}	
		}
		
		Lib.debug(dbgProcess, "\t(handleJoin(curPID = " + this.getPID() + 
				"))List endedchildren");
		for (Integer i : endedChildren.keySet()) {
			Lib.debug(dbgProcess, "\t# pid = " + i + ", status = " + endedChildren.get(i));
		}
		if (endedChildren.get(joinpid) == null) {
			Lib.debug(dbgProcess, "\t(handleJoin(curPID = " + this.getPID() 
					+ "))Cannot find joinpid = " + joinpid 
					+ " from endedchildren list of current pid = " + getPID());
			return -1;
		}
		
		writeVirtualMemory(statusPtr, Lib.bytesFromInt(endedChildren.get(joinpid)));
		
		joinLock.acquire();
		if (endedChildren.get(joinpid) == 0) {
			endedChildren.remove(joinpid);
			joinLock.release();
			Lib.debug(dbgProcess, "In handleJoin, joinpid = " + joinpid + 
					" current pid = " + getPID() + ": child exited normally");
			return 1;
		} else {
			endedChildren.remove(joinpid);
			joinLock.release();
			Lib.debug(dbgProcess, "In handleJoin, joinpid = " + joinpid + 
					" current pid = " + getPID() + ": child exited with unhandled exception");
			return 0;
		}
	}
	
	/**   
	 * void exit(int status);
	 * Terminate the current process immediately. Any open file descriptors
	 * belonging to the process are closed. Any children of the process no longer
	 * have a parent process.
	 *
	 * status is returned to the parent process as this process's exit status and
	 * can be collected using the join syscall. A process exiting normally should
	 * (but is not required to) set status to 0.
	 *
	 * exit() never returns.
	 */
	private void handleExit(int status) {
		Lib.debug(dbgProcess, "In handleExit(" + status + "), curPID = " + getPID());
		
		// when process 0 tries to exit, halt the machine...
		Lib.assertTrue(totalProcess > 0);
		// the last process in the OS
		if (--totalProcess == 0) {
			Kernel.kernel.terminate();
		}
		
		int localStatus = status;
		for (int i = 2; i < MAX_FILES; i++) { // do not close stdin and stdout
			if(openedFiles[i] != null) {
				int succlose = handleClose(i);
				if (succlose != 0) {
					localStatus = 1;
				}
			}
		}
		
		// unlink the running children with the current process
		Lib.debug(dbgProcess, "\t(handleExit(curPID = " + this.getPID() 
				+ "))Opened files closed");
		Collection<UserProcess> coll = children.values();
		List<UserProcess> exitchildren = new ArrayList<UserProcess>(coll);
		for(int m = 0; m < exitchildren.size(); m++) {
			exitchildren.get(m).parent = null; // problematic
		}
		Lib.debug(dbgProcess, "\t(handleExit(curPID = " + this.getPID() 
				+ ")) Children's parent reset");
		
		// find out whether we have parent to notify
		if (parent != null) {
			joinLock.acquire();
			parent.endedChildren.put(this.getPID(), localStatus);
			joinLock.release();
			
			Lib.debug(dbgProcess, "\t(handleExit(curPID = " + this.getPID() +
					")) Put status into endedChildren of " + parent + ": pid = "
					+ this.getPID() + " status = " + parent.endedChildren.get(this.getPID()));
		}
		
		// unregister all resources
		unloadSections();
		virtualToTransEntry = null;
		pageTable = null;
		children = null;
		coff.close();

		Lib.debug(dbgProcess, "\t(handleExit(curPID = " + this.getPID() 
				+ ")) Process memory deallocated, leaving handleExit()");
		UThread.finish();
	}
	
	// backup
	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 * 
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall the syscall number.
	 * @param a0 the first syscall argument.
	 * @param a1 the second syscall argument.
	 * @param a2 the third syscall argument.
	 * @param a3 the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
		case syscallHalt:
			return handleHalt();
		case syscallExit: // XXX: for initial test only!
//			Lib.debug(dbgProcess, "Handle syscallExit " + syscall);
			handleExit(a0);
			break;
		case syscallExec:
			Lib.debug(dbgProcess, "Handle syscallExec " + syscall);
//			byte[] mm = Machine.processor().getMemory();
//			for(int i= 10000; i < mm.length; i++)
//				if (mm[i] != 0)
//					System.out.println("index" + i + " " + (char) mm[i]);
//			byte[] temp = new byte[1000];
//			readVirtualMemory(a2 + 128, temp, 0, 1000);
//			String str;
//			try {
//				str = new String(temp, "UTF-8");
//				Lib.debug(dbgProcess, str);
//			} catch (UnsupportedEncodingException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			return handleExec(a0, a1, a2);
		case syscallJoin:
//			Lib.debug(dbgProcess, "Handle syscallJoin " + syscall);
			return handleJoin(a0, a1);
		case syscallCreate:
//			Lib.debug(dbgProcess, "Handle syscallCreate " + syscall);
			return handleCreate(a0);
		case syscallOpen:
//			Lib.debug(dbgProcess, "Handle syscallOpen " + syscall);
			return handleOpen(a0);
		case syscallRead:
//			Lib.debug(dbgProcess, "Handle syscallRead " + syscall);
			return handleRead(a0, a1, a2);
		case syscallWrite:
//			Lib.debug(dbgProcess, "Handle syscallWrite " + syscall);
			return handleWrite(a0, a1, a2);
		case syscallClose:
//			Lib.debug(dbgProcess, "Handle syscallClose " + syscall);
			return handleClose(a0);
		case syscallUnlink:
//			Lib.debug(dbgProcess, "Handle syscallUnlink " + syscall);
			return handleUnlink(a0);

		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			Lib.assertNotReached("Unknown system call!");
		}
		return 0;
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
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0),
					processor.readRegister(Processor.regA1),
					processor.readRegister(Processor.regA2),
					processor.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
			break;
		
		default:
			Lib.debug(dbgProcess, "Unexpected exception: "
					+ Processor.exceptionNames[cause]);
			Lib.assertNotReached("Unexpected exception");
		}
	}

	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;

	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	private int initialPC, initialSP;

	private int argc, argv;

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';
	
	/**
	 * New added data structures. 
	 */
	/** Total number of running processes. */
	private static int totalProcess = 0;
	
	/** Process ID */
	private int pid;
	
	private int waitjoinpid;
	
	private int VtoSmaxLength = 256;
	
	/** Accumulated PID */
	private static int pidAccumulated = 0;
	
	/** The underlying UThread corresponding to the process */
	private UThread thread = null;
	
	/** The parent of the current process. */
	private UserProcess parent = null;
	
	/** The children of the current process: <PID, UserProcess>. */
	private HashMap<Integer, UserProcess> children = null;
	
	/** Exit status for each child: <Child PID, exit status> */
	public HashMap<Integer, Integer> endedChildren = null;
	
	/** Maximum file length */
	private static final int MAX_FILENAME_LEN = 256;
	
	/** Opened files. */
	private final int MAX_FILES = 16;
	private OpenFile openedFiles[] = new OpenFile[MAX_FILES];
	
	/** A map from virtual memory to Translation entry: <vaddr, TranslationEntry>. */
	private HashMap<Integer, TranslationEntry> virtualToTransEntry = null;
	
	/** Resource lockers */
	private static Lock pidLock = new Lock();
	private static Lock openFileLock = new Lock();
	private static Lock joinLock = new Lock();
	
	/** Console file: standard input & standard output */
	private OpenFile stdin = null;
	private OpenFile stdout = null;
}
