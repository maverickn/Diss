package exp;

import java.io.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import cloudsim.UtilizationModelBitbrains;
import cloudsim.*;
import cloudsim.power.*;
import cloudsim.provisioners.BwProvisionerSimple;
import cloudsim.provisioners.PeProvisionerSimple;
import cloudsim.provisioners.RamProvisionerSimple;

public class SetupEntities {

    public final static int CLOUDLET_LENGTH	= 2500 * (int) ParseConfig.simulationLimit;
    public final static int CLOUDLET_PES = 1;

	public static List<Vm> createVmList(int brokerId, int vmsCount) {
		List<Vm> vms = new ArrayList<>();
		for (int i = 0; i < vmsCount; i++) {
			int vmType = i / (int) Math.ceil((double) vmsCount / ParseConfig.vmTypesCount);
			vms.add(
					new PowerVm(
					i,
					brokerId, ParseConfig.vmMips[vmType], ParseConfig.vmPes[vmType],
							ParseConfig.vmRam[vmType], ParseConfig.vmBw, ParseConfig.vmSize,
					1,
					"Xen",
					new CloudletSchedulerDynamicWorkload(ParseConfig.vmMips[vmType], ParseConfig.vmPes[vmType]),
                            ParseConfig.schedulingInterval)
			);
		}
		return vms;
	}

	public static List<PowerHost> createHostList(int hostsCount) {
		List<PowerHost> hostList = new ArrayList<>();
		for (int i = 0; i < hostsCount; i++) {
			int hostType = i % ParseConfig.hostTypesCount;
			List<Pe> peList = new ArrayList<>();
			for (int j = 0; j < ParseConfig.hostPes[hostType]; j++) {
				peList.add(new Pe(j, new PeProvisionerSimple(ParseConfig.hostMips[hostType])));
			}
			hostList.add(
					new PowerHostUtilizationHistory(
					i,
					new RamProvisionerSimple(ParseConfig.hostRam[hostType]),
					new BwProvisionerSimple(ParseConfig.hostBw), ParseConfig.hostStorage, peList,
					new VmSchedulerTimeSharedOverSubscription(peList), ParseConfig.hostTypes[hostType])
			);
		}
		return hostList;
	}

	public static DatacenterBroker createBroker() throws Exception {
		return new PowerDatacenterBroker("Broker");
	}

	public static Datacenter createDatacenter(
			String name,
			Class<? extends Datacenter> datacenterClass,
			List<PowerHost> hostList,
			VmAllocationPolicy vmAllocationPolicy)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		String arch = "x86";
		String os = "Linux";
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);
		Datacenter datacenter = datacenterClass.getConstructor(
					String.class,
					DatacenterCharacteristics.class,
					VmAllocationPolicy.class,
					List.class,
					Double.TYPE).newInstance(
						name,
						characteristics,
						vmAllocationPolicy,
						new LinkedList<Storage>(),
                    ParseConfig.schedulingInterval);
		return datacenter;
	}

	public static List<Cloudlet> createCloudletList(int brokerId, String inputFolderName) throws IOException {
		List<Cloudlet> list = new ArrayList<>();
		int dataSamples = ParseConfig.simulationLimit / ParseConfig.schedulingInterval;
		long fileSize = 300;
		long outputSize = 300;
		java.io.File inputFolder = new java.io.File(inputFolderName);
		File[] files = inputFolder.listFiles();
		for (int i = 0; i < files.length; i++) {
			Cloudlet cloudlet = new Cloudlet(i, CLOUDLET_LENGTH, CLOUDLET_PES, fileSize, outputSize,
						new UtilizationModelBitbrains(files[i].getAbsolutePath(), ParseConfig.schedulingInterval, dataSamples),
						new UtilizationModelBitbrains(files[i].getAbsolutePath(), ParseConfig.schedulingInterval, dataSamples, ParseConfig.vmRam),
						new UtilizationModelBitbrains(files[i].getAbsolutePath(), ParseConfig.schedulingInterval, dataSamples, ParseConfig.vmBw));
			cloudlet.setUserId(brokerId);
			cloudlet.setVmId(i);
			list.add(cloudlet);
		}
		return list;
	}
}
