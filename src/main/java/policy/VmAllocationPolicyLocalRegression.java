package policy;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicyMigrationAbstract;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicyMigrationLocalRegression;
import org.cloudbus.cloudsim.power.PowerVmSelectionPolicy;

import java.util.List;

public class VmAllocationPolicyLocalRegression extends PowerVmAllocationPolicyMigrationLocalRegression {

    public VmAllocationPolicyLocalRegression(List<? extends Host> hostList, PowerVmSelectionPolicy vmSelectionPolicy, double safetyParameter, double schedulingInterval, PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy) {
        super(hostList, vmSelectionPolicy, safetyParameter, schedulingInterval, fallbackVmAllocationPolicy);
    }

    protected boolean isHostOverUtilized(PowerHost host) {
        HostPowerModeSelectionPolicyAgent.getTimeList().add(CloudSim.clock());
        HostPowerModeSelectionPolicyAgent.getSlaViolationTime(getHostList());
        HostPowerModeSelectionPolicyAgent.getTotalPowerAndMigrationCount(getHostList());
        return super.isHostOverUtilized(host);
    }

    public boolean isHostOverUtil(PowerHost host) {
        return super.isHostOverUtilized(host);
    }
}