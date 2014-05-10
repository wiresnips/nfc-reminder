
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
import org.achartengine.chart.LineChart;
import org.achartengine.renderer.BasicStroke;
import org.achartengine.chart.PointStyle;

public class LineGraph extends Graph {

    public LineGraph (Metric metric, Context context) {
        super(metric, context);
    }

    protected XYChart getChart () {
        return new LineChart(dataset, renderer);
    }

    protected void initAxisRanges () {
        super.initAxisRanges();

        if (values.length == 0)
            return;

        double dataMinX = series.getMinX();
        double dataMaxX = series.getMaxX();
        double dataMinY = series.getMinY();
        double dataMaxY = series.getMaxY();

        double axisMinX = renderer.getXAxisMin();
        double axisMaxX = renderer.getXAxisMax();
        double axisMinY = renderer.getYAxisMin();
        double axisMaxY = renderer.getYAxisMax();

        int[] pixelDims = getChartPixelDims();
        double unitsPerPixelX = (axisMaxX - axisMinX) / (double) pixelDims[0];
        double unitsPerPixelY = (axisMaxY - axisMinY) / (double) pixelDims[1];

        //we need to nudge things enough that no datapoint crosses any of the edges
        double nudgeX = unitsPerPixelX * style.getLineWidth();
        double nudgeY = unitsPerPixelY * style.getLineWidth();

        if (dataMinX - axisMinX < nudgeX) renderer.setXAxisMin(dataMinX - nudgeX);
        if (axisMaxX - dataMaxX < nudgeX) renderer.setXAxisMax(dataMaxX + nudgeX);
        if (dataMinY - axisMinY < nudgeY) renderer.setYAxisMin(dataMinY - nudgeY);
        if (axisMaxY - dataMaxY < nudgeY) renderer.setYAxisMax(dataMaxY + nudgeY);
    }

    protected void applyStyle () {
        super.applyStyle();

        BasicStroke stroke = new BasicStroke( Paint.Cap.ROUND, Paint.Join.ROUND, 0, null, 0 );        
        XYSeriesRenderer lineRenderer = (XYSeriesRenderer) renderer.getSeriesRendererAt(0);

        lineRenderer.setStroke( stroke );
        lineRenderer.setLineWidth( style.getLineWidth() );

        if (series.getItemCount() == 1) {
            //lineRenderer.setFillPoints( true );
            lineRenderer.setPointStyle( PointStyle.CIRCLE );
            lineRenderer.setPointStrokeWidth( style.getLineWidth() );
        }
    }

}



