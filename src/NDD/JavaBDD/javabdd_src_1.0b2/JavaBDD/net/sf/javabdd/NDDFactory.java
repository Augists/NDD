package net.sf.javabdd;

import jdd.util.Configuration;
import jdd.util.Options;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.BitSet;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Set;
import java.util.Collection;
import java.util.LinkedList;

import jndd;

public class NDDFactory extends BDDFactory {

    public void free() {
        bdd = null;
    }

    public static boolean test = true;
    public static int mkCount = 0;

    public jdd.bdd.BDD bdd;
    public int[] vars;
    public bdd[] ndds;

    public jndd.NDD NDDTrue = jndd.NDDTrue;
    public jndd.NDD NDDFalse = jndd.NDDFalse;

    public int fieldNum;
    public int[] upperBound;
    public double[] div;
    public int max;

    public HashSet<BDDVector> toProtect = new HashSet<>();

    private NDDFactory(int nodeNum, int cacheSize) {
        bdd = new jdd.bdd.BDD(nodeNum, cacheSize);
        Options.verbose = true;

        vars = new int[256];
        ndds = new bdd[256];
    }

    public static BDDFactory init(int nodeNum, int cacheSize) {
        return new NDDFactory(nodeNum, cacheSize);
    }

    public void showStatus() {
        System.out.println("=================================");
        System.out.println("NDD node count: " + mkCount);
        System.out.println("BDD node count: " + bdd.mkCount);
        System.out.println("BDD show status: ");
        bdd.showStats();
        System.out.println("=================================");
    }

    /**
     * Returns the total number of {@link BDD BDDs} allocated from this {@link BDDFactory factory}
     * that were never {@link BDD#free() freed}.
     */
    @Override
    public long numOutstandingBDDs() {
        return jndd.table.refCount.size();
    }

    public int setVarNum(int[] field) {
        /*
         * maximum of field[] -> jdd
         * sum -> NDD
         * size of field[] -> table initialize
         */
        int max = 0;
        int num = 0;
        int size = field.length;
        for (int i = 0; i < size; i++) {
            if (field[i] > max)
                max = field[i];
            num += field[i];
        }
        if (num > Integer.MAX_VALUE / 2)
            throw new BDDException();

        setFieldBoundDiv(max, field);

        int old = bdd.numberOfVariables();
        int oldSize = vars.length;
        int newSize = oldSize;
        while (num > newSize) {
            newSize *= 2;
        }
        if (oldSize != newSize) {
            int[] oldVars = vars;
            bdd[] oldBdds = ndds;
            // should be max
            vars = new int[newSize];
            ndds = new bdd[newSize];
            System.arraycopy(oldVars, 0, vars, 0, old);
            System.arraycopy(oldBdds, 0, ndds, 0, oldBdds.length);
        }
        for (int k = 0; k < num; k++) {
            if (k < max) {
                vars[k] = bdd.createVar();
                bdd.ref(vars[k]);
            }

            // get field number using upperBound (k start from 0)
            int f = 0;
            int offset = 0;
            for (int i = 0; i < upperBound.length; i++) {
                if (upperBound[i] > k) {
                    // find the first upperBound upper more than k
                    // then the i equals NDD field
                    // and field start from 0
                    f = i;
                    // out of bound if using k - upperBound[i - 1] as offset
                    offset = field[i] - (upperBound[i] - k);
                    break;
                }
            }

            // jdd.BDD align right in vars
            // create edges: vars[start] -> NDDTrue
            HashMap<BDDVector, Integer> e = new HashMap<>();
            int start = max - field[f] + offset;
            e.put(NDDTrue, vars[start]);

            ndds[k] = new bdd(f, e);
            // ref in bdd initialization
            // table.ref(ndds[k]._index);
        }
        return old;
    }

    /*
     * num: sum of field[]
     */
    public void setFieldBoundDiv(int max, int[] field) {
        this.max = max;
        int size = field.length;
        fieldNum = size;

        upperBound = new int[size];
        upperBound[0] = field[0];
        for (int i = 1; i < size; i++) {
            upperBound[i] = upperBound[i - 1] + field[i];
        }

        div = new double[size];
        for (int i = 0; i < size; i++) {
            div[i] = Math.pow(2.0, max - field[i]);
        }
    }

    @Override
    public BDD ithVar(int var) {
        return ndds[var];
    }

    @Override
    public BDD nithVar(int var) {
        return new bdd(((bdd) ithVar(var))._index.NOT());
    }

