package detection;

public class FlowDetector {

    private double weightTyping = 0.30;
    private double weighterr = 0.25;
    private double weightFocus = 0.20;
    private double weightBuilds = 0.15;
    private double weightActivity = 0.10;


    //aove is just the cateogry weights, below will be all thresholds now

    private int kpmlow = 20;
    private int kpmoptimal = 80;
    private int kpmhigh = 150;

    private int filechangetolerance =5;
    private int focuslosstolerance =3;

    private int syntaxerrtolerance = 3;
    private int compilationerrtolerance = 2;

    private int buildfailtolerance = 2;

    private double flowthreshold = 0.65;
    private double procrastinatethreshold =0.35; 
}
