/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package cloudsim.power;

import java.util.LinkedList;
import java.util.List;

import cloudsim.CloudletScheduler;
import cloudsim.Vm;
import cloudsim.core.CloudSim;
import cloudsim.util.MathUtil;

/**
 * A class of VM that stores its CPU utilization percentage history. The history is used by VM allocation
 * and selection policies.
 * 
 * <br/>If you are using any algorithms, policies or workload included in the power package please cite
 * the following paper:<br/>
 * 
 * <ul>
 * <li><a href="http://dx.doi.org/10.1002/cpe.1867">Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012</a>
 * </ul>
 * 
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 2.0
 */
public class PowerVm extends Vm {

	/** The Constant HISTORY_LENGTH. */
	public static final int HISTORY_LENGTH = 30;

	/** The CPU utilization percentage history. */
	private final List<Double> utilizationHistoryCpu = new LinkedList<Double>();

	private final List<Double> utilizationHistoryRam = new LinkedList<Double>();

	private final List<Double> utilizationHistoryBw = new LinkedList<Double>();

	/** The previous time that cloudlets were processed. */
	private double previousTime;

	/** The scheduling interval to update the processing of cloudlets
         * running in this VM. */
	private double schedulingInterval;

	/**
	 * Instantiates a new PowerVm.
	 * 
	 * @param id the id
	 * @param userId the user id
	 * @param mips the mips
	 * @param pesNumber the pes number
	 * @param ram the ram
	 * @param bw the bw
	 * @param size the size
	 * @param priority the priority
	 * @param vmm the vmm
	 * @param cloudletScheduler the cloudlet scheduler
	 * @param schedulingInterval the scheduling interval
	 */
	public PowerVm(
			final int id,
			final int userId,
			final double mips,
			final int pesNumber,
			final int ram,
			final long bw,
			final long size,
			final int priority,
			final String vmm,
			final CloudletScheduler cloudletScheduler,
			final double schedulingInterval) {
		super(id, userId, mips, pesNumber, ram, bw, size, vmm, cloudletScheduler);
		setSchedulingInterval(schedulingInterval);
	}

	@Override
	public double updateVmProcessing(final double currentTime, final List<Double> mipsShare) {
		double time = super.updateVmProcessing(currentTime, mipsShare);
		if (currentTime > getPreviousTime() && (currentTime - 0.1) % getSchedulingInterval() == 0) {
			double utilizationCpu = getTotalUtilizationOfCpu(getCloudletScheduler().getPreviousTime());
			if (CloudSim.clock() != 0 || utilizationCpu != 0) {
				addUtilizationHistoryValueCpu(utilizationCpu);
			}
			double utilizationRam = getTotalUtilizationOfRam(getCloudletScheduler().getPreviousTime());
			if (CloudSim.clock() != 0 || utilizationRam != 0) {
				addUtilizationHistoryValueRam(utilizationRam);
			}
			double utilizationBw = getTotalUtilizationOfBw(getCloudletScheduler().getPreviousTime());
			if (CloudSim.clock() != 0 || utilizationBw != 0) {
				addUtilizationHistoryValueBw(utilizationBw);
			}
			setPreviousTime(currentTime);
		}
		return time;
	}

	/**
	 * Gets the utilization MAD.
	 * 
	 * @return the utilization MAD
	 */
	public double getUtilizationMad(List<Double> utilizationHistory) {
		double mad = 0;
		if (!utilizationHistory.isEmpty()) {
			int n = HISTORY_LENGTH;
			if (HISTORY_LENGTH > utilizationHistory.size()) {
				n = utilizationHistory.size();
			}
			double median = MathUtil.median(utilizationHistory);
			double[] deviationSum = new double[n];
			for (int i = 0; i < n; i++) {
				deviationSum[i] = Math.abs(median - utilizationHistory.get(i));
			}
			mad = MathUtil.median(deviationSum);
		}
		return mad;
	}

	/**
	 * Gets the utilization mean in percents.
	 * 
	 * @return the utilization mean
	 */
	public double getUtilizationMean(List<Double> utilizationHistory) {
		double mean = 0;
		if (!utilizationHistory.isEmpty()) {
			int n = HISTORY_LENGTH;
			if (HISTORY_LENGTH > utilizationHistory.size()) {
				n = utilizationHistory.size();
			}
			for (int i = 0; i < n; i++) {
				mean += utilizationHistory.get(i);
			}
			mean /= n;
		}
		return mean * getMips();
	}

	/**
	 * Gets the utilization variance.
	 * 
	 * @return the utilization variance
	 */
	public double getUtilizationVariance(List<Double> utilizationHistory) {
		double mean = getUtilizationMean(utilizationHistory);
		double variance = 0;
		if (!utilizationHistory.isEmpty()) {
			int n = HISTORY_LENGTH;
			if (HISTORY_LENGTH > utilizationHistory.size()) {
				n = utilizationHistory.size();
			}
			for (int i = 0; i < n; i++) {
				double tmp = utilizationHistory.get(i) * getMips() - mean;
				variance += tmp * tmp;
			}
			variance /= n;
		}
		return variance;
	}

	/**
	 * Adds a CPU utilization percentage history value.
	 * 
	 * @param utilization the CPU utilization percentage to add
	 */
	public void addUtilizationHistoryValueCpu(final double utilization) {
		getUtilizationHistoryCpu().add(0, utilization);
		if (getUtilizationHistoryCpu().size() > HISTORY_LENGTH) {
			getUtilizationHistoryCpu().remove(HISTORY_LENGTH);
		}
	}

	public void addUtilizationHistoryValueRam(final double utilization) {
		getUtilizationHistoryRam().add(0, utilization);
		if (getUtilizationHistoryRam().size() > HISTORY_LENGTH) {
			getUtilizationHistoryRam().remove(HISTORY_LENGTH);
		}
	}

	public void addUtilizationHistoryValueBw(final double utilization) {
		getUtilizationHistoryBw().add(0, utilization);
		if (getUtilizationHistoryBw().size() > HISTORY_LENGTH) {
			getUtilizationHistoryBw().remove(HISTORY_LENGTH);
		}
	}

	/**
	 * Gets the CPU utilization percentage history.
	 * 
	 * @return the CPU utilization percentage history
	 */
	protected List<Double> getUtilizationHistoryCpu() {
		return utilizationHistoryCpu;
	}

	public List<Double> getUtilizationHistoryRam() {
		return utilizationHistoryRam;
	}

	public List<Double> getUtilizationHistoryBw() {
		return utilizationHistoryBw;
	}

	/**
	 * Gets the previous time.
	 * 
	 * @return the previous time
	 */
	public double getPreviousTime() {
		return previousTime;
	}

	/**
	 * Sets the previous time.
	 * 
	 * @param previousTime the new previous time
	 */
	public void setPreviousTime(final double previousTime) {
		this.previousTime = previousTime;
	}

	/**
	 * Gets the scheduling interval.
	 * 
	 * @return the schedulingInterval
	 */
	public double getSchedulingInterval() {
		return schedulingInterval;
	}

	/**
	 * Sets the scheduling interval.
	 * 
	 * @param schedulingInterval the schedulingInterval to set
	 */
	protected void setSchedulingInterval(final double schedulingInterval) {
		this.schedulingInterval = schedulingInterval;
	}

}
