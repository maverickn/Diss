package models.power;

import org.cloudbus.cloudsim.power.models.PowerModelSpecPower;

public class SuperMicroComputer6025BTRE5345 extends PowerModelSpecPower {

    //spec.org/power_ssj2008/results/res2008q1/power_ssj2008-20080115-00026.html
    //average active power (W), first item - active idle, rest of the items - target load 10%, 20%,...100%
    private final double[] power = { 220, 235, 249, 261, 273, 286, 298, 309, 319, 327, 334 };

    @Override
    protected double getPowerData(int index) {
        return power[index];
    }
}
