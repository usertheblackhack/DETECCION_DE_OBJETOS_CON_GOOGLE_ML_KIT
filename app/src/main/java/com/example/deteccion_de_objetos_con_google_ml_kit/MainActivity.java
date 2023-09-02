package com.example.deteccion_de_objetos_con_google_ml_kit;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


/* INTEGRANTES:
CASANOVA MORANTE HECTOR MIGUEL
LETURNE PLUAS JHON BYRON
OCHOA GILCES GEOVANNY ALEXANDER
PIÑA PAREDES MIGUEL ANGEL
*/


public class MainActivity extends AppCompatActivity {

    ImageView imagenview;
    InputImage imgImput;


    public static int REQUEST_CAMERA = 111;
    public static int REQUEST_GALLERY = 222;
    //
    Button galeria;
    Button camara;
    TextView txt_resultados;

    Bitmap bitmap;

    String ruta_abs;

    static String ERROR_APP_EXEC="ERROR_EJECUCION";
    static String SUCESS_APP_EXEC="EJECUTO_CORRECTAMENTE";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        camara_galeria();
    }

    public void init() {
        ruta_abs="";
        galeria = findViewById(R.id.btGallery);
        camara = findViewById(R.id.btCamera);
        /*******************************************/
        imagenview = findViewById(R.id.image_view);
        imagenview.setDrawingCacheEnabled(true); //habilita la cache del drawing
        imagenview.buildDrawingCache(); //construye la cache
        /*******************************************/
        txt_resultados = findViewById(R.id.txtresults);
    }

    public void camara_galeria() {

        //abre la galeria
        galeria.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ruta_abs="";
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                try {
                    cameraLa.launch(intent);
                } catch (Exception ex) {
                    Log.i(ERROR_APP_EXEC, ex.getMessage());
                }
            }
        });


        //abre la camara
        camara.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                try {
                    File file_imagen = temporal_file();
                    if (file_imagen != null) {
                        Uri foto_uri = FileProvider.getUriForFile(MainActivity.this,
                                "com.example.deteccion_de_objetos_con_google_ml_kit.fileprovider", file_imagen);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, foto_uri);
                        cameraLa.launch(intent);
                    }
                } catch (Exception ex) {
                    Log.i(ERROR_APP_EXEC, ex.getMessage());
                }
            }
        });

    }


    //Obtiene los resultados de la imagen y la transforma de BitMap a ImputImage
    ActivityResultLauncher<Intent> cameraLa = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                    if (result.getResultCode() == RESULT_OK) {
                        Bitmap imgBipMap;
                        if (ruta_abs=="") { //PARA GALERIA
                            imagenview.setImageURI(result.getData().getData());
                            imgBipMap = imagenview.getDrawingCache();
                            bitmap = imagenview.getDrawingCache();
                        } else { //para la camara
                            imgBipMap = BitmapFactory.decodeFile(ruta_abs);
                            imgBipMap=rotate_image(imgBipMap,90);
                            imagenview.setImageBitmap(imgBipMap);
                            bitmap = imgBipMap;
                        }

                        imgImput = InputImage.fromBitmap(imgBipMap, 0);
                        Log.i(SUCESS_APP_EXEC, "La imagen esta en InputImage");
                        lecturaCodigoQR();
                    }
                }
            });


    //crea el archivo de imagen temporal
    public File temporal_file() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        ruta_abs=image.getAbsolutePath();
        return image;
    }

    //ROTA LA IMAGEN
    public Bitmap rotate_image(Bitmap bitmap, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                matrix, true);
    }

    private void lecturaCodigoQR()
    {
        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_QR_CODE,
                                Barcode.FORMAT_AZTEC,
                                Barcode.FORMAT_CODE_128,
                                Barcode.FORMAT_CODE_39,
                                Barcode.FORMAT_CODE_93,
                                Barcode.FORMAT_CODABAR,
                                Barcode.FORMAT_EAN_13,
                                Barcode.FORMAT_EAN_8,
                                Barcode.FORMAT_ITF,
                                Barcode.FORMAT_UPC_A,
                                Barcode.FORMAT_UPC_E,
                                Barcode.FORMAT_PDF417,
                                Barcode.FORMAT_DATA_MATRIX)
                        .build();
        BarcodeScanner scanner= BarcodeScanning.getClient(options);
        Task<List<Barcode>> result=scanner.process(imgImput)
                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(List<Barcode> barcodes) {
                        for(Barcode barcode:barcodes) {
                            Point[] corde = barcode.getCornerPoints();

                            paint_points(corde);

                            int tipoValor = barcode.getValueType();
                            Log.i("dfssdfsdfsdf", tipoValor + "");
                            String concat = "";

                            if (tipoValor == Barcode.TYPE_WIFI) {
                                String ssid = barcode.getWifi().getSsid();
                                String password = barcode.getWifi().getPassword();
                                int type = barcode.getWifi().getEncryptionType();
                                concat = "SSID: " + ssid + "\n" + "CONTRASEÑA: " + password + "\n" + "Tipo: " + type;
                            } else if (tipoValor == Barcode.TYPE_URL) {
                                String title = barcode.getUrl().getTitle();
                                String url = barcode.getUrl().getUrl();
                                concat = "Titulo: " + title + "\n" + "URL: " + url;
                            } else if (tipoValor == Barcode.TYPE_SMS) {
                                String mensaje = barcode.getSms().getMessage();
                                String telefono = barcode.getSms().getPhoneNumber();
                                concat = "Mensaje: " + mensaje + "\n" + "Telefono: " + telefono;
                            } else if (tipoValor == Barcode.TYPE_PHONE) {
                                String numero = barcode.getPhone().getNumber();
                                concat = "Numero telefono: " + numero;
                            } else if (tipoValor == Barcode.TYPE_CALENDAR_EVENT) {
                                String titulo = barcode.getCalendarEvent().getDescription();
                                String estado = barcode.getCalendarEvent().getStatus();
                                String locacion = barcode.getCalendarEvent().getLocation();
                                String resumen = barcode.getCalendarEvent().getSummary();
                                String organizador = barcode.getCalendarEvent().getOrganizer();
                                String fecha_final = barcode.getCalendarEvent().getEnd().getDay() +
                                        "/" + barcode.getCalendarEvent().getEnd().getMonth() +
                                        "/" + barcode.getCalendarEvent().getEnd().getYear() + "  " + barcode.getCalendarEvent().getEnd().getHours() +
                                        ":" + barcode.getCalendarEvent().getEnd().getMinutes() +
                                        ":" + barcode.getCalendarEvent().getEnd().getSeconds();

                                String fecha_inicio = barcode.getCalendarEvent().getStart().getDay() +
                                        "/" + barcode.getCalendarEvent().getStart().getMonth() +
                                        "/" + barcode.getCalendarEvent().getStart().getYear() +
                                        "  " + barcode.getCalendarEvent().getStart().getHours() +
                                        ":" + barcode.getCalendarEvent().getStart().getMinutes() +
                                        ":" + barcode.getCalendarEvent().getStart().getSeconds();

                                concat = "Titulo: " + titulo + "\n" +
                                        "Estado: " + estado + "\n"
                                        + "Locacion: " + locacion + "\n"
                                        + "Resumen: " + resumen + "\n"
                                        + "Organizador" + organizador + "\n"
                                        + "Fecha inicio: " + fecha_inicio + "\n"
                                        + "Fecha final: " + fecha_final + "\n";

                            } else if (tipoValor == Barcode.TYPE_GEO) {
                                String latitud = barcode.getGeoPoint().getLat() + "";
                                String longitud = barcode.getGeoPoint().getLng() + "";
                                concat = "Latitud: " + latitud + " Longitud: " + longitud;
                            } else if (tipoValor == Barcode.TYPE_CONTACT_INFO) {
                                String nombre = barcode.getContactInfo().getName().getFirst() + " " + barcode.getContactInfo().getName().getLast();
                                String titulo = barcode.getContactInfo().getTitle();
                                String organizacion = barcode.getContactInfo().getOrganization();
                                concat = "Nombre: " + nombre + "\n" + " Titulo: " + titulo + "\n" + "Organizacion: " + organizacion;

                            } else if (tipoValor == Barcode.TYPE_EMAIL) {
                                String email = barcode.getEmail().getAddress();
                                String cuerpo = barcode.getEmail().getBody();
                                concat = "Email: " + email + "\n" + " Cuerpo: " + cuerpo;
                            } else if (tipoValor == Barcode.TYPE_PHONE)
                                concat = "Numero: " + barcode.getPhone().getNumber();
                            else if (tipoValor == Barcode.TYPE_UNKNOWN)
                                concat = "Error! no se reconocio ningun codigo Por favor itente nuevamente";
                            else {
                                Log.i("dataJhoncito", barcode.getRawValue());
                                concat = "El codigo de barra es: " + barcode.getRawValue();
                            }
                            txt_resultados.setText(concat);
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(ERROR_APP_EXEC,e.getMessage());
                    }
                });
    }


    public void paint_points(Point[] point)
    {
        try
        {
            Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutableBitmap);
            Paint paint = new Paint();
            paint.setColor(Color.parseColor("#0073D2"));
            paint.setStrokeWidth(25);
            for(int x=0;x<point.length;x++){

                if (x <= 2)
                    canvas.drawLine(point[x].x, point[x].y, point[x+1].x, point[x+1].y, paint);
                else
                    canvas.drawLine(point[x].x, point[x].y, point[0].x, point[0].y, paint);
            }
            imagenview.setImageBitmap(mutableBitmap);
        }
        catch (Exception ex){
            Log.i(ERROR_APP_EXEC,ex.getMessage());
        }
    }

}