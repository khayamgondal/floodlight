package net.floodlightcontroller.sos;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.floodlightcontroller.sos.web.SOSClientSerializer;
import org.projectfloodlight.openflow.types.TransportPort;

@JsonSerialize(using=SOSClientSerializer.class)
public interface ISOSClient extends ISOSDevice {
	
	public TransportPort getTcpPort();
}
