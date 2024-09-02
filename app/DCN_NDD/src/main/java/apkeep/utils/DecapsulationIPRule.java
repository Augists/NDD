package apkeep.utils;

import apkeep.core.RewriteRule;

public class DecapsulationIPRule extends RewriteRule {

	public DecapsulationIPRule(long old_prefix, int old_prefix_len, long new_prefix, int new_prefix_len, String rname,
			int p, BDDACLWrapper baw) {
		super(old_prefix, old_prefix_len, new_prefix, new_prefix_len, rname, p, baw);
		// TODO Auto-generated constructor stub
	}

	public DecapsulationIPRule(int old_val, int field, int new_val, String rname, int p, BDDACLWrapper baw) {
		super(old_val, field, new_val, rname, p, baw);
		// TODO Auto-generated constructor stub
	}
	@Override
	public int applyRewrite_katra(int pkt) {

		return new_val_bdd;
	}
	@Override
	public int applyRewrite(int pkt) {
		// TODO Auto-generated method stub
		int temp = _baw.and(old_pkt_bdd, pkt);
		if (temp!=BDDACLWrapper.BDDFalse){
			_baw.deref(temp);
			return new_val_bdd;
		}
		return 0;
//		int temp = _baw.and(old_pkt_bdd, pkt);
//
//		int pkt_out = _baw.popDstInnerIP(temp);
//		_baw.deref(temp);
//		return pkt_out;
	}

}
