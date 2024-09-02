package apkeep.parser.entity;

public class MCSRule {
	String type;
	
	public String ipAddress;
	public String mask;
	public String vpn;
	
	public String action;
	public String src_epg;
	public String dst_epg;
	
	public MCSRule() {
		type = "null";
	}
	public void setEPGRule(String ipstring, String mask, String vpn) {
		// TODO Auto-generated constructor stub
		if(!type.equals("null")) {
			System.err.println("type error!");
			return;
		}
		type = "EPG";
		ipAddress = ipstring;
		this.mask = mask;
		this.vpn = vpn;
	}
	public void setClassifierRule(String action, String src_epg, String dst_epg) {
		// TODO Auto-generated constructor stub
		if(!type.equals("null")) {
			System.err.println("type error!");
			return;
		}
		type = "classifier";
		this.action = action;
		this.src_epg = src_epg;
		this.dst_epg = dst_epg;
	}
	@Override
	public String toString() {
		if(type.equals("EPG")) return type+" "+ipAddress+" "+mask+" "+vpn;
		else if(type.equals("classifier")) return type+" "+action+" "+src_epg+" "+dst_epg;
		else return type;
	}
}
