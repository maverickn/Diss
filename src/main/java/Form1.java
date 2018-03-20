import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

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
    private JPanel chartPanel;

    private JButton selectConfigButton;
    private JButton runButton;

    private JLabel fileNameLabel;
    private JLabel processingLabel;

    private File selectedFile = null;

    public Form1() {
        JFrame frame = new JFrame("Diss");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setContentPane(mainPanel);
        frame.setVisible(true);
        frame.setSize(1250, 750);
        frame.setLocationRelativeTo(null);

        mainPanel.setLayout(null);
        selectConfigButton.setBounds(10,10,150,30);
        fileNameLabel.setBounds(10,40,300, 30);
        runButton.setBounds(10,70,150,30);
        runButton.setEnabled(false);
        processingLabel.setBounds(10,100,300, 30);
        chartPanel.setBounds(170,10,1050,690);
        chartPanel.setLayout(new BorderLayout());

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
                chartPanel.add(getChart(), BorderLayout.NORTH);
            }
        });

        runButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if (selectedFile != null) {
                    processingLabel.setText("Output to log file. Please, wait...");
                }
            }
        });
    }

    private ChartPanel getChart() {
        JFreeChart xylineChart = ChartFactory.createXYLineChart("","X", "Y",
                createDataset(), PlotOrientation.VERTICAL, true, true, false);

        ChartPanel chartPanel = new ChartPanel(xylineChart);
        chartPanel.setPreferredSize(new Dimension(1050,690));
        final XYPlot plot = xylineChart.getXYPlot();

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesPaint(1, Color.GREEN);
        renderer.setSeriesPaint(2, Color.YELLOW);
        renderer.setSeriesStroke(0, new BasicStroke(1.0f));
        renderer.setSeriesStroke(1, new BasicStroke(1.0f));
        renderer.setSeriesStroke(2, new BasicStroke(1.0f));
        plot.setRenderer(renderer);
        chartPanel.setDomainZoomable(true);
        return chartPanel;
    }

    private XYDataset createDataset() {
        final XYSeries test1 = new XYSeries("test1");
        test1.add(1.0, 1.0);
        test1.add(2.0, 4.0);
        test1.add(3.0, 3.0);

        final XYSeries test2 = new XYSeries("test2");
        test2.add(1.0, 4.0);
        test2.add(2.0, 5.0);
        test2.add(3.0, 6.0);

        final XYSeries test3 = new XYSeries("test3");
        test3.add(3.0, 4.0);
        test3.add(4.0, 5.0);
        test3.add(5.0, 4.0);

        final XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(test1);
        dataset.addSeries(test2);
        dataset.addSeries(test3);
        return dataset;
    }

    public static void main(String[] args) {
        new Form1();
    }
}
