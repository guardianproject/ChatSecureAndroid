package info.guardianproject.otr.app.im;

import info.guardianproject.otr.app.im.engine.ConnectionConfig;

import java.util.Map;


public class ImpsConnectionConfig extends ConnectionConfig {

	public ImpsConnectionConfig(Map<String, String> settings) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getProtocolName() {
		return "DUMMY";
	}

}
