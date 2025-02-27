package com.awsite;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


public class GUI extends JFrame {

    int width, height;
    Color[] temperatureGradient;
    ArrayList<ArrayList<JPanel>> frames;

    public GUI(int width, int height){
        this.width = width;
        this.height = height;
        buildColors();

        // main stuff
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        this.setLayout(null);
        this.setTitle("CSC375-AW");
        this.setSize(800, 500);
        this.getContentPane().setBackground(new Color(0x353836));
        this.setVisible(true);


        // add frames
        int size = 600/width;
        frames = new ArrayList<>();
        for (int w=0; w<width; w++){
            ArrayList<JPanel> f1 = new ArrayList<>();
            for (int h=0; h<height; h++){
                JPanel r = new JPanel();
                r.setBounds(w*size+100, h*size+100, size, size);
                r.setBackground(temperatureGradient[12]);
                r.setBorder(BorderFactory.createLineBorder(Color.black));
                r.setVisible(true);
                this.add(r);
                f1.add(r);}
            frames.add(f1);
        }
        this.repaint();

    }

    // repaint frames
    public void render(double[][] mt){
        for (int w=0; w<width; w++){
            for (int h=0; h<height; h++){
                int t = ((int)mt[w][h] / 5)+10;
                if (t>=100) t=99;
                if (t<0) t=0;
                frames.get(w).get(h).setBackground(temperatureGradient[t]);}}}

    private void buildColors(){
        int numSteps = 100;
        Color[] temperatureGradient = new Color[numSteps];
        for (int i = 0; i < numSteps; i++) {
            float ratio = (float) i / (numSteps - 1);
            int red = (int) (255 * ratio);
            int blue = (int) (255 * (1 - ratio));
            temperatureGradient[i] = new Color(red, 0, blue);}
        this.temperatureGradient = temperatureGradient;}

}