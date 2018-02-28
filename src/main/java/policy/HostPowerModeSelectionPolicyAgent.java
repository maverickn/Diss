package policy;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.HostDynamicWorkload;
import org.cloudbus.cloudsim.HostStateHistoryEntry;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVmSelectionPolicy;

import java.util.*;

public class HostPowerModeSelectionPolicyAgent extends VmAllocationPolicyMigrationAbstract {

    private double learningRate;

    private double discountFactor;

    private double cofImportanceSla;

    private double cofImportancePower;

    private final List<Integer> statesList = new ArrayList<>();

    private List<Boolean> actionsList = new ArrayList<>();

    private List<List<Double>> qTable = new ArrayList<>();

    private final List<Double> slaViolationTimeList = new ArrayList<>();

    private final List<Double> powerConsumptionList = new ArrayList<>();

    private Double oldQValue = 0.0;

    private int counter = 0;

    public HostPowerModeSelectionPolicyAgent(double learningRate, double discountFactor, double cofImportanceSla, double cofImportancePower,
                                             PowerVmSelectionPolicy vmSelectionPolicy, List<? extends Host> hostList) {
        super(hostList, vmSelectionPolicy);
        setLearningRate(learningRate);
        setDiscountFactor(discountFactor);
        setCofImportanceSla(cofImportanceSla);
        setCofImportancePower(cofImportancePower);
    }

    /**
     * Get host id and power mode
     * @return
     */
    public Map<Integer, Boolean> getHostPowerMode() {
        Map<Integer, Boolean> idAndHostPowerMode = new HashMap<>();
        Random rand = new Random();
        int probality = (rand.nextInt(10) + 1);

        int state = observeState();
        System.out.println("state: " + state);

        int stateIndex = saveState(state);
        System.out.println("stateIndex: " + stateIndex);

        double penalty = getPenalty();
        System.out.println("penalty: " + penalty);

        double minQValue = Collections.min(getQTable().get(stateIndex));
        System.out.println("minQValue: " + minQValue);

        for (int i = 0; i < qTable.size(); i++) {
            for (int j = 0; j < qTable.get(0).size(); j++) {
                if (qTable.get(i).get(j) == Double.MAX_VALUE) {
                    System.out.print("-\t");
                } else {
                    System.out.print(qTable.get(i).get(j) + "\t");
                }
            }
            System.out.print("\n");
        }

        System.out.println("getSlaViolationTimeList:");
        for (int i = 0; i < getSlaViolationTimeList().size(); i++) {
            System.out.println(i + "\t" + getSlaViolationTimeList().get(i));
        }

        System.out.println("getPowerConsumptionList:");
        for (int i = 0; i < getPowerConsumptionList().size(); i++) {
            System.out.println(i + "\t" + getPowerConsumptionList().get(i));
        }

        int actionIndex;
        if (minQValue == Double.MAX_VALUE || probality == 1) {
            actionIndex = rand.nextInt(getActionsList().size());
        } else {
            actionIndex = getQTable().get(stateIndex).indexOf(minQValue);
        }
        System.out.println("actionIndex: " + actionIndex);

        double newQvalue = getNewQValue(penalty, minQValue);
        System.out.println("newQvalue: " + newQvalue);

        getQTable().get(stateIndex).set(actionIndex, newQvalue);
        for (int i = 0; i < qTable.size(); i++) {
            for (int j = 0; j < qTable.get(0).size(); j++) {
                if (qTable.get(i).get(j) == Double.MAX_VALUE) {
                    System.out.print("-\t");
                } else {
                    System.out.print(qTable.get(i).get(j) + "\t");
                }
            }
            System.out.print("\n");
        }

        oldQValue = getQTable().get(stateIndex).get(actionIndex);

        boolean powerMode = getActionsList().get(actionIndex);
        System.out.println("powerMode: " + powerMode);

        idAndHostPowerMode.put(actionIndex, powerMode);
        System.out.println("idAndHostPowerMode: " + idAndHostPowerMode);

        getActionsList().set(actionIndex, !powerMode);
        for (int i = 0; i < getActionsList().size(); i++) {
            System.out.println(i + "\t" + getActionsList().get(i));
        }

        if (counter == 4) {
            System.exit(0);
        } else {
            counter ++;
        }

        return idAndHostPowerMode;
    }

