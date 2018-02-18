import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.CloudletSchedulerDynamicWorkload;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.VmSchedulerTimeSharedOverSubscription;
import org.cloudbus.cloudsim.power.PowerDatacenterBroker;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerHostUtilizationHistory;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class Environment {

	public static List<Vm> createVmList(int brokerId, int vmsCount) {
		List<Vm> vms = new ArrayList<>();
		for (int i = 0; i < vmsCount; i++) {
			int vmType = i / (int) Math.ceil((double) vmsCount / Parameters.VM_TYPES);
			vms.add(
					new PowerVm(
					i,
					brokerId,
					Parameters.VM_MIPS[vmType],
					Parameters.VM_PES[vmType],
					Parameters.VM_RAM[vmType],
					Parameters.VM_BW,
					Parameters.VM_SIZE,
					1,
					"Xen",
					new CloudletSchedulerDynamicWorkload(Parameters.VM_MIPS[vmType], Parameters.VM_PES[vmType]),
					Parameters.SCHEDULING_INTERVAL)
			);
		}
		return vms;
	}

	public static List<PowerHost> createHostList(int hostsCount) {
		List<PowerHost> hostList = new ArrayList<>();
		for (int i = 0; i < hostsCount; i++) {
			int hostType = i % Parameters.HOST_TYPES;
			List<Pe> peList = new ArrayList<>();
			for (int j = 0; j < Parameters.HOST_PES[hostType]; j++) {
				peList.add(new Pe(j, new PeProvisionerSimple(Parameters.HOST_MIPS[hostType])));
			}
			hostList.add(
					new PowerHostUtilizationHistory(
					i,
					new RamProvisionerSimple(Parameters.HOST_RAM[hostType]),
					new BwProvisionerSimple(Parameters.HOST_BW),
					Parameters.HOST_STORAGE,
					peList,
					new VmSchedulerTimeSharedOverSubscription(peList),
					Parameters.HOST_POWER[hostType])
			);
		}
		return hostList;
	}

	public static DatacenterBroker createBroker() {
		DatacenterBroker broker = null;
		try {
			broker = new PowerDatacenterBroker("Broker");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		return broker;
	}

	public static Datacenter createDatacenter(
			String name,
			Class<? extends Datacenter> datacenterClass,
			List<PowerHost> hostList,
			VmAllocationPolicy vmAllocationPolicy) throws Exception {
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
		Datacenter datacenter = null;
		try {
			datacenter = datacenterClass.getConstructor(
					String.class,
					DatacenterCharacteristics.class,
					VmAllocationPolicy.class,
					List.class,
					Double.TYPE).newInstance(
						name,
						characteristics,
						vmAllocationPolicy,
						new LinkedList<Storage>(),
						Parameters.SCHEDULING_INTERVAL);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		return datacenter;
	}

}
