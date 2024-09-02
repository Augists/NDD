package apkeep.FDN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import molecule.Molecule;
import common.PositionTuple;
import javafx.util.*;

public class TranverseNodeAP {
    public PositionTuple source;
    public PositionTuple curr;
    public Molecule APs;
    HashSet<String> visited;
    ArrayList<PositionTuple> path;
    public TranverseNodeAP()
    {

    }
    public TranverseNodeAP(PositionTuple source, Molecule APs)
    {
        this.source = source;
        this.curr = source;
        this.APs = APs;
        visited = new HashSet<String>();
        // if(curr.getDeviceName().split("_").length == 1)
        // {
            visited.add(curr.getDeviceName());
        // }
        path = new ArrayList<>();
        path.add(curr);
    }
    public TranverseNodeAP(PositionTuple source, PositionTuple curr, Molecule APs, HashSet<String> visited, ArrayList<PositionTuple> path, PositionTuple next)
    {
        this.source = source;
        this.curr = curr;
        this.APs = APs;
        this.visited = new HashSet<String>(visited);
        // if(curr.getDeviceName().split("_").length == 1)
        // {
            this.visited.add(curr.getDeviceName());
        // }
        this.path = new ArrayList<>(path);
        this.path.add(next);
    }
}