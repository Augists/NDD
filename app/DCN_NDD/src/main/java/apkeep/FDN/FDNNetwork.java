package apkeep.FDN;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;

import NDD.NDD;
// import apkeep.core.APKeeperNew;
import apkeep.core.ChangeItem;
import apkeep.utils.*;

import common.*;
import javafx.util.Pair;
import jdd.bdd.BDD;
import jdd.bdd.Permutation;

public class FDNNetwork {
    public static int field_num = 6;
    public static int dst_ip_field = 0;
    public static int src_ip_field = 1;
    public static int vni_field = 2;
    public static int src_epg_field = 3;
    public static int dst_epg_field = 4;
    public static int outter_ip_field = 5;
    public static int multi_filed = 6;
    public static int no_field = 7;

    public static boolean checkCorrectness = false;

    public String name;

    public String currStage;

    public HashMap<PositionTuple, HashSet<PositionTuple>> topology;
    public HashMap<String, HashSet<String>> edge_ports;
    public HashSet<String> startEndPoint;// leaf-0-0_vpn4
    public HashSet<String> endEndPoint;// bd

    public HashMap<String, FieldNode> FieldNodes;

    public BDDACLWrapper bdd_engine;

    protected String config_folder;
    protected String acl_folder;
    protected String parsed_folder;
    protected String edge_port_folder;

    /*
     * Local variables
     */
    protected HashSet<String> acl_node_names;
    protected HashSet<String> nat_element_names;
    HashSet<String> natelement = new HashSet<>();

    protected ArrayList<ChangeItem> change_set;
    protected HashSet<Integer> moved_aps;

    protected Checker checker;

    public NDD affected_aps = NDD.NDDTrue;

    protected int[] now_v = new int[32];
    int index_now = 0;
    protected int[] max_v = new int[32];
    protected int[] min_v = new int[32];

    String parsedRoot;
    String updateRoot;

    // ip -> device -> (+/-, port)
    public HashMap<String, HashMap<String, HashSet<Pair<String, String>>>> fwd_rules;
    // vni -> device -> (+/-, port)
    public HashMap<String, HashMap<String, HashSet<Pair<String, String>>>> vc_rules;
    // src/dst -> device -> (+/-, port)
    public HashMap<String, HashMap<String, HashSet<Pair<String, String>>>> gbp_rules;
    public HashMap<String, HashSet<ACLRule>> acl_rules;
    // vpn -> inner ip/mask -> (+/-, outter ip)
    public Hashtable<String, Hashtable<String, HashSet<Pair<String, String>>>> ip_encap_rules;
    public Hashtable<String, Hashtable<String, HashSet<Pair<String, String>>>> Ip_encap_rules;
    // vpn -> vni
    public Hashtable<String, String> vni_table;
    // nveip -> device
    public Hashtable<String, HashSet<String>> nve_table;
    // vpn -> epg id -> (+/-, ip/mask)
    public Hashtable<String, Hashtable<String, HashSet<Pair<String, String>>>> epg_table;
    public Hashtable<String, Hashtable<String, HashSet<Pair<String, String>>>> EPG_table;

    public boolean isBatch = false;
    boolean splitBatch = true;
    boolean show_node_num = true;

    public int fe = 0;
    public int vpne = 0;
    public int ne = 0;
    public int ae = 0;

    public int epge = 0;
    public int ipe = 0;
    public int vnie = 0;

    public FDNNetwork(String name, String mode, String postid, String dataset, boolean extend) {
        this.name = name;
        currStage = postid;

        topology = new HashMap<PositionTuple, HashSet<PositionTuple>>();
        edge_ports = new HashMap<String, HashSet<String>>();

        FieldNodes = new HashMap<String, FieldNode>();

        acl_node_names = new HashSet<String>();
        nat_element_names = new HashSet<String>();

        String tinytopo = dataset + "/";
        String path = "Field-Decision-Network/"
        String root = path + "/apkatra-main/src/main/java/dataset/";

        if (dataset.startsWith("APKeep") || dataset.startsWith("GateAPKeep") || dataset.startsWith("V2")
                || dataset.startsWith("V3")) {
            parsedRoot = root + tinytopo + "parsed/";
            updateRoot = root + tinytopo + "update/";
        } else if (dataset.split("_").length == 2 && dataset.split("_")[0].equals("DCN")) {
            tinytopo = tinytopo + "/format-apkeep/";
            parsedRoot = root + tinytopo + "parsed/";
            updateRoot = root + tinytopo + "update/";
        } else {
            tinytopo = tinytopo + "/format-apkeep/";
            parsedRoot = root + tinytopo + "parsed/";
            updateRoot = root + tinytopo + "update/";
        }

        fwd_rules = new HashMap<>();
        vc_rules = new HashMap<>();
        gbp_rules = new HashMap<>();
        acl_rules = new HashMap<>();
        ip_encap_rules = new Hashtable<>();
        vni_table = new Hashtable<>();
        nve_table = new Hashtable<>();
        epg_table = new Hashtable<>();
        startEndPoint = new HashSet<>();

        endEndPoint = new HashSet<>();
    }

    public FDNNetwork(String name, String mode, String postid, String dataset) throws Exception {

        Runtime r = Runtime.getRuntime();
        r.gc();
        r.gc();
        long m1 = r.totalMemory() - r.freeMemory();

        this.name = name;
        currStage = postid;

        topology = new HashMap<PositionTuple, HashSet<PositionTuple>>();
        edge_ports = new HashMap<String, HashSet<String>>();

        FieldNodes = new HashMap<String, FieldNode>();

        int[] upperBound = new int[field_num];
        upperBound[0] = 31;
        upperBound[1] = 63;
        upperBound[2] = 87;
        upperBound[3] = 103;
        upperBound[4] = 119;
        upperBound[5] = 151;
        bdd_engine = new BDDACLWrapper();
        NDD.SetFieldNum(field_num);
        NDD.SetUpperBound(upperBound);

        acl_node_names = new HashSet<String>();
        nat_element_names = new HashSet<String>();

        FieldNode.network = this;
        FieldNode.bdd = bdd_engine;
        NDD.bdd = bdd_engine.getBDD();

        String tinytopo = dataset + "/";
        String path = "Field-Decision-Network/"
        String root = path + "/apkatra-main/src/main/java/dataset/";

        if (dataset.startsWith("APKeep") || dataset.startsWith("NewAPKeep")) {
            parsedRoot = root + tinytopo + "parsed/";
            updateRoot = root + tinytopo + "update/";
        } else if (dataset.split("_").length == 2 && dataset.split("_")[0].equals("DCN")) {
            tinytopo = tinytopo + "/format-apkeep/";
            parsedRoot = root + tinytopo + "parsed/";
            updateRoot = root + tinytopo + "update/";
        } else {
            tinytopo = tinytopo + "/format-apkeep/";
            parsedRoot = root + tinytopo + "parsed/";
            updateRoot = root + tinytopo + "update/";
        }

        fwd_rules = new HashMap<>();
        vc_rules = new HashMap<>();
        gbp_rules = new HashMap<>();
        acl_rules = new HashMap<>();
        ip_encap_rules = new Hashtable<>();
        vni_table = new Hashtable<>();
        nve_table = new Hashtable<>();
        epg_table = new Hashtable<>();
        startEndPoint = new HashSet<>();

        endEndPoint = new HashSet<>();

        double t1 = System.nanoTime();

        constructPPMFromFile(postid);

        double t2 = System.nanoTime();

        // NDD.test = true;
        isBatch = true;
        updatePPMSnapshot_Increasement();
        affected_aps = NDD.NDDTrue;

        double t3 = System.nanoTime();
        // System.out.println("update model:" + (t3 - t2) / 1000000000);
        pw.println("update model:" + (t3 - t2) / 1000000000);
        pw.flush();

        // CheckCorrectness();

        // bdd_engine.getBDD().showStats();
        checker = new Checker(this, new HashSet<String>(), new HashMap<>());
        checker.PropertyCheck();
        // System.out.println();
        // bdd_engine.getBDD().showStats();
        pw.println("reach size:" + checker.reach.size());
        pw.flush();

        double t4 = System.nanoTime();

        pw.println("property check:" + (t4 - t3) / 1000000000);
        pw.flush();
        pw.println("total:" + (t4 - t2) / 1000000000);
        pw.flush();

        // for(String device : startEndPoint)
        // {
        // double perT1 = System.nanoTime();
        // checker = new Checker(this, device);
        // checker.PropertyCheck();
        // double perT2 = System.nanoTime();
        // System.out.println(device + " "+ (perT2 - perT1) / 1000000000);
        // }

        Runtime r1 = Runtime.getRuntime();
        r1.gc();
        r1.gc();
        long m2 = r1.totalMemory() - r1.freeMemory();

        pw.println("memory:" + (m2 - m1) / (1024 * 1024) + "MB");
        pw.flush();

        // if(checkCorrectness)
        // {
        // checker.OutputReachBDD();
        // }
    }