    public BDD createBDD(BDDVector n) {
        return new bdd(n);
    }

    public class bdd extends BDD {
        public jndd.NDD _index;

        public bdd() {
            _index = new jndd.NDD();
        }

        public bdd(BDDVector n) {
            _index = n;
            jndd.ref(_index);
        }

        public bdd(BDD b) {
            _index = ((bdd) b)._index;
            jndd.ref(_index);
        }

        public bdd(int f, HashMap<BDDVector, Integer> e) {
            _index = jndd.addAtField(f, e);
            jndd.ref(_index);
        }

        @Override
        public BDDFactory getFactory() {
            return NDDFactory.this;
        }

        /**
         * Returns true if this BDD is a satsifiable assignment.
         *
         * <p>A BDD is an assignment if there is exactly a single path to the {@link BDDFactory#one()}
         * BDD.
         *
         * <p>Note that being an assignment does not mean that there is a value assigned to every
         * variable. See {@link #satOne()} and {@link #fullSatOne()}.
         */
        @Override
        public boolean isAssignment() {
            if (this.isOne()) {
                return true;
            } else if (this.isZero()) {
                return false;
            }
            int flag = 0;
            Iterator iter = this._index.edges.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<BDDVector, Integer> edge = (Map.Entry<BDDVector, Integer>) iter.next();
                BDD child = new bdd(edge.getKey());
                if (child.isZero()) {
                    continue;
                } else if (child.isOne()) {
                    return ((bdd) child)._index.field == fieldNum - 1 ? true : false;
                } else {
                    if (!child.isAssignment())
                        return false;
                    flag++;
                }
            }
            if (flag == 1) {
                return true;
            }
            return false;
        }

        @Override
        public boolean isZero() {
            return _index.is_False();
        }

        @Override
        public boolean isOne() {
            return _index.is_True();
        }

        @Override
        public int var() {
            BitSet bit = null;
            for (Map.Entry<BDDVector, Integer> entry : this._index.edges.entrySet()) {
                BDDVector child = entry.getKey();
                if (child.is_False())
                    return 0;
                BitSet bitset = bdd.minAssignment(entry.getValue());
                if (bit == null) {
                    bit = bitset;
                } else {
                    // compare and set the min
                    int i = 0;
                    for ( ; i < bitset.length(); i++) {
                        if (bitset.get(i) != bit.get(i))
                            break;
                    }
                    if (bit.get(i)) {
                        bit = bitset;
                    }
                }
            }
            int start = bit.nextSetBit(0);
            int offset = upperBound[this._index.field] - upperBound[this._index.field - 1];
            if (this._index.field == 0) {
                return start - (max - offset);
            } else {
                return start - (max - offset) + upperBound[this._index.field - 1];
            }
        }

