package generateDataset;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import common.Utility;

public class generateNat {

    public void generate() throws IOException {
        // read forwarding rules
        String fw_path = "/data/zcli-data/pd/dpv/purdue/change_base";
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(fw_path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ArrayList<String> FW_rules = new ArrayList<String>();
        String OneLine;
        while ((OneLine = br.readLine()) != null) {
            String linestr = OneLine.trim();
            FW_rules.add(linestr);
        }

        // read edge ports
        String edge_path = "/data/zcli-data/pd/edgePorts";
        try {
            br = new BufferedReader(new FileReader(edge_path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        HashSet<String> devices = new HashSet<>();
        HashMap<String, HashSet<String>> edge_ports = new HashMap<>();
        while ((OneLine = br.readLine()) != null) {
            String linestr = OneLine.trim();
            String[] tokens = linestr.split(" ");
            devices.add(tokens[0]);
            HashSet<String> set = edge_ports.get(tokens[0]);
            if (set == null) {
                set = new HashSet<>();
            }
            set.add(tokens[1]);
            edge_ports.put(tokens[0], set);
        }

        // get other ports
        String used_path = "/data/zcli-data/pd/usedPorts.txt";
        try {
            br = new BufferedReader(new FileReader(used_path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        HashMap<String, HashSet<String>> other_ports = new HashMap<>();
        while ((OneLine = br.readLine()) != null) {
            String linestr = OneLine.trim();
            String[] tokens = linestr.split(" ");
            for (int curr = 1; curr < tokens.length; curr++) {
                if (tokens[curr].equalsIgnoreCase("default"))
                    continue;
                if (edge_ports.get(tokens[0]) != null && !edge_ports.get(tokens[0]).contains(tokens[curr]))
                    continue;
                HashSet<String> set = other_ports.get(tokens[0]);
                if (set == null) {
                    set = new HashSet<>();
                }
                set.add(tokens[curr]);
                other_ports.put(tokens[0], set);
            }
        }

        // get src ip and dst ip pool
        HashSet<String> SrcIPPool = new HashSet<>();
        HashMap<String, HashMap<String, HashSet<String>>> SrcIPMap = new HashMap<>();
        HashSet<String> DstIPPool = new HashSet<>();
        HashMap<String, HashMap<String, HashSet<String>>> DstIPMap = new HashMap<>();
        for (String line : FW_rules) {
            String[] tokens = line.split(" ");
            String device = tokens[2];
            String ip = tokens[3];
            String len = tokens[4];
            String prefix = ip + "/" + len;
            String port = tokens[5];
            if (edge_ports.get(device).contains(port)) {
                SrcIPPool.add(prefix);
                HashMap<String, HashSet<String>> map = SrcIPMap.get(device);
                if (map == null) {
                    map = new HashMap<>();
                }
                HashSet<String> set = map.get(port);
                if (set == null) {
                    set = new HashSet<>();
                }
                set.add(prefix);
                map.put(port, set);
                SrcIPMap.put(device, map);
            } else {
                DstIPPool.add(prefix);
                HashMap<String, HashSet<String>> map = DstIPMap.get(device);
                if (map == null) {
                    map = new HashMap<>();
                }
                HashSet<String> set = map.get(port);
                if (set == null) {
                    set = new HashSet<>();
                }
                set.add(prefix);
                map.put(port, set);
                DstIPMap.put(device, map);
            }
        }

        // not used prefix
        // HashSet<Long> prefixSet = new HashSet<>();
        // for(Long curr=0L;curr<256L;curr++)
        // {
        // prefixSet.add(curr);
        // }
        // for(String ip : SrcIPPool)
        // {
        // Long ipLong = Long.parseLong(ip.split("/")[0]);
        // ipLong = ipLong/256;
        // ipLong = ipLong/256;
        // ipLong = ipLong/256;
        // prefixSet.remove(ipLong);
        // }
        // for(String ip : DstIPPool)
        // {
        // Long ipLong = Long.parseLong(ip.split("/")[0]);
        // ipLong = ipLong/256;
        // ipLong = ipLong/256;
        // ipLong = ipLong/256;
        // prefixSet.remove(ipLong);
        // }
        // System.out.println(prefixSet);

        // generate dataset
        FileWriter fw = new FileWriter("/data/zcli-data/pd/NAT/nat", false);
        PrintWriter pw = new PrintWriter(fw);
        int NAT = 100;
        int twiceNAT = 100;
        int targetNum = 1;

        HashSet<Integer> NAT_Device = new HashSet<>();
        HashSet<Integer> twiceNAT_Device = new HashSet<>();
        Random r = new Random();
        while (NAT_Device.size() < NAT) {
            int id = r.nextInt(1646);
            NAT_Device.add(id + 1);
        }
        while (twiceNAT_Device.size() < twiceNAT) {
            int id = r.nextInt(1646);
            if (!NAT_Device.contains(id + 1)) {
                twiceNAT_Device.add(id + 1);
            }
        }

        int id = 0;
        for (int d : NAT_Device) {
            id++;
            String device = "config" + String.valueOf(d);
            String OldSrc = generatePrefix(id);
            String NewSrc = ToPrefix(Long.parseLong(SrcIPMap.get(device).get("self").iterator().next().split("/")[0]));
            PrintRule(device, "self", "in", OldSrc, "any", NewSrc, "any", pw);
            PrintRule(device, "self", "out", "any", NewSrc, "any", OldSrc, pw);
        }

        for (int d : twiceNAT_Device) {
            HashSet<Integer> targetSet = new HashSet<>();
            while (targetSet.size() < targetNum) {
                int target = r.nextInt(1646);
                if (((target + 1) != d) && (!targetSet.contains(target + 1))) {
                    targetSet.add(target + 1);
                }
            }
            for (int target : targetSet) {
                id++;
                String device = "config" + String.valueOf(d);
                String OldSrc = generatePrefix(id);
                String NewSrc = ToPrefix(
                        Long.parseLong(SrcIPMap.get(device).get("self").iterator().next().split("/")[0]));
                String targetDevice = "config" + String.valueOf(target);
                String Dst = ToPrefix(
                        Long.parseLong(SrcIPMap.get(targetDevice).get("self").iterator().next().split("/")[0]));
                PrintRule(device, "self", "in", OldSrc, Dst, NewSrc, "any", pw);
                PrintRule(device, "self", "out", Dst, NewSrc, "any", OldSrc, pw);
            }
        }
    }

    public static String generatePrefix(int v) {
        int first = 100;
        int second;
        int third;
        int forth;
        String ret;
        forth = v % 256;
        v = v / 256;
        third = v % 256;
        v = v / 256;
        second = v % 256;
        ret = String.valueOf(first) + "." + String.valueOf(second) + "." + String.valueOf(third) + "."
                + String.valueOf(forth);
        return ret;
    }

    public String ToPrefix(Long v) {
        Long first;
        Long second;
        Long third;
        Long forth;
        String ret;
        forth = v % 256;
        v = v / 256;
        third = v % 256;
        v = v / 256;
        second = v % 256;
        v = v / 256;
        first = v % 256;
        ret = String.valueOf(first) + "." + String.valueOf(second) + "." + String.valueOf(third) + "."
                + String.valueOf(forth);
        return ret;
    }

    public void PrintRule(String device, String port, String direction, String src, String dst, String new_src,
            String new_dst, PrintWriter pw) {
        pw.println("+ NAT " + device + " " + port + " " + direction + " " + src + " " + dst + " " + new_src + " "
                + new_dst);
        pw.flush();
    }

    class IPPrefix {
        public Long prefix;
        public int len;
        public int[] binRep;
        public String choosen;
    }

    public int[] IPToBin(Long prefix) {
        int[] bin = new int[32];
        for (int curr = 0; curr < 32; curr++) {
            bin[curr] = 0;
        }
        String[] subIP = ToPrefix(prefix).split("\\.");
        for (int i = 0; i < subIP.length; i++) {
            int sum = Integer.parseInt(subIP[i]);
            for (int j = 7; j >= 0; j--) {
                bin[i * 8 + (7 - j)] = sum / Utility.Power2_int(j);
                sum = sum % Utility.Power2_int(j);
            }
        }
        return bin;
    }

    public int[] IPToBin(String prefix) {
        int[] bin = new int[32];
        for (int curr = 0; curr < 32; curr++) {
            bin[curr] = 0;
        }
        String[] subIP = prefix.split("\\.");
        // System.out.println(prefix);
        for (int i = 0; i < subIP.length; i++) {
            int sum = Integer.parseInt(subIP[i]);
            for (int j = 7; j >= 0; j--) {
                bin[i * 8 + (7 - j)] = sum / Utility.Power2_int(j);
                sum = sum % Utility.Power2_int(j);
            }
        }
        return bin;
    }

    public void GenerateNATNew(int num1, int num2) throws IOException {
        // read forwarding rules
        String fw_path = "/data/zcli-data/pd/dpv/purdue/change_base";
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(fw_path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ArrayList<String> FW_rules = new ArrayList<String>();
        String OneLine;
        while ((OneLine = br.readLine()) != null) {
            String linestr = OneLine.trim();
            FW_rules.add(linestr);
        }

        // read edge ports
        String edge_path = "/data/zcli-data/pd/edgePorts";
        try {
            br = new BufferedReader(new FileReader(edge_path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        HashSet<String> devices = new HashSet<>();
        HashMap<String, HashSet<String>> edge_ports = new HashMap<>();
        while ((OneLine = br.readLine()) != null) {
            String linestr = OneLine.trim();
            String[] tokens = linestr.split(" ");
            devices.add(tokens[0]);
            HashSet<String> set = edge_ports.get(tokens[0]);
            if (set == null) {
                set = new HashSet<>();
            }
            set.add(tokens[1]);
            edge_ports.put(tokens[0], set);
        }

        // get src ip pool
        HashMap<String, HashMap<String, HashSet<IPPrefix>>> SrcIPMap = new HashMap<>();
        for (String line : FW_rules) {
            String[] tokens = line.split(" ");
            String device = tokens[2];
            String ip = tokens[3];
            String len = tokens[4];
            String prefix = ip + "/" + len;
            String port = tokens[5];
            if (edge_ports.get(device).contains(port)) {
                HashMap<String, HashSet<IPPrefix>> map = SrcIPMap.get(device);
                if (map == null) {
                    map = new HashMap<>();
                }
                HashSet<IPPrefix> set = map.get(port);
                if (set == null) {
                    set = new HashSet<>();
                }
                IPPrefix p = new IPPrefix();
                p.prefix = Long.parseLong(ip);
                p.len = Integer.parseInt(len);
                set.add(p);
                map.put(port, set);
                SrcIPMap.put(device, map);
            }
        }

        // get real ip prefix in config file
        for (String device : SrcIPMap.keySet()) {
            for (String port : SrcIPMap.get(device).keySet()) {
                for (IPPrefix p : SrcIPMap.get(device).get(port)) {
                    boolean match = false;
                    p.binRep = IPToBin(p.prefix);
                    for (String realIP : PrefixInFile(device)) {
                        int[] realBin = IPToBin(realIP);
                        if (Contain(p.binRep, realBin, p.len)) {
                            match = true;
                            p.choosen = realIP;
                            break;
                        }
                    }
                    if (!match){
                        String [] tokens = ToPrefix(p.prefix).split("\\.");
                        p.choosen = tokens[0]+"."+tokens[1]+"."+tokens[2]+".1";
                    }
                }
            }
        }

        // generate dataset
        FileWriter fw = new FileWriter("/data/zcli-data/pd/NAT/nat"+num1+"_"+num2, false);
        PrintWriter pw = new PrintWriter(fw);
        int NAT = num1;
        int twiceNAT = num2;
        int targetNum = 1;

        HashSet<Integer> NAT_Device = new HashSet<>();
        HashSet<Integer> twiceNAT_Device = new HashSet<>();
        Random r = new Random();
        while (NAT_Device.size() < NAT) {
            int id = r.nextInt(1646);
            NAT_Device.add(id + 1);
        }
        while (twiceNAT_Device.size() < twiceNAT) {
            int id = r.nextInt(1646);
            if (!NAT_Device.contains(id + 1)) {
                twiceNAT_Device.add(id + 1);
            }
        }

        int id = 0;
        for (int d : NAT_Device) {
            id++;
            String device = "config" + String.valueOf(d);
            String OldSrc = generatePrefix(id);
            String NewSrc = SrcIPMap.get(device).get("self").iterator().next().choosen;
            PrintRule(device, "self", "in", OldSrc, "any", NewSrc, "any", pw);
            PrintRule(device, "self", "out", "any", NewSrc, "any", OldSrc, pw);
        }

        for (int d : twiceNAT_Device) {
            HashSet<Integer> targetSet = new HashSet<>();
            while (targetSet.size() < targetNum) {
                int target = r.nextInt(1646);
                if (((target + 1) != d) && (!targetSet.contains(target + 1))) {
                    targetSet.add(target + 1);
                }
            }
            for (int target : targetSet) {
                id++;
                String device = "config" + String.valueOf(d);
                String OldSrc = generatePrefix(id);
                String NewSrc = SrcIPMap.get(device).get("self").iterator().next().choosen;
                String targetDevice = "config" + String.valueOf(target);
                String Dst = SrcIPMap.get(targetDevice).get("self").iterator().next().choosen;
                PrintRule(device, "self", "in", OldSrc, Dst, NewSrc, "any", pw);
                PrintRule(device, "self", "out", Dst, NewSrc, "any", OldSrc, pw);
            }
        }
    }

    public boolean Contain(int[] a, int[] b, int len) {
        boolean contain = true;
        for (int curr = 0; curr < len; curr++) {
            if (a[curr] != b[curr]) {
                contain = false;
                break;
            }
        }
        return contain;
    }

    public ArrayList<String> PrefixInFile(String device) throws IOException {
        String basePath = "/data/zcli-data/purdue/config/";
        String path = basePath + device;

        ArrayList<String> prefixs = new ArrayList<>();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ArrayList<String> lines = new ArrayList<String>();
        String OneLine;
        while ((OneLine = br.readLine()) != null) {
            String linestr = OneLine.trim();
            String[] tokens = linestr.split(" ");
            for (int curr = 0; curr < tokens.length; curr++) {
                if (tokens[curr].split("\\.").length == 4 && isNumeric(tokens[curr].split("\\.")[0]) && isNumeric(tokens[curr].split("\\.")[1]) && isNumeric(tokens[curr].split("\\.")[2]) && isNumeric(tokens[curr].split("\\.")[3])) {
                    prefixs.add(tokens[curr]);
                }
            }
        }

        return prefixs;
    }

    public static boolean isNumeric(String str)
    {
        // System.out.println(str);
        for (int i = 0;i < str.length();i++)
        {
            // System.out.println(str.charAt(i));
            if (!Character.isDigit(str.charAt(i)))
            {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) throws IOException {
        generateNat g = new generateNat();
        // g.GenerateNATNew(10,10);
        g.GenerateNATNew(50,50);
        // g.GenerateNATNew(100,100);
        // g.GenerateNATNew(200,200);
        // g.GenerateNATNew(500,500);
        // g.generate();
    }
}