/*
 * Atomic Predicates Verifier
 * 
 * Copyright (c) 2013 UNIVERSITY OF TEXAS AUSTIN. All rights reserved. Developed
 * by: HONGKUN YANG and SIMON S. LAM http://www.cs.utexas.edu/users/lam/NRL/
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * with the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimers.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimers in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the UNIVERSITY OF TEXAS AUSTIN nor the names of the
 * developers may be used to endorse or promote products derived from this
 * Software without specific prior written permission.
 * 
 * 4. Any report or paper describing results derived from using any part of this
 * Software must cite the following publication of the developers: Hongkun Yang
 * and Simon S. Lam, Real-time Verification of Network Properties using Atomic
 * Predicates, IEEE/ACM Transactions on Networking, April 2016, Volume 24, No.
 * 2, pages 887-900 (first published March 2015, Digital Object Identifier:
 * 10.1109/TNET.2015.2398197).
 * 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS WITH
 * THE SOFTWARE.
 */

package apkeep.utils;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import apkeep.utils.Parameters;
import common.ACLRule;
import common.Fields;
import common.Range;
import common.Utility;
import jdd.bdd.*;
import jdd.bdd.debug.DebugBDD;

/**
 * Computes BDD for ACL rules and Forwardding rules
 * 
 * 
 * Note: The true, false, bdd variables, the negation of bdd variables: their
 * reference count are already set to maximal, so they will never be garbage
 * collected. And no need to worry about the reference count for them.
 *
 */
public class BDDACLWrapper implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7284490986562707221L;

//	BDD aclBDD;
	DebugBDD aclBDD;
	/**
	 * the arrays store BDD variables.
	 */

	final static int epgBits = 16;
	final static int vniBits = 24;
	final static int protocolBits = 8;
	final static int portBits = 16;
	final static int ipBits = 32;

	// outmost fields
	int[] dstIPInner;
	
	// vxlan fields
	int[] srcEPG;
	int[] dstEPG;
	int[] vni;
	
	// ipv4
	int[] protocol;
	int[] srcPort;
	int[] dstPort;
	int[] srcIP;
	int[] dstIP;

	// the union of specific fields
	public int dstIPField;
	public int srcIPField;
	public int vniLabelField;
	public int srcEPGLabelField;
	public int dstEPGLabelField;
	public int epgLabelField;
//	public int vniLabelFieldDecoration;
	
	// the bit represents whether the field is used
