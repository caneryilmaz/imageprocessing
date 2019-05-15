package com.example.opencv;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class EdgeDetection extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private TesseractOCR mTessOCR;

    private TextView textView;
    //private final OcrDetectorListener ocrDetectorListener;
    private TextRecognizer textRecognizer;

    private GraphicOverlay<OcrGraphic> graphicOverlay;


    /*public EdgeDetection(@NonNull OcrDetectorProcessor.OcrDetectorListener ocrDetectorListener) {
        this.ocrDetectorListener = (OcrDetectorListener) ocrDetectorListener;
    }*/

    private TextView mTextView;
    private static Scalar CONTOUR_COLOR = null;
    private static double areaThreshold = 0.025; //threshold for the area size of an object
    private static final String TAG = "EdgeDetection";
    private CameraBridgeViewBase cameraBridgeViewBase;
    private CameraBridgeViewBase.CvCameraViewListener2 cameraViewListener;

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    cameraBridgeViewBase.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Nullable
    private AsyncTask<Void, Void, Boolean> textDetectAsyncTask;

    private void doOCR(final Bitmap bitmap) {
        String text = mTessOCR.getOCRResult(bitmap);
    }

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, EdgeDetection.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_open_cvcamera);
        cameraBridgeViewBase = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);
        graphicOverlay = (GraphicOverlay<OcrGraphic>) findViewById(R.id.graphicOverlay);
        textRecognizer = new TextRecognizer.Builder(this).build();
        mTessOCR = new TesseractOCR(this);
