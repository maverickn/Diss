import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.HostDynamicWorkload;
import org.cloudbus.cloudsim.HostStateHistoryEntry;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicyMigrationAbstract;
import org.cloudbus.cloudsim.power.PowerVmSelectionPolicy;

import java.util.*;

public class Agent extends PowerVmAllocationPolicyMigrationAbstract{

    private double learningRate;

    private double discountFactor;

    private double cofImportanceSla;

    private double cofImportancePower;

    private PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy;

    private final List<Integer> statesList = new LinkedList<>();

    private final List<Boolean> actionsList = new ArrayList<>();

    private List<List<Double>> qTable = new ArrayList<>();

    private final List<Double> slaViolationTimeList = new LinkedList<>();

    private final List<Double> powerConsumptionList = new LinkedList<>();

    public Agent(double learningRate, double discountFactor, double cofImportanceSla, double cofImportancePower,
                 PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy,
                 PowerVmSelectionPolicy vmSelectionPolicy, List<? extends Host> hostList) {
        super(hostList, vmSelectionPolicy);

        setLearningRate(learningRate);
        setDiscountFactor(discountFactor);
        setCofImportanceSla(cofImportanceSla);
        setCofImportancePower(cofImportancePower);

        setFallbackVmAllocationPolicy(fallbackVmAllocationPolicy);
        setVmSelectionPolicy(vmSelectionPolicy);
        setActionsList();
    }

    public Agent(PowerVmSelectionPolicy vmSelectionPolicy, List<? extends Host> hostList) {
        super(hostList, vmSelectionPolicy);
        setActionsList();
    }

    /**
     * Get cpu utilization from hosts, convert utilization values to intervals
     * @return hashcode of hosts state
     */
    private int getState() {
        StringBuilder convertedCpuUtilizationList = new StringBuilder();
        double cpuUtil;
        for (PowerHost host : this.<PowerHost> getHostList()) {
            cpuUtil = host.getUtilizationOfCpu();
            if (cpuUtil >= 0 || cpuUtil < 0.3) {
                convertedCpuUtilizationList.append("1");
            } else if (cpuUtil >= 0.3 || cpuUtil < 0.8) {
                convertedCpuUtilizationList.append("2");
            } else if (cpuUtil >= 0.8 || cpuUtil <= 1) {
                convertedCpuUtilizationList.append("3");
            }
        }
        return convertedCpuUtilizationList.hashCode();
    }

    /**
     * Best action choice policy
     * @param state state
     * @return index of action with min Q-value
     */
    private int getActionPolicy(int state) {
        int index = getStatesList().indexOf(state);
        double minQvalue = Collections.min(getQTable().get(index));
        return getQTable().get(index).indexOf(minQvalue);
    }

    /**
     * Save state in statesList, add row to qTable with Double.MAX_VALUE
     * @param state state
     * @return index of state in statesList
     */
    private int saveState(int state) {
        getSlaViolationTime();
        getPower();
        int actionsCount = getActionsList().size();
        if (getStatesList().isEmpty()) {
            getStatesList().add(state);
            getQTable().add(new ArrayList<>());
            for (int i = 0; i < actionsCount; i++) {
                getQTable().get(0).add(Double.MAX_VALUE);
            }
            return 0;
        } else if (getStatesList().contains(state)) {
            return getStatesList().indexOf(state);
        }
        else {
            getStatesList().add(state);
            int index = getStatesList().indexOf(state);
            getQTable().add(new ArrayList<>());
            for (int i = 0; i < actionsCount; i++) {
                getQTable().get(index).add(Double.MAX_VALUE);
            }
            return index;
        }
    }

