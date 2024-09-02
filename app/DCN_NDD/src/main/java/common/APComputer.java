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

/**
 * APComputer for packet transformer
 * only handle ref count for atomic predicates
 */

import java.io.*;
import java.util.*;

import common.*;
import jdd.bdd.BDD;

public class APComputer {

	// predicateBDD -> count
	HashMap<Integer, Integer> PredicateBDD;
	// aclbdd -> {apbdd1, apbdd2, ...}
	HashMap<Integer, HashSet<Integer>> PredicateREP;
	HashSet<Integer> AP;
	// for each ap, store the set of acls whose expression has this ap, use the index in ACLAPREP to identify the acl
	// ap -> {aclbdd1, aclbdd2, ...}
	HashMap<Integer, HashSet<Integer>> APREF;

	static BDDACLWrapper bddengine;
	
	public Set<Integer> getPredicates()
	{
		return PredicateBDD.keySet();
	}
	
	public HashMap<Integer, Integer> getPredicateBDD()
	{
	    return PredicateBDD;
	}
	
	/**
	 * 
	 * @param p
	 * @return a set of atomic predicates that cover the predicate p
	 */
	public HashSet<Integer> find_cover(int p)
	{
		HashSet<Integer> new_set = new HashSet<Integer>();
		BDD theBDD = bddengine.getBDD();
		for (int ap : AP)
		{
			if(theBDD.and(p, ap) != BDDACLWrapper.BDDFalse)
			{
				new_set.add(ap);
			}
		}
		return new_set;
	}


	private void AddPredicateCount(int pbdd)
	{
		if(PredicateBDD.containsKey(pbdd))
		{
			PredicateBDD.put(pbdd, PredicateBDD.get(pbdd) + 1);
		}else
		{
			PredicateBDD.put(pbdd, 1);
		}
	}
	
	/**
	 * 
	 * @param pbdd - port predicate to be removed
	 * @return true  - also need to change other data structures 
	 *               false - only decrease the count, no need to do anything else
	 */
	private boolean DecPredicateCount(int pbdd)
	{
	    if(PredicateBDD.containsKey(pbdd))
	    {
	        int count = PredicateBDD.get(pbdd);
	        if(count == 1)
	        {
	            PredicateBDD.remove(pbdd);
	            return true;
	        }else
	        {
	            PredicateBDD.put(pbdd, count-1);
	            return false;
	        }
	    }else
	    {
	        // if pbdd does not exist, then do nothing
	        return false;
	    }
	}

	public APComputer(ArrayList<Integer> PredicateBDDREP, BDDACLWrapper bddengine)
	{

		this(bddengine);
		ComputeAP(PredicateBDDREP);
	}

	private void iniPredicateBDD(ArrayList<Integer> PredicateBDDList)
	{
		for(int abdd : PredicateBDDList)
		{
			AddPredicateCount(abdd);
		}
	}

	public APComputer(BDDACLWrapper bddengine)
	{
		APComputer.bddengine = bddengine;
	      PredicateBDD = new HashMap<Integer, Integer> ();
            AP = new HashSet<Integer>();

	}
	
	public int addPredicates(ArrayList<Integer> predicates)
	{
	      HashSet<Integer> new_predicate = new HashSet<Integer>();
	      for(int predicate : predicates)
	      {
	            if(!PredicateBDD.containsKey(predicate))
	            {
	                  new_predicate.add(predicate);
	            }
	            AddPredicateCount(predicate);
	      }
	      
	      for(int predicate : new_predicate)
	      {
	            AddOnePredicateForAP(predicate);
	      }
	      
	      return AP.size();
	}

	public void ComputeAP(ArrayList<Integer> PredicateBDDREP)
	{
		iniPredicateBDD(PredicateBDDREP);
		int[] index = new int[PredicateBDDREP.size()];
		for(int i = 0; i < index.length; i ++)
		{
			index[i] = i;
		}
		//Sample.GetSample(index.length, index.length, index);
		CalAPs(PredicateBDDREP, index);
		CalAPExp();
		CalAPREF();
	}
	
	public void computeExpressions()
	{
	      CalAPExp();
	      CalAPREF();
	}

	static public BDDACLWrapper getBDDEngine()
	{
		return bddengine;
	}


	/**
	 * get number of aps
	 */
	public int getAPNum()
	{
		return AP.size();
	}

