package com.lighthouse.dashboard.repository;

import com.lighthouse.dashboard.model.ResumenDiario;
import com.lighthouse.dashboard.model.TimeSeriesData;
import com.lighthouse.dashboard.model.ErrorStat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Repository
public class DashboardRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // --- 1. RESUMEN PRINCIPAL ---
    private static final String BASE_SQL_QUERY =
            "select " +
                    "client_id as cliente, " +
                    "entity_id as entidad," +
                    "to_char(created_on,'DD/MM/YYYY') as fecha_scrapper," +
                    "min(hora_ini_scrapper)::TEXT as hora_inicio, " +
                    "max(hora_fin_scrapper)::TEXT as hora_fin," +
                    "status as status," +
                    "message as message," +
                    "count(*) as cantidad " +
                    "from lighthouse.lighthouse_scrapper_detalle as a " +
                    "where cast(a.created_on as date) = TO_DATE(?, 'YYYY-MM-DD') " +
                    "group by client_id,entity_id,to_char(created_on,'DD/MM/YYYY'), status,message " +
                    "order by client_id,entity_id";

    public List<ResumenDiario> getResumenDiario(LocalDate date) {
        String dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return jdbcTemplate.query(
                BASE_SQL_QUERY,
                new Object[]{dateString},
                (rs, rowNum) -> new ResumenDiario(
                        rs.getString("cliente"),
                        rs.getString("entidad"),
                        rs.getString("fecha_scrapper"),
                        rs.getString("hora_inicio"),
                        rs.getString("hora_fin"),
                        rs.getString("status"),
                        rs.getString("message"),
                        rs.getInt("cantidad")
                )
        );
    }

    // --- 2. SERIE DE TIEMPO (TENDENCIA) ---
    private static final String TIME_SERIES_SQL =
            "SELECT to_char(a.created_on, 'DD-MM-YYYY') AS date, a.status, COUNT(*) AS quantity " +
                    "FROM lighthouse.lighthouse_scrapper_detalle AS a " +
                    "WHERE CAST(a.created_on AS DATE) > NOW()::DATE - 7 " +
                    "  AND a.status IN ('SCRAPED-ERROR', 'ERROR', 'SCRAPING') " +
                    "GROUP BY 1, 2 " +
                    "ORDER BY MIN(a.created_on), 2";

    public List<TimeSeriesData> getTimeSeriesData() {
        return jdbcTemplate.query(
                TIME_SERIES_SQL,
                (rs, rowNum) -> new TimeSeriesData(
                        rs.getString("date"),
                        rs.getString("status"),
                        rs.getLong("quantity")
                )
        );
    }

    // --- 3. TOP ERRORES ---
    private static final String TOP_ERRORS_SQL =
            "SELECT " +
                    "   COALESCE(message, 'SIN MENSAJE') as mensaje_error, " +
                    "   status, " +
                    "   COUNT(*) as cantidad " +
                    "FROM lighthouse.lighthouse_scrapper_detalle " +
                    "WHERE cast(created_on as date) = TO_DATE(?, 'YYYY-MM-DD') " +
                    "  AND (? = 'Todos' OR status = ?) " +
                    "  AND (? = 'Todos' OR entity_id = ?) " +
                    "  AND (? = '' OR message ILIKE ?) " +
                    "  AND status IN ('ERROR', 'SCRAPED-ERROR') " +
                    "GROUP BY message, status " +
                    "ORDER BY cantidad DESC " +
                    "LIMIT 500";

    public List<ErrorStat> getTopErrors(LocalDate date, String status, String entidad, String msgSearch) {
        String dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String search = (msgSearch == null) ? "" : msgSearch.trim();

        Object[] params = new Object[]{
                dateString,
                status, status,
                entidad, entidad,
                search, "%" + search + "%"
        };

        return jdbcTemplate.query(
                TOP_ERRORS_SQL,
                params,
                (rs, rowNum) -> new ErrorStat(
                        rs.getString("mensaje_error"),
                        rs.getString("status"),
                        rs.getLong("cantidad")
                )
        );
    }

    // --- 4. ULTIMA ACTUALIZACION ---
    private static final String UPDATE_DATE_SQL = "SELECT valor FROM lighthouse.lighthouse_parametros WHERE id = 3";

    public String getLastUpdateDate() {
        try {
            return jdbcTemplate.queryForObject(UPDATE_DATE_SQL, String.class);
        } catch (Exception e) {
            return "Sin datos";
        }
    }

    // --- 5. NOMBRE DEL AMBIENTE ---
    private static final String ENV_DB_SQL = "SELECT valor FROM lighthouse.lighthouse_parametros WHERE id = 5";

    public String getEnvironmentName() {
        try {
            return jdbcTemplate.queryForObject(ENV_DB_SQL, String.class);
        } catch (Exception e) {
            return "DESCONOCIDO";
        }
    }

    // --- 6. BÚSQUEDA POR CASE KEY ---
    private static final String SEARCH_BY_CASE_KEY_SQL =
            "SELECT " +
                    "   client_id as cliente, entity_id as entidad, " +
                    "   to_char(created_on,'DD/MM/YYYY') as fecha_scrapper, " +
                    "   min(hora_ini_scrapper)::TEXT as hora_inicio, " +
                    "   max(hora_fin_scrapper)::TEXT as hora_fin, " +
                    "   status, message " +
                    "FROM lighthouse.lighthouse_scrapper_detalle " +
                    "WHERE case_key = ? " +
                    "AND cast(created_on as date) = TO_DATE(?, 'YYYY-MM-DD') " +
                    "GROUP BY client_id, entity_id, fecha_scrapper, status, message";

    public List<ResumenDiario> findByCaseKey(String caseKey, LocalDate date) {
        String dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return jdbcTemplate.query(
                SEARCH_BY_CASE_KEY_SQL,
                new Object[]{caseKey.trim(), dateString},
                (rs, rowNum) -> new ResumenDiario(
                        rs.getString("cliente"), rs.getString("entidad"),
                        rs.getString("fecha_scrapper"), rs.getString("hora_inicio"),
                        rs.getString("hora_fin"), rs.getString("status"),
                        rs.getString("message"),
                        1
                )
        );
    }

    // --- 7. DETALLE ERROR PARA EXPORTAR ---
    private static final String ERROR_DETALLE_SQL =
            "SELECT " +
                    "   to_char(a.created_on, 'DD/MM/YYYY') as created_on, " +
                    "   a.client_id, a.entity_id, a.case_key, a.tribunal, a.application, " +
                    "   a.last_result, " +
                    "   b.nivel, b.clave, b.cant, " +
                    "   c.last_result as last_result_OK " +
                    "FROM lighthouse.lighthouse_scrapper_detalle as a " +
                    "INNER JOIN lighthouse.lighthouse_last_result_error_detalle as b ON a.last_result = b.last_result_error " +
                    "LEFT JOIN lighthouse.lighthouse_last_result_ok as c ON a.client_id = c.client_id AND a.entity_id = c.entity_id AND a.case_key = c.case_key " +
                    "WHERE cast(a.created_on as date) = TO_DATE(?, 'YYYY-MM-DD') " +
                    "  AND a.status = 'SCRAPED-ERROR' " +
                    "ORDER BY a.created_on, a.client_id, a.case_key";

    public List<Map<String, Object>> getErrorDetail(LocalDate date) {
        String dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return jdbcTemplate.queryForList(ERROR_DETALLE_SQL, dateString);
    }

    // --- 8. DETALLE DE FILA (ROW DETAIL) ---
    private static final String ROW_DETAIL_SQL =
            "SELECT " +
                    "   client_id, entity_id, " +
                    "   to_char(created_on,'DD/MM/YYYY') as fecha, " +
                    "   hora_ini_scrapper::TEXT as hora_inicio, " +
                    "   hora_fin_scrapper::TEXT as hora_fin, " +
                    "   status, message, tribunal, case_key, application, last_result " +
                    "FROM lighthouse.lighthouse_scrapper_detalle " +
                    "WHERE cast(created_on as date) = TO_DATE(?, 'YYYY-MM-DD') " +
                    "  AND client_id = ? " +
                    "  AND entity_id = ? " +
                    "  AND status = ? " +
                    "  AND COALESCE(message,'0') = COALESCE(?, '0')";

    public List<Map<String, Object>> getRowDetail(LocalDate date, String client, String entity, String status, String message) {
        String dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return jdbcTemplate.queryForList(ROW_DETAIL_SQL, dateString, client, entity, status, message);
    }


} // <--- ¡ASEGÚRATE DE QUE ESTA LLAVE ESTÉ PRESENTE!
