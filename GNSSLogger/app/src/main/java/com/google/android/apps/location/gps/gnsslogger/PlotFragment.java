/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.location.gps.gnsslogger;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.collection.ArrayMap;
import androidx.fragment.app.Fragment;
import com.google.location.lbs.gnss.gps.pseudorange.GpsNavigationMessageStore;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.util.MathHelper;

/** A plot fragment to show real-time Gnss analysis migrated from GnssAnalysis Tool. */
public class PlotFragment extends Fragment {

  /** Total number of kinds of plot tabs */
  private static final int NUMBER_OF_TABS = 2;

  /** The position of the CN0 over time plot tab */
  private static final int CN0_TAB = 0;

  /** The position of the prearrange residual plot tab */
  private static final int PR_RESIDUAL_TAB = 1;

  /** The number of Gnss constellations */
  private static final int NUMBER_OF_CONSTELLATIONS = 6;

  /** The X range of the plot, we are keeping the latest one minute visible */
  private static final double TIME_INTERVAL_SECONDS = 60;

  /** The index in data set we reserved for the plot containing all constellations */
  private static final int DATA_SET_INDEX_ALL = 0;

  /** The number of satellites we pick for the strongest satellite signal strength calculation */
  private static final int NUMBER_OF_STRONGEST_SATELLITES = 4;

  /** Data format used to format the data in the text view */
  private static final DecimalFormat sDataFormat =
      new DecimalFormat("##.#", new DecimalFormatSymbols(Locale.US));

  private GraphicalView mChartView;

  /** The average of the average of strongest satellite signal strength over history */
  private double mAverageCn0 = 0;

  /** Total number of {@link GnssMeasurementsEvent} has been received */
  private int mMeasurementCount = 0;

  private double mInitialTimeSeconds = -1;
  private TextView mAnalysisView;
  private double mLastTimeReceivedSeconds = 0;
  private final ColorMap mColorMap = new ColorMap();
  private DataSetManager mDataSetManager;
  private XYMultipleSeriesRenderer mCurrentRenderer;
  private LinearLayout mLayout;
  private int mCurrentTab = 0;

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View plotView = inflater.inflate(R.layout.fragment_plot, container, false /* attachToRoot */);

    mDataSetManager =
        new DataSetManager(NUMBER_OF_TABS, NUMBER_OF_CONSTELLATIONS, getContext(), mColorMap);

    // Set UI elements handlers
    final Spinner spinner = plotView.findViewById(R.id.constellation_spinner);
    final Spinner tabSpinner = plotView.findViewById(R.id.tab_spinner);

    OnItemSelectedListener spinnerOnSelectedListener =
        new OnItemSelectedListener() {

          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mCurrentTab = tabSpinner.getSelectedItemPosition();
            XYMultipleSeriesRenderer renderer =
                mDataSetManager.getRenderer(mCurrentTab, spinner.getSelectedItemPosition());
            XYMultipleSeriesDataset dataSet =
                mDataSetManager.getDataSet(mCurrentTab, spinner.getSelectedItemPosition());
            if (mLastTimeReceivedSeconds > TIME_INTERVAL_SECONDS) {
              renderer.setXAxisMax(mLastTimeReceivedSeconds);
              renderer.setXAxisMin(mLastTimeReceivedSeconds - TIME_INTERVAL_SECONDS);
            }
            mCurrentRenderer = renderer;
            mLayout.removeAllViews();
            mChartView = ChartFactory.getLineChartView(getContext(), dataSet, renderer);
            mLayout.addView(mChartView);
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        };

    spinner.setOnItemSelectedListener(spinnerOnSelectedListener);
    tabSpinner.setOnItemSelectedListener(spinnerOnSelectedListener);

