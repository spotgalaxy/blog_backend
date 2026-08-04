package com.linshen.blog.mybatis;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * PostgreSQL jsonb 列专用 TypeHandler。
 * 父类 JacksonTypeHandler 用 ps.setString 绑定参数，PG 无法将 varchar 隐式转为 jsonb，
 * 会报 "column is of type jsonb but expression is of type character varying"。
 * 改用 ps.setObject(json, Types.OTHER) 让驱动以 unknown 类型发送，由 PG 完成 jsonb 转换。
 */
public class JsonbTypeHandler extends JacksonTypeHandler {

    public JsonbTypeHandler(Class<?> type) {
        super(type);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter,
                                    JdbcType jdbcType) throws SQLException {
        ps.setObject(i, toJson(parameter), Types.OTHER);
    }
}
