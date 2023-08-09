package com.ruoyi.common.utils.security;

import javax.crypto.KeyGenerator;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

/**
 * 对称密钥密码算法工具类
 *
 * @author ruoyi
 */
public class CipherUtils
{
    /**
     * 生成随机秘钥
     *
     * @param keyBitSize 字节大小
     * @param algorithmName 算法名称
     * @return 创建密匙
     */
    public static Key generateNewKey(int keyBitSize, String algorithmName)
    {
        KeyGenerator kg;
        try
        {
            kg = KeyGenerator.getInstance(algorithmName);
        }
        catch (NoSuchAlgorithmException e)
        {
            String msg = "Unable to acquire " + algorithmName + " algorithm.  This is required to function.";
            throw new IllegalStateException(msg, e);
        }
        kg.init(keyBitSize);
        return kg.generateKey();
    }
}
