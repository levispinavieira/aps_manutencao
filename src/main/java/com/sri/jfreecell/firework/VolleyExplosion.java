package com.sri.jfreecell.firework;

import java.awt.Color;
import java.awt.Graphics;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.pushingpixels.trident.Timeline;
import org.pushingpixels.trident.TimelineScenario;
import org.pushingpixels.trident.ease.Spline;

/**
 * Represents a whole explosion of firework.
 * 
 * @author Sateesh Gampala
 *
 */
public class VolleyExplosion {
    private int x;
    private int y;
    private Color color;
    private Set<SingleExplosion> circles;

    public VolleyExplosion(int x, int y, Color color) {
	this.x = x;
	this.y = y;
	this.color = color;
	this.circles = new HashSet<SingleExplosion>();
    }

    /**
     * Creates a timelined scenario for an explosion.
     * 
     * @return TimelineScenario
     */
    public TimelineScenario getExplosionScenario() {
	TimelineScenario scenario = new TimelineScenario.Parallel();

	Random randomizer = new Random();
	int duration = 1000 + randomizer.nextInt(1000);
	for (int i = 0; i < 18; i++) {
	    float dist = (float) (100 + 10 * Math.random());
	    float radius = (float) (2 + 2 * Math.random());
	    for (float delta = 0.6f; delta <= 1.0f; delta += 0.2f) {
		float circleRadius = radius * delta;

		double degrees = 20.0 * (i + Math.random());
		float radians = (float) (2.0 * Math.PI * degrees / 360.0);

		float initDist = delta * dist / 10.0f;
		float finalDist = delta * dist;
		float initX = (float) (this.x + initDist * Math.cos(radians));
		float initY = (float) (this.y + initDist * Math.sin(radians));
		float finalX = (float) (this.x + finalDist * Math.cos(radians));
		float finalY = (float) (this.y + finalDist * Math.sin(radians));

		SingleExplosion circle = new SingleExplosion(this.color, initX, initY, circleRadius);
		Timeline timeline = new Timeline(circle);
		timeline.addPropertyToInterpolate("x", initX, finalX);
		timeline.addPropertyToInterpolate("y", initY, finalY);
		timeline.addPropertyToInterpolate("opacity", 1.0f, 0.0f);
		timeline.setDuration(duration - 200 + randomizer.nextInt(400));
		timeline.setEase(new Spline(0.4f));

		synchronized (this.circles) {
		    circles.add(circle);
		}
		scenario.addScenarioActor(timeline);
	    }
	}
	return scenario;
    }

    public void paint(Graphics g) {
	synchronized (this.circles) {
	    for (SingleExplosion circle : this.circles) {
		circle.paint(g);
	    }
	}
    }
}