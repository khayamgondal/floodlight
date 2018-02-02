package net.floodlightcontroller.sos.web;

import net.floodlightcontroller.sos.ISOSService;
import org.restlet.data.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class StatusResource extends ServerResource {
	protected static Logger log = LoggerFactory.getLogger(StatusResource.class);

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Get
	public Map<String, String> handleModule(String json) {
		Series<Header> responseHeaders = (Series<Header>) getResponse().getAttributes().get("org.restlet.http.headers"); 
	    getResponse().getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
	    if (responseHeaders == null) {
	    	responseHeaders = new Series(Header.class);
	    	getResponse().getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS, responseHeaders);
	    }
	    responseHeaders.add(new Header("Access-Control-Allow-Origin", "http://openflow.sites.clemson.edu")); 
		
		ISOSService sosService = (ISOSService) getContext().getAttributes().get(ISOSService.class.getCanonicalName());
		
		Map<String, String> ret = new HashMap<String, String>();
		
		switch (sosService.ready()) {
		case READY:
			ret.put(Code.CODE, Code.OKAY);
			ret.put(Code.MESSAGE, "Ready to accept a transfer");
			break;
		case NOT_READY:
			ret.put(Code.CODE, Code.ERR_NOT_READY);
			ret.put(Code.MESSAGE, "Not ready to accept a transfer");
			break;
		default:
			ret.put(Code.CODE, Code.ERR_BAD_ERR_CODE);
			ret.put(Code.MESSAGE, "Received improper SOS error code");
			break;
		}
		return ret;
	}
}