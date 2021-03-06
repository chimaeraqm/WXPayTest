package com.crazydwarf.africatopup;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity
{
    private static final String ORDER_REQUEST_URL = "https://wx.dwarfworkshop.com/congo/wxAppPay.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermission();
        payV2();
    }

    /**
     * Alipay task
     */
    public void payV2()
    {
        /**
         * 这里只是为了方便直接向商户展示支付宝的整个支付流程；所以此处加签过程直接放在客户端完成；
         * 真实App里，privateKey等数据严禁放在客户端，加签过程务必要放在服务端完成；
         * 防止商户私密数据泄露，造成不必要的资金损失，及面临各种安全风险；
         *
         * orderInfo 的获取必须来自服务端；
         */
        showToast(this, "Pay $0.02 to Target Wechat account.");
//        sendOrderRequest("supersmashbros","0.01");
        sendOrderRequestMD5("devilmaycry","0.02");
    }

    void sendOrderRequestMD5(final String request_uid, String request_price)
    {
        String request_appid = "wxb4737b9a4e5b24b4";
        String request_mch_id = "1527548721";
        String request_notify_url = "https://wx.dwarfworkshop.com/congo/wxAppPay_Validate.php";
        final String request_key = "MIIEvwIBADANBgkqhkiG9w0BAQEFAASC";
//        String request_key = "MIIEvwIBADANBgkqhkiG9w0BAQEFAASC";

        RequestBody requestBody = new FormBody.Builder()
                .add("appid",request_appid)
                .add("mch_id",request_mch_id)
                .add("notify_url",request_notify_url)
                .add("key",request_key)
                .build();

        OkHttpClient okHttpClient = new OkHttpClient();

        Request request = new Request.Builder()
                .url(ORDER_REQUEST_URL)
                .post(requestBody)
                .build();

        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Toast.makeText(MainActivity.this, "fail to connect to sever", Toast.LENGTH_SHORT).show();
                    }
                });
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                String str = response.body().string();
                //向服务端请求并得到反馈信息，获取了appid、partnerId、prepayId
                try{
                    JSONObject jsonObject = new JSONObject(str);

                    //showToast(MainActivity.this, str);

                    String response_appid = jsonObject.getString("appid");
                    String response_partnerid = jsonObject.getString("partnerid");
                    String response_prepayid = jsonObject.getString("prepayid");
                    String response_package = jsonObject.getString("package");
                    String response_timestamp = jsonObject.getString("timestamp");
                    String response_noncestr = jsonObject.getString("noncestr");
                    String response_key = jsonObject.getString("sandbox_key");
                    String response_sign = jsonObject.getString("sign");
                    WXPayUtils.WXPayBuilder builder = new WXPayUtils.WXPayBuilder();
                    /*builder.setAppId(response_appid)
                            .setPartnerId(response_partnerid)
                            .setPrepayId(response_prepayid)
                            .setPackageValue(response_package)
                            .setTimeStamp(response_timestamp)
                            .setNonceStr(response_noncestr)
                            .build()
                            .toWXPayAndSign(MainActivity.this,response_appid,response_key);*/
                    builder.setAppId(response_appid)
                            .setPartnerId(response_partnerid)
                            .setPrepayId(response_prepayid)
                            .setPackageValue(response_package)
                            .setTimeStamp(response_timestamp)
                            .setNonceStr(response_noncestr)
                            .setSign(response_sign)
                            .build()
                            .toWXPayNotSign(MainActivity.this);
                } catch (JSONException e){
                    e.printStackTrace();
                }
            }
        });
    }

    private static void showToast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
    }

    //convert values in map to String
    private static StringBuilder getMapToString(Map<String,String> map)
    {
        Set<String> keySet = map.keySet();
        //将set集合转换为数组
        String[] keyArray = keySet.toArray(new String[keySet.size()]);
        //给数组排序(升序)
        Arrays.sort(keyArray);
        //因为String拼接效率会很低的，所以转用StringBuilder
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keyArray.length; i++) {
            // 参数值为空，则不参与签名 这个方法trim()是去空格
            if (map.get(keyArray[i]).trim().length() > 0) {
                sb.append(keyArray[i]).append("=").append(map.get(keyArray[i]).trim());
            }
            if(i != keyArray.length-1){
                sb.append("&");
            }
        }
        return sb;
    }

    /**
     * 获取权限使用的 RequestCode
     */
    private static final int PERMISSIONS_REQUEST_CODE = 1002;


    /**
     * 检查支付宝 SDK 所需的权限，并在必要的时候动态获取。
     * 在 targetSDK = 23 以上，READ_PHONE_STATE 和 WRITE_EXTERNAL_STORAGE 权限需要应用在运行时获取。
     * 如果接入支付宝 SDK 的应用 targetSdk 在 23 以下，可以省略这个步骤。
     */
    private void requestPermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, PERMISSIONS_REQUEST_CODE);

        } else {
            showToast(this, "支付宝 SDK 已有所需的权限");
        }
    }
}