    public static double model_realtime = 0;
    public static double check_realtime = 0;

    public void processDifferentialUpdate(String pre_state, String curr_state) throws Exception {
        // System.out.println();
        // System.out.println("update: " + pre_state + " to " + curr_state);
        pw.println();
        pw.flush();
        pw.println("update: " + pre_state + " to " + curr_state);
        pw.flush();
        // time1 = 0;
        // time2 = 0;
        // time3 = 0;
        // time4 = 0;
        // time5 = 0;
        affected_aps = NDD.NDDFalse;
        currStage = curr_state;
        double t1 = System.nanoTime();
        updatePPMFromFile(pre_state, curr_state);
        double t2 = System.nanoTime();
        // System.out.println("parse file:" + (t2 - t1) / 1000000000);
        pw.println("parse file:" + (t2 - t1) / 1000000000);
        pw.flush();
        updatePPMSnapshot_Increasement();
        double t3 = System.nanoTime();
        // System.out.println("update model:" + (t3 - t2) / 1000000000);
        pw.println("update model:" + (t3 - t2) / 1000000000);
        pw.flush();

        model_realtime += (t3 - t2) / 1000000000;

        checker = new Checker(this, checker.reach, null);
        checker.PropertyCheck();
        pw.println("size:" + checker.reach.size());
        pw.flush();

        double t4 = System.nanoTime();
        check_realtime += (t4 - t3) / 1000000000;

        pw.println("property check:" + (t4 - t3) / 1000000000);
        pw.flush();
        pw.println("total:" + (t4 - t2) / 1000000000);
        pw.flush();
    }

    public void constructPPMFromFile(String postid) throws Exception {
        String topoFile = parsedRoot + postid + "/topology";
        String fibFile = parsedRoot + postid + "/fibs";
        String vxlanFile = parsedRoot + postid + "/vxlan";
        String epgFile = parsedRoot + postid + "/epg";
        String gbpFile = parsedRoot + postid + "/gbp";
        String pbrFile = parsedRoot + postid + "/pbr";

        processTopoFile(topoFile);
        processFibFile(fibFile);
        processVxlanFile(vxlanFile);
        processEpgFile(epgFile);
        processGbpFile(gbpFile);
        processPBRFile(pbrFile);
    }

    public void updatePPMFromFile(String pre_state, String cur_state) throws Exception {
        String updateId = cur_state + "_" + pre_state;
        String fibFile = updateRoot + updateId + "/fibs";
        String vxlanFile = updateRoot + updateId + "/vxlan";
        String epgFile = updateRoot + updateId + "/epg";
        String gbpFile = updateRoot + updateId + "/gbp";
        String pbrFile = updateRoot + updateId + "/pbr";
        // build overlay elements and topology
        processFibFile(fibFile);
        // parse vni classifier rules
        // parse vni and outter ip encap/decap rules
        processVxlanFile(vxlanFile);
        // parse epg id and its ip prefix
        processEpgFile(epgFile);
        // parse gbp rules
        processGbpFile(gbpFile);
        // parse pbr rules
        processPBRFile(pbrFile);
    }

    public void processTopoFile(String file) throws IOException {
        ArrayList<String> rules = ExperimentTools.readFromFile(file);
        for (String rule : rules) {
            String[] tokens = rule.split(" ");
            String e1 = tokens[0];
            String p1 = tokens[1];
            String e2 = tokens[2];
            String p2 = tokens[3];
            // add underlay element
            addForwardNode(e1, outter_ip_field); // 5
            addForwardNode(e2, outter_ip_field);

            AddOneWayLink(e1, p1, e2, p2);

            // add outter ip encapsulator and decapsualtor

            addNATNode(e1 + "_encapip", multi_filed, outter_ip_field); // 6
            addNATNode(e2 + "_encapip", multi_filed, outter_ip_field);
            addNATNode(e1 + "_decapip", no_field, outter_ip_field); // 7
            addNATNode(e2 + "_decapip", no_field, outter_ip_field);
            // 每一个device都有一个封装和解封装，是针对ip进行的
            // 进入设备需要封装，从设备的vxlan端口出来需要解封装。
            // 这里是underlay的，进入设备肯定是从overlay封装一下，然后转发，最后到了目的地，就通过vxlan解封装去到了另外一个overlay

            AddOneWayLink(e1 + "_encapip", "default", e1, "inport");
            AddOneWayLink(e2 + "_encapip", "default", e2, "inport");
            AddOneWayLink(e1, "vxlan", e1 + "_decapip", "inport");
            AddOneWayLink(e2, "vxlan", e2 + "_decapip", "inport");

            // add vni classifier
            // 添加vni的内容，包括了转发element，
            addForwardNode(e1 + "_vc", vni_field);
            addForwardNode(e2 + "_vc", vni_field);
            // 从设备的解封装进入了vni的层，如果说有vni，更加细腻的话，就是说明要进一步的划分，因此到了这样一个转发element，负责去不同的vni
            AddOneWayLink(e1 + "_decapip", "default", e1 + "_vc", "inport");
            AddOneWayLink(e2 + "_decapip", "default", e2 + "_vc", "inport");
        }
    }

