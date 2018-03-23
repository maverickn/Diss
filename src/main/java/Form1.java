import org.cloudbus.cloudsim.core.CloudSim;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.json.simple.parser.ParseException;
import policy.HostPowerModeSelectionPolicyAgent;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class Form1 {
    private JPanel mainPanel;
    private JPanel slaPanel;
    private JPanel powerPanel;
    private JPanel migrationPanel;

    private JButton selectConfigButton;
    private JButton runButton;

    private JLabel fileNameLabel;
    private JLabel processingLabel;

    private File selectedFile = null;

    private XYPlot slaPlot;
    private XYPlot powerPlot;
    private XYPlot migrationPlot;

    private int datasetSlaIndex = 0;
    private int datasetPowerIndex = 0;
    private int datasetMigrationIndex = 0;

    public Form1() {
        JFrame frame = new JFrame("Diss");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setContentPane(mainPanel);
        frame.setSize(1250, 1000);
        frame.setLocationRelativeTo(null);

        mainPanel.setLayout(null);
        slaPanel.setBounds(170,10,1050,290);
        slaPanel.setLayout(new BorderLayout());
        powerPanel.setBounds(170,320,1050,290);
        powerPanel.setLayout(new BorderLayout());
        migrationPanel.setBounds(170,630,1050,290);
        migrationPanel.setLayout(new BorderLayout());

        selectConfigButton.setBounds(10,10,150,30);
        runButton.setBounds(10,70,150,30);
        runButton.setEnabled(false);

        fileNameLabel.setBounds(10,40,300, 30);
        processingLabel.setBounds(10,100,300, 30);

        final JFreeChart slaChart = ChartFactory.createXYLineChart("SLA violation time","Time", "SLA Violation Time",
                null, PlotOrientation.VERTICAL, true, true, false);
        slaPlot = slaChart.getXYPlot();
        final ValueAxis axis1 = slaPlot.getDomainAxis();
        axis1.setAutoRange(true);
        final NumberAxis rangeAxis1 = new NumberAxis("Range Axis 1");
        rangeAxis1.setAutoRangeIncludesZero(false);
        final ChartPanel slaChartPanel = new ChartPanel(slaChart);
        slaPanel.add(slaChartPanel);
        slaChartPanel.setDomainZoomable(true);

        final JFreeChart powerChart = ChartFactory.createXYLineChart("Power consumption","Time", "Power consumption",
                null, PlotOrientation.VERTICAL, true, true, false);
        powerPlot = powerChart.getXYPlot();
        final ValueAxis axis2 = powerPlot.getDomainAxis();
        axis2.setAutoRange(true);
        final NumberAxis rangeAxis2 = new NumberAxis("Range Axis 2");
        rangeAxis2.setAutoRangeIncludesZero(false);
        final ChartPanel powerChartPanel = new ChartPanel(powerChart);
        powerPanel.add(powerChartPanel);
        powerChartPanel.setDomainZoomable(true);

        final JFreeChart migrationChart = ChartFactory.createXYLineChart("Migration count","Time", "Migration count",
                null, PlotOrientation.VERTICAL, true, true, false);
        migrationPlot = migrationChart.getXYPlot();
        final ValueAxis axis3 = migrationPlot.getDomainAxis();
        axis3.setAutoRange(true);
        final NumberAxis rangeAxis3 = new NumberAxis("Range Axis 3");
        rangeAxis3.setAutoRangeIncludesZero(false);
        final ChartPanel migrationChartPanel = new ChartPanel(migrationChart);
        migrationPanel.add(migrationChartPanel);
        migrationChartPanel.setDomainZoomable(true);

        frame.setVisible(true);

        selectConfigButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
                jfc.setDialogTitle("Select a config file");
                jfc.setAcceptAllFileFilterUsed(false);
                FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON files", "json");
                jfc.addChoosableFileFilter(filter);
                int returnValue = jfc.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    selectedFile = jfc.getSelectedFile();
                    fileNameLabel.setText(selectedFile.getName());
                    runButton.setEnabled(true);
                    processingLabel.setText("");
                }
            }
        });

        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    ParseConfig.getData(selectedFile.getAbsolutePath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Config file not found.\n" + ex.toString(),
                            "Config file not found", JOptionPane.ERROR_MESSAGE);
                    processingLabel.setText("");
                    return;
                } catch (ParseException ex) {
                    JOptionPane.showMessageDialog(frame, "Catching exception while parsing a config file.\n" + ex.toString(),
                            "Parse exception", JOptionPane.ERROR_MESSAGE);
                    processingLabel.setText("");
                    return;
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Catching exception:\n" + ex.toString(),
                            "Exception", JOptionPane.ERROR_MESSAGE);
                    processingLabel.setText("");
                    return;
                }
                try {
                    new Runner(ParseConfig.inputFolder, ParseConfig.outputFolder, ParseConfig.experimentName);

                    datasetSlaIndex++;
                    slaPlot.setDataset(datasetSlaIndex, createDataset(HostPowerModeSelectionPolicyAgent.getTimeList(), HostPowerModeSelectionPolicyAgent.getSlaViolationTimeList(), "Q-learning agent"));
                    slaPlot.setRenderer(datasetSlaIndex, new StandardXYItemRenderer());

                    datasetPowerIndex++;
                    powerPlot.setDataset(datasetPowerIndex, createDataset(HostPowerModeSelectionPolicyAgent.getTimeList(), HostPowerModeSelectionPolicyAgent.getPowerConsumptionList(), "Q-learning agent"));
                    powerPlot.setRenderer(datasetPowerIndex, new StandardXYItemRenderer());
                    processingLabel.setText("Done!");
                    frame.toFront();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Simulation terminated! Catching exception:\n" + ex.toString(),
                            "Exception", JOptionPane.ERROR_MESSAGE);
                    CloudSim.terminateSimulation();
                    processingLabel.setText("");
                }
            }
        });

        runButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if (selectedFile != null) {
                    processingLabel.setText("Processing...");
                }
            }
        });
    }

    private XYDataset createDataset(List<Double> timeList, List<Double> list, String key) {
        final XYSeries series = new XYSeries(key);
        for (int i = 0; i < timeList.size(); i++) {
            series.add(timeList.get(i), list.get(i));
        }
        final XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        return dataset;
    }

    public static void main(String[] args) {
        new Form1();
    }
}
