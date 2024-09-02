package apkeep.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;

import apkeep.parser.ParseTools;
import common.ACLParser;
import common.ACLRule;
import common.ACLUse;
import common.ForwardingRule;
import common.ACLParser.ACLType;

public class DeviceParser {

	String device_name;
	public String device_type;
	String config_file_name;
	String fw_file_name;
	String origin_file_name;
	String output_folder;
	
	int rule_number;
	
	public ArrayList<ForwardingRule> fw_rules;
	public ArrayList<ACLRule> acl_rules;
	
	HashMap<String, LinkedList<ACLRule>> aclmap;
	HashSet<String> effectiveacls;
	HashMap<String, HashSet<String>> vlan_ports;
	ArrayList<ACLUse> acluses;
	
	HashSet<String> fw_ports;
	
	public DeviceParser(String dname, String dtype)
	{
		device_name = dname;
		device_type = dtype;
		acl_rules = new ArrayList<ACLRule>();
		fw_rules = new ArrayList<ForwardingRule>();
		fw_ports = new HashSet<String>();
	}
	
	public DeviceParser(String dname, String dtype, String cname, String fname, String oname) {
		// TODO Auto-generated constructor stub

		this(dname, dtype);
		
		config_file_name = cname;
		fw_file_name = fname;
		output_folder = oname;
		rule_number = 0;
		
		aclmap = new HashMap<String, LinkedList<ACLRule>>();
		acluses = new ArrayList<ACLUse>();
		effectiveacls = new HashSet<String>();
		vlan_ports = new HashMap<String, HashSet<String>>();
	}

	public DeviceParser(String dname, String dtype, String fname, String oname) {
		// TODO Auto-generated constructor stub

		this(dname, dtype);
		
		fw_file_name = fname;
		output_folder = oname;
		rule_number = 0;
		
		aclmap = new HashMap<String, LinkedList<ACLRule>>();
		acluses = new ArrayList<ACLUse>();
		effectiveacls = new HashSet<String>();
		vlan_ports = new HashMap<String, HashSet<String>>();
	}
	
	public DeviceParser(String dname, String dtype, String cname, String fname, String orginname, String oname) {
		// TODO Auto-generated constructor stub
		
		this(dname, dtype);
		
		config_file_name = cname;
		fw_file_name = fname;
		origin_file_name = orginname;
		output_folder = oname;
		rule_number = 0;
		
		aclmap = new HashMap<String, LinkedList<ACLRule>>();
		acluses = new ArrayList<ACLUse>();
		effectiveacls = new HashSet<String>();
		vlan_ports = new HashMap<String, HashSet<String>>();
	}

	public String GetDeviceName ()
	{
		return device_name;
	}
	
	public String getDeviceType ()
	{
		return device_type;
	}
	
	public LinkedList<ACLRule> getACLRules (String aclname)
	{
		return aclmap.get(aclname);
	}
	
	public void addACL(String name, LinkedList<ACLRule> acl)
	{
		aclmap.put(name, acl);
	}
	
	public void addFW(ForwardingRule fw)
	{
		fw_ports.add(fw.getiname());
		fw_rules.add(fw);
	}
	
	public void addVlanPorts(String vlan, HashSet<String> ports)
	{
		vlan_ports.put(vlan, ports);
	}
	
	public void addACLUse(ACLUse oneuse)
	{
		acluses.add(oneuse);
	}
	
	public void addACLRules (LinkedList<ACLRule> rules)
	{
		int priority = 65535;
		for (ACLRule rule : rules) {
			rule.set_priority(priority);
			acl_rules.add(rule);
			priority --;
		}
	}
	
	public boolean addEffectiveACL (String aclname)
	{
		return effectiveacls.add(aclname);
	}
	 
	public boolean hasVlanPort (String vlanport) 
	{
		return vlan_ports.containsKey(vlanport);
	}
	
	public HashSet<String> vlanToPhy(String vlanport)
	{
		return vlan_ports.get(vlanport);
	}
	