    /**
     * Get total SLA violation time per each active host
     */
    private void getSlaViolationTime() {
        double slaViolationTimePerHost = 0;
        for (HostDynamicWorkload host : this.<HostDynamicWorkload> getHostList()) {
            double previousTime = -1;
            double previousAllocated = 0;
            double previousRequested = 0;
            boolean previousIsActive = true;
            for (HostStateHistoryEntry entry : host.getStateHistory()) {
                if (previousTime != -1 && previousIsActive) {
                    double timeDiff = entry.getTime() - previousTime;
                    if (previousAllocated < previousRequested) {
                        slaViolationTimePerHost += timeDiff;
                    }
                }
                previousAllocated = entry.getAllocatedMips();
                previousRequested = entry.getRequestedMips();
                previousTime = entry.getTime();
                previousIsActive = entry.isActive();
            }
        }
        getSlaViolationTimeList().add(slaViolationTimePerHost);
    }

    /**
     * Get total power per each host
     */
    private void getPower() {
        double totalPower = 0;
        for (PowerHost host : this.<PowerHost> getHostList()) {
            totalPower += host.getPower();
        }
        getPowerConsumptionList().add(totalPower);
    }

    /**
     * Get SLA violation penalty
     * @return SLA violation penalty
     */
    private double getSlaViolationPenalty() {
        List<Double> slaViolationTimeList = getSlaViolationTimeList();
        Collections.reverse(slaViolationTimeList);
        if (slaViolationTimeList.size() < 3) {
            return Double.MAX_VALUE;
        } else {
            return ((slaViolationTimeList.get(0) - slaViolationTimeList.get(1))/
                    (slaViolationTimeList.get(1) - slaViolationTimeList.get(2)));
        }
    }

    /**
     * Get power consumption penalty
     * @return Power consumption penalty
     */
    private double getPowerConsumptionPenalty() {
        List<Double> powerList = getPowerConsumptionList();
        Collections.reverse(powerList);
        if (powerList.size() < 3) {
            return Double.MAX_VALUE;
        } else {
            return ((powerList.get(0) - powerList.get(1))/(powerList.get(1) - powerList.get(2)));
        }
    }

    private double getPenalty() {
        return (cofImportanceSla * getSlaViolationPenalty() + cofImportancePower * getPowerConsumptionPenalty());
    }

    public void setLearningRate(double learningRate) {
        if (learningRate < 0 || learningRate > 1) {
            throw new IllegalArgumentException("Learning rate must be between 0 and 1");
        } else {
            this.learningRate = learningRate;
        }
    }

    public void setDiscountFactor(double discountFactor) {
        if (discountFactor < 0 || discountFactor > 1) {
            throw new IllegalArgumentException("Discount factor must be between 0 and 1");
        } else {
            this.discountFactor = discountFactor;
        }
    }

    public void setCofImportanceSla(double cofImportanceSla) {
        if (cofImportanceSla < 0 || cofImportanceSla > 1) {
            throw new IllegalArgumentException("Coefficient of SLA importance must be between 0 and 1");
        } else {
            this.cofImportanceSla = cofImportanceSla;
        }
    }

    public void setCofImportancePower(double cofImportancePower) {
        if (cofImportancePower < 0 || cofImportancePower > 1) {
            throw new IllegalArgumentException("Coefficient of power importance must be between 0 and 1");
        } else {
            this.cofImportancePower = cofImportancePower;
        }
    }

    @Override
    protected boolean isHostOverUtilized(PowerHost powerHost) {
        return false;
    }

    public void setFallbackVmAllocationPolicy(PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy) {
        this.fallbackVmAllocationPolicy = fallbackVmAllocationPolicy;
    }

    public PowerVmAllocationPolicyMigrationAbstract getFallbackVmAllocationPolicy() {
        return fallbackVmAllocationPolicy;
    }

    public List<Integer> getStatesList() {
        return statesList;
    }

    public void setActionsList() {
        int size = this.getHostList().size();
        for (int i = 0; i < size; i++) {
            getActionsList().add(true);
            getActionsList().add(false);
        }
    }

    public List<Boolean> getActionsList() {
        return actionsList;
    }

    public List<List<Double>> getQTable() {
        return qTable;
    }

    public List<Double> getSlaViolationTimeList() {
        return slaViolationTimeList;
    }

    public List<Double> getPowerConsumptionList() {
        return powerConsumptionList;
    }
}
