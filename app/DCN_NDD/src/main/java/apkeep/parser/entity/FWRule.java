package apkeep.parser.entity;

public class FWRule {
	public String location;
	public String prefix;
	public String nexthop;
	public String outinterface;
	
	public FWRule (String prefix, String nexthop, String outinterface) {
		super();
		this.prefix = prefix;
		this.nexthop = nexthop;
		this.outinterface = outinterface;
	}
	public FWRule (String location, String prefix, String nexthop, String outinterface) {
		super();
		this.location = location;
		this.prefix = prefix;
		this.nexthop = nexthop;
		this.outinterface = outinterface;
	}
	public String toString() {
		return prefix + " " + nexthop + " " + outinterface;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getNexthop() {
		return nexthop;
	}

	public String getOutinterface() {
		return outinterface;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public void setNexthop(String nexthop) {
		this.nexthop = nexthop;
	}

	public void setOutinterface(String outinterface) {
		this.outinterface = outinterface;
	}
}
