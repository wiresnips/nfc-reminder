
package com.novorobo.tracker.graph;

import com.novorobo.tracker.graph.GraphStyle.TitlePosition;
import com.novorobo.tracker.graph.GraphStyle.LabelDisplay;

import com.novorobo.tracker.app.R;
import com.novorobo.constants.*;
import com.novorobo.tracker.metric.Metric;
import com.novorobo.tracker.metric.Metric.AggregatePeriod;
import com.novorobo.tracker.metric.Metric.AggregateFunction;

import android.util.Log;
import java.util.List;
import java.util.ArrayList;
import java.text.DecimalFormat;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Rect;
import android.content.Context;

import android.util.DisplayMetrics;

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




public abstract class Graph {
    protected static final float XLABEL_ANGLE = -45; // sane range: -90 <= XLABEL_ANGLE <= 90
    protected static final double XLABEL_ANGLE_RAD;
    protected static final double ABS_XLABEL_ANGLE_RAD;
    static {
        XLABEL_ANGLE_RAD = Math.toRadians( XLABEL_ANGLE );
        ABS_XLABEL_ANGLE_RAD = Math.abs(XLABEL_ANGLE_RAD);
    }


    private Metric metric;
    protected Context context;

    public GraphStyle style;


    private int width, height;
    private int marginX, marginY;

    protected int[] titleDims;
    protected int[] xLabelDims;


    protected XYSeries series;
    protected XYMultipleSeriesDataset dataset;
    protected XYMultipleSeriesRenderer renderer;

    protected double[] values = new double[0];
    private String[] xLabels = new String[0];

    // silly fix for silly problem
    public boolean usingExternalBackground = false;


    // different flavours of graph can supply their own chart
    abstract protected XYChart getChart ();




    // whatever, I can faff about with smarter decorators LATER. right now, I just want a fucking line chart
    public static Graph buildGraph (Metric metric, Context context) {
        GraphStyle style = metric.getGraphStyle();

        if (style == null || style.graphType == GraphStyle.GraphType.BAR)
            return new BarGraph(metric, context);
        else
            return new LineGraph(metric, context);
    }




    protected Graph (Metric metric, Context context) {
        if (metric == null)
            throw new IllegalArgumentException("metric == null");

        this.context = context;
        this.metric = metric;

        style = metric.getGraphStyle();
        if (style == null)
            style = new GraphStyle();

        style.setDisplay( context.getResources().getDisplayMetrics() );

        values = metric.getGraphValues(style.maxValues);

        if (style.isShowLabelsX())
            xLabels = metric.getGraphLabels(values.length);

        initGraphMachinery();

        // convert durations to something useful. No-one wants to see hours of seconds
        if (metric.datatype == Metric.DataType.DURATION)
            scaleDurationDisplay();
    }
    
    public boolean isEmpty () {
        return values.length == 0;
    }

    public Bitmap render (int width, int height) {

        this.width = width;
        this.height = height;        

        initTitle();
        initAxisRanges();

        initMarginX(); // depends on title pos/size
        initAxisY();   // depends on marginX
        initLabelsX(); // depends on marginY

        applyStyle();

        GraphicalView graph = new GraphicalView(context, getChart());
        graph.layout(0, 0, width, height);
        return graph.toBitmap();
    }

    private void initGraphMachinery () {
        series = new XYSeries(metric.name);
        for (int i = 0; i < values.length; i++)
            series.add( i, values[i] ); 

        dataset = new XYMultipleSeriesDataset();
        dataset.addSeries( series );

        renderer = new XYMultipleSeriesRenderer();
        renderer.addSeriesRenderer( new XYSeriesRenderer() );
    }



    protected void applyStyle () {
        renderer.setShowGrid(style.showGrid);
        renderer.setGridColor(style.gridColor);

        //renderer.setApplyBackgroundColor(!usingExternalBackground);
        renderer.setBackgroundColor(style.backgroundColor);

        renderer.setMarginsColor(style.marginColor);
        renderer.setLabelsColor(style.textColor);
        renderer.setXLabelsColor(style.textColor);
        renderer.setYLabelsColor(0, style.textColor);

        renderer.setShowAxes(false);                   // thin lines along axies
        renderer.setShowTickMarks(xLabels.length > 0); // tick marks along axies
        renderer.setForceHideYTickMarks(true);         // dumb hack I slipped in for myself
        renderer.setXLabels(0);                        // # of NUMERIC labels in X
        renderer.setShowLegend(false);                 // color-coding explanation of data series
        renderer.setShowGridX(true);                   // horizontal lines denoting y-values

        renderer.setMargins(new int[] { 0, marginY, marginX, 0 });

        renderer.setLabelsTextSize(style.getLabelTextSize());
        renderer.setAxisTitleTextSize(style.getTitleTextSize());

        renderer.getSeriesRendererAt(0).setColor( style.dataColor );
    }


