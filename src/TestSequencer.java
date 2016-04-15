import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

/**
 * Created by otonbraga on 07/04/16.
 */
public class TestSequencer extends Frame implements Runnable, Group.MsgHandler, AdjustmentListener, ActionListener {

    String returned = "Fred";
    Group group;
    Thread t;
    Scrollbar slider;
    Button stopIt;
    Button pauseIt;
    TextField text;
    int rate;
    String myName;
    boolean paused = false;

    public TestSequencer(String host, String myName) {
        super("SeqTest");
        setSize(200, 200);
        this.myName = myName;
        this.slider = new Scrollbar(0, 0, 10, 0, 100);
        this.slider.addAdjustmentListener(this);
        this.rate = this.slider.getValue();

        this.stopIt = new Button("Quit");
        this.stopIt.addActionListener(this);

        this.pauseIt = new Button("Pause/Continue");
        this.pauseIt.addActionListener(this);

        this.text = new TextField(80);
        setLayout(new BorderLayout());
        add("North", this.text);
        add("East", this.stopIt);
        add("West", this.pauseIt);
        add("South", this.slider);

        try {
            this.group = new Group(host, this, myName);
            this.t = new Thread(this);
            this.t.start();
        } catch (Exception ex) {
            System.out.println("Can't create group: " + ex);
        }
  }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.stopIt) {
            this.group.leave("");
            System.exit(1);
        } else if (e.getSource() == this.pauseIt) {
            this.paused = (!this.paused);
        }
    }

    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
        this.rate = this.slider.getValue();
        this.slider.setValue(this.rate);
    }

    @Override
    public void handle(int count, byte[] msg) {
        this.text.setText(new String(msg, 0, count));
    }

    @Override
    public void run() {
        try {
          int i = 0;
          for (;;) {
              if (this.rate <= 90) {
                  try {
                      Thread.sleep((90 - this.rate) * 10);
                  } catch (Exception localException1) {}
              }
              if (!this.paused) {
                  this.group.send(new String(this.myName + ": " + i++).getBytes());
              }
          }
        }
        catch (Exception ex)
        {
            System.out.println("Applet exception " + ex);
        }
    }

    public static void main(String args[]) {
        if (args.length < 2)
        {
            System.out.println("Usage: prog host clientName");
        }
        else
        {
            TestSequencer st = new TestSequencer("localhost", "oidim");
            st.run();
        }
    }
}