	public void writeACLs() throws IOException
	{
		for (String aclname : effectiveacls) {
			String newname = device_name + "_" + aclname;
			System.out.println("Write ACL for " + newname);
			File aclfile = new File(output_folder + newname);
			FileWriter aclwriter = new FileWriter(aclfile);
			LinkedList<ACLRule> acl = aclmap.get(aclname);
			int priority = 65535;
			for (ACLRule r : acl) {
				aclwriter.write(r.toString()+" " + priority + "\n");
			}
			aclwriter.close();
		}
	}
	
	public ForwardingRule assembleFW(String[] entries)
	{
		//use short name for physical port
		String portname;
		if(entries[3].startsWith("vlan"))
		{
			portname = entries[3];
		}else
		{
			//System.out.println(entries[3]);
			portname = entries[3].split("\\.")[0];
//			System.out.println(portname);
		}
		return new ForwardingRule(Long.parseLong(entries[1]), Integer.parseInt(entries[2]), portname);
	}

	private ForwardingRule parseFW(String[] tokens) {
		// TODO Auto-generated method stub

		String prefix = tokens[0];
		String nexthop = tokens[1];
		String port = "";
		
		if(tokens.length > 2) {
			port = tokens[2];
		}
		else {
			port = "self";
		}
//		System.out.print(prefix+" ");
		if(port.toLowerCase().startsWith("loopback") || 
				port.toLowerCase().startsWith("null") ||
				nexthop.toLowerCase().startsWith("drop")) {
			port = "self";
		}
		
		prefix = ParseTools.dotted_prefix_to_int(prefix);
		port = ParseTools.get_ethernet_port_name(port);
		
		if(port.startsWith("vlan"))
		{
			//do nothing
		}else
		{
			//System.out.println(entries[3]);
			port = port.split("\\.")[0];
		}
//		System.out.println(prefix+" "+port);
		String[] results = prefix.split(" ");
		ForwardingRule fwr = new ForwardingRule(Long.parseLong(results[0]), Integer.parseInt(results[1]), port);
		return fwr;
	}
	
	public void readParsedFW() throws IOException
	{
		Scanner OneLine = null;
		try {
			File inputFile = new File(fw_file_name);
			OneLine = new Scanner (inputFile);
			OneLine.useDelimiter("\n");
			//scanner.useDelimiter(System.getProperty("line.separator"));
			// doesn't work for .conf files
		} catch (FileNotFoundException e) {
			System.out.println ("File not found!"); // for debugging
			System.exit (0); // Stop program if no file found
		}
		
		while(OneLine.hasNext())
		{
			String linestr = OneLine.next().trim();
			String[] tokens = linestr.split(" ");
			if(tokens[0].equals("fw"))
			{
				ForwardingRule onerule = assembleFW(tokens);
				addFW(onerule);
				//System.out.println("add: " + onerule);
			}else if(tokens[0].equals("vlanport"))
			{
				String vlanname = tokens[1];
				HashSet<String> ports = new HashSet<String>();
				for(int i = 2; i < tokens.length; i ++)
				{
					ports.add(tokens[i]);
				}
				addVlanPorts(vlanname, ports);
				//System.out.println("add: " + vlanname + " " + ports);
			}else if(tokens[0].equals("acl"))
			{
				for (int i = 0; i < (tokens.length - 2)/2; i ++)
				{
					addACLUse(new ACLUse(tokens[1], tokens[(i+1)*2], tokens[(i+1)*2+1]));
					//System.out.println(tokens[1] + " " + tokens[(i+1)*2] + " " + tokens[(i+1)*2+1]);
				}
			}
		}
	}

