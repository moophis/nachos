package nachos.vm;

/**
 * Virtual page number - PID pair.
 */
public class VP {
    private int pid;
    private int vpn;

    public VP(int vpn, int pid) {
        this.pid = pid;
        this.vpn = vpn;
    }
}
