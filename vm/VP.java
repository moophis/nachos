package nachos.vm;

/**
 * Virtual page number - PID pair.
 */
public class VP {
    public int pid;
    public int vpn;

    public VP(int vpn, int pid) {
        this.pid = pid;
        this.vpn = vpn;
    }

    @Override
    public String toString() {
        return "<vpn: " + vpn + ", pid: " + pid + "> ";
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof VP)) {
            return false;
        }
        VP that = (VP) obj;

        return this.pid == that.pid && this.vpn == that.vpn;
    }
}
