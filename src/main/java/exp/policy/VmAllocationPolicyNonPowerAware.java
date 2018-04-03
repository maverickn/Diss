package exp.policy;

import cloudsim.Host;
import cloudsim.Vm;
import cloudsim.VmAllocationPolicySimple;
import cloudsim.core.CloudSim;

import java.util.List;
import java.util.Map;

public class VmAllocationPolicyNonPowerAware extends VmAllocationPolicySimple {

    public VmAllocationPolicyNonPowerAware(List<? extends Host> list) {
        super(list);
    }

    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList) {
        HostPowerModeSelectionPolicyAgent.getTimeList().add(CloudSim.clock());
        HostPowerModeSelectionPolicyAgent.getSlaViolationTime(getHostList());
        HostPowerModeSelectionPolicyAgent.getTotalPowerAndMigrationCount(getHostList());
        return null;
    }
}

