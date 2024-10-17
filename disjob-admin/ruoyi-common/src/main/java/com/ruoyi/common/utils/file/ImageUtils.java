package com.ruoyi.common.utils.file;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.StringUtils;
import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

/**
 * 图片处理工具类
 *
 * @author ruoyi
 */
public class ImageUtils
{
    private static final Logger log = LoggerFactory.getLogger(ImageUtils.class);

    public static byte[] getImage(String imagePath)
    {
        InputStream is = getFile(imagePath);
        try
        {
            return IOUtils.toByteArray(is);
        }
        catch (Exception e)
        {
            log.error("图片加载异常 {}", e);
            return null;
        }
        finally
        {
            IOUtils.closeQuietly(is);
        }
    }

    public static InputStream getFile(String imagePath)
    {
        try
        {
            byte[] result = readFile(imagePath);
            result = Arrays.copyOf(result, result.length);
            return new ByteArrayInputStream(result);
        }
        catch (Exception e)
        {
            log.error("获取图片异常 {}", e);
        }
        return null;
    }

    /**
     * 读取文件为字节数据
     *
     * @param url 地址
     * @return 字节数据
     */
    public static byte[] readFile(String url)
    {
        InputStream in = null;
        try
        {
            if (url.startsWith("http"))
            {
                // 网络地址
                URL urlObj = new URL(url);
                URLConnection urlConnection = urlObj.openConnection();
                urlConnection.setConnectTimeout(30 * 1000);
                urlConnection.setReadTimeout(60 * 1000);
                urlConnection.setDoInput(true);
                in = urlConnection.getInputStream();
            }
            else
            {
                // 本机地址
                String localPath = RuoYiConfig.getProfile();
                String downloadPath = localPath + StringUtils.substringAfter(url, Constants.RESOURCE_PREFIX);
                in = new FileInputStream(downloadPath);
            }
            return IOUtils.toByteArray(in);
        }
        catch (Exception e)
        {
            log.error("获取文件路径异常 {}", e);
            return null;
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }

    public static void createImage(String text, OutputStream output, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // 设置背景色
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // 自定义字体 Courier
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 25);
        g2d.setFont(font);

        // 最大文本行宽度
        int maxLineWidth = width - 60;
        // 单行文本高度
        int lineHeight = 40;

        FontRenderContext frc = g2d.getFontRenderContext();
        TextLayout textLayout = new TextLayout(text, font, frc);
        Rectangle2D bounds = textLayout.getBounds();

        // 居左显示
        float x = 30;
        // 居中显示，并考虑上方间距
        float y = (float) bounds.getHeight() * 0.4f + textLayout.getAscent();

        // 将文本按空格分割为单词
        String[] words = text.split("");
        StringBuilder line = new StringBuilder();

        // 设置文字颜色
        g2d.setColor(Color.BLACK);

        for (String word : words) {
            if (line.length() == 0) {
                line.append(word);
            } else {
                String tempLine = line + word;
                TextLayout tempLayout = new TextLayout(tempLine, font, frc);
                if (tempLayout.getBounds().getWidth() <= maxLineWidth) {
                    line.append(word);
                } else {
                    g2d.drawString(line.toString(), x, y);
                    line.setLength(0);
                    line.append(word);
                    y += lineHeight;
                }
            }
        }

        // 绘制最后一行文本
        g2d.drawString(line.toString(), x, y);
        g2d.dispose();

        ImageIO.write(image, "png", output);
        output.flush();
    }

    public static void createImage(String text, OutputStream output) throws IOException {
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 50);
        FontRenderContext frc = new FontRenderContext(AffineTransform.getScaleInstance(1, 1), false, false);
        Rectangle2D r2d = font.getStringBounds(text, frc);
        int unitHeight = (int) Math.floor(r2d.getHeight());
        int width = (int) Math.round(r2d.getWidth()) + 3;
        int height = unitHeight + 5;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        g.setColor(Color.BLACK);
        g.setFont(font);
        g.drawString(text, 0, font.getSize());
        g.dispose();

        ImageIO.write(image, "png", output);
        output.flush();
    }

}
