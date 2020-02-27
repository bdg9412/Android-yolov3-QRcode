package com.example.yolo_qr;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.speech.tts.TextToSpeech.ERROR;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;
    boolean startYolo = false;
    boolean firstTimeYolo = false;
    Net tinyYolo;

    //액티비티 변경 위한 버튼 사용 변수
    private Button QRscanBtn;
    //어플 종료 위한 버튼 사용 변수
    private Button END;


    public void YOLO(View Button){

        if (startYolo == false){

            startYolo = true;

            if (firstTimeYolo == false){

                AssetManager am =getResources().getAssets();

                firstTimeYolo = true;
                //opencv DNN.readNetFromDarknet을 사용하기 위해 string 인자를 두개(cfg,weight) 넘겨줘야 합니다.
                //getpath라는 임의의 함수를 이용하여 filepath를 string으로 저장합니다.
                String tinyYoloCfg = getPath("yolov3.cfg",this) ;
                String tinyYoloWeights = getPath("yolov3.weights",this);

                //opencv에서 제공하는 Dnn모델(Deep Neural Network)을 이용
                tinyYolo = Dnn.readNet(tinyYoloWeights,tinyYoloCfg);

                System.out.println(tinyYoloCfg);

            }
        }
        else{
            startYolo = false;
        }

    }

    //참고 https://park-duck.tistory.com/108?category=843507
    //qr코드 스캔을 위한 액티비티 변경
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.CameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);

        //https://copycoding.tistory.com/49
        //qr코드 스캔 버튼 설정
        //View 인자에 바로 리스너를 구현
        //setOnclickListenr를 통해 onClick이벤트 부여
        QRscanBtn = (Button) findViewById(R.id.QRscan);
        QRscanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //https://coding-factory.tistory.com/203
                //Intent를 이용하여 페이지 전환과 페이지간 데이터 전달을 구현
                //Intent란 앱 컴포넌트가 무엇을 할 것인지 담는 메시지 객체
                Intent intent = new Intent(MainActivity.this,QRscan.class);
                //satartActivity를 이용하여 액티비티를 전환하여 띄운다.
                //QRscan이라 만든 액티비티를 intent를 이용하여 띄운다.
                startActivity(intent);
            }
        });
        END =(Button)findViewById(R.id.END);
        END.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               finish();
            }
        });
        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);

                switch(status){

                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }


            }

        };

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //inputframe중 rgba format을 프레임변수에 할당
        Mat frame = inputFrame.rgba();

        if (startYolo == true) {
            //Imgproc을 이용해 이미지 프로세싱을 한다. rgba를 rgb로 컬러체계변환
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);

            //blob이란 input image가  mean subtraction, normalizing, and channel swapping을 거치고 난 후를 말합니다.
            //Dnn.blobFromImage를 이용하여 이미지 픽셀의 평균값을 계산하여 제외하고 스케일링을 하고 또 채널 스왑(RED와 BLUE)을 진행합니다.
            //현재는 128 x 128로 스케일링하고 채널 스왑은 하지 않습니다. 생성된 4-dimensional blob 값을 imageBlob에 할당합니다.
            //www.pyimagesearch.com/2017/11/06/deep-learning-opencvs-blobfromimage-works 참고하였습니다.
            Mat imageBlob = Dnn.blobFromImage(frame, 0.00392, new Size(128,128),new Scalar(0, 0, 0),/*swapRB*/false, /*crop*/false);


            tinyYolo.setInput(imageBlob);


            //cfg파일에서 yolo layer number를 확인하여 이를 순전파에 넣어줍니다.
            //yolov3의 경우 yolo layer가 3개임으로 initialCapacity를 3으로 줍니다.
            java.util.List<Mat> result = new java.util.ArrayList<Mat>(3);
            //List<String> outBlobNames = getOutputNames(tinyYolo);
            List<String> outBlobNames = new java.util.ArrayList<>();
            outBlobNames.add(0, "yolo_82");
            outBlobNames.add(1, "yolo_94");
            outBlobNames.add(2, "yolo_106");

            //순전파를 진행합니다.
            tinyYolo.forward(result,outBlobNames);

            //30%이상의 확률만 출력해주겠다.
            float confThreshold = 0.3f;

            List<Integer> clsIds = new ArrayList<>();
            List<Float> confs = new ArrayList<>();
            List<Rect> rects = new ArrayList<>();

            for (int i = 0; i < result.size(); ++i)
            {

                Mat level = result.get(i);

                for (int j = 0; j < level.rows(); ++j)
                {
                    Mat row = level.row(j);
                    Mat scores = row.colRange(5, level.cols());

                    Core.MinMaxLocResult mm = Core.minMaxLoc(scores);




                    float confidence = (float)mm.maxVal;


                    Point classIdPoint = mm.maxLoc;



                    if (confidence > confThreshold)
                    {
                        int centerX = (int)(row.get(0,0)[0] * frame.cols());
                        int centerY = (int)(row.get(0,1)[0] * frame.rows());
                        int width   = (int)(row.get(0,2)[0] * frame.cols());
                        int height  = (int)(row.get(0,3)[0] * frame.rows());


                        int left    = centerX - width  / 2;
                        int top     = centerY - height / 2;

                        clsIds.add((int)classIdPoint.x);
                        confs.add((float)confidence);




                        rects.add(new Rect(left, top, width, height));
                    }
                }
            }
            int ArrayLength = confs.size();

            if (ArrayLength>=1) {
                // Apply non-maximum suppression procedure.
                float nmsThresh = 0.2f;




                MatOfFloat confidences = new MatOfFloat(Converters.vector_float_to_Mat(confs));


                Rect[] boxesArray = rects.toArray(new Rect[0]);

                MatOfRect boxes = new MatOfRect(boxesArray);

                MatOfInt indices = new MatOfInt();



                Dnn.NMSBoxes(boxes, confidences, confThreshold, nmsThresh, indices);


                // Draw result boxes:
                int[] ind = indices.toArray();
                for (int i = 0; i < ind.length; ++i) {

                    int idx = ind[i];
                    Rect box = boxesArray[idx];

                    int idGuy = clsIds.get(idx);

                    float conf = confs.get(idx);

                    List<String> cocoNames = Arrays.asList("a person", "a bicycle", "a motorbike", "an airplane", "a bus", "a train", "a truck", "a boat", "a traffic light", "a fire hydrant", "a stop sign", "a parking meter", "a car", "a bench", "a bird", "a cat", "a dog", "a horse", "a sheep", "a cow", "an elephant", "a bear", "a zebra", "a giraffe", "a backpack", "an umbrella", "a handbag", "a tie", "a suitcase", "a frisbee", "skis", "a snowboard", "a sports ball", "a kite", "a baseball bat", "a baseball glove", "a skateboard", "a surfboard", "a tennis racket", "a bottle", "a wine glass", "a cup", "a fork", "a knife", "a spoon", "a bowl", "a banana", "an apple", "a sandwich", "an orange", "broccoli", "a carrot", "a hot dog", "a pizza", "a doughnut", "a cake", "a chair", "a sofa", "a potted plant", "a bed", "a dining table", "a toilet", "a TV monitor", "a laptop", "a computer mouse", "a remote control", "a keyboard", "a cell phone", "a microwave", "an oven", "a toaster", "a sink", "a refrigerator", "a book", "a clock", "a vase", "a pair of scissors", "a teddy bear", "a hair drier", "a toothbrush");

                    int intConf = (int) (conf * 100);

                    //opencv의 이미지 프로세싱을 진행합니다.
                    //putText를 이용하여 label의 이름을 입력하여 줍니다.
                    Imgproc.putText(frame,cocoNames.get(idGuy) + " " + intConf + "%",box.tl(),Core.FONT_HERSHEY_SIMPLEX, 2, new Scalar(255,255,0),2);

                    //위의 cocoNames 주석처리 임으로 밑의 코드로 변경하여 이름이 아닌 숫자로 구분하여 detection 확인
                    //Imgproc.putText(frame,idGuy + " " + intConf + "%",box.tl(),Core.FONT_HERSHEY_SIMPLEX, 2, new Scalar(255,255,0),2);


                    //opencv의 이미지 프로세싱을 진행합니다.
                    //rectangle을 이용하여 사각형을 그려줍니다.
                    Imgproc.rectangle(frame, box.tl(), box.br(), new Scalar(255, 0, 0), 2);


                }
            }
        }



        return frame;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {


        if (startYolo == true){

            String tinyYoloCfg = getPath("yolov3.cfg",this);
            String tinyYoloWeights = getPath("yolov3.weights",this);

            tinyYolo = Dnn.readNetFromDarknet(tinyYoloCfg, tinyYoloWeights);

        }



    }
    // Upload file to storage and return a path.
    // YOLO의 cfg와 weight를 불러오기 위한 코드입니다.
    // asset 폴더를 읽어오는 과정에서 string이 아닌 inputstream으로 받아오기에 이를 다시 string으로 변환해줍니다.
    // https://recipes4dev.tistory.com/125을 참고하여 asset폴더를 생성하고 yolo 모델 파일을 저장하였습니다.
    // https://docs.opencv.org/3.4/d0/d6c/tutorial_dnn_android.html에서 getpath함수를 참고하였습니다.
    private static String getPath(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream = null;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return "";
    }
    @Override
    public void onCameraViewStopped() {

    }


    @Override
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(),"There's a problem, yo!", Toast.LENGTH_SHORT).show();
        }

        else
        {
            baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS);
        }



    }
    @Override
    protected void onPause() {

        super.onPause();
        if(cameraBridgeViewBase!=null){

            cameraBridgeViewBase.disableView();
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase!=null){
            cameraBridgeViewBase.disableView();
        }
    }
}