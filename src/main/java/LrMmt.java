public class LrMmt {

    public static void main(String[] args) {
        String inputFolder = LrMmt.class.getClassLoader().getResource("workload/planetlab").getPath();
        // Local Regression (LR) VM allocation policy
        // Minimum Migration Time (MMT) VM selection policy
        // the safety parameter of the LR policy
        new Runner(Parameters.ENABLE_OUTPUT, false, inputFolder, "output", "20110303", "lr_mmt", "1.2");
    }
}