	public void readParsedFWOrigin() {
		// TODO Auto-generated method stub
		Scanner OneLine = null;
		try {
			File inputFile = new File(fw_file_name);
			OneLine = new Scanner (inputFile);
			OneLine.useDelimiter("\n");
			//scanner.useDelimiter(System.getProperty("line.separator"));
			// doesn't work for .conf files
		} catch (FileNotFoundException e) {
			System.out.println ("File not found!"); // for debugging
			System.exit (0); // Stop program if no file found
		}
		
		while(OneLine.hasNext())
		{
			String linestr = OneLine.next().trim();
			String[] tokens = linestr.split(" ");
			if(tokens[0].equals("fw"))
			{
//				ForwardingRule onerule = assembleFW(tokens);
//				addFW(onerule);
				continue;
				//System.out.println("add: " + onerule);
			}else if(tokens[0].equals("vlanport"))
			{
				String vlanname = tokens[1];
				HashSet<String> ports = new HashSet<String>();
				for(int i = 2; i < tokens.length; i ++)
				{
					ports.add(tokens[i]);
				}
				addVlanPorts(vlanname, ports);
				//System.out.println("add: " + vlanname + " " + ports);
			}else if(tokens[0].equals("acl"))
			{
				for (int i = 0; i < (tokens.length - 2)/2; i ++)
				{
					addACLUse(new ACLUse(tokens[1], tokens[(i+1)*2], tokens[(i+1)*2+1]));
					//System.out.println(tokens[1] + " " + tokens[(i+1)*2] + " " + tokens[(i+1)*2+1]);
				}
			}
		}

		try {
			File inputFile = new File(origin_file_name);
			OneLine = new Scanner (inputFile);
			OneLine.useDelimiter("\n");
			//scanner.useDelimiter(System.getProperty("line.separator"));
			// doesn't work for .conf files
		} catch (FileNotFoundException e) {
			System.out.println ("File not found!"); // for debugging
			System.exit (0); // Stop program if no file found
		}
		
		while(OneLine.hasNext())
		{
			String linestr = OneLine.next().trim();
			String[] tokens = linestr.split(" ");
			
			ForwardingRule onerule = parseFW(tokens);
			addFW(onerule);
		}
	}

	// build aclmap in device d
	public void readConfig() throws IOException
	{

		/************************************************************************
		 * Set up a Scanner to read the file using tokens
		 ************************************************************************/
		Scanner OneLine = null;
		try {
			File inputFile = new File(config_file_name);
			OneLine = new Scanner (inputFile);
			OneLine.useDelimiter("\n");
			//scanner.useDelimiter(System.getProperty("line.separator"));
			// doesn't work for .conf files
		} catch (FileNotFoundException e) {
			System.out.println ("File not found!"); // for debugging
			System.exit (0); // Stop program if no file found
		}

		/* Read line by line */
		while (OneLine.hasNext()) {
			/* Read token by token in each line */
			Scanner TokenInLine = new Scanner(OneLine.next());
			String keyword;
			if (TokenInLine.hasNext()) {
				keyword = TokenInLine.next();
				/**************************************************************
				 * This section handles ACL rules that start with "access-list"
				 ***************************************************************/
				if (keyword.equals("access-list")) {
					HandleAccessList(OneLine, TokenInLine);
				}
				/* handles acl rules that start with ip access-list extend/standard*/
				else if(keyword.equals("ip"))
				{
					keyword = TokenInLine.next();
					if(keyword.equals("access-list"))
					{
						HandleAccessListGrouped(OneLine, TokenInLine);
					}
				}
			}
		} // end of while (oneline.hasNext())
	}
	

	public void HandleAccessListGrouped(Scanner oneline, Scanner tokeninline)throws IOException
	{
		ArrayList<String> argument = new ArrayList<String> ();
		ACLParser.GetArgument(tokeninline, argument);
		while(true){
			//"access-list" already removed		
			LinkedList<ACLRule> oneacl = new LinkedList<ACLRule>();
			ACLType thisType;
			if(argument.get(0).equals("extended"))
			{
				thisType = ACLType.extend;
			}else
			{
				thisType = ACLType.standard;
			}
			String thisNumber = argument.get(1);

			while(true){
				tokeninline = new Scanner(oneline.next());
				ACLParser.GetArgument(tokeninline, argument);
				if(argument.get(0).equals("ip"))
				{//a new ACL, need to add the old one 
					addACL(thisNumber, oneacl);
					argument.remove(0);// remove ip
					argument.remove(0);// remove access-list, so that it can restart
					//Parser.DebugInput(System.out, null, "Add:"+thisNumber);
					break;
				}
				if((!argument.get(0).equals("permit")) && (!argument.get(0).equals("deny")))
				{//this means the end of the ACL definition
					addACL(thisNumber, oneacl);
					//Parser.DebugInput(System.out, null, "Add:"+thisNumber);
					return;
				}
				ACLRule onerule = new ACLRule();
				onerule.accessList = "access-list";
				onerule.accessListNumber = thisNumber;
				ACLParser.CheckPermitDeny(onerule, argument);
				if(thisType == ACLType.extend)
				{
					ACLParser.HandleACLRuleExtend(onerule, argument);
				}else{
					ACLParser.HandleACLRuleStandard(onerule, argument);
				}
				oneacl.add(onerule);
			}
		}

	}

