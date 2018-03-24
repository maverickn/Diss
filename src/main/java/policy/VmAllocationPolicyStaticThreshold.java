package policy;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicyMigrationStaticThreshold;
import org.cloudbus.cloudsim.power.PowerVmSelectionPolicy;

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