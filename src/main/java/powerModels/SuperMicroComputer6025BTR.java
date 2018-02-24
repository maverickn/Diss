package powerModels;

import org.cloudbus.cloudsim.power.models.PowerModelSpecPower;

public class SuperMicroComputer6025BTR extends PowerModelSpecPower {

    //spec.org/power_ssj2008/results/res2008q1/power_ssj2008-20080115-00030.html
    //average active power (W), first item - active idle, rest of the items - target load 10%, 20%,...100%
    private final double[] power = { 216, 221, 227, 235, 245, 258, 270, 282, 294, 307, 315 };

    @Override
    protected double getPowerData(int index) {
        return power[index];
    }
}
