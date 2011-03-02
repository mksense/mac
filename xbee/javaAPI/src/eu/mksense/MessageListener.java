package eu.mksense;

import com.rapplogic.xbee.api.wpan.RxResponse16;

/**
 * Interface to be implemented in order to receive incoming Messages.
 */
public interface MessageListener {

    public void receive(RxResponse16 response);
}