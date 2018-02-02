package net.floodlightcontroller.sos;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.floodlightcontroller.sos.web.SOSStatisticsSerializer;

import java.util.Collection;

@JsonSerialize(using=SOSStatisticsSerializer.class)
public interface ISOSStatistics {
		
	public Collection<ISOSWhitelistEntry> getWhitelistEntries();
	
	public Collection<ISOSConnection> getActiveConnections();
	
	public Collection<ISOSConnection> getTerminatedConnections();
	
	public Collection<ISOSAgent> getAgents();
}