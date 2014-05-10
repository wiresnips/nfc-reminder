
package com.novorobo.tracker.graph;
import com.novorobo.tracker.graph.GraphStyle.TitlePosition;

import com.novorobo.tracker.app.R;
import com.novorobo.constants.*;
import com.novorobo.tracker.metric.Metric;

import android.util.Log;
import java.util.List;
import java.text.DecimalFormat;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Rect;
import android.content.Context;

import org.achartengine.util.MathHelper;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYSeries;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.chart.XYChart;
import org.achartengine.chart.BarChart;
import org.achartengine.chart.BarChart.Type;

public class BarGraph extends Graph {

    public BarGraph (Metric metric, Context context) {
        super(metric, context);
    }

    protected XYChart getChart () {
        return new BarChart(dataset, renderer, Type.DEFAULT);
    }


    protected void initAxisRanges () {
        super.initAxisRanges();
        // bump our axies outwards by a half-step to prevent our bars from cutting off
        renderer.setXAxisMin( renderer.getXAxisMin() - 0.5 );
        renderer.setXAxisMax( renderer.getXAxisMax() + 0.5 );
    }



    protected void applyStyle () {
        super.applyStyle();

        double chartWidthUnits = renderer.getXAxisMax() - renderer.getXAxisMin();
        double chartWidthPixels = getChartPixelDims()[0];
        double pixelsPerUnit = chartWidthPixels / chartWidthUnits;

        float barWidth = Math.round(style.barWidth * pixelsPerUnit);
        if (barWidth < 1)
            barWidth = 1;

        renderer.setBarWidth( barWidth );
    }

}



