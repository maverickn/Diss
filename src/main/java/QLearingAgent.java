import org.json.simple.parser.ParseException;

import java.io.IOException;

public class QLearingAgent {

    public static void main(String[] args) {
        try {
            ParseConfig.getData("dc_config_bitbrains.json");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(0);
        }
        try {
            new Runner(ParseConfig.inputFolder, ParseConfig.outputFolder, ParseConfig.experimentName);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
