package com.ruoyi.common.annotation;

import com.ruoyi.common.utils.poi.ExcelHandlerAdapter;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;

/**
 * 自定义导出Excel数据注解
 * 
 * @author ruoyi
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Excel
{
    /**
     * 导出时在excel中排序
     */
    int sort() default Integer.MAX_VALUE;

    /**
     * 导出到Excel中的名字.
     */
    String name() default "";

    /**
     * 日期格式, 如: yyyy-MM-dd
     */
    String dateFormat() default "";

    /**
     * 如果是字典类型，请设置字典的type值 (如: sys_user_sex)
     */
    String dictType() default "";

    /**
     * 读取内容转表达式 (如: 0=男,1=女,2=未知)
     */
    String readConverterExp() default "";

    /**
     * 分隔符，读取字符串组内容
     */
    String separator() default ",";

    /**
     * BigDecimal 精度 默认:-1(默认不开启BigDecimal格式化)
     */
    int scale() default -1;

    /**
     * BigDecimal 舍入规则 默认:BigDecimal.ROUND_HALF_EVEN
     */
    int roundingMode() default BigDecimal.ROUND_HALF_EVEN;

    /**
     * 导出时在excel中每个列的高度
     */
    double height() default 14;

    /**
     * 导出时在excel中每个列的宽度
     */
    double width() default 16;

    /**
     * 文字后缀,如% 90 变成90%
     */
    String suffix() default "";

    /**
     * 当值为空时,字段的默认值
     */
    String defaultValue() default "";

    /**
     * 提示信息
     */
    String prompt() default "";

    /**
     * 设置只能选择不能输入的列内容.
     */
    String[] combo() default {};

    /**
     * 是否从字典读数据到combo,默认不读取,如读取需要设置dictType注解.
     */
    boolean comboReadDict() default false;

    /**
     * 是否需要纵向合并单元格,应对需求:含有list集合单元格)
     */
    boolean needMerge() default false;

    /**
     * 是否导出数据,应对需求:有时我们需要导出一份模板,这是标题需要但内容需要用户手工填写.
     */
    boolean isExport() default true;

    /**
     * 另一个类中的属性名称,支持多级获取,以小数点隔开
     */
    String targetAttr() default "";

    /**
     * 是否自动统计数据,在最后追加一行统计数据总和
     */
    boolean isStatistics() default false;

    /**
     * 导出类型（0数字 1字符串 2图片）
     */
    ColumnType cellType() default ColumnType.STRING;

    /**
     * 导出列头背景颜色
     */
    IndexedColors headerBackgroundColor() default IndexedColors.GREY_50_PERCENT;

    /**
     * 导出列头字体颜色
     */
    IndexedColors headerColor() default IndexedColors.WHITE;

    /**
     * 导出单元格背景颜色
     */
    IndexedColors backgroundColor() default IndexedColors.WHITE;

    /**
     * 导出单元格字体颜色
     */
    IndexedColors color() default IndexedColors.BLACK;

    /**
     * 导出字段对齐方式
     */
    HorizontalAlignment align() default HorizontalAlignment.CENTER;

    /**
     * 自定义数据处理器
     */
    Class<?> handler() default ExcelHandlerAdapter.class;

    /**
     * 自定义数据处理器参数
     */
    String[] args() default {};

    /**
     * 字段类型（0：导出导入；1：仅导出；2：仅导入）
     */
    Type type() default Type.ALL;

    enum Type
    {
        ALL(0), EXPORT(1), IMPORT(2);
        private final int value;

        Type(int value)
        {
            this.value = value;
        }

        public int value()
        {
            return this.value;
        }
    }

    enum ColumnType
    {
        NUMERIC(0), STRING(1), IMAGE(2), TEXT(3);
        private final int value;

        ColumnType(int value)
        {
            this.value = value;
        }

        public int value()
        {
            return this.value;
        }
    }
}
