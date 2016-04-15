import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;

/**
 * Created by otonbraga on 07/04/16.
 */
public class SequencerImpl extends UnicastRemoteObject implements Sequencer {

    public static final int MAX_MSG_LENGTH = 10240;
    public static final int GROUP_PORT = 10000;
    //private static final String ipAddr = "228.5.6.7";
    private String name;
    public static long sequence = 0;
    MulticastSocket socket;
    InetAddress groupAddr;
    History history;
    Vector mySenders;

    public SequencerImpl (String name) throws RemoteException {
        this.name = name;
        try {
            this.history = new History();
            this.mySenders = new Vector();
            this.socket = new MulticastSocket();
            this.groupAddr = InetAddress.getByName("228.5.6.7");
            System.out.println("passou aqui!");
        } catch (Exception ex) {
            System.out.println("Couldn't initialise seq: " + ex);
        }
    }

    // join -- request for "sender" to join sequencer's multicasting service;
    // returns an object specifying the multicast address and the first sequence number to expect
    @Override
    public synchronized SequencerJoinInfo join(String sender) throws RemoteException, SequencerException {

        // esse sender é o ip do cliente (remetente), logo eu devo criar um joinIfo a partir dele
        //return new SequencerJoinInfo(InetAddress.getByName(sender), sequence);

        System.out.println("join");

        if (this.mySenders.contains(sender)) {
            throw new SequencerException(sender + " not unique");
        }
        this.mySenders.addElement(sender);
        this.history.noteReceived(sender, this.sequence);
        return new SequencerJoinInfo(this.groupAddr, this.sequence);
    }

    // send -- "sender" supplies the msg to be sent, its identifier,
    // and the sequence number of the last received message
    @Override
    public synchronized void send(String sender, byte[] msg, long msgID, long lastSequenceReceived) throws RemoteException {

        System.out.println("send");

        try {
            ByteArrayOutputStream bstream = new ByteArrayOutputStream(10240);
            DataOutputStream dstream = new DataOutputStream(bstream);
            dstream.writeLong(++this.sequence);
            dstream.write(msg, 0, msg.length);
            System.out.println("AQUI");

            this.socket.send( new DatagramPacket(bstream.toByteArray(), bstream.size(), this.groupAddr, 10000));
        } catch (Exception ex) {
            System.out.println("problem sending by sequ " + ex);
        }
        this.history.noteReceived(sender, lastSequenceReceived);
        this.history.addMsg(sender, this.sequence, msg);
    }

    // leave -- tell sequencer that "sender" will no longer need its services
    @Override
    public synchronized void leave(String sender) throws RemoteException {
        System.out.println("leave");
        this.mySenders.removeElement(sender);
        this.history.eraseSender(sender);
    }

    // getMissing -- ask sequencer for the message whose sequence number is "sequence"
    @Override
    public byte[] getMissing(String sender, long sequence) throws RemoteException, SequencerException {

        System.out.println("getMissing");
        byte[] found = this.history.getMsg(sequence);

        if (found != null) {
            System.out.println("Sequencer supplies missing " + sequence);
            return found;
        }
        System.out.println("Sequencer couldn't find missing " + sequence);
        throw new SequencerException("Couldn't find missing " + sequence);
    }

    // heartbeat -- we have received messages up to number "lastSequenceReceived"
    @Override
    public synchronized void heartbeat(String sender, long lastSequenceReceived) throws RemoteException {
        System.out.println("heartbeat");
        System.out.println(sender + " HEARTBEAT: " + lastSequenceReceived);
        this.history.noteReceived(sender, lastSequenceReceived);
    }

    public static void main(String args[]) throws MalformedURLException, RemoteException {
        System.setProperty("java.rmi.server.hostname", "169.254.88.7");
        LocateRegistry.createRegistry(1099);
        Naming.rebind("/TKSequencer", new SequencerImpl("TKSequencer"));
        System.out.println("data a lagata");
    }
}
