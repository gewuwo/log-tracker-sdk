package com.gewuwo.logging.util;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @program: fittime-user-boot
 * @description: ${description}
 * @author: tanshaohua
 * @date: 2021-12-28 14:38
 **/
public class SendFeiShuUtil {

    public static int sendFeishuMessage(String url, String content) {
        Content cc = new Content();
        cc.setText(content);
        Map<String,String> stringMap = new HashMap<>();
        stringMap.put("msg_type","text");
        stringMap.put("content", JSONObject.toJSONString(cc));

        try {
            String post = HttpClientUtils.post(url, stringMap);

            if(post != null && !"".equals(post)){
                JSONObject jsonObject = JSONObject.parseObject(post);
                if(jsonObject != null && jsonObject.size()>0){
                    Integer statusCode = jsonObject.getInteger("StatusCode");
                    if(statusCode.equals(0)){
                        return 1;
                    }
                }
            }
        }catch (Exception e){
        }
        return -1;
    }
}