	public void HandleAccessList(Scanner oneline, Scanner tokeninline) throws IOException
	{
		ArrayList<String> argument = new ArrayList<String>();
		/**set up argument, 'access-list' has already been parsed*/
		ACLParser.GetArgument(tokeninline, argument);

		// Test Function 1 : output argument array into a test file
		//DebugInput(System.out, argument, "access-list");

		int currentACLNum = -1;
		int preACLNum = -1;
		int[] aclNumbers = {preACLNum, currentACLNum};// pay attention to the order

		ACLRule onerule = null;
		LinkedList<ACLRule> oneacl = new LinkedList<ACLRule>();

		if(ACLParser.CheckValidACL(argument, aclNumbers))
		{
			onerule = new ACLRule();
			onerule.accessList = "access-list";

			preACLNum = aclNumbers[0];
			currentACLNum = aclNumbers[1];
			onerule.accessListNumber = Integer.toString(currentACLNum);
			//
			ACLParser.AddDynamic(onerule, argument);
			ACLParser.CheckPermitDeny(onerule, argument);

			if(ACLParser.CheckACLType(currentACLNum) == ACLType.standard)
			{
				ACLParser.HandleACLRuleStandard(onerule, argument);
			}else
			{
				ACLParser.HandleACLRuleExtend(onerule, argument);
			}
			oneacl.add(onerule);
		}


		while(oneline.hasNext())
		{
			String keyword = "";
			tokeninline = new Scanner(oneline.next());
			if(tokeninline.hasNext()){
				keyword = tokeninline.next();
			}else{
				// seems reach the end of file, finish parsing
				break;
			}
			if(keyword.equals("access-list"))
			{
				aclNumbers[0] = preACLNum; 
				aclNumbers[1] = currentACLNum;
				ACLParser.GetArgument(tokeninline, argument);

				if(ACLParser.CheckValidACL(argument, aclNumbers))
				{
					preACLNum = aclNumbers[0];
					currentACLNum = aclNumbers[1];
					if(preACLNum != currentACLNum)
					{
						// finish parsing an acl, add to the router
						addACL(Integer.toString(preACLNum), oneacl);

						//debug
						ArrayList<String> oneaclInfo = new ArrayList<String>();
						oneaclInfo.add(oneacl.get(0).accessListNumber);
						oneaclInfo.add(Integer.toString(oneacl.size()));
						//Parser.DebugInput(System.out, oneaclInfo, "added access-list");

						// get a new one to store
						oneacl = new LinkedList<ACLRule>();
						preACLNum = currentACLNum;
					}

					onerule = new ACLRule();
					onerule.accessList = "access-list";

					onerule.accessListNumber = Integer.toString(currentACLNum);
					//
					ACLParser.AddDynamic(onerule, argument);
					ACLParser.CheckPermitDeny(onerule, argument);

					if(ACLParser.CheckACLType(currentACLNum) == ACLType.standard)
					{
						ACLParser.HandleACLRuleStandard(onerule, argument);
					}else
					{
						ACLParser.HandleACLRuleExtend(onerule, argument);
					}
					//debug
					//DebugTools.IntermediateACLRuleCheck(onerule, System.out);
					oneacl.add(onerule);
				}

			}else
			{// the acl part ends
				break;
			}
		}

		// need to add the last acl
		if(currentACLNum != -1)
		{
			//debug
			ArrayList<String> oneaclInfo = new ArrayList<String>();
			oneaclInfo.add(oneacl.get(0).accessListNumber);
			oneaclInfo.add(Integer.toString(oneacl.size()));
			//Parser.DebugInput(System.out, oneaclInfo, "added access-list");
			addACL(Integer.toString(currentACLNum), oneacl);
		}
	}
}
