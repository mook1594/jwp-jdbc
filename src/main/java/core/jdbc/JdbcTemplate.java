package core.jdbc;

import next.exception.DataAccessException;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcTemplate {
    public static int executeUpdate(String sql, Object... objects) throws DataAccessException {
        try (
                Connection con = ConnectionManager.getConnection();
                PreparedStatement pstmt = con.prepareStatement(sql);
        ) {
            setObjects(pstmt, objects);

            return pstmt.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException(ex);
        }
    }

    public static int executeUpdate(String sql, List<Object> list) throws DataAccessException {
        return executeUpdate(sql, list.toArray());
    }

    public static <T> T executeQuery(String sql, Object[] objects, RowMapper<T> mapper) throws DataAccessException {
        try (
                Connection con = ConnectionManager.getConnection();
                PreparedStatement pstmt = con.prepareStatement(sql);
        ) {
            setObjects(pstmt, objects);

            try (ResultSet rs = pstmt.executeQuery()) {
                return mapper.mapRow(rs);
            } catch (SQLException ex) {
                throw new DataAccessException(ex);
            }
        } catch (SQLException ex) {
            throw new DataAccessException(ex);
        }
    }

    public static <T> T executeQuery(String sql, List<Object> list, RowMapper<T> mapper) throws DataAccessException {
        return executeQuery(sql, list.toArray(), mapper);
    }

    public static <T> T executeQuery(String sql, RowMapper<T> mapper) throws DataAccessException {
        return executeQuery(sql, new Object[0], mapper);
    }

    public static <T> List<T> executeQuery(String sql, Class<T> type) throws DataAccessException {


        return executeQuery(sql, new Object[0], (rs) -> setResultListObjects(type, rs));
    }

    public static <T> List<T> executeQuery(String sql, List<Object> list, Class<T> type) throws DataAccessException {
        return executeQuery(sql, list.toArray(), (rs) -> setResultListObjects(type, rs));
    }

    private static void setObjects(PreparedStatement pstmt, Object[] objects) throws DataAccessException {
        if (objects == null) {
            return;
        }
        try {
            for (int i = 0; i < objects.length; i++) {
                pstmt.setObject(i + 1, objects[i]);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new DataAccessException(ex);
        }
    }

    private static <T> List<T> setResultListObjects(Class<T> type, ResultSet rs) {
        List<T> dataList = new ArrayList<>();

        try {
            while (rs.next()) {
                T obj = setResultObject(type, rs);
                dataList.add(obj);
            }
        } catch (Exception ex) {
            throw new DataAccessException(ex);
        }

        return dataList;
    }

    private static <T> T setResultObject(Class<T> type, ResultSet rs) throws Exception {
        T obj = type.newInstance();
        Field[] fields = type.getDeclaredFields();
        for (Field f : fields) {
            String fieldName = f.getName();

            setResultSetField(obj, f, rs.getObject(fieldName));
        }
        return obj;
    }

    private static void setResultSetField(Object obj, Field field, Object value) throws Exception {
        field.setAccessible(true);
        field.set(obj, value);
    }
}
