package exp.policy;

import cloudsim.Host;
import cloudsim.core.CloudSim;
import cloudsim.power.PowerHost;
import cloudsim.power.PowerVmAllocationPolicyMigrationAbstract;
import cloudsim.power.PowerVmAllocationPolicyMigrationMedianAbsoluteDeviation;
import cloudsim.power.PowerVmSelectionPolicy;

import java.util.List;

public class VmAllocationPolicyMedianAbsoluteDeviation extends PowerVmAllocationPolicyMigrationMedianAbsoluteDeviation {

    public VmAllocationPolicyMedianAbsoluteDeviation(List<? extends Host> hostList, PowerVmSelectionPolicy vmSelectionPolicy, double safetyParameter, PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy) {
        super(hostList, vmSelectionPolicy, safetyParameter, fallbackVmAllocationPolicy);
    }

    @Override
    protected boolean isHostOverUtilized(PowerHost host) {
        HostPowerModeSelectionPolicyAgent.getTimeList().add(CloudSim.clock());
        HostPowerModeSelectionPolicyAgent.getSlaViolationTime(getHostList());
        HostPowerModeSelectionPolicyAgent.getTotalPowerAndMigrationCount(getHostList());
        return super.isHostOverUtilized(host);
    }
}