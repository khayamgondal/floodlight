package net.floodlightcontroller.sos;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.floodlightcontroller.sos.web.SOSServerSerializer;
import org.projectfloodlight.openflow.types.TransportPort;

@JsonSerialize(using=SOSServerSerializer.class)
public interface ISOSServer extends ISOSDevice {

	public TransportPort getTcpPort();
}