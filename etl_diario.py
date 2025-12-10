import pandas as pd
from sqlalchemy import create_engine, text
from sqlalchemy.exc import OperationalError
from datetime import date, timedelta
import sys

# --- CONFIGURACIÓN ---
LOCAL_DB_URL = 'postgresql://lighthouse:lighthouse@localhost:50000/lighthouse'
RENDER_DB_URL = 'postgresql://dashboard_db_i9qj_user:MNJ0YWGjMhUvwuWimYl7Ls05c9EW3IqB@dpg-d4qapd0gjchc73b921c0-a.oregon-postgres.render.com/dashboard_db_i9qj'

# ⚙️ CONFIGURACIÓN DE CARGA
DIAS_VENTANA = 2
TAMANO_LOTE = 2000

def probar_conexion(engine, nombre_db):
    """
    Intenta conectar a la base de datos haciendo un SELECT 1 simple.
    Retorna True si conecta, False si falla.
    """
    print(f"   🔎 Probando conexión a {nombre_db}...", end=" ")
    try:
        with engine.connect() as conn:
            conn.execute(text("SELECT 1"))
        print("✅ OK")
        return True
    except OperationalError:
        print("❌ FALLÓ")
        print(f"      (No se pudo establecer conexión con {nombre_db}. Verifica red/credenciales)")
        return False
    except Exception as e:
        print(f"❌ ERROR DESCONOCIDO: {e}")
        return False

def get_param_value(engine, param_id):
    try:
        with engine.connect() as conn:
            result = conn.execute(text(f"SELECT valor FROM lighthouse.lighthouse_parametros WHERE id = {param_id}"))
            row = result.fetchone()
            return row[0] if row else None
    except Exception:
        return None