    // I expect this to get smarter later
    protected void initAxisRanges () {
        double minX = 0, maxX = 0, minY = 0, maxY = 0;

        // if we haven't got values, there isn't a whole lot to do here
        if (values.length > 0) {
            maxX = series.getMaxX();
            if (maxX < GraphStyle.MIN_VALUES - 1)
                maxX = GraphStyle.MIN_VALUES - 1;

            maxY = series.getMaxY();

            if (series.getMinY() < 0)
                minY = series.getMinY();
        }

        // if we're a range and our aggregateFunc won't break shit, maxY goes to rangeMax
        if (metric.datatype == Metric.DataType.RANGE) {
            AggregatePeriod aggTime = metric.getAggregatePeriod();
            AggregateFunction aggFunc = metric.getAggregateFunction();

            boolean rangeBreaker = (aggFunc == AggregateFunction.CUMULATIVE) || 
                                   (aggFunc == AggregateFunction.TOTAL && aggTime != AggregatePeriod.NONE);

            if (!rangeBreaker)
                maxY = metric.rangeMax;
        }
        
        renderer.setXAxisMin(minX);
        renderer.setXAxisMax(maxX);
        renderer.setYAxisMin(minY);
        renderer.setYAxisMax(maxY);
    }


    private void initTitle () {

        if (style.titlePosition == TitlePosition.NONE) {
            titleDims = new int[] {0,0};
            return;
        }

        int pixels = graphTitleInX() ? width : height;
        float textSize = style.getTitleTextSize();

        String prefix = getTitlePrefix() + " ";
        String prefixAbrv = getTitlePrefixAbbreviated() + " ";
        String name = metric.name;
        String suffix = " " + getTitleSuffix();

        String title = "";
        String[] proposals = new String[] {
            prefix + name + suffix,
            prefixAbrv + name + suffix,
            prefix + name,
            prefixAbrv + name,
            name
        };

        // half of this is built in by aChartEngine, and the rest is for symmetry
        int padding = (int) Math.ceil(style.getTitleTextSize() * 2f / 3f);

        for (int i = 0; i < proposals.length; i++) {
            title = proposals[i];
            titleDims = getTextDims(title, textSize);
            titleDims[0] += padding;
            titleDims[1] += padding;

            if (titleDims[0] <= pixels)
                break;
        }

        // if we're in AUTO mode, we can nix the title entirely if it doesn't fit
        if (style.titlePosition == TitlePosition.AUTO && titleDims[0] > pixels) {
            titleDims = new int[] {0,0};
            return;
        }

        // if we're forced, we'll take the last option even if it overflows
        // add the title to the graph
        renderer.setForceShowTitle(true);

        if (graphTitleInX())
            renderer.setXTitle(title);
        else
            renderer.setYTitle(title);
    }

    private String getTitlePrefix () {
        switch (metric.getAggregateFunction()) {
            case TOTAL:      return context.getString(R.string.widget_value_label_total  );
            case AVERAGE:    return context.getString(R.string.widget_value_label_average);
            case MAXIMUM:    return context.getString(R.string.widget_value_label_maximum);
            case MINIMUM:    return context.getString(R.string.widget_value_label_minimum);
            case CUMULATIVE: return context.getString(R.string.widget_value_label_cumulative);
            default:         return "";
        }
    }

    private String getTitlePrefixAbbreviated () {
        switch (metric.getAggregateFunction()) {
            case TOTAL:      return context.getString(R.string.widget_value_label_total_abrv  );
            case AVERAGE:    return context.getString(R.string.widget_value_label_average_abrv);
            case MAXIMUM:    return context.getString(R.string.widget_value_label_maximum_abrv);
            case MINIMUM:    return context.getString(R.string.widget_value_label_minimum_abrv);
            case CUMULATIVE: return context.getString(R.string.widget_value_label_cumulative_abrv);
            default:         return "";
        }        
    }

    private String getTitleSuffix () {
        switch (metric.getAggregatePeriod()) {
            case DAY:   return context.getString(R.string.widget_period_label_day) ;
            case WEEK:  return context.getString(R.string.widget_period_label_week);
            case MONTH: return context.getString(R.string.widget_period_label_month);
            case YEAR:  return context.getString(R.string.widget_period_label_year);
            case NONE:
            default:    return "";
        }
    }








