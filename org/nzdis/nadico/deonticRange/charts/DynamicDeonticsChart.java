package org.nzdis.nadico.deonticRange.charts;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.SpiderWebPlot;
import org.sofosim.environment.stats.charts.DeonticSpiderWebPlot;
import org.sofosim.environment.stats.charts.RadarChart;

import java.awt.*;

/**
 * Radar chart visualisation adapted for displaying Dynamic Deontics.
 * 
 * Use in simulation by calling getRadarChart(title, class of chart specialisation) 
 * and assign to radarChart field in Statistics.
 * 
 * @author cfrantz
 *
 */
public class DynamicDeonticsChart extends RadarChart{

	public DynamicDeonticsChart(String title) {
		super(title);
		plotImplementation = DeonticSpiderWebPlot.class;
	}
	
	@Override
	public void formatChart(JFreeChart chart, SpiderWebPlot plot){
		plot.setBackgroundPaint(Color.WHITE);
		plot.setLabelFont(labelFont.deriveFont(24f));
		addLegend();
		chart.getLegend().setItemFont(labelFont.deriveFont(18f));
	}

}