    public void processFibFile(String file) throws IOException {
        fwd_rules.clear();
        ArrayList<String> rules = ExperimentTools.readFromFile(file);
        for (String rule : rules) {
            String[] tokens = rule.split(" ");
            String vpn = tokens[2];
            String intf = tokens[5];
            // + fwd (Core-0-0_vpn8) 167796480 24 (vxlan) 24
            // + fwd (Leaf-0-5) 335609892 30 (10ge1/0/1) 30

            // 终结点有两种，第一种是vrf节点，另外一种就是bd节点，这两个的区别？？？？
            addForwardNode(vpn, dst_ip_field);
            addEndHost(vpn, intf);
            addFWDRule(rule);

            if (vpn.contains("_vpn")) {
                // one overlay element
                // 从overlay发送，每一个vpn相当于一个小的转发表
                String device = vpn.split("_")[0];
                String vpnid = vpn.split("_")[1];
                // add vni encapsulator and decapsulator
                // 我这个vrf，要有解封装vni和封装vni的
                addNATNode(vpn + "_encapvni", dst_ip_field, vni_field);
                addNATNode(vpn + "_decapvni", no_field, vni_field);

                // add epg ip encapsulator
                // 以及添加src，dst和去掉 epg
                addNATNode(vpn + "_encapsrcepg", src_ip_field, src_epg_field);
                addNATNode(vpn + "_encapdstepg", dst_ip_field, dst_epg_field);
                addNATNode(vpn + "_decapepg", no_field, multi_filed);
                AddOneWayLink(device + "_vc", vpnid, vpn + "_decapvni", "inport");
                AddOneWayLink(vpn + "_decapvni", "default", vpn + "_encapdstepg", "inport");
                AddOneWayLink(vpn + "_encapdstepg", "default", vpn + "_decapepg", "inport");
                AddOneWayLink(vpn + "_decapepg", "default", vpn, "inport");

                AddOneWayLink(vpn, "vxlan", vpn + "_encapsrcepg", "inport");
                AddOneWayLink(vpn + "_encapsrcepg", "default", vpn + "_encapvni", "inport");

                AddOneWayLink(vpn + "_encapvni", "default", device + "_encapip", "inport");
                // + fwd Leaf-0-16_vpn8 167813889 32 vbdif3 32
                if (intf.contains("vpn")) {
                    // cross-vpn rule
                    AddOneWayLink(vpn, intf, device + "_" + intf, "inport");
                }
            } else if (intf.equals("vxlan")) {
                // an underlay element with nve ip
                String nve = tokens[3];
                if (!nve_table.containsKey(nve))
                    nve_table.put(nve, new HashSet<>());
                nve_table.get(nve).add(vpn);
            }
        }
    }

    public void processVxlanFile(String file) throws IOException {
        vc_rules.clear();
        ip_encap_rules.clear();
        ArrayList<String> rules = ExperimentTools.readFromFile(file);
        for (String rule : rules) {
            String[] tokens = rule.split(" ");
            String type = tokens[1];
            // + vxlan Core-0-1_vpn12 167789057 32 318899204 32 32
            // + vni Leaf-0-3_vpn12 50012
            // rewrite
            // vxlan是一个封装，是要确定封装后的传输ip是啥
            // vni是一个分组，有些时候ip不能起到业务分流的作用
            if (type.equals("vni")) {
                // batch update vni classifier
                // batch update vni encapsulator and decapsulator
                addVNIRule(rule);
            } else if (type.equals("vxlan")) {
                // batch update ip encapsulator and decapsulator
                addVxLANRule(rule);
            }
        }
    }

    public void processEpgFile(String file) throws Exception {
        epg_table.clear();
        ArrayList<String> rules = ExperimentTools.readFromFile(file);
        for (String rule : rules) {
            // + epg Leaf-0-14_vpn18 167808513 24 40141
            // + epg Leaf-0-11_vpn18 167802369 24 40117
            // + epg Leaf-0-16_vpn17 167814657 24 40165
            // + epg Leaf-0-17_vpn17 167814657 24 40165
            // 在这个vrf节点上，这个前缀的，会被划分为这个id，然后这里有一堆vpn节点对应的封装和解封装的内容
            String[] tokens = rule.split(" ");
            String op = tokens[0];
            String vpn = tokens[2];
            String ipaddrLong = tokens[3];
            String prefixLen = tokens[4];
            String epg_id = tokens[5];
            // vpn <epgid,ip>
            if (!epg_table.containsKey(vpn))
                epg_table.put(vpn, new Hashtable<>());
            if (!epg_table.get(vpn).containsKey(epg_id))
                epg_table.get(vpn).put(epg_id, new HashSet<>());
            epg_table.get(vpn).get(epg_id).add(new Pair<>(op, ipaddrLong + "/" + prefixLen));

            // vpn是forward
            addNATNode(vpn + "_encapsrcepg", src_ip_field, src_epg_field);
            addNATNode(vpn + "_encapdstepg", dst_ip_field, dst_epg_field);
            addNATNode(vpn + "_decapepg", no_field, multi_filed);

            insertOneWayLink(vpn, "vxlan", vpn + "_encapsrcepg", "inport", "default");
            insertOneWayLink(vpn + "_decapvni", "default", vpn + "_encapdstepg", "inport", "default");
            insertOneWayLink(vpn + "_encapdstepg", "default", vpn + "_decapepg", "inport", "default");
        }
    }

    public void processGbpFile(String file) throws Exception {
        // + gbp Leaf-0-14_tp_segment 40033 40101 permit 65535
        // + policy Leaf-0-7_tp_segment vpn3 in
        // + gbp Leaf-0-18_tp_segment 40149 40193 permit 65535
        // + policy Leaf-0-17_tp_segment vpn5 in
        // + gbp Leaf-0-8_tp_segment 40124 40146 permit 65535

        // + gbp Leaf-0-6_tp_segment 40035 40079 permit 65535
        // + gbp Leaf-0-10_tp_segment 40136 40092 permit 65535
        // + gbp Leaf-0-19_tp_segment 40133 40067 permit 65535
        // + gbp Leaf-0-17_tp_segment 40180 40046 permit 65535
        // + gbp Leaf-0-9_tp_segment 40088 40022 permit 65535
        // + gbp Leaf-0-1_tp_segment 40154 40044 permit 65535
        // + gbp Leaf-0-15_tp_segment 40033 40101 permit 65535
        // + gbp Leaf-0-17_tp_segment 40101 40033 permit 65535
        // 这里都是acl规则，规定了src和dst分别在哪个组的内容会被怎么执行
        ArrayList<String> rules = ExperimentTools.readFromFile(file);
        for (String rule : rules) {
            String[] tokens = rule.split(" ");
            String type = tokens[1];
            if (type.equals("policy")) {
                // + policy Leaf-0-0_tp_segment vpn1 in
                String policy_name = tokens[2];
                String device_name = policy_name.split("_")[0];
                String vpn = tokens[3];
                String vpn_name = device_name + "_" + vpn;
                addACLNode(policy_name);

                insertOneWayLink(vpn_name + "_encapdstepg", "default", policy_name, "inport", "permit");
                // if (vpn_name.equals("leaf-0-0_vpn4")){
                // System.out.println(topology.get(new
                // PositionTuple(vpn_name+"_encapdstepg","default")));
                // }
            } else if (type.equals("gbp")) {
                // 一条acl规则，指定了src，ds满足什么条件会怎么样
                addGBPRule(rule);
            }
        }
    }

