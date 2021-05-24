package com.pingcap.test;

public class Main {

    private static TestDate td;

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]){
                case "IndexInfoJson":
                    td = new IndexInfoJson();
                    break;
                case "IndexInfoCSV":
                    td = new IndexInfoCSV();
                    break;
                case "TempIndexInfoJson":
                    td = new TempIndexInfoJson();
                    break;
                case "TempIndexInfoCSV":
                    td = new TempIndexInfoCSV();
                    break;
                case "CTSCSV":
                    td = new CTSCSV();
                    break;
                case "IndCredCSV":
                    td = new IndexInfoCSV();
                    break;
                default:

            }
            td.run();

        }


    }
}
