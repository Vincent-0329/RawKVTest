package com.pingcap.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class IndexInfoCSV  implements TestDate {
    static final Logger logger = LoggerFactory.getLogger("IndexInfoCSV");

    private static long nums;
    private static long startNums;
    private static long eachSlices;
    private static int wrongData;
    private static String filePathWithOutSuffix;

    static {
        try {
            Properties properties = new Properties();
            InputStream input = IndexInfoCSV.class.getClassLoader().getResourceAsStream("Test.properties");
            properties.load(input);
            nums = Long.valueOf(properties.getProperty("IndexInfoCSV.nums", "1000000"));
            startNums = Long.valueOf(properties.getProperty("IndexInfoCSV.startNums", "1000000"));
            eachSlices = Long.valueOf(properties.getProperty("IndexInfoCSV.eachSlices", "100000"));
            wrongData = Integer.parseInt(properties.getProperty("IndexInfoCSV.wrongData", "10000"));
            filePathWithOutSuffix = properties.getProperty("IndexInfoCSV.filePathWithOutSuffix", "/temIndexCSV");

            logger.info("nums: {} \n eachSilices: {} \n wrongData: {} \n filePathWithOutSuffix: {} \n ",
                    nums, eachSlices, wrongData, filePathWithOutSuffix);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static StringBuffer sb = new StringBuffer();

    private static AtomicInteger fileId = new AtomicInteger();

    public void run() {
        logger.info("Create Test Data begin ");
        long now = System.currentTimeMillis();
        try {
            ForkJoinPool pool = new ForkJoinPool();
            ForkJoinTask<Long> task = pool.submit(new ParallelExecuteCreateDataTask(startNums, nums));
            task.get();
            pool.shutdown();
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (Exception e) {
            System.out.println("create test data error {} " + e);
        }
        logger.info(" create test data end  size {} cost time {} ", nums, Long.valueOf(System.currentTimeMillis() - now));
        logger.info(sb.toString());
    }

    private static class ParallelExecuteCreateDataTask extends RecursiveTask<Long> {

        private static final long serialVersionUID = 1L;
        private static Random r = new Random();

        private long startValue;

        private long endValue;

        public ParallelExecuteCreateDataTask(long startValue, long endValue) {
            this.startValue = startValue;
            this.endValue = endValue;
        }

        protected Long compute() {
            if (endValue - startValue < IndexInfoCSV.eachSlices) {
                logger.debug(Thread.currentThread().getName() + " startValue {" + startValue + "} endValue {" + endValue + "} ");
                AtomicInteger WRONG = new AtomicInteger();
                try {
                    String fileFullPath = filePathWithOutSuffix + fileId.getAndAdd(1) + ".txt";
                    File file = getByName(fileFullPath);
                    StringBuilder s = new StringBuilder();
                    long i = startValue;
                    for (; i <= endValue; i++) {
                        int w_d = r.nextInt(wrongData);
                        if (w_d == 1) {
                            s.append("tempIndexWrongData \n");
                            WRONG.addAndGet(1);
                        } else {
                            s.append(""+i +
                                    "|APD0101|0025000##10##99##SP00001##1##                    ##                    ##" +
                                    "\n");
                        }
                        if (i % 10000 == 0) {
                            writeToFile(file, s.toString());
                            s = new StringBuilder();
                        }
                    }
                    writeToFile(file, s.toString());

                    sb.append("(" + fileFullPath + ") create test data end WRONG{" + WRONG + "}  \n");

                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println(" create test data parallelExecuteCreateDataTask exception {} " + e);
                    throw e;
                }
                return startValue;
            }
            ParallelExecuteCreateDataTask subTask1 = new ParallelExecuteCreateDataTask(startValue, (startValue + IndexInfoCSV.eachSlices - 1));
            subTask1.fork();
            ParallelExecuteCreateDataTask subTask2 = new ParallelExecuteCreateDataTask((startValue + IndexInfoCSV.eachSlices), endValue);
            subTask2.fork();
            return startValue;
        }
    }


    private synchronized static void writeToFile(File file, String str) {
        RandomAccessFile fout = null;
        FileChannel fcout = null;
        try {
            fout = new RandomAccessFile(file, "rw");
            long filelength = fout.length();//?????????????????????
            fout.seek(filelength);//????????????????????????????????????????????????
            fcout = fout.getChannel();//??????????????????
            FileLock flout = null;
            while (true) {
                try {
                    flout = fcout.tryLock();//????????????????????????????????????????????????????????????
                    break;
                } catch (Exception e) {
                    Thread.sleep(1000);
                }
            }
            fout.write(str.getBytes());//????????????????????????????????????

            flout.release();
            fcout.close();
            fout.close();

        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {

            if (fcout != null) {

                try {
                    fcout.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    fcout = null;
                }
            }
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    fout = null;
                }
            }
        }
    }

    private static File getByName(String path) {
        File file = new File(path);

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new IllegalArgumentException("create file failed", e);
            }
        }

        if (file.isDirectory()) {
            throw new IllegalArgumentException("not a file");
        }

        return file;
    }
}
