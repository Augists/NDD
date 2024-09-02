import apkeep.utils.ExperimentTools;
import jdd.bdd.BDD;
import jdd.bdd.BDDIO;
import jdd.util.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

public class test {
    public static void compare_file() throws IOException {
        String path1 = "/home/zcli/lzc/Field-Decision-Network/apkatra-main/pic/post3.txt";
        String path2 = "/home/zcli/lzc/Field-Decision-Network/apkatra-main/pic/katra_post3.txt";
        ArrayList<String> array1 = ExperimentTools.readFromFile(path1);
        ArrayList<String> array2 = ExperimentTools.readFromFile(path2);
        HashMap<String, HashSet<String>> map1 = new HashMap<>();
        HashMap<String, HashSet<String>> map2 = new HashMap<>();

        for (String str : array1)
        {
            String [] tokens = str.split(" ");
            HashSet<String> set = map1.get(tokens[0]);
            if (set == null)
            {
                set = new HashSet<>();
            }
            for (int curr=1;curr<tokens.length;curr++)
            {
                set.add(tokens[curr]);
            }
            map1.put(tokens[0], set);
        }

        for (String str : array2)
        {
            String [] tokens = str.split(" ");
            HashSet<String> set = map2.get(tokens[0]);
            if (set == null)
            {
                set = new HashSet<>();
            }
            for (int curr=1;curr<tokens.length;curr++)
            {
                set.add(tokens[curr]);
            }
            map2.put(tokens[0], set);
        }

        System.out.println("FDN - Katra");
        for (String from : map1.keySet())
        {
            HashSet<String> set1 = new HashSet<>(map1.get(from));
            if(map2.containsKey(from))
            {
                HashSet<String> set2 = new HashSet<>(map2.get(from));
                set1.removeAll(set2);
            }
            if (!set1.isEmpty())
            {
                System.out.println(from+" "+set1);
            }
        }

        System.out.println("Katra - FDN");
        for (String from : map2.keySet())
        {
            HashSet<String> set2 = new HashSet<>(map2.get(from));
            if(map1.containsKey(from))
            {
                HashSet<String> set1 = new HashSet<>(map1.get(from));
                set2.removeAll(set1);
            }
            if (!set2.isEmpty())
            {
                System.out.println(from+" "+set2);
            }
        }
    }

    public static void compare_file1() throws IOException {
        // String path1 = "/home/zcli/lzc/Field-Decision-Network/apkatra-main/output/test/Katra";
        // String path2 = "/home/zcli/lzc/Field-Decision-Network/apkatra-main/output/test/NDD";

        String path1 = "/home/zcli/lzc/Field-Decision-Network/apkatra-main/output/test/APKeepNAT";
        String path2 = "/home/zcli/lzc/Field-Decision-Network/apkatra-main/output/test/NDDNAT";

        ArrayList<String> array1 = ExperimentTools.readFromFile(path1);
        ArrayList<String> array2 = ExperimentTools.readFromFile(path2);
        HashSet<String> set1 = new HashSet<>();
        HashSet<String> set2 = new HashSet<>();

        for (String str : array1)
        {
            // set1.add(str.split(" ")[0]+" "+str.split(" ")[1]);
            set1.add(str);
        }

        for (String str : array2)
        {
            // set2.add(str.split(" ")[0]+" "+str.split(" ")[1]);
            set2.add(str);
        }

        System.out.println("NDD - BDD");
        for (String from : set2)
        {
            if(!set1.contains(from))
            {
                System.out.println(from);
            }
        }

        System.out.println("BDD - NDD");
        for (String from : set1)
        {
            if(!set2.contains(from))
            {
                System.out.println(from);
            }
        }
    }

