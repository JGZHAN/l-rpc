package cn.jgzhan.lrpc.common.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/4
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RpcResponseMessage extends Message {

    {
        setType((byte) 2);
    }

    // 返回值
    private Object returnValue;

    // 异常值
    private Error exceptionValue;

}
