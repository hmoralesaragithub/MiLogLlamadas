package mx.unam.milogllamadas;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int CODIGO_SOLICITUD = 1;
    Activity actividad;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        actividad=this;
    }

    public void mostrarLlamadas(View v){
        //Verificamos para que no se solicite el permiso cada vez que demos click en el boton
        //si ya esta activo el permiso simplemente debe consultar el content provider y mostrar el log de llamadas
        if(verificarStatusPermiso()){
            consultarContentProviderLlamadas();
        }else{
            solicitarPermiso();
        }
    }


    //Metodos para solicitar permiso de ver el log de llamadas
    
    //En este metodo se verifica y se solicita el permiso
    public void solicitarPermiso(){
        //Preguntamos por el permiso read call log
        //y write call log
        boolean solicitarPermisoRCL =
                ActivityCompat.shouldShowRequestPermissionRationale(actividad, Manifest.permission.READ_CALL_LOG);

        boolean solicitarPermisoWCL =
                ActivityCompat.shouldShowRequestPermissionRationale(actividad, Manifest.permission.WRITE_CALL_LOG);

        if(solicitarPermisoRCL && solicitarPermisoWCL){
            Toast.makeText(MainActivity.this, "Los permisos ya fueron otorgados", Toast.LENGTH_SHORT).show();
        }else{
            ActivityCompat.requestPermissions(actividad,
                    new String[]{Manifest.permission.READ_CALL_LOG,Manifest.permission.WRITE_CALL_LOG},CODIGO_SOLICITUD);
        }
    }

    //En este metodo se verifica el otorgamiento real del permiso
    public boolean verificarStatusPermiso(){
        boolean permisoReadCallLog=ActivityCompat.checkSelfPermission(actividad,Manifest.permission.READ_CALL_LOG)== PackageManager.PERMISSION_GRANTED;
        boolean permisoWriteCallLog=ActivityCompat.checkSelfPermission(actividad,Manifest.permission.WRITE_CALL_LOG) == PackageManager.PERMISSION_GRANTED;

        if(permisoReadCallLog && permisoWriteCallLog){
            return true;
        }else {
            return false;
        }
    }


    //Se ejecuta en modo callback(2 plano) automaticamente despues de ejecutar nuestro metodo
    //solicitarPermiso
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case CODIGO_SOLICITUD:
                if (verificarStatusPermiso()){
                    Toast.makeText(MainActivity.this, "Ya esta activo realmente el permiso", Toast.LENGTH_SHORT).show();
                    consultarContentProviderLlamadas();
                }else{
                    Toast.makeText(MainActivity.this, "No se ha activado el permiso", Toast.LENGTH_SHORT).show();
                }
        }

    }

    public void consultarContentProviderLlamadas(){
        TextView tvLlamadas=(TextView)findViewById(R.id.tvLlamadas);
        tvLlamadas.setText("");

        //obtenemos el Uri del log de llamadas
        Uri direccionUriLlamadas= CallLog.Calls.CONTENT_URI;

        //Ahora definimos que campos queremos recuperar de la tabla call
        //Numero, fecha, tipo, duracion
        String[] campos={
                CallLog.Calls.NUMBER,
                CallLog.Calls.DATE,
                CallLog.Calls.TYPE,
                CallLog.Calls.DURATION
        };

        //Necesitamos un ContentResolver para ejecutar nuestro query
        //y obtener el resultado de los 4 campos
        ContentResolver contentResolver=getContentResolver();
        Cursor registros=contentResolver.query(direccionUriLlamadas,campos,null,null, CallLog.Calls.DATE + " DESC");

        while (registros.moveToNext()) {
            //Debido a que no sabemos en que columna esta ubicado cada campo
            //usamos getColumnIndex para buscarlo por el nombre de la columna
            String numero=registros.getString(registros.getColumnIndex(campos[0]));
            Long fecha=registros.getLong(registros.getColumnIndex(campos[1]));
            int tipo=registros.getInt(registros.getColumnIndex(campos[2]));
            String duracion=registros.getString(registros.getColumnIndex(campos[3]));

            //Validacion del tipo de llamada
            String tipoLlamada="";
            switch (tipo){
                case CallLog.Calls.INCOMING_TYPE:
                    tipoLlamada=getResources().getString(R.string.entrante);
                    break;
                case CallLog.Calls.MISSED_TYPE:
                    tipoLlamada=getResources().getString(R.string.perdida);
                    break;
                case CallLog.Calls.OUTGOING_TYPE:
                    tipoLlamada=getResources().getString(R.string.saliente);
                    break;
                default:
                    tipoLlamada=getResources().getString(R.string.desconocida);
            }

            String detalle= getResources().getString(R.string.etiqueta_numero) + numero +
            //le damos formato de fecha a la variable fecha k:hora mm:minutos
                    "\n" + getResources().getString(R.string.etiqueta_fecha) + DateFormat.format("dd/mm/yy k:mm",fecha) +
                    "\n" + getResources().getString(R.string.etiqueta_tipo) + tipoLlamada +
                    "\n" + getResources().getString(R.string.etiqueta_duracion) + duracion + " segundos";

            //Como necesitamos ver el log no usamos setText ya que se cambiaria con la ultima llamada
            //para concatenar usamos append
            tvLlamadas.append(detalle);

            //Ahora si solo falta dar permiso en nuestro archivo AndroidManifest.xml el ReadCallLog y el WriteCallLog
        }
    }
}