    // Set up the Graph View
    mCurrentRenderer = mDataSetManager.getRenderer(mCurrentTab, DATA_SET_INDEX_ALL);
    XYMultipleSeriesDataset currentDataSet =
        mDataSetManager.getDataSet(mCurrentTab, DATA_SET_INDEX_ALL);
    mChartView = ChartFactory.getLineChartView(getContext(), currentDataSet, mCurrentRenderer);
    mAnalysisView = plotView.findViewById(R.id.analysis);
    mAnalysisView.setTextColor(Color.BLACK);
    mLayout = plotView.findViewById(R.id.plot);
    mLayout.addView(mChartView);
    return plotView;
  }

  /** Updates the CN0 versus Time plot data from a {@link GnssMeasurement} */
  protected void updateCnoTab(GnssMeasurementsEvent event) {
    long timeInSeconds = TimeUnit.NANOSECONDS.toSeconds(event.getClock().getTimeNanos());
    if (mInitialTimeSeconds < 0) {
      mInitialTimeSeconds = timeInSeconds;
    }

    // Building the texts message in analysis text view
    List<GnssMeasurement> measurements =
        sortByCarrierToNoiseRatio(new ArrayList<>(event.getMeasurements()));
    SpannableStringBuilder builder = new SpannableStringBuilder();
    double currentAverage = 0;
    if (measurements.size() >= NUMBER_OF_STRONGEST_SATELLITES) {
      mAverageCn0 =
          (mAverageCn0 * mMeasurementCount
                  + (measurements.get(0).getCn0DbHz()
                          + measurements.get(1).getCn0DbHz()
                          + measurements.get(2).getCn0DbHz()
                          + measurements.get(3).getCn0DbHz())
                      / NUMBER_OF_STRONGEST_SATELLITES)
              / (++mMeasurementCount);
      currentAverage =
          (measurements.get(0).getCn0DbHz()
                  + measurements.get(1).getCn0DbHz()
                  + measurements.get(2).getCn0DbHz()
                  + measurements.get(3).getCn0DbHz())
              / NUMBER_OF_STRONGEST_SATELLITES;
    }
    builder.append(
        getString(R.string.history_average_hint, sDataFormat.format(mAverageCn0) + "\n"));
    builder.append(
        getString(R.string.current_average_hint, sDataFormat.format(currentAverage) + "\n"));
    for (int i = 0; i < NUMBER_OF_STRONGEST_SATELLITES && i < measurements.size(); i++) {
      int start = builder.length();
      builder.append(
          mDataSetManager.getConstellationPrefix(measurements.get(i).getConstellationType())
              + measurements.get(i).getSvid()
              + ": "
              + sDataFormat.format(measurements.get(i).getCn0DbHz())
              + "\n");
      int end = builder.length();
      builder.setSpan(
          new ForegroundColorSpan(
              mColorMap.getColor(
                  measurements.get(i).getSvid(), measurements.get(i).getConstellationType())),
          start,
          end,
          Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    }
    builder.append(getString(R.string.satellite_number_sum_hint, measurements.size()));
    mAnalysisView.setText(builder);

    // Adding incoming data into Dataset
    mLastTimeReceivedSeconds = timeInSeconds - mInitialTimeSeconds;
    for (GnssMeasurement measurement : measurements) {
      int constellationType = measurement.getConstellationType();
      int svID = measurement.getSvid();
      if (constellationType != GnssStatus.CONSTELLATION_UNKNOWN) {
        mDataSetManager.addValue(
            CN0_TAB, constellationType, svID, mLastTimeReceivedSeconds, measurement.getCn0DbHz());
      }
    }

    mDataSetManager.fillInDiscontinuity(CN0_TAB, mLastTimeReceivedSeconds);

    // Checks if the plot has reached the end of frame and resize
    if (mLastTimeReceivedSeconds > mCurrentRenderer.getXAxisMax()) {
      mCurrentRenderer.setXAxisMax(mLastTimeReceivedSeconds);
      mCurrentRenderer.setXAxisMin(mLastTimeReceivedSeconds - TIME_INTERVAL_SECONDS);
    }

    mChartView.invalidate();
  }

  /**
   * Updates the pseudorange residual plot from residual results calculated by {@link
   * RealTimePositionVelocityCalculator}
   *
   * @param residuals An array of MAX_NUMBER_OF_SATELLITES elements where indexes of satellites was
   *     not seen are fixed with {@code Double.NaN} and indexes of satellites what were seen are
   *     filled with pseudorange residual in meters
   * @param timeInSeconds the time at which measurements are received
   */
  protected void updatePseudorangeResidualTab(double[] residuals, double timeInSeconds) {
    double timeSinceLastMeasurement = timeInSeconds - mInitialTimeSeconds;
    for (int i = 1; i <= GpsNavigationMessageStore.MAX_NUMBER_OF_SATELLITES; i++) {
      if (!Double.isNaN(residuals[i - 1])) {
        mDataSetManager.addValue(
            PR_RESIDUAL_TAB,
            GnssStatus.CONSTELLATION_GPS,
            i,
            timeSinceLastMeasurement,
            residuals[i - 1]);
      }
    }
    mDataSetManager.fillInDiscontinuity(PR_RESIDUAL_TAB, timeSinceLastMeasurement);
  }

  private List<GnssMeasurement> sortByCarrierToNoiseRatio(List<GnssMeasurement> measurements) {
    Collections.sort(
        measurements,
        new Comparator<GnssMeasurement>() {
          @Override
          public int compare(GnssMeasurement o1, GnssMeasurement o2) {
            return Double.compare(o2.getCn0DbHz(), o1.getCn0DbHz());
          }
        });
    return measurements;
  }

  /**
   * An utility class provides and keeps record of all color assignments to the satellite in the
   * plots. Each satellite will receive a unique color assignment through out every graph.
   */
  private static class ColorMap {

    private ArrayMap<Integer, Integer> mColorMap = new ArrayMap<>();
    private int mColorsAssigned = 0;
    /**
     * Source of Kelly's contrasting colors:
     * https://medium.com/@rjurney/kellys-22-colours-of-maximum-contrast-58edb70c90d1
     */
    private static final String[] CONTRASTING_COLORS = {
      "#222222", "#F3C300", "#875692", "#F38400", "#A1CAF1", "#BE0032", "#C2B280", "#848482",
      "#008856", "#E68FAC", "#0067A5", "#F99379", "#604E97", "#F6A600", "#B3446C", "#DCD300",
      "#882D17", "#8DB600", "#654522", "#E25822", "#2B3D26"
    };

    private final Random mRandom = new Random();

    private int getColor(int svId, int constellationType) {
      // Assign the color from Kelly's 21 contrasting colors to satellites first, if all color
      // has been assigned, use a random color and record in {@link mColorMap}.
      if (mColorMap.containsKey(constellationType * 1000 + svId)) {
        return mColorMap.get(getUniqueSatelliteIdentifier(constellationType, svId));
      }
      if (this.mColorsAssigned < CONTRASTING_COLORS.length) {
        int color = Color.parseColor(CONTRASTING_COLORS[mColorsAssigned++]);
        mColorMap.put(getUniqueSatelliteIdentifier(constellationType, svId), color);
        return color;
      }
      int color = Color.argb(255, mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256));
      mColorMap.put(getUniqueSatelliteIdentifier(constellationType, svId), color);
      return color;
    }
  }

  private static int getUniqueSatelliteIdentifier(int constellationType, int svID) {
    return constellationType * 1000 + svID;
  }

  /**
   * An utility class stores and maintains all the data sets and corresponding renders. We use 0 as
   * the {@code dataSetIndex} of all constellations and 1 - 6 as the {@code dataSetIndex} of each
   * satellite constellations
   */
  private static class DataSetManager {
    /** The Y min and max of each plot */
    private static final int[][] RENDER_HEIGHTS = {{5, 45}, {-60, 60}};
    /**
     *
     *
     * <ul>
     *   <li>A list of constellation prefix
     *   <li>G : GPS, US Constellation
     *   <li>S : Satellite-based Augmentation System
     *   <li>R : GLONASS, Russia Constellation
     *   <li>J : QZSS, Japan Constellation
     *   <li>C : BEIDOU China Constellation
     *   <li>E : GALILEO EU Constellation
     * </ul>
     */
    private static final String[] CONSTELLATION_PREFIX = {"G", "S", "R", "J", "C", "E"};

    private final List<ArrayMap<Integer, Integer>>[] mSatelliteIndex;
    private final List<ArrayMap<Integer, Integer>>[] mSatelliteConstellationIndex;
    private final List<XYMultipleSeriesDataset>[] mDataSetList;
    private final List<XYMultipleSeriesRenderer>[] mRendererList;
    private final Context mContext;
    private final ColorMap mColorMap;

    public DataSetManager(
        int numberOfTabs, int numberOfConstellations, Context context, ColorMap colorMap) {
      mDataSetList = new ArrayList[numberOfTabs];
      mRendererList = new ArrayList[numberOfTabs];
      mSatelliteIndex = new ArrayList[numberOfTabs];
      mSatelliteConstellationIndex = new ArrayList[numberOfTabs];
      mContext = context;
      mColorMap = colorMap;

      // Preparing data sets and renderer for all six constellations
      for (int i = 0; i < numberOfTabs; i++) {
        mDataSetList[i] = new ArrayList<>();
        mRendererList[i] = new ArrayList<>();
        mSatelliteIndex[i] = new ArrayList<>();
        mSatelliteConstellationIndex[i] = new ArrayList<>();
        for (int k = 0; k <= numberOfConstellations; k++) {
          mSatelliteIndex[i].add(new ArrayMap<Integer, Integer>());
          mSatelliteConstellationIndex[i].add(new ArrayMap<Integer, Integer>());
          XYMultipleSeriesRenderer tempRenderer = new XYMultipleSeriesRenderer();
          setUpRenderer(tempRenderer, i);
          mRendererList[i].add(tempRenderer);
          XYMultipleSeriesDataset tempDataSet = new XYMultipleSeriesDataset();
          mDataSetList[i].add(tempDataSet);
        }
      }
    }

    // The constellation type should range from 1 to 6
    private String getConstellationPrefix(int constellationType) {
      if (constellationType <= GnssStatus.CONSTELLATION_UNKNOWN
          || constellationType > NUMBER_OF_CONSTELLATIONS) {
        return "";
      }
      return CONSTELLATION_PREFIX[constellationType - 1];
    }

    /** Returns the multiple series data set at specific tab and index */
    private XYMultipleSeriesDataset getDataSet(int tab, int dataSetIndex) {
      return mDataSetList[tab].get(dataSetIndex);
    }

    /** Returns the multiple series renderer set at specific tab and index */
    private XYMultipleSeriesRenderer getRenderer(int tab, int dataSetIndex) {
      return mRendererList[tab].get(dataSetIndex);
    }

    /**
     * Adds a value into the both the data set containing all constellations and individual data set
     * of the constellation of the satellite
     */
    private void addValue(
        int tab, int constellationType, int svID, double timeInSeconds, double value) {
      XYMultipleSeriesDataset dataSetAll = getDataSet(tab, DATA_SET_INDEX_ALL);
      XYMultipleSeriesRenderer rendererAll = getRenderer(tab, DATA_SET_INDEX_ALL);
      value = Double.parseDouble(sDataFormat.format(value));
      if (hasSeen(constellationType, svID, tab)) {
        // If the satellite has been seen before, we retrieve the dataseries it is add and add new
        // data
        dataSetAll
            .getSeriesAt(mSatelliteIndex[tab].get(constellationType).get(svID))
            .add(timeInSeconds, value);
        mDataSetList[tab]
            .get(constellationType)
            .getSeriesAt(mSatelliteConstellationIndex[tab].get(constellationType).get(svID))
            .add(timeInSeconds, value);
      } else {
        // If the satellite has not been seen before, we create new dataset and renderer before
        // adding data
        mSatelliteIndex[tab].get(constellationType).put(svID, dataSetAll.getSeriesCount());
        mSatelliteConstellationIndex[tab]
            .get(constellationType)
            .put(svID, mDataSetList[tab].get(constellationType).getSeriesCount());
        XYSeries tempSeries = new XYSeries(CONSTELLATION_PREFIX[constellationType - 1] + svID);
        tempSeries.add(timeInSeconds, value);
        dataSetAll.addSeries(tempSeries);
        mDataSetList[tab].get(constellationType).addSeries(tempSeries);
        XYSeriesRenderer tempRenderer = new XYSeriesRenderer();
        tempRenderer.setLineWidth(5);
        tempRenderer.setColor(mColorMap.getColor(svID, constellationType));
        rendererAll.addSeriesRenderer(tempRenderer);
        mRendererList[tab].get(constellationType).addSeriesRenderer(tempRenderer);
      }
    }

    /**
     * Creates a discontinuity of the satellites that has been seen but not reported in this batch
     * of measurements
     */
    private void fillInDiscontinuity(int tab, double referenceTimeSeconds) {
      for (XYMultipleSeriesDataset dataSet : mDataSetList[tab]) {
        for (int i = 0; i < dataSet.getSeriesCount(); i++) {
          if (dataSet.getSeriesAt(i).getMaxX() < referenceTimeSeconds) {
            dataSet.getSeriesAt(i).add(referenceTimeSeconds, MathHelper.NULL_VALUE);
          }
        }
      }
    }

    /** Returns a boolean indicating whether the input satellite has been seen. */
    private boolean hasSeen(int constellationType, int svID, int tab) {
      return mSatelliteIndex[tab].get(constellationType).containsKey(svID);
    }

    /** Set up a {@link XYMultipleSeriesRenderer} with the specs customized per plot tab. */
    private void setUpRenderer(XYMultipleSeriesRenderer renderer, int tabNumber) {
      renderer.setXAxisMin(0);
      renderer.setXAxisMax(60);
      renderer.setYAxisMin(RENDER_HEIGHTS[tabNumber][0]);
      renderer.setYAxisMax(RENDER_HEIGHTS[tabNumber][1]);
      renderer.setYAxisAlign(Align.RIGHT, 0);
      renderer.setLegendTextSize(30);
      renderer.setLabelsTextSize(30);
      renderer.setYLabelsColor(0, Color.BLACK);
      renderer.setXLabelsColor(Color.BLACK);
      renderer.setFitLegend(true);
      renderer.setShowGridX(true);
      renderer.setMargins(new int[] {10, 10, 30, 10});
      // setting the plot untouchable
      renderer.setZoomEnabled(false, false);
      renderer.setPanEnabled(false, true);
      renderer.setClickEnabled(false);
      renderer.setMarginsColor(Color.WHITE);
      renderer.setChartTitle(
          mContext.getResources().getStringArray(R.array.plot_titles)[tabNumber]);
      renderer.setChartTitleTextSize(50);
    }
  }
}
