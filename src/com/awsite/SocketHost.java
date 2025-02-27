package com.awsite;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Phaser;

public class SocketHost implements Runnable {

    Alloy a;
    int workerCount = 8;
    Thread[] tasks = null;

    public void start1(int port) throws IOException {

        // accept new connections
        ServerSocket serverSocket = new ServerSocket(port);
        while (true){
            Socket clientSocket = serverSocket.accept();
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String input = in.readLine();
            //System.out.println("\n\nINP:");
            //System.out.println(input);
            if (input != null){

                // put string into double array
                ArrayList<Double> inputVals = new ArrayList<>();
                StringBuilder num = new StringBuilder();
                for(char c: input.toCharArray()){
                    if (c == ' '){
                        inputVals.add(Double.parseDouble(num.toString()));
                        num = new StringBuilder();
                    } else {num.append(c);}}
                if (!num.toString().equals("")) inputVals.add(Double.parseDouble(num.toString()));
                int type = inputVals.get(0).intValue();

                // send final alloy
                if (type >= 10){
                    StringBuilder msg = new StringBuilder(type + " " + a.width + " " + a.height + " ");
                    for (int w=0; w<a.width; w++){
                        for (int h=0; h<a.height; h++){
                            msg.append(a.metalTemperatures[w][h]).append(" ");}}
                    out.println(msg);}

                // initialise new alloy
                else if ((type <= 2) && (type > -2)){
                    int width = inputVals.get(1).intValue();
                    int height = inputVals.get(2).intValue();
                    a = new Alloy(false, width, height);
                    a.subRegionType = type;
                    tasks = null;

                    // populates temps and percents
                    int k=3;
                    for(int w=0; w<width; w++){
                        for (int h=0; h<height; h++){
                            a.metalTemperatures[w][h] = inputVals.get(k);
                            a.metalPercentages[w][h] = new double[]{inputVals.get(k+1),
                                    inputVals.get(k+2),
                                    1-(inputVals.get(k+1)+inputVals.get(k+2))};

                            k += 3;}}

                    //System.out.println(a);
                    out.println(runCalculation());
                    //System.out.println(a);
                    //System.out.println("type: " + a.subRegionType)
                    ;}

                // update existing data
                else {
                    int len = inputVals.get(1).intValue();

                    // pull new edge data from response
                    double[][] newEdges = new double[2][len];
                    inputVals.remove(0);
                    inputVals.remove(0);

                    if (!inputVals.isEmpty()){
                        int i=0, k=0;
                        for (double d: inputVals){
                            newEdges[i][k] = d;
                            k++;
                            if (k==len){ k=0; i++;}}

                        //if (inputVals.size() == len){ newEdges[1] = null; }

                        // puts new data into tasks
                        if (type-3==0) ((Alloy)tasks[workerCount-1]).metalTemperatures[
                                ((Alloy)tasks[workerCount-1]).metalTemperatures.length-1] = newEdges[0].clone();
                        else if (type-3==2) ((Alloy)tasks[0]).metalTemperatures[0] = newEdges[0].clone();
                        else {
                            ((Alloy)tasks[workerCount-1]).metalTemperatures[
                                    ((Alloy)tasks[workerCount-1]).metalTemperatures.length-1] = newEdges[0].clone();
                            ((Alloy)tasks[0]).metalTemperatures[0] = newEdges[1].clone();}}

                    // respond with new edge data
                    //System.out.println(a);
                    String res = runCalculation();
                    //System.out.println("Out: " + res);
                    out.println(res);}}

            // close connection
            in.close();
            out.close();
            clientSocket.close();}
    }


    private String runCalculation(){
        int subWidth = a.width/workerCount, left=0, right;
        //System.out.println(a.width);

        // subdivide the alloy according to number of workers
        if (tasks == null){
            //System.out.println(a.subRegionType);
            tasks = new Thread[workerCount];
            int len;
            for (int i=0; i<workerCount; i++){
                if (((i==0)&&(a.subRegionType==0))||((i==workerCount-1)&&(a.subRegionType==2))) len = subWidth+1;
                else len = subWidth+2;
                if (i==0) {
                    right = len-1;}
                else {
                    right = left+len-1;}
                tasks[i] = a.getSubRegion(left, right);
                //System.out.println(tasks[i]);
                //System.out.println(left + " " + right + " " + ((Alloy)tasks[i]).subRegionType);
                left = right-1;
            }}

        // wait for each worker complete its own task
        doTasks(tasks);

        // exchange edges internally
        for (int i=0; i<workerCount-1; i++){
            ((Alloy)tasks[i+1]).metalTemperatures[0] =
                    ((Alloy)tasks[i]).metalTemperatures[((Alloy)tasks[i]).width-2].clone();
            ((Alloy)tasks[i]).metalTemperatures[((Alloy)tasks[i]).width-1] =
                    ((Alloy)tasks[i+1]).metalTemperatures[1].clone();}

        // re-build alloy
        double[][] allTemps = new double[a.width][a.height];
        int start, end, offset = 0;
        for (int t=0; t<workerCount; t++){
            start = 1;
            end = ((Alloy)tasks[t]).width-1;
            if (t == 0) start = 0;
            if (t == workerCount-1) end=end+1;
            for (int i=start; i<end; i++){
                allTemps[offset] = ((Alloy)tasks[t]).metalTemperatures[i].clone();
                ++offset;}}
        a.metalTemperatures = allTemps.clone();

        // look for convergence
       int conv = 1;
        for (Thread subRegion: tasks){
            if (!((Alloy)subRegion).hasConverged) {
                conv = 0;
                break;}}

        // build outgoing data
        StringBuilder out = new StringBuilder(conv + " " + a.height + " ");
        if (a.subRegionType <= 1){
            for (double val: a.metalTemperatures[a.width-2]){
                out.append(val).append(" ");}}
        if (a.subRegionType >= 1){
            for (double val: a.metalTemperatures[1]){
                out.append(val).append(" ");}}
        return out.toString();}

    @Override
    public void run(){
        main(null);}

    // start worker
    public static void main(String[] args){
        SocketHost server = new SocketHost();
        System.out.println("StartingWorker");
        try { server.start1(6778);
        } catch (IOException e) {
            e.printStackTrace();}
        System.out.println("done");}

    // do task, wait, come back here when done
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