    public static void compareReachPacketSet(String dataset, String post) throws IOException
	{
        System.out.println(dataset+" "+post);
        int [] vars = new int [104];
        BDD bdd = new BDD(10000000, 10000000);
        for (int i = 104 - 1; i >= 0; i--) {
			vars[i] = bdd.createVar();
        }
        
        int otherFields = 1;
        for(int curr=24;curr<104;curr++)
        {
            int t = bdd.ref(bdd.and(otherFields, vars[curr]));
            bdd.deref(otherFields);
            otherFields = t;
        }

		HashMap<String,Integer> map = new HashMap<>();

        System.out.println("NDD");
		String NDDPath = "/home/zcli/lzc/Field-Decision-Network/apkatra-main/output/correctness/"+dataset+"/NDD/"+post+"/";
		File[] inFiles = Paths.get(NDDPath).toFile().listFiles();
		for(File f : inFiles)
		{
			String StrReach = f.toString().split("/")[11];
            map.put(StrReach, bdd.ref(BDDIO.load(bdd, f.toString())));
            // System.out.println(StrReach);
        }
        System.out.println(map.size());

        System.out.println("Katra");
        int KatraCount=0;
		String BDDPath1 = "/home/zcli/lzc/Field-Decision-Network/apkatra-main/output/correctness/"+dataset+"/Katra/"+post+"/";
		File[] inFiles1 = Paths.get(BDDPath1).toFile().listFiles();
		for(File f : inFiles1)
		{
			String StrReach = f.toString().split("/")[11];
            int packets = bdd.ref(BDDIO.load(bdd, f.toString()));
            // System.out.println(StrReach+" "+map.get(StrReach)+" "+packets);

            if(!map.containsKey(StrReach))
            {
                // System.out.println(StrReach);
            }
			else if(map.get(StrReach) != packets)
			{
                System.out.println("tag:     "+StrReach);

                bdd.printDot("/home/zcli/lzc/Field-Decision-Network/apkatra-main/pic/"+StrReach+"_NDD", map.get(StrReach));
                bdd.printDot("/home/zcli/lzc/Field-Decision-Network/apkatra-main/pic/"+StrReach+"_Katra", packets);
                break;
            }
            else
            {
                KatraCount++;
            }
        }
        System.out.println(KatraCount);
        
        if(dataset.equalsIgnoreCase("APKeep_6") || dataset.equalsIgnoreCase("APKeep_10"))
        {
            System.out.println("APT");
            int APTCount=0;
            String BDDPath2 = "/home/zcli/lzc/Field-Decision-Network/apkatra-main/output/correctness/"+dataset+"/APT/"+post+"/";
            File[] inFiles2 = Paths.get(BDDPath2).toFile().listFiles();
            for(File f : inFiles2)
            {
                String StrReach = f.toString().split("/")[11];
                int packets = bdd.ref(BDDIO.load(bdd, f.toString()));
                // System.out.println(StrReach+" "+map.get(StrReach)+" "+packets);
    
                if(!map.containsKey(StrReach))
                {
                    System.out.println(StrReach);
                }
                else if(map.get(StrReach) != packets)
                {
                    System.out.println("tag:     "+StrReach);
    
                    // bdd.printDot("/home/zcli/lzc/Field-Decision-Network/apkatra-main/pic/"+StrReach+"_NDD", map.get(StrReach));
                    // bdd.printDot("/home/zcli/lzc/Field-Decision-Network/apkatra-main/pic/"+StrReach+"_Katra", packets);
                    // break;
                }
                else
                {
                    // System.out.println(StrReach);
                    APTCount++;
                }
            }
            System.out.println(APTCount);
        }
        
        if(dataset.equalsIgnoreCase("APKeep_6"))
        {
            System.out.println("APKeep");
            int APKeepCount=0;
            String BDDPath3 = "/home/zcli/lzc/Field-Decision-Network/apkatra-main/output/correctness/"+dataset+"/APKeep/"+post+"/";
            File[] inFiles3 = Paths.get(BDDPath3).toFile().listFiles();
            for(File f : inFiles3)
            {
                String StrReach = f.toString().split("/")[11];
                int packets = bdd.ref(BDDIO.load(bdd, f.toString()));
                // System.out.println(StrReach+" "+map.get(StrReach)+" "+packets);
    
                if(!map.containsKey(StrReach))
                {
                    // System.out.println(StrReach);
                }
                else if(map.get(StrReach) != packets)
                {
                    System.out.println("tag:     "+StrReach);
    
                    // bdd.printDot("/home/zcli/lzc/Field-Decision-Network/apkatra-main/pic/"+StrReach+"_NDD", map.get(StrReach));
                    // bdd.printDot("/home/zcli/lzc/Field-Decision-Network/apkatra-main/pic/"+StrReach+"_Katra", packets);
                    // break;
                }
                else
                {
                    // System.out.println(StrReach);
                    APKeepCount++;
                }
            }
            System.out.println(APKeepCount);
        }

        System.out.println("Katra_NDD");
        int KatraNDDCount=0;
		String BDDPath4 = "/home/zcli/lzc/Field-Decision-Network/apkatra-main/output/correctness/"+dataset+"/Katra_NDD/"+post+"/";
		File[] inFiles4 = Paths.get(BDDPath4).toFile().listFiles();
		for(File f : inFiles4)
		{
			String StrReach = f.toString().split("/")[11];
            int packets = bdd.ref(BDDIO.load(bdd, f.toString()));
            // System.out.println(StrReach+" "+map.get(StrReach)+" "+packets);

            if(!map.containsKey(StrReach))
            {
                // System.out.println(StrReach);
            }
			else if(map.get(StrReach) != packets)
			{
                System.out.println("tag:     "+StrReach);

                bdd.printDot("/home/zcli/lzc/Field-Decision-Network/apkatra-main/pic/"+StrReach+"_NDD", map.get(StrReach));
                bdd.printDot("/home/zcli/lzc/Field-Decision-Network/apkatra-main/pic/"+StrReach+"_Katra", packets);
                break;
            }
            else
            {
                KatraNDDCount++;
            }
        }
        System.out.println(KatraNDDCount);
    }
    
