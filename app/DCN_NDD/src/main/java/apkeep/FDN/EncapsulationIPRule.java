package apkeep.FDN;

public class EncapsulationIPRule extends RewriteRule {

	public EncapsulationIPRule(long old_prefix, int old_prefix_len, long new_prefix, int new_prefix_len, String rname,
                               int p, BDDACLWrapper baw) {
		super(old_prefix, old_prefix_len, new_prefix, new_prefix_len, rname, p, baw);
		// TODO Auto-generated constructor stub
	}

	public EncapsulationIPRule(int old_val, int field, int new_val, String rname, int p, BDDACLWrapper baw) {
		super(old_val, field, new_val, rname, p, baw);
		// TODO Auto-generated constructor stub
	}


	@Override
	public int applyRewrite_katra(int pkt) {
		// TODO Auto-generated method stub
//		System.out.println("pkt "+pkt);
		int ans=_baw.rewriteDstIP(pkt, new_val_bdd);
//		System.out.println(ans);
		return ans;
	}
	@Override
	public int applyRewrite(int pkt) {
		// TODO Auto-generated method stub
//		System.out.println("applying rule: "+toString());

//		int temp = _baw.and(old_pkt_bdd, pkt);
//		int pkt_out = _baw.pushDstInnerIP(temp, new_val_bdd);
//		_baw.deref(temp);
//		return pkt_out;


//
		int temp = _baw.and(old_pkt_bdd, pkt);
		return _baw.rewriteDstIP(temp, new_val_bdd);
	}


}