    private void initMarginX () {

        if (xLabels.length == 0) {
            xLabelDims = new int[]{0,0};
            marginX = 0;
        }
        
        else {
            xLabelDims = getMaxTextDims(xLabels, style.getLabelTextSize());
            marginX = (int) Math.ceil(
                (Math.sin(ABS_XLABEL_ANGLE_RAD) * xLabelDims[0]) + // labelDim's length after rotation
                (Math.cos(ABS_XLABEL_ANGLE_RAD) * xLabelDims[1]) + // labelDim's height after rotation
                (style.getLabelTextSize() * 1 / 3) +                   // downwards offset for the axis's tick mark
                (style.getLabelTextSize() * 1 / 3) );                  // padding underneath to match the tick's offset above            
        }

        boolean haveTitle = titleDims[1] > 0;

        if (haveTitle && graphTitleInX()) {
            // undo the default padding situation
            renderer.setXLabelsPadding( marginX -(style.getLabelTextSize() * 4/3) );
            marginX += titleDims[1];
        }        
    }


    private void initAxisY () {
        int chartHeight = height - marginX;
        int labelHeight = getTextDims("0", style.getLabelTextSize())[1];

        // defaults in case we turned off labels
        int labelCount = 0;
        String[] yLabels = new String[0];
        int[] yLabelDims = new int[] {0,0};
        marginY = 0;

        // but if we HAVE labels, fill the variables above with real values
        if (style.isShowLabelsY()) {
            labelCount = (int) (chartHeight / (labelHeight * style.yLabelSpacing));
            if (labelCount < 2)
                labelCount = 2;

            yLabels = buildLabelsY(labelCount);

            // nix the lone zero label that appears if there's nothing else
            if (yLabels.length == 0)
                renderer.addYTextLabel(0,"");

            yLabelDims = getMaxTextDims(yLabels, style.getLabelTextSize());
            marginY = yLabelDims[0];

            if (marginY > 0)
                marginY += style.getYLabelPadding() * 2; // if we have content, we get padding
        }


        boolean haveTitle = titleDims[1] > 0;
        if (haveTitle && graphTitleInY())
            marginY += titleDims[1];

        // nudge our Y-range to keep OUR labels from being cut off in the vertical
        preventYLabelClipping(yLabels, labelHeight);

        renderer.setYLabels( labelCount );
        renderer.setYLabelsPadding( (yLabelDims[0] * 0.5f) + style.getYLabelPadding() );
        renderer.setYLabelsVerticalPadding( (float) Math.round(-0.5 * yLabelDims[1]) );
    }





    protected int[] getChartPixelDims () {
        return new int[] { width - marginY, height - marginX };
    }


    private boolean graphTitleInX () {
        switch (style.titlePosition) {
            case NONE:
            case Y_AXIS: return false;
            case X_AXIS: return true;
            case AUTO:   
            default: return height >= width;
        }
    }

    private boolean graphTitleInY () {
        switch (style.titlePosition) {
            case NONE:
            case X_AXIS: return false;
            case Y_AXIS: return true;
            case AUTO:   
            default: return height <= width;
        }
    }



    private void initLabelsX () {
        int labelStride = getMinXLabelSpacing();

        // if there's any unused label-space at the front of the graph, grow the
        // gaps between labels until as much as possible has been reclaimed
        double chartMax = renderer.getXAxisMax();
        int usableLabelSockets = (int) Math.floor(chartMax) - 1; // the LAST socket is my fencepost
        if (usableLabelSockets > labelStride) {
            int reclaimableSockets = usableLabelSockets % labelStride;
            int labelGaps = usableLabelSockets / labelStride;
            labelStride += reclaimableSockets / labelGaps;
        }

        // keep a list of the labels that actually get used, to redo marginX with
        List<String> finalLabels = new ArrayList<String>();

        // okay, add the labels to the chart
        for (int i = xLabels.length - 1; i >= 0; i -= labelStride) {
            renderer.addXTextLabel( i, xLabels[i] );
            finalLabels.add( xLabels[i] );
        }

        double firstLabelPos = (xLabels.length - 1) % labelStride;
        preventXLabelClipping( firstLabelPos );

        // And now we're going to recalculate marginX with the newly culled list of labels. This tightens
        // things up if particularly long labels were removed and short ones remain, but mostly it solves
        // the huge empty space that's shown if NO labels remain.
        xLabels = finalLabels.toArray(new String[finalLabels.size()]);

        // technically, I could recalculate marginY, but that's a causal loop I don't want to go down.
        // two iterations on marginX is all you get, and the labels in Y can just stretch a bit to fill the space.
        initMarginX();

        // also some random-assed style stuff

        // text is drawn from the bottom-left corner. This is also the origin of any rotation.
        // this can throw things off-kilter so I apply offsets to balance things out. (see XYChart.drawXLabels())
        float labelOffsetX = (float) Math.sin(XLABEL_ANGLE_RAD) * xLabelDims[1] * -0.5f ;
        float labelOffsetY = style.getLabelTextSize() - (xLabelDims[1] * (float) Math.cos(ABS_XLABEL_ANGLE_RAD));

        renderer.setXLabelsHorizontalOffset(labelOffsetX);
        renderer.setXLabelsVerticalOffset(-labelOffsetY);

        renderer.setXLabelsAngle(XLABEL_ANGLE);
        renderer.setXLabelsAlign( (XLABEL_ANGLE > 0) ? Paint.Align.LEFT : Paint.Align.RIGHT );
    }


