package apkeep.main;

import java.io.IOException;

import apkeep.utils.ExperimentTools;
import apkeep.utils.Parameters;

public class Test {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		String name = "DCN_xjtu";
		String apt_results = Parameters.dataset_path+name+"/reach-apt/"+name+"_base";
		String apkeep_results = Parameters.dataset_path+name+"/reach-apkeep/"+name+"_base";
		String target_results = Parameters.dataset_path+name+"/reach-apt/"+name+"_diff";
		
		ExperimentTools.compareFile(apt_results, apkeep_results,target_results);
	}

}
