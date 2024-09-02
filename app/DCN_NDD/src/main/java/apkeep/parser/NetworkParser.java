package apkeep.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.json.JSONObject;
import org.json.JSONTokener;

import apkeep.core.ChangeItem;
// import apkeep.elements.ACLElement;
// import apkeep.elements.Element;
// import apkeep.elements.ForwardElement;
import apkeep.parser.entity.DeviceConfig;
import apkeep.parser.entity.FWRule;
import apkeep.parser.entity.Interface;
import apkeep.parser.entity.MCSRule;
import apkeep.parser.entity.NetworkConfig;
import apkeep.utils.BDDACLWrapper;
import apkeep.utils.Parameters;
import common.ACLRule;
import common.ACLUse;
import javafx.util.Pair;

public class NetworkParser {
	
	public NetworkParser(NetworkConfig dcn_config) {
		// TODO Auto-generated constructor stub
	}

	public NetworkParser(NetworkConfig network, String input_path, String type) throws IOException {
		
		/*
		 *  parse config
		 */
		ParseConfig(network, input_path, type);
		
		/*
		 * parse route
		 * huawei device has its route in config file its own
		 */
		ParseRoute(network, input_path, type);
		
		String output_dir = input_path + network.getName()+"\\json\\";
		
		// output the parsed config to JSON format, one for each device
		for(DeviceConfig device : network.GetDevices()) {
			ParseTools.ProcessEthPort(device);
			ParseTools.ProcessPolicyUse(device);
			ParseTools.ProcessACLUse(device);
			ParseTools.ProcessVlanUse(device);
			DeviceParser.WriteConfigJSON(device,output_dir+device.getname()+"_config.json");
			DeviceParser.WriteRoutesJSON(device, output_dir+device.getname()+"_route.json");
		}
	}

	/* this section is for parsing configuration files in one particular direction 
	 * need to change to handle different types of devices */
	public void ParseConfig(NetworkConfig network, String input_path, String type) throws IOException
	{
//		String input_dir = input_path + network.getName()+"\\config\\";
		String input_dir = input_path;
		
		File[] config_files = new File(input_dir).listFiles();
		
		// read in the config file of each device, and parse it
		for(File config_file : config_files){
			if(!config_file.isDirectory()) {
				
				DeviceConfig device = new DeviceConfig();
				
				String[] tokens = config_file.getName().split("_");
				DeviceParser cp;
				switch(type) {
				case "demo":
					cp = new DCNParser();
					device.device_name = tokens[0].split("\\.")[0];
					break;
				case "DCN":
					cp = new DCNParser();
					device.device_name = tokens[0].split("\\.")[0];
					break;
				case "DCN_Complete":
					cp = new DCNParser();
					device.device_name = tokens[0].split("\\.")[0];
					break;
				case "vxlan_test":
					cp = new DCNParser();
					device.device_name = tokens[0].split("\\.")[0];
					break;
				case "campus":
					cp = new DCNParser();
					device.device_name = config_file.getName().split("\\.")[0];
					break;
				default:
					cp = new CiscoParser();
					break;
				}
//				DeviceParser cp = new CiscoParser();
//				DeviceParser cp = new HuaweiParser();
//				DeviceParser cp = new TencentParser();
				
				// set device name
//				if(tokens.length>1) {
//					device.device_name = tokens[0] + "_" + tokens[1];
////					cp = new CiscoParser();
//				}
//				else {
//					device.device_name = tokens[0];
//				}
				// read in the config file of device
				cp.ParseConfig(device, input_dir+config_file.getName());
				
				if (device.device_name != null) {
					ParseTools.ProcessEthPort(device);
					ParseTools.ProcessVBDIFSubnet(network,device);
					ParseTools.ProcessEPGSubnets(network, device);
					ParseTools.ProcessPolicyUse(device);
//					ParseTools.ProcessACLUse(device);
//					ParseTools.ProcessVlanUse(device);
					network.devices.put(device.device_name, device);
				}
			}
		}
	}
	