    public void processPBRFile(String file) throws Exception {
        acl_rules.clear();
        ArrayList<String> rules = ExperimentTools.readFromFile(file);
        for (String rule : rules) {
            String[] tokens = rule.split(" ");
            String type = tokens[1];
            String policy_name = tokens[2];
            if (type.equals("policy")) {
                addACLNode(policy_name);
            } else if (type.equals("pbr")) {
                ACLRule r = new ACLRule(rule.substring(tokens[0].length() + tokens[1].length() + tokens[2].length() + 3,
                        rule.length() - tokens[tokens.length - 1].length() - 1), "redirect");
                addACLRule(policy_name, r);
            }
        }
    }

    void addFWDRule(String linestr) {
        // TODO Auto-generated method stub
        String token = " ";
        if (name.equals("campus"))
            token = ",";
        String[] tokens = linestr.split(token);

        String op = tokens[0];
        String element_name = tokens[2];
        if (!FieldNodes.containsKey(element_name))
            return;
        String ipInt = tokens[3];
        String prio = tokens[4];
        String outport = tokens[5];

        String ip = ipInt + "/" + prio;
        // ip和前缀
        HashMap<String, HashSet<Pair<String, String>>> rules = fwd_rules.get(ip);
        if (rules == null) {
            rules = new HashMap<String, HashSet<Pair<String, String>>>();
        }
        HashSet<Pair<String, String>> actions = rules.get(element_name);
        if (actions == null) {
            actions = new HashSet<Pair<String, String>>();
        }
        Pair<String, String> pair = new Pair<String, String>(op, outport);
        // 加或者减，然后是output
        actions.add(pair);
        rules.put(element_name, actions);
        fwd_rules.put(ip, rules);
    }

    void addVNIRule(String rule) {
        String[] tokens = rule.split(" ");
        String op = tokens[0];
        String vpn = tokens[2];
        String vni = tokens[3];
        String device = vpn.split("_")[0] + "_vc";
        String vpnid = vpn.split("_")[1];
        // + vni Leaf-0-16_vpn4 50004
        // vc设备的一个端口号
        // 规则所在的设备根据设备的前缀，转发端口是vpnid
        if (!vc_rules.containsKey(vni))
            vc_rules.put(vni, new HashMap<>());
        if (!vc_rules.get(vni).containsKey(device))
            vc_rules.get(vni).put(device, new HashSet<>());
        HashSet<Pair<String, String>> rules = vc_rules.get(vni).get(device);
        rules.add(new Pair<String, String>(op, vpnid));
        // vni device vpnid

        vni_table.put(vpn, vni);
    }

    void addVxLANRule(String rule) {
        String[] tokens = rule.split(" ");
        String op = tokens[0];
        String vpn = tokens[2];// Leaf-0-13_vpn18
        String inner_ip = tokens[3] + "/" + tokens[4] + "/" + vpn.substring(vpn.indexOf("vpn") + 3);// 167821568 24
        String outter_ip = tokens[5];// 318767106

        // 封装的ip
        // + vxlan Leaf-0-13_vpn18 167821568 24 318767106 32 24
        if (!ip_encap_rules.containsKey(vpn))
            ip_encap_rules.put(vpn, new Hashtable<>());
        if (!ip_encap_rules.get(vpn).containsKey(inner_ip))
            ip_encap_rules.get(vpn).put(inner_ip, new HashSet<>());
        ip_encap_rules.get(vpn).get(inner_ip).add(new Pair<>(op, outter_ip));
    }

    void addGBPRule(String rule) {
        String[] tokens = rule.split(" ");
        String op = tokens[0];
        String policy = tokens[2];
        String srcEPG = tokens[3];
        String dstEPG = tokens[4];
        String port = tokens[5];
        // + gbp Leaf-0-8_tp_segment 40124 40146 permit 65535
        String key = srcEPG + "/" + dstEPG;
        Pair<String, String> action = new Pair<>(op, port);
        // srcepg+dstepg aclname + action
        if (!gbp_rules.containsKey(key))
            gbp_rules.put(key, new HashMap<>());
        if (!gbp_rules.get(key).containsKey(policy))
            gbp_rules.get(key).put(policy, new HashSet<>());
        gbp_rules.get(key).get(policy).add(action);
    }

    public void addACLRule(String element, ACLRule rule) {
        if (!acl_rules.containsKey(element))
            acl_rules.put(element, new HashSet<>());
        acl_rules.get(element).add(rule);
    }

    void addForwardNode(String eName, int field) {
        if (!FieldNodes.containsKey(eName)) {
            if (eName.contains("vpn")) {
                fe++;
                vpne++;
            } else {
                fe++;
            }

            FieldNode e = new FieldNode(eName, this, 0, false, field, no_field);
            FieldNodes.put(e.name, e);
        }
    }

    void addACLNode(String eName) {
        if (!FieldNodes.containsKey(eName)) {
            ae++;
            FieldNode e = new FieldNode(eName, this, 1, false, multi_filed, no_field);
            FieldNodes.put(e.name, e);
        }
    }

    void addNATNode(String eName, int field, int actionField) {
        if (!FieldNodes.containsKey(eName)) {
            if (eName.contains("epg")) {
                epge++;
            } else if (eName.contains("vni")) {
                // vni这个不能修改
                vnie++;
            } else if (eName.contains("ip")) {
                ipe++;
            }
            ne++;
            if (false & eName.contains("decap")) {
                FieldNode e = new FieldNode(eName, this, 2, true, field, actionField);
                FieldNodes.put(e.name, e);
            } else {
                FieldNode e = new FieldNode(eName, this, 2, false, field, actionField);
                FieldNodes.put(e.name, e);
            }
        }
    }

    void addEndHost(String device, String port) {
        if (device.contains("_vpn")) {
            startEndPoint.add(device);
        }
        if (port.startsWith("vbdif")) {
            // + fwd Leaf-0-9_vpn20 167795712 24 vbdif12 24
            // device = Leaf-0-9_vpn20
            // port = vbdif12
            String bdid = "bd" + port.substring(5);// 12
            // 设备+bdid，比如leaf-0-9_12
            String host = device.split("_")[0] + "_" + bdid;
            endEndPoint.add(host);
            AddOneWayLink(device, port, host, "inport");
        }
    }

    public void AddOneWayLink(String d1, String p1, String d2, String p2) {
        // System.out.println(d1 + " " + p1 + " -> " + d2 + " " + p2);
        PositionTuple pt1 = new PositionTuple(d1, p1);
        PositionTuple pt2 = new PositionTuple(d2, p2);
        // links are one way
        if (topology.containsKey(pt1)) {
            topology.get(pt1).add(pt2);
        } else {
            HashSet<PositionTuple> newset = new HashSet<PositionTuple>();
            newset.add(pt2);
            topology.put(pt1, newset);
        }
    }

    public void insertOneWayLink(String startDevice, String startPort, String insertDevice, String insertInputPort,
            String insertOutputPort) throws Exception {
        PositionTuple insertOutTuple = new PositionTuple(insertDevice, insertOutputPort);
        if (topology.containsKey(insertOutTuple))
            return;
        HashSet<PositionTuple> targetSet = new HashSet<PositionTuple>();

        PositionTuple startTuple = new PositionTuple(startDevice, startPort);
        PositionTuple insertInTuple = new PositionTuple(insertDevice, insertInputPort);

        if (!topology.containsKey(startTuple)) {
            return;
        }

        for (PositionTuple pt : topology.get(startTuple)) {
            targetSet.add(pt);
        }
        topology.get(startTuple).clear();
        topology.get(startTuple).add(insertInTuple);
        topology.put(insertOutTuple, new HashSet<>());
        topology.get(insertOutTuple).addAll(targetSet);
    }

