import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Created by otonbraga on 07/04/16.
 */
public class History extends Hashtable{

    long historyCleanedTo = -1L;
    public static final int MAX_HISTORY = 1024;
    private Hashtable senders = new Hashtable();

    public synchronized void noteReceived(String sender, long recvd)
    {
        this.senders.put(sender, new Long(recvd));
    }

    public synchronized void addMsg(String sender, long sequence, byte[] msg) {
        if (msg != null) {
            put(new Long(sequence), msg);
        }
        if (size() > 1024) {
            System.out.println("CLEAN HISTORY; size: " + size());
            long min = Long.MAX_VALUE;
            Enumeration enumeration = this.senders.keys();

            while (enumeration.hasMoreElements()) {
                String sent = (String)enumeration.nextElement();
                Long got = (Long)this.senders.get(sent);
                long have = got.longValue();
                System.out.println(sent + " has received " + have);
                if (have < min) {
                    min = have;
                }
            }
            System.out.println("clean from " + (this.historyCleanedTo + 1L) + " to " + min);
            for (long s = this.historyCleanedTo + 1L; s <= min; s += 1L) {
                remove(new Long(s));
                this.historyCleanedTo = s;
            }
            System.out.println("CLEANED HISTORY; size is now " + size());
        }
    }

    public byte[] getMsg(long sequence) {
        return (byte[])get(new Long(sequence));
    }

    public synchronized void eraseSender(String sender) {
        this.senders.remove(sender);
    }
}
