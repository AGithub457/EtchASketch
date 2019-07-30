import com.fazecast.jSerialComm.SerialPort;
import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Scanner;

/**
 * Created by Armand on 3/22/17.
 */
public class Main {
    public static SerialPort[] ports;
    private static JFrame frame = new JFrame();
    private static JPanel top = new JPanel();
    private static JPanel bottom = new JPanel();
    private static JButton commConnect = new JButton("Connect");
    private static JButton commRefresh = new JButton("Refresh");
    private static JComboBox<String> portList = new JComboBox<>();
    private static XYSeries graph = new XYSeries("Potentiometer Values");
    private static XYSeriesCollection dataset = new XYSeriesCollection(graph);
    private static SerialPort chosenPort;
    private static JFreeChart chart = ChartFactory.createXYLineChart("Potentiometer Values", "Left/Right", "Up/Down", dataset, PlotOrientation.VERTICAL, false, false, false);
    private static XYPlot xyPlot = chart.getXYPlot();
    private static ValueAxis domainAxis = xyPlot.getDomainAxis();
    private static ValueAxis rangeAxis = xyPlot.getRangeAxis();
    private static Color trans = new Color(255, 255, 255, 0);

    public static void main(String[] args) {
        gui();

        ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            portList.addItem(port.getSystemPortName());
        }

        commConnect.addActionListener(new ActionListener(){
            @Override public void actionPerformed(ActionEvent arg0) {
                if(commConnect.getText().equals("Connect")) {
                    chosenPort = SerialPort.getCommPort(portList.getSelectedItem().toString());
                    chosenPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
                    if(chosenPort.openPort()) {
                        commConnect.setText("Disconnect");
                        portList.setEnabled(false);
                        commRefresh.setEnabled(false);
                    }

                    Thread thread = new Thread(){
                        @Override public void run() {
                            Scanner scanner = new Scanner(chosenPort.getInputStream());
                            int numberOld = 0, number2Old = 0;
                            while(scanner.hasNextLine()) {
                                try {
                                    String line = scanner.nextLine();
                                    StringUtils.substring(line, 0, 4);
                                    int number = Integer.parseInt(StringUtils.substring(line, 0, 4));
                                    int number2 = Integer.parseInt(StringUtils.substring(line, 4, 8));
                                    number = 1023 - number;
                                    number2 = 1023 - number2;
                                    if(number != numberOld || number2 != number2Old) {
                                        System.out.println(number + " " + number2);
                                        graph.add(number, number2);
                                    }
                                    numberOld = number;
                                    number2Old = number2;
                                } catch(Exception e) {}
                            }

                            chosenPort.closePort();
                            portList.setEnabled(true);
                            commRefresh.setEnabled(true);
                            commConnect.setText("Connect");
                            graph.clear();

                            scanner.close();
                        }
                    };
                    thread.start();
                } else {
                    chosenPort.closePort();
                    portList.setEnabled(true);
                    commRefresh.setEnabled(true);
                    commConnect.setText("Connect");
                    graph.clear();
                }
            }
        });

        commRefresh.addActionListener(new ActionListener(){
            @Override public void actionPerformed(ActionEvent arg0) {
                portList.removeAllItems();
                ports = SerialPort.getCommPorts();
                for (SerialPort port : ports) {
                    portList.addItem(port.getSystemPortName());
                }
            }
        });
    }

    public static void gui() {
        frame.setTitle("Etch-A-Sletch");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700,550);
        frame.setLayout(new BorderLayout());

        top.setLayout(new FlowLayout(FlowLayout.CENTER));
        top.add(commRefresh);
        top.add(portList);
        top.add(commConnect);

        bottom.setLayout(new FlowLayout(FlowLayout.CENTER));
        chart.setBackgroundPaint(trans);
        rangeAxis.setRange(0, 1023);
        domainAxis.setRange(0,1023);
        bottom.add(new ChartPanel(chart));

        frame.add(top, BorderLayout.NORTH);
        frame.add(bottom, BorderLayout.SOUTH);
        frame.setVisible(true);
    }
}
