package jndd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import javafx.util.*;

public class NodeTable {
    public boolean lazyGC = true;
    public int maxSize = 100000;
    public int tableSize = 0;
    public double quickGrowThreshhold = 0.1;
    public ArrayList<HashMap<HashMap<NDD, Integer>, NDD>> NDDs;
    public HashMap<NDD, Integer> refCount;

    OPCache andCache;
    OPCache orCache;
    OPCache notCache;
    OPCache existCache;

    public NodeTable() {
        NDDs = new ArrayList<>();
        refCount = new HashMap<NDD, Integer>();
    }

    public NodeTable(int maxSize, int cacheSize) {
        NDDs = new ArrayList<>();
        refCount = new HashMap<NDD, Integer>();
        if (lazyGC) {
            this.maxSize = maxSize;
            andCache = new OPCache(cacheSize, 3);
            orCache = new OPCache(cacheSize, 3);
            notCache = new OPCache(cacheSize, 2);
            existCache = new OPCache(cacheSize, 2);
        }
    }

    public static boolean test = false;
    public static int mkCount = 0;

    // ensure that all used node are refed before invoking
    public NDD mk(int field, HashMap<NDD, Integer> port_pred) {
        if (port_pred.size() == 0)
            return NDD.NDDFalse;
        // redundant node
        if (port_pred.size() == 1) {
            Iterator<Map.Entry<NDD, Integer>> iterator = port_pred.entrySet().iterator();
            Map.Entry<NDD, Integer> entry = iterator.next();
            if (entry.getValue() == 1)
                return entry.getKey();
        }
        NDD node = NDDs.get(field).get(port_pred);
        // create new node
        if (node == null) {
            if (test)
                mkCount++;
            if (lazyGC)
                tableSize++;
            Iterator<Map.Entry<NDD, Integer>> iterator = port_pred.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<NDD, Integer> entry = iterator.next();
                NDD next = entry.getKey();
                if (next == NDD.NDDFalse || next == NDD.NDDTrue) {
                    continue;
                }
                refCount.put(next, refCount.get(next) + 1);
            }
            if (lazyGC && tableSize >= maxSize)
                gcOrGrow();
            NDD ret = new NDD(field, port_pred);
            NDDs.get(field).put(port_pred, ret);
            refCount.put(ret, 0);
            return ret;
        } else { // find
            Iterator<Map.Entry<NDD, Integer>> iterator = port_pred.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<NDD, Integer> entry = iterator.next();
                NDD.bdd.deref(entry.getValue());
            }
            return node;
        }
    }

    private void gcOrGrow() {
        if (!lazyGC)
            return;
        gc(); // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        if (maxSize < 100000000 && maxSize - tableSize <= maxSize * quickGrowThreshhold)
            grow();

        andCache.clear_cache();
        orCache.clear_cache();
        notCache.clear_cache();
        existCache.clear_cache();
    }

    private void gc() {
        for (NDD ndd : NDD.toProtect) {
            ref(ndd);
        }
        Queue<NDD> queue = new LinkedList<NDD>();
        Iterator<Map.Entry<NDD, Integer>> iterator = refCount.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<NDD, Integer> entry = iterator.next();
            if (entry.getValue() == 0)
                queue.add(entry.getKey());
        }
        while (!queue.isEmpty()) {
            NDD ndd = queue.poll();
            if (ndd == NDD.NDDFalse || ndd == NDD.NDDTrue)
                return;
            for (Map.Entry<NDD, Integer> entry : ndd.edges.entrySet()) {
                int refC = refCount.get(entry.getKey());
                refC--;
                refCount.put(entry.getKey(), refC);
                if (refC == 0)
                    queue.offer(entry.getKey());
            }
            NDDs.get(ndd.field).remove(ndd.edges);
            refCount.remove(ndd);
        }
        for (NDD ndd : NDD.toProtect) {
            deref(ndd);
        }
        tableSize = 0;
        for (int i = 0; i < NDD.fieldNum; i++) {
            tableSize += NDDs.get(i).size();
        }
    }

    private void grow() {
        maxSize *= 2;
    }

    public NDD ref(NDD a) {
        if (a == NDD.NDDFalse || a == NDD.NDDTrue)
            return a;
        refCount.put(a, refCount.get(a) + 1);
        return a;
    }

    public void deref(NDD a) {
        if (a == NDD.NDDFalse || a == NDD.NDDTrue)
            return;
        int newRef = refCount.get(a) - 1;
        if (newRef > 0) {
            refCount.put(a, newRef);
        } else if (newRef == 0 && !lazyGC) {
            delete(a);
        } else if (newRef < 0) {
            System.out.println("error: ret count less than 0");
            System.exit(0);
        }
    }

    public void testAndDelete(NDD a) {
        if (a == NDD.NDDFalse || a == NDD.NDDTrue || lazyGC)
            return;
        if (refCount.containsKey(a) && refCount.get(a) == 0)
            delete(a);
    }

    private void delete(NDD a) {
        Iterator<Map.Entry<NDD, Integer>> iterator = a.edges.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<NDD, Integer> entry = iterator.next();
            NDD next = entry.getKey();
            NDD.bdd.deref(entry.getValue());
            if (next == NDD.NDDFalse || next == NDD.NDDTrue)
                continue;
            deref(next);
        }
        NDDs.get(a.field).remove(a.edges);
        refCount.remove(a);
    }

    public int getSize() {
        return NDDs.size();
    }
}