	/**
	 * add one acl and recompute aps 
	 * @param ind - the bddnode for the ind-th Predicate in PredicateBDDREP
	 */
	private void AddOnePredicateForAP(int bddToAdd)
	{

		BDD thebdd = bddengine.getBDD();

		int bddToAddNeg = thebdd.not(bddToAdd);
		thebdd.ref(bddToAddNeg);

		if(AP.size() == 0)
		{
			// initialize...
			if(bddToAdd != BDDACLWrapper.BDDFalse)
			{
				thebdd.ref(bddToAdd);
				AP.add(bddToAdd);
			}
			if(bddToAddNeg != BDDACLWrapper.BDDFalse)
			{
				AP.add(bddToAddNeg);
			}
		}else
		{
			// old list
			HashSet<Integer> oldList = AP;
			// set up a new list
			AP = new HashSet<Integer>();
			Iterator<Integer> iterold = oldList.iterator();
			while(iterold.hasNext())
			{
				int oldap = iterold.next();

				int tmps = thebdd.and(bddToAdd, oldap);
				thebdd.ref(tmps);
				if(tmps != BDDACLWrapper.BDDFalse)
				{
					AP.add(tmps);
				}

				tmps = thebdd.and(bddToAddNeg, oldap);
				thebdd.ref(tmps);
				if(tmps != BDDACLWrapper.BDDFalse)
				{
					AP.add(tmps);
				}
			}
			/**
			 * in this case, we need to de-ref useless nodes.
			 * we still keep bddToAdd, since it is the bdd node for an acl
			 * we will de-ref:
			 * bddToAddNeg, the whole list of oldList.
			 */
			int [] toDeRef = new int[oldList.size() + 1];
			int cntr = 0;
			for(int oldbdd : oldList)
			{
				toDeRef[cntr] = oldbdd;
				cntr ++;
			}
			toDeRef[oldList.size()] = bddToAddNeg;
			bddengine.DerefInBatch(toDeRef);
		}

	}



	/**
	 * Adapt from AtomicPredicate.CalAPs
	 * @param index - the order of combining ACLs
	 * to compute ap, need to specify index and predicatebddrep
	 */
	public void CalAPs(ArrayList<Integer> PredicateBDDList, int[] index)
	{
		HashSet<Integer> done = new HashSet<Integer> ();
		if(index.length != PredicateBDDList.size())
		{
			System.err.println("The size of indexes does not match" +
					"the number of ACLs!");
			return;
		}
		long start = System.nanoTime();
		for(int i = 0; i < index.length; i ++)
		{   
			int bddtoadd = PredicateBDDList.get(index[i]);
			if(done.contains(bddtoadd))
			{

			}else
			{
				AddOnePredicateForAP(bddtoadd);
				done.add(bddtoadd);
				//System.out.println(AP.size());
			}
		}
		long end = System.nanoTime();
		System.out.println("number of predicates: " + index.length);
		System.out.println("number of AP: " + AP.size());
		System.out.println("takes " + (end - start)/1000000.0 + " ms");
	}


	/**
	 * 
	 * @param predicatebdd
	 * @return if the acl is true or false, return either an empty set or a full set;
	 *         otherwise, return an ap expression
	 */
	public HashSet<Integer> getAPExp(int PredicateBDD)
	{
		HashSet<Integer> apexp = new HashSet<Integer> ();
		// get the expression
		if(PredicateBDD == BDDACLWrapper.BDDFalse)
		{
			return apexp;
		}
		else if ( PredicateBDD == BDDACLWrapper.BDDTrue)
		{
			return new HashSet<Integer>(AP);
		}

		for(int oneap : AP)
		{
			if(bddengine.getBDD().and(oneap, PredicateBDD) != BDDACLWrapper.BDDFalse)
			{
				apexp.add(oneap);
			}
		}
		return apexp;
	}

	/**
	 * it is already computed
	 * @param PredicateBDD
	 * @return the expression
	 */
	public HashSet<Integer> getAPExpComputed(int PredicateBDD)
	{			
		//return new HashSet<Integer>(PredicateAPREP.get(PredicateBDD));
		return PredicateREP.get(PredicateBDD);	
	}

	/**
	 * calculate ACLAPREP
	 * for true or false, we do not assign the expression
	 */
	public void CalAPExp()
	{
		PredicateREP = new HashMap<Integer, HashSet<Integer>>();
		for(int abdd : PredicateBDD.keySet())
		{
			PredicateREP.put(abdd, getAPExp(abdd));
		}
		System.out.println("AP expression for each predicate is computed.");
	}

	/**
	 * calculate APREF
	 * should be called after CalAPExp
	 */

