package net.floodlightcontroller.sos.web;

import net.floodlightcontroller.core.IListener;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.sos.*;
import org.restlet.resource.Delete;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionResource extends ServerResource {
    protected static Logger log = LoggerFactory.getLogger(ServerResource.class);


    @Delete
    public void deleteConnection(String json) {
        ISOSService sosService = (ISOSService) getContext().getAttributes().get(ISOSService.class.getCanonicalName());
        ISOSTerminationStats stats = SOSTerminationStats.parseFromJson(json);
        SOSConnections conns = sosService.getSosConnections();
        SOSConnection conn = conns.getConnection(stats.getTransferID());
        if (conn == null) {
            log.error("Could not locate UUID {} in connection storage. Report SOS bug", stats.getTransferID());
        }
        if (conn.getClientSideAgent().getActiveTransfers().contains(stats.getTransferID()) &&
                conn.getClientSideAgent().getIPAddr().equals(stats.getSourceAddress()) &&
                stats.isClientSideAgent()) {
            log.warn("Received termination message from client side agent {} for transfer ID {}", conn.getClientSideAgent().getIPAddr(), stats.getTransferID());
            conn.getClientSideAgent().removeTransferId(stats.getTransferID());
            if (stats.getSentBytesAvg() != 0) { /* only record valid set of stats; dependent on direction of transfer */
                log.info("Setting stats for client side agent {} for transfer ID {}", conn.getClientSideAgent().getIPAddr(), stats.getTransferID());
                conn.setTerminationStats(stats);
            }
            /* continue; we might have just removed the 2nd agent */
        } else if (conn.getServerSideAgent().getActiveTransfers().contains(stats.getTransferID()) &&
                conn.getServerSideAgent().getIPAddr().equals(l3.getSourceAddress()) &&
                !stats.isClientSideAgent()) {
            log.warn("Received termination message from server side agent {} for transfer ID {}", conn.getServerSideAgent().getIPAddr(), stats.getTransferID());
            conn.getServerSideAgent().removeTransferId(stats.getTransferID());
            if (stats.getSentBytesAvg() != 0) { /* only record valid set of stats; dependent on direction of transfer */
                log.info("Setting stats for server side agent {} for transfer ID {}", conn.getServerSideAgent().getIPAddr(), stats.getTransferID());
                conn.setTerminationStats(stats);
            }
            /* continue; we might have just removed the 2nd agent */
        } else if (!conn.getServerSideAgent().getActiveTransfers().contains(stats.getTransferID()) &&
                !conn.getClientSideAgent().getActiveTransfers().contains(stats.getTransferID())) {
            log.error("Received termination message for transfer ID {} but both agents were already terminated. Report SOS bug.", stats.getTransferID());
         //   return IListener.Command.STOP;
        } else {
            log.error("SOS in inconsistent state when processing termination message. Report SOS bug. Transfer: {}", conn);
          //  return IListener.Command.STOP;
        }

    }

}