//        textView = findViewById(R.id.textView);

    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraBridgeViewBase != null)
            cameraBridgeViewBase.disableView();

        if (textDetectAsyncTask != null) {
            textDetectAsyncTask.cancel(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, baseLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase != null)
            cameraBridgeViewBase.disableView();

    }


    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    public interface OcrDetectorListener {
        //void onTextDetected(String chassis);
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat color = inputFrame.rgba();
        Mat edges = inputFrame.gray();
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.Canny(edges,edges,50,250);
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        double maxArea = -1;
        int maxAreaIdx = -1;

        //Null Check for contours
        if (contours != null) {
            if (!contours.isEmpty()) {
                MatOfPoint temp_contour = contours.get(0); //the largest is at the index 0 for starting point
                MatOfPoint2f approxCurve = new MatOfPoint2f();
                MatOfPoint largest_contour = contours.get(0);

                List<MatOfPoint> largest_contours = new ArrayList<MatOfPoint>();

                for (int idx = 0; idx < contours.size(); idx++) {
                    temp_contour = contours.get(idx);
                    double contourarea = Imgproc.contourArea(temp_contour);
                    //compare this contour to the previous largest contour found
                    if (contourarea > maxArea) {
                        //check if this contour is a square
                        MatOfPoint2f new_mat = new MatOfPoint2f(temp_contour.toArray());
                        int contourSize = (int) temp_contour.total();
                        MatOfPoint2f approxCurve_temp = new MatOfPoint2f();
                        Imgproc.approxPolyDP(new_mat, approxCurve_temp, contourSize * 0.05, true);
                        if (approxCurve_temp.total() == 4) {
                            maxArea = contourarea;
                            maxAreaIdx = idx;
                            approxCurve = approxCurve_temp;
                            largest_contour = temp_contour;
                        }
                    }
                }

                //Convert back to MatOfPoint
                final MatOfPoint points = new MatOfPoint(approxCurve.toArray());
                final Rect rect = Imgproc.boundingRect(points);

                textDetectAsyncTask = new TextDetectAsyncTask(contours, color, edges, rect).execute();


                //TESSERACT DENEMESİ
                /*Bitmap bitMap = null;
                StringBuilder sb = new StringBuilder();
                //crop = cameraViewListener.onCameraFrame(inputFrame);
                for(int j = 0; j < contours.size(); j++) {
                    try {
                        Point bottomRight = rect.br();
                        Point topLeft = rect.tl();
                        Mat subImage = color.submat((int) topLeft.y, (int) bottomRight.y, (int) topLeft.x, (int) bottomRight.x);
                        bitMap = Bitmap.createBitmap(subImage.width(), subImage.height(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(subImage, bitMap);
                    } catch (Exception e) {
                        Log.d(TAG,"Cropped part error");
                    }
                    if(bitMap != null) {
                        doOCR(bitMap);
                    }
                }

        /*for (int contourIdx=0; contourIdx < contours.size(); contourIdx++ ) {
            // Minimum size allowed for consideration
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(contourIdx).toArray());
            //Processing on mMOP2f1 which is in type MatOfPoint2f
            double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

        }
        /*Mat lines = new Mat();
        Mat edgesp = edges.clone();
        Imgproc.cvtColor(edges, edgesp, Imgproc.COLOR_GRAY2BGR);
        Imgproc.HoughLinesP(edges, lines, 1, Math.PI/180, 100, 50, 10); // runs the actual detection
        // Draw the lines
        for (int x = 0; x < lines.rows(); x++) {
            double[] l = lines.get(x, 0);
            Imgproc.line(edgesp, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);
        }*/

                Imgproc.rectangle(color, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(250, 0, 0, 255), 3);
                return color;

            } else {
                return color;
            }
        } else {
            return color;
        }

    }
    

    @SuppressLint("StaticFieldLeak")
    private class TextDetectAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private final List<MatOfPoint> contours;
        private final Rect rect;
        private final Mat color;
        private final Mat edges;

        TextDetectAsyncTask(final List<MatOfPoint> contours, final Mat color, final Mat edges, final Rect rect) {
            this.contours = contours;
            this.rect = rect;
            this.color = color;
            this.edges = edges;
        }

        @Override
        protected Boolean doInBackground(final Void... voids) {

            Imgproc.cvtColor(edges, edges, Imgproc.COLOR_BayerBG2RGB);

            //GMS VISION DENEMESİ
            /*for (int j = 0; j < contours.size(); j++) {
                Point bottomRight = rect.br();
                Point topLeft = rect.tl();
                Mat subImage = color.submat((int) topLeft.y, (int) bottomRight.y, (int) topLeft.x, (int) bottomRight.x);
                if (subImage.width() > 0 && subImage.height() > 0) {
                    Bitmap bitMap = Bitmap.createBitmap(subImage.width(), subImage.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(subImage, bitMap);
                    Frame frame = new Frame.Builder().setBitmap(bitMap).build();
                    graphicOverlay.clear();
                    SparseArray<TextBlock> items = textRecognizer.detect(frame);
                    if (items != null) {
                        int size = items.size();
                        for (int i = 0; i < size; ++i) {
                            TextBlock item = items.valueAt(i);
                            if (item != null) {
                                List<? extends Text> textComponents = item.getComponents();
                                //for (Text currentText : textComponents) {
                                //if (CollectionUtils.isNotEmpty(textComponents)) {
                                Log.i("OcrDetectorProcessor", "Text detected! " + textComponents.get(i));
                                OcrGraphic graphic = new OcrGraphic(graphicOverlay, item);
                                graphicOverlay.add(graphic);

                            }
                        }
                    }
                }*/
            //TESSERACT DENEMESİ
                Bitmap bitMap = null;
                StringBuilder sb = new StringBuilder();
                //crop = cameraViewListener.onCameraFrame(inputFrame);
                for(int j = 0; j < contours.size(); j++) {
                    try {
                        //Point bottomRight = rect.br();
                        //Point topLeft = rect.tl();
                        //Mat subImage = color.submat((int) topLeft.y, (int) bottomRight.y, (int) topLeft.x, (int) bottomRight.x);
                        bitMap = Bitmap.createBitmap(color.cols(), color.rows(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(color, bitMap);

                    } catch (Exception e) {
                        Log.d(TAG,"Cropped part error");
                    }
                    color.release();
                    if(bitMap != null) {
                        doOCR(bitMap);
                    }
                }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            // if you need some other type you'd like to use in UI change type
        }
    }


}

