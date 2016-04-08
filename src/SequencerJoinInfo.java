import java.io.*;
import java.net.*;

/**
 * Created by otonbraga on 07/04/16.
 */
public class SequencerJoinInfo implements Serializable{
    public InetAddress addr;
    public long sequence;

    public SequencerJoinInfo(InetAddress addr, long sequence)
    {
        this.addr = addr;
        this.sequence = sequence;
    }
}
