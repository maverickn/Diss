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

public class Form1 {
    private JPanel mainPanel;
    private JPanel panelSlaViolationTimeChart;
    private JPanel panelPowerConsumptionChart;

    private JButton selectConfigButton;
    private JButton runButton;

    private JLabel fileNameLabel;
    private JLabel processingLabel;

    private File selectedFile = null;

    private XYPlot plotSlaViolationTime;
    private int datasetSlaIndex = 0;

    private XYPlot plotPowerConsumption;
    private int datasetPowerIndex = 0;

    public Form1() {
        JFrame frame = new JFrame("Diss");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setContentPane(mainPanel);
        frame.setSize(1250, 750);
        frame.setLocationRelativeTo(null);

        mainPanel.setLayout(null);
        panelSlaViolationTimeChart.setBounds(170,10,1050,330);
        panelSlaViolationTimeChart.setLayout(new BorderLayout());
        panelPowerConsumptionChart.setBounds(170,355,1050,330);
        panelPowerConsumptionChart.setLayout(new BorderLayout());

        selectConfigButton.setBounds(10,10,150,30);
        runButton.setBounds(10,70,150,30);
        runButton.setEnabled(false);

        fileNameLabel.setBounds(10,40,300, 30);
        processingLabel.setBounds(10,100,300, 30);

        final JFreeChart slaViolationTimeChart = ChartFactory.createXYLineChart("SLA violation time","Time", "SLA Violation Time",
                null, PlotOrientation.VERTICAL, true, true, false);

        slaViolationTimeChart.setBackgroundPaint(Color.white);
        plotSlaViolationTime = slaViolationTimeChart.getXYPlot();
        plotSlaViolationTime.setBackgroundPaint(Color.lightGray);
        plotSlaViolationTime.setDomainGridlinePaint(Color.white);
        plotSlaViolationTime.setRangeGridlinePaint(Color.white);

        final ValueAxis axis1 = plotSlaViolationTime.getDomainAxis();
        axis1.setAutoRange(true);

        final NumberAxis rangeAxis1 = new NumberAxis("Range Axis 1");
        rangeAxis1.setAutoRangeIncludesZero(false);

        final ChartPanel chartPanelSla = new ChartPanel(slaViolationTimeChart);
        panelSlaViolationTimeChart.add(chartPanelSla);
        chartPanelSla.setDomainZoomable(true);

        final JFreeChart powerConsumptionChart = ChartFactory.createXYLineChart("Power consumption","Time", "Power consumption",
                null, PlotOrientation.VERTICAL, true, true, false);

        powerConsumptionChart.setBackgroundPaint(Color.white);
        plotPowerConsumption = powerConsumptionChart.getXYPlot();
        plotPowerConsumption.setBackgroundPaint(Color.lightGray);
        plotPowerConsumption.setDomainGridlinePaint(Color.white);
        plotPowerConsumption.setRangeGridlinePaint(Color.white);

        final ValueAxis axis2 = plotPowerConsumption.getDomainAxis();
        axis2.setAutoRange(true);

        final NumberAxis rangeAxis2 = new NumberAxis("Range Axis 2");
        rangeAxis2.setAutoRangeIncludesZero(false);

        final ChartPanel chartPanelPower = new ChartPanel(powerConsumptionChart);
        panelPowerConsumptionChart.add(chartPanelPower);
        chartPanelPower.setDomainZoomable(true);

        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
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
                ParseConfig.getData(selectedFile.getAbsolutePath());
                new Runner(ParseConfig.inputFolder, ParseConfig.outputFolder, ParseConfig.experimentName);
                processingLabel.setText("Done!");
                //panelSlaViolationTimeChart.add(getChart(), BorderLayout.NORTH);
                datasetSlaIndex++;
                plotSlaViolationTime.setDataset(datasetSlaIndex, createDatasetSlaViolationTime());
                plotSlaViolationTime.setRenderer(datasetSlaIndex, new StandardXYItemRenderer());

                datasetPowerIndex++;
                plotPowerConsumption.setDataset(datasetPowerIndex, createDatasetPowerConsumption());
                plotPowerConsumption.setRenderer(datasetPowerIndex, new StandardXYItemRenderer());
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

    private XYDataset createDatasetSlaViolationTime() {
        final XYSeries agent = new XYSeries("Q-learning agent");
        java.util.List<Double> timeList = HostPowerModeSelectionPolicyAgent.getTimeList();
        java.util.List<Double> slaViolationTimeList = HostPowerModeSelectionPolicyAgent.getSlaViolationTimeList();
        for (int i = 0; i < slaViolationTimeList.size(); i++) {
            agent.add(timeList.get(i),slaViolationTimeList.get(i));
        }
        final XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(agent);
        return dataset;
    }

    private XYDataset createDatasetPowerConsumption() {
        final XYSeries agent = new XYSeries("Q-learning agent");
        java.util.List<Double> timeList = HostPowerModeSelectionPolicyAgent.getTimeList();
        java.util.List<Double> powerConsumptionList = HostPowerModeSelectionPolicyAgent.getPowerConsumptionList();
        for (int i = 0; i < powerConsumptionList.size(); i++) {
            agent.add(timeList.get(i),powerConsumptionList.get(i));
        }
        final XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(agent);
        return dataset;
    }

    public static void main(String[] args) {
        new Form1();
    }
}
