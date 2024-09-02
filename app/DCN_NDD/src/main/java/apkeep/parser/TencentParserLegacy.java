package apkeep.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

import org.json.*;

import apkeep.parser.entity.DeviceConfig;
import apkeep.parser.entity.FWRule;
import apkeep.parser.entity.Interface;

public class TencentParserLegacy extends DeviceParser {

	public TencentParserLegacy() {
		super();
		// TODO Auto-generated constructor stub
	}

	@Override
	public void ParseConfig(DeviceConfig device, String inpath) throws IOException {
		// TODO Auto-generated method stub
		
		// cess json file
		Scanner OneLine = null;
		try {
			File route_file = new File(inpath);
			OneLine = new Scanner (route_file);
			OneLine.useDelimiter("\n");
		} catch (FileNotFoundException e) {
			System.out.println ("File not found!" + inpath);
			System.exit (0);
		}
		
		String content = "";
		
		/* Read line by line */
		while (OneLine.hasNext()) {
			Scanner TokenInLine = new Scanner(OneLine.next());
			while(TokenInLine.hasNext()) {
				content = content + TokenInLine.next();
			}
		} // end of while (oneline.hasNext())
		
		JSONObject json = new JSONObject(content);
		JSONObject interfaces = json.getJSONObject("*INTERFACE").getJSONObject("value");
		
		for(String key : interfaces.keySet()) {
//			System.out.println(key);
			String[] tokens = key.split("\\|");
			String interface_name = tokens[0];
			String ip_prefix = tokens[1];
			
			Interface router_interface = new Interface();
			// set name
			router_interface.interface_name = interface_name;
			// set ip address
//			System.out.println(ip_prefix);
			int slashInd = ip_prefix.indexOf('/');
//			System.out.println(slashInd);
			int maskNum = Integer.parseInt(ip_prefix.substring(slashInd + 1));
			// ipv6
			if(maskNum > 32) break;
			String mask = ParseTools.SeperateMask(ip_prefix, slashInd);
			router_interface.ip_prefix = ip_prefix.substring(0, slashInd);
			router_interface.ip_mask = mask;
			// add to router
			device.interfaces.put(router_interface.interface_name, router_interface);
			device.interfaces_with_ip.put(router_interface.ip_prefix, router_interface);
			
		}
	}

	@Override
	public void ParseRoutes(DeviceConfig device, String inpath) throws IOException {
		// TODO Auto-generated method stub
//		System.out.println("in route parser!");
		Scanner OneLine = null;
		try {
			File route_file = new File(inpath);
			OneLine = new Scanner (route_file);
			OneLine.useDelimiter("\n");
		} catch (FileNotFoundException e) {
			System.out.println ("File not found!" + inpath);
			System.exit (0);
		}
		
		String content = "";
		
		/* Read line by line */
		while (OneLine.hasNext()) {
			Scanner TokenInLine = new Scanner(OneLine.next());
			while(TokenInLine.hasNext()) {
				content = content + TokenInLine.next();
			}
		} // end of while (oneline.hasNext())
		
		JSONObject json = new JSONObject(content);
		JSONObject routes = json.getJSONObject("ROUTE_TABLE").getJSONObject("value");
		
		for(String ip_prefix : routes.keySet()) {
			String out_interface = routes.getJSONObject(ip_prefix).getString("ifname");
			String next_hop = routes.getJSONObject(ip_prefix).getString("nexthop");
			
			FWRule fw = new FWRule(ip_prefix,next_hop,out_interface);
			
			device.fw_rules.add(fw);
		}
	}

	@Override
	public void ParseArps(DeviceConfig router, String inpath) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void ParseMacs(DeviceConfig router, String inpath) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