    public HashSet<PositionTuple> LinkTransfer(PositionTuple pt) {
        // if (pt.getPortName().toLowerCase().startsWith("vlan")) {
        // HashSet<PositionTuple> next_hops = new HashSet<PositionTuple>();
        // String element_name = pt.getDeviceName();
        // FieldNode e = FieldNodes.get(element_name);
        // HashSet<String> ports = e.VlanToPhy(pt.getPortName());
        // if(ports == null) return null;
        // for (String port: ports) {
        // PositionTuple pt_temp = new PositionTuple(element_name, port);
        // if (topology.containsKey(pt_temp)) {
        // next_hops.addAll(topology.get(pt_temp));
        // }
        // }
        // return next_hops;
        // }
        FieldNode e = FieldNodes.get(pt.getDeviceName());
        if (e.type == 2) {
            return topology.get(new PositionTuple(e.name, "default"));
        } else {
            return topology.get(pt);
        }
    }

    public static int FW_num = 0;
    public static int VXlan_num = 0;
    public static int VC_num = 0;
    public static int VNI_num = 0;
    public static int EPG_num = 0;
    public static int GBP_num = 0;
    public static int PBR_num = 0;

    public static double FW_time = 0;
    public static double VXlan_time = 0;
    public static double VC_time = 0;
    public static double VNI_time = 0;
    public static double EPG_time = 0;
    public static double GBP_time = 0;
    public static double PBR_time = 0;

    public HashMap<String, HashSet<Integer>> updatePPMSnapshot_Increasement() throws IOException {
        HashMap<String, HashSet<Integer>> moved_aps = new HashMap<>();

        double t1 = System.nanoTime();
        updateFWRuleBatch_Increasement(fwd_rules, moved_aps);

        double t2 = System.nanoTime();
        pw.println("FW:" + (t2 - t1) / 1000000000);
        pw.flush();
        FW_time += (t2 - t1) / 1000000000;
        updateVxLANEncapDecap_Increasement_Batch(moved_aps);

        double t3 = System.nanoTime();
        pw.println("VxLAN:" + (t3 - t2) / 1000000000);
        pw.flush();
        VXlan_time += (t3 - t2) / 1000000000;

        updateVCRuleBatch_Increasement(moved_aps);

        double t4 = System.nanoTime();
        pw.println("VC:" + (t4 - t3) / 1000000000);
        pw.flush();
        VC_time += (t4 - t3) / 1000000000;
        updateVNIEncapDecap_Increasement(moved_aps);

        double t5 = System.nanoTime();
        pw.println("VNI:" + (t5 - t4) / 1000000000);
        pw.flush();
        VNI_time += (t5 - t4) / 1000000000;
        updateEPGEncapDecap_Increasement(moved_aps);

        double t6 = System.nanoTime();
        pw.println("EPG:" + (t6 - t5) / 1000000000);
        pw.flush();
        EPG_time += (t6 - t5) / 1000000000;
        updateGBPRuleBatch_Increasement(moved_aps);

        double t7 = System.nanoTime();
        pw.println("GBP:" + (t7 - t6) / 1000000000);
        pw.flush();
        GBP_time += (t7 - t6) / 1000000000;
        updatePBRRuleBatch_Increasement(moved_aps);
        double t8 = System.nanoTime();
        pw.println("PBR:" + (t8 - t7) / 1000000000);
        pw.flush();
        PBR_time += (t8 - t7) / 1000000000;

        return moved_aps;
    }

    public void updateFWRuleBatch_Increasement(HashMap<String, HashMap<String, HashSet<Pair<String, String>>>> fw_rules,
            HashMap<String, HashSet<Integer>> moved_fwd_aps) {
        // TODO Auto-generated method stub
        ArrayList<String> ips = new ArrayList<String>();
        HashSet<String> update_points = new HashSet<String>();

        for (String ip : fw_rules.keySet()) {
            ips.add(ip);
        }
        Collections.sort(ips, new sortRulesByPriority());

        // first step - get the change_set & copy_set & remove_set
        HashMap<String, ArrayList<ChangeTuple>> change_set = new HashMap<String, ArrayList<ChangeTuple>>();
        HashMap<String, ArrayList<ChangeTuple>> remove_set = new HashMap<String, ArrayList<ChangeTuple>>();
        HashMap<String, ArrayList<ChangeTuple>> copyto_set = new HashMap<String, ArrayList<ChangeTuple>>();

        for (String ip : ips) {
            for (String element_name : fw_rules.get(ip).keySet()) {
                HashSet<Pair<String, String>> actions = fw_rules.get(ip).get(element_name);
                HashSet<String> to_ports = new HashSet<String>();
                HashSet<String> from_ports = new HashSet<String>();

                for (Pair<String, String> pair : actions) {
                    FW_num++;
                    if (pair.getKey().equals("+"))
                        to_ports.add(pair.getValue());
                    else if (pair.getKey().equals("-"))
                        from_ports.add(pair.getValue());
                }
                // batch move atom
                HashSet<String> retained = new HashSet<String>(to_ports);
                retained.retainAll(from_ports);
                if (!retained.isEmpty()) {
                    to_ports.removeAll(retained);
                    from_ports.removeAll(retained);
                }

                FieldNode e = FieldNodes.get(element_name);
                e.updateFWRuleBatch(ip, to_ports, from_ports, change_set, copyto_set, remove_set);
                update_points.add(element_name);
            }
        }

        for (String device : remove_set.keySet()) {
            if (remove_set.get(device).size() != 0) {
                System.out.println("remove not implemented");
            }
        }
        for (String element_name : update_points) {
            FieldNode e = FieldNodes.get(element_name);
            e.update_FW(change_set.get(element_name), copyto_set.get(element_name));
        }
    }

    Long time1 = 0L;
    Long time2 = 0L;
    Long time3 = 0L;
    Long time4 = 0L;
    Long time5 = 0L;
    Long time6 = 0L;
    Long time7 = 0L;

