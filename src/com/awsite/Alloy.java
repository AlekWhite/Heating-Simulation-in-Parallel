package com.awsite;

import java.util.concurrent.ThreadLocalRandom;

public class Alloy extends Thread {

    double[][][] metalPercentages;
    double[][] metalTemperatures;
    final int height;
    final int width;
    boolean hasConverged;
    int subRegionType = 1; // 0-leftmost-edge 1-middle-subregion 2-rightmost-edge

    // generate a new alloy
    public Alloy(boolean build, int width, int height){
        this.height = height;
        this.width = width;
        metalPercentages = new double[width][height][3];
        metalTemperatures = new double[width][height];

        if (!build) return;
        for (int w=0; w<width; w++){
            for (int h=0; h<height; h++){

                // generate random percentages for each metal type
                double[] percents = new double[3];
                int q = -1;
                if (ThreadLocalRandom.current().nextBoolean()) q=1;
                percents[0] = (double)(33+q*ThreadLocalRandom.current().nextInt(0, 16))/100;
                if (ThreadLocalRandom.current().nextBoolean()) q=-1*q;
                percents[1] = (double)(33+q*ThreadLocalRandom.current().nextInt(0, 16))/100;
                percents[2] = 1 - (percents[0] + percents[1]);
                if (percents[0] + percents[1] + percents[2] != 1) System.out.println(percents[0] + " " + percents[1] + " " + percents[2]);

                // add data to fields
                metalTemperatures[w][h] = 20.0;
                metalPercentages[w][h] = percents.clone();}}
    }

    // apply heat relaxation formal
    public void computeNewTemps(){
        double[][] newTemps =  new double[width][height];
        int start = 1, end = width-1;

        if (subRegionType == 0) {start = 0;}
        if (subRegionType == 2) {end = end+1;}
        newTemps[width-1] = metalTemperatures[width-1].clone();
        newTemps[0] = metalTemperatures[0].clone();

        for (int w=start; w<end; w++){
            for (int h=0; h<height; h++){
                int[][] neighbors = {{w, h}, {w, h+1}, {w, h-1}, {w+1, h}, {w-1, h}};
                int maxNeighbors = neighbors.length;
                double temp = 0;
                double[] localTemps = new double[maxNeighbors];
                double[][] localPercents = new double[maxNeighbors][3];
                int realNeighborsCount = 0;

                // get values from neighbors
                for (int[] neighbor : neighbors) {
                    if ((neighbor[0] <= -1) || (neighbor[1] <= -1)) continue;
                    if ((neighbor[0] >= width) || (neighbor[1] >= height)) continue;
                    int[] ind = {neighbor[0], neighbor[1]};
                    localTemps[realNeighborsCount] = metalTemperatures[ind[0]][ind[1]];
                    localPercents[realNeighborsCount] = metalPercentages[ind[0]][ind[1]].clone();
                    realNeighborsCount++;}

                // calculate new temperature
                for (int i=0; i<3; i++){
                    double val = 0;
                    for (int j=0; j<realNeighborsCount; j++){
                        val = val + ((localTemps[j]-metalTemperatures[w][h])*localPercents[j][i]);}
                    val = val / realNeighborsCount;
                    temp += Main.heatCoefficients[i]*val;}
                newTemps[w][h] = temp+metalTemperatures[w][h];}}

        // look for convergence
        metalTemperatures = newTemps.clone();
        double avg = 0;
        for (double[] k: metalTemperatures){
            for (double v: k){
                avg += v;}}
        avg = avg/(width*height);
        double deviation = 0;
        for (double[] k: metalTemperatures){
            for (double v: k){
                deviation += Math.pow(v-avg, 2);}}
        deviation = Math.sqrt(deviation/(width*height));
        hasConverged = deviation < 1;

    }

    // divides the alloy into vertical strips of the given width
    public Alloy getSubRegion(int leftBoundInclusive, int rightBoundInclusive){
        int srt=1, outs_width = rightBoundInclusive-leftBoundInclusive+1;
        if (((subRegionType==0)||(subRegionType==-1))&&(leftBoundInclusive == 0)) srt = 0;
        if (((subRegionType==2)||(subRegionType==-1))&&(rightBoundInclusive == width-1)) srt = 2;
        //if (srt == 1) outs_width = outs_width-1;

        Alloy out = new Alloy(false, outs_width, height);
        out.subRegionType = srt;
        for (int i=0; i<outs_width; i++){
            out.metalTemperatures[i] = metalTemperatures[i+leftBoundInclusive].clone();
            out.metalPercentages[i] = metalPercentages[i+leftBoundInclusive].clone();}
        return out;}

    public void run(){
        computeNewTemps();}

    public String toString(){
        StringBuilder out = new StringBuilder();
        for (int h=0; h<height; h++){
            for (int w=0; w<width; w++){
                out.append(metalTemperatures[w][h]).append(" ");}
            out.append("\n");}
        return out.toString();}
}