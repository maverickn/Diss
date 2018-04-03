package exp.models.power;

import cloudsim.power.models.PowerModelSpecPower;

public class FujitsuSiemensComputersPrimergyRX300S4 extends PowerModelSpecPower {

    //spec.org/power_ssj2008/results/res2008q1/power_ssj2008-20080116-00034.html
    //average active power (W), first item - active idle, rest of the items - target load 10%, 20%,...100%
    private final double[] power = { 166, 179, 191, 200, 212, 224, 235, 245, 252, 259, 265 };

    @Override
    protected double getPowerData(int index) {
        return power[index];
    }
}