	public void CalAPREF()
	{
		APREF = new HashMap<Integer,HashSet<Integer>> ();

		for(int apbdd : AP)
		{
			APREF.put(apbdd, new HashSet<Integer>());
		}

		for(int predicatebdd : PredicateREP.keySet())
		{	
			for(int apbdd : PredicateREP.get(predicatebdd))
			{

				APREF.get(apbdd).add(predicatebdd);
			}
		}

		System.out.println("AP Reference is computed.");

	}
	
	/**
	 * 
	 * @param predicates
	 * @return true: ap not changed, false: ap changed
	 */
	public boolean update_a(Collection<Integer> predicates)
	{
		boolean nochange = true;
		for(int onep : predicates)
		{
			if(!update_a(onep))
			{
				nochange = false;
			}
		}
		return nochange;
	}
	
	/**
	 * @return - true: ap not changed, false: ap changed
	 * @param predicatebdd
	 */
	public boolean update_a(int predicatebdd)
	{
		if(PredicateBDD.containsKey(predicatebdd))
		{
			// already has the predicate
			AddPredicateCount(predicatebdd);
			return true;
		}else
		{
			int oldsize = AP.size();
			update_a_i(predicatebdd);
			int newsize = AP.size();
			if(oldsize == newsize)
			{
				return true;
			}else
			{
				return false;
			}
		}
	}
	
	/**
	 * 
	 * @param predicatebdd - to be removed
	 * @return true - ap not changed, false - ap changed
	 */
	public boolean update_r(int predicatebdd)
	{
	    boolean res = DecPredicateCount(predicatebdd);
	    if(res)
	    {
	        return update_r_i(predicatebdd);
	    }else
	    {
	        return true;
	    }
	}
	
	/**
	 * 
	 * @param predicatebdd
	 * @return true - ap not changed, false - ap changed
	 */
	private boolean update_r_i(int predicatebdd)
	{
	     HashSet<Integer> ap_set = PredicateREP.remove(predicatebdd);
	     
	     HashMap<HashSet<Integer>, Integer> merge_dict = new HashMap<HashSet<Integer>, Integer> ();
	     ArrayList<int[]> merge_list = new ArrayList<int[]>();

	     
	     for(int ap : ap_set)
	     {
	         HashSet<Integer> ref = APREF.get(ap);
	         ref.remove(predicatebdd);
	         merge_dict.put(ref, ap);
	     }
	     
	     for(int ap : APREF.keySet())
	     {
	         HashSet<Integer> ref = APREF.get(ap);
	         if(merge_dict.containsKey(ref))
	         {
	             if(merge_dict.get(ref) != ap)
	             {
	                 int[] to_merge = new int[] {ap, merge_dict.get(ref)};
	                 merge_list.add(to_merge);
	             }
	         }
	     }
	     
	     if(merge_list.isEmpty())
	     {
	         return true;
	     }
	     
	     for(int[] to_merge : merge_list)
	     {
	         int new_ap = bddengine.or(to_merge[0], to_merge[1]);
	         bddengine.DerefInBatch(to_merge);
	         
	         HashSet<Integer> ref = APREF.get(to_merge[0]);
	         
	         for(int predicate : ref)
	         {
	             HashSet<Integer> expr = PredicateREP.get(predicate);
	             expr.remove(to_merge[0]);
	             expr.remove(to_merge[1]);
	             expr.add(new_ap);
	         }
	         
	         APREF.remove(to_merge[0]);
	         APREF.remove(to_merge[1]);
	         APREF.put(new_ap, ref);
	         
	         AP.remove(to_merge[0]);
	         AP.remove(to_merge[1]);
	         AP.add(new_ap);
	     }
	     
	     return false;
	     
	}

	/**
	 * 
	 * @param predicatebdd - the predicate bdd to add
	 * 1. update ap set, generate change list oldap -> {newap1, newap2}, generate the expression 
	 * for the new predicatebdd
	 * 2. apply change list to predicateaprep
	 * 3. update apref
	 */
	private HashMap<Integer, ArrayList<Integer>> update_a_i(int predicatebdd)
	{
		// update ap set, get change list, get the expression for the new predicate
	    // the expression for the new predicate is added to PredicateREP table
		HashMap<Integer, ArrayList<Integer>> changelist = AddOnePredicate(predicatebdd);
		// update exp for old predicates
		ApplyChangelistExp(changelist);
		// update ap ref
		ApplyChangelistRef(predicatebdd, changelist);
		
		return changelist;
	}