//    int dstIPInnerBit;
//    int srcEPGBit;
//    int dstEPGBit;
//    int vniLabelBit;
//    int protocolBit;
//    int srcPortBit;
//    int dstPortBit;
//    int srcIPBit;
//    int dstIPBit;
	
    Permutation push_perm;
    Permutation pop_perm;
    
    public int total_bits;

	/**
	 * for readability. In bdd: 0 is the false node 1 is the true node
	 */
	public final static int BDDFalse = 0;
	public final static int BDDTrue = 1;

	public BDDACLWrapper() {
		//aclBDD = new BDD(10000000, 10000000); // works well for 4Switch, 27us
		//aclBDD = new BDD(1000000, 1000000); // works well for stanford-noacl, 142us
		//aclBDD = new BDD(1000, 1000); // works well for internet2, 22us
//		aclBDD = new BDD(Parameters.BDD_TABLE_SIZE, Parameters.BDD_TABLE_SIZE);
		aclBDD = new DebugBDD(Parameters.BDD_TABLE_SIZE, Parameters.BDD_TABLE_SIZE);
//		aclBDD.satCount()
//		dstIPInner = new int[ipBits];
		dstIP = new int[ipBits];

		srcEPG = new int[epgBits];
		dstEPG = new int[epgBits];
		vni = new int[vniBits];
		
//		protocol = new int[protocolBits];
//		srcPort = new int[portBits];
//		dstPort = new int[portBits];
		srcIP = new int[ipBits];

		DeclareFields();

//		createTPinIPPermutation();
		
		dstIPField = AndInBatch(dstIP);
		srcIPField = AndInBatch(srcIP);
		vniLabelField = AndInBatch(vni);
		srcEPGLabelField = AndInBatch(srcEPG);
		dstEPGLabelField = AndInBatch(dstEPG);
		epgLabelField = and(srcEPGLabelField,dstEPGLabelField);
//		vniLabelFieldDecoration = aclBDD.ref(aclBDD.and(vniLabelField, vniLabelBit));
		total_bits = ipBits*2 + epgBits*2 + vniBits ;
	}
	public int getBddVar(int bdd){
		return aclBDD.getVar(bdd);
	}
	public int getBddLow(int bdd){
		return aclBDD.getLow(bdd);
	}
	public int getBddHigh(int bdd){
		return aclBDD.getHigh(bdd);
	}
	public void printField(){
		for (int i = 0; i < ipBits; i++) {
			System.out.println(dstIP[i]);
		}
//		return aclBDD.getVar(bdd);
	}
	public double satcount(int bdd){
		return aclBDD.satCount(bdd);
	}

	public void printBDD(int bdd){
		aclBDD.print(bdd);
		aclBDD.printDot("F:\\Data_set\\katra\\file",bdd);
	}
	private void DeclareFields() {
		// TODO Auto-generated method stub
		/**
		 * will try more orders of variables
		 */
		
		DeclareDstIP();//0
		DeclareSrcIP();//32
//		DeclareSrcPort();//64
//		DeclareDstPort();//80
//		DeclareProtocol();//96

		DeclareVNI();//104
		DeclareSrcEPG();//128
		DeclareDstEPG();//144
		
//		DeclareDstIPInner();//160
	}
	
	public void createTPinIPPermutation() {
		push_perm = aclBDD.createPermutation(dstIP, dstIPInner);
        pop_perm = aclBDD.createPermutation(dstIPInner, dstIP);
	}
	
	public int rewriteDstIP(int old_pkt, int new_pkt) {
		int tmp1 = aclBDD.ref(aclBDD.exists(old_pkt, dstIPField));
		int res = aclBDD.ref(aclBDD.and(tmp1, new_pkt));
		aclBDD.deref(tmp1);
		return res;
	}
	public int pushDstInnerIP(int inputAP, int OutterIPBDD) {
		int tmp1 = aclBDD.ref(aclBDD.replace(inputAP, push_perm));
        int new_pkt_bit = aclBDD.ref(aclBDD.and(OutterIPBDD, tmp1));

//		System.out.println("applying pushing: "+inputAP+"->"+new_pkt_bit);
//		int new_pkt_no_bit = aclBDD.ref(aclBDD.exists(new_pkt_bit, dstIPInnerBit));
//        int new_pkt = aclBDD.andTo(new_pkt_no_bit, dstIPInnerBit);
        
        aclBDD.deref(tmp1);
//        aclBDD.deref(new_pkt_bit);
        
//        return new_pkt;
        return new_pkt_bit;
	}
	public int popDstInnerIP(int encapsulated) {
		int tmp1 =   aclBDD.ref( aclBDD.exists(encapsulated, dstIPField) );
        int new_pkt_bit = aclBDD.ref(aclBDD.replace(tmp1, pop_perm));
//        int new_pkt_no_bit = aclBDD.ref(aclBDD.exists(new_pkt_bit, dstIPInnerBit));
//        int new_pkt = aclBDD.andTo(new_pkt_no_bit, aclBDD.not(dstIPInnerBit));

//        System.out.println(encapsulated+"->"+tmp1+"->"+new_pkt_bit+"->"+new_pkt_no_bit+"->"+new_pkt_bit);
        aclBDD.deref(tmp1);
//        aclBDD.deref(new_pkt_bit);
        
       //int support = aclBDD.ref(aclBDD.exists(new_pkt, dstIPField));
        //System.out.println("after decap:" + aclBDD.exists(support, dstIPInnerBit));
        
//        return new_pkt_no_bit;        
//        return new_pkt;
        return new_pkt_bit;
	}
	
	public int pushVNILabel(int inputAP, int vniBDD) {
		// TODO Auto-generated method stub
		int pkt_no_label = aclBDD.ref(aclBDD.exists(inputAP, vniLabelField));
//        int tmp = aclBDD.ref(aclBDD.and(pkt_no_label, vniLabelBit));
        int pkt_out = aclBDD.ref(aclBDD.and(pkt_no_label, vniBDD));
//        System.out.println("pushing vni label: "+inputAP+"->"+pkt_out);
        aclBDD.deref(pkt_no_label);
//        aclBDD.deref(tmp);
        return pkt_out;
	}
	
	public int popVNILabel(int pkt_label) {
//		int pkt = aclBDD.ref(aclBDD.exists(pkt_label, vniLabelFieldDecoration));
//        int pkt_out = aclBDD.ref(aclBDD.and(pkt, aclBDD.not(vniLabelBit)));
//        aclBDD.deref(pkt);

//        return pkt_out;
		int pkt = aclBDD.ref(aclBDD.exists(pkt_label, vniLabelField));
//        System.out.println("popping vni label: "+pkt_label+"->"+pkt);
//		int pkt = aclBDD.ref(aclBDD.exists(temp, vniLabelBit));
//		System.out.println(pkt_label+"->"+pkt);
//		aclBDD.deref(temp);
        return pkt;
	}
	public int pushEPGLabel(int inputAP, int epgBDD, int field_bdd) {
		// TODO Auto-generated method stub
		int pkt_no_label = aclBDD.ref(aclBDD.exists(inputAP, field_bdd));
//        int tmp = aclBDD.ref(aclBDD.and(pkt_no_label, vniLabelBit));
        int pkt_out = aclBDD.ref(aclBDD.and(pkt_no_label, epgBDD));
        aclBDD.deref(pkt_no_label);
//        aclBDD.deref(tmp);
        return pkt_out;
	}
	public int popEPGLabel(int pkt_label, int field_bdd) {
//		int pkt = aclBDD.ref(aclBDD.exists(pkt_label, vniLabelFieldDecoration));
//        int pkt_out = aclBDD.ref(aclBDD.and(pkt, aclBDD.not(vniLabelBit)));
//        aclBDD.deref(pkt);

//        return pkt_out;
		int pkt = aclBDD.ref(aclBDD.exists(pkt_label, field_bdd));
//		int pkt = aclBDD.ref(aclBDD.exists(temp, vniLabelBit));
//		System.out.println(pkt_label+"->"+pkt);
//		aclBDD.deref(temp);
        return pkt;
	}

	public BDD getBDD() {
		return aclBDD;
	}

    public int get_field_bdd(Fields field_name)
    {
          switch(field_name){
          case dst_ip: return dstIPField;
          default: return BDDFalse;
          }
    }


	public String getPrelen(int N){
		//N bit in32。
		int m=N/8;
		int n=N%8;
		String ans="";
		for (int i = 0; i <m ; i++) {
			ans=ans+"255.";
		}
		//128 64 32 16 8 4 2 1
		if (m==4){
			ans = ans.substring(0,ans.length() - 1);
			return ans;
		}
		switch (n){
			case 7:
				ans+="254";
				break;
			case 6:
				ans+="252";
				break;
			case 5:
				ans+="248";
				break;
			case 4:
				ans+="240";
				break;
			case 3:
				ans+="224";
				break;
			case 2:
				ans+="192";
				break;
			case 1:
				ans+="128";
				break;
			case 0:
				ans+="0";
				break;
		}
		switch (m){
			case 3:
				break;
			case 2:
				ans+=".0";
				break;
			case 1:
				ans+=".0.0";
				break;
			case 0:
				ans+=".0.0.0";
				break;
		}
		return ans;

	}
    public int get_field_bdd(String field_name)
    {
    	switch(field_name) {
    	case "vni": return vniLabelField;
    	case "srcepg": return srcEPGLabelField;
    	case "dstepg": return dstEPGLabelField;
    	case "epg": return epgLabelField;
    	default: return BDDFalse;
    	}
    }
//    /**
//     * change the field of old_pkt to new_val
//     * @param old_pkt
//     * @param new_pkt
//     * @return
//     */
    public int apply_rewrite(int old_pkt, int exists_quant, int new_val)
    {
		exists_quant=get_ipprelen_bdd(exists_quant);
		int tmp1 = aclBDD.ref(aclBDD.exists(old_pkt, exists_quant));
		int res = aclBDD.ref(aclBDD.and(tmp1, new_val));
		aclBDD.deref(tmp1);
		return res;
    }

	public int encodeSrcIPPrefix(long ipaddr, int prefixlen) {
		int[] ipbin = Utility.CalBinRep(ipaddr, ipBits);
		int[] ipbinprefix = new int[prefixlen];
		for (int k = 0; k < prefixlen; k++) {
			ipbinprefix[k] = ipbin[k + ipBits - prefixlen];
		}
		int entrybdd = EncodePrefix(ipbinprefix, srcIP, ipBits);
		return entrybdd;
	}

	public int encodeDstIPPrefix(long ipaddr, int prefixlen) {
		int[] ipbin = Utility.CalBinRep(ipaddr, ipBits);
		int[] ipbinprefix = new int[prefixlen];
		for (int k = 0; k < prefixlen; k++) {
			ipbinprefix[k] = ipbin[k + ipBits - prefixlen];
		}
		int entrybdd = EncodePrefix(ipbinprefix, dstIP, ipBits);
		return entrybdd;
	}
	public int get_ipprelen_bdd(int len)
	{
		String pre=getPrelen(len);
		long prefix_long = Utility.IPStringToLong(pre);
		int testbdd=encodeDstIPPrefix(prefix_long,len);
		return testbdd;
	}

	
	public int encodeSrcPortPrefix(long ipaddr, int prefixlen) {
		int[] ipbin = Utility.CalBinRep(ipaddr, portBits);
		int[] ipbinprefix = new int[prefixlen];
		for (int k = 0; k < prefixlen; k++) {
			ipbinprefix[k] = ipbin[k + portBits - prefixlen];
		}
		int entrybdd = EncodePrefix(ipbinprefix, srcPort, portBits);
		return entrybdd;
	}
	
	public int encodeDstPortPrefix(long ipaddr, int prefixlen) {
		int[] ipbin = Utility.CalBinRep(ipaddr, portBits);
		int[] ipbinprefix = new int[prefixlen];
		for (int k = 0; k < prefixlen; k++) {
			ipbinprefix[k] = ipbin[k + portBits - prefixlen];
		}
		int entrybdd = EncodePrefix(ipbinprefix, dstPort, portBits);
		return entrybdd;
	}
	
	public int encodeProtoPrefix(long ipaddr, int prefixlen) {
		int[] ipbin = Utility.CalBinRep(ipaddr, protocolBits);
		int[] ipbinprefix = new int[prefixlen];
		for (int k = 0; k < prefixlen; k++) {
			ipbinprefix[k] = ipbin[k + protocolBits - prefixlen];
		}
		int entrybdd = EncodePrefix(ipbinprefix, protocol, protocolBits);
		return entrybdd;
	}
	public int encodeVNI(String vni_id) {
		// TODO Auto-generated method stub
		long ipaddr = Long.valueOf(vni_id);
		int[] ipbin = Utility.CalBinRep(ipaddr, vniBits);
		int prefixlen = ipbin.length;
		int[] ipbinprefix = new int[prefixlen];
		for (int k = 0; k < prefixlen; k++) {
			ipbinprefix[k] = ipbin[k + vniBits - prefixlen];
		}
		int entrybdd = EncodePrefix(ipbinprefix, vni, vniBits);
		return entrybdd;
	}
	public int encodeSrcEPG(String epg) {
		// TODO Auto-generated method stub
		long ipaddr = Long.valueOf(epg);
		int[] ipbin = Utility.CalBinRep(ipaddr, epgBits);
		int prefixlen = ipbin.length;
		int[] ipbinprefix = new int[prefixlen];
		for (int k = 0; k < prefixlen; k++) {
			ipbinprefix[k] = ipbin[k + epgBits - prefixlen];
		}
		int entrybdd = EncodePrefix(ipbinprefix, srcEPG, epgBits);
		return entrybdd;
	}
	public int encodeDstEPG(String epg) {
		// TODO Auto-generated method stub
		long ipaddr = Long.valueOf(epg);
		int[] ipbin = Utility.CalBinRep(ipaddr, epgBits);
		int prefixlen = ipbin.length;
		int[] ipbinprefix = new int[prefixlen];
		for (int k = 0; k < prefixlen; k++) {
			ipbinprefix[k] = ipbin[k + epgBits - prefixlen];
		}
		int entrybdd = EncodePrefix(ipbinprefix, dstEPG, epgBits);
		return entrybdd;
	}



	public void multipleref(int bddnode, int reftimes) {
		for (int i = 0; i < reftimes; i++) {
			aclBDD.ref(bddnode);
		}
	}

	/**
	 * 
	 * @return the size of bdd (in bytes)
	 */
	public long BDDSize() {
		return aclBDD.getMemoryUsage();
	}

	private void DeclareVars(int[] vars, int bits) {
		for (int i = bits - 1; i >= 0; i--) {
			vars[i] = aclBDD.createVar();
		}
	}
	
	private void DeclareSrcEPG() {
		// TODO Auto-generated method stub
		DeclareVars(srcEPG, epgBits);
//		srcEPGBit = aclBDD.createVar();
	}
	
	private void DeclareDstEPG() {
		// TODO Auto-generated method stub
		DeclareVars(dstEPG, epgBits);
//		dstEPGBit = aclBDD.createVar();
	}
	
	private void DeclareVNI() {
		// TODO Auto-generated method stub
		DeclareVars(vni, vniBits);
//		vniLabelBit = aclBDD.createVar();
	}

	// protocol is 8 bits
	private void DeclareProtocol() {
		DeclareVars(protocol, protocolBits);
//		protocolBit = aclBDD.createVar();
	}

	private void DeclareSrcPort() {
		DeclareVars(srcPort, portBits);
//		srcPortBit = aclBDD.createVar();
	}

	private void DeclareDstPort() {
		DeclareVars(dstPort, portBits);
//		dstPortBit = aclBDD.createVar();
	}

	private void DeclareSrcIP() {
		DeclareVars(srcIP, ipBits);
//		srcIPBit = aclBDD.createVar();
	}

	private void DeclareDstIP() {
		DeclareVars(dstIP, ipBits);
//		dstIPBit = aclBDD.createVar();
	}
    private void DeclareDstIPInner(){
    	DeclareVars(dstIPInner, ipBits);
//    	dstIPInnerBit = aclBDD.createVar();
    }

	/**
	 * @param vars
	 *            - a list of bdd nodes that we do not need anymore
	 */
	public void DerefInBatch(int[] vars) {
		for (int i = 0; i < vars.length; i++) {
			aclBDD.deref(vars[i]);
		}
	}

	public void DerefInBatch(HashSet<Integer> vars) {
		for (Integer var: vars) {
			aclBDD.deref(var);
		}
	}
	
    public void deref(int bdd)
    {
    	aclBDD.deref(bdd);
    }

    public int ref(int bdd)
    {
       return aclBDD.ref(bdd);
    }

	/**
	 * 
	 * @param aclr
	 *            - an acl rule
	 * @return a bdd node representing this rule
	 */
	public int ConvertACLRule(ACLRule aclr) {
		/**
		 * protocol
		 */
		// no need to ref the true node
		int protocolNode = BDDTrue;
		if (aclr.protocolLower == null
				|| aclr.protocolLower.equalsIgnoreCase("any")) {
			// do nothing, just a shortcut
		} else {
			Range r = ACLRule.convertProtocolToRange(aclr.protocolLower,
					aclr.protocolUpper);
			protocolNode = ConvertProtocol(r);
		}

		/**
		 * src port
		 */
		int srcPortNode = BDDTrue;
		if (aclr.sourcePortLower == null
				|| aclr.sourcePortLower.equalsIgnoreCase("any")) {
			// do nothing, just a shortcut
		} else {
			Range r = ACLRule.convertPortToRange(aclr.sourcePortLower,
					aclr.sourcePortUpper);
			srcPortNode = ConvertSrcPort(r);
		}

		/**
		 * dst port
		 */
		int dstPortNode = BDDTrue;
		if (aclr.destinationPortLower == null
				|| aclr.destinationPortLower.equalsIgnoreCase("any")) {
			// do nothing, just a shortcut
		} else {
			Range r = ACLRule.convertPortToRange(aclr.destinationPortLower,
					aclr.destinationPortUpper);
			dstPortNode = ConvertDstPort(r);
		}

		/**
		 * src IP
		 */
		int srcIPNode = ConvertIPAddress(aclr.source, aclr.sourceWildcard,
				srcIP);

		/**
		 * dst IP
		 */
		int dstIPNode = ConvertIPAddress(aclr.destination,
				aclr.destinationWildcard, dstIP);

		// put them together
		int[] fiveFields = { protocolNode, srcPortNode, dstPortNode, srcIPNode,
				dstIPNode };
		int tempnode = AndInBatch(fiveFields);
		// clean up internal nodes
		DerefInBatch(fiveFields);

		return tempnode;
	}
	
	// bdd1 and bdd2
    public int and(int bdd1, int bdd2)
    {
        return aclBDD.ref(aclBDD.and(bdd1, bdd2));
    }
    
	// bdd1 <- bdd1 and bdd2
    public int andto(int bdd1, int bdd2)
    {
    	int and = aclBDD.ref(aclBDD.and(bdd1, bdd2));
    	aclBDD.deref(bdd1);
        return and;
    }
    
    // bdd1 or bdd2
    public int or(int bdd1, int bdd2) 
    {
        return aclBDD.ref(aclBDD.or(bdd1, bdd2));
    }
 
    // bdd1 <- bdd1 or bdd2
    public int orto(int bdd1, int bdd2) 
    {
    	int or = aclBDD.ref(aclBDD.or(bdd1, bdd2));
    	aclBDD.deref(bdd1);
        return or;
    }
    
    // bdd1 and (not bdd2)
    public int diff(int bdd1, int bdd2)
    {
        int not2 = aclBDD.ref(aclBDD.not(bdd2));
        int diff = aclBDD.ref(aclBDD.and(bdd1, not2));
        aclBDD.deref(not2);
        
        return diff;
	}
    
    // bdd1 and (not bdd2)
    public int diffnoref(int bdd1, int bdd2)
    {
        int not2 = aclBDD.ref(aclBDD.not(bdd2));
        int diff = aclBDD.and(bdd1, not2);
        aclBDD.deref(not2);
        
        return diff;
	}
   
    // bdd1 <- bdd1 and (not bdd2)
    public int diffto(int bdd1, int bdd2)
    {
        int not2 = aclBDD.ref(aclBDD.not(bdd2));
        int diff = aclBDD.ref(aclBDD.and(bdd1, not2));
        aclBDD.deref(not2);
        aclBDD.deref(bdd1);
        //System.out.println(bdd1 + ": " + aclBDD.getRef(bdd1));
        
        return diff;
    }
    // bdd1 <- not bdd1
    public int not(int bdd1) 
    {
    	int not = aclBDD.ref(aclBDD.not(bdd1));
        return not;
    }
    
    
	/**
	 * @param bddnodes
	 *            - an array of bdd nodes
	 * @return - the bdd node which is the AND of all input nodes all temporary
	 *         nodes are de-referenced. the input nodes are not de-referenced.
	 */
	public int AndInBatch(int[] bddnodes) {
		int tempnode = BDDTrue;
		for (int i = 0; i < bddnodes.length; i++) {
			if (i == 0) {
				tempnode = bddnodes[i];
				aclBDD.ref(tempnode);
			} else {
				if (bddnodes[i] == BDDTrue) {
					// short cut, TRUE does not affect anything
					continue;
				}
				if (bddnodes[i] == BDDFalse) {
					// short cut, once FALSE, the result is false
					// the current tempnode is useless now
					aclBDD.deref(tempnode);
					tempnode = BDDFalse;
					break;
				}
				int tempnode2 = aclBDD.and(tempnode, bddnodes[i]);
				aclBDD.ref(tempnode2);
				// do not need current tempnode
				aclBDD.deref(tempnode);
				// refresh
				tempnode = tempnode2;
			}
		}
		return tempnode;
	}

	/**
	 * @param bddnodes
	 *            - an array of bdd nodes
	 * @return - the bdd node which is the OR of all input nodes all temporary
	 *         nodes are de-referenced. the input nodes are not de-referenced.
	 */
	public int OrInBatch(int[] bddnodes) {
		int tempnode = BDDFalse;
		for (int i = 0; i < bddnodes.length; i++) {
			if (i == 0) {
				tempnode = bddnodes[i];
				aclBDD.ref(tempnode);
			} else {
				if (bddnodes[i] == BDDFalse) {
					// short cut, FALSE does not affect anything
					continue;
				}
				if (bddnodes[i] == BDDTrue) {
					// short cut, once TRUE, the result is true
					// the current tempnode is useless now
					aclBDD.deref(tempnode);
					tempnode = BDDTrue;
					break;
				}
				aclBDD.ref(bddnodes[i]);
				int tempnode2 = aclBDD.or(tempnode, bddnodes[i]);
				aclBDD.ref(tempnode2);
				// do not need current tempnode
				aclBDD.deref(tempnode);
				// refresh
				tempnode = tempnode2;
			}
		}
		return tempnode;
	}

	public int OrInBatch(HashSet<Integer> bddnodes) {
		int count = 0;
		int tempnode = BDDFalse;
		for (int bddnode : bddnodes) {
			if (count == 0) {
				tempnode = bddnode;
				aclBDD.ref(tempnode);
			} 
			else {
				if (bddnode == BDDFalse) {
					// short cut, FALSE does not affect anything
					continue;
				}
				if (bddnode == BDDTrue) {
					// short cut, once TRUE, the result is true
					// the current tempnode is useless now
					aclBDD.deref(tempnode);
					tempnode = BDDTrue;
					break;
				}
				int tempnode2 = aclBDD.or(tempnode, bddnode);
				aclBDD.ref(tempnode2);
				// do not need current tempnode
				aclBDD.deref(tempnode);
				// refresh
				tempnode = tempnode2;
			}
			count ++;
		}
		return tempnode;
	}
	
	/**
	 * @param
	 *
	 * @return the corresponding bdd node
	 */
	protected int ConvertIPAddress(String IP, String Mask, int[] vars) {
		int tempnode = BDDTrue;
		// case 1 IP = any
		if (IP == null || IP.equalsIgnoreCase("any")) {
			// return TRUE node
			return tempnode;
		}

		// binary representation of IP address
		int[] ipbin = Utility.IPBinRep(IP);
		// case 2 Mask = null
		if (Mask == null) {
			// no mask is working
			return EncodePrefix(ipbin, vars, ipBits);
		} else {
			int[] maskbin = Utility.IPBinRep(Mask);
			int numMasked = Utility.NumofNonZeros(maskbin);
//			System.out.println();
			int[] prefix = new int[maskbin.length - numMasked];
//			int[] prefix = new int[numMasked];
			System.out.println(IP+" "+Mask+" "+prefix.length);
			int[] varsUsed = new int[prefix.length];
			int ind = 0;
			for (int i = 0; i < maskbin.length; i++) {
//				System.out.println(IP+" "+Mask+" "+prefix.length +" "+ maskbin.length);
				if (maskbin[i] == 0) {
					prefix[ind] = ipbin[i];
					varsUsed[ind] = vars[i];
					ind++;
				}
			}

			return EncodePrefix(prefix, varsUsed, prefix.length);
		}
	}

	/***
	 * convert a range of protocol numbers to a bdd representation
	 */
	protected int ConvertProtocol(Range r) {
		return ConvertRange(r, protocol, protocolBits);

	}

	/**
	 * convert a range of source port numbers to a bdd representation
	 */
	protected int ConvertSrcPort(Range r) {
		return ConvertRange(r, srcPort, portBits);
	}

	/**
	 * convert a range of destination port numbers to a bdd representation
	 */
	protected int ConvertDstPort(Range r) {
		return ConvertRange(r, dstPort, portBits);
	}

	/**
	 * 
	 * @param r
	 *            - the range
	 * @param vars
	 *            - bdd variables used
	 * @param bits
	 *            - number of bits in the representation
	 * @return the corresponding bdd node
	 */
	private int ConvertRange(Range r, int[] vars, int bits) {

		LinkedList<int[]> prefix = Utility.DecomposeInterval(r, bits);
		// System.out.println(vars.length);
		if (prefix.size() == 0) {
			return BDDTrue;
		}

		int tempnode = BDDTrue;
		for (int i = 0; i < prefix.size(); i++) {
			if (i == 0) {
				tempnode = EncodePrefix(prefix.get(i), vars, bits);
			} else {
				int tempnode2 = EncodePrefix(prefix.get(i), vars, bits);
				int tempnode3 = aclBDD.or(tempnode, tempnode2);
				aclBDD.ref(tempnode3);
				DerefInBatch(new int[] { tempnode, tempnode2 });
				tempnode = tempnode3;
			}
		}
		return tempnode;
	}

	/**
	 * 
	 * @param prefix
	 *            -
	 * @param vars
	 *            - bdd variables used
	 * @param bits
	 *            - number of bits in the representation
	 * @return a bdd node representing the predicate e.g. for protocl, bits = 8,
	 *         prefix = {1,0,1,0}, so the predicate is protocol[4] and (not
	 *         protocol[5]) and protocol[6] and (not protocol[7])
	 */
	private int EncodePrefix(int[] prefix, int[] vars, int bits) {
		if (prefix.length == 0) {
			return BDDTrue;
		}

		int tempnode = BDDTrue;
		for (int i = 0; i < prefix.length; i++) {
			if (i == 0) {
				tempnode = EncodingVar(vars[bits - prefix.length + i],
						prefix[i]);
			} else {
				int tempnode2 = EncodingVar(vars[bits - prefix.length + i],
						prefix[i]);
				int tempnode3 = aclBDD.and(tempnode, tempnode2);
				aclBDD.ref(tempnode3);
				// do not need tempnode2, tempnode now
				// aclBDD.deref(tempnode2);
				// aclBDD.deref(tempnode);
				DerefInBatch(new int[] { tempnode, tempnode2 });
				// refresh tempnode 3
				tempnode = tempnode3;
			}
		}
		return tempnode;
	}

	/***
	 * return a bdd node representing the predicate on the protocol field
	 */
	private int EncodeProtocolPrefix(int[] prefix) {
		return EncodePrefix(prefix, protocol, protocolBits);
	}

	/**
	 * print out a graph for the bdd node var
	 */
	public void PrintVar(int var) {
		if (aclBDD.isValid(var)) {
			aclBDD.printDot(Integer.toString(var), var);
			System.out.println("BDD node " + var + " printed.");
		} else {
			System.err.println(var + " is not a valid BDD node!");
		}
	}

	/**
	 * return the size of the bdd tree
	 */
	public int getNodeSize(int bddnode) {
		int size = aclBDD.nodeCount(bddnode);
		if (size == 0) {// this means that it is only a terminal node
			size++;
		}
		return size;
	}

	/*
	 * cleanup the bdd after usage
	 */
	public void CleanUp() {
		aclBDD.cleanup();
	}

	/***
	 * var is a BDD variable if flag == 1, return var if flag == 0, return not
	 * var, the new bdd node is referenced.
	 */
	private int EncodingVar(int var, int flag) {
		if (flag == 0) {
			int tempnode = aclBDD.not(var);
			// no need to ref the negation of a variable.
			// the ref count is already set to maximal
			// aclBDD.ref(tempnode);
			return tempnode;
		}
		if (flag == 1) {
			return var;
		}

		// should not reach here
		System.err.println("flag can only be 0 or 1!");
		return -1;
	}
}
