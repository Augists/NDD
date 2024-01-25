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

public class NDDFactory extends BDDFactory {

    public jdd.bdd.BDD bdd;
    public NodeTable table;
    public int[] vars;
    public bdd[] ndds;

    public NDD NDDTrue = new NDD();
    public NDD NDDFalse = new NDD();

    public int fieldNum;
    public int[] upperBound;
    public double[] div;
    public int max;

    public HashSet<NDD> toProtect = new HashSet<>();

    private NDDFactory(int nodeNum, int cacheSize) {
        bdd = new jdd.bdd.BDD(nodeNum, cacheSize);
        Options.verbose = true;

        vars = new int[256];
        ndds = new bdd[256];
    }

    public static BDDFactory init(int nodeNum, int cacheSize) {
        BDDFactory f = new NDDFactory(nodeNum, cacheSize);
        // ((NDDFactory) f).setFieldBoundDiv(nodenum, 0);
        return f;
    }

    /**
     * Returns the total number of {@link BDD BDDs} allocated from this {@link BDDFactory factory}
     * that were never {@link BDD#free() freed}.
     */
    @Override
    public long numOutstandingBDDs() {
        return table.refCount.size();
    }

    public int setVarNum(int[] field, int maxSize, int cacheSize) {
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

        table = new NodeTable(size, maxSize, cacheSize);
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
            HashMap<NDD, Integer> e = new HashMap<>();
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

    public class bdd extends BDD {
        public NDD _index;

        public bdd() {
            _index = new NDD();
        }

        public bdd(NDD n) {
            _index = n;
            table.ref(_index);
        }

        public bdd(BDD b) {
            _index = ((bdd) b)._index;
            table.ref(_index);
        }

        public bdd(int f, HashMap<NDD, Integer> e) {
            _index = table.mk(f, e);
            table.ref(_index);
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
                Map.Entry<NDD, Integer> edge = (Map.Entry<NDD, Integer>) iter.next();
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
            return this._index.field;
        }

        // TODO: edges array instead of low or high
        @Override
        public BDD high() {
            BDD ret = null;
            Iterator iter = this._index.edges.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<NDD, Integer> edge = (Map.Entry<NDD, Integer>) iter.next();
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
                Map.Entry<NDD, Integer> edge = (Map.Entry<NDD, Integer>) iter.next();
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
            return new bdd(_index.NOT());
        }

        /**
         * {@link #not()}, but {@code this} {@link BDD} is changed to the result rather than creating a
         * new BDD object.
         */
        @Override
        public BDD notEq() {
            NDD result = this._index.NOT();
            table.ref(result);
            table.deref(this._index);
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
        public BDD constrain(BDD that) {
            return null;
        }

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
        public BDD project(BDD var) {
            return null;
        }

        @Override
        public BDD forAll(BDD var) { return null; }

        @Override
        public BDD unique(BDD var) { return null; }

        @Override
        public BDD restrict(BDD var) { return null;
        }

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
            NDD r = null;
            NDD x = this._index;
            NDD y = ((bdd) that)._index;
            switch (opr.id) {
                case 0: r = x.AND(y); break;
                case 1: r = x.AND(y.NOT()).OR(y.AND(x.NOT())); break;  // r = bdd.xor(x, y);
                case 2: r = x.OR(y); break;
                case 3: r = x.AND(y).NOT(); break;
                case 4: r = x.OR(y).NOT(); break;
                case 5: r = x.NOT().OR(y); break;  // r = bdd.imp(x, y);
                case 6: r = x.BIIMP(y); break;     // r = bdd.biimp(x, y);
                case 7: r = x.DIFF(y); break;
                case 9: r = y.NOT().OR(x); break;  // inverse imp
                default:
                    throw new BDDException();
            }
            if (makeNew) {
                return new bdd(r);
            }
            if (!x.equals(r)) {
                table.ref(r);
                table.deref(x);
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
            table.ref(tmp._index);
            table.deref(this._index);
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
            NDD ret = this._index.satOne_rec();
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
            System.out.println();
            System.out.println("one time minAssignment");
            BitSet set = new BitSet(upperBound[fieldNum - 1]);
            minassignmentbits_rec(set, this._index);
            System.out.println("minAssignment " + set);
            return set;
        }

        private void minassignmentbits_rec(BitSet set, NDD ndd) {
            if (ndd.is_False() || ndd.is_True()) {
                System.out.println("rec true or false");
                return;
            }
            NDD child = null;
            BitSet bitset = null;
            System.out.println("edge size " + ndd.edges.size());
            for (Map.Entry<NDD, Integer> entry : ndd.edges.entrySet()) {
                NDD c = entry.getKey();
                if (c.is_False())
                    continue;
                int e = entry.getValue();
                BitSet bit = bdd.minAssignment(e);
                System.out.println("rec edge bit " + bit);
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
                System.out.println("b " + b + " offset " + offset + " field " + ndd.field + " field start " + upperBound[ndd.field - 1]);
                set.set(b + (max - offset));
            }
            // set.or(bitset);
            System.out.println("set " + set);
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
            if (div.length == 0)
                throw new BDDException("no initialization for NDD");
            //return this._index.SATCOUNT(this._index.field);
            return this._index.SATCOUNT(0);
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
            table.deref(_index);
            // create new NDD to point to
            // _index = null;
        }
    }

    public class NDD {
        public int field;
        public HashMap<NDD, Integer> edges;

        // empty constructor for True and False NDD node
        public NDD() {
            // field start from 0
            field = fieldNum;
        }

        public NDD(int field, HashMap<NDD, Integer> edges) {
            this.field = field;
            this.edges = edges;
        }

        public NDD(NDD a) {
            this.field = a.field;
            this.edges = a.edges;
        }

        private NDD satOne_rec() {
            if (this.is_True() || this.is_False())
                return this;
            Map<NDD, Integer> edges = this.edges;
            NDD child = null;
            int edge = 0;
            for (Map.Entry<NDD, Integer> entry : edges.entrySet()) {
                if (entry.getKey().is_False())
                    continue;
                child = entry.getKey();
                edge = entry.getValue();
                break;
            }
            if (child == null)
                return NDDFalse;        // TODO
            int jddSat = bdd.oneSat(edge);
            NDD result = child.satOne_rec();
            HashMap<NDD, Integer> newEdge = new HashMap<>();
            newEdge.put(result, jddSat);
            bdd newbdd = new bdd(this.field, newEdge);
            return newbdd._index;
        }


        public boolean equals(NDD obj) {
            return this == obj;
        }

        public boolean is_True() {
            return this == NDDTrue;
        }

        public boolean is_False() {
            return this == NDDFalse;
        }

        public NDD AND(NDD b) {
            if (this.is_False() || b.is_False())
                return NDDFalse;
            if (this.is_True())
                return b;
            if (b.is_True())
                return this;
            if (this == b)
                return this;

            // check cache and save hash for insert
            if (table.andCache.lookup(this, b)) {
                return table.andCache.answer;
            }
            int hash = table.andCache.hashValue;

            if (this.field == b.field) {
                HashMap<NDD, Integer> tempSet = new HashMap<NDD, Integer>();
                Iterator itera = this.edges.entrySet().iterator();
                while (itera.hasNext()) {
                    Map.Entry<NDD, Integer> entrya = (Map.Entry<NDD, Integer>) itera.next();
                    Iterator iterb = b.edges.entrySet().iterator();
                    while (iterb.hasNext()) {
                        Map.Entry<NDD, Integer> entryb = (Map.Entry<NDD, Integer>) iterb.next();
                        int intersect = bdd.ref(bdd.and(entrya.getValue(), entryb.getValue()));
                        if (intersect == 0)
                            continue;
                        NDD subRet = entrya.getKey().AND(entryb.getKey());
                        if (subRet.is_False()) {
                            bdd.deref(intersect);
                            continue;
                        }
                        Integer pred = tempSet.get(subRet);
                        if (pred == null) {
                            pred = 0;
                        }
                        int sum = bdd.ref(bdd.or(pred, intersect));
                        bdd.deref(pred);
                        bdd.deref(intersect);
                        tempSet.put(subRet, sum);
                    }
                }
                NDD ret = table.mk(this.field, tempSet);
                toProtect.add(ret);
                table.andCache.insert(hash, this, b, ret);
                return ret;
            } else {
                if(this.field > b.field) {
                    // swap
                    return b.AND(this);
                }
                HashMap<NDD, Integer> tempSet = new HashMap<NDD, Integer>();
                Iterator itera = this.edges.entrySet().iterator();
                while (itera.hasNext()) {
                    Map.Entry<NDD, Integer> entrya = (Map.Entry<NDD, Integer>) itera.next();
                    NDD subRet = entrya.getKey().AND(b);
                    if (subRet.is_False())
                        continue;
                    Integer pred = tempSet.get(subRet);
                    if (pred == null) {
                        pred = 0;
                    }
                    int sum = bdd.ref(bdd.or(pred, entrya.getValue()));
                    bdd.deref(pred);
                    tempSet.put(subRet, sum);
                }
                NDD ret = table.mk(this.field, tempSet);
                toProtect.add(ret);
                table.andCache.insert(hash, this, b, ret);
                return ret;
            }
        }

        public NDD OR(NDD b) {
            if (this.is_True() || b.is_True())
                return NDDTrue;
            if (this.is_False())
                return b;
            if (b.is_False())
                return this;
            if (this == b)
                return this;

            // check cache and save hash for insert
            if (table.orCache.lookup(this, b))
                return table.orCache.answer;
            int hash = table.orCache.hashValue;

            if (this.field == b.field) {
                HashMap<NDD, Integer> tempSet = new HashMap<NDD, Integer>();
                HashMap<NDD, Integer> temp_a = new HashMap<NDD, Integer>(this.edges);
                for(int oneBDD : this.edges.values())
                    bdd.ref(oneBDD);
                HashMap<NDD, Integer> temp_b = new HashMap<NDD, Integer>(b.edges);
                for(int oneBDD : b.edges.values())
                    bdd.ref(oneBDD);
                Iterator itera = this.edges.entrySet().iterator();
                while (itera.hasNext()) {
                    Map.Entry<NDD, Integer> entrya = (Map.Entry<NDD, Integer>) itera.next();
                    Iterator iterb = b.edges.entrySet().iterator();
                    while (iterb.hasNext()) {
                        Map.Entry<NDD, Integer> entryb = (Map.Entry<NDD, Integer>) iterb.next();
                        int intersect = bdd.ref(bdd.and(entrya.getValue(), entryb.getValue()));
                        if (intersect == 0)
                            continue;
                        int t = temp_a.get(entrya.getKey());
                        int n = bdd.ref(bdd.not(intersect));
                        temp_a.put(entrya.getKey(),
                                bdd.ref(bdd.and(t, n)));
                        bdd.deref(t);
                        t = temp_b.get(entryb.getKey());
                        temp_b.put(entryb.getKey(),
                                bdd.ref(bdd.and(t, n)));
                        bdd.deref(t);
                        bdd.deref(n);
                        NDD subRet = entrya.getKey().OR(entryb.getKey());
                        if (subRet.is_False()) {
                            bdd.deref(intersect);
                            continue;
                        }
                        Integer pred = tempSet.get(subRet);
                        if (pred == null) {
                            pred = 0;
                        }
                        int sum = bdd.ref(bdd.or(pred, intersect));
                        bdd.deref(pred);
                        bdd.deref(intersect);
                        tempSet.put(subRet, sum);
                    }
                }
                for (Map.Entry<NDD, Integer> entry_a : temp_a.entrySet()) {
                    if (entry_a.getValue() != 0) {
                        Integer aps = tempSet.get(entry_a.getKey());
                        if (aps == null)
                            aps = 0;
                        int sum = bdd.ref(bdd.or(aps, entry_a.getValue()));
                        bdd.deref(aps);
                        bdd.deref(entry_a.getValue());
                        tempSet.put(entry_a.getKey(), sum);
                    }
                }
                for (Map.Entry<NDD, Integer> entry_b : temp_b.entrySet()) {
                    if (entry_b.getValue() != 0) {
                        Integer aps = tempSet.get(entry_b.getKey());
                        if (aps == null) {
                            aps = 0;
                        }
                        int sum = bdd.ref(bdd.or(aps, entry_b.getValue()));
                        bdd.deref(aps);
                        bdd.deref(entry_b.getValue());
                        tempSet.put(entry_b.getKey(), sum);
                    }
                }
                NDD ret = table.mk(this.field, tempSet);
                toProtect.add(ret);
                table.orCache.insert(hash, this, b, ret);
                return ret;
            } else {
                if(this.field > b.field) {
                    return b.OR(this);
                }
                HashMap<NDD, Integer> tempSet = new HashMap<NDD, Integer>();
                int false_set = 1;
                Iterator itera = this.edges.entrySet().iterator();
                while (itera.hasNext()) {
                    Map.Entry<NDD, Integer> entrya = (Map.Entry<NDD, Integer>) itera.next();
                    int n = bdd.ref(bdd.not(entrya.getValue()));
                    int temp = bdd.ref(bdd.and(false_set, n));
                    bdd.deref(false_set);
                    bdd.deref(n);
                    false_set = temp;
                    NDD subRet = entrya.getKey().OR(b);
                    if (subRet.is_False())
                        continue;
                    Integer pred = tempSet.get(subRet);
                    if (pred == null) {
                        pred = 0;
                    }
                    int sum = bdd.ref(bdd.or(pred, entrya.getValue()));
                    bdd.deref(pred);
                    tempSet.put(subRet, sum);
                }
                if (false_set != 0) {
                    Integer aps = tempSet.get(b);
                    if (aps == null) {
                        aps = 0;
                    }
                    int sum = bdd.ref(bdd.or(aps, false_set));
                    bdd.deref(aps);
                    bdd.deref(false_set);
                    tempSet.put(b, sum);
                }
                NDD ret = table.mk(this.field, tempSet);
                toProtect.add(ret);
                table.orCache.insert(hash, this, b, ret);
                return ret;
            }
        }

        public NDD BIIMP(NDD that) {
            if (this.equals(that)) {
                return NDDTrue;
            } else if (this.is_False()) {
                return that.NOT();
            } else if (this.is_True()) {
                return that;
            } else if (that.is_False()) {
                return this.NOT();
            } else if (that.is_True()) {
                return this;
            }
            // TODO
            System.out.println("BIIMP x " + this + " y " + that);
            return null;
        }

        public NDD NOT() {
            if (this.is_True())
                return NDDFalse;
            if (this.is_False())
                return NDDTrue;

            if (table.notCache.lookup(this))
                return table.notCache.answer;
            int hash = table.notCache.hashValue;

            HashMap<NDD, Integer> tempSet = new HashMap<NDD, Integer>();
            Integer false_set = 1;
            Iterator itera = this.edges.entrySet().iterator();
            while (itera.hasNext()) {
                Map.Entry<NDD, Integer> entrya = (Map.Entry<NDD, Integer>) itera.next();
                int n = bdd.ref(bdd.not(entrya.getValue()));
                int temp = bdd.ref(bdd.and(false_set, n));
                bdd.deref(false_set);
                bdd.deref(n);
                false_set = temp;
                NDD subRet = entrya.getKey().NOT();
                if (subRet.is_False())
                    continue;
                Integer pred = tempSet.get(subRet);
                if (pred == null)
                    pred = 0;
                int sum = bdd.ref(bdd.or(pred, entrya.getValue()));
                bdd.deref(pred);
                tempSet.put(subRet, sum);
            }
            if (false_set != 0) {
                Integer aps = tempSet.get(NDDTrue);
                if (aps == null)
                    aps = 0;
                int sum = bdd.ref(bdd.or(aps, false_set));
                bdd.deref(aps);
                bdd.deref(false_set);
                tempSet.put(NDDTrue, sum);
            }
            NDD ret = table.mk(this.field, tempSet);
            toProtect.add(ret);
            table.notCache.insert(hash, this, ret);
            return ret;
        }

        public NDD DIFF(NDD b) {
            NDD n = b.NOT();
            table.ref(n);
            NDD ret = this.AND(n);
            table.deref(n);
            return ret;
        }

        public NDD EXIST(int field) {
            if (this.is_True() || this.is_False()) {
                return this;
            }
            if (this.field > field) {
                return this;
            }

            if (table.existCache.lookup(this))
                return table.existCache.answer;
            int hash = table.existCache.hashValue;

            if (this.field == field) {
                NDD sum = null;
                for (NDD next : this.edges.keySet()) {
                    if (sum == null) {
                        sum = next;
                    } else {
                        NDD old = sum;
                        sum = sum.OR(next);
                        if(old != sum)
                            table.deref(old);
                    }
                }
                return sum;
            } else {
                HashMap<NDD, Integer> tempSet = new HashMap<NDD, Integer>();
                for (NDD next : this.edges.keySet()) {
                    NDD subRet = next.EXIST(field);
                    if (subRet.is_False())
                        continue;
                    int aps = 0;
                    if(tempSet.containsKey(subRet)) {
                        aps = tempSet.get(subRet);
                    }
                    int t = aps;
                    aps = bdd.ref(bdd.or(aps, this.edges.get(next)));
                    bdd.deref(t);
                    tempSet.put(subRet, aps);
                }
                if (tempSet.size() == 0)
                    return NDDFalse;
                if (tempSet.size() == 1) {
                    for (NDD key : tempSet.keySet()) {
                        if (tempSet.get(key) == 1) {
                            return key;
                        }
                    }
                }
                NDD ret = table.mk(this.field, tempSet);
                toProtect.add(ret);
                table.existCache.insert(hash, this, ret);
                return ret;
            }
        }

        public double SATCOUNT(int field) {
            if (this.is_False()) return 0;
            else if (this.is_True()) {
                if (field == fieldNum) return 1;
                else {  // in case of NDD which field != fieldNum but point to NDDTrue
                    int len = 0;
                    if (field == 0) len = upperBound[field];
                    else len = upperBound[field] - upperBound[field - 1];
                    return Math.pow(2.0, len) * SATCOUNT(field + 1);
                }
            } else {
                if (field == this.field) {
                    double sum = 0;
                    for(NDD next : this.edges.keySet()) {
                        double subCurr = bdd.satCount(this.edges.get(next)) / div[this.field];
                        double subNext = next.SATCOUNT(field + 1);
                        sum = sum + (subNext * subCurr);
                    }
                    return sum;
                } else {
                    int len = 0;
                    if (field == 0)
                        len = upperBound[field];
                    else
                        len = upperBound[field] - upperBound[field - 1];
                    return Math.pow(2.0, len) * SATCOUNT(field + 1);
                }
            }
        }

        public ArrayList<int[]> ToArray() {
            ArrayList<int[]> array = new ArrayList<>();
            int[] vec = new int[fieldNum];
            ToArray_rec(this, array, vec, 0);
            return array;
        }

        public void ToArray_rec(NDD curr, ArrayList<int[]> array, int[] vec, int currField) {
            if (curr.is_False()) {
                return;
            }
            if (curr.is_True()) {
                for (int i = currField; i < fieldNum; i++) {
                    vec[i] = 1;
                }
                int[] temp = new int[fieldNum];
                for (int i = 0; i < fieldNum; i++) {
                    temp[i] = vec[i];
                }
                array.add(temp);
                return;
            }
            for (int i = currField; i < curr.field; i++) {
                vec[i] = 1;
            }
            Iterator iter = curr.edges.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<NDD, Integer> entry = (Map.Entry<NDD, Integer>) iter.next();
                vec[curr.field] = entry.getValue();
                ToArray_rec(entry.getKey(), array, vec, curr.field + 1);
            }
        }

        public int toBDD() {
            if (this.is_True())
                return 1;
            if (this.is_False())
                return 0;
            int ret = 0;
            for (NDD next : this.edges.keySet()) {
                int curr_field = this.edges.get(next);
                int next_field = next.toBDD();
                curr_field = bdd.ref(bdd.and(curr_field, next_field));
                bdd.deref(next_field);
                int t = ret;
                ret = bdd.ref(bdd.or(t, curr_field));
                bdd.deref(t);
                bdd.deref(curr_field);
            }
            return ret;
        }

        public void printOut() {
            printOutRec(this);
            System.out.println();
        }

        private void printOutRec(NDD curr) {
            if(curr.is_True()) {
                System.out.println("T");
                return;
            }
            if(curr.is_False()) {
                System.out.println("F");
                return;
            }
            System.out.print(curr+" "+curr.field+" "+curr.edges.size());
            for(NDD next : curr.edges.keySet()) {
                if(next.is_True()) {
                    System.out.print(" T");
                } else if(next.is_False()) {
                    System.out.print(" F");
                } else {
                    System.out.print(" "+next);
                }
            }
            System.out.println();
            for(NDD next : curr.edges.keySet())
                printOutRec(next);
        }
    }

    public class NodeTable {
        public ArrayList<HashMap<HashMap<NDD, Integer>, NDD>> NDDs;
        public HashMap<NDD, Integer> refCount;

        public int maxSize = 100000;
        public int tableSize = 0;
        // judgement if gc < quickGrowThreshold then grow up table
        public double quickGrowThreshold = 0.1;

        OPCache andCache;
        OPCache orCache;
        OPCache notCache;
        OPCache existCache;

        public void growTable() {
            NDDs.add(new HashMap<>());
        }

        public NodeTable(int size) {
            NDDs = new ArrayList<>();
            for (int i = 0; i < size; i++)
                NDDs.add(new HashMap<>());
            refCount = new HashMap<NDD, Integer>();
        }

        public NodeTable(int size, int maxSize, int cacheSize) {
            NDDs = new ArrayList<>();
            for (int i = 0; i < size; i++)
                NDDs.add(new HashMap<>());
            refCount = new HashMap<NDD, Integer>();
            setMaxAndCache(maxSize, cacheSize);
        }

        public void setMaxAndCache(int maxSize, int cacheSize) {
            this.maxSize = maxSize;
            andCache = new OPCache(cacheSize, 3);
            orCache = new OPCache(cacheSize, 3);
            notCache = new OPCache(cacheSize, 2);
            existCache = new OPCache(cacheSize, 2);
        }

        public NDD mk(int field, HashMap<NDD, Integer> port_pred) { // ensure that all used node are refed before invoking
            if (port_pred.size() == 0)
                return NDDFalse;
            if (port_pred.size() == 1) { // redundant node
                Iterator<Map.Entry<NDD, Integer>> iterator = port_pred.entrySet().iterator();
                Map.Entry<NDD, Integer> entry = iterator.next();
                if (entry.getValue() == 1)
                    return entry.getKey();
            }
            NDD node = NDDs.get(field).get(port_pred);
            if (node == null) { // create new node
                tableSize++;
                Iterator<Map.Entry<NDD, Integer>> iterator = port_pred.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<NDD, Integer> entry = iterator.next();
                    NDD next = entry.getKey();
                    if (next.is_False() || next.is_True())
                        continue;
                    if (!refCount.containsKey(next))
                        System.out.println("in mk " + next);
                    refCount.put(next, refCount.get(next) + 1);
                }
                if (tableSize >= maxSize)
                    GCOrGrow();
                NDD ret = new NDD(field, port_pred);
                NDDs.get(field).put(port_pred, ret);
                refCount.put(ret, 0);
                return ret;
            } else { // find
                Iterator<Map.Entry<NDD, Integer>> iterator = port_pred.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<NDD, Integer> entry = iterator.next();
                    bdd.deref(entry.getValue());
                }
                return node;
            }
        }

