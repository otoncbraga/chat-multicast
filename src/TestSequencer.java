import java.awt.*;
import java.rmi.RemoteException;
import java.util.Random;

/**
 * Created by otonbraga on 07/04/16.
 */
public class TestSequencer implements Runnable, Group.MsgHandler {

    Group group;
    Thread t;
    Scrollbar slider;
    Button stopIt;
    Button pauseIt;
    TextField text;
    int rate;
    String myName;
    boolean paused = false;

    String[] nomes = {"hdieiuw", "jdnoew", "ndowe", "noduew", "dienow", "nodewn", "ndoew", "ndeuwon", "bdyew", "bdiew","hdiuwdew"};
    Random gerador = new Random();

    public TestSequencer(String host, String myName) {
        //super("TestSequencer");
        //setSize(200, 200);
        this.myName = myName;
//        this.slider = new Scrollbar(0, 0, 10, 0, 100);
//        this.slider.addAdjustmentListener(this);
//        this.rate = this.slider.getValue();
//
//        this.stopIt = new Button("Quit");
//        this.stopIt.addActionListener(this);
//
//        this.pauseIt = new Button("Pause/Continue");
//        this.pauseIt.addActionListener(this);
//
//        this.text = new TextField(80);
//        setLayout(new BorderLayout());
//        add("North", this.text);
//        add("East", this.stopIt);
//        add("West", this.pauseIt);
//        add("South", this.slider);

        try {
            this.group = new Group(host, this, myName);
            this.t = new Thread(this);
            this.t.start();
        } catch (Exception ex) {
            System.out.println("Can't create group: " + ex);
        }
  }

//    @Override
//    public void actionPerformed(ActionEvent e) {
//        if (e.getSource() == this.stopIt) {
//            this.group.leave("");
//            System.exit(1);
//        } else if (e.getSource() == this.pauseIt) {
//            this.paused = (!this.paused);
//        }
//    }
//
//    @Override
//    public void adjustmentValueChanged(AdjustmentEvent e) {
//        this.rate = this.slider.getValue();
//        this.slider.setValue(this.rate);
//    }

    @Override
    public void handle(int count, byte[] msg) {
        //this.text.setText(new String(msg, 0, count));
        System.out.println(new String(msg, 0, count));
    }

    @Override
    public void run() {
        try {
          int i = 0;
//          for (;;) {
//              if (this.rate <= 90) {
//                  try {
//                      Thread.sleep((90 - this.rate) * 10);
//                  } catch (Exception localException1) {}
//              }
//              if (!this.paused) {
                  this.group.send(new String( nomes[gerador.nextInt(11)] + ": " + i++).getBytes());

              //}
          //}
        }
        catch (Exception ex)
        {
            System.out.println("Applet exception " + ex);
        }
    }

    public static void main(String args[]) throws RemoteException {
        TestSequencer st = new TestSequencer("localhost", "/TKSequencer");
        st.run();
    }
}
