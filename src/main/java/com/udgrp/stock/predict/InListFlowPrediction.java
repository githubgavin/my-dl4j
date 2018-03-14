package com.udgrp.stock.predict;

import com.udgrp.stock.model.RecurrentNets;
import com.udgrp.stock.representation.InListDataSetIterator;
import com.udgrp.stock.utils.DBUtil;
import com.udgrp.stock.utils.PlotUtil;
import javafx.util.Pair;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.io.ClassPathResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Created by zhanghao on 26/7/17.
 * Modified by zhanghao on 28/9/17.
 *
 * @author ZHANG HAO
 */
public class InListFlowPrediction {

    private static final Logger log = LoggerFactory.getLogger(InListFlowPrediction.class);

    private static int exampleLength = 24; // time series length, assume 22 working days per month
    static InListDataSetIterator iterator;

    public static void main(String[] args) throws IOException {
        String file = new ClassPathResource("2016_in_list_data.csv").getFile().getAbsolutePath();
        int batchSize = 24; // mini-batch size
        double splitRatio = 0.98; // 90% for training, 10% for testing
        int epochs = 3; // training epochs

        log.info("Create dataSet iterator...");
        iterator = new InListDataSetIterator(file, batchSize, exampleLength, splitRatio);
        log.info("Load test dataset...");
        List<Pair<INDArray, INDArray>> test = iterator.getTestDataSet();

        log.info("Build lstm networks...");
        MultiLayerNetwork net = RecurrentNets.buildLstmNetworks(iterator.inputColumns(), iterator.totalOutcomes());

        log.info("Training...");
        for (int i = 0; i < epochs; i++) {
            while (iterator.hasNext()) net.fit(iterator.next()); // fit model using mini-batch data
            iterator.reset(); // reset iterator
            net.rnnClearPreviousState(); // clear previous state
        }

        log.info("Saving model...");
        File locationToSave = new File("src/main/resources/StockPriceLSTM_".concat(".zip"));
        // saveUpdater: i.e., the state for Momentum, RMSProp, Adagrad etc. Save this to train your network more in the future
        ModelSerializer.writeModel(net, locationToSave, true);

        log.info("Load model...");
        net = ModelSerializer.restoreMultiLayerNetwork(locationToSave);

        log.info("Testing...");

        double max = iterator.getMaxNum();
        double min = iterator.getMinNum();
        predictPriceOneAhead(net, test, max, min);
        //predict(net, test, max, min);
    }

    /**
     * Predict one feature of a stock one-day ahead
     */
    private static void predictPriceOneAhead(MultiLayerNetwork net, List<Pair<INDArray, INDArray>> testData, double max, double min) {
        double[] predicts = new double[testData.size()];
        double[] actuals = new double[testData.size()];
        System.out.println(testData.size());
      /*  INDArray arrs = net.rnnTimeStep(testData.get(0).getKey());
        INDArray arr = arrs.getColumn(exampleLength - 24);
        System.out.println(arrs);
        System.out.println(arr);*/
        for (int i = 0; i < testData.size(); i++) {

            predicts[i] = net.rnnTimeStep(testData.get(i).getKey()).getDouble(exampleLength - 24) * (max - min) + min;
            actuals[i] = testData.get(i).getValue().getDouble(0);
        }
        log.info("Print out Predictions and Actual Values...");
        log.info("Predict,Actual");
        for (int i = 0; i < predicts.length; i++) {
            log.info(predicts[i] + "," + actuals[i]);
        }
        log.info("Plot...");
        PlotUtil.plot(predicts, actuals, String.valueOf("ss"));
    }

    /**
     * Predict one feature of a stock one-day ahead
     */
    private static void predict(MultiLayerNetwork net, List<Pair<INDArray, INDArray>> testData, double max, double min) {
        INDArray arrs = net.rnnTimeStep(testData.get(0).getKey());
        INDArray arr = arrs.getColumn(exampleLength - 24);
        double[] predicts = new double[arr.length()];
        //System.out.println(arrs);
        //System.out.println(arr);
        for (int i = 0; i < arr.length(); i++) {
            predicts[i] = arr.getDouble(i) * (max - min) + min;
        }
        PlotUtil.plot(predicts, String.valueOf("predict"));
        //double predicts = arr.getDouble(exampleLength - 24) * (max - min) + min;
        //log.info("Plot..."+predicts);
        //DBUtil.insert(predicts);
        //Thread.sleep(1000);
    }


    public static List<Pair<INDArray, INDArray>> preLastDayDataSet() {
        double[] maxArray = iterator.getMaxArray();
        double[] minArray = iterator.getMinArray();
        List<double[]> lastDayDataList = DBUtil.readLastDayData();
        List<Pair<INDArray, INDArray>> test = new ArrayList<>();
        System.out.println(lastDayDataList.size());
        INDArray input = Nd4j.create(new int[]{exampleLength, 4}, 'f');
        for (int j = 0; j < lastDayDataList.size(); j++) {
            double[] stock = lastDayDataList.get(j);
            input.putScalar(new int[]{j, 3}, (stock[8] - minArray[3]) / (maxArray[3] - minArray[3]));
        }
        test.add(new Pair<>(input, null));
        return test;
    }

    private static void predictPriceMultiple(MultiLayerNetwork net, List<Pair<INDArray, INDArray>> testData,
                                             double max, double min) {
        // TODO
    }

    /**
     * Predict all the features (open, close, low, high prices and volume) of a stock one-day ahead
     */
    private static void predictAllCategories(MultiLayerNetwork
                                                     net, List<Pair<INDArray, INDArray>> testData, INDArray max, INDArray min) {
        INDArray[] predicts = new INDArray[testData.size()];
        INDArray[] actuals = new INDArray[testData.size()];
        for (int i = 0; i < testData.size(); i++) {
            predicts[i] = net.rnnTimeStep(testData.get(i).getKey()).getRow(exampleLength - 1).mul(max.sub(min)).add(min);
            actuals[i] = testData.get(i).getValue();
        }
        log.info("Print out Predictions and Actual Values...");
        log.info("Predict\tActual");
        for (int i = 0; i < predicts.length; i++) log.info(predicts[i] + "\t" + actuals[i]);
        log.info("Plot...");
        for (int n = 0; n < 5; n++) {
            double[] pred = new double[predicts.length];
            double[] actu = new double[actuals.length];
            for (int i = 0; i < predicts.length; i++) {
                pred[i] = predicts[i].getDouble(n);
                actu[i] = actuals[i].getDouble(n);
            }
            String name;
            switch (n) {
                case 0:
                    name = "Stock LAST_MONTH Price";
                    break;
                case 1:
                    name = "Stock LAST_WEEK Price";
                    break;
                case 2:
                    name = "Stock LAST_DAY Price";
                    break;
                case 3:
                    name = "Stock CURRENT Price";
                    break;
                case 4:
                    name = "Stock VOLUME Amount";
                    break;
                default:
                    throw new NoSuchElementException();
            }
            PlotUtil.plot(pred, actu, name);
        }
    }

}
