package com.awsite;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class SocketSender extends Thread {

    Alloy a;
    String ip;
    int port;
    boolean serverInitialized = false;

    boolean hasConverged = false;
    public double[][] outgoingEdges;
    public double[][] newEdges;

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public SocketSender(Alloy a, String ip, int port){
        outgoingEdges = new double[][]{null, null};
        this.a = a;
        this.port = port;
        this.ip = ip;}

    // send request, wait for reply, record response
    public void run(){

        // connect to other device
        try {connect(ip, port);}
        catch (IOException e) {e.printStackTrace();}

        // send data
        int type = a.subRegionType;
        if ((serverInitialized) && (type!=-1)) type = type+3;
        if ((serverInitialized) && (type==-1)) type = -2;
        serverInitialized = true;
        try {
            String response = send(type);
            //System.out.println("Got:" + response);
            parseResponse(response, type);
        } catch (IOException e) {e.printStackTrace();}

        // exit socket connection
        try {exitConnection();}
        catch (IOException e) {e.printStackTrace();}}

    // send request to worker
    private String send(int type) throws IOException {

        // send initial data to build the alloy
        StringBuilder msg;
        if (type == 10) msg = new StringBuilder(type);

        else if ((type <= 2)&&(type>=-1)){
            msg = new StringBuilder(type + " " + a.width + " " + a.height + " ");
            for (int w=0; w<a.width; w++){
                for (int h=0; h<a.height; h++){
                    msg.append(a.metalTemperatures[w][h]).append(" ");
                    msg.append(a.metalPercentages[w][h][0]).append(" ");
                    msg.append(a.metalPercentages[w][h][1]).append(" ");}}}

        // send new edge data, return calculated changes
        else {
            msg = new StringBuilder(type + " " + a.height + " ");
            if (outgoingEdges != null){
                for (double[] edge: outgoingEdges){
                    if (edge == null) continue;
                    for (double val: edge){
                        msg.append(val).append(" ");}}}}

        out.println(msg);
        return in.readLine();

    }

    // record new edge data from worker
    private void parseResponse(String input, int type){

        //System.out.println(input);
        // put string into double array
        ArrayList<Double> inputVals = new ArrayList<>();
        StringBuilder num = new StringBuilder();
        for(char c: input.toCharArray()){
            if (c == ' '){
                inputVals.add(Double.parseDouble(num.toString()));
                num = new StringBuilder();
            } else {num.append(c);}}
        if (!num.toString().equals("")) inputVals.add(Double.parseDouble(num.toString()));

        // record convergence
        if (inputVals.get(0).intValue() == 1) hasConverged = true;
        int len = inputVals.get(1).intValue();

        // read final output into a
        if(inputVals.get(0) == 13){
            int  k=3;
            int height = inputVals.get(2).intValue();
            double[][] out = new double[len][height];
            for (int w=0; w<len; w++){
                for (int h=0; h<height; h++){
                    out[w][h] = inputVals.get(k);
                    k++;}}
            a.metalTemperatures = out.clone();
            return;}

        // pull new edge data from response
        newEdges = new double[][]{null, null};
        int i=0, k=0;
        inputVals.remove(0);
        inputVals.remove(0);
        for (double d: inputVals){
            if(k==0) newEdges[i] = new double[len];
            newEdges[i][k] = d;
            k++;
            if (k==len){ k=0; i++;}}
        if(inputVals.size() == len) newEdges[1] = null;
        if(type==3){
            newEdges[1] = newEdges[0].clone();
            newEdges[0] = null;}
    }

    // connect to the worker
    private void connect(String ip, int port) throws IOException {
        clientSocket = new Socket(ip, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    // disconnect when done with request
    private void exitConnection() throws IOException {
        in.close();
        out.close();
        clientSocket.close();}

}

