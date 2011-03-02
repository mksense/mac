package eu.mksense;

import com.rapplogic.xbee.api.*;
import com.rapplogic.xbee.api.wpan.TxRequest16;
import com.rapplogic.xbee.api.wpan.TxStatusResponse;

/**
 * Created by IntelliJ IDEA.
 * User: akribopo
 * Date: Nov 22, 2010
 * Time: 3:29:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class XBeeRadio {

    public static final char LP1 = 0x7f;
    public static final char LP2 = 0x69;

    private int my_address;

    /**
     * Broadcast address.
     */
    public static final XBeeAddress16 BROADCAST = new XBeeAddress16(0xFF, 0xFF);

    /**
     * An instance of the defautl Xbee.
     */
    private final XBee xbee;

    /**
     * The private instance of the XBeeRadio.
     */
    private static XBeeRadio instance;


    /**
     * Receives incoming messages.
     */
    private Receiver messageReceiver;

    /**
     * Channel.
     */
    private int channel = 12;

    /**
     * Channel.
     */
    private int panId = 1;


    private static final int apiMode = 2;

    private static final int macMode = 2;
    
    private XBeeAddress16 myXbeeAddress;

    /**
     * @return XBeeRadio instance
     */
    public static synchronized XBeeRadio getInstance() {
        if (instance == null) {
            instance = new XBeeRadio();
        }

        return instance;
    }

    /**
     * Default constructor.
     */
    private XBeeRadio() {
        xbee = new XBee();

    }

    /**
     * Open a connection to the given port with specific BaudRate.
     *
     * @param port
     * @param baudRate
     * @throws Exception
     */
    public void open(final String port, final int baudRate) throws Exception {
        xbee.open(port, baudRate);
        initialize();
    }

    /**
     * Initialize the XBee.
     *
     * @throws Exception
     */
    private void initialize() throws Exception {
        xbee.sendAtCommand(new AtCommand("AP", apiMode));
        xbee.sendAtCommand(new AtCommand("CH", channel));
        xbee.sendAtCommand(new AtCommand("MM", macMode));
        xbee.sendAtCommand(new AtCommand("ID", panId));
        //xbee.sendAtCommand(new AtCommand("BD", 5));

        final AtCommandResponse slResponse = xbee.sendAtCommand(new AtCommand("SL"));
        int myHigh = slResponse.getValue()[2];
        myHigh = myHigh << 8;
        myHigh += slResponse.getValue()[3];
        my_address = myHigh;
        xbee.sendAtCommand(new AtCommand("MY", new int[]{slResponse.getValue()[2], slResponse.getValue()[3]}));
        messageReceiver = new Receiver(xbee);

        myXbeeAddress = new XBeeAddress16(slResponse.getValue()[2], slResponse.getValue()[3]);


    }

    /**
     * Sends a message.
     *
     * @param remoteAddr16
     * @param port
     * @param payload
     */
    public void send(XBeeAddress16 remoteAddr16, int port, int[] payload) throws Exception {
        final int[] buffer = new int[payload.length + 3];
        buffer[0] = LP1;
        buffer[1] = LP2;
        buffer[2] = port;

        System.arraycopy(payload, 0, buffer, 3, payload.length);

        if (remoteAddr16 == XBeeRadio.BROADCAST) {
            xbee.sendAsynchronous(new TxRequest16(remoteAddr16, buffer));
        } else {
            final TxStatusResponse txResp = (TxStatusResponse) xbee.sendSynchronous(new TxRequest16(remoteAddr16, buffer));
            if (!txResp.isSuccess()) {
                throw new Exception("Returned a " + txResp.getStatus() + " Exception while sending to " + remoteAddr16.toString());
            }

        }
    }

    /**
     * Returns an int with the 16-bit address.
     *
     * @return an int
     */
    public int getMyAddress() {
        return my_address;
    }


    public void addMessageListener(final int port, final MessageListener messageListener) {
        if (!messageReceiver.isAlive()) {
            messageReceiver.start();
        }
        messageReceiver.addMessageListener(port, messageListener);
    }

    public void setChannel(final int chan) throws Exception {
        try {
            final AtCommandResponse resp = xbee.sendAtCommand(new AtCommand("CH", chan));
            if (!resp.isOk()) {
                throw new Exception("Exception: Channel didn't changed -- status:" + resp.toString());
            } else {
                this.channel = chan;
            }
        } catch (XBeeException e) {
            e.printStackTrace();
        }

    }

    public int getChannel() {
        return channel;
    }

    public int getPanId() {
        return panId;
    }

    public void setPanId(int panId) throws Exception {
        try {
            final AtCommandResponse resp = xbee.sendAtCommand(new AtCommand("ID", panId));
            if (!resp.isOk()) {
                throw new Exception("Exception: Pan ID didn't changed -- status:" + resp.toString());
            } else {
                this.panId = panId;
            }
        } catch (XBeeException e) {
            e.printStackTrace();
        }
    }

    public XBeeAddress16 getMyXbeeAddress() {
        return myXbeeAddress;
    }
}