def run_etl():
    print(f"--- 🚀 INICIANDO ETL (CON VALIDACIÓN DE CONEXIÓN) ---")

    try:
        # 1. Crear los motores (Esto no conecta todavía, solo configura)
        local_engine = create_engine(LOCAL_DB_URL)
        # pool_pre_ping=True ayuda a reconectar si la conexión estaba muerta
        render_engine = create_engine(RENDER_DB_URL, pool_pre_ping=True, connect_args={'connect_timeout': 10})

        # ==========================================
        # PASO 0: VALIDACIÓN DE CONECTIVIDAD 🛑
        # ==========================================
        print("\n[PASO 0] Validando Conexiones...")

        # Si falla Local O falla Nube, abortamos todo.
        if not probar_conexion(local_engine, "BASE LOCAL"):
            print("⛔ STOP: Abortando proceso por fallo en origen.")
            return

        if not probar_conexion(render_engine, "NUBE RENDER"):
            print("⛔ STOP: Abortando proceso por fallo en destino.")
            return

        # ==========================================
        # PASO 1: VERIFICAR VERSIÓN
        # ==========================================
        print("\n[PASO 1] Verificando Versiones...")
        val_local = get_param_value(local_engine, 3)
        val_cloud = get_param_value(render_engine, 3)

        print(f"   -> Versión Local: '{val_local}'")
        print(f"   -> Versión Nube:  '{val_cloud}'")

        if val_local and val_cloud == val_local:
            print("✅ El sistema ya está actualizado. No se requiere carga.")
            return

        print("⚡ DETECTADA NUEVA VERSIÓN. Iniciando carga...")

        # ==========================================
        # PASO 2: TABLA PRINCIPAL (VENTANA)
        # ==========================================
        fecha_corte = date.today() - timedelta(days=DIAS_VENTANA)
        print(f"\n[PASO 2] Procesando datos desde: {fecha_corte}...")

        query_recent = f"""
            SELECT client_id, entity_id, case_key, application, last_result,
                   created_on, hora_ini_scrapper, hora_fin_scrapper,
                   status, message, tribunal
            FROM lighthouse.lighthouse_scrapper_detalle
            WHERE created_on::date >= '{fecha_corte}'
        """

        df_recent = pd.read_sql(query_recent, local_engine)

        if df_recent.empty:
            print("   ⚠️ No hay datos recientes.")
        else:
            print(f"   -> Leídos {len(df_recent)} registros locales.")
            df_recent.columns = [x.lower() for x in df_recent.columns]

            with render_engine.connect() as conn:
                trans = conn.begin()
                try:
                    print(f"   -> 🧹 Limpiando Nube (desde {fecha_corte})...")
                    conn.execute(text(f"DELETE FROM lighthouse.lighthouse_scrapper_detalle WHERE created_on::date >= '{fecha_corte}'"))

                    print(f"   -> 📥 Subiendo registros (Lotes de {TAMANO_LOTE})...")
                    df_recent.to_sql(
                        'lighthouse_scrapper_detalle', conn, schema='lighthouse',
                        if_exists='append', index=False, method='multi',
                        chunksize=TAMANO_LOTE
                    )

                    trans.commit()
                    print("   ✅ Tabla Principal Sincronizada.")

                except Exception as e:
                    trans.rollback()
                    print(f"   ❌ ERROR EN PASO 2: {e}")
                    return

        # ==========================================
        # PASO 3: TABLAS DE ERROR
        # ==========================================
        print("\n[PASO 3] Sincronizando Errores...")

        df_error_p = pd.read_sql("SELECT * FROM lighthouse.lighthouse_last_result_error", local_engine)
        df_error_c = pd.read_sql("SELECT * FROM lighthouse.lighthouse_last_result_error_detalle", local_engine)

        if 'id' in df_error_p.columns: df_error_p = df_error_p.drop(columns=['id'])
        if 'id' in df_error_c.columns: df_error_c = df_error_c.drop(columns=['id'])
        df_error_p.columns = [x.lower() for x in df_error_p.columns]
        df_error_c.columns = [x.lower() for x in df_error_c.columns]

        with render_engine.connect() as conn:
            trans = conn.begin()
            try:
                try:
                    conn.execute(text("TRUNCATE TABLE lighthouse.lighthouse_last_result_error CASCADE"))
                except Exception:
                    conn.execute(text("DELETE FROM lighthouse.lighthouse_last_result_error_detalle"))
                    conn.execute(text("DELETE FROM lighthouse.lighthouse_last_result_error"))

                if not df_error_p.empty:
                    print(f"   -> Insertando Padre...")
                    df_error_p.to_sql('lighthouse_last_result_error', conn, schema='lighthouse', if_exists='append', index=False, method='multi', chunksize=TAMANO_LOTE)

                if not df_error_c.empty:
                    print(f"   -> Insertando Hijo...")
                    df_error_c.to_sql('lighthouse_last_result_error_detalle', conn, schema='lighthouse', if_exists='append', index=False, method='multi', chunksize=TAMANO_LOTE)

                trans.commit()
                print("   ✅ Errores Sincronizados.")

            except Exception as e:
                trans.rollback()
                print(f"   ❌ Error en Errores: {e}")

        # ==========================================
        # PASO 4: ACTUALIZAR MARCA
        # ==========================================
        print("\n[PASO 4] Actualizando Versión...")
        with render_engine.connect() as conn:
            trans = conn.begin()
            try:
                sql_update = text("""
                    INSERT INTO lighthouse.lighthouse_parametros (id, clave, valor)
                    VALUES (3, 'Fecha Ejecucion Proceso Carga Datos SCRAPER', :val )
                    ON CONFLICT (id) DO UPDATE SET valor = EXCLUDED.valor;
                """)
                conn.execute(sql_update, {"val": val_local})
                trans.commit()
                print(f"   ✅ Versión actualizada a: {val_local}")
            except Exception as e:
                trans.rollback()
                print(f"   ❌ Error final: {e}")

        print("\n🚀 PROCESO FINALIZADO.")

    except Exception as e:
        print(f"\n❌ ERROR CRÍTICO NO CONTROLADO: {e}")

if __name__ == "__main__":
    run_etl()