    public void updateVxLANEncapDecap_Increasement(HashMap<String, HashSet<Integer>> moved_fwd_aps) {
        for (String vpn : ip_encap_rules.keySet()) {
            Long t0 = System.nanoTime();
            FieldNode encap_element = FieldNodes.get(vpn.split("_")[0] + "_encapip");
            ArrayList<String> iporder = new ArrayList<>();
            for (String origin_ip : ip_encap_rules.get(vpn).keySet()) {
                if (iporder.isEmpty())
                    iporder.add(origin_ip);
                else {
                    boolean isInser = false;
                    for (int i = 0; i < iporder.size(); i++) {
                        if (Integer.valueOf(origin_ip.split("/")[1]) < Integer.valueOf(iporder.get(i).split("/")[1])) {
                            iporder.add(i, origin_ip);
                            isInser = true;
                            break;
                        }
                    }
                    if (!isInser) {
                        iporder.add(origin_ip);
                    }
                }
            }

            Long t1 = System.nanoTime();
            time1 = time1 + (t1 - t0);

            for (String origin_ip : iporder) {
                for (Pair<String, String> pair : ip_encap_rules.get(vpn).get(origin_ip)) {

                    Long t3 = System.nanoTime();

                    VXlan_num++;
                    String op = pair.getKey();
                    String outter_ip = pair.getValue();

                    long ipaddr = Long.parseLong(origin_ip.split("/")[0]);
                    int prefixlen = Integer.parseInt(origin_ip.split("/")[1]);

                    int field_bdd = bdd_engine.get_field_bdd("outterIP");

                    int dstIP = bdd_engine.encodeDstIPPrefix(ipaddr, prefixlen);

                    int vni = Integer.parseInt(origin_ip.split("/")[2]);
                    vni = 50000 + vni;
                    int vnibdd = bdd_engine.encodeVNI(String.valueOf(vni));
                    dstIP = bdd_engine.and(dstIP, vnibdd);

                    Long t4 = System.nanoTime();
                    time2 = time2 + (t4 - t3);

                    int newHeader = bdd_engine.encodeDstIPPrefix_outter(Long.parseLong(outter_ip), 32);
                    EncapsulationIPRule encap_r = new EncapsulationIPRule(dstIP, field_bdd, newHeader, outter_ip,
                            prefixlen, bdd_engine);

                    Long t5 = System.nanoTime();
                    time3 = time3 + (t5 - t4);

                    ArrayList changelist = encap_element.InsertRewriteRule(encap_r);

                    Long t6 = System.nanoTime();
                    time4 = time4 + (t6 - t5);

                    encap_element.update_ACL(changelist); // time

                    Long t7 = System.nanoTime();
                    time5 = time5 + (t7 - t6);
                }
            }
        }
        System.out.println("time1:" + (time1 / 1000000000.0));
        System.out.println("time2:" + (time2 / 1000000000.0));
        System.out.println("time3:" + (time3 / 1000000000.0));
        System.out.println("time4:" + (time4 / 1000000000.0));
        System.out.println("time5:" + (time5 / 1000000000.0));
        // System.out.println("insert time1:"+(FieldNode.insertVxLANT1/1000000000.0));
        // System.out.println("insert time2:"+(FieldNode.insertVxLANT2/1000000000.0));
        // System.out.println("insert time3:"+(FieldNode.insertVxLANT3/1000000000.0));
        // System.out.println("insert time4:"+(FieldNode.insertVxLANT4/1000000000.0));
        // System.out.println("ACL time1:"+(FieldNode.ACLTime1/1000000000.0));
        // System.out.println("ACL time2:"+(FieldNode.ACLTime2/1000000000.0));
        // System.out.println("ACL time3:"+(FieldNode.ACLTime3/1000000000.0));
        // System.out.println("not time in diff:"+(BDDACLWrapper.notTime/1000000000.0));
        // System.out.println("and time in diff:"+(BDDACLWrapper.andTime/1000000000.0));
    }

    public void updateVxLANEncapDecap_Increasement_Batch(HashMap<String, HashSet<Integer>> moved_fwd_aps) {
        HashMap<String, HashMap<String, HashSet<Integer>>> toRemove = new HashMap<>(); // device from_port delta
        HashMap<String, HashMap<String, HashSet<Integer>>> toAdd = new HashMap<>(); // device to_port delta

        for (String vpn : ip_encap_rules.keySet()) {
            // Long t0 = System.nanoTime();
            FieldNode encap_element = FieldNodes.get(vpn.split("_")[0] + "_encapip");

            if (!toRemove.containsKey(encap_element.name))
                toRemove.put(encap_element.name, new HashMap());
            if (!toAdd.containsKey(encap_element.name))
                toAdd.put(encap_element.name, new HashMap());
            HashMap<String, HashSet<Integer>> subToRemove = toRemove.get(encap_element.name);
            HashMap<String, HashSet<Integer>> subToAdd = toAdd.get(encap_element.name);

            ArrayList<String> iporder = new ArrayList<>();
            for (String origin_ip : ip_encap_rules.get(vpn).keySet()) {
                if (iporder.isEmpty())
                    iporder.add(origin_ip);
                else {
                    boolean isInser = false;
                    for (int i = 0; i < iporder.size(); i++) {
                        if (Integer.valueOf(origin_ip.split("/")[1]) < Integer.valueOf(iporder.get(i).split("/")[1])) {
                            iporder.add(i, origin_ip);
                            isInser = true;
                            break;
                        }
                    }
                    if (!isInser) {
                        iporder.add(origin_ip);
                    }
                }
            }

            // Long t1 = System.nanoTime();
            // time1 = time1+(t1-t0);

            for (String origin_ip : iporder) {
                for (Pair<String, String> pair : ip_encap_rules.get(vpn).get(origin_ip)) {

                    // Long t3 = System.nanoTime();

                    VXlan_num++;
                    String op = pair.getKey();
                    String outter_ip = pair.getValue();

                    long ipaddr = Long.parseLong(origin_ip.split("/")[0]);
                    int prefixlen = Integer.parseInt(origin_ip.split("/")[1]);

                    int field_bdd = bdd_engine.get_field_bdd("outterIP");

                    int dstIP = bdd_engine.encodeDstIPPrefix(ipaddr, prefixlen);

                    int vni = Integer.parseInt(origin_ip.split("/")[2]);
                    vni = 50000 + vni;
                    int vnibdd = bdd_engine.encodeVNI(String.valueOf(vni));
                    dstIP = bdd_engine.and(dstIP, vnibdd);

                    // Long t4 = System.nanoTime();
                    // time2 = time2+(t4-t3);

                    int newHeader = bdd_engine.encodeDstIPPrefix_outter(Long.parseLong(outter_ip), 32);
                    EncapsulationIPRule encap_r = new EncapsulationIPRule(dstIP, field_bdd, newHeader, outter_ip,
                            prefixlen, bdd_engine);

                    // Long t5 = System.nanoTime();
                    // time3 = time3+(t5-t4);

                    ArrayList<ChangeItem> changelist = encap_element.InsertRewriteRule(encap_r);

                    // Long t6 = System.nanoTime();
                    // time4 = time4+(t6-t5);

                    for (ChangeItem item : changelist) {
                        if (!subToRemove.containsKey(item.from_port)) {
                            HashSet<Integer> set = new HashSet<>();
                            set.add(item.delta);
                            // for(int d : item.delta.edges.values())
                            // {
                            // set.add(d);
                            // }
                            subToRemove.put(item.from_port, set);
                        } else {
                            HashSet<Integer> set = subToRemove.get(item.from_port);
                            set.add(item.delta);
                        }
                        if (!subToAdd.containsKey(item.to_port)) {
                            HashSet<Integer> set = new HashSet<>();
                            set.add(item.delta);
                            subToAdd.put(item.to_port, set);
                        } else {
                            HashSet<Integer> set = subToAdd.get(item.to_port);
                            set.add(item.delta);
                        }
                    }

                    // Long t7 = System.nanoTime();
                    // time5 = time5+(t7-t6);
                }
            }
        }

        // Long t8 = System.nanoTime();

        HashMap<String, HashMap<String, Integer>> mergedRemove = mergeChange(toRemove);
        HashMap<String, HashMap<String, Integer>> mergedAdd = mergeChange(toAdd);

        // Long t9 = System.nanoTime();
        // time6 = time6+(t9-t8);

        for (Entry<String, HashMap<String, Integer>> entry : mergedRemove.entrySet()) {
            FieldNode device = FieldNodes.get(entry.getKey());
            device.update_ACL(entry.getValue(), mergedAdd.get(entry.getKey()));
        }

        // Long t10 = System.nanoTime();
        // time7 = time7+(t10-t9);

        // System.out.println("time1:"+(time1/1000000000.0));
        // System.out.println("time2:"+(time2/1000000000.0));
        // System.out.println("time3:"+(time3/1000000000.0));
        // System.out.println("time4:"+(time4/1000000000.0));
        // System.out.println("time5:"+(time5/1000000000.0));
        // System.out.println("time6:"+(time6/1000000000.0));
        // System.out.println("time7:"+(time7/1000000000.0));
        // System.out.println("insert time1:"+(FieldNode.insertVxLANT1/1000000000.0));
        // System.out.println("insert time2:"+(FieldNode.insertVxLANT2/1000000000.0));
        // System.out.println("insert time3:"+(FieldNode.insertVxLANT3/1000000000.0));
        // System.out.println("insert time4:"+(FieldNode.insertVxLANT4/1000000000.0));
        // System.out.println("insert time5:"+(FieldNode.insertVxLANT5/1000000000.0));
        // System.out.println("insert time6:"+(FieldNode.insertVxLANT6/1000000000.0));
        // System.out.println("ACL time1:"+(FieldNode.ACLTime1/1000000000.0));
        // System.out.println("ACL time2:"+(FieldNode.ACLTime2/1000000000.0));
        // System.out.println("ACL time3:"+(FieldNode.ACLTime3/1000000000.0));
        // System.out.println("not time in diff:"+(BDDACLWrapper.notTime/1000000000.0));
        // System.out.println("and time in diff:"+(BDDACLWrapper.andTime/1000000000.0));
    }