    public static void compareReachPacketSetKatraDataset(String dataset, String post) throws IOException
	{
        System.out.println(dataset+" "+post);
        BDD bdd = new BDD(10000000, 10000000);

		HashMap<String,Integer> map = new HashMap<>();

        System.out.println("NDD");
		String NDDPath = "/home/zcli/lzc/Field-Decision-Network/apkatra-main/output/correctness/"+dataset+"/NDD/"+post+"/";
		File[] inFiles = Paths.get(NDDPath).toFile().listFiles();
		for(File f : inFiles)
		{
			String StrReach = f.toString().split("/")[11];
            map.put(StrReach, bdd.ref(BDDIO.load(bdd, f.toString())));
            // System.out.println(StrReach);
        }
        System.out.println(map.size());

        System.out.println("Katra");
        int KatraCount=0;
		String BDDPath1 = "/home/zcli/lzc/Field-Decision-Network/apkatra-main/output/correctness/"+dataset+"/Katra/"+post+"/";
		File[] inFiles1 = Paths.get(BDDPath1).toFile().listFiles();
		for(File f : inFiles1)
		{
			String StrReach = f.toString().split("/")[11];
            int packets = bdd.ref(BDDIO.load(bdd, f.toString()));

            if(!map.containsKey(StrReach))
            {
                if(StrReach.split("_").length != 2)System.out.println(StrReach);
            }
			else if(map.get(StrReach) != packets)
			{
                System.out.println("tag:     "+StrReach);

                // bdd.printDot("/home/zcli/lzc/Field-Decision-Network/apkatra-main/pic/"+StrReach+"_NDD", map.get(StrReach));
                // bdd.printDot("/home/zcli/lzc/Field-Decision-Network/apkatra-main/pic/"+StrReach+"_Katra", packets);
                // break;
            }
            else
            {
                // System.out.println(StrReach+" "+map.get(StrReach)+" "+packets);
                KatraCount++;
            }
        }
        System.out.println(KatraCount);
    }

    public static void compareReachPacketSetNAT() throws IOException
	{
        BDD bdd = new BDD(10000000, 10000000);

		HashMap<String,Integer> map = new HashMap<>();

        System.out.println("NDD");
		String NDDPath = "/home/zcli/lzc/Field-Decision-Network/apkatra-main/output/single/NAT/correctness/NDD/";
		File[] inFiles = Paths.get(NDDPath).toFile().listFiles();
		for(File f : inFiles)
		{
			String StrReach = f.toString().split("/")[11];
            map.put(StrReach, bdd.ref(BDDIO.load(bdd, f.toString())));
            // System.out.println(StrReach);
        }
        System.out.println(map.size());

        System.out.println("BDD");
        int KatraCount=0;
		String BDDPath1 = "/home/zcli/lzc/Field-Decision-Network/apkatra-main/output/single/NAT/correctness/APKeep/";
		File[] inFiles1 = Paths.get(BDDPath1).toFile().listFiles();
		for(File f : inFiles1)
		{
			String StrReach = f.toString().split("/")[11];
            int packets = bdd.ref(BDDIO.load(bdd, f.toString()));

            // if(!map.containsKey(StrReach))
            // {
            //     if(StrReach.split("_").length != 2)System.out.println(StrReach);
            // }
			if(map.get(StrReach) != packets)
			{
                System.out.println("tag:     "+StrReach);

                // bdd.printDot("/home/zcli/lzc/Field-Decision-Network/apkatra-main/pic/"+StrReach+"_NDD", map.get(StrReach));
                // bdd.printDot("/home/zcli/lzc/Field-Decision-Network/apkatra-main/pic/"+StrReach+"_Katra", packets);
                // break;
            }
            else
            {
                // System.out.println(StrReach+" "+map.get(StrReach)+" "+packets);
                KatraCount++;
            }
        }
        System.out.println(KatraCount);
    }

    public static void main(String[] args) throws IOException {
        // compare_file();

        // compare_file1();

        compareReachPacketSet("APKeep_6","base");
        compareReachPacketSet("APKeep_6","post0");
        compareReachPacketSet("APKeep_6","post1");
        compareReachPacketSet("APKeep_6","post2");
        compareReachPacketSet("APKeep_6","post3");
        compareReachPacketSet("APKeep_6","post4");

        compareReachPacketSet("APKeep_10","base");
        compareReachPacketSet("APKeep_10","post0");
        compareReachPacketSet("APKeep_10","post1");
        compareReachPacketSet("APKeep_10","post2");
        compareReachPacketSet("APKeep_10","post3");
        compareReachPacketSet("APKeep_10","post4");

        // compareReachPacketSetKatraDataset("katra_2_60","origin");

        // compareReachPacketSetNAT();
    }
}
