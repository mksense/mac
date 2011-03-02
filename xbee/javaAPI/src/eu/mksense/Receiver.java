package eu.mksense;

import com.rapplogic.xbee.api.ApiId;
import com.rapplogic.xbee.api.PacketListener;
import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.api.wpan.RxResponse16;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Receiver Thread.
 */
public class Receiver extends Thread implements PacketListener {

    private final XBee thisXBee;

    private final HashMap<Integer, MessageListener> messageListeners;

    private final BlockingQueue<RxResponse16> queue;

    public Receiver(final XBee xbee) {
        thisXBee = xbee;
        thisXBee.addPacketListener(this);
        queue = new LinkedBlockingQueue<RxResponse16>();
        messageListeners = new HashMap<Integer, MessageListener>();
    }

    @Override
    public void run() {
        super.run();

        while (true) {

            try {
                final RxResponse16 response = queue.take();
                //messageListeners.get(110).receive(response);
                //Check if a valid RxResponse16 message.
                if (response.getData()[0] == XBeeRadio.LP1
                        && response.getData()[1] == XBeeRadio.LP2) {
                    //Forward the message to the registered Listeners.
                    final int port = response.getData()[2];
                    if (messageListeners.containsKey(port)) {
                        int[] tmpArray = new int[response.getData().length - 3];
                        System.arraycopy(response.getData(), 3, tmpArray, 0, tmpArray.length);
                        response.setData(tmpArray);
                        messageListeners.get(port).receive(response);
                    }
                } else {

                    final StringBuffer intToStr = new StringBuffer();
                    for (int b : response.getData()) {
                        intToStr.append((char) b);
                    }
                    System.out.println(response.getData() + "\n\t\t" + intToStr.toString());

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Implementation of xbee.PacketListener to get notifications.
     *
     * @param response
     */
    public void processResponse(XBeeResponse response) {
     System.out.println(response.toString());

        if (response.getApiId() == ApiId.RX_16_RESPONSE) {
            queue.offer((RxResponse16) response);
        }
    }

    /**
     * Add listeners to the Receiver Thread on a specific port.
     *
     * @param port    the port number to listen to.
     * @param msgList the MessageListener Object.
     */
    public void addMessageListener(final int port, final MessageListener msgList) {
        messageListeners.put(port, msgList);
    }
}
