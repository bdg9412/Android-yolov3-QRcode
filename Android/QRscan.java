package com.example.yolo_qr;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.StringTokenizer;

public class QRscan extends AppCompatActivity {
    //토큰화에 사용할 변수
    private String temp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscan);

        //스캔을 위한 코드
        new IntentIntegrator(this).initiateScan();
    }
    //스캔결과값 받아오는 코드
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
                // todo
            } else {
                //QR코드 인식값 저장
                String uri =result.getContents();
                System.out.println(uri);
                //주소의 형태를 띄는 값인지 알기 위해 토큰화 진행
                StringTokenizer token =new StringTokenizer(uri,":",true);

                //token의 첫번째 값을 temp변수에 할당
                temp =token.nextToken();
                System.out.println(temp);
                //첫번째 토큰이 'http'로 시작하면 주소를 나타내는 uri값이니 웹페이지로 이동진행
                if(temp.equals(("http"))){
                    //인식한 QR코드를 바탕으로 해당 웹페이지 이동
                    Intent intent_action = new Intent(Intent.ACTION_VIEW, Uri.parse(result.getContents()));
                    startActivity(intent_action);
                }
                //만약 토크이 http로 시작하지 않는다면 주소를 나타내는 uri가 아니니 단순 띄워주기 진행
                else{
                    Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                }

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
