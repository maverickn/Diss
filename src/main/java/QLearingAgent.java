public class QLearingAgent {

    public static void main(String[] args) {
        ParseConfig.getData("dc_config_bitbrains.json");
        new Runner(ParseConfig.enableOutput, ParseConfig.outputLog, ParseConfig.outputFolder, ParseConfig.experimentName, ParseConfig.inputFolder);
    }
}
