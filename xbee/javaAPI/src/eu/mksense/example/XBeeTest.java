package eu.mksense.example;

import com.rapplogic.xbee.api.wpan.RxResponse16;
import eu.mksense.MessageListener;
import eu.mksense.XBeeRadio;

/**
 * Example of XBeeRadio.
 */
public class XBeeTest implements MessageListener {


    public void receive(RxResponse16 response) {
        System.out.println(response.toString());
        System.out.println(new String(response.getData().toString()));
    }

    public static void main(String[] args) throws Exception {
        XBeeRadio.getInstance().open("/dev/ttyUSB0", 38400);
        XBeeTest test = new XBeeTest();

       XBeeRadio.getInstance().addMessageListener(111, test);


       XBeeRadio.getInstance().send(XBeeRadio.BROADCAST, 111, new int[2]);
       //XBeeRadio.getInstance().send(XBeeRadio.BROADCAST, 111, new int[2]);
       //XBeeRadio.getInstance().send(XBeeRadio.BROADCAST,111, new int[2]);
        

        

    }
}
