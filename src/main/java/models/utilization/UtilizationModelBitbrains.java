package models.utilization;

import org.cloudbus.cloudsim.UtilizationModel;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class UtilizationModelBitbrains implements UtilizationModel {

    private double schedulingInterval;

    private final double[] data;

    public UtilizationModelBitbrains(String inputPath, double schedulingInterval, int dataSamples)
            throws NumberFormatException, IOException {
        setSchedulingInterval(schedulingInterval);
        data = new double[dataSamples + 1];
        BufferedReader input = new BufferedReader(new FileReader(inputPath));
        input.readLine();
        String line = input.readLine();
        int n = data.length;
        for (int i = 0; i < n; i++) {
            String[] elements = line.split(";\t");
            data[i] = Double.valueOf(elements[4]) / 100;
            line = input.readLine();
        }
        input.close();
    }

    public UtilizationModelBitbrains(String inputPath, double schedulingInterval, int dataSamples, int[] vmRam)
            throws NumberFormatException, IOException {
        setSchedulingInterval(schedulingInterval);
        int maxRam = Arrays.stream(vmRam).max().getAsInt() * 1024;
        data = new double[dataSamples + 1];
        BufferedReader input = new BufferedReader(new FileReader(inputPath));
        input.readLine();
        String line = input.readLine();
        int n = data.length;
        for (int i = 0; i < n; i++) {
            String[] elements = line.split(";\t");
            data[i] = Double.valueOf(elements[6]) / maxRam;
            line = input.readLine();
        }
        input.close();
    }

    public UtilizationModelBitbrains(String inputPath, double schedulingInterval, int dataSamples, int vmBw)
            throws NumberFormatException, IOException {
        setSchedulingInterval(schedulingInterval);
        data = new double[dataSamples + 1];
        BufferedReader input = new BufferedReader(new FileReader(inputPath));
        input.readLine();
        String line = input.readLine();
        int n = data.length;
        for (int i = 0; i < n; i++) {
            String[] elements = line.split(";\t");
            data[i] = Double.valueOf(elements[10]) * 8 / vmBw;
            line = input.readLine();
        }
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
