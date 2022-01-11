package com.gewuwo.logging.client;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 发送客户端枚举
 *
 * @author jishan.guo
 * @version 1.0
 * @since 2022/1/10 3:23 下午
 */
public enum ClientEnum {

    /**
     * 发送飞书
     */
    FEI_SHU("feishu", FeiShuClient.class.getName()),

    /**
     * 发送tracker服务端
     */
    TRACKER_SERVER("tracker-server", LogTrackerServerClient.class.getName());

    public final static Map<String, ClientEnum> CLIENT_MAP;

    private final String value;

    private final String clientClassName;

    static {
        CLIENT_MAP = Arrays.stream(ClientEnum.values())
            .collect(Collectors.toMap(ClientEnum::getValue, Function.identity()));
    }


    ClientEnum(String value, String clientClassName) {
        this.value = value;
        this.clientClassName = clientClassName;
    }

    public String getValue() {
        return value;
    }

    public String getClientClassName() {
        return clientClassName;
    }

}