    /**
     * Observe state (get cpu utilization from hosts, convert utilization values to intervals)
     * @return hashcode of hosts state
     */
    private int observeState() {
        StringBuilder convertedCpuUtilizationList = new StringBuilder();
        double cpuUtil;
        for (PowerHost host : this.<PowerHost> getHostList()) {
            // TODO: 28.02.2018 get sum of each resource utilization by each host (CPU, RAM, Bw, Storage) and convert to intervals
            cpuUtil = host.getUtilizationOfCpu();
            if (cpuUtil >= 0 || cpuUtil < 0.3) {
                convertedCpuUtilizationList.append("1");
            } else if (cpuUtil >= 0.3 || cpuUtil < 0.8) {
                convertedCpuUtilizationList.append("2");
            } else if (cpuUtil >= 0.8 || cpuUtil <= 1) {
                convertedCpuUtilizationList.append("3");
            }
        }
        getSlaViolationTime();
        getTotalPower();
        return convertedCpuUtilizationList.hashCode();
    }

    /**
     * Save state in statesList, add row to qTable with Double.MAX_VALUE
     * @param state state
     * @return index of state in statesList
     */
    private int saveState(int state) {
        if (getStatesList().isEmpty()) {
            setActionsList();
            getStatesList().add(state);
            getQTable().add(new ArrayList<>());
            for (int i = 0; i < getActionsList().size(); i++) {
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
            for (int i = 0; i < getActionsList().size(); i++) {
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
        if (getSlaViolationTimeList().isEmpty()) {
            getSlaViolationTimeList().add(0.0);
        } else {
            getSlaViolationTimeList().add(slaViolationTimePerHost);
        }
    }

    /**
     * Get total power per each host
     */
    private void getTotalPower() {
        double totalPower = 0;
        for (PowerHost host : this.<PowerHost> getHostList()) {
            totalPower += host.getPower();
        }
        if (getPowerConsumptionList().isEmpty()) {
            getPowerConsumptionList().add(0.0);
        } else {
            totalPower += getPowerConsumptionList().get(getPowerConsumptionList().size() - 1);
            getPowerConsumptionList().add(totalPower);
        }
    }

    /**
     * Get SLA violation penalty or power consumption penalty
     * @param list list with SLA violation time or power consumption
     * @return penalty
     */
    private double getPartPenalty(List<Double> list) {
        int size = list.size();
        if (size < 3) {
            // TODO: 26.02.2018 what to return
            return Double.MAX_VALUE;
        } else {
            return ((list.get(size - 1) - list.get(size - 2))/(list.get(size - 2) - list.get(size - 3)));
        }
    }

    /**
     * Get total penalty (SLA violation penalty + power consumption penalty)
     * @return total penalty
     */
    private double getPenalty() {
        return (cofImportanceSla * getPartPenalty(getSlaViolationTimeList()) + cofImportancePower * getPartPenalty(getPowerConsumptionList()));
    }

    /**
     * Get new Q-value
     * @param penalty total penalty
     * @param estimateOptimalFutureValue estimate of optimal future value
     * @return new Q-value
     */
    private double getNewQValue(double penalty, double estimateOptimalFutureValue) {
        return ((1 - learningRate) * oldQValue + learningRate * (penalty + discountFactor * estimateOptimalFutureValue));
    }

    private boolean isSwitchedOffHost(PowerHost host) {
        return host.getUtilizationOfCpu() == 0;
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

    public List<Integer> getStatesList() {
        return statesList;
    }

    private void setActionsList() {
        int size = this.<PowerHost>getHostList().size();
        for (int i = 0; i < size; i++) {
            getActionsList().add(true);
        }
        for (PowerHost host: this.<PowerHost>getHostList()) {
            getActionsList().set(host.getId(), isSwitchedOffHost(host));
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