        private void GCOrGrow() {
            GC();
            if ((maxSize - tableSize) <= maxSize * quickGrowThreshold) {
                grow();
            }
            andCache.clear_cache();
            orCache.clear_cache();
            notCache.clear_cache();
            existCache.clear_cache();
        }

        private void GC() {
            for (NDD ndd : toProtect) {
                ref(ndd);
            }
            Queue<NDD> queue = new LinkedList<NDD>();
            Iterator<Map.Entry<NDD, Integer>> iterator = refCount.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<NDD, Integer> entry = iterator.next();
                if (entry.getValue() == 0) {
                    queue.add(entry.getKey());
                }
            }
            while (!queue.isEmpty()) {
                NDD ndd = queue.poll();
                if (ndd.is_False() || ndd.is_True()) return;
                for (Map.Entry<NDD, Integer> entry : ndd.edges.entrySet()) {
                    NDD key = entry.getKey();
                    if (key.is_False() || key.is_True()) {
                        continue;
                    }
                    refCount.put(key, refCount.get(key) - 1);
                    if (refCount.get(key) == 0)
                        queue.offer(key);
                }
                NDDs.get(ndd.field).remove(ndd.edges);
                refCount.remove(ndd);
            }
            for (NDD ndd : toProtect) {
                deref(ndd);
            }
            tableSize = 0;
            for (int i = 0; i < fieldNum; i++)
                tableSize += NDDs.get(i).size();
        }

        private void grow() {
            maxSize *= 2;
        }

        public NDD ref(NDD a) {
            if (a == null) {
                System.out.println("ref a null NDD");
            }
            if (a == NDDFalse || a == NDDTrue) return a;
            refCount.put(a, refCount.get(a) + 1);
            return a;
        }

        public void deref(NDD a) {
            if (a == null) {
                System.out.println("deref a null NDD");
            }
            if (a == NDDFalse || a == NDDTrue) return;
            int newRef = refCount.get(a) - 1;
            if (newRef >= 0) {
                refCount.put(a, newRef);
            }
            else {  // TODO: will delete else
                System.out.println("ret count less than 0");
            }
        }

        public int getSize() {
            return NDDs.size();
        }
    }

    public class OPCache {
        NDD[] data;
        int width;
        int size;
        public NDD answer;
        public int hashValue;

        public OPCache(int size, int width) {
            data = new NDD[width*size];
            this.size = size;
            this.width = width;
        }

        private NDD getOut(int i) {
            return data[i * width];
        }

        private void setOut(int i, NDD v) {
            data[i * width] = v;
        }

        private NDD getIn(int i, int member) {
            return data[i * width + member];
        }

        private void setIn(int i, int member, NDD v) {
            data[i * width + member] = v;
        }

        public void clear_cache() {
            for (int i = size; i != 0; )
                invalidate(--i);
        }

        private void invalidate(int number) {
            setIn(number, 1, null);
        }

        private boolean isValid(int number) {
            return getIn(number, 1) != null;
        }

        // (key1 -> value)
        public void insert(int hash, NDD key1, NDD value) {
            setOut(hash, value);
            setIn(hash, 1, key1);
        }

        // (key1, key2 -> value)
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
            if((getIn(hash, 1) == a && getIn(hash, 2) == b) ||
                    (getIn(hash, 1) == b && getIn(hash, 2) == a)) {
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
            return Math.abs(i.hashCode()) % size;
        }

        private int good_hash(NDD i, NDD j) {
            // return HashFunctions.mix(HashFunctions.hash_prime(i,j)) & cache_mask;
            // NEW: cache size is prime
            return Math.abs((i.hashCode()+j.hashCode())) % size;
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

        table.growTable();
        for (int i = 0; i < newNodeCount; i++) {
            // jdd.BDD align right in vars
            // create edges: vars[start] -> NDDTrue
            HashMap<NDD, Integer> e = new HashMap<>();
            int start = vars.length - newNodeCount + i;
            e.put(NDDTrue, vars[start]);

            ndds[k + i] = new bdd(fieldNum - 1, e);
        }
        return k;
    }

    public NDD toNDD(int a) {
        HashMap<Integer, HashMap<Integer, Integer>> decomposed = decompose(a);
        HashMap<Integer, NDD> converted = new HashMap<>();
        converted.put(1, NDDTrue);
        while(decomposed.size() != 0) {
            Set<Integer> finished = converted.keySet();
            for(Map.Entry<Integer, HashMap<Integer, Integer>> entry : decomposed.entrySet()) {
                if(finished.containsAll(entry.getValue().keySet())) {
                    int field = bdd_getField(entry.getKey());
                    HashMap<NDD, Integer> map = new HashMap<>();
                    for(Map.Entry<Integer, Integer> entry1 : entry.getValue().entrySet())
                        map.put(converted.get(entry1.getKey()), bdd.ref(entry1.getValue()));
                    NDD n = table.mk(field, map);
                    converted.put(entry.getKey(), n);
                    decomposed.remove(entry.getKey());
                    break;
                }
            }
        }
        for(HashMap<Integer, Integer> map : decomposed.values())
            for(Integer pred : map.values())
                bdd.deref(pred);
        return converted.get(a);
    }

    private HashMap<Integer, HashMap<Integer, Integer>> decompose(int a) {
        HashMap<Integer, HashMap<Integer, Integer>> decomposed_bdd =
                new HashMap<Integer, HashMap<Integer, Integer>>();
        if (a == 0)
            return decomposed_bdd;
        if (a == 1) {
            HashMap<Integer, Integer> map = new HashMap<>();
            map.put(1, 1);
            decomposed_bdd.put(1, map);
            return decomposed_bdd;
        }
        HashMap<Integer, HashSet<Integer>> boundary_tree = new HashMap<Integer, HashSet<Integer>>();
        ArrayList<HashSet<Integer>> boundary_points = new ArrayList<HashSet<Integer>>();

        get_boundary_tree(a, boundary_tree, boundary_points);

        for (int curr_level = 0; curr_level < fieldNum - 1; curr_level++) {
            for (int root : boundary_points.get(curr_level)) {
                for (int end_point : boundary_tree.get(root)) {
                    int res = construct_decomposed_bdd(root, end_point, root);
                    bdd.ref(res);
                    if (!decomposed_bdd.containsKey(root)) {
                        decomposed_bdd.put(root, new HashMap<Integer, Integer>());
                    }
                    decomposed_bdd.get(root).put(end_point, res);
                }
            }
        }

        for (int abdd : boundary_points.get(fieldNum - 1)) {
            if (!decomposed_bdd.containsKey(abdd)) {
                decomposed_bdd.put(abdd, new HashMap<Integer, Integer>());
            }
            decomposed_bdd.get(abdd).put(1, bdd.ref(abdd));
        }

        return decomposed_bdd;
    }

    private void detect_boundary_point(int root, int curr, HashMap<Integer, HashSet<Integer>> boundary_tree,
            ArrayList<HashSet<Integer>> boundary_points) {
        if (curr == 0)
            return;
        if (curr == 1) {
            if (!boundary_tree.containsKey(root)) {
                boundary_tree.put(root, new HashSet<Integer>());
            }
            boundary_tree.get(root).add(1);
            return;
        }
        if (bdd_getField(root) != bdd_getField(curr)) {
            if (!boundary_tree.containsKey(root)) {
                boundary_tree.put(root, new HashSet<Integer>());
            }
            boundary_tree.get(root).add(curr);
            boundary_points.get(bdd_getField(curr)).add(curr);
            return;
        }
        detect_boundary_point(root, bdd.getLow(curr), boundary_tree, boundary_points);
        detect_boundary_point(root, bdd.getHigh(curr), boundary_tree, boundary_points);
    }

    private int construct_decomposed_bdd(int root, int end_point, int curr) {
        if (curr == 0)
            return curr;
        if (curr == 1) {
            if (end_point == 1)
                return 1;
            else
                return 0;
        } else if (bdd_getField(root) != bdd_getField(curr)) {
            if (end_point == curr)
                return 1;
            else
                return 0;
        }

        int low = bdd.getLow(curr);
        int high = bdd.getHigh(curr);

        int new_low = construct_decomposed_bdd(root, end_point, low);
        bdd.ref(new_low);
        int new_high = construct_decomposed_bdd(root, end_point, high);
        bdd.ref(new_high);

        int result = bdd.mk(bdd.getVar(curr), new_low, new_high);
        bdd.deref(new_low);
        bdd.deref(new_high);
        return result;
    }

    private void get_boundary_tree(int a, HashMap<Integer, HashSet<Integer>> boundary_tree,
                                   ArrayList<HashSet<Integer>> boundary_points) {
        int start_level;
        start_level = bdd_getField(a);
        for(int curr = 0; curr < fieldNum; curr++)
        {
            boundary_points.add(new HashSet<Integer>());
        }
        boundary_points.get(start_level).add(a);
        if (start_level == fieldNum - 1) {
            boundary_tree.put(a, new HashSet<Integer>());
            boundary_tree.get(a).add(1);
            return;
        }

        for (int curr_level = start_level; curr_level < fieldNum; curr_level++) {
            for (int abdd : boundary_points.get(curr_level)) {
                detect_boundary_point(abdd, abdd, boundary_tree, boundary_points);
            }
        }
    }

    private int bdd_getField(int a) {
        int va = bdd.getVar(a);
        if (a == 1 || a == 0)
            return fieldNum;
        int curr = 0;
        while(curr < fieldNum) {
            if(va < upperBound[curr])
                break;
            curr++;
        }
        return curr;
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
        return table.getSize();
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
        BDD ret = new bdd(NDDTrue);
        for (BDD bdd : literals) {
            BDD tmp = ret.and(bdd);
            // ref in create new bdd
            table.deref(((bdd) ret)._index);
            ret = tmp;
        }
        return ret;
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
            return free ? bdd1.orWith(bdd2) : bdd1.or(bdd2);
        }
        BDD ret = new bdd(NDDFalse);
        for (BDD bdd : bdds) {
            ret = ret.or(bdd);
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
