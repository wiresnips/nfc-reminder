
package com.novorobo.tracker.graph;

import com.novorobo.tracker.metric.Metric;
import com.novorobo.util.database.Database;

import android.database.Cursor;
import android.provider.BaseColumns;
import android.content.ContentValues;
import android.os.Bundle;

import org.json.JSONObject;
import org.json.JSONException;

import android.util.DisplayMetrics;

public class GraphStyle implements Database.Entry {
	public static final String BUNDLE = "graphStyleBundle";

	private DisplayMetrics screen = null;
	public void setDisplay (DisplayMetrics screen) {
		this.screen = screen;
	}
	public boolean hasDisplay () { return screen != null; }

	public enum GraphType { BAR, LINE }
	public GraphType graphType = GraphType.BAR;

	public enum TitlePosition { AUTO, X_AXIS, Y_AXIS, NONE }
	public TitlePosition titlePosition = TitlePosition.AUTO;

	public enum LabelDisplay { ALL, NONE, X_ONLY, Y_ONLY }
	public LabelDisplay labelShow = LabelDisplay.ALL;

	public boolean isShowLabelsX () { return labelShow == LabelDisplay.ALL || labelShow == LabelDisplay.X_ONLY; }
	public boolean isShowLabelsY () { return labelShow == LabelDisplay.ALL || labelShow == LabelDisplay.Y_ONLY; }

	public static final int MIN_VALUES = 3;
	public static final int MAX_VALUES = 400;
	public int maxValues = 12;

	public static final float MIN_TITLE_SIZE = 10;
	public static final float MAX_TITLE_SIZE = 20;
	private float titleTextSize = 15;

	public float getTitleTextSize () {
		return (screen == null) ? titleTextSize : titleTextSize * screen.density;
	}

	public static final float MIN_LABEL_SIZE = 5;
	public static final float MAX_LABEL_SIZE = 15;
	private float labelTextSize = 10;

	public float getLabelTextSize () {
		return (screen == null) ? labelTextSize : labelTextSize * screen.density;
	}

	public int dataColor = 0xFF1cb4e7;
	public int textColor = 0xFFFFFFFF;
	public int marginColor = 0xFF000000;
	public int backgroundColor = 0x88000000;

	public boolean showGrid = true;
	public int gridColor = 0x88FFFFFF;

	public float barWidth = 0.7f;

	public static final float MIN_LINE_WIDTH = 1;
	public static final float MAX_LINE_WIDTH = 10;
	private float lineWidth = 2;

	public float getLineWidth () {
		float width = (screen == null) ? lineWidth : lineWidth * screen.density;
		return (width < 1) ? 1f : width;
	}


	public float xLabelSpacing = 1.2f;
	public float yLabelSpacing = 3.5f;	// spacing, as a multiple of label height

	private int yLabelPadding = 3;		// horizontal padding, in  pixels

	public float getYLabelPadding () {
		return (screen == null) ? yLabelPadding : yLabelPadding * screen.density;
	}



	// convenience functions for the preferences 
	public float getNormalizedTitleTextSize () {
		return (titleTextSize - MIN_TITLE_SIZE) / (MAX_TITLE_SIZE - MIN_TITLE_SIZE);
	}
	public void setNormalizedTitleTextSize (float size) {
		titleTextSize = MIN_TITLE_SIZE + ((MAX_TITLE_SIZE - MIN_TITLE_SIZE) * size);
	}

	public float getNormalizedLabelTextSize () {
		return (labelTextSize - MIN_LABEL_SIZE) / (MAX_LABEL_SIZE - MIN_LABEL_SIZE);
	}
	public void setNormalizedLabelTextSize (float size) {
		labelTextSize = MIN_LABEL_SIZE + ((MAX_LABEL_SIZE - MIN_LABEL_SIZE) * size);
	}

	public float getNormalizedLineWidth () {
		return (lineWidth - MIN_LINE_WIDTH) / (MAX_LINE_WIDTH - MIN_LINE_WIDTH);
	}
	public void setNormalizedLineWidth (float width) {
		lineWidth = MIN_LINE_WIDTH + ((MAX_LINE_WIDTH - MIN_LINE_WIDTH) * width);
	}






	// some quicky utilities to toss the styles back and forth

