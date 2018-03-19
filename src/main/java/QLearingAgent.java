public class QLearingAgent {

    public static void main(String[] args) {
        ParseConfig.getData("dc_config_bitbrains.json");
        new Runner(ParseConfig.inputFolder, ParseConfig.outputFolder, ParseConfig.experimentName);
    }
}
