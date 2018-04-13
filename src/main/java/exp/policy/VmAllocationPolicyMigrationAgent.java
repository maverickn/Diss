package exp.policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cloudsim.*;
import cloudsim.core.CloudSim;
import cloudsim.power.*;
import cloudsim.power.lists.PowerVmList;

public abstract class VmAllocationPolicyMigrationAgent extends PowerVmAllocationPolicyAbstract {

    /**
     * The vm selection policy.
     */
    private PowerVmSelectionPolicy vmSelectionPolicy;

    /**
     * The vm allocation policy.
     */
    private PowerVmAllocationPolicyMigrationLocalRegression vmAllocationPolicy;

    /**
     * A list of maps between a VM and the host where it is place.
     */
    private final List<Map<String, Object>> savedAllocation = new ArrayList<>();

    /**
     * Instantiates a new VmAllocationPolicyMigrationAgent.
     *
     * @param hostList the host list
     * @param vmSelectionPolicy the vm selection policy
     */
    public VmAllocationPolicyMigrationAgent(List<? extends Host> hostList, PowerVmSelectionPolicy vmSelectionPolicy, PowerVmAllocationPolicyMigrationLocalRegression vmAllocationPolicy) {
        super(hostList);
        setVmSelectionPolicy(vmSelectionPolicy);
        setVmAllocationPolicy(vmAllocationPolicy);
    }