	public String toJSON () {
		try {
			JSONObject json = new JSONObject();
			json.put("graphType", graphType.name() );
			json.put("titlePosition", titlePosition.name() );
			json.put("labelShow", labelShow.name() );
			json.put("titleTextSize", titleTextSize );
			json.put("labelTextSize", labelTextSize );
			json.put("dataColor", dataColor );
			json.put("textColor", textColor );
			json.put("marginColor", marginColor );
			json.put("backgroundColor", backgroundColor );
			json.put("barWidth", barWidth );
			json.put("lineWidth", lineWidth );
			return json.toString();
		}
		catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean validateJSON (String clip) {
		try {
			JSONObject json = new JSONObject(clip);
			return json.has("graphType") && json.has("titlePosition") && 
					json.has("labelShow") && json.has("titleTextSize") && 
					json.has("labelTextSize") && json.has("dataColor") &&
					json.has("textColor") && json.has("marginColor") &&
					json.has("backgroundColor") && json.has("barWidth") &&
					json.has("lineWidth");
		}
		catch (JSONException e) {
			return false;
		}
	}

	public void applyJSON (String clip) {
		try {
			JSONObject json = new JSONObject(clip);

			// temp objects first - if ONE fails, NONE will happen
			GraphType _graphType = Enum.valueOf(GraphType.class, json.getString("graphType") );
			TitlePosition _titlePosition = Enum.valueOf(TitlePosition.class, json.getString("titlePosition") );
			LabelDisplay _labelShow = Enum.valueOf(LabelDisplay.class, json.getString("labelShow") );
			float _titleTextSize = (float) json.getDouble("titleTextSize");
			float _labelTextSize = (float) json.getDouble("labelTextSize");
			int _dataColor = json.getInt("dataColor");
			int _textColor = json.getInt("textColor");
			int _marginColor = json.getInt("marginColor");
			int _backgroundColor = json.getInt("backgroundColor");
			float _barWidth = (float) json.getDouble("barWidth");
			float _lineWidth = (float) json.getDouble("lineWidth");

			graphType = _graphType;
			titlePosition = _titlePosition;
			labelShow = _labelShow;
			titleTextSize = _titleTextSize;
			labelTextSize = _labelTextSize;
			dataColor = _dataColor;
			textColor = _textColor;
			marginColor = _marginColor;
			backgroundColor = _backgroundColor;
			barWidth = _barWidth;
			lineWidth = _lineWidth;
		}
		catch (JSONException e) {
			throw new IllegalArgumentException(e);
		}
	}














	// DATABASE INTERFACE STUFF

    protected long _id;

    public long getID () {
        return _id;
    }

    public void setID (long id) {
        _id = id;
    }

	public static final String[] COLUMN_NAMES = new String[] {
		BaseColumns._ID, 
		"graphType", "titlePosition", "labelShow", "maxValues", "titleTextSize", "labelTextSize", 
		"dataColor", "textColor", "marginColor", "backgroundColor", "barWidth", "lineWidth"
	};
	public static final String[] COLUMN_TYPES = new String[] {
		Database.TableSchema.ID_TYPE_DECLARATION, 
		"text", "text", "text", "integer", "real", "real", 
		"integer", "integer", "integer", "integer", "real", "real"
	};

	public ContentValues getValues () {
	    ContentValues values = new ContentValues();
		values.put( BaseColumns._ID, _id );
	    values.put( "graphType", graphType.name() );
	    values.put( "titlePosition", titlePosition.name() );
	    values.put( "labelShow", labelShow.name() );		
		values.put( "maxValues", maxValues );
		values.put( "titleTextSize", titleTextSize );
		values.put( "labelTextSize", labelTextSize );
		values.put( "dataColor", dataColor );
		values.put( "textColor", textColor );
		values.put( "marginColor", marginColor );
		values.put( "backgroundColor", backgroundColor );
		values.put( "barWidth", barWidth );
		values.put( "lineWidth", lineWidth );
	    return values;
	}

    public void setValues (Cursor values) {
    	_id = values.getLong(0);

    	try { graphType = Enum.valueOf(GraphType.class, values.getString(1)); } 
    	catch (IllegalArgumentException e) { graphType = GraphType.BAR; }    		

		try { titlePosition = Enum.valueOf(TitlePosition.class, values.getString(2)); } 
		catch (IllegalArgumentException e) { titlePosition = TitlePosition.AUTO; }

		try { labelShow = Enum.valueOf(LabelDisplay.class, values.getString(3)); } 
		catch (IllegalArgumentException e) { labelShow = LabelDisplay.ALL; }

		maxValues = values.getInt(4);
		titleTextSize = values.getFloat(5);
		labelTextSize = values.getFloat(6);
		dataColor = values.getInt(7);
		textColor = values.getInt(8);
		marginColor = values.getInt(9);
		backgroundColor = values.getInt(10);
		barWidth = values.getFloat(11);
		lineWidth = values.getFloat(12);
    }

    public Bundle bundle () {
        Bundle values = new Bundle();
		values.putLong( BaseColumns._ID, _id );
	    values.putString( "graphType", graphType.name() );
	    values.putString( "titlePosition", titlePosition.name() );
	    values.putString( "labelShow", labelShow.name() );		
		values.putInt( "maxValues", maxValues );
		values.putFloat( "titleTextSize", titleTextSize );
		values.putFloat( "labelTextSize", labelTextSize );
		values.putInt( "dataColor", dataColor );
		values.putInt( "textColor", textColor );
		values.putInt( "marginColor", marginColor );
		values.putInt( "backgroundColor", backgroundColor );
		values.putFloat( "barWidth", barWidth );
		values.putFloat( "lineWidth", lineWidth );
        return values;
    }

	public void setValues (Bundle values) {
    	_id = values.getLong(BaseColumns._ID);

    	try { graphType = Enum.valueOf(GraphType.class, values.getString("graphType")); } 
    	catch (IllegalArgumentException e) { graphType = GraphType.BAR; }    		

		try { titlePosition = Enum.valueOf(TitlePosition.class, values.getString("titlePosition")); } 
		catch (IllegalArgumentException e) { titlePosition = TitlePosition.AUTO; }

		try { labelShow = Enum.valueOf(LabelDisplay.class, values.getString("labelShow")); } 
		catch (IllegalArgumentException e) { labelShow = LabelDisplay.ALL; }

		maxValues = values.getInt("maxValues");
		titleTextSize = values.getFloat("titleTextSize");
		labelTextSize = values.getFloat("labelTextSize");
		dataColor = values.getInt("dataColor");
		textColor = values.getInt("textColor");
		marginColor = values.getInt("marginColor");
		backgroundColor = values.getInt("backgroundColor");
		barWidth = values.getFloat("barWidth");
		lineWidth = values.getFloat("lineWidth");
    }





}