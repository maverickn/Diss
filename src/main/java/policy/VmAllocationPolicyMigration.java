package policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.*;
import org.cloudbus.cloudsim.power.lists.PowerVmList;
import org.cloudbus.cloudsim.util.ExecutionTimeMeasurer;
import org.cloudbus.cloudsim.util.MathUtil;

public abstract class VmAllocationPolicyMigration extends PowerVmAllocationPolicyAbstract {

    /**
     * The vm selection policy.
     */
    private PowerVmSelectionPolicy vmSelectionPolicy;

    /**
     * A list of maps between a VM and the host where it is place.
     */
    private final List<Map<String, Object>> savedAllocation = new ArrayList<>();

    /**
     * A map of CPU utilization history (in percentage) for each host,
     * where each key is a host id and each value is the CPU utilization percentage history.
     */
    private final Map<Integer, List<Double>> utilizationHistory = new HashMap<>();

    /**
     * A map of CPU utilization history (in percentage) for each host,
     * where each key is a host id and each value is the CPU utilization percentage history.
     */
    private final Map<Integer, List<Double>> hostsUtilizationHistory = new HashMap<>();

    /**
     * The metric history.
     */
    private final Map<Integer, List<Double>> metricHistory = new HashMap<>();

    /**
     * The time when entries in each history list was added. All history lists are updated at the same time.
     */
    private final Map<Integer, List<Double>> timeHistory = new HashMap<>();

    /**
     * The history of time spent in VM selection every time the optimization of VM allocation method is called.
     */
    private final List<Double> executionTimeHistoryVmSelection = new LinkedList<>();

    /**
     * The history of time spent in host selection every time the optimization of VM allocation method is called.
     */
    private final List<Double> executionTimeHistoryHostSelection = new LinkedList<>();

    /**
     * The history of time spent in VM reallocation every time the optimization of VM allocation method is called.
     */
    private final List<Double> executionTimeHistoryVmReallocation = new LinkedList<>();

    /**
     * The history of total time spent in every call of the optimization of VM allocation method.
     */
    private final List<Double> executionTimeHistoryTotal = new LinkedList<>();