    // labels in X are placed on the integers, and cannot overlap.
    // with dimensions as they stand, how far apart do labels need to be from each other?
    private int getMinXLabelSpacing () {
        int pixels = width - marginY;
        double units = renderer.getXAxisMax() - renderer.getXAxisMin();
        double unitsPerPixel = units / pixels;

        int padding = (int) Math.ceil(xLabelDims[1] * style.xLabelSpacing);

        int labelWidthFlat = xLabelDims[0] + padding;
        int labelWidthTilt = (int) Math.ceil((xLabelDims[1] + padding) / (float) Math.sin(ABS_XLABEL_ANGLE_RAD));
        int labelWidthPixels = labelWidthTilt < labelWidthFlat ? labelWidthTilt : labelWidthFlat;

        double labelWidthUnits = unitsPerPixel * labelWidthPixels;

        return (int) Math.max(1, Math.ceil(labelWidthUnits));
    }


    private void preventXLabelClipping (double firstLabelPos) {
        if (xLabels.length == 0)
            return;

        int chartPixels = width - marginY;
        double chartMinX = renderer.getXAxisMin();
        double pixelsPerUnit = chartPixels / (renderer.getXAxisMax() - chartMinX);
        int firstLabelPixel = (int) Math.floor((firstLabelPos - chartMinX) * pixelsPerUnit);

        // how wide is a label, in pixels?
        int xLabelPixelWidth = (int) Math.ceil( (Math.cos(ABS_XLABEL_ANGLE_RAD) * xLabelDims[0]) +
                                                (Math.sin(ABS_XLABEL_ANGLE_RAD) * xLabelDims[1]) );
        xLabelPixelWidth += style.getYLabelPadding(); // 'cause why not?
        
        // how far would the label stick out past the edge of the graph?
        int leftClip = xLabelPixelWidth - firstLabelPixel - marginY;

        if (leftClip > 0) {
            // if we HAVE a marginY, just use that
            if (marginY > 0)
                marginY += leftClip;

            // if we DON'T have a marginY we need to nudge XAxisMin instead
            // otherwise, we get an unsigthly empty sliver of background up the side
            else {
                chartMinX -= leftClip / pixelsPerUnit;
                renderer.setXAxisMin(chartMinX);
            }
        }

        // there's a FURTHER problem, which is a risk that our rightmost label will stick out the side.
        // if this turns out to be the case, we will compress the scale inwards, and damned be the collisions
        int xLabelPixelsRight = (int) Math.ceil( Math.sin(ABS_XLABEL_ANGLE_RAD) * xLabelDims[1] / 2 );
        int clearance = (int) Math.floor( (renderer.getXAxisMax() % 1) * pixelsPerUnit );
        int rightClip = xLabelPixelsRight - clearance;

        if (rightClip > 0) {
            double nudge = rightClip / pixelsPerUnit;
            renderer.setXAxisMin( renderer.getXAxisMin() - nudge );
            renderer.setXAxisMax( renderer.getXAxisMax() + nudge );
        }
    }


