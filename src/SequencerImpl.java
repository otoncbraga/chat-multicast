import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Vector;

/**
 * Created by otonbraga on 07/04/16.
 */
public class SequencerImpl implements Sequencer {

    public static final int MAX_MSG_LENGTH = 1024;
    public static final int GROUP_PORT = 1235;
    private static final String ipAddr = "228.5.6.7";
    private String name;
    public static long sequence = 0;
    MulticastSocket socket;
    InetAddress groupAddr;
    History history;
    Vector mySenders;

    // lista de destinos <------- sequencerjoininfo


    public SequencerImpl (String name) {
        this.name = name;
        try {
         this.history = new History();
          this.mySenders = new Vector();
          this.socket = new MulticastSocket();
          this.groupAddr = InetAddress.getByName("228.5.6.7");
        }
        catch (Exception ex) {
          System.out.println("Couldn't initialise seq: " + ex);
        }
    }

    // join -- request for "sender" to join sequencer's multicasting service;
    // returns an object specifying the multicast address and the first sequence number to expect
    @Override
    public SequencerJoinInfo join(String sender) throws RemoteException, SequencerException {

        // esse sender é o ip do cliente (remetente), logo eu devo criar um joinIfo a partir dele
        //return new SequencerJoinInfo(InetAddress.getByName(sender), sequence);

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
    public void send(String sender, byte[] msg, long msgID, long lastSequenceReceived) throws RemoteException {

        //eu preciso enviar a mensagem com o id certo e esperar um ack, cabe saber se devo usar o msgHandler
//        if (lastSequenceReceived == sequence) {
//            sequence++;
//        }

        try {
          ByteArrayOutputStream bstream = new ByteArrayOutputStream(10240);
          DataOutputStream dstream = new DataOutputStream(bstream);
          dstream.writeLong(++this.sequence);
          dstream.write(msg, 0, msg.length);

          this.socket.send(
              new DatagramPacket(
              bstream.toByteArray(),
             bstream.size(),
              this.groupAddr,
              10000));

        } catch (Exception ex) {
          System.out.println("problem sending by sequ " + ex);
        }
        this.history.noteReceived(sender, lastSequenceReceived);
        this.history.addMsg(sender, this.sequence, msg);
    }

    // leave -- tell sequencer that "sender" will no longer need its services
    @Override
    public void leave(String sender) throws RemoteException {
        this.mySenders.removeElement(sender);
        this.history.eraseSender(sender);
    }

    // getMissing -- ask sequencer for the message whose sequence number is "sequence"
    @Override
    public byte[] getMissing(String sender, long sequence) throws RemoteException, SequencerException {

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
    public void heartbeat(String sender, long lastSequenceReceived) throws RemoteException {
        System.out.println(sender + " HEARTBEAT: " + lastSequenceReceived);
        this.history.noteReceived(sender, lastSequenceReceived);
    }

    public static void main (String args[]) throws MalformedURLException, RemoteException {
        Naming.rebind("equipe3", new SequencerImpl("abestados"));
        System.out.println("data a lagata");
    }
}
