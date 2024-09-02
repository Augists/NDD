package apkeep.FDN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import NDD.NDD;
import apkeep.FDN.FDNNetwork;
import javafx.util.*;

public class SplitMap {
    public FDNNetwork net;
    public int fieldNum;
    public HashMap<Integer, HashSet<Pair<String, String>>>[] ap_ports;
    public boolean isAction = false;

    public SplitMap(int fieldNum, FDNNetwork net) {
        this.net = net;
        this.fieldNum = fieldNum;
        ap_ports = new HashMap[fieldNum];
        for (int i = 0; i < fieldNum; i++) {
            ap_ports[i] = new HashMap<>();
            ap_ports[i].put(1, new HashSet<>());
        }
    }

    public void SetType(boolean isAction) {
        this.isAction = isAction;
    }

    public void AddDefaultPort(String dName) {
        // for (int field = 0; field < fieldNum; field++) {
        //     HashMap<Integer, HashSet<Pair<String, String>>> map = ap_ports[field];
        //     for (HashSet<Pair<String, String>> ports : map.values()) {
        //         ports.add(new Pair<>(dName, "default"));
        //     }
        // }
    }

    public void AddDefaultPort(String dName, int field) {
        // HashMap<Integer, HashSet<Pair<String, String>>> dstIPMap = ap_ports[field];
        // for (HashSet<Pair<String, String>> ports : dstIPMap.values()) {
        //     ports.add(new Pair<>(dName, "default"));
        // }
    }

    public void AddDenyPort(String dName) {
        // for (int field = 0; field < fieldNum; field++) {
        //     HashMap<Integer, HashSet<Pair<String, String>>> map = ap_ports[field];
        //     for (HashSet<Pair<String, String>> ports : map.values()) {
        //         ports.add(new Pair<>(dName, "deny"));
        //     }
        // }
    }

    public void AddDenyPort(String dName, int field) {
        // HashMap<Integer, HashSet<Pair<String, String>>> map = ap_ports[field];
        // for (HashSet<Pair<String, String>> ports : map.values()) {
        //     ports.add(new Pair<>(dName, "deny"));
        // }
    }

    public void clear() {
        for (int field = 0; field < fieldNum; field++) {
            ap_ports[field] = new HashMap<>();
        }
    }

    public void split(ArrayList<HashMap<Integer, HashSet<Integer>>> split_ap) {
        int maxField = NDD.fieldNum;
        for (int field = 0; field < maxField; field++) {
            HashMap<Integer, HashSet<Pair<String, String>>> sub_ap_ports = ap_ports[field];
            HashMap<Integer, HashSet<Integer>> apToSplit = split_ap.get(field);
            for (Map.Entry<Integer, HashSet<Integer>> entry : apToSplit.entrySet()) {
                HashSet<Pair<String, String>> ports = sub_ap_ports.get(entry.getKey());
                sub_ap_ports.remove(entry.getKey());
                for (int newAP : entry.getValue()) {
                    if(FDNNetworkAP_V2.runOriginDataset && ports == null)
                    {
                        sub_ap_ports.put(newAP, new HashSet<>());
                    }
                    else
                    {
                        sub_ap_ports.put(newAP, new HashSet<>(ports));
                    }
                }
            }
        }
    }

    public void split(HashMap<Integer, HashSet<Integer>> apToSplit, int field) {
        HashMap<Integer, HashSet<Pair<String, String>>> sub_ap_ports = ap_ports[field];
        for (Map.Entry<Integer, HashSet<Integer>> entry : apToSplit.entrySet()) {
            HashSet<Pair<String, String>> ports = sub_ap_ports.get(entry.getKey());
            sub_ap_ports.remove(entry.getKey());
            for (int newAP : entry.getValue()) {
                // System.out.println("tag1 "+sub_ap_ports);
                // System.out.println("tag2 "+ports);
                if(FDNNetworkAP_V2.runOriginDataset && ports == null)
                {
                    sub_ap_ports.put(newAP, new HashSet<>());
                }
                else
                {
                    sub_ap_ports.put(newAP, new HashSet<>(ports));
                }
            }
        }
    }
}