package jndd;

import jndd.NDD;

public class OPCache {
    NDD [] data;
    int width;
    int size;
    public NDD answer;
    public int hashValue;

    public OPCache(int size, int width) {
        data = new NDD [width*size];
        this.size = size;
        this.width = width;
    }

	private NDD getOut(int i) {	return data[i * width]; }
	private void setOut(int i, NDD v) { data[i * width] = v; }

	private NDD getIn(int i, int member) { return data[i * width + member]; }
	private void setIn(int i, int member, NDD v) { data[i * width + member] = v; }

	public void clear_cache() { for(int i = size; i != 0; ) invalidate(--i); }

	private void invalidate(int number) { setIn(number, 1, null); }
	private boolean isValid(int number) { return getIn(number, 1)  != null;	}

	/** this is the _correct_ way to insert something into the cache. format: (key1->value)  */
	public void insert(int hash, NDD key1, NDD value) {
		setOut(hash, value);
		setIn(hash, 1, key1);
	}

	/** this is the _correct_ way to insert something into the cache. format: (key1,key2->value)  */
	public void insert(int hash, NDD key1, NDD key2, NDD value) {
		setOut(hash, value);
		setIn(hash, 1, key1);
		setIn(hash, 2, key2);
	}

	/**
	 * lookup the element associated with (a)
	 * returns true if element found (stored in SimpleCache.answer)
	 * returns false if element not found. user should copy the hash value
	 * from SimpleCache.hash_value before doing any more cache-operations!
	 */
	public boolean lookup(NDD a) {
		int hash = good_hash(a);
		if(getIn(hash, 1)  == a){
			answer = getOut(hash);
			return true;
		} else {
			hashValue = hash;
			return false;
		}
	}

	/**
	 * lookup the element associated with (a,b)
	 * returns true if element found (stored in SimpleCache.answer)
	 * returns false if element not found. user should copy the hash value
	 * from SimpleCache.hash_value before doing any more cache-operations!
	 */
	public boolean lookup(NDD a, NDD b) {
		int hash = good_hash(a,b);
		if((getIn(hash, 1) == a && getIn(hash, 2) == b) || (getIn(hash, 1) == b && getIn(hash, 2) == a)) {
			answer = getOut(hash);
			return true;
		} else {
			hashValue = hash;
			return false;
		}
	}

	private int good_hash(NDD i) {
		// return HashFunctions.mix(i) & cache_mask;
		// NEW: cache size is prime
		return Math.abs(i.hashCode())%size;
	}

	private int good_hash(NDD i, NDD j) {
		 // return HashFunctions.mix(HashFunctions.hash_prime(i,j)) & cache_mask;
		 // NEW: cache size is prime
		 return (int) (Math.abs((((long)i.hashCode())+((long)j.hashCode())))%size);
	}
}