    /**
     * Optimize allocation of the VMs according to current utilization.
     *
     * @param vmList the vm list
     * @return the array list< hash map< string, object>>
     */
    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList) {
        HostPowerModeSelectionPolicyAgent.getTimeList().add(CloudSim.clock());
        HostPowerModeSelectionPolicyAgent.getSlaViolationTime(getHostList());
        HostPowerModeSelectionPolicyAgent.getTotalPowerAndMigrationCount(getHostList());

        int[] actionIdAndHostPowerMode = getHostPowerMode();
        if (actionIdAndHostPowerMode[0] % 2 == 0) {
            actionIdAndHostPowerMode[0] = actionIdAndHostPowerMode[0] / 2;
        } else {
            actionIdAndHostPowerMode[0] = (actionIdAndHostPowerMode[0] - 1) / 2;
        }

        PowerHostUtilizationHistory hostToChangePowerMode = null;
        for (PowerHostUtilizationHistory host : this.<PowerHostUtilizationHistory> getHostList()) {
            if (actionIdAndHostPowerMode[0] == host.getId()) {
                hostToChangePowerMode = host;
            }
        }

        List<PowerHostUtilizationHistory> hibernateHosts = getSwitchedOffHosts();

        if(actionIdAndHostPowerMode[1] == -1) {
            System.out.println("Need to poweroff host");
            System.out.println("Host id " + hostToChangePowerMode.getId());

            if(isSwitchedOffHost(hostToChangePowerMode)) {
                return null;
            }

            saveAllocation();

            List<? extends Vm> vmsToMigrate = getVmsToMigrateFromUnderUtilizedHost(hostToChangePowerMode);

            hibernateHosts.add(hostToChangePowerMode);

            Log.printLine("Reallocation of VMs from the powered off host:");
            List<Map<String, Object>> migrationMap = getNewVmPlacementFromUnderUtilizedHost(vmsToMigrate, new HashSet<Host>(hibernateHosts));
            Log.printLine();

            restoreAllocation();

            System.out.println("Finish to poweroff host");

            System.out.println("Migration map: " + migrationMap.toString());
            //System.exit(0);

            return migrationMap;
        } else {
            System.out.println("Need to poweron host");
            System.out.println("Host id " + hostToChangePowerMode.getId());

            if(!isSwitchedOffHost(hostToChangePowerMode)) {
                return null;
            }

            hibernateHosts.remove(hostToChangePowerMode);
            List<PowerHostUtilizationHistory> excludedHosts = hibernateHosts;

            List<PowerHostUtilizationHistory> overUtilizedHosts = getOverUtilizedHosts();

            printOverUtilizedHosts(overUtilizedHosts);

            saveAllocation();

            List<? extends Vm> vmsToMigrate = getVmsToMigrateFromHosts(overUtilizedHosts);

            Log.printLine("Reallocation of VMs from the over-utilized hosts:");
            excludedHosts.addAll(overUtilizedHosts);
            List<Map<String, Object>> migrationMap = getNewVmPlacement(vmsToMigrate, new HashSet<Host>(excludedHosts));
            Log.printLine();

            restoreAllocation();

            System.out.println("Finish to poweron host");

            System.out.println("Migration map: " + migrationMap.toString());
            //System.exit(0);

            return migrationMap;
        }
    }

    /**
     * Get host id and power mode.
     *
     * @return action index and host power mode
     */
    protected abstract int[] getHostPowerMode();

    /**
     * Prints the over utilized hosts.
     *
     * @param overUtilizedHosts the over utilized hosts
     */
    protected void printOverUtilizedHosts(List<PowerHostUtilizationHistory> overUtilizedHosts) {
        if (!Log.isDisabled()) {
            Log.printLine("Over-utilized hosts:");
            for (PowerHostUtilizationHistory host : overUtilizedHosts) {
                Log.printConcatLine("Host #", host.getId());
            }
            Log.printLine();
        }
    }

    /**
     * Finds a PM that has enough resources to host a given VM and that will not be overloaded after placing the VM on it.
     * The selected host will be that one with most efficient power usage for the given VM.
     *
     * @param vm the VM
     * @param excludedHosts the excluded hosts
     * @return the host found to host the VM
     */
    public PowerHost findHostForVm(Vm vm, Set<? extends Host> excludedHosts) {
        double minPower = Double.MAX_VALUE;
        PowerHost allocatedHost = null;
        for (PowerHost host : this.<PowerHost> getHostList()) {
            if (excludedHosts.contains(host)) {
                continue;
            }
            if (host.isSuitableForVm(vm)) {
                if (getUtilizationOfCpuMips(host) != 0 && isHostOverUtilizedAfterAllocation(host, vm)) {
                    continue;
                }
                try {
                    double powerAfterAllocation = getPowerAfterAllocation(host, vm);
                    if (powerAfterAllocation != -1) {
                        double powerDiff = powerAfterAllocation - host.getPower();
                        if (powerDiff < minPower) {
                            minPower = powerDiff;
                            allocatedHost = host;
                        }
                    }
                } catch (Exception ignored) {

                }
            }
        }
        return allocatedHost;
    }

    /**
     * Checks if a host will be over utilized after placing of a candidate VM.
     *
     * @param host the host to verify
     * @param vm the candidate vm
     * @return true, if the host will be over utilized after VM placement; false otherwise
     */
    protected boolean isHostOverUtilizedAfterAllocation(PowerHost host, Vm vm) {
        boolean isHostOverUtilizedAfterAllocation = true;
        if (host.vmCreate(vm)) {
            isHostOverUtilizedAfterAllocation = getVmAllocationPolicy().isHostOverUtilized(host);
            host.vmDestroy(vm);
        }
        return isHostOverUtilizedAfterAllocation;
    }

    @Override
    public PowerHost findHostForVm(Vm vm) {
        Set<Host> excludedHosts = new HashSet<Host>();
        if (vm.getHost() != null) {
            excludedHosts.add(vm.getHost());
        }
        return findHostForVm(vm, excludedHosts);
    }

    /**
     * Gets a new vm placement considering the list of VM to migrate.
     *
     * @param vmsToMigrate the list of VMs to migrate
     * @param excludedHosts the list of hosts that aren't selected as destination hosts
     * @return the new vm placement map
     */
    protected List<Map<String, Object>> getNewVmPlacement(List<? extends Vm> vmsToMigrate, Set<? extends Host> excludedHosts) {
        List<Map<String, Object>> migrationMap = new LinkedList<>();
        PowerVmList.sortByCpuUtilization(vmsToMigrate);
        for (Vm vm : vmsToMigrate) {
            PowerHost allocatedHost = findHostForVm(vm, excludedHosts);
            if (allocatedHost != null) {
                allocatedHost.vmCreate(vm);
                Log.printConcatLine("VM #", vm.getId(), " allocated to host #", allocatedHost.getId());
                Map<String, Object> migrate = new HashMap<>();
                migrate.put("vm", vm);
                migrate.put("host", allocatedHost);
                migrationMap.add(migrate);
            }
        }
        return migrationMap;
    }

    /**
     * Gets the new vm placement from under utilized host.
     *
     * @param vmsToMigrate the list of VMs to migrate
     * @param excludedHosts the list of hosts that aren't selected as destination hosts
     * @return the new vm placement from under utilized host
     */
    protected List<Map<String, Object>> getNewVmPlacementFromUnderUtilizedHost(
            List<? extends Vm> vmsToMigrate,
            Set<? extends Host> excludedHosts) {
        List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
        PowerVmList.sortByCpuUtilization(vmsToMigrate);
        for (Vm vm : vmsToMigrate) {
            PowerHost allocatedHost = findHostForVm(vm, excludedHosts);
            if (allocatedHost != null) {
                allocatedHost.vmCreate(vm);
                Log.printConcatLine("VM #", vm.getId(), " allocated to host #", allocatedHost.getId());

                Map<String, Object> migrate = new HashMap<String, Object>();
                migrate.put("vm", vm);
                migrate.put("host", allocatedHost);
                migrationMap.add(migrate);
            } else {
                Log.printLine("Not all VMs can be reallocated from the host, reallocation cancelled");
                for (Map<String, Object> map : migrationMap) {
                    ((Host) map.get("host")).vmDestroy((Vm) map.get("vm"));
                }
                migrationMap.clear();
                break;
            }
        }
        return migrationMap;
    }

    /**
     * Gets the VMs to migrate from hosts.
     *
     * @param overUtilizedHosts the over utilized hosts
     * @return the VMs to migrate from hosts
     */
    protected List<? extends Vm> getVmsToMigrateFromHosts(List<PowerHostUtilizationHistory> overUtilizedHosts) {
        List<Vm> vmsToMigrate = new LinkedList<>();
        for (PowerHostUtilizationHistory host : overUtilizedHosts) {
            while (true) {
                Vm vm = getVmSelectionPolicy().getVmToMigrate(host);
                if (vm == null) {
                    break;
                }
                vmsToMigrate.add(vm);
                host.vmDestroy(vm);
                if (!getVmAllocationPolicy().isHostOverUtilized(host)) {
                    break;
                }
            }
        }
        return vmsToMigrate;
    }

    /**
     * Gets the VMs to migrate from under utilized host.
     *
     * @param host the host
     * @return the vms to migrate from under utilized host
     */
    protected List<? extends Vm> getVmsToMigrateFromUnderUtilizedHost(PowerHost host) {
        List<Vm> vmsToMigrate = new LinkedList<Vm>();
        for (Vm vm : host.getVmList()) {
            if (!vm.isInMigration()) {
                vmsToMigrate.add(vm);
            }
        }
        return vmsToMigrate;
    }

    /**
     * Gets the over utilized hosts.
     *
     * @return the over utilized hosts
     */
    protected List<PowerHostUtilizationHistory> getOverUtilizedHosts() {
        List<PowerHostUtilizationHistory> overUtilizedHosts = new LinkedList<>();
        for (PowerHostUtilizationHistory host : this.<PowerHostUtilizationHistory> getHostList()) {
            if (getVmAllocationPolicy().isHostOverUtilized(host)) {
                overUtilizedHosts.add(host);
            }
        }
        return overUtilizedHosts;
    }

    /**
     * Gets the switched off hosts.
     *
     * @return the switched off hosts
     */
    protected List<PowerHostUtilizationHistory> getSwitchedOffHosts() {
        List<PowerHostUtilizationHistory> switchedOffHosts = new LinkedList<>();
        for (PowerHostUtilizationHistory host : this.<PowerHostUtilizationHistory> getHostList()) {
            if (host.getUtilizationOfCpu() == 0) {
                switchedOffHosts.add(host);
            }
        }
        return switchedOffHosts;
    }

    /**
     * Check if host is switched off.
     *
     * @param host host
     * @return
     */
    private boolean isSwitchedOffHost(PowerHostUtilizationHistory host) {
        return host.getUtilizationOfCpu() == 0.0;
    }

    /**
     * Updates the list of maps between a VM and the host where it is place.
     */
    protected void saveAllocation() {
        getSavedAllocation().clear();
        for (Host host : getHostList()) {
            for (Vm vm : host.getVmList()) {
                if (host.getVmsMigratingIn().contains(vm)) {
                    continue;
                }
                Map<String, Object> map = new HashMap<>();
                map.put("host", host);
                map.put("vm", vm);
                getSavedAllocation().add(map);
            }
        }
    }

    /**
     * Restore VM allocation from the allocation history.
     */
    protected void restoreAllocation() {
        for (Host host : getHostList()) {
            host.vmDestroyAll();
            host.reallocateMigratingInVms();
        }
        for (Map<String, Object> map : getSavedAllocation()) {
            Vm vm = (Vm) map.get("vm");
            PowerHost host = (PowerHost) map.get("host");
            if (!host.vmCreate(vm)) {
                Log.printConcatLine("Couldn't restore VM #", vm.getId(), " on host #", host.getId());
                System.exit(0);
            }
            getVmTable().put(vm.getUid(), host);
        }
    }

    /**
     * Gets the power consumption of a host after placement of a candidate VM.
     * The VM is not in fact placed at the host.
     *
     * @param host the host
     * @param vm the candidate vm
     * @return the power after allocation
     */
    protected double getPowerAfterAllocation(PowerHost host, Vm vm) {
        double power = 0;
        try {
            power = host.getPowerModel().getPower(getMaxUtilizationAfterAllocation(host, vm));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        return power;
    }

    /**
     * Gets the max power consumption of a host after placement of a candidate VM.
     * The VM is not in fact placed at the host. Load is balanced between PEs.
     * Restriction is: VM's max MIPS < PE's MIPS.
     *
     * @param host the host
     * @param vm the vm
     * @return the power after allocation
     */
    protected double getMaxUtilizationAfterAllocation(PowerHost host, Vm vm) {
        double requestedTotalMips = vm.getCurrentRequestedTotalMips();
        double hostUtilizationMips = getUtilizationOfCpuMips(host);
        double hostPotentialUtilizationMips = hostUtilizationMips + requestedTotalMips;
        double pePotentialUtilization = hostPotentialUtilizationMips / host.getTotalMips();
        return pePotentialUtilization;
    }

    /**
     * Gets the utilization of the CPU in MIPS for the current potentially allocated VMs.
     *
     * @param host the host
     * @return the utilization of the CPU in MIPS
     */
    protected double getUtilizationOfCpuMips(PowerHost host) {
        double hostUtilizationMips = 0;
        for (Vm vm2 : host.getVmList()) {
            if (host.getVmsMigratingIn().contains(vm2)) {
                // calculate additional potential CPU usage of a migrating in VM
                hostUtilizationMips += host.getTotalAllocatedMipsForVm(vm2) * 0.9 / 0.1;
            }
            hostUtilizationMips += host.getTotalAllocatedMipsForVm(vm2);
        }
        return hostUtilizationMips;
    }

    /**
     * Gets the saved allocation.
     *
     * @return the saved allocation
     */
    protected List<Map<String, Object>> getSavedAllocation() {
        return savedAllocation;
    }

    /**
     * Sets the vm selection policy.
     *
     * @param vmSelectionPolicy the new vm selection policy
     */
    protected void setVmSelectionPolicy(PowerVmSelectionPolicy vmSelectionPolicy) {
        this.vmSelectionPolicy = vmSelectionPolicy;
    }

    /**
     * Gets the vm selection policy.
     *
     * @return the vm selection policy
     */
    protected PowerVmSelectionPolicy getVmSelectionPolicy() {
        return vmSelectionPolicy;
    }

    /**
     * Sets the vm allocation policy.
     *
     * @param vmAllocationPolicy the new vm allocation policy
     */
    public void setVmAllocationPolicy(PowerVmAllocationPolicyMigrationLocalRegression vmAllocationPolicy) {
        this.vmAllocationPolicy = vmAllocationPolicy;
    }

    /**
     * Gets the vm allocation policy.
     *
     * @return the vm allocation policy
     */
    public PowerVmAllocationPolicyMigrationLocalRegression getVmAllocationPolicy() {
        return vmAllocationPolicy;
    }

}

