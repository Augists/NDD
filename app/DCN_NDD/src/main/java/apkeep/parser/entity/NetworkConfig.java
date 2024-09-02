package apkeep.parser.entity;

import java.util.*;

public class NetworkConfig {
	
	String network_name;
	public Hashtable<String, DeviceConfig> devices;
	// subnet 2 nves
	public HashMap<String,HashSet<String>> subnets;
	public HashMap<String,HashSet<String>> epg_fields;// epg_id 2 subnets
	
	public NetworkConfig() {
		network_name = null;
		devices = new Hashtable<String, DeviceConfig>();
		subnets = new HashMap<String,HashSet<String>>();
		epg_fields = new HashMap<String,HashSet<String>>();
	}
	
	public NetworkConfig(String name)
	{
		network_name = name;
		devices = new Hashtable<String, DeviceConfig>();
		subnets = new HashMap<String,HashSet<String>>();
		epg_fields = new HashMap<String,HashSet<String>>();
	}
	
	@Override
	public String toString() {
		return network_name;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return network_name;
	}

	public Collection<DeviceConfig> GetDevices() {
		// TODO Auto-generated method stub
		return devices.values();
	}
}