    private void preventYLabelClipping (String[] labels, int labelHeight) {
        if (labels.length == 0)
            return;

        // figure out how large (in chart units) the labels are
        double chartMin = renderer.getYAxisMin();
        double chartMax = renderer.getYAxisMax();

        int pixelHeight = height - marginX;
        double unitsPerPixel = (chartMax - chartMin) / pixelHeight;
        double labelInUnits = unitsPerPixel * labelHeight;

        // figure out where in the chart the top and bottom labels are shown
        double minLabelValue, maxLabelValue;

        // strip out any characters, but leave the capital E (which is valid in doubles)
        String minLabel = labels[0].replaceAll("[a-zA-DF-Z]", ""); 
        String maxLabel = labels[labels.length - 1].replaceAll("[a-zA-DF-Z]", "");
        try {
            minLabelValue = Double.parseDouble( minLabel );
            maxLabelValue = Double.parseDouble( maxLabel );
        } catch (NumberFormatException e) {
            return;
        }

        // knowing the size and position of the labels, we can offset
        // the upper and lower range of the chart to prevent clipping
        double minWithOffset = minLabelValue - labelInUnits;
        double maxWithOffset = maxLabelValue + labelInUnits;

        // only pad the bottom if there's no marginX- otherwise, we'll have plenty of space for the label        
        if (marginX == 0 && minWithOffset < chartMin) {
            renderer.setForcedMinYLabel(minLabelValue);
            renderer.setYAxisMin(minWithOffset);
        }
        if (maxWithOffset > chartMax) {
            renderer.setForcedMaxYLabel(maxLabelValue);
            renderer.setYAxisMax(maxWithOffset);
        }
    }








    private String[] buildLabelsY (int labelCount) {
        if (isEmpty() || labelCount == 0)
            return new String[0];

        java.text.NumberFormat format = renderer.getLabelFormat();

        double minChartValue = renderer.isMinYSet() ? renderer.getYAxisMin() : series.getMinY();
        double maxChartValue = renderer.isMaxYSet() ? renderer.getYAxisMax() : series.getMaxY();
        List<Double> labelNumbers = MathHelper.getLabels( minChartValue, maxChartValue, labelCount );

        String[] labels = new String[labelNumbers.size()];
        for (int i = 0; i < labels.length; i++)
            labels[i] = formatLabel( format, labelNumbers.get(i) );

        return labels;
    }

    private String formatLabel (java.text.NumberFormat format, double label) {
        if (format != null)
            return format.format(label);

        if (label == Math.round(label))
            return Math.round(label) + "";

        return label + "";
    }



    private void scaleDurationDisplay () {

        // be default, values are in milliseconds
        double divisor = 1;
        String suffix = "ms";

        // scale our values to the largest unit that we have more than 2 of
        // ie, show 47hrs in hours, but step up to days for 48hrs
        double valueScale = 0;
        for (double value : values)
            if (value > valueScale)
                valueScale = value;
        valueScale /= 2;

        if (valueScale >= MILLI_PER.DAY) {
            divisor = MILLI_PER.DAY;
            suffix = "d";
        } else if (valueScale >= MILLI_PER.HOUR) {
            divisor = MILLI_PER.HOUR;
            suffix = "h";
        } else if (valueScale >= MILLI_PER.MINUTE) {
            divisor = MILLI_PER.MINUTE;
            suffix = "m";
        } else if (valueScale >= MILLI_PER.SECOND) {
            divisor = MILLI_PER.SECOND;
            suffix = "s";
        } 

        // not SUPER efficient to just scrap and rebuild the series, 
        // but the extra coordination to avoid repeating work isn't worth it
        series.clear();
        for (int i = 0; i < values.length; i++) {
            values[i] /= divisor;
            series.add( i, values[i] );
        }

        renderer.setLabelFormat(new DecimalFormat("#.##'" + suffix + "'"));
    }


    // utils that don't need reinstantiating
    private Paint paint = new Paint();
    private Rect bounds = new Rect();

    private int[] getMaxTextDims (String[] strings, float textSize) {
        paint.setTextSize(textSize);

        int xDim = 0;
        int yDim = 0;

        for (String text : strings) {
            paint.getTextBounds(text, 0, text.length(), bounds);

            int width = bounds.width();
            int height = bounds.height();

            if (width > xDim)  xDim = width;
            if (height > yDim) yDim = height;
        }
        
        return new int[] { xDim, yDim };
    }

    private int[] getTextDims (String text, float textSize) {
        paint.setTextSize(textSize);
        paint.getTextBounds(text, 0, text.length(), bounds);
        return new int[] { bounds.width(), bounds.height() };
    }


    // this ought to show me when I'm being scaled against my will
    private Bitmap testPattern (int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888 );

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                boolean border = (x == 0 || y == 0 || x == width - 1 || y == height - 1);
                boolean bothEven = ((x % 2) == 0 && (y % 2) == 0);

                int color = border ? 0xFF0000FF : bothEven ? 0xFFFFFFFF : 0xFF000000;
                bmp.setPixel(x, y, color);
            }
        }

        return bmp;
    }


}