	public void ParseRoute(NetworkConfig network, String input_path, String type) throws IOException
	{
//		String input_dir = input_path + network.getName()+"\\route\\";
		String input_dir = input_path;
		
		File[] route_files = new File(input_dir).listFiles();
		
		// read in the route file of each device, and parse it
		for(File route_file : route_files){
			if(!route_file.isDirectory()) {
				
				String device_name = "";
				
				String[] tokens = route_file.getName().split("_");
				DeviceParser cp;
				switch(type) {
				case "demo":
					cp = new DCNParser();
					device_name = tokens[0].split("\\.")[0];
					break;
				case "DCN":
					cp = new DCNParser();
					device_name = tokens[0].split("\\.")[0];
					break;
				case "DCN_Complete":
					cp = new DCNParser();
					device_name = tokens[0].split("\\.")[0];
					break;
				case "vxlan_test":
					cp = new DCNParser();
					device_name = tokens[0].split("\\.")[0];
					break;
				default:
					cp = new CiscoParser();
					break;
				}
//				String[] tokens = route_file.getName().split("_");
////				DeviceParser cp = new CiscoParser();
////				DeviceParser cp = new HuaweiParser();
//				DeviceParser cp = new TencentParser();
//				
//				// set device name
//				if(tokens.length>1) {
//					device_name = tokens[0] + "_" + tokens[1];
////					cp = new CiscoParser();
//				}
//				else {
//					device_name = tokens[0];
//				}
				
				DeviceConfig device = network.devices.get(device_name);
				if(device == null) device = new DeviceConfig();
				
				
				
//				System.out.println(device.device_name);
				// read in the route file of device
				cp.ParseRoutes(device,input_dir+route_file.getName());
				
				if (device.device_name!=null) {
//					ParseTools.ProcessACLUse(device);
//					ParseTools.ProcessVlanUse(device);
					network.devices.put(device.device_name, device);
				}
			}
		}
	}
	private void ParseRouteJson(NetworkConfig network, String input_file, String type) throws IOException {
		// TODO Auto-generated method stub
		JSONTokener jt = new JSONTokener(new FileInputStream(new File(input_file)));
		JSONObject routeFile = new JSONObject(jt);
		JSONObject detail = routeFile.getJSONObject("detail");
		// read in the route file of each device, and parse it
		for(String device_name : detail.keySet()){
			DCNParser cp = new DCNParser();
			DeviceConfig device = network.devices.get(device_name);
			if(device == null) device = new DeviceConfig();
			// read in the route file of device
			cp.ParseRoutesJson(device,detail.getJSONObject(device_name));
			if (device.device_name!=null) {
				network.devices.put(device.device_name, device);
			}
		}
	}
	public void writeUpdate(NetworkConfig network, String output) {
		// TODO Auto-generated method stub
		File output_file = new File(output);
		try {
			FileWriter output_writer = new FileWriter(output_file);
			HashSet<String> rules = new HashSet<String>();
			// add device
			rules.add("+ device layer_classifier");
			rules.add("+ device l2_vrf");
			for(DeviceConfig router : network.devices.values()) {
				if(router.bds.keySet().isEmpty()) continue;
				// construct elements for every bd
				for(String bd_id : router.bds.keySet()) {
//					System.out.println(bd_id+", "+router.bds.get(bd_id));
					String element_name = router.device_name+"_"+bd_id;
					if(bd_id.startsWith("vpn")) {
						String rule = "+ device " + element_name;
						rules.add(rule);
					}
					else if(bd_id.startsWith("bd")){
						// we build one Layer2 forwarding vrf for all l2-fwd
					}
					else {
						// other vpn like dumbedge
						String rule = "+ device " + element_name;
						rules.add(rule);
					}
				}
			}
//			for(String rule : rules) {
//				output_writer.write(rule+"\n");
//			}
			// add link
//			rules.clear();
			HashSet<String> fwd_rules = new HashSet<String>();
			for(DeviceConfig router : network.devices.values()) {		
				// add l3 vrf link
				for(FWRule r : router.fw_rules) {
					String location = r.location;
					String prefix = r.prefix.split("/")[0];
					String priority = r.prefix.split("/")[1];
					String nexthop = r.nexthop;
					String action = r.outinterface;
					String subnet = r.prefix;
					if(location.contains("_")) {
						String device_rule = "+ device " + location;
						rules.add(device_rule);
						// for vpn table generate vxlan tunnels
//						System.out.println(router.device_name+" "+location);
						String vpn_name = location.split("_")[1];
						String ipString = ParseTools.dotted_prefix_to_int(subnet);
						long ipaddr = Long.valueOf(ipString.split(" ")[0]);
						int prefixlen = Integer.valueOf(ipString.split(" ")[1]);	
						String fwd_rule = "+ fwd "+location+" "+ipaddr+" "+prefixlen;
						if(action.equals("vxlan")) {
							Interface intf = router.getInterfacebyName("nve1");
							String srcip = intf.ip_prefix;
							
							fwd_rule = fwd_rule + " " + nexthop + " " + prefixlen;
							String vni = router.bds.get(vpn_name);
							HashSet<String> vteps = getVXLANEndPoint(network,srcip,nexthop,vni);
							for(String vtep_name : vteps) {
//								System.out.println(nexthop);
//								if(vtep_name == null) continue;
//								addOverlayTopologyOneWayLink(location,nexthop,vtep_name,srcip);
								String rule = "+ link "+location+" "+nexthop+" "+vtep_name+" "+srcip;
								rules.add(rule);
							}
						}
						else{
							fwd_rule = fwd_rule + " " + action + " " + prefixlen;
							if(action.startsWith("vbdif")) {
								// add host link from vbdif port
								String bd_name = router.device_name+"_bd" + action.substring(5);
//								addOverlayTopologyOneWayLink(location,action,bd_name,"inport");
								String rule = "+ link "+location+" "+action+" "+bd_name+" inport";
								rules.add(rule);
							}
							else if(action.startsWith("vpn")){// cross vpn flow
								String vpn = router.device_name + "_" + action;
								String rule = "+ link "+location+" "+action+" "+vpn+" inport";
								rules.add(rule);
							}
							else {
								// physic interface dumbedge
								String dumbedge = location.split("_")[1];
								String rule = "+ link "+location+" "+action+" "+dumbedge+" inport";
								rules.add(rule);
							}
						}
						fwd_rules.add(fwd_rule);
					}
					else {
						// do nothing
					}
				}
				if(router.bds.isEmpty()) continue;
				String src_ip = router.interfaces.get("nve1").ip_prefix;
				for(String bd_id : router.bds.keySet()) {
					if(bd_id.startsWith("bd")) {
						// add l2 vrf link
						String vbdif = "vbdif"+bd_id.substring(2);
						Interface one_interface = router.interfaces.get(vbdif);
						String subnet = ParseTools.getSlashedPrefix(one_interface.ip_prefix,one_interface.ip_mask);
						
						HashSet<String> nves = network.subnets.get(subnet);
						if(nves == null) continue;
						String l2_name = "l2_vrf_"+router.device_name+"_"+bd_id;
						
						for(String nexthop : nves) {
//							LOG.log(nexthop);
							if(nexthop.equals(src_ip)) {
								// add host link
//								System.out.println("add host link here!");
							}
							else {
								String vni = router.bds.get(bd_id);
								HashSet<String> vteps = getVXLANEndPoint(network,src_ip,nexthop,vni);
								for(String vtep_name : vteps) {
									String vtepname = "l2_vrf_" + vtep_name;
//									System.out.println(l2_name+" "+nexthop);
//									addOverlayTopologyOneWayLink(l2_name,subnet,vtep_name,subnet);
									String rule = "+ link "+l2_name+" "+subnet+" "+vtepname+" "+subnet;
									rules.add(rule);
								}
							}
						}
						
						// add LC to vrf link
						String LC_node = "layer_classifier_"+router.device_name+"_"+bd_id;
//						addOverlayTopologyOneWayLink(LC_node,"layer2",l2_name,"inport");
						String rule = "+ link "+LC_node+" "+"layer2"+" "+l2_name+" inport";
						rules.add(rule);
						String vrf_element = router.device_name+"_"+router.bd_vrfs.get(bd_id);
//						addOverlayTopologyOneWayLink(LC_node,"layer3",vrf_element,"inport");
						rule = "+ link "+LC_node+" "+"layer3"+" "+vrf_element+" inport";
						rules.add(rule);
					}
				}	
			}
			for(String rule : rules) {
				output_writer.write(rule+"\n");
			}		
			// add vpn rules
			for(String rule : fwd_rules) {
				output_writer.write(rule+"\n");
			}	
			// add GBP
			rules.clear();
			for(DeviceConfig router : network.devices.values()) {
				for(String policy_name : router.traffic_policy.keySet()) {
					String element_name = router.device_name + "_" + policy_name;
					String rule = "+ policy " + element_name + " " + router.device_name;
					if(router.policy_usage.containsKey(policy_name)) {
						// pbr policy
						for(ACLUse aclu : router.policy_usage.get(policy_name)) {
							String intf = aclu.getinterface();
							if(intf.startsWith("vbdif")) {
								String bd_id = "bd" + intf.substring(5);
								String vpn = router.bd_vrfs.get(bd_id);
								String link = rule + " " + vpn +" "+aclu.getdirection();
								rules.add(link);
							}
							else {
								String link = rule + " " + intf +" "+aclu.getdirection();
								rules.add(link);
							}
						}
					}
					else {
						// mcs policy
						rule = rule + " vpn in";
						rules.add(rule);
					}
				}
			}
			for(String rule : rules) {
				output_writer.write(rule+"\n");
			}
			rules.clear();
			// add epg
			for(DeviceConfig router : network.devices.values()) {
				for(String epg : router.epgs.keySet()) {
					for(MCSRule rule : router.epgs.get(epg)) {
						String subnet = ParseTools.getSlashedPrefix(rule.ipAddress, rule.mask);
						String ipprefix = ParseTools.dotted_prefix_to_int(subnet);
						
						long ipaddr = Long.valueOf(ipprefix.split(" ")[0]);
						int prefixlen = Integer.valueOf(ipprefix.split(" ")[1]);
						
						String epgr = "+ epg "+router.device_name+" "+epg+" "+ipaddr+" "+prefixlen+" "+rule.vpn;
						rules.add(epgr);
					}
				}
			}
			for(String epg : network.epg_fields.keySet()) {
				for(String subnet : network.epg_fields.get(epg)) {
					
				}
			}
			for(String rule : rules) {
				output_writer.write(rule+"\n");
			}
			// add gbp
			rules.clear();
			for(DeviceConfig router : network.devices.values()) {
				for(String policy_name : router.traffic_policy.keySet()) {
					String element_name = router.device_name + "_" + policy_name;

					for(Pair<String,String> policy : router.traffic_policy.get(policy_name)) {
						String classifier = policy.getKey();
						String behavior = policy.getValue();
						
						if(router.segment_classifier.containsKey(classifier)) {
							// segement classifier
							for(MCSRule rule : router.segment_classifier.get(classifier)) {
//								System.out.println(rule);
								String action = rule.action;
								String src_epg = rule.src_epg;
								String dst_epg = rule.dst_epg;
								
								String behavior_action = "permit";
								if(!router.traffic_behavior.get(behavior).isEmpty())
									behavior_action = router.traffic_behavior.get(behavior).get(0);
								if(behavior_action.equals("deny")) action = "deny";
								
								String aclr = "+ gbp "+ element_name + " "+ src_epg+" "+dst_epg+" "+action;
								rules.add(aclr);
							}
						}
						else {
							// pbr classifier
							for(String acl_name : router.traffic_classifier.get(classifier)) {
								String[] tokens = acl_name.split(" ");
								String behavior_action = "permit";
								if(!router.traffic_behavior.get(behavior).isEmpty()) {
									behavior_action = router.traffic_behavior.get(behavior).get(0);
								}
								if(behavior_action.startsWith("redirect")) {
									// if redirect, must generate redirect link	
									String[] redirect = behavior_action.split(" ");
									String vpn = redirect[3];
									if(vpn.equals("nexthop")) vpn = redirect[2];
									String nexthop = redirect[4];
									for(ACLUse aclu : router.policy_usage.get(policy_name)) {
										
										String intf = aclu.getinterface();
										if(intf.startsWith("vbdif")) {
											String bd_id = "bd" + intf.substring(5);
											intf = router.device_name+"_"+router.bd_vrfs.get(bd_id);
										}
										String rule = "+ redirect "+router.device_name+"_"+policy_name+" "+intf+" "+aclu.getdirection()
											+" redirect "+vpn+" "+nexthop;
										rules.add(rule);
									}
								}
								System.out.println(router.device_name+" "+acl_name);
								if(acl_name.startsWith("acl")) {
									//acl rule
									for(ACLRule aclr : router.acls.get(tokens[1])) {
										String action = aclr.get_type();
										if(behavior_action.equals("deny")) action = "deny";
										else if(behavior_action.startsWith("redirect") && action.equals("permit")) action = "redirect";
										String aclrule = "+ pbr "+ element_name + " "+ aclr.toString()+" "+action;
										System.out.println(aclrule);
										rules.add(aclrule);
									}
								}
								else {
									// vni
									String vni = tokens[1];
									if(behavior_action.startsWith("redirect")) {
										// if redirect, must generate redirect link	
										String[] redirect = behavior_action.split(" ");
										String vpn = redirect[3];
										if(vpn.equals("nexthop")) vpn = redirect[2];
										String nexthop = redirect[4];
										for(ACLUse aclu : router.policy_usage.get(policy_name)) {
											
											String intf = aclu.getinterface();
											if(intf.startsWith("vbdif")) {
												String bd_id = "bd" + intf.substring(5);
												intf = router.device_name+"_"+router.bd_vrfs.get(bd_id);
											}
											String rule = "+ redirect "+router.device_name+"_"+policy_name+" "+intf+" "+aclu.getdirection()
												+" redirect "+vpn+" "+nexthop;
											rules.remove(rule);
										}
										String vpn_element = router.vni_bd.get(vni);
										vpn_element = router.device_name+"_"+vpn_element;
										String rule = "+ redirect "+router.device_name+"_"+policy_name+" "+vpn_element+" "+"in"
											+" redirect "+vpn+" "+nexthop;
										rules.add(rule);
									}
								}
							}
						}
					}
				}
			}
			for(String rule : rules) {
				output_writer.write(rule+"\n");
			}
			output_writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void writeCampusUpdate(NetworkConfig network, String output) {
		// TODO Auto-generated method stub
		File output_file = new File(output);
		try {
			FileWriter output_writer = new FileWriter(output_file);
			HashSet<String> rules = new HashSet<String>();
			rules.add("+ device layer_classifier");
			// add device
			for(DeviceConfig router : network.devices.values()) {
				// construct elements for every bd
				String element_name = router.device_name;
				String rule = "+ device " + element_name;
				rules.add(rule);
			}
			for(String rule : rules) {
				output_writer.write(rule+"\n");
			}		
			// add GBP
			rules.clear();
			for(DeviceConfig router : network.devices.values()) {
				for(String policy_name : router.traffic_policy.keySet()) {
					String element_name = router.device_name + "_" + policy_name;
					String rule = "+ policy " + element_name + " " + router.device_name;
					if(router.policy_usage.containsKey(policy_name)) {
						// pbr policy
						for(ACLUse aclu : router.policy_usage.get(policy_name)) {
							String intf = aclu.getinterface();
							String link = rule + " " + intf +" "+aclu.getdirection();
							rules.add(link);
						}
					}
				}
			}
			for(String rule : rules) {
				output_writer.write(rule+"\n");
			}
			// add gbp
			rules.clear();
			for(DeviceConfig router : network.devices.values()) {
				for(String policy_name : router.traffic_policy.keySet()) {
					String element_name = router.device_name + "_" + policy_name;

					for(Pair<String,String> policy : router.traffic_policy.get(policy_name)) {
						String classifier = policy.getKey();
						String behavior = policy.getValue();
						// pbr classifier
						for(String acl_name : router.traffic_classifier.get(classifier)) {
							String[] tokens = acl_name.split(" ");
							String behavior_action = "permit";
							if(!router.traffic_behavior.get(behavior).isEmpty()) {
								behavior_action = router.traffic_behavior.get(behavior).get(0);
							}
							if(behavior_action.startsWith("redirect")) {
								// if redirect, must generate redirect link	
								String[] redirect = behavior_action.split(" ");
								String vpn = redirect[3];
								if(vpn.equals("nexthop")) vpn = redirect[2];
								String nexthop = redirect[4];
								for(ACLUse aclu : router.policy_usage.get(policy_name)) {
									
									String intf = aclu.getinterface();
									if(intf.startsWith("vbdif")) {
										String bd_id = "bd" + intf.substring(5);
										intf = router.device_name+"_"+router.bd_vrfs.get(bd_id);
									}
									String rule = "+ redirect "+router.device_name+"_"+policy_name+" "+intf+" "+aclu.getdirection()
										+" redirect "+vpn+" "+nexthop;
									rules.add(rule);
								}
							}
							System.out.println(router.device_name+" "+acl_name);
							if(acl_name.startsWith("acl")) {
								//acl rule
								for(ACLRule aclr : router.acls.get(tokens[1])) {
									String action = aclr.get_type();
									if(behavior_action.equals("deny")) action = "deny";
									else if(behavior_action.startsWith("redirect") && action.equals("permit")) action = "redirect";
									String aclrule = "+ pbr "+ element_name + " "+ aclr.toString()+" "+action;
									System.out.println(aclrule);
									rules.add(aclrule);
								}
							}
							else {
								// vni
								String vni = tokens[1];
								if(behavior_action.startsWith("redirect")) {
									// if redirect, must generate redirect link	
									String[] redirect = behavior_action.split(" ");
									String vpn = redirect[3];
									if(vpn.equals("nexthop")) vpn = redirect[2];
									String nexthop = redirect[4];
									for(ACLUse aclu : router.policy_usage.get(policy_name)) {
										
										String intf = aclu.getinterface();
										if(intf.startsWith("vbdif")) {
											String bd_id = "bd" + intf.substring(5);
											intf = router.device_name+"_"+router.bd_vrfs.get(bd_id);
										}
										String rule = "+ redirect "+router.device_name+"_"+policy_name+" "+intf+" "+aclu.getdirection()
											+" redirect "+vpn+" "+nexthop;
										rules.remove(rule);
									}
									String vpn_element = router.vni_bd.get(vni);
									vpn_element = router.device_name+"_"+vpn_element;
									String rule = "+ redirect "+router.device_name+"_"+policy_name+" "+vpn_element+" "+"in"
										+" redirect "+vpn+" "+nexthop;
									rules.add(rule);
								}
							}
						}
					}
				}
			}
			for(String rule : rules) {
				output_writer.write(rule+"\n");
			}
			// add fwd rule
			rules.clear();
			for(DeviceConfig router : network.devices.values()) {		
				// add l3 vrf link
				for(FWRule r : router.fw_rules) {
					String location = r.location;
					String action = r.outinterface;
					String subnet = r.prefix;
					String ipString = ParseTools.dotted_prefix_to_int(subnet);
					long ipaddr = Long.valueOf(ipString.split(" ")[0]);
					int prefixlen = Integer.valueOf(ipString.split(" ")[1]);	
					String fwd_rule = "+ fwd "+location+" "+ipaddr+" "+prefixlen;
					fwd_rule = fwd_rule + " " + action + " " + prefixlen;
					rules.add(fwd_rule);
					if(action.startsWith("vlanif")) {
						String link_rule = "+ link " + location + " " + action + " " + location+","+action+" inport";
						rules.add(link_rule);
						link_rule = "+ link layer_classifier_" + location +","+action +" layer3 " + location + " "+action;
						rules.add(link_rule);
					}
				}
			}	
			for(String rule : rules) {
				output_writer.write(rule+"\n");
			}
			output_writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private HashSet<String> getVXLANEndPoint(NetworkConfig network, String src_ip, String dst_ip, String vni) {
		// TODO Auto-generated method stub
		//check underlay reachability
		
		//if true
		HashSet<String> ret = new HashSet<String>();
		for(DeviceConfig router : network.devices.values()) {
			if(!router.interfaces.containsKey("nve1")) continue;
			String nve_ip = router.interfaces.get("nve1").ip_prefix;
			if(nve_ip.equals(dst_ip)) {
//				System.out.println("yes! "+ src_ip +" "+dst_ip + " " + router.device_name + " " + router.interfaces_with_ip.get(dst_ip).interface_name);
//				break;
				/* since vlxan endpoint use vni to choose L3VPN instance,
				 * we use vni to build overlay topo
				 */
				if(router.vni_bd.containsKey(vni)) {
					String bd_id = router.vni_bd.get(vni);
					ret.add(router.device_name+"_"+bd_id);
				}
//				else return null;
			}
		}
//		return null;
		return ret;
	}

	public static void convertFIBtoUpdate(String in_path, String out_path) throws IOException {
		
		BufferedReader fileBufferReader = null;
		try {
			fileBufferReader = new BufferedReader(new FileReader(in_path));
		} catch (FileNotFoundException e) {
			System.err.println ("File not found! " + in_path); // for debugging
			System.exit (0); // Stop program if no file found
		}
		
		ArrayList<String> rules = new ArrayList<String> ();
		
		String OneLine;
		while((OneLine = fileBufferReader.readLine()) != null)
		{
			String linestr = OneLine.trim();
			String[]tokens = linestr.split("\t");
			String op = "+";
			int i = 0;
			String[]temps = tokens[0].split(" ");
			if( temps.length > 1) {
				tokens[0] = temps[1];
				op = temps[0];
			}
			op = op + " fwd";
			String int_ip = ParseTools.dotted_prefix_to_int(tokens[i+1]);
			String rule = op;
			for(int j=i;j<tokens.length-1;j++) {
				rule = rule + " "+tokens[j];
				if(j==i) {
					rule = rule + " "+ int_ip;
					j++;
				}
			}
			rule = rule + " " + int_ip.split(" ")[1];
			rules.add(rule);
		}
		
		File templog_file = new File(out_path);
		if(!templog_file.exists()){
			templog_file.createNewFile();
		}
		FileWriter log_writer = new FileWriter(templog_file);
		for(String rule : rules) {
			log_writer.write(rule+"\n");
		}
		log_writer.close();
	}

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

//		String networkname = "stanford";
//		String networkname = "purdue";
//		String networkname = "huawei";
//		String networkname = "bgp_apkeep";
//		String networkname = "DCN_xjtu";
//		String networkname = "DCN_xjtu_20";
//		String networkname = "DCN_xjtu_50";
//		String networkname = "DCN_xjtu_100";
//		String networkname = "DCN_xjtu_200";
		String networkname = "Campus";
		String inpath = Parameters.dataset_path;
//		String name = "svn-perform-test20-20-200-40";
//		String name = "svn-perform-test50-50-500-100";
//		String name = "svn-perform-test100-100-1000-200";
//		String name = "svn-perform-test200-200-2000-400";
		String name = "_500";
//		String[] state = new String[] {"","_post0","_post1","_post2","_post3","_post4"};
		String[] state = new String[] {"base","post0","post1"};

//		String outpath = Parameters.dataset_path;
//		String inpath = Parameters.dataset_path+"/"+networkname+"/parsed/";
		
//		convertFIBtoUpdate(inpath+"full.fib",inpath+"updates");
//		convertFIBtoUpdate(inpath+"update.fib",inpath+"batch_updates");
		NetworkConfig network = new NetworkConfig(networkname);
		NetworkParser np = new NetworkParser(network);
		
		// write base config update
		for(String s : state) {
			// for dcn network
//			String st = name+s;
//			String input_dir = inpath + networkname + "/config/"+st+"/";
//			np.ParseConfig(network, input_dir, "DCN");
//			input_dir = inpath + networkname + "/route/"+st+"/";
//			np.ParseRoute(network, input_dir, "DCN");
//			String output = inpath + networkname + "/parsed/"+st;
//			np.writeUpdate(network, output);
			// for campus network
			String update = "update_"+s;
			String rootFolder = inpath + networkname+"/"+networkname+name+"/";
			np.ParseConfig(network, rootFolder+"config/"+s+"/", "campus");
			np.ParseRouteJson(network, rootFolder+"route/test_per_campus"+name+(s.equals("base")?"":"_"+s)+"_routes.json", "DCN");
			String output = rootFolder + "parsed/"+networkname+name+"_"+s;
			np.writeCampusUpdate(network, output);
		}
		// write post0 config update
	}
}
