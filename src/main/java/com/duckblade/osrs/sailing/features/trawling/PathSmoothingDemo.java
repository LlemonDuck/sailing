package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.features.trawling.ShoalPathData.*;

/**
 * Demo class to test path smoothing on existing routes.
 * Run this to see the potential improvements from applying path smoothing
 * to the 6 existing routes in the ShoalPathData folder.
 */
public class PathSmoothingDemo {

    public static void main(String[] args) {
        System.out.println("=== Path Smoothing Analysis for Existing Routes ===\n");

        // Analyze all existing routes
        analyzeRoute("HalibutPortRoberts", HalibutPortRoberts.INSTANCE.getWaypoints());
        analyzeRoute("HalibutSouthernExpanse", HalibutSouthernExpanse.INSTANCE.getWaypoints());
        analyzeRoute("BluefinBuccaneersHaven", BluefinBuccaneersHaven.INSTANCE.getWaypoints());
        analyzeRoute("BluefinRainbowReef", BluefinRainbowReef.INSTANCE.getWaypoints());
        analyzeRoute("MarlinWeissmere", MarlinWeissmere.INSTANCE.getWaypoints());
        analyzeRoute("MarlinBrittleIsle", MarlinBrittleIsle.INSTANCE.getWaypoints());

        System.out.println("=== Analysis Complete ===");
        System.out.println("To apply smoothing, copy the generated code from the output above");
        System.out.println("and replace the WAYPOINTS arrays in the respective route files.");
    }

    private static void analyzeRoute(String routeName, ShoalWaypoint[] waypoints) {
        System.out.println("--- " + routeName + " ---");
        
        // Print analysis
        String analysis = PathSmoothingUtil.analyzePath(waypoints);
        System.out.println(analysis);
        
        // Generate smoothed code
        String smoothedCode = PathSmoothingUtil.generateSmoothedCode(waypoints, routeName);
        System.out.println("Smoothed code for " + routeName + ":\n" + smoothedCode);
        
        System.out.println(""); // Empty line for readability
    }
}