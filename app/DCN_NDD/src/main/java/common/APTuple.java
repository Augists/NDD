/*
 * Atomic Predicates for Transformers
 * 
 * Copyright (c) 2015 UNIVERSITY OF TEXAS AUSTIN. All rights reserved. Developed
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
 * and Simon S. Lam, Scalable Verification of Networks With Packet Transformers
 * Using Atomic Predicates, IEEE/ACM Transactions on Networking, October 2017,
 * Volume 25, No. 5, pages 2900-2915 (first published as IEEE Early Access
 * Article, July 2017, Digital Object Identifier: 10.1109/TNET.2017.2720172).
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
package common;

public class APTuple {

	FWDAPSet fwdaps;
	ACLAPSet aclaps;
	
	/**
	 * 
	 * @param type 0,1
	 */
	public APTuple(int type)
	{
		if(type == BDDACLWrapper.BDDTrue)
		{
			fwdaps = new FWDAPSet(BDDACLWrapper.BDDTrue);
			aclaps = new ACLAPSet(BDDACLWrapper.BDDTrue);
		}else
		{
			fwdaps = new FWDAPSet(BDDACLWrapper.BDDFalse);
			aclaps = new ACLAPSet(BDDACLWrapper.BDDFalse);
		}
	}
	
	public APTuple(FWDAPSet fwdaps, ACLAPSet aclaps)
	{
		this.fwdaps = fwdaps;
		this.aclaps = aclaps;
	}
	
	public APTuple(APTuple anotherapt)
	{
		this.fwdaps = new FWDAPSet(anotherapt.fwdaps);
		this.aclaps = new ACLAPSet(anotherapt.aclaps);
	}
	
	public void union(APTuple anotherapt)
	{
		fwdaps.union(anotherapt.fwdaps);
		aclaps.union(anotherapt.aclaps);
	}
	
	public void union(FWDAPSet faps)
	{
		fwdaps.union(faps);
	}
	
	public void union(ACLAPSet aaps)
	{
		aclaps.union(aaps);
	}
	
	public void intersect(APTuple anotherapt)
	{
		fwdaps.intersect(anotherapt.fwdaps);
		aclaps.intersect(anotherapt.aclaps);
	}
	
	public void intersect(FWDAPSet faps)
	{
		fwdaps.intersect(faps);
	}
	
	public void intersect(ACLAPSet aaps)
	{
		aclaps.intersect(aaps);
	}
	
	public boolean isempty()
	{
		return fwdaps.isempty() || aclaps.isempty();
	}
	
	public boolean isfull()
	{
		return fwdaps.isfull() && aclaps.isfull();
	}
	
	public String toString()
	{
		return "(" + fwdaps + "," + aclaps + ")";
	}
}