    public HashMap<String, HashMap<String, Integer>> mergeChange(
            HashMap<String, HashMap<String, HashSet<Integer>>> changes) {
        HashMap<String, HashMap<String, Integer>> ret = new HashMap<>();
        HashMap<HashSet<Integer>, Integer> cache = new HashMap<>();
        BDD bdd = bdd_engine.getBDD();
        for (Entry<String, HashMap<String, HashSet<Integer>>> map : changes.entrySet()) {
            String device = map.getKey();
            HashMap<String, Integer> subRet = new HashMap<>();
            ret.put(device, subRet);
            for (Entry<String, HashSet<Integer>> subMap : map.getValue().entrySet()) {
                String port = subMap.getKey();
                int sum = 0;
                if (cache.containsKey(subMap.getValue())) {
                    sum = bdd.ref(cache.get(subMap.getValue()));
                } else {
                    for (int delta : subMap.getValue()) {
                        int temp = bdd.ref(bdd.or(sum, delta));
                        bdd.deref(sum);
                        sum = temp;
                    }
                    cache.put(subMap.getValue(), sum);
                }
                subRet.put(port, sum);
            }
        }
        return ret;
    }

    public void updateVCRuleBatch_Increasement(HashMap<String, HashSet<Integer>> moved_aps) {
        // TODO Auto-generated method stub
        // vni device <op,vpn4>
        for (String vni_id : vc_rules.keySet()) {
            int vniBDD = bdd_engine.encodeVNI(vni_id);
            for (String element : vc_rules.get(vni_id).keySet()) {
                FieldNode e = FieldNodes.get(element);
                ArrayList<ChangeItem> changeList = new ArrayList<>();
                for (Pair<String, String> pair : vc_rules.get(vni_id).get(element)) {
                    VC_num++;
                    String port = pair.getValue();
                    // port vpn4，
                    bdd_engine.ref(vniBDD);
                    if (pair.getKey().equals("+")) {
                        e.ports.add(port);
                        e.ports_pred.put(port, NDD.NDDFalse);
                        ChangeItem ci = new ChangeItem("default", port, vniBDD);
                        changeList.add(ci);
                    } else {
                        ChangeItem ci = new ChangeItem(port, "default", vniBDD);
                        changeList.add(ci);
                    }
                }
                e.update_ACL(changeList);
            }
        }
    }

    public void updateVNIEncapDecap_Increasement(HashMap<String, HashSet<Integer>> moved_aps) {
        HashSet<ChangeItem> applyList = new HashSet<>();
        ArrayList<String> iporder = new ArrayList<>();
        for (String origin_ip : fwd_rules.keySet()) {
            if (iporder.isEmpty())
                iporder.add(origin_ip);
            else {
                boolean isInser = false;
                for (int i = 0; i < iporder.size(); i++) {
                    if (Integer.valueOf(origin_ip.split("/")[1]) < Integer.valueOf(iporder.get(i).split("/")[1])) {
                        iporder.add(i, origin_ip);
                        isInser = true;
                        break;
                    }
                }
                if (!isInser) {
                    iporder.add(origin_ip);
                }
            }
        }

        for (String ipPrefix : iporder) {
            Long ipaddr = Long.valueOf(ipPrefix.split("/")[0]);
            int prefixlen = Integer.valueOf(ipPrefix.split("/")[1]);
            int ipBDD = bdd_engine.encodeDstIPPrefix(ipaddr, prefixlen);

            // IP device <op,port>
            // 封装ip的要求是，首先得有个类似这样的转发规则
            // + fwd Leaf-0-7_vpn17 167814656 24 vxlan 24
            for (String vpn : fwd_rules.get(ipPrefix).keySet()) {
                if (!vpn.contains("_vpn"))
                    continue;
                if (!vni_table.containsKey(vpn))
                    continue;
                String vni_id = vni_table.get(vpn);
                int vniBDD = bdd_engine.encodeVNI(vni_id);
                int field_bdd = bdd_engine.get_field_bdd("vni");
                // 其次，这个转发规则本身还需要一定的有vni规则
                // 比如+ vni Leaf-0-7_vpn17 50017
                // 在这种情况下，把原来的ip封装成vni
                // encode vni
                FieldNode encap_element = FieldNodes.get(vpn + "_encapvni");
                for (Pair<String, String> pair : fwd_rules.get(ipPrefix).get(vpn)) {
                    VNI_num++;
                    String op = pair.getKey();
                    String port = pair.getValue();
                    if (!port.equals("vxlan"))
                        continue;
                    // 只有从vxlan出去的才需要，虽然是先封装ip
                    // System.out.println(op+" "+vpn+" "+ origin_ip+"->"+outter_ip);
                    // + vxlan Leaf-0-14_vpn17 167797248 24 318899461 32 24
                    EncapsulationVNIRule encap_r = new EncapsulationVNIRule(bdd_engine.ref(ipBDD), field_bdd,
                            bdd_engine.ref(vniBDD), vni_id, prefixlen, bdd_engine);

                    if (op.equals("+")) {
                        // System.out.println(encap_element.name);
                        ArrayList<ChangeItem> changeset = encap_element.InsertRewriteRule(encap_r);
                        encap_element.update_ACL(changeset);// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                        // check_empty();
                        // System.out.println();
                    } else if (op.equals("-")) {
                        System.out.println("not implemented yet");
                    }
                }
            }
        }
    }