	/**
	 * add one predicate to the current predicate sets,
	 * change AP, PredicateBDD,get the expression for the new predicate, return change list
	 */
	private HashMap<Integer, ArrayList<Integer>> AddOnePredicate(int bddToAdd)
	{

		HashMap<Integer, ArrayList<Integer>> changelist = new HashMap<Integer, ArrayList<Integer>>();
		BDD thebdd = bddengine.getBDD();
		
		AddPredicateCount(bddToAdd);

		int bddToAddNeg = thebdd.not(bddToAdd);
		thebdd.ref(bddToAddNeg);

		// old list
		HashSet<Integer> oldList = AP;
		// set up a new list
		AP = new HashSet<Integer>();
		Iterator<Integer> iterold = oldList.iterator();
		HashSet<Integer> newexp = new HashSet<Integer>();
		while(iterold.hasNext())
		{
			int oldap = iterold.next();
			
			int tmps1 = thebdd.and(bddToAdd, oldap);
			if(tmps1 != BDDACLWrapper.BDDFalse)
			{
				thebdd.ref(tmps1);
				AP.add(tmps1);
				newexp.add(tmps1);
			}

			int tmps2 = thebdd.and(bddToAddNeg, oldap);
			if(tmps2 != BDDACLWrapper.BDDFalse)
			{
				thebdd.ref(tmps2);
				AP.add(tmps2);
			}
			
			if(tmps1 != oldap && tmps2 != oldap)
			{
				ArrayList<Integer> newary = new ArrayList<Integer>();
				newary.add(tmps1);
				newary.add(tmps2);
				changelist.put(oldap, newary);
			}
		}
		/**
		 * in this case, we need to de-ref useless nodes.
		 * we still keep bddToAdd, since it is the bdd node for an acl
		 * we will de-ref:
		 * bddToAddNeg, the whole list of oldList.
		 */
		int [] toDeRef = new int[oldList.size() + 1];
		int cntr = 0;
		for(int oldbdd : oldList)
		{
			toDeRef[cntr] = oldbdd;
			cntr ++;
		}
		toDeRef[oldList.size()] = bddToAddNeg;
		bddengine.DerefInBatch(toDeRef);
		
		PredicateREP.put(bddToAdd, newexp);

		return changelist;
	}
	
	private void ApplyChangelistExp(HashMap<Integer, ArrayList<Integer>> changelist)
	{
		for(int oldap : changelist.keySet())
		{
			HashSet<Integer> predicates = APREF.get(oldap);
			for(int onep : predicates)
			{
				PredicateREP.get(onep).remove(oldap);
				ArrayList<Integer> toadd = changelist.get(oldap);
				PredicateREP.get(onep).add(toadd.get(0));
				PredicateREP.get(onep).add(toadd.get(1));
			}
		}
	}
	
	private void ApplyChangelistRef(int bddtoadd, HashMap<Integer, ArrayList<Integer>> changelist)
	{
		for(int oldap : changelist.keySet())
		{
			HashSet<Integer> oldref = APREF.get(oldap);
			ArrayList<Integer> newaps = changelist.get(oldap);
			HashSet<Integer> newref = new HashSet<Integer> (oldref);
			APREF.put(newaps.get(0), newref);
			APREF.put(newaps.get(1), oldref);
			APREF.remove(oldap);
		}
		
		for(int oneap : PredicateREP.get(bddtoadd))
		{
			APREF.get(oneap).add(bddtoadd);
		}
	}
	

	public HashSet<Integer> getAllAP()
	{
		return AP;
	}

	public static void main(String[] args) throws FileNotFoundException
	{
	    BDDACLWrapper bddengine = new BDDACLWrapper();
	    BDD bdd = bddengine.getBDD();
	    int v1 = bdd.createVar();
	    int v2 = bdd.createVar();
	    int v3 = bdd.createVar();
	    int p1 = v1;
	    int p2 = bdd.ref(bdd.and(v1, v2));
	    int p3 = bdd.ref(bdd.and(v1, v3));
	    
	    ArrayList<Integer> predicates = new ArrayList<Integer>();
	    predicates.add(p1);
	    predicates.add(p2);
	    predicates.add(p3);
	    
	    APComputer apc = new APComputer(predicates, bddengine);
	    System.out.println(apc.PredicateREP);
	    System.out.println(apc.APREF);
	    
	    apc.update_r(p3);
	    System.out.println(apc.PredicateREP);
	    System.out.println(apc.APREF);
	    
	    apc.update_a(p3);
	    System.out.println(apc.PredicateREP);
	    System.out.println(apc.APREF);
	    
	    apc.update_r(p2);
        System.out.println(apc.PredicateREP);
        System.out.println(apc.APREF);
        
        apc.update_a(p2);
        System.out.println(apc.PredicateREP);
        System.out.println(apc.APREF);
	    
	}
}


