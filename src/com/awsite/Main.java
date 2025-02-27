package com.awsite;

import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Phaser;

public class Main {

    public static final int size = 16*3; // must be divisible by 4
    public static final int workerCount = 3;
    public static final int[] heat = {500000, 1000}; // heat to be applied
    public static final double[] heatCoefficients = {0.75, 1.0, 1.25};
    public static final String[] ips = {"0.0.0.0", "gee.cs.oswego.edu", "rho.cs.oswego.edu"};
    public static final int port = 6778;

    /*
     ssh gee; ssh rho
     java -jar /home/awhite26/dev/csc375/a3/csc375a3worker/csc375a3server2.jar
     */

    public static void main(String[] args) {
        //GUI gui = new GUI(size, size/4);

        // start a local worker
        Thread localWorker = new Thread(new SocketHost());
        localWorker.start();

        // subdivide the alloy according to number of devices available
        Alloy a = new Alloy(true, size, size/4);
        Thread[] tasks = new Thread[workerCount];
        int smallGap = size/workerCount, left=0, right;
        a.subRegionType = -1;
        for (int i=0; i<workerCount; i++){

            if ((i==0)||(i==workerCount-1)) right = left+smallGap;
            else right = left+smallGap+1;

            System.out.println(left + " " + right  );
            tasks[i] = a.getSubRegion(left, right);
            System.out.println(left + " " + right + " " + ((Alloy)tasks[i]).subRegionType + " " + ((Alloy)tasks[i]).width );
            left = right-1;
        }

        // apply heat
        ((Alloy)tasks[0]).metalTemperatures[0][0] = heat[0];
        ((Alloy)tasks[workerCount-1]).metalTemperatures[smallGap][(size/4)-1] = heat[1];

        // connect to and setup worker devices
        Thread[] largeTasks = new Thread[workerCount];
        for (int i=0; i<workerCount; i++){
            largeTasks[i] = new SocketSender((Alloy)tasks[i], ips[i], port);}
        doTasks(largeTasks);
        System.out.println("done with startup");

        // main loop
        int count = 0;
        while (true){

            // do task
            doTasks(largeTasks);

            // do edge swap
            for (int i=0; i<largeTasks.length-1; i++){
                ((SocketSender)largeTasks[i+1]).outgoingEdges[0] = ((SocketSender)largeTasks[i]).newEdges[1].clone();
                ((SocketSender)largeTasks[i]).outgoingEdges[1] = ((SocketSender)largeTasks[i+1]).newEdges[0].clone();}


            // look for convergence
            boolean con = true;
            for (Thread alloyChunk: largeTasks){
                if (!((SocketSender)alloyChunk).hasConverged ) {
                    con = false;
                    break;}}

            // exit conditions
            if (con) break;
            if (count >= 10000) break;
            count++;
            //System.out.println(count);
        }

        System.out.println(count);

        // send final data request
        for(Thread t: largeTasks){
            ((SocketSender)t).a.subRegionType = 10;}
        doTasks(largeTasks);

        // re-build alloy
        double[][] allTemps = new double[a.width][a.height];
        int start, end, offset = 0;
        for (int t=0; t<workerCount; t++){
            start = 1;
            end = ((SocketSender)largeTasks[t]).a.width-1;
            if (t == 0) start = 0;
            if (t == workerCount-1) end=end+1;
            for (int i=start; i<end; i++){
                allTemps[offset] = ((SocketSender)largeTasks[t]).a.metalTemperatures[i].clone();
                ++offset;}}
        a.metalTemperatures = allTemps.clone();
        System.out.println("\n");
        StringBuilder out = new StringBuilder("{");
        for(int j=0; j<allTemps.length; j++) {
            out.append("{ ");
            for (int i=0; i<allTemps[j].length; i++){
                out.append(allTemps[j][i]);
                if (i != allTemps[j].length-1) out.append(", ");}
                out.append("}");
            if (j != allTemps.length-1) out.append(", ");}
        out.append("}");
        System.out.println(out);}

    // start everything, do task, wait, come back here when done
    static void doTasks(Runnable[] tasks) {
        Phaser phaser = new Phaser(1);
        for (Runnable task : tasks) {
            phaser.register();
            new Thread(() -> {
                phaser.arriveAndAwaitAdvance();
                task.run();
                phaser.arriveAndDeregister();
            }).start();}
        phaser.arriveAndAwaitAdvance();
        phaser.arriveAndDeregister();
        phaser.register();
        while (!phaser.isTerminated() && (phaser.getRegisteredParties() != 1)){
            phaser.arriveAndAwaitAdvance();}
        phaser.arriveAndDeregister();}

}