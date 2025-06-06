package apkeep.FDN;

import NDD.NDD;
import common.Fields;

public abstract class RewriteRule {
	
	public String name;
	
	public int field_bdd;
	public int new_val_bdd;
	public int old_pkt_bdd;
	public NDD new_val_ndd;
	
	public String new_dst_ip;
	public int priority;
	
	protected BDDACLWrapper _baw;
	public RewriteRule(int old_val, int field, int new_val, String rname, int p)
	{
		old_pkt_bdd = old_val;
		field_bdd = field;
		new_val_bdd = new_val;
		name = rname;
		priority = p;
		new_val_ndd = NDD.table.ref(NDD.toNDD(new_val_bdd));
	}
	protected RewriteRule(long old_prefix, int old_prefix_len, long new_prefix, int new_prefix_len, String rname, int p, BDDACLWrapper baw)
	{
		old_pkt_bdd = baw.encodeDstIPPrefix(old_prefix, old_prefix_len);
		new_val_bdd = baw.encodeDstIPPrefix(new_prefix, new_prefix_len);
		field_bdd = baw.get_field_bdd(Fields.dst_ip);
		name = rname;
		priority = p;
		_baw = baw;
		new_val_ndd = NDD.table.ref(NDD.toNDD(new_val_bdd));
	}
	
	protected RewriteRule(int old_val, int field, int new_val, String rname, int p, BDDACLWrapper baw)
	{
		old_pkt_bdd = old_val;
		field_bdd = field;
		new_val_bdd = new_val;
		name = rname;
		priority = p;
		_baw = baw;
		new_val_ndd = NDD.table.ref(NDD.toNDD(new_val_bdd));
	}

	@Override
	public boolean equals(Object o)
	{
		RewriteRule rule = (RewriteRule) o;
		if (!name.equals(rule.name))
			return false;
		if (priority != rule.priority)
			return false;
		return true;	
	}
	public void setDstIP(String prefix)
	{
		new_dst_ip = prefix;
	}
	
	public void setName(String rname) {
		// TODO Auto-generated method stub
		name = rname;
	}
	
	public String toString()
	{
		return old_pkt_bdd + "->" + new_val_bdd + "=>" + name+", "+priority;
	}
	public int apply_rewrite(int old_pkt, int exists_quant, int new_val)
	{
		;
//		int tmp1 = _baw.ref(_baw.(old_pkt, exists_quant));
//		int res = _baw.ref(_baw.and(tmp1, new_val));//做了一个交集运算
//		_baw.deref(tmp1);
		return _baw.apply_rewrite(old_pkt,exists_quant,new_val);
	}
	public abstract int applyRewrite(int pkt);
	public abstract int applyRewrite_katra(int pkt);
	public int compareTo(RewriteRule rule) {
		// TODO Auto-generated method stub
		return (this.priority < rule.priority) ? -1 : (this.priority == rule.priority ? 0 : 1);
	}
}