    /**
     * Instantiates a new VmAllocationPolicyMigration.
     *
     * @param hostList the host list
     * @param vmSelectionPolicy the vm selection policy
     */
    public VmAllocationPolicyMigration(List<? extends Host> hostList, PowerVmSelectionPolicy vmSelectionPolicy) {
        super(hostList);
        setVmSelectionPolicy(vmSelectionPolicy);
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
        ExecutionTimeMeasurer.start("optimizeAllocationTotal");

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

            ExecutionTimeMeasurer.start("optimizeAllocationVmSelection");
            List<? extends Vm> vmsToMigrate = getAllVmsToMigrateFromHost(hostToChangePowerMode);
            getExecutionTimeHistoryVmSelection().add(ExecutionTimeMeasurer.end("optimizeAllocationVmSelection"));

            hibernateHosts.add(hostToChangePowerMode);

            Log.printLine("Reallocation of VMs from the powered off host:");
            ExecutionTimeMeasurer.start("optimizeAllocationVmReallocation");
            List<Map<String, Object>> migrationMap = getNewVmPlacement(vmsToMigrate, new HashSet<Host>(hibernateHosts));
            getExecutionTimeHistoryVmReallocation().add(ExecutionTimeMeasurer.end("optimizeAllocationVmReallocation"));
            Log.printLine();

            restoreAllocation();

            System.out.println("Finish to poweroff host");

            getExecutionTimeHistoryTotal().add(ExecutionTimeMeasurer.end("optimizeAllocationTotal"));

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

            ExecutionTimeMeasurer.start("optimizeAllocationHostSelection");
            List<PowerHostUtilizationHistory> overUtilizedHosts = getOverUtilizedHosts();
            getExecutionTimeHistoryHostSelection().add(ExecutionTimeMeasurer.end("optimizeAllocationHostSelection"));

            printOverUtilizedHosts(overUtilizedHosts);

            saveAllocation();

            ExecutionTimeMeasurer.start("optimizeAllocationVmSelection");
            List<? extends Vm> vmsToMigrate = getVmsToMigrateFromHosts(overUtilizedHosts);
            getExecutionTimeHistoryVmSelection().add(ExecutionTimeMeasurer.end("optimizeAllocationVmSelection"));

            Log.printLine("Reallocation of VMs from the over-utilized hosts:");
            excludedHosts.addAll(overUtilizedHosts);
            ExecutionTimeMeasurer.start("optimizeAllocationVmReallocation");
            List<Map<String, Object>> migrationMap = getNewVmPlacement(vmsToMigrate, new HashSet<Host>(excludedHosts));
            getExecutionTimeHistoryVmReallocation().add(ExecutionTimeMeasurer.end("optimizeAllocationVmReallocation"));
            Log.printLine();

            restoreAllocation();

            System.out.println("Finish to poweron host");

            getExecutionTimeHistoryTotal().add(ExecutionTimeMeasurer.end("optimizeAllocationTotal"));

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
            isHostOverUtilizedAfterAllocation = isHostOverUtilized(host);
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

    /*
     * Extracts the host list from a migration map.
     *
     * @param migrationMap the migration map
     * @return the list
     */
    /*protected List<PowerHost> extractHostListFromMigrationMap(List<Map<String, Object>> migrationMap) {
        List<PowerHost> hosts = new LinkedList<PowerHost>();
        for (Map<String, Object> map : migrationMap) {
            hosts.add((PowerHost) map.get("host"));
        }
        return hosts;
    }*/

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

    /*
     * Gets the new vm placement from under utilized host.
     *
     * @param vmsToMigrate the list of VMs to migrate
     * @param excludedHosts the list of hosts that aren't selected as destination hosts
     * @return the new vm placement from under utilized host
     */
    /*protected List<Map<String, Object>> getNewVmPlacementFromUnderUtilizedHost(
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
    }*/

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
                if (!isHostOverUtilized(host)) {
                    break;
                }
            }
        }
        return vmsToMigrate;
    }

    /**
     * Gets all VMs to migrate from hosts.
     *
     * @param host target host
     * @return the VMs to migrate from hosts
     */
    protected List<? extends Vm> getAllVmsToMigrateFromHost(PowerHostUtilizationHistory host) {
        List<Vm> vmsToMigrate = new LinkedList<>();
        vmsToMigrate.addAll(host.getVmList());
        host.vmDestroyAll();
        return vmsToMigrate;
    }

    /*
     * Gets the VMs to migrate from under utilized host.
     *
     * @param host the host
     * @return the vms to migrate from under utilized host
     */
    /*protected List<? extends Vm> getVmsToMigrateFromUnderUtilizedHost(PowerHost host) {
        List<Vm> vmsToMigrate = new LinkedList<Vm>();
        for (Vm vm : host.getVmList()) {
            if (!vm.isInMigration()) {
                vmsToMigrate.add(vm);
            }
        }
        return vmsToMigrate;
    }*/

    /**
     * Gets the over utilized hosts.
     *
     * @return the over utilized hosts
     */
    protected List<PowerHostUtilizationHistory> getOverUtilizedHosts() {
        List<PowerHostUtilizationHistory> overUtilizedHosts = new LinkedList<>();
        for (PowerHostUtilizationHistory host : this.<PowerHostUtilizationHistory> getHostList()) {
            if (isHostOverUtilized(host)) {
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

    /*
     * Gets the most under utilized host.
     *
     * @param excludedHosts the excluded hosts
     * @return the most under utilized host
     */
    /*protected PowerHost getUnderUtilizedHost(Set<? extends Host> excludedHosts) {
        double minUtilization = 1;
        PowerHost underUtilizedHost = null;
        for (PowerHost host : this.<PowerHost> getHostList()) {
            if (excludedHosts.contains(host)) {
                continue;
            }
            double utilization = host.getUtilizationOfCpu();
            if (utilization > 0 && utilization < minUtilization
                    && !areAllVmsMigratingOutOrAnyVmMigratingIn(host)) {
                minUtilization = utilization;
                underUtilizedHost = host;
            }
        }
        return underUtilizedHost;
    }*/

    /*
     * Checks whether all VMs of a given host are in migration.
     *
     * @param host the host
     * @return true, if successful
     */
    /*protected boolean areAllVmsMigratingOutOrAnyVmMigratingIn(PowerHost host) {
        for (PowerVm vm : host.<PowerVm> getVmList()) {
            if (!vm.isInMigration()) {
                return false;
            }
            if (host.getVmsMigratingIn().contains(vm)) {
                return true;
            }
        }
        return true;
    }*/

    /**
     * Checks if host is over utilized (static threshold).
     *
     * @param host the host
     * @return true, if the host is over utilized; false otherwise
     */
    protected boolean isHostOverUtilizedStaticThreshold(PowerHost host) {
        addHistoryEntry(host, 0.9);
        double totalRequestedMips = 0;
        for (Vm vm : host.getVmList()) {
            totalRequestedMips += vm.getCurrentRequestedTotalMips();
        }
        double utilization = totalRequestedMips / host.getTotalMips();
        return utilization > 0.9;
    }

    /**
     * Checks if host is over utilized.
     *
     * @param host the host
     * @return true, if the host is over utilized; false otherwise
     */
    protected boolean isHostOverUtilized(PowerHost host) {
        for (PowerHost _host : this.<PowerHost> getHostList()) {
            if (!getHostsUtilizationHistory().containsKey(_host.getId())) {
                getHostsUtilizationHistory().put(_host.getId(), new LinkedList<>());
            }
            getHostsUtilizationHistory().get(_host.getId()).add(_host.getUtilizationOfCpu());
        }

        List<Double> utilizationHistoryList = new ArrayList<>();
        utilizationHistoryList.addAll(getHostsUtilizationHistory().get(host.getId()));

        double[] utilizationHistory = MathUtil.listToArray(utilizationHistoryList);
        int length = 10; // we use 10 to make the regression responsive enough to latest values
        if (utilizationHistory.length < length) {
            return isHostOverUtilizedStaticThreshold(host);
        }
        double[] utilizationHistoryReversed = new double[length];
        for (int i = 0; i < length; i++) {
            utilizationHistoryReversed[i] = utilizationHistory[length - i - 1];
        }
        double[] estimates = null;
        try {
            estimates = MathUtil.getLoessParameterEstimates(utilizationHistoryReversed);
        } catch (IllegalArgumentException e) {
            return isHostOverUtilizedStaticThreshold(host);
        }
        double migrationIntervals = Math.ceil(getMaximumVmMigrationTime(host) / 10);
        double predictedUtilization = estimates[0] + estimates[1] * (length + migrationIntervals);
        predictedUtilization *= 1.2;

        addHistoryEntry(host, predictedUtilization);

        return predictedUtilization >= 1;
    }

    /**
     * Gets the maximum vm migration time.
     *
     * @param host the host
     * @return the maximum vm migration time
     */
    protected double getMaximumVmMigrationTime(PowerHost host) {
        int maxRam = Integer.MIN_VALUE;
        for (Vm vm : host.getVmList()) {
            int ram = vm.getRam();
            if (ram > maxRam) {
                maxRam = ram;
            }
        }
        return maxRam / ((double) host.getBw() / (2 * 8000));
    }

    /**
     * Adds an entry for each history map of a host.
     *
     * @param host the host to add metric history entries
     * @param metric the metric to be added to the metric history map
     */
    protected void addHistoryEntry(HostDynamicWorkload host, double metric) {
        int hostId = host.getId();
        if (!getTimeHistory().containsKey(hostId)) {
            getTimeHistory().put(hostId, new LinkedList<>());
        }
        if (!getUtilizationHistory().containsKey(hostId)) {
            getUtilizationHistory().put(hostId, new LinkedList<>());
        }
        if (!getMetricHistory().containsKey(hostId)) {
            getMetricHistory().put(hostId, new LinkedList<>());
        }
        if (!getTimeHistory().get(hostId).contains(CloudSim.clock())) {
            getTimeHistory().get(hostId).add(CloudSim.clock());
            getUtilizationHistory().get(hostId).add(host.getUtilizationOfCpu());
            getMetricHistory().get(hostId).add(metric);
        }
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
     * Gets the utilization history.
     *
     * @return the utilization history
     */
    public Map<Integer, List<Double>> getUtilizationHistory() {
        return utilizationHistory;
    }

    /**
     * Gets the metric history.
     *
     * @return the metric history
     */
    public Map<Integer, List<Double>> getMetricHistory() {
        return metricHistory;
    }

    /**
     * Gets the time history.
     *
     * @return the time history
     */
    public Map<Integer, List<Double>> getTimeHistory() {
        return timeHistory;
    }

    /**
     * Gets the execution time history vm selection.
     *
     * @return the execution time history vm selection
     */
    public List<Double> getExecutionTimeHistoryVmSelection() {
        return executionTimeHistoryVmSelection;
    }

    /**
     * Gets the execution time history host selection.
     *
     * @return the execution time history host selection
     */
    public List<Double> getExecutionTimeHistoryHostSelection() {
        return executionTimeHistoryHostSelection;
    }

    /**
     * Gets the execution time history vm reallocation.
     *
     * @return the execution time history vm reallocation
     */
    public List<Double> getExecutionTimeHistoryVmReallocation() {
        return executionTimeHistoryVmReallocation;
    }

    /**
     * Gets the execution time history total.
     *
     * @return the execution time history total
     */
    public List<Double> getExecutionTimeHistoryTotal() {
        return executionTimeHistoryTotal;
    }

    /**
     * Gets the utilization history for each host
     *
     * @return utilization history for each host
     */
    public Map<Integer, List<Double>> getHostsUtilizationHistory() {
        return hostsUtilizationHistory;
    }
}

