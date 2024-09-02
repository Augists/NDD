package apkeep.utils;

public enum Types {
	device("device",0),
	bd("bd",1),
	vpn("vpn",2),
	link("link",3),
	policy("policy",4),
	accesslist("accesslist",5),
	epg("epg",6),
	gbp("gbp",7),
	pbr("pbr",8),
	acl("acl",9),
	fwd("fwd",10),
	nat("nat",11),
	redirect("redirect",12);
	private String type;
	private int index;
	private Types(String type,int index) {
		this.type = type;
		this.index = index;
	}
	public static int getIndex(String type) {
		for(Types tp : Types.values()) {
			if(tp.type.equals(type)) {
				return tp.index;
			}
		}
		return -1;
	}
	public static Types getType(String type) {
		for(Types tp : Types.values()) {
			if(tp.type.equals(type)) {
				return tp;
			}
		}
		return null;
	}
}
