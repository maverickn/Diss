package exp.models.utilization;

import cloudsim.UtilizationModel;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class UtilizationModelPlanetLab implements UtilizationModel {

    private double schedulingInterval;

    //5 minutes * 288 = 24 hours
    private final double[] data;

    public UtilizationModelPlanetLab(String inputPath, double schedulingInterval)
            throws NumberFormatException, IOException {
        data = new double[289];
        setSchedulingInterval(schedulingInterval);
        BufferedReader input = new BufferedReader(new FileReader(inputPath));
        int n = data.length;
        for (int i = 0; i < n - 1; i++) {
            data[i] = Integer.valueOf(input.readLine()) / 100.0;
        }
        data[n - 1] = data[n - 2];
        input.close();
    }

    public UtilizationModelPlanetLab(String inputPath, double schedulingInterval, int dataSamples)
            throws NumberFormatException, IOException {
        setSchedulingInterval(schedulingInterval);
        data = new double[dataSamples];
        BufferedReader input = new BufferedReader(new FileReader(inputPath));
        int n = data.length;
        for (int i = 0; i < n - 1; i++) {
            data[i] = Integer.valueOf(input.readLine()) / 100.0;
        }
        data[n - 1] = data[n - 2];
        input.close();
    }

    public void setSchedulingInterval(double schedulingInterval) {
        this.schedulingInterval = schedulingInterval;
    }

    public double getSchedulingInterval() {
        return schedulingInterval;
    }

    public double[] getData(){
        return data;
    }

    @Override
    public double getUtilization(double time) {
        if (time % getSchedulingInterval() == 0) {
            return data[(int) time / (int) getSchedulingInterval()];
        }
        int time1 = (int) Math.floor(time / getSchedulingInterval());
        int time2 = (int) Math.ceil(time / getSchedulingInterval());
        double utilization1 = data[time1];
        double utilization2 = data[time2];
        double delta = (utilization2 - utilization1) / ((time2 - time1) * getSchedulingInterval());
        double utilization = utilization1 + delta * (time - time1 * getSchedulingInterval());
        return utilization;
    }
}