        // TODO: edges array instead of low or high
        @Override
        public BDD high() {
            BDD ret = null;
            Iterator iter = this._index.edges.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<BDDVector, Integer> edge = (Map.Entry<BDDVector, Integer>) iter.next();
                BDD child = new bdd(edge.getKey());
                if (child.isOne()) {
                    return child;
                }
                ret = child;
            }
            return ret;
        }

        @Override
        public BDD low() {
            BDD ret = null;
            Iterator iter = this._index.edges.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<BDDVector, Integer> edge = (Map.Entry<BDDVector, Integer>) iter.next();
                BDD child = new bdd(edge.getKey());
                if (child.isZero()) {
                    System.out.println("return false");
                    return child;
                }
                ret = child;
            }
            if (ret.isOne()) {
                return new bdd(NDDFalse);
            }
            return ret;
        }

        @Override
        public BDD id() {
            return new bdd(_index);
        }

        @Override
        public BDD not() {
            return new bdd(jndd.Not(_index));
        }

        /**
         * {@link #not()}, but {@code this} {@link BDD} is changed to the result rather than creating a
         * new BDD object.
         */
        @Override
        public BDD notEq() {
            BDDVector result = jndd.Not(this._index);
            jndd.ref(result);
            jndd.deref(this._index);
            this._index = result;
            return this;
        }

        /**
         * Return true iff the {@code and} of the two BDDs is satisfiable. Equivalent to {@code
         * !this.and(that).isZero()}.
         *
         * @param that BDD to 'and' with
         * @return whether the 'and' is satisfiable
         */
        @Override
        public boolean andSat(BDD that) {
            return !this.and(that).isZero();
        }

        /**
         * Return true iff the {@code diff} of the two BDDs is satisfiable. Equivalent to {@code
         * !this.diff(that).isZero()}.
         *
         * @param that BDD to 'diff' with
         * @return whether the 'diff' is satisfiable
         */
        @Override
        public boolean diffSat(BDD that) {
            return !this.diff(that).isZero();
        }

        @Override
        public BDD ite(BDD thenBDD, BDD elseBDD) {
            if (this.isOne()) {
                return thenBDD;
            } else if (this.isZero()) {
                return elseBDD;
            } else {
                // TODO
                System.out.println("ite else");
                return null;
            }
        }

        @Override
        public BDD relprod(BDD that, BDD var) { return null; }

        @Override
        public BDD compose(BDD g, int var) { return null; }

        @Override
        public BDD veccompose(BDDPairing pair) { return null; }

        @Override
        public BDD constrain(BDD that) { return null; }

        @Override
        public BDD exist(BDD var) {
            return new bdd(_index.EXIST(((bdd) var)._index.field));
        }

        @Override
        BDD exist(BDD var, boolean makeNew) {
            return null;
        }

        /**
         * Return true if this BDD tests any of the variables set in the given {@code var} BDD. Equivalent
         * to {@code !this.exist(var).equals(this)}, but doesn't create BDDs.
         *
         * @param var BDD specifying the variables to test
         */
        @Override
        public boolean testsVars(BDD var) {
            return !this.exist(var).equals(this);
        }

        /**
         * Project this BDD onto the variables in the set. i.e. existentially quantify all other
         * variables.
         *
         * <p>Compare to bdd_project.
         *
         * @param var BDD containing the variables to be projected onto
         * @return the result of the projection
         * @see BDDDomain#set()
         */
        @Override
        public BDD project(BDD var) { return null; }

        @Override
        public BDD forAll(BDD var) { return null; }

        @Override
        public BDD unique(BDD var) { return null; }

        @Override
        public BDD restrict(BDD var) { return null; }

        @Override
        public BDD restrictWith(BDD var) {
            return null;
        }

        @Override
        public BDD simplify(BDD d) {
            return null;
        }

        @Override
        public BDD support() {
            return null;
        }

        /**
         * Returns the result of applying the binary operator <tt>opr</tt> to the two BDDs.
         *
         * @param that    the BDD to apply the operator on
         * @param opr     the operator to apply
         * @param makeNew whether a new BDD is created ({@code true}) or {@code this} BDD is modified.
         *                Note that {@code that} is never changed.
         * @return the result of applying the operator
         */
        @Override
        BDD apply(BDD that, BDDOp opr, boolean makeNew) {
            toProtect.clear();
            // initialize assurance for return in default case
            BDDVector r = null;
            BDDVector x = this._index;
            BDDVector y = ((bdd) that)._index;
            switch (opr.id) {
                case 0: r = jndd.AND(x, y); break;
                case 1: r = jndd.OR(jndd.AND(x, jndd.NOT(y)), (jndd.AND(y, jndd.NOT(x)))); break;  // r = bdd.xor(x, y);
                case 2: r = jndd.OR(x, y); break;
                case 3: r = jndd.NOT(jndd.AND(x, y)); break;
                case 4: r = jndd.NOT(jndd.OR(x, y)); break;
                case 5: r = jndd.OR(jndd.NOT(x), y); break;  // r = bdd.imp(x, y);
                case 6: r = x.BIIMP(y); break;     // r = bdd.biimp(x, y);
                case 7: r = jndd.DIFF(x, y); break;
                case 9: r = jndd.OR(jndd.NOT(y), x); break;  // inverse imp
                default:
                    throw new BDDException();
            }
            if (makeNew) {
                return new bdd(r);
            }
            if (!x.equals(r)) {
                jndd.ref(r);
                jndd.deref(x);
                this._index = r;
            }
            return this;
        }

        public BDD apply(BDD that, BDDOp opr) {
            return apply(that, opr, true);
        }

        @Override
        public BDD applyWith(BDD that, BDDOp opr) {
            bdd tmp = (NDDFactory.bdd) this.apply(that, opr);
            // ref new node and deref this and that
            jndd.ref(tmp._index);
            jndd.deref(this._index);
            if (!this.equals(that)) {
                // deep copy
                that.free();
            }
            this._index = tmp._index;
            return this;
        }

        @Override
        public BDD applyAll(BDD that, BDDOp opr, BDD var) {
            return null;
        }

        @Override
        public BDD applyEx(BDD that, BDDOp opr, BDD var) {
            return null;
        }

        /**
         * Shorthand for {@code this.applyEx(rel, BDDFactory.and, vars).replace(pair)}, where
         *
         * <ol>
         *   <li>vars is a varset BDD representation of the codomain of pair
         *   <li>if pair maps variable V1 to V2, then LEVEL(V1) == LEVEL(V2)+1
         * </ol>
         *
         * <p>Use case: {@code rel} represents a relation (multi-valued or nondeterministic function) as a
         * constraint over unprimed and and primed variables (unprimed variables represent inputs and
         * primed variables represent outputs), {@code x} represents a set of values as a constraint over
         * unprimed variables, and {@code pair} maps the primed variables to their corresponding unprimed
         * variables. {@code x.transform(rel, pair)} returns the image of {@code x} under {@code rel},
         * i.e. the set containing all possible results of apply {@code rel} to a value in {@code x},
         * represented as a constraint over unprimed variables.
         *
         * @param rel
         * @param pair
         */
        @Override
        public BDD transform(BDD rel, BDDPairing pair) {
            return null;
        }

        @Override
        public BDD applyUni(BDD that, BDDOp opr, BDD var) {
            return null;
        }

        @Override
        public BDD satOne() {
            BDDVector ret = jndd.satOne(this._index);
            return new bdd(ret);
        }

        @Override
        public BDD fullSatOne() {
            return null;
        }

        /**
         * Returns a {@link BitSet} containing the smallest possible assignment to this BDD, using
         * variable order.
         *
         * <p>Note that the returned {@link BitSet} is in little-Endian order. That is, the least
         * significant value in the BitSet is the first BDD variable.
         */
        @Override
        public BitSet minAssignmentBits() {
            BitSet set = new BitSet(upperBound[fieldNum - 1]);
            minassignmentbits_rec(set, this._index);
            // System.out.println("minAssignment " + set);
            return set;
        }

        private void minassignmentbits_rec(BitSet set, BDDVector ndd) {
            if (ndd.is_False() || ndd.is_True()) {
                return;
            }
            BDDVector child = null;
            BitSet bitset = null;
            for (Map.Entry<BDDVector, Integer> entry : ndd.edges.entrySet()) {
                BDDVector c = entry.getKey();
                if (c.is_False())
                    continue;
                int e = entry.getValue();
                BitSet bit = bdd.minAssignment(e);
                if (bitset == null) {
                    bitset = bit;
                    child = c;
                } else {
                    // compare to get min
                    int i = 0;
                    for ( ; i < bit.length(); i++) {
                        if (bitset.get(i) != bit.get(i))
                            break;
                    }
                    if (bitset.get(i)) {
                        bitset = bit;
                        child = c;
                    }
                }
            }
            // + offset
            // upperBound[ndd.field - 1] -
            int offset = upperBound[ndd.field] - upperBound[ndd.field - 1];
            for (int b = bitset.length(); (b = bitset.previousSetBit(b - 1)) >= 0; ) {
                if (ndd.field == 0) {
                    set.set(b - (max - offset));
                } else {
                    set.set(b - (max - offset) + upperBound[ndd.field - 1]);
                }
            }
            // set.or(bitset);
            minassignmentbits_rec(set, child);
        }

        /**
         * Finds one satisfying variable assignment, deterministically produced as a function of the seed.
         * Finds a BDD with exactly one variable at all levels. The new BDD implies this BDD and is not
         * false unless this BDD is false.
         *
         * @param seed
         * @return one satisfying variable assignment
         */
        @Override
        public BDD randomFullSatOne(int seed) {
            return null;
        }

        @Override
        public BDD satOne(BDD var, boolean pol) { return null; }

        // TODO: need all satify
        @Override
        public AllSatIterator allsat() { return null; }

        // TODO: batfish needs?
        @Override
        public BDD replace(BDDPairing pair) { return null; }

        @Override
        public BDD replaceWith(BDDPairing pair) { return null; }

        @Override
        public int nodeCount() { return 0; }

        @Override
        public double pathCount() { return 0; }

        @Override
        public double satCount() {
            return jndd.satCount(this._index);
        }

        @Override
        public int[] varProfile() { return new int[0]; }

        @Override
        public boolean equals(@Nullable Object o) {
            if (!(o instanceof bdd)) {
                return false;
            }
            return _index == ((bdd) o)._index;
        }

        public boolean equals(BDD that) {
            return this._index == ((bdd) that)._index;
        }

        @Override
        public int hashCode() { return 0; }

        @Override
        public void free() {
            jndd.deref(_index);
            // create new NDD to point to
            // _index = null;
        }
    }

    /*
     * setVarNum(int) after setVarNum(int[] field, int, int)
     * dynamically set var num
     */
    @Override
    public int setVarNum(int num) {
        if (num > Integer.MAX_VALUE / 2)
            throw new BDDException();

        int k = upperBound[fieldNum - 1];
        int newNodeCount = num - k;
        assert newNodeCount <= vars.length;
        fieldNum++;

        // grow upperBound
        int[] oldUpperBound = upperBound;
        upperBound = new int[fieldNum];
        System.arraycopy(oldUpperBound, 0, upperBound, 0, fieldNum - 1);
        upperBound[fieldNum - 1] = num - 1;
        // grow div
        // TODO: recompute div
        double[] oldDiv = div;
        div = new double[fieldNum];
        System.arraycopy(oldDiv, 0, div, 0, fieldNum - 1);
        div[fieldNum - 1] = 0;

        // grow vars and ndds
        int oldSize = vars.length;
        int newSize = oldSize;
        while (num > newSize) {
            newSize *= 2;
        }
        if (oldSize != newSize) {
            int[] oldVars = vars;
            bdd[] oldBdds = ndds;
            // should be max
            vars = new int[newSize];
            ndds = new bdd[newSize];
            System.arraycopy(oldVars, 0, vars, 0, oldVars.length);
            System.arraycopy(oldBdds, 0, ndds, 0, oldBdds.length);
        }

        // never use field to judge NDDTrue or False
        // no need to set NDDTrue and NDDFalse field++
        // NDDTrue = new NDD();
        // NDDFalse = new NDD();

        jndd.table.growTable();
        for (int i = 0; i < newNodeCount; i++) {
            // jdd.BDD align right in vars
            // create edges: vars[start] -> NDDTrue
            HashMap<BDDVector, Integer> e = new HashMap<>();
            int start = vars.length - newNodeCount + i;
            e.put(NDDTrue, vars[start]);

            ndds[k + i] = new bdd(fieldNum - 1, e);
        }
        return k;
    }

    @Override
    public BDD zero() {
        return new bdd(NDDFalse);
    }

    @Override
    public BDD one() {
        return new bdd(NDDTrue);
    }

    @Override
    public int getNodeTableSize() {
        return jndd.table.getSize();
    }

    /**
     * Run garbage collection. Returns the number of freed nodes.
     */
    @Override
    public long runGC() {
        return 0;
    }

    @Override
    public double setMinFreeNodes(double x) {
        int old = Configuration.minFreeNodesProcent;
        Configuration.minFreeNodesProcent = (int) (x * 100);
        return (double) old / 100.;
    }

    @Override
    public int varNum() {
        return upperBound[fieldNum - 1];
    }

    @Override
    protected void initialize(int nodenum, int cachesize) {}

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public double setIncreaseFactor(double x) {
        return 0;
    }

    /**
     * Sets the cache ratio for the operator caches. When the node table grows, operator caches will
     * also grow to maintain the ratio.
     *
     * <p>Compare to bdd_setcacheratio.
     *
     * @param x cache ratio
     * @return the previous cache ratio
     */
    @Override
    public int setCacheRatio(int x) {
        return 0;
    }

    /**
     * Returns the logical 'and' of zero or more BDD literals (constraints on exactly one variable --
     * i.e. the variable must be true or must be false). Does not consume any inputs, and returns a
     * fresh BDD.
     *
     * <p>Precondition: The variables' levels must be strictly increasing.
     *
     * @param literals
     */
    @Override
    public BDD andLiterals(BDD... literals) {
        if (literals.length == 0) {
            return new bdd(NDDTrue);
        }
        if (literals.length == 1) {
            return literals[0].id();
        }
        /*
        int result = 1;
        for (int i = 0; i < literals.length; i++) {
            Map<NDDFactory.NDD, Integer> e = ((NDDFactory.bdd) literals[i])._index.edges;
            Iterator<Map.Entry<NDDFactory.NDD, Integer>> iter = e.entrySet().iterator();
            int tmp = result;
            result = bdd.and(result, iter.next().getValue());
            bdd.ref(result);
            bdd.deref(tmp);
        }
        HashMap<NDDFactory.NDD, Integer> m = new HashMap<>();
        m.put(NDDTrue, result);
        NDDFactory.NDD ndd = table.mk(((NDDFactory.bdd) literals[0])._index.field, m);
        return new bdd(ndd);
         */
        BDD ret = new bdd(NDDTrue);
        for (BDD bdd : literals) {
            BDD tmp = jndd.AND(ret, bdd);
            // ref in create new bdd
            jndd.deref(((bdd) ret)._index);
            ret = tmp;
        }
        return ret;
    }

    public BDD andLiterals(boolean ip, BDD... literals) {
        int length = literals.length;
        if (length == 0) {
            return new bdd(NDDTrue);
        }
        if (length == 1) {
            return literals[0].id();
        }
        int lastFieldNum = length % 8;
        int fieldCount = length / 8;
        BDDVector ret = NDDTrue;
        while (fieldCount > 0) {
            int result = 1;
            int start = fieldCount * 8;
            int end = start + lastFieldNum;
            while (start < end) {
                Map<BDDVector, Integer> e = ((bdd) literals[start])._index.edges;
                Iterator<Map.Entry<BDDVector, Integer>> iter = e.entrySet().iterator();
                int tmp = result;
                result = bdd.and(result, iter.next().getValue());
                bdd.ref(result);
                bdd.deref(tmp);
                start++;
            }
            HashMap<BDDVector, Integer> m = new HashMap<>();
            m.put(ret, result);
            ret = jndd.addAtField(((bdd) literals[start - 1])._index.field, m);
            fieldCount--;
        }
        ret.printOut();
        return new bdd(ret);
    }

    /**
     * Implementation of {@link #andAll(Collection)} and {@link #andAllAndFree(Collection)}.
     *
     * @param bdds
     * @param free
     */
    @Override
    protected BDD andAll(Collection<BDD> bdds, boolean free) {
        if (bdds.isEmpty()) {
            return new bdd(NDDTrue);
        }
        if (bdds.size() == 1) {
            BDD bdd = bdds.iterator().next();
            return free ? bdd : bdd.id();
        }
        if (bdds.size() == 2) {
            Iterator<BDD> iter = bdds.iterator();
            BDD bdd1 = iter.next();
            BDD bdd2 = iter.next();
            return free ? bdd1.andWith(bdd2) : bdd1.and(bdd2);
        }
        BDD ret = new bdd(NDDTrue);
        for (BDD bdd : bdds) {
            ret.andWith(bdd);
        }
        /* free when in with method
         * if (free) {
         *     bdds.forEach(BDD::free);
         * }
         */
        return new bdd(ret);
    }

    /**
     * Implementation of {@link #orAll(Collection)} and {@link #orAllAndFree(Collection)}.
     *
     * @param bdds
     * @param free
     */
    @Override
    protected BDD orAll(Collection<BDD> bdds, boolean free) {
        if (bdds.isEmpty()) {
            return new bdd(NDDTrue);
        }
        if (bdds.size() == 1) {
            BDD bdd = bdds.iterator().next();
            return free ? bdd : bdd.id();
        }
        if (bdds.size() == 2) {
            Iterator<BDD> iter = bdds.iterator();
            BDD bdd1 = iter.next();
            BDD bdd2 = iter.next();
            return free ? bdd1.orWith(bdd2) : jndd.OR(bdd1, bdd2);
        }
        BDD ret = new bdd(NDDFalse);
        for (BDD bdd : bdds) {
            ret = jndd.OR(ret, bdd);
        }
        if (free) {
            bdds.forEach(BDD::free);
        }
        return new bdd(ret);
    }

    @Override
    public int setNodeTableSize(int n) { return 0; }

    @Override
    public int setCacheSize(int n) { return 0; }

    @Override
    public void printAll() {}

    @Override
    public void printTable(BDD b) {}

    @Override
    public int level2Var(int level) { return 0; }

    @Override
    public int var2Level(int var) {
        return var;
    }

    @Override
    public void setVarOrder(int[] neworder) {}

    @Override
    public BDDPairing makePair() { return null; }

    @Override
    public int duplicateVar(int var) { return 0; }

    @Override
    public int nodeCount(Collection r) { return 0; }

    @Override
    public int getNodeNum() { return 0; }

    @Override
    public int getCacheSize() { return 0; }
    @Override
    public void printStat() {}

    @Override
    protected BDDDomain createDomain(int a, BigInteger b) { return null; }

    @Override
    protected BDDBitVector createBitVector(int a) { return null; }
}