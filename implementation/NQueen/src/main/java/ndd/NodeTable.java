package ndd;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javafx.util.*;

public class NodeTable {
    public HashMap<HashMap<NDD, Integer>, NDD> NDDs;
    public HashMap<NDD, Integer> refCount;

    public NodeTable() {
        NDDs = new HashMap<HashMap<NDD, Integer>, NDD>();
        refCount = new HashMap<NDD, Integer>();
    }

    // public static boolean test = true;
    // public static int mkCount = 0;

    public NDD mk(int field, HashMap<NDD, Integer> port_pred) // ensure that all used node are refed before invoking
    {
        if (port_pred.size() == 0)
        {
            return NDD.NDDFalse;
        }
        if (port_pred.size() == 1) // redundant node
        {
            Iterator<Map.Entry<NDD, Integer>> iterator = port_pred.entrySet().iterator();
            Map.Entry<NDD, Integer> entry = iterator.next();
            if(entry.getValue() == 1)return entry.getKey();
        }
        NDD node = NDDs.get(port_pred);
        if (node == null) // create new node
        {
            // if(test)mkCount++;
            NDD ret = new NDD(field, port_pred);
            NDDs.put(port_pred, ret);
            refCount.put(ret, 0);
            Iterator<Map.Entry<NDD, Integer>> iterator = port_pred.entrySet().iterator();
            while(iterator.hasNext())
            {
                Map.Entry<NDD, Integer> entry = iterator.next();
                NDD next = entry.getKey();
                if (next == NDD.NDDFalse || next == NDD.NDDTrue) {
                    continue;
                }
                refCount.put(next, refCount.get(next)+1);
            }
            return ret;
        } else // find
        {
            Iterator<Map.Entry<NDD, Integer>> iterator = port_pred.entrySet().iterator();
            while(iterator.hasNext())
            {
                Map.Entry<NDD, Integer> entry = iterator.next();
                NDD.bdd.deref(entry.getValue());
            }
            return node;
        }
    }

    public NDD ref(NDD a) {
        if(a == NDD.NDDFalse || a == NDD.NDDTrue)return a;
        refCount.put(a, refCount.get(a)+1);
        return a;
    }

    public void deref(NDD a) {
        if(a == NDD.NDDFalse || a == NDD.NDDTrue)return;
        int newRef = refCount.get(a)-1;
        if (newRef > 0) {
            refCount.put(a, newRef);
        } else if (newRef == 0) {
            delete(a);
        } else {
            System.out.println("error: ret count less than 0");
        }
    }

    public void testAndDelete(NDD a)
    {
        if(a == NDD.NDDFalse || a == NDD.NDDTrue)return;
        if(refCount.containsKey(a) && refCount.get(a) == 0)
        {
            delete(a);
        }
    }

    private void delete(NDD a) {
        Iterator<Map.Entry<NDD, Integer>> iterator = a.edges.entrySet().iterator();
        while(iterator.hasNext())
        {
            Map.Entry<NDD, Integer> entry = iterator.next();
            NDD next = entry.getKey();
            NDD.bdd.deref(entry.getValue());
            if (next == NDD.NDDFalse || next == NDD.NDDTrue) {
                continue;
            }
            deref(next);

        }
        NDDs.remove(a.edges);
        refCount.remove(a);
    }
}