package exp.policy;

import cloudsim.Host;
import cloudsim.core.CloudSim;
import cloudsim.power.PowerHost;
import cloudsim.power.PowerVmAllocationPolicyMigrationAbstract;
import cloudsim.power.PowerVmAllocationPolicyMigrationLocalRegressionRobust;
import cloudsim.power.PowerVmSelectionPolicy;

import java.util.List;

public class VmAllocationPolicyLocalRegressionRobust extends PowerVmAllocationPolicyMigrationLocalRegressionRobust {

    public VmAllocationPolicyLocalRegressionRobust(List<? extends Host> hostList, PowerVmSelectionPolicy vmSelectionPolicy, double safetyParameter, double schedulingInterval, PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy) {
        super(hostList, vmSelectionPolicy, safetyParameter, schedulingInterval, fallbackVmAllocationPolicy);
    }

    @Override
    protected boolean isHostOverUtilized(PowerHost host) {
        HostPowerModeSelectionPolicyAgent.getTimeList().add(CloudSim.clock());
        HostPowerModeSelectionPolicyAgent.getSlaViolationTime(getHostList());
        HostPowerModeSelectionPolicyAgent.getTotalPowerAndMigrationCount(getHostList());
        return super.isHostOverUtilized(host);
    }
}