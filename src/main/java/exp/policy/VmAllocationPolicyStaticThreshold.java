package exp.policy;

import cloudsim.Host;
import cloudsim.core.CloudSim;
import cloudsim.power.PowerHost;
import cloudsim.power.PowerVmAllocationPolicyMigrationStaticThreshold;
import cloudsim.power.PowerVmSelectionPolicy;

import java.util.List;

public class VmAllocationPolicyStaticThreshold extends PowerVmAllocationPolicyMigrationStaticThreshold {

    public VmAllocationPolicyStaticThreshold(List<? extends Host> hostList, PowerVmSelectionPolicy vmSelectionPolicy, double utilizationThreshold) {
        super(hostList, vmSelectionPolicy, utilizationThreshold);
    }

    @Override
    protected boolean isHostOverUtilized(PowerHost host) {
        HostPowerModeSelectionPolicyAgent.getTimeList().add(CloudSim.clock());
        HostPowerModeSelectionPolicyAgent.getSlaViolationTime(getHostList());
        HostPowerModeSelectionPolicyAgent.getTotalPowerAndMigrationCount(getHostList());
        return super.isHostOverUtilized(host);
    }
}