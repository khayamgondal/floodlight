package net.floodlightcontroller.sos.web;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import net.floodlightcontroller.sos.ISOSService;
import net.floodlightcontroller.sos.ISOSService.SOSReturnCode;
import net.floodlightcontroller.sos.ISOSWhitelistEntry;
import net.floodlightcontroller.sos.SOSWhitelistEntry;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.TransportPort;
import org.restlet.data.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class WhitelistResource extends ServerResource {
	protected static Logger log = LoggerFactory.getLogger(WhitelistResource.class);
	protected static final String STR_OPERATION_ADD = "add";	
	protected static final String STR_OPERATION_REMOVE = "remove";

	protected static final String STR_SERVER_IP = "server-ip-address";
	protected static final String STR_SERVER_PORT = "server-tcp-port";
	protected static final String STR_CLIENT_IP = "client-ip-address";
	protected static final String STR_START_TIME = "start-time";
	protected static final String STR_STOP_TIME = "stop-time";

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Get
	public Object getWhitelistEntries() {
		Series<Header> responseHeaders = (Series<Header>) getResponse().getAttributes().get("org.restlet.http.headers"); 
	    getResponse().getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
	    if (responseHeaders == null) {
	    	responseHeaders = new Series(Header.class);
	    	getResponse().getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS, responseHeaders);
	    }
	    responseHeaders.add(new Header("Access-Control-Allow-Origin", "http://openflow.sites.clemson.edu")); 
		
		ISOSService sosService = (ISOSService) getContext().getAttributes().get(ISOSService.class.getCanonicalName());

		return sosService.getWhitelistEntries();
	}
	
	@Put
	@Post
	public Map<String, String> handleWhitelist(String json) {
		ISOSService sosService = (ISOSService) getContext().getAttributes().get(ISOSService.class.getCanonicalName());
		String operation = ((String) getRequestAttributes().get(SOSWebRoutable.STR_OPERATION)).toLowerCase().trim();
		
		Map<String, String> ret = new HashMap<String, String>();

		ISOSWhitelistEntry entry = parseWhitelistEntryFromJson(json);
		if (entry == null) {
			ret.put(Code.CODE, Code.ERR_JSON);
			ret.put(Code.MESSAGE, "Error: Could not parse JSON.");
		} else if (operation.equals(STR_OPERATION_ADD)) {
			SOSReturnCode rc = sosService.addWhitelistEntry(entry);
			switch (rc) {
			case WHITELIST_ENTRY_ADDED:
				ret.put(Code.CODE, Code.OKAY);
				ret.put(Code.MESSAGE, "WhitelistEntry successfully added. The entry may initiate the data transfer.");
				break;
			case ERR_DUPLICATE_WHITELIST_ENTRY:
				ret.put(Code.CODE, Code.ERR_DUPLICATE);
				ret.put(Code.MESSAGE, "Error: A duplicate entry was detected. Unable to add entry to SOS.");
				break;
			default:
				ret.put(Code.CODE, Code.ERR_BAD_ERR_CODE);
				ret.put(Code.MESSAGE, "Error: Unexpected error code " + rc.toString() + ". WhitelistEntry was not added.");
			}
		} else if (operation.equals(STR_OPERATION_REMOVE)) {
			SOSReturnCode rc = sosService.removeWhitelistEntry(entry);
			switch (rc) {
			case WHITELIST_ENTRY_REMOVED:
				ret.put(Code.CODE, Code.OKAY);
				ret.put(Code.MESSAGE, "WhitelistEntry successfully removed. Any ongoing data transfers for this entry will persist until their individual termination.");
				break;
			case ERR_UNKNOWN_WHITELIST_ENTRY:
				ret.put(Code.CODE, Code.ERR_NOT_FOUND);
				ret.put(Code.MESSAGE, "Error: The entry specified was not found. Unable to remove entry from SOS.");
				break;
			default:
				ret.put(Code.CODE, Code.ERR_BAD_ERR_CODE);
				ret.put(Code.MESSAGE, "Error: Unexpected error code " + rc.toString() + ". WhitelistEntry was not removed.");
			}
		} else {
			ret.put(Code.CODE, Code.ERR_UNDEF_OPERATION);
			ret.put(Code.MESSAGE, "Error: Undefined operation " + operation);
		}

		return ret;
	}

	/**
	 * Expect JSON:
	 * {
	 * 		"server-ip-address"		:	"valid-ip-address",
	 * 		"sever- tcp-port"		:	"valid-tcp-port",
	 * 		"client-ip-address"		:	"valid-ip-address",
	 * }
	 * 
	 * @param json
	 * @return
	 */
	@SuppressWarnings("deprecation")
	private static ISOSWhitelistEntry parseWhitelistEntryFromJson(String json) {
		MappingJsonFactory f = new MappingJsonFactory();
		JsonParser jp;

		IPv4Address serverIp = IPv4Address.NONE;
		TransportPort serverPort = TransportPort.NONE;
		IPv4Address clientIp = IPv4Address.NONE;
		Date startTime = ISOSWhitelistEntry.NO_RESERVTION;
		Date stopTime = ISOSWhitelistEntry.NO_RESERVTION;

		if (json == null || json.isEmpty()) {
			return null;
		}

		try {
			try {
				jp = f.createParser(json);
			} catch (JsonParseException e) {
				throw new IOException(e);
			}

			jp.nextToken();
			if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
				throw new IOException("Expected START_OBJECT");
			}

			while (jp.nextToken() != JsonToken.END_OBJECT) {
				if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
					throw new IOException("Expected FIELD_NAME");
				}

				String key = jp.getCurrentName().toLowerCase().trim();
				jp.nextToken();
				String value = jp.getText().toLowerCase().trim();
				if (value.isEmpty() || key.isEmpty()) {
					continue;
				} else if (key.equals(STR_SERVER_IP)) {
					try {
						serverIp = IPv4Address.of(value);
					} catch (IllegalArgumentException e) {
						log.error("Invalid IPv4 address {}", value);
					}
				} else if (key.equals(STR_SERVER_PORT)) {
					try {
						serverPort = TransportPort.of(Integer.parseInt(value));
					} catch (IllegalArgumentException e) {
						log.error("Invalid port {}", value);
					}
				} else if (key.equals(STR_CLIENT_IP)) {
					try {
						clientIp = IPv4Address.of(value);
					} catch (IllegalArgumentException e) {
						log.error("Invalid IPv4 address {}", value);
					}
				} else if (key.equals(STR_START_TIME)) {
					try {
						startTime = new Date(value); //TODO
					} catch (IllegalArgumentException e) {
						log.error("Invalid start time {}", value);
					}
				} else if (key.equals(STR_STOP_TIME)) {
					try {
						stopTime = new Date(value); //TODO
					} catch (IllegalArgumentException e) {
						log.error("Invalid stop time {}", value);
					}
				}
			}
		} catch (IOException e) {
			log.error("Error parsing JSON into SOSWhitelistEntry {}", e);
		}
		
		if (!serverIp.equals(IPv4Address.NONE) && !serverPort.equals(TransportPort.NONE) && !clientIp.equals(IPv4Address.NONE) ) {
			return SOSWhitelistEntry.of(serverIp, serverPort, clientIp, startTime, stopTime);
		} else {
			return null;
		}
	}
}