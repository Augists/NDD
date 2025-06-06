package apkeep.utils;

import apkeep.core.RewriteRule;

public class NATRule extends RewriteRule {

	public NATRule(long old_prefix, int old_prefix_len, long new_prefix, int new_prefix_len, String rname, int p,
			BDDACLWrapper baw) {
		super(old_prefix, old_prefix_len, new_prefix, new_prefix_len, rname, p, baw);
		// TODO Auto-generated constructor stub
	}

	public NATRule(int old_val, int field, int new_val, String rname, int p, BDDACLWrapper baw) {
		super(old_val, field, new_val, rname, p, baw);
		// TODO Auto-generated constructor stub
	}

	@Override
	public int applyRewrite_katra(int pkt) {
		return 0;
	}

	@Override
	public int applyRewrite(int pkt) {
		// TODO Auto-generated method stub
		int temp = _baw.and(old_pkt_bdd, pkt);
		int pkt_out = _baw.rewriteDstIP(temp, new_val_bdd);
		_baw.ref(temp);
		return pkt_out;
	}
}