    public void updateEPGEncapDecap_Increasement(HashMap<String, HashSet<Integer>> moved_aps) {
        for (String element : FieldNodes.keySet()) {
            if (!element.endsWith("epg"))
                continue;
            FieldNode epg_element = FieldNodes.get(element);
            String vpn = element.split("_")[0] + "_" + element.split("_")[1];
            if (element.endsWith("_encapsrcepg")) {
                if (!epg_table.containsKey(vpn))
                    continue;
                for (String epg_id : epg_table.get(vpn).keySet()) {
                    int srcEPGBDD = bdd_engine.encodeSrcEPG(epg_id);
                    for (Pair<String, String> pair : epg_table.get(vpn).get(epg_id)) {
                        EPG_num++;
                        String op = pair.getKey();
                        String ipPrefix = pair.getValue();
                        long ipaddr = Long.parseLong(ipPrefix.split("/")[0]);
                        int prefixLen = Integer.parseInt(ipPrefix.split("/")[1]);
                        int srcBDD = bdd_engine.encodeSrcIPPrefix(ipaddr, prefixLen);
                        int field_bdd = bdd_engine.get_field_bdd("srcepg");
                        bdd_engine.ref(srcEPGBDD);
                        EncapsulationEPGRule encap_r = new EncapsulationEPGRule(srcBDD, field_bdd, srcEPGBDD, epg_id,
                                65535, bdd_engine);

                        if (op.equals("+")) {
                            epg_element.update_ACL(epg_element.InsertRewriteRule(encap_r));
                        } else {
                            System.out.println("not implemented yet");
                        }
                    }
                }
            } else if (element.endsWith("_encapdstepg")) {
                if (!epg_table.containsKey(vpn))
                    continue;
                for (String epg_id : epg_table.get(vpn).keySet()) {
                    int dstEPGBDD = bdd_engine.encodeDstEPG(epg_id);
                    for (Pair<String, String> pair : epg_table.get(vpn).get(epg_id)) {
                        EPG_num++;
                        String op = pair.getKey();
                        String ipPrefix = pair.getValue();

                        long ipaddr = Long.valueOf(ipPrefix.split("/")[0]);
                        int prefixLen = Integer.valueOf(ipPrefix.split("/")[1]);

                        int dstBDD = bdd_engine.encodeDstIPPrefix(ipaddr, prefixLen);
                        int field_bdd = bdd_engine.get_field_bdd("dstepg");
                        bdd_engine.ref(dstEPGBDD);
                        EncapsulationEPGRule encap_r = new EncapsulationEPGRule(dstBDD, field_bdd, dstEPGBDD, epg_id,
                                65535, bdd_engine);

                        if (op.equals("+")) {
                            epg_element.update_ACL(epg_element.InsertRewriteRule(encap_r));
                        } else {
                            System.out.println("not implemented yet");
                        }
                    }
                }
            }
        }
    }

    public void updateGBPRuleBatch_Increasement(HashMap<String, HashSet<Integer>> moved_aps) {
        for (String key : gbp_rules.keySet()) {
            String srcEPG = key.split("/")[0];
            String dstEPG = key.split("/")[1];

            int srcEPGBDD = bdd_engine.encodeSrcEPG(srcEPG);
            int dstEPGBDD = bdd_engine.encodeDstEPG(dstEPG);
            int epgBDD = bdd_engine.and(srcEPGBDD, dstEPGBDD);

            for (String policy : gbp_rules.get(key).keySet()) {

                FieldNode policy_element = FieldNodes.get(policy);

                for (Pair<String, String> pair : gbp_rules.get(key).get(policy)) {
                    GBP_num++;
                    String op = pair.getKey();
                    String port = pair.getValue();

                    ACLRule rule = new ACLRule();
                    rule.setType(port);
                    rule.set_priority(65535);

                    bdd_engine.ref(epgBDD);
                    if (port.equals("permit")) {
                        rule.insert_vals(epgBDD, epgBDD, 0);
                    } else if (port.equals("deny")) {
                        rule.insert_vals(epgBDD, 0, epgBDD);
                    }

                    if (op.equals("+")) {
                        policy_element.update_ACL(policy_element.InsertACLRuleBDD(rule));
                    } else if (op.equals("-")) {
                        System.out.println("not implemented yet");
                    }
                }
            }
            bdd_engine.DerefInBatch(new int[] { srcEPGBDD, dstEPGBDD, epgBDD });
        }
    }

    public void updatePBRRuleBatch_Increasement(HashMap<String, HashSet<Integer>> moved_aps) {
        for (String element : acl_rules.keySet()) {
            for (ACLRule rule : acl_rules.get(element)) {
                PBR_num++;
                FieldNode e = FieldNodes.get(element);
                change_set = e.InsertACLRule(rule);
                e.update_ACL(change_set);
            }
        }
    }

    public static FileWriter fw;
    public static PrintWriter pw;

    public static void main(String[] args) throws Exception {
        // System.out.println(System.getProperty("java.version"));
        path = "Field-Decision-Network"
        fw = new FileWriter(path + "/DCN_NDD/output/NDD/no_cache/" + args[0] + ".txt",
                false);
        pw = new PrintWriter(fw);

        String mode = "normal";

        FDNNetwork n = new FDNNetwork(args[0], mode, "base", args[0]);

        // for(FieldNode d : n.FieldNodes.values())
        // {
        // if(d.name.contains("encap") || d.name.contains("decap"))continue;
        // System.out.println(d.name+" "+d.ports);
        // }

        // n.processDifferentialUpdate("base", "post0");
        // n.processDifferentialUpdate("post0", "post1");
        // n.processDifferentialUpdate("post1", "post2");
        // n.processDifferentialUpdate("post2", "post3");
        // n.processDifferentialUpdate("post3", "post4");

        // pw.println();
        // pw.flush();
        // pw.println("FW_update:" + FW_time / 5);
        // pw.flush();
        // pw.println("VXlan_update:" + VXlan_time / 5);
        // pw.flush();
        // pw.println("VC_update:" + VC_time / 5);
        // pw.flush();
        // pw.println("VNI_update:" + VNI_time / 5);
        // pw.flush();
        // pw.println("EPG_update:" + EPG_time / 5);
        // pw.flush();
        // pw.println("GBP_update:" + GBP_time / 5);
        // pw.flush();
        // pw.println("PBR_update:" + PBR_time / 5);
        // pw.flush();
        // pw.println("Model_update:" + model_realtime / 5);
        // pw.flush();
        // pw.println("Check_update:" + check_realtime / 5);
        // pw.flush();
        // pw.println("Total_update:" + (model_realtime + check_realtime) / 5);
        // pw.flush();
    }
}

class sortRulesByPriority implements Comparator<Object> {
    @Override
    public int compare(Object o1, Object o2) {
        String ip1 = (String) o1;
        String ip2 = (String) o2;
        int p1 = Integer.valueOf(ip1.split("/")[1]);
        int p2 = Integer.valueOf(ip2.split("/")[1]);
        return p1 - p2;
    }
}