package com.bearpot.dgjung.contextawareness_demo.Tracking;

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by dg.jung on 2017-11-01.
 */

public class GetHospitalLocationHttp {

    // Thread로 웹서버에 접속
    /**
     * 서버에 검색 데이터를 요청하는 메소드
     * @param lat,lng,myRadius,name
     * @return
     */
    public String[][] SendByHttp(Double lat, Double lng, int myRadius, String name) {
        String URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";
        HttpGet get = new HttpGet(URL + "?location=" + lat + "," + lng
                + "&radius=" + myRadius
                + "&name=" + name
                + "&key=" + "AIzaSyAQMlpuPaFPa9gfSRqE6KJE4ThyhgEOeCs");

        Log.d("CONTEXT_AWARENESS", "Place API 이용 내역 : "+
                URL +
                "?location=" + lat + "," + lng
                + "&radius=" + myRadius
                + "&name=" + name
                + "&key=" + "AIzaSyAQMlpuPaFPa9gfSRqE6KJE4ThyhgEOeCs");

        DefaultHttpClient client = new DefaultHttpClient();

        String[][] HospitalInfo = new String[3][];
        try {
             /*    검색할 문자열 서버에 전송       */
             /* 데이터 보낸 뒤 서버에서 데이터를 받아오는 과정 */
            HttpResponse response = client.execute(get);
            HttpEntity resEntity = response.getEntity();
            String jsonString = EntityUtils.toString(resEntity);
            Log.d("json", jsonString);
            JSONObject jsonObject = new JSONObject(jsonString);
            String code = jsonObject.getString("status");
            Log.d("json", code);
            JSONArray jsonArray = jsonObject.getJSONArray("results");

            HospitalInfo= new String[3][jsonArray.length()+1];
            for (int i = 0; i < jsonArray.length(); i++) {
                HospitalInfo[0][i] = jsonArray.getJSONObject(i).getString("name");
                HospitalInfo[1][i] = String.valueOf(jsonArray.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getDouble("lat"));
                HospitalInfo[2][i] = String.valueOf(jsonArray.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getDouble("lng"));

                Log.d("json", i + "번째 : " + jsonArray.getJSONObject(i).getString("name") +
                        "---"+
                        jsonArray.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getDouble("lat") +
                        "," +
                        jsonArray.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getDouble("lng") );
            }

            return HospitalInfo;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("CONTEXT_AWARENESS", "ERROR: " + e.toString());
            client.getConnectionManager().shutdown();   // 연결 지연 종료
            return HospitalInfo;
        }
    }

}