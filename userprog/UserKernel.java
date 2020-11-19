package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.util.LinkedList;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
	super();
    }

    static LinkedList<Integer> freePages;
    static boolean[] pageState;
    static int pageNum;
    static Lock pageLock;

    public static void initPages(){
	pageNum=Machine.processor().getNumPhysPages();
	freePages=new LinkedList<Integer>();
	pageState=new boolean[pageNum];
	for (int i=0; i<pageNum; i++){
	    freePages.add(i);
	    pageState[i]=true;
	}
	pageLock=new Lock();
    }

    public static int allocate(){
	pageLock.acquire();
	if (freePages.isEmpty()){
	    pageLock.release();
	    return -1;
	}
	else{
	    int ret=freePages.pop();
	    pageState[ret]=false;
	    pageLock.release();
	    return ret;
	}
    }

    public static int deallocate(int i){
	pageLock.acquire();
	if ((i<0||i>=pageNum)||pageState[i]){
	    pageLock.release();
	    return -1;
	}
	else{
	    pageState[i]=true;
	    freePages.push(i);
	    pageLock.release();
	    return 0;
	}
    }
	    

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    public void initialize(String[] args) {
	super.initialize(args);

	console = new SynchConsole(Machine.console());
	
	Machine.processor().setExceptionHandler(new Runnable() {
		public void run() { exceptionHandler(); }
	    });
	initPages();
    }

    /**
     * Test the console device.
     */	
    public void selfTest() {
	super.selfTest();

	System.out.println("Testing allocation");
	int a=allocate(),b=allocate();
	System.out.println("allocate "+a+" "+b);
	System.out.println("deallocate(0 or -1) "+deallocate(a));
	System.out.println("deallocate again "+deallocate(a));
	a=allocate();
	System.out.println("allocate "+a);
	System.out.println("deallocate(0 or -1) "+deallocate(a)+" "+deallocate(b));

	System.out.println("Testing the console device. Typed characters");
	System.out.println("will be echoed until q is typed.");

	char c;

	do {
	    c = (char) console.readByte(true);
	    console.writeByte(c);
	}
	while (c != 'q');

	System.out.println("");
    }

    /**
     * Returns the current process.
     *
     * @return	the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
	if (!(KThread.currentThread() instanceof UThread))
	    return null;
	
	return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
	Lib.assertTrue(KThread.currentThread() instanceof UThread);

	UserProcess process = ((UThread) KThread.currentThread()).process;
	int cause = Machine.processor().readRegister(Processor.regCause);
	process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see	nachos.machine.Machine#getShellProgramName
     */
    public void run() {
	super.run();

	UserProcess process = UserProcess.newUserProcess();
	
	String shellProgram = Machine.getShellProgramName();
	boolean result = process.execute(shellProgram, new String[] { });
	Lib.assertTrue(result);

	KThread.currentThread().finish();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }

    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;

    // dummy variables to make javac smarter
    private static Coff dummy1 = null;
}
