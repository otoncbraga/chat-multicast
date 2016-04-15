import java.io.*;
import java.rmi.Naming;
import java.net.*;
import java.rmi.NotBoundException;
import java.util.Date;

/**
 * Created by otonbraga on 07/04/16.
 */
public class Group implements Runnable {

    Thread t;
    Thread heartBeater;
    Sequencer sequencer;
    MulticastSocket socket;
    Group.MsgHandler handler;
    long lastSequenceRecd = -1L;
    long lastSequenceSent = -1L;
    InetAddress groupAddr;
    InetAddress myAddr;
    String myName;
    long lastSendTime;


    public Group(String host, MsgHandler handler, String senderName) throws GroupException, IOException, NotBoundException {
       // contact Sequencer on "host" to join group,
       // create MulticastSocket and thread to listen on it,
       // perform other initialisations #

        try{
            String[] fred =
            Naming.list("//mpc2/");

            for (int i = 0; i < fred.length; i++) {
                System.out.println(String.valueOf(fred[i]));
            }

            this.myAddr = InetAddress.getLocalHost();
            this.sequencer =
            ((Sequencer)Naming.lookup("//" + host + "/equipe3"));
            this.myName = (senderName + this.myAddr);
            SequencerJoinInfo joinInfo = this.sequencer.join(this.myName);
            this.groupAddr = joinInfo.addr;
            this.lastSequenceRecd = joinInfo.sequence;
            System.out.println("ip of group: " + this.groupAddr);
            this.socket = new MulticastSocket(10000);
            this.socket.joinGroup(this.groupAddr);
            this.handler = handler;
            this.t = new Thread(this);
            this.t.start();
            this.heartBeater = new Group.HeartBeater(5);
            this.heartBeater.start();

        } catch (SequencerException ex) {
            System.out.println("Couldn't create group " + ex);
            throw new Group.GroupException(String.valueOf(ex));
        } catch (Exception ex) {
            System.out.println("Couldn't create group " + ex);
            throw new Group.GroupException("Couldn't join to sequencer");
        }
        SequencerJoinInfo joinInfo;
        int i;
        String[] fred;
    }

    public void send(byte[] msg) throws GroupException, IOException {
        // send the given message to all instances of Group using the same sequencer

        if (this.socket != null) {
            try {
                this.sequencer.send(this.myName, msg,++lastSequenceSent, this.lastSequenceRecd);
                this.lastSendTime = new Date().getTime();
            } catch (Exception ex) {
                System.out.println("couldn't contact sequencer " + ex);
                throw new Group.GroupException("Couldn't send to sequencer");
            }
        } else {
            throw new Group.GroupException("Group not joined");
        }
    }

    public void leave(String group) {
        // leave group
        if (this.socket != null) {
            try{
                this.socket.leaveGroup(this.groupAddr);
                this.sequencer.leave(this.myName);
            }catch (Exception ex){
                System.out.println("couldn't leave group " + ex);
            }
        }
    }

    public void run()
    {
        // repeatedly: listen to MulticastSocket created in constructor, and on receipt
        // of a datagram call "handle" on the instance
        // of Group.MsgHandler which was supplied to the constructor

        try{
            for (;;){
                byte[] buf = new byte[' '];
                DatagramPacket dgram = new DatagramPacket(buf, buf.length);
                this.socket.receive(dgram);

                ByteArrayInputStream bstream = new ByteArrayInputStream(buf, 0, dgram.getLength());
                DataInputStream dstream = new DataInputStream(bstream);
                long gotSequence = dstream.readLong();
                int count = dstream.read(buf);
                long wantSeq = this.lastSequenceRecd + 1L;
                if ((this.lastSequenceRecd >= 0L) && (wantSeq < gotSequence)) {
                    for (long getSeq = wantSeq; getSeq < gotSequence; getSeq += 1L) {
                        byte[] bufExtra = this.sequencer.getMissing("", getSeq);
                        int countExtra = bufExtra.length;
                        System.out.println("Group: fetch missing " + getSeq);
                        this.handler.handle(countExtra, bufExtra);
                    }
                }
                this.lastSequenceRecd = gotSequence;
                this.handler.handle(count, buf);
            }
        } catch (Exception ex){
            System.out.println("bad in run " + ex);
        }
    }

    public interface MsgHandler
    {
         public void handle(int count, byte[] msg);
    }

    public class GroupException extends Exception
    {
        public GroupException(String s)
        {
            super(s);
        }
    }

    public class HeartBeater extends Thread
    {
        // This thread sends heartbeat messages when required
        int period;

        public HeartBeater(int period) {
            this.period = period;
        }


        public void run(){
        try{
            for (;;){
                Thread.sleep(this.period * 1000);
                if (new Date().getTime() - lastSendTime >= this.period * 1000) {
                    sequencer.heartbeat(myName, lastSequenceRecd);
                }
            }
        }
        catch (Exception localException) {}
        }
    }

    private Sequencer getSequencer(String sequencerHost) throws Exception {
        return (Sequencer)Naming.lookup("rmi://" + sequencerHost + "/sequencer");
    